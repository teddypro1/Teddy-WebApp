# Backend services

**Status:** Supabase Edge Function provides server-authorized administrator FCM broadcasts and per-device delivery diagnostics. The separate Cloudflare Workers API and web dashboard are not implemented.

## Supabase Edge Functions

### send-push-notification
- Requires a valid Supabase Auth session and verifies the caller against the private `public.admin_users` table on the server.
- Supports `{ "action": "check_admin" }`, `{ "action": "history" }`, and administrator-only `{ "action": "send", "title": "...", "body": "..." }`.
- Broadcasts only to registered Android app installations in `public.device_tokens`; it does not require recipient accounts to be online or logged in at delivery time.
- Reports per-installation FCM HTTP status, FCM error code, masked token suffix, retry attempts, and whether a confirmed unregistered token was removed.
- Retries temporary FCM failures (HTTP 429, 500, 503) with bounded backoff.
- Automatically removes a token only for the definitive FCM `UNREGISTERED` result. It does not delete tokens for ambiguous `INVALID_ARGUMENT` or `SENDER_ID_MISMATCH` errors.
- Stores delivery diagnostics in the private `public.notification_history.delivery_details` JSONB column.
- Keeps `SUPABASE_SERVICE_ROLE_KEY` and `FIREBASE_SERVICE_ACCOUNT_JSON` exclusively in server-side secrets. Never commit either secret or place it in Android code.

## Database migrations

- `backend/supabase/migrations/build1_single_admin_and_notification_history.sql`
- `backend/supabase/migrations/20261119052417_add_fcm_delivery_diagnostics.sql`

## Android FCM client
- The signed-in installation registers its current FCM token against the active Supabase user.
- FCM token rotations are now synchronized to Supabase when a valid local session exists.
- Registration rechecks the encrypted active session before sending the token, reducing account-switch race conditions.
- Raw FCM tokens are never displayed in Admin Panel diagnostics; only the final six characters are shown.

## Future work
- User targeting and scheduling remain separate roadmap work.
- A separate web dashboard is postponed until Android admin workflows are complete.


## Admin Panel Build 5
- Notification analytics are calculated from the latest 50 history records returned by the server; metrics are explicitly labeled as a recent-history window, not lifetime totals.
- Notification history can be searched by title/body and filtered by All, Successful, or Failed.
- FCM acceptance rate measures FCM API acceptance, not end-user delivery/open/read confirmation.
- Security review confirms the Edge Function requires JWT verification, verifies the user against the private admin allow-list, and uses server-only service-role/Firebase credentials.
- `admin_users` and `notification_history` are RLS-enabled private tables with no `anon` or `authenticated` table grants; server-side service role is used by the Edge Function.
- Supabase Security Advisor currently reports the expected no-policy informational findings for those private tables and a warning that leaked-password protection is disabled. The latter is a plan-level feature; no paid upgrade was made.
- This review is a configuration/code review, not a full penetration test.
