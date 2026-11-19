alter table public.notification_history
  drop constraint if exists notification_history_audience_check;

alter table public.notification_history
  add constraint notification_history_audience_check
  check (audience = any (array['admin_devices'::text, 'all_users'::text, 'selected_users'::text]));
