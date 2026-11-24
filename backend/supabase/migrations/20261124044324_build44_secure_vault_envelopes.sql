-- Build 44: store only the client-encrypted Secure Vault envelope for cross-device recovery.
-- Passwords, Recovery Keys and plaintext master keys are never stored in this table.

create table public.vault_envelopes (
  user_id uuid primary key references auth.users(id) on delete cascade,
  envelope jsonb not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.vault_envelopes enable row level security;

-- Existing project defaults revoke client grants; expose only the required operations.
grant select, insert on table public.vault_envelopes to authenticated;

create policy "Users can view their own Vault envelope"
  on public.vault_envelopes
  for select to authenticated
  using ((select auth.uid()) = user_id);

create policy "Users can create their own Vault envelope"
  on public.vault_envelopes
  for insert to authenticated
  with check ((select auth.uid()) = user_id);
