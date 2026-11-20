-- Maintenance: harden public-schema grants and make server-only tables explicitly deny client roles.
-- No rows are deleted or modified. Existing authenticated app access is preserved by explicit grants.

-- Remove accidental anonymous access and dangerous non-row privileges.
revoke all privileges on all tables in schema public from anon;
revoke truncate, references, trigger on all tables in schema public from authenticated;

-- Keep client permissions limited to operations used by the app.
grant select, insert, update, delete on table public.device_tokens to authenticated;
grant select, insert, update, delete on table public.note_conflicts to authenticated;
grant select, insert, update, delete on table public.notes to authenticated;
grant select, insert, update on table public.profiles to authenticated;
grant select, update on table public.user_notifications to authenticated;
grant select on table public.user_topic_subscriptions to authenticated;

-- These tables are accessed only by trusted Edge Functions using service_role.
revoke all privileges on table public.admin_users from anon, authenticated;
revoke all privileges on table public.notification_history from anon, authenticated;
revoke all privileges on table public.notification_delivery_attempts from anon, authenticated;

drop policy if exists "Deny direct client access" on public.admin_users;
create policy "Deny direct client access"
  on public.admin_users for all to anon, authenticated
  using (false) with check (false);

drop policy if exists "Deny direct client access" on public.notification_history;
create policy "Deny direct client access"
  on public.notification_history for all to anon, authenticated
  using (false) with check (false);

drop policy if exists "Deny direct client access" on public.notification_delivery_attempts;
create policy "Deny direct client access"
  on public.notification_delivery_attempts for all to anon, authenticated
  using (false) with check (false);

-- Prevent unsafe default grants on future objects created by the migration owner.
alter default privileges for role postgres in schema public
  revoke all on tables from anon, authenticated;
alter default privileges for role postgres in schema public
  revoke all on sequences from anon, authenticated;
alter default privileges for role postgres in schema public
  revoke execute on functions from public, anon, authenticated;

-- Add missing indexes identified by Supabase Performance Advisor.
create index if not exists admin_users_created_by_idx
  on public.admin_users (created_by);
create index if not exists notification_history_admin_user_id_idx
  on public.notification_history (admin_user_id);

comment on policy "Deny direct client access" on public.admin_users is
  'Admin membership is read and changed only by trusted server-side code.';
