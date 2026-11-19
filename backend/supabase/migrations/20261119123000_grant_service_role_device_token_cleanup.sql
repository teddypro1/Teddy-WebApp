-- Allow the server-side service_role to remove only confirmed stale FCM token rows.
-- No client role receives this privilege; RLS policies remain unchanged.
grant delete on table public.device_tokens to service_role;
