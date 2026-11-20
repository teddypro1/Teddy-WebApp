import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { importPKCS8, SignJWT } from "npm:jose@5.9.6";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

async function readJson(req: Request) {
  try { return await req.json(); } catch { return null; }
}

type DeviceRow = {
  id: string;
  user_id: string;
  fcm_token: string;
  platform: string;
  installation_id: string;
};

type DeliveryResult = {
  user_id: string;
  token_suffix: string;
  success: boolean;
  http_status: number;
  error_code: string | null;
  error_message: string | null;
  fcm_message_name: string | null;
  attempts: number;
  stale_token_removed: boolean;
};

function getFcmError(body: any, httpStatus: number) {
  const error = body?.error ?? {};
  const details = Array.isArray(error.details) ? error.details : [];
  const fcmDetail = details.find((item: any) =>
    item?.["@type"] === "type.googleapis.com/google.firebase.fcm.v1.FcmError"
  );
  const badRequest = details.find((item: any) =>
    item?.["@type"] === "type.googleapis.com/google.rpc.BadRequest"
  );
  let code = typeof fcmDetail?.errorCode === "string"
    ? fcmDetail.errorCode
    : typeof error.status === "string" ? error.status : null;
  if (!code) {
    if (httpStatus === 401) code = "UNAUTHENTICATED";
    else if (httpStatus === 403) code = "PERMISSION_DENIED";
    else if (httpStatus === 404) code = "NOT_FOUND";
    else if (httpStatus === 429) code = "QUOTA_EXCEEDED";
    else if (httpStatus >= 500) code = "FCM_SERVER_ERROR";
    else if (httpStatus === 400) code = "INVALID_ARGUMENT";
  }
  const fieldHint = badRequest?.fieldViolations?.[0]?.description;
  const message = typeof error.message === "string"
    ? error.message
    : typeof fieldHint === "string" ? fieldHint : "FCM rejected this device message.";
  return { code, message: message.slice(0, 240) };
}

function retryDelay(response: Response | null, attempt: number) {
  const retryAfter = response?.headers.get("Retry-After");
  if (retryAfter) {
    const seconds = Number(retryAfter);
    if (Number.isFinite(seconds)) return Math.min(Math.max(seconds * 1000, 250), 5000);
    const date = Date.parse(retryAfter);
    if (Number.isFinite(date)) return Math.min(Math.max(date - Date.now(), 250), 5000);
  }
  return Math.min(500 * (2 ** attempt) + Math.floor(Math.random() * 250), 3000);
}

async function sendToDevice(
  row: DeviceRow,
  projectId: string,
  accessToken: string,
  supabaseUrl: string,
  serviceRoleKey: string,
  title: string,
  body: string,
): Promise<DeliveryResult> {
  let lastStatus = 0;
  let lastCode: string | null = null;
  let lastMessage: string | null = null;
  let messageName: string | null = null;
  let attempts = 0;

  for (let attempt = 0; attempt < 3; attempt++) {
    attempts++;
    let response: Response | null = null;
    try {
      response = await fetch(
        `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            message: {
              token: row.fcm_token,
              notification: { title, body },
              data: { title, body },
              android: { priority: "HIGH", ttl: "604800s" },
            },
          }),
        },
      );
      const responseBody = await response.json().catch(() => ({}));
      lastStatus = response.status;

      if (response.ok) {
        messageName = typeof responseBody?.name === "string" ? responseBody.name : null;
        return {
          user_id: row.user_id,
          token_suffix: row.fcm_token.slice(-6),
          success: true,
          http_status: response.status,
          error_code: null,
          error_message: null,
          fcm_message_name: messageName,
          attempts,
          stale_token_removed: false,
        };
      }

      const error = getFcmError(responseBody, response.status);
      lastCode = error.code;
      lastMessage = error.message;
      const retryable = response.status === 429 || response.status === 500 || response.status === 503;
      if (!retryable || attempt === 2) break;
      await new Promise((resolve) => setTimeout(resolve, retryDelay(response, attempt)));
    } catch (error) {
      lastStatus = 0;
      lastCode = "NETWORK_ERROR";
      lastMessage = error instanceof Error ? error.message.slice(0, 240) : "Network error while contacting FCM.";
      if (attempt === 2) break;
      await new Promise((resolve) => setTimeout(resolve, retryDelay(response, attempt)));
    }
  }

  let staleTokenRemoved = false;
  // Only UNREGISTERED proves that this installation token is no longer valid.
  // Do not delete on INVALID_ARGUMENT or SENDER_ID_MISMATCH.
  if (lastCode === "UNREGISTERED") {
    try {
      const deleteResponse = await fetch(
        `${supabaseUrl}/rest/v1/device_tokens?id=eq.${encodeURIComponent(row.id)}&select=id`,
        {
          method: "DELETE",
          headers: {
            apikey: serviceRoleKey,
            Authorization: `Bearer ${serviceRoleKey}`,
            Prefer: "return=representation",
          },
        },
      );
      const deletedRows = await deleteResponse.json().catch(() => []);
      staleTokenRemoved = deleteResponse.ok &&
        Array.isArray(deletedRows) &&
        deletedRows.some((deleted: { id?: string }) => deleted.id === row.id);
      if (!staleTokenRemoved) {
        console.error("Stale FCM token cleanup failed", {
          user_id: row.user_id,
          token_suffix: row.fcm_token.slice(-6),
          http_status: deleteResponse.status,
          rows_deleted: Array.isArray(deletedRows) ? deletedRows.length : 0,
        });
      } else {
        console.log("Stale FCM token removed", {
          user_id: row.user_id,
          token_suffix: row.fcm_token.slice(-6),
        });
      }
    } catch (error) {
      console.error("Stale FCM token cleanup network error", {
        user_id: row.user_id,
        token_suffix: row.fcm_token.slice(-6),
        message: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }

  return {
    user_id: row.user_id,
    token_suffix: row.fcm_token.slice(-6),
    success: false,
    http_status: lastStatus,
    error_code: lastCode,
    error_message: lastMessage,
    fcm_message_name: null,
    attempts,
    stale_token_removed: staleTokenRemoved,
  };
}

async function mapWithConcurrency<T, R>(
  items: T[],
  concurrency: number,
  mapper: (item: T) => Promise<R>,
): Promise<R[]> {
  const results = new Array<R>(items.length);
  let nextIndex = 0;
  const workers = Array.from({ length: Math.min(concurrency, items.length) }, async () => {
    while (true) {
      const index = nextIndex++;
      if (index >= items.length) return;
      results[index] = await mapper(items[index]);
    }
  });
  await Promise.all(workers);
  return results;
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
  if (!input || typeof input !== "object" || Array.isArray(input)) {
    return json({ error: "Request body must be a JSON object." }, 400);
  }

  try {
    const userResponse = await fetch(`${supabaseUrl}/auth/v1/user`, {
      headers: { apikey: anonKey, Authorization: authorization },
    });
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
    const isAdmin = Array.isArray(adminRows) &&
      adminRows.some((row: { user_id?: string }) => row.user_id === user.id);
    const action = typeof input.action === "string" ? input.action : "send";
    if (action === "check_admin") return json({ success: true, is_admin: isAdmin });
    if (!isAdmin) return json({ error: "Administrator access required." }, 403);

    if (action === "user_directory") {
      const authUsers: any[] = [];
      const pageSize = 1000;
      for (let page = 1; page <= 100; page++) {
        const usersResponse = await fetch(
          supabaseUrl + "/auth/v1/admin/users?page=" + page + "&per_page=" + pageSize,
          { headers: { apikey: serviceRoleKey, Authorization: "Bearer " + serviceRoleKey } },
        );
        if (!usersResponse.ok) {
          console.error("User directory Auth lookup failed:", usersResponse.status);
          return json({ error: "Could not load user accounts." }, 502);
        }
        const usersData = await usersResponse.json();
        const pageUsers = Array.isArray(usersData?.users) ? usersData.users : [];
        authUsers.push(...pageUsers);
        if (pageUsers.length < pageSize) break;
      }

      const profileById = new Map<string, { full_name: string; username: string | null }>();
      const deviceCountByUser = new Map<string, number>();
      for (let index = 0; index < authUsers.length; index += 100) {
        const ids = authUsers.slice(index, index + 100)
          .map((account: any) => account?.id)
          .filter((id: unknown): id is string => typeof id === "string");
        if (ids.length === 0) continue;
        const idFilter = "in.(" + ids.join(",") + ")";
        const profileUrl = new URL(supabaseUrl + "/rest/v1/profiles");
        profileUrl.searchParams.set("select", "id,full_name,username");
        profileUrl.searchParams.set("id", idFilter);
        const profileResponse = await fetch(profileUrl.toString(), {
          headers: { apikey: serviceRoleKey, Authorization: "Bearer " + serviceRoleKey },
        });
        if (!profileResponse.ok) {
          console.error("User directory profile lookup failed:", profileResponse.status);
          return json({ error: "Could not load user profiles." }, 502);
        }
        const profiles = await profileResponse.json();
        for (const profile of Array.isArray(profiles) ? profiles : []) {
          profileById.set(profile.id, {
            full_name: typeof profile.full_name === "string" ? profile.full_name : "",
            username: typeof profile.username === "string" ? profile.username : null,
          });
        }

        const tokenUrl = new URL(supabaseUrl + "/rest/v1/device_tokens");
        tokenUrl.searchParams.set("select", "user_id");
        tokenUrl.searchParams.set("platform", "eq.android");
        tokenUrl.searchParams.set("user_id", idFilter);
        const tokenResponse = await fetch(tokenUrl.toString(), {
          headers: { apikey: serviceRoleKey, Authorization: "Bearer " + serviceRoleKey },
        });
        if (!tokenResponse.ok) {
          console.error("User directory device lookup failed:", tokenResponse.status);
          return json({ error: "Could not load registered device status." }, 502);
        }
        const tokens = await tokenResponse.json();
        for (const token of Array.isArray(tokens) ? tokens : []) {
          deviceCountByUser.set(token.user_id, (deviceCountByUser.get(token.user_id) ?? 0) + 1);
        }
      }

      const users = authUsers
        .filter((account: any) => typeof account?.id === "string" && typeof account?.email === "string")
        .map((account: any) => {
          const profile = profileById.get(account.id);
          return {
            id: account.id,
            email: account.email,
            full_name: profile?.full_name?.trim() || "Unnamed user",
            username: profile?.username ?? null,
            created_at: account.created_at ?? null,
            android_device_count: deviceCountByUser.get(account.id) ?? 0,
          };
        })
        .sort((left: any, right: any) => left.email.localeCompare(right.email));

      return json({ success: true, users, total: users.length });
    }

    if (action === "history") {
      const historyResponse = await fetch(
        `${supabaseUrl}/rest/v1/notification_history?select=id,title,body,audience,total_devices,sent_count,failed_count,delivery_details,created_at,notification_delivery_attempts(id,user_id,device_token_id,installation_id,token_suffix,success,http_status,error_code,error_message,fcm_message_name,attempts,stale_token_removed,created_at)&admin_user_id=eq.${encodeURIComponent(user.id)}&order=created_at.desc&limit=50`,
        { headers: { apikey: serviceRoleKey, Authorization: `Bearer ${serviceRoleKey}` } },
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

    const audience = input.audience === "selected_users" ? "selected_users" : "all_users";
    let recipientUserIds: string[] = [];
    const authUsers: any[] = [];
    for (let page = 1; page <= 100; page++) {
      const usersResponse = await fetch(
        `${supabaseUrl}/auth/v1/admin/users?page=${page}&per_page=1000`,
        { headers: { apikey: serviceRoleKey, Authorization: `Bearer ${serviceRoleKey}` } },
      );
      if (!usersResponse.ok) {
        console.error("Recipient account lookup failed:", usersResponse.status);
        return json({ error: "Could not load recipient accounts." }, 502);
      }
      const usersData = await usersResponse.json();
      const pageUsers = Array.isArray(usersData?.users) ? usersData.users : [];
      authUsers.push(...pageUsers);
      if (pageUsers.length < 1000) break;
    }

    if (audience === "selected_users") {
      const rawEmails = Array.isArray(input.recipient_emails) ? input.recipient_emails : [];
      const emails = [...new Set(rawEmails
        .filter((value: unknown): value is string => typeof value === "string")
        .map((value: string) => value.trim().toLowerCase())
        .filter((value: string) => value.length > 3 && value.length <= 254))];
      if (emails.length === 0 || emails.length > 100) {
        return json({ error: "Select between 1 and 100 recipient email addresses." }, 400);
      }
      const emailToId = new Map<string, string>(
        authUsers.filter((account: any) => typeof account?.email === "string" && typeof account?.id === "string")
          .map((account: any) => [account.email.toLowerCase(), account.id]),
      );
      const unknownEmails = emails.filter((email) => !emailToId.has(email));
      if (unknownEmails.length > 0) return json({ error: `Account not found: ${unknownEmails.join(", ")}` }, 404);
      recipientUserIds = [...new Set(emails.map((email) => emailToId.get(email) as string))];
    } else {
      recipientUserIds = [...new Set(authUsers.map((account: any) => account?.id)
        .filter((id: unknown): id is string => typeof id === "string"))];
    }

    if (recipientUserIds.length === 0) return json({ error: "No recipient accounts were found." }, 404);

    // Store an in-app copy for every selected account, including accounts with no FCM token.
    let inboxSaved = 0;
    for (let index = 0; index < recipientUserIds.length; index += 500) {
      const batch = recipientUserIds.slice(index, index + 500).map((recipientId) => ({
        user_id: recipientId,
        title,
        body,
      }));
      const inboxResponse = await fetch(`${supabaseUrl}/rest/v1/user_notifications`, {
        method: "POST",
        headers: {
          apikey: serviceRoleKey,
          Authorization: `Bearer ${serviceRoleKey}`,
          "Content-Type": "application/json",
          Prefer: "return=minimal",
        },
        body: JSON.stringify(batch),
      });
      if (!inboxResponse.ok) {
        console.error("In-app notification persistence failed:", inboxResponse.status);
        return json({ error: "Could not save notifications to user inboxes." }, 502);
      }
      inboxSaved += batch.length;
    }

    const tokenUrl = new URL(`${supabaseUrl}/rest/v1/device_tokens`);
    tokenUrl.searchParams.set("select", "id,fcm_token,user_id,platform,installation_id");
    tokenUrl.searchParams.set("platform", "eq.android");
    if (audience === "selected_users") {
      tokenUrl.searchParams.set("user_id", `in.(${recipientUserIds.join(",")})`);
    }
    const tokensResponse = await fetch(tokenUrl.toString(), {
      headers: { apikey: serviceRoleKey, Authorization: `Bearer ${serviceRoleKey}` },
    });
    if (!tokensResponse.ok) {
      console.error("Registered device lookup failed:", tokensResponse.status);
      return json({ error: "Could not load registered Android devices." }, 502);
    }
    const rows = await tokensResponse.json() as DeviceRow[];
    if (!Array.isArray(rows)) return json({ error: "Invalid registered device response." }, 502);

    let results: DeliveryResult[] = [];
    if (rows.length > 0) {
      const serviceAccountRaw = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON");
      if (!serviceAccountRaw) return json({ error: "Firebase server configuration is incomplete." }, 500);
      const serviceAccount = JSON.parse(serviceAccountRaw);
      const projectId = serviceAccount.project_id;
      const clientEmail = serviceAccount.client_email;
      const privateKey = serviceAccount.private_key;
      if (!projectId || !clientEmail || !privateKey) {
        return json({ error: "Firebase service account JSON is missing required fields." }, 500);
      }
      const key = await importPKCS8(privateKey, "RS256");
      const now = Math.floor(Date.now() / 1000);
      const assertion = await new SignJWT({
        scope: "https://www.googleapis.com/auth/firebase.messaging",
      }).setProtectedHeader({ alg: "RS256", typ: "JWT" })
        .setIssuer(clientEmail).setSubject(clientEmail)
        .setAudience("https://oauth2.googleapis.com/token")
        .setIssuedAt(now).setExpirationTime(now + 3600).sign(key);

      const oauthResponse = await fetch("https://oauth2.googleapis.com/token", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
          grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
          assertion,
        }),
      });
      const oauthData = await oauthResponse.json();
      if (!oauthResponse.ok || !oauthData.access_token) {
        console.error("Firebase OAuth failed:", oauthData.error ?? "unknown");
        return json({ error: "Could not obtain Firebase access token." }, 502);
      }
      results = await mapWithConcurrency(rows, 10, (row) =>
        sendToDevice(row, projectId, oauthData.access_token, supabaseUrl, serviceRoleKey, title, body)
      );
    }
    const sent = results.filter((result) => result.success).length;
    const failed = results.length - sent;
    const deliveryDetails = results.map((result) => ({
      user_id: result.user_id,
      token_suffix: result.token_suffix,
      success: result.success,
      http_status: result.http_status,
      error_code: result.error_code,
      error_message: result.error_message,
      attempts: result.attempts,
      stale_token_removed: result.stale_token_removed,
    }));

    console.log("FCM broadcast completed", JSON.stringify({
      admin_user_id: user.id,
      total: rows.length,
      sent,
      failed,
      errors: deliveryDetails.filter((item) => !item.success).map((item) => ({
        user_id: item.user_id,
        token_suffix: item.token_suffix,
        http_status: item.http_status,
        error_code: item.error_code,
        stale_token_removed: item.stale_token_removed,
      })),
    }));

    const historyResponse = await fetch(`${supabaseUrl}/rest/v1/notification_history`, {
      method: "POST",
      headers: {
        apikey: serviceRoleKey,
        Authorization: `Bearer ${serviceRoleKey}`,
        "Content-Type": "application/json",
        Prefer: "return=representation",
      },
      body: JSON.stringify({
        admin_user_id: user.id,
        title,
        body,
        audience,
        total_devices: rows.length,
        sent_count: sent,
        failed_count: failed,
        delivery_details: deliveryDetails,
      }),
    });

    let trackingSaved = false;
    if (historyResponse.ok) {
      const historyRows = await historyResponse.json().catch(() => []);
      const notificationId = Array.isArray(historyRows) ? historyRows[0]?.id : null;
      if (typeof notificationId === "string") {
        const deliveryAttempts = results.map((result, index) => ({
          notification_id: notificationId,
          user_id: result.user_id,
          device_token_id: rows[index]?.id ?? null,
          installation_id: rows[index]?.installation_id ?? null,
          token_suffix: result.token_suffix,
          success: result.success,
          http_status: result.http_status,
          error_code: result.error_code,
          error_message: result.error_message,
          fcm_message_name: result.fcm_message_name,
          attempts: result.attempts,
          stale_token_removed: result.stale_token_removed,
        }));
        if (deliveryAttempts.length === 0) {
          trackingSaved = true;
        } else {
          const trackingResponse = await fetch(`${supabaseUrl}/rest/v1/notification_delivery_attempts`, {
            method: "POST",
            headers: {
              apikey: serviceRoleKey,
              Authorization: `Bearer ${serviceRoleKey}`,
              "Content-Type": "application/json",
              Prefer: "return=minimal",
            },
            body: JSON.stringify(deliveryAttempts),
          });
          trackingSaved = trackingResponse.ok;
          if (!trackingSaved) {
            console.error("Structured notification delivery tracking insert failed:", trackingResponse.status);
          }
        }
      }
    } else {
      console.error("Notification history insert failed:", historyResponse.status);
    }

    return json({
      success: inboxSaved > 0,
      sent,
      total: rows.length,
      failed,
      history_saved: historyResponse.ok,
      tracking_saved: trackingSaved,
      inbox_saved: inboxSaved,
      results: deliveryDetails,
    });
  } catch (error) {
    console.error("Push notification function error:", error);
    return json({ error: "Unexpected server error while processing the request." }, 500);
  }
});
