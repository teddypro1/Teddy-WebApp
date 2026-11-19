-- Build 4: retain safe, per-device FCM delivery diagnostics in private admin history.
-- This is additive; existing notification history rows remain valid.
alter table public.notification_history
  add column if not exists delivery_details jsonb not null default '[]'::jsonb;
