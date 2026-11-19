import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
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
    console.error("Device registry server configuration is incomplete.");
    return json({ error: "Server configuration is incomplete." }, 500);
  }

  try {
    const authResponse = await fetch(supabaseUrl + "/auth/v1/user", {
      headers: { apikey: anonKey, Authorization: authorization },
    });
    if (!authResponse.ok) return json({ error: "Invalid or expired user session." }, 401);
    const user = await authResponse.json();
    if (typeof user?.id !== "string") return json({ error: "Authenticated user could not be verified." }, 401);

    const input = await req.json().catch(() => null);
    if (!input || typeof input !== "object" || Array.isArray(input)) {
      return json({ error: "Request body must be a JSON object." }, 400);
    }
    const action = input.action;
    const installationId = typeof input.installation_id === "string" ? input.installation_id : "";
    const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    if (!uuidPattern.test(installationId)) return json({ error: "Invalid installation ID." }, 400);

    const admin = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    if (action === "unregister") {
      const { error } = await admin.from("device_tokens")
        .delete()
        .eq("installation_id", installationId)
        .eq("user_id", user.id);
      if (error) {
        console.error("Device unregister failed:", error.message);
        return json({ error: "Could not unregister this device." }, 502);
      }
      return json({ success: true, action: "unregistered" });
    }

    if (action !== "register") return json({ error: "Unsupported action." }, 400);
    const token = typeof input.fcm_token === "string" ? input.fcm_token.trim() : "";
    if (token.length < 20 || token.length > 4096) return json({ error: "Invalid FCM token." }, 400);
    if (input.platform !== "android") return json({ error: "Unsupported device platform." }, 400);

    // A token is globally unique to an app installation. Reassign only that exact
    // token, then upsert by the stable installation ID so token rotations update
    // the same device row and account switches cannot leave stale ownership.
    const { error: tokenCleanupError } = await admin.from("device_tokens")
      .delete()
      .eq("fcm_token", token)
      .neq("installation_id", installationId);
    if (tokenCleanupError) {
      console.error("Existing token ownership reconciliation failed:", tokenCleanupError.message);
      return json({ error: "Could not reconcile the existing device token." }, 502);
    }

    const { error } = await admin.from("device_tokens").upsert({
      installation_id: installationId,
      user_id: user.id,
      fcm_token: token,
      platform: "android",
      updated_at: new Date().toISOString(),
    }, { onConflict: "installation_id" });
    if (error) {
      console.error("Device registration failed:", error.message);
      return json({ error: "Could not register this device." }, 502);
    }

    return json({ success: true, action: "registered", user_id: user.id });
  } catch (error) {
    console.error("Device registry error:", error);
    return json({ error: "Unexpected device registry error." }, 500);
  }
});