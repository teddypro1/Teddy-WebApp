alter table public.device_tokens
    add column if not exists installation_id uuid;

update public.device_tokens
set installation_id = gen_random_uuid()
where installation_id is null;

alter table public.device_tokens
    alter column installation_id set default gen_random_uuid(),
    alter column installation_id set not null;

create unique index if not exists device_tokens_installation_id_key
    on public.device_tokens (installation_id);

comment on column public.device_tokens.installation_id is
    'Stable per-app-installation UUID used to safely reassign the installation FCM token to the currently authenticated account.';
