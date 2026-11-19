import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { initializeApp, cert, getApps } from "npm:firebase-admin@13.6.0/app";
import { getMessaging } from "npm:firebase-admin@13.6.0/messaging";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
const allowedTopics = new Set(["all_users", "announcements", "updates"]);
const json = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), { status, headers: { ...cors, "Content-Type": "application/json" } });

function messaging() {
  const raw = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON");
  if (!raw) throw new Error("Firebase server configuration is missing.");
  const account = JSON.parse(raw);
  const app = getApps()[0] ?? initializeApp({ credential: cert(account) });
  return getMessaging(app);
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return json({ error: "Method not allowed." }, 405);
  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    const authHeader = req.headers.get("Authorization") ?? "";
    if (!supabaseUrl || !serviceKey || !authHeader.startsWith("Bearer ")) {
      return json({ error: "Authentication required." }, 401);
    }
    const userResponse = await fetch(`${supabaseUrl}/auth/v1/user`, {
      headers: { apikey: serviceKey, Authorization: authHeader },
    });
    if (!userResponse.ok) return json({ error: "Invalid or expired session." }, 401);
    const user = await userResponse.json();
    if (!user?.id) return json({ error: "Account could not be verified." }, 401);
    const input = await req.json().catch(() => null);
    if (!input || typeof input.action !== "string") return json({ error: "Invalid request." }, 400);
    const adminHeaders = { apikey: serviceKey, Authorization: `Bearer ${serviceKey}` };

    if (input.action === "preferences") {
      const response = await fetch(
        `${supabaseUrl}/rest/v1/user_topic_subscriptions?select=topic,subscribed&user_id=eq.${user.id}`,
        { headers: adminHeaders },
      );
      if (!response.ok) return json({ error: "Could not load topic preferences." }, 502);
      const rows = await response.json();
      const preferences = Object.fromEntries(["all_users", "announcements", "updates"].map((topic) => {
        const row = rows.find((item: any) => item.topic === topic);
        return [topic, row ? Boolean(row.subscribed) : topic === "all_users"];
      }));
      return json({ success: true, preferences });
    }

    if (input.action === "subscribe" || input.action === "unsubscribe") {
      const topic = input.topic;
      if (typeof topic !== "string" || !allowedTopics.has(topic)) {
        return json({ error: "Unsupported notification topic." }, 400);
      }
      const tokenResponse = await fetch(
        `${supabaseUrl}/rest/v1/device_tokens?select=fcm_token&user_id=eq.${user.id}&platform=eq.android`,
        { headers: adminHeaders },
      );
      if (!tokenResponse.ok) return json({ error: "Could not load this account's devices." }, 502);
      const tokenRows = await tokenResponse.json();
      const tokens = [...new Set(tokenRows.map((row: any) => row.fcm_token).filter((value: any) => typeof value === "string"))];
      if (tokens.length === 0) return json({ error: "No registered Android device was found for this account." }, 409);
      const client = messaging();
      const subscribed = input.action === "subscribe";
      const errors: string[] = [];
      for (let index = 0; index < tokens.length; index += 1000) {
        const batch = tokens.slice(index, index + 1000);
        const result = subscribed
          ? await client.subscribeToTopic(batch, topic)
          : await client.unsubscribeFromTopic(batch, topic);
        if (result.failureCount > 0) errors.push(`${result.failureCount} device(s) failed`);
      }
      if (errors.length) return json({ error: errors.join("; "), partial: true }, 502);
      const prefResponse = await fetch(`${supabaseUrl}/rest/v1/user_topic_subscriptions`, {
        method: "POST",
        headers: { ...adminHeaders, "Content-Type": "application/json", Prefer: "resolution=merge-duplicates,return=minimal" },
        body: JSON.stringify({ user_id: user.id, topic, subscribed, updated_at: new Date().toISOString() }),
      });
      if (!prefResponse.ok) return json({ error: "Device topic updated, but preference could not be saved." }, 502);
      return json({ success: true, topic, subscribed, devices_updated: tokens.length });
    }

    if (input.action === "send_topic") {
      const adminResponse = await fetch(
        `${supabaseUrl}/rest/v1/admin_users?select=user_id&user_id=eq.${user.id}&limit=1`,
        { headers: adminHeaders },
      );
      if (!adminResponse.ok) return json({ error: "Could not verify administrator access." }, 502);
      if ((await adminResponse.json()).length === 0) return json({ error: "Administrator access required." }, 403);
      const topic = input.topic;
      const title = typeof input.title === "string" ? input.title.trim() : "";
      const body = typeof input.body === "string" ? input.body.trim() : "";
      if (typeof topic !== "string" || !allowedTopics.has(topic)) return json({ error: "Unsupported notification topic." }, 400);
      if (!title || title.length > 100 || !body || body.length > 500) return json({ error: "Title (1–100) and message (1–500) are required." }, 400);
      const messageId = await messaging().send({
        topic, notification: { title, body }, data: { title, body, topic },
        android: { priority: "high", ttl: 604800000 },
      });
      const history = await fetch(`${supabaseUrl}/rest/v1/notification_history`, {
        method: "POST",
        headers: { ...adminHeaders, "Content-Type": "application/json", Prefer: "return=minimal" },
        body: JSON.stringify({
          admin_user_id: user.id, title, body, audience: "topic:" + topic,
          total_devices: 0, sent_count: 1, failed_count: 0,
          delivery_details: [{ type: "topic", topic, fcm_message_name: messageId }],
        }),
      });
      return json({ success: true, topic, fcm_message_name: messageId, history_saved: history.ok });
    }
    return json({ error: "Unknown action." }, 400);
  } catch (error) {
    console.error("Topic function error:", error);
    return json({ error: "Topic operation failed on the server." }, 500);
  }
});
