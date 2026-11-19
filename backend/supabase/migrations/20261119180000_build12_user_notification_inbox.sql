create table if not exists public.user_notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  body text not null,
  created_at timestamptz not null default now(),
  read_at timestamptz null
);
create index if not exists user_notifications_user_created_idx
  on public.user_notifications (user_id, created_at desc);
create index if not exists user_notifications_unread_idx
  on public.user_notifications (user_id, created_at desc)
  where read_at is null;
alter table public.user_notifications enable row level security;
grant select on table public.user_notifications to authenticated;
grant update (read_at) on table public.user_notifications to authenticated;
grant select, insert, update, delete on table public.user_notifications to service_role;
drop policy if exists "Users can read their own notifications" on public.user_notifications;
create policy "Users can read their own notifications"
  on public.user_notifications for select to authenticated
  using ((select auth.uid()) = user_id);
drop policy if exists "Users can mark their own notifications as read" on public.user_notifications;
create policy "Users can mark their own notifications as read"
  on public.user_notifications for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
