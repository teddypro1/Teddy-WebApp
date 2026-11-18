import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { importPKCS8, SignJWT } from "npm:jose@5.9.6";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { ...corsHeaders, "Content-Type": "application/json" } });
}
async function readJson(req: Request) {
  try { return await req.json(); } catch { return null; }
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed." }, 405);
  const authorization = req.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) return json({ error: "Missing user session." }, 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceRoleKey) {
    console.error("Required Supabase server environment variables are missing.");
    return json({ error: "Server authorization configuration is incomplete." }, 500);
  }

  const input = await readJson(req);
  if (!input || typeof input !== "object" || Array.isArray(input)) return json({ error: "Request body must be a JSON object." }, 400);

  try {
    const userResponse = await fetch(`${supabaseUrl}/auth/v1/user`, { headers: { apikey: anonKey, Authorization: authorization } });
    if (!userResponse.ok) return json({ error: "Invalid or expired user session." }, 401);
    const user = await userResponse.json();
    if (!user?.id) return json({ error: "Authenticated user could not be verified." }, 401);

    const adminResponse = await fetch(
      `${supabaseUrl}/rest/v1/admin_users?select=user_id&user_id=eq.${encodeURIComponent(user.id)}&limit=1`,
      { headers: { apikey: serviceRoleKey, Authorization: `Bearer ${serviceRoleKey}` } },
    );
    if (!adminResponse.ok) {
      console.error("Admin authorization lookup failed:", adminResponse.status);
      return json({ error: "Could not verify administrator access." }, 503);
    }
    const adminRows = await adminResponse.json();
    const isAdmin = Array.isArray(adminRows) && adminRows.some((row: { user_id?: string }) => row.user_id === user.id);
    const action = typeof input.action === "string" ? input.action : "send";
    if (action === "check_admin") return json({ success: true, is_admin: isAdmin });
    if (!isAdmin) return json({ error: "Administrator access required." }, 403);

    if (action === "history") {
      const historyResponse = await fetch(
        `${supabaseUrl}/rest/v1/notification_history?select=id,title,body,audience,total_devices,sent_count,failed_count,created_at&admin_user_id=eq.${encodeURIComponent(user.id)}&order=created_at.desc&limit=50`,
        {
          headers: {
            apikey: serviceRoleKey,
            Authorization: `Bearer ${serviceRoleKey}`,
          },
        },
      );
      if (!historyResponse.ok) {
        console.error("Notification history lookup failed:", historyResponse.status);
        return json({ error: "Could not load notification history." }, 502);
      }
      const history = await historyResponse.json();
      return json({ success: true, history: Array.isArray(history) ? history : [] });
    }

    if (action !== "send") return json({ error: "Unsupported action." }, 400);

    const title = typeof input.title === "string" ? input.title.trim().slice(0, 100) : "";
    const body = typeof input.body === "string" ? input.body.trim().slice(0, 500) : "";
    if (!title || !body) return json({ error: "Both title and body are required." }, 400);

    // Build 1 only permits the administrator to test delivery to their own registered devices.
    const tokensResponse = await fetch(
      `${supabaseUrl}/rest/v1/device_tokens?select=fcm_token&user_id=eq.${encodeURIComponent(user.id)}&platform=eq.android`,
      { headers: { apikey: anonKey, Authorization: authorization } },
    );
    if (!tokensResponse.ok) return json({ error: "Could not read registered administrator devices." }, 502);
    const rows = await tokensResponse.json();
    if (!Array.isArray(rows) || rows.length === 0) return json({ error: "No registered Android devices were found for the administrator account." }, 404);

    const serviceAccountRaw = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON");
    if (!serviceAccountRaw) return json({ error: "Firebase server configuration is incomplete." }, 500);
    const serviceAccount = JSON.parse(serviceAccountRaw);
    const projectId = serviceAccount.project_id, clientEmail = serviceAccount.client_email, privateKey = serviceAccount.private_key;
    if (!projectId || !clientEmail || !privateKey) return json({ error: "Firebase service account JSON is missing required fields." }, 500);

    const key = await importPKCS8(privateKey, "RS256");
    const now = Math.floor(Date.now() / 1000);
    const assertion = await new SignJWT({ scope: "https://www.googleapis.com/auth/firebase.messaging" })
      .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuer(clientEmail).setSubject(clientEmail)
      .setAudience("https://oauth2.googleapis.com/token").setIssuedAt(now).setExpirationTime(now + 3600).sign(key);
    const oauthResponse = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
    });
    const oauthData = await oauthResponse.json();
    if (!oauthResponse.ok || !oauthData.access_token) {
      console.error("Firebase OAuth failed:", oauthData.error ?? "unknown");
      return json({ error: "Could not obtain Firebase access token." }, 502);
    }

    const results = await Promise.all(rows.map(async (row: { fcm_token: string }) => {
      const response = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
        method: "POST",
        headers: { Authorization: `Bearer ${oauthData.access_token}`, "Content-Type": "application/json" },
        body: JSON.stringify({ message: {
          token: row.fcm_token, notification: { title, body }, data: { title, body }, android: { priority: "HIGH" },
        } }),
      });
      const responseBody = await response.json().catch(() => ({}));
      return { success: response.ok, status: response.status, error: response.ok ? undefined : responseBody };
    }));
    const sent = results.filter((result) => result.success).length;
    const historyResponse = await fetch(`${supabaseUrl}/rest/v1/notification_history`, {
      method: "POST",
      headers: { apikey: serviceRoleKey, Authorization: `Bearer ${serviceRoleKey}`, "Content-Type": "application/json", Prefer: "return=minimal" },
      body: JSON.stringify({
        admin_user_id: user.id, title, body, audience: "admin_devices",
        total_devices: rows.length, sent_count: sent, failed_count: rows.length - sent,
      }),
    });
    if (!historyResponse.ok) console.error("Notification history insert failed:", historyResponse.status);
    return json({ success: sent > 0, sent, total: rows.length, history_saved: historyResponse.ok, results });
  } catch (error) {
    console.error("Push notification function error:", error);
    return json({ error: "Unexpected server error while processing the request." }, 500);
  }
});
