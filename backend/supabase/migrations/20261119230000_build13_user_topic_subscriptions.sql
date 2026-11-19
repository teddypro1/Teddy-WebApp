create table if not exists public.user_topic_subscriptions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  topic text not null check (topic in ('all_users','announcements','updates')),
  subscribed boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, topic)
);
create index if not exists user_topic_subscriptions_user_idx
  on public.user_topic_subscriptions(user_id);
alter table public.user_topic_subscriptions enable row level security;
grant select on public.user_topic_subscriptions to authenticated;
drop policy if exists "Users can read their own topic preferences" on public.user_topic_subscriptions;
create policy "Users can read their own topic preferences"
  on public.user_topic_subscriptions for select to authenticated
  using ((select auth.uid()) = user_id);
grant select, insert, update, delete on public.user_topic_subscriptions to service_role;
