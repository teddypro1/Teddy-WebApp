-- Build 1: server-side single-admin authorization and private notification audit history.
create table if not exists public.admin_users (
  singleton boolean primary key default true check (singleton = true),
  user_id uuid not null unique references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  created_by uuid references auth.users(id) on delete set null
);
alter table public.admin_users enable row level security;
revoke all on table public.admin_users from anon, authenticated;
grant all on table public.admin_users to service_role;

insert into public.admin_users (singleton, user_id, created_by)
select true, u.id, u.id
from auth.users as u
where lower(u.email) = lower('infoteddyahsan@gmail.com')
on conflict (singleton) do update
set user_id = excluded.user_id, created_by = excluded.created_by;

create table if not exists public.notification_history (
  id uuid primary key default gen_random_uuid(),
  admin_user_id uuid not null references public.admin_users(user_id) on delete restrict,
  title text not null,
  body text not null,
  audience text not null default 'admin_devices' check (audience in ('admin_devices')),
  total_devices integer not null default 0 check (total_devices >= 0),
  sent_count integer not null default 0 check (sent_count >= 0),
  failed_count integer not null default 0 check (failed_count >= 0),
  created_at timestamptz not null default now()
);
create index if not exists notification_history_created_at_idx
  on public.notification_history (created_at desc);
alter table public.notification_history enable row level security;
revoke all on table public.notification_history from anon, authenticated;
grant all on table public.notification_history to service_role;
