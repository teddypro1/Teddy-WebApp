# Backend services

**Status:** Supabase Edge Function is deployed for administrator-only FCM test delivery. The shared Cloudflare Workers API and web dashboard are not implemented.

## Supabase Edge Functions

### send-push-notification
- Requires a valid Supabase Auth session.
- Checks the caller against the private `public.admin_users` table on the server.
- Supports `{ "action": "check_admin" }` to return the caller's administrator status.
- Sending requires administrator authorization and currently targets only the administrator's own registered Android devices (Build 1 safety scope).
- Records delivery counts in the private `public.notification_history` table.
- Uses `SUPABASE_SERVICE_ROLE_KEY` and `FIREBASE_SERVICE_ACCOUNT_JSON` only as server-side secrets. Never commit either secret or place them in Android code.

## Database migration

The Build 1 migration is stored at `backend/supabase/migrations/build1_single_admin_and_notification_history.sql`.

## Future work
- Add the Android Admin Panel and audience selection in a later build.
- Add the separate web dashboard only when Android admin workflows are complete.
- Do not implement a broadcast-to-all-users path until its audience controls and confirmation UX are designed.
