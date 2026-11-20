-- Build 14: persist per-device FCM send outcomes in a queryable, additive table.
-- Existing notification_history.delivery_details JSON remains intact for backward compatibility.
create table if not exists public.notification_delivery_attempts (
  id uuid primary key default gen_random_uuid(),
  notification_id uuid not null references public.notification_history(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  device_token_id uuid,
  installation_id uuid,
  token_suffix text not null default '',
  success boolean not null,
  http_status integer not null default 0,
  error_code text,
  error_message text,
  fcm_message_name text,
  attempts smallint not null default 1 check (attempts between 1 and 10),
  stale_token_removed boolean not null default false,
  created_at timestamptz not null default now()
);

create index if not exists notification_delivery_attempts_notification_idx
  on public.notification_delivery_attempts (notification_id, created_at desc);
create index if not exists notification_delivery_attempts_user_created_idx
  on public.notification_delivery_attempts (user_id, created_at desc);

alter table public.notification_delivery_attempts enable row level security;
revoke all on table public.notification_delivery_attempts from public, anon, authenticated;
grant all on table public.notification_delivery_attempts to service_role;

comment on table public.notification_delivery_attempts is
  'Per-device FCM request outcomes. success means FCM accepted the send request, not that the device displayed or the user read the notification.';
comment on column public.notification_delivery_attempts.success is
  'True means FCM accepted the message request; it is not a device-delivery or read receipt.';
