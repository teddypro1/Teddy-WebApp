-- Teddy Messaging — Build 01
-- Additive future-ready private messaging database foundation.
-- Does not alter Notes, profiles, device_tokens, notifications or Vault data.
-- Security-reviewed additive migration for the approved Build 01 scope.

begin;

create schema if not exists messaging_private;
revoke all on schema messaging_private from public, anon;
grant usage on schema messaging_private to authenticated;

create table public.conversations (
    id uuid primary key default gen_random_uuid(),
    conversation_type text not null default 'direct'
        check (conversation_type in ('direct', 'group')),
    direct_user_low_id uuid references auth.users(id) on delete restrict,
    direct_user_high_id uuid references auth.users(id) on delete restrict,
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    last_message_at timestamptz,
    constraint conversations_direct_pair_shape check (
        (conversation_type = 'direct'
            and direct_user_low_id is not null
            and direct_user_high_id is not null
            and direct_user_low_id < direct_user_high_id)
        or
        (conversation_type = 'group'
            and direct_user_low_id is null
            and direct_user_high_id is null)
    )
);

create unique index conversations_direct_pair_unique
    on public.conversations (direct_user_low_id, direct_user_high_id)
    where conversation_type = 'direct';

create index conversations_last_message_idx
    on public.conversations (last_message_at desc nulls last, id desc);

create table public.conversation_members (
    conversation_id uuid not null
        references public.conversations(id) on delete restrict,
    user_id uuid not null references auth.users(id) on delete restrict,
    membership_role text not null default 'member'
        check (membership_role in ('owner', 'admin', 'member')),
    membership_state text not null default 'active'
        check (membership_state in ('active', 'left', 'removed')),
    joined_at timestamptz not null default now(),
    left_at timestamptz,
    primary key (conversation_id, user_id),
    constraint conversation_members_left_state check (
        (membership_state = 'active' and left_at is null)
        or (membership_state in ('left', 'removed') and left_at is not null)
    )
);

create index conversation_members_user_active_idx
    on public.conversation_members (user_id, conversation_id)
    where membership_state = 'active';

create table public.messages (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid not null
        references public.conversations(id) on delete restrict,
    sender_id uuid not null references auth.users(id) on delete restrict,
    client_message_id uuid not null,
    body text,
    created_at timestamptz not null default now(),
    edited_at timestamptz,
    deleted_at timestamptz,
    deleted_by uuid references auth.users(id) on delete restrict,
    constraint messages_body_length check (
        body is null or char_length(body) <= 10000
    ),
    constraint messages_delete_state check (
        (deleted_at is null and deleted_by is null and body is not null)
        or
        (deleted_at is not null and deleted_by is not null and body is null)
    ),
    constraint messages_id_conversation_unique unique (id, conversation_id),
    constraint messages_sender_membership_fkey
        foreign key (conversation_id, sender_id)
        references public.conversation_members(conversation_id, user_id) on delete restrict,
    constraint messages_sender_client_id_unique unique (sender_id, client_message_id)
);

create index messages_conversation_cursor_idx
    on public.messages (conversation_id, created_at desc, id desc);

create index messages_sender_created_idx
    on public.messages (sender_id, created_at desc);

create table public.message_receipts (
    message_id uuid not null,
    conversation_id uuid not null,
    user_id uuid not null,
    delivered_at timestamptz,
    read_at timestamptz,
    primary key (message_id, user_id),
    constraint message_receipts_message_conversation_fkey
        foreign key (message_id, conversation_id)
        references public.messages(id, conversation_id) on delete restrict,
    constraint message_receipts_member_fkey
        foreign key (conversation_id, user_id)
        references public.conversation_members(conversation_id, user_id) on delete restrict,
    constraint message_receipts_read_requires_delivery check (
        read_at is null or delivered_at is not null
    )
);

create index message_receipts_user_unread_idx
    on public.message_receipts (user_id, message_id)
    where read_at is null;

create index message_receipts_conversation_idx
    on public.message_receipts (conversation_id, user_id, message_id);

create table public.message_deletions (
    message_id uuid not null,
    conversation_id uuid not null,
    user_id uuid not null references auth.users(id) on delete restrict,
    deleted_at timestamptz not null default now(),
    primary key (message_id, user_id),
    constraint message_deletions_message_conversation_fkey
        foreign key (message_id, conversation_id)
        references public.messages(id, conversation_id) on delete restrict,
    constraint message_deletions_member_fkey
        foreign key (conversation_id, user_id)
        references public.conversation_members(conversation_id, user_id) on delete restrict
);

create index message_deletions_user_idx
    on public.message_deletions (user_id, message_id);

-- Derive identity exclusively from the verified JWT; callers cannot probe
-- membership for arbitrary user UUIDs.
create or replace function messaging_private.is_conversation_member(
    p_conversation_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
    select exists (
        select 1
        from public.conversation_members cm
        where cm.conversation_id = p_conversation_id
          and cm.user_id = (select auth.uid())
          and cm.membership_state = 'active'
    );
$function$;

revoke all on function messaging_private.is_conversation_member(uuid)
    from public, anon;
grant execute on function messaging_private.is_conversation_member(uuid)
    to authenticated;

alter table public.conversations enable row level security;
alter table public.conversation_members enable row level security;
alter table public.messages enable row level security;
alter table public.message_receipts enable row level security;
alter table public.message_deletions enable row level security;

create policy messaging_conversations_member_select
    on public.conversations
    for select to authenticated
    using (
        messaging_private.is_conversation_member(id)
    );

create policy messaging_members_member_select
    on public.conversation_members
    for select to authenticated
    using (
        messaging_private.is_conversation_member(conversation_id)
    );

create policy messaging_messages_member_select
    on public.messages
    for select to authenticated
    using (
        messaging_private.is_conversation_member(conversation_id)
        and not exists (
            select 1
            from public.message_deletions md
            where md.message_id = id
              and md.user_id = (select auth.uid())
        )
    );

create policy messaging_receipts_member_select
    on public.message_receipts
    for select to authenticated
    using (
        messaging_private.is_conversation_member(conversation_id)
    );

create policy messaging_receipts_recipient_insert
    on public.message_receipts
    for insert to authenticated
    with check (
        user_id = (select auth.uid())
        and messaging_private.is_conversation_member(conversation_id)
        and exists (
            select 1
            from public.messages m
            where m.id = message_id
              and m.conversation_id = message_receipts.conversation_id
              and m.sender_id <> (select auth.uid())
        )
    );

create policy messaging_receipts_recipient_update
    on public.message_receipts
    for update to authenticated
    using (
        user_id = (select auth.uid())
        and messaging_private.is_conversation_member(conversation_id)
        and exists (
            select 1
            from public.messages m
            where m.id = message_id
              and m.conversation_id = message_receipts.conversation_id
              and m.sender_id <> (select auth.uid())
        )
    )
    with check (
        user_id = (select auth.uid())
        and messaging_private.is_conversation_member(conversation_id)
        and exists (
            select 1
            from public.messages m
            where m.id = message_id
              and m.conversation_id = message_receipts.conversation_id
              and m.sender_id <> (select auth.uid())
        )
    );

create policy messaging_deletions_recipient_select
    on public.message_deletions
    for select to authenticated
    using (user_id = (select auth.uid()));

create policy messaging_deletions_own_insert
    on public.message_deletions
    for insert to authenticated
    with check (
        user_id = (select auth.uid())
        and messaging_private.is_conversation_member(conversation_id)
    );

grant select on public.conversations to authenticated;
grant select on public.conversation_members to authenticated;
grant select on public.messages to authenticated;
grant select, insert on public.message_receipts to authenticated;
grant update (delivered_at, read_at) on public.message_receipts to authenticated;
grant select, insert on public.message_deletions to authenticated;

create or replace function public.create_direct_conversation(p_other_user_id uuid)
returns uuid
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_user_id uuid := auth.uid();
    v_low_id uuid;
    v_high_id uuid;
    v_conversation_id uuid;
begin
    if v_user_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;
    if p_other_user_id is null or p_other_user_id = v_user_id then
        raise exception 'A different registered user is required' using errcode = '22023';
    end if;
    if not exists (select 1 from auth.users u where u.id = p_other_user_id) then
        raise exception 'Target user does not exist' using errcode = '23503';
    end if;

    v_low_id := least(v_user_id, p_other_user_id);
    v_high_id := greatest(v_user_id, p_other_user_id);

    insert into public.conversations (
        conversation_type, direct_user_low_id, direct_user_high_id, created_by
    )
    values ('direct', v_low_id, v_high_id, v_user_id)
    on conflict (direct_user_low_id, direct_user_high_id)
        where conversation_type = 'direct'
    do nothing;

    select c.id into v_conversation_id
      from public.conversations c
     where c.conversation_type = 'direct'
       and c.direct_user_low_id = v_low_id
       and c.direct_user_high_id = v_high_id;

    if v_conversation_id is null then
        raise exception 'Unable to create or load direct conversation';
    end if;

    insert into public.conversation_members (
        conversation_id, user_id, membership_role, membership_state, joined_at, left_at
    )
    values
        (v_conversation_id, v_low_id, 'member', 'active', now(), null),
        (v_conversation_id, v_high_id, 'member', 'active', now(), null)
    on conflict (conversation_id, user_id)
    do update set
        membership_state = 'active',
        joined_at = case
            when public.conversation_members.membership_state <> 'active'
                then now()
            else public.conversation_members.joined_at
        end,
        left_at = null;

    return v_conversation_id;
end;
$function$;

revoke all on function public.create_direct_conversation(uuid)
    from public, anon;
grant execute on function public.create_direct_conversation(uuid)
    to authenticated;

create or replace function public.send_message(
    p_conversation_id uuid,
    p_client_message_id uuid,
    p_body text
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_user_id uuid := auth.uid();
    v_body text := btrim(coalesce(p_body, ''));
    v_message_id uuid;
    v_created_at timestamptz;
    v_existing_body text;
    v_existing_conversation_id uuid;
begin
    if v_user_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;
    if p_conversation_id is null or p_client_message_id is null then
        raise exception 'Conversation and client message IDs are required' using errcode = '22023';
    end if;
    if char_length(v_body) < 1 or char_length(v_body) > 10000 then
        raise exception 'Message must contain 1 to 10000 characters' using errcode = '22023';
    end if;
    if not messaging_private.is_conversation_member(p_conversation_id) then
        raise exception 'Active conversation membership required' using errcode = '42501';
    end if;

    insert into public.messages (
        conversation_id, sender_id, client_message_id, body
    )
    values (p_conversation_id, v_user_id, p_client_message_id, v_body)
    on conflict (sender_id, client_message_id) do nothing
    returning id, created_at into v_message_id, v_created_at;

    if v_message_id is null then
        select m.id, m.body, m.created_at, m.conversation_id
          into v_message_id, v_existing_body, v_created_at, v_existing_conversation_id
          from public.messages m
         where m.sender_id = v_user_id
           and m.client_message_id = p_client_message_id;

        if v_message_id is null
           or v_existing_conversation_id <> p_conversation_id
           or v_existing_body is distinct from v_body then
            raise exception 'Idempotency key was already used with a different payload'
                using errcode = '23505';
        end if;
    else
        insert into public.message_receipts (message_id, conversation_id, user_id)
        select v_message_id, p_conversation_id, cm.user_id
          from public.conversation_members cm
         where cm.conversation_id = p_conversation_id
           and cm.membership_state = 'active'
           and cm.user_id <> v_user_id
        on conflict (message_id, user_id) do nothing;
    end if;

    update public.conversations
       set last_message_at = greatest(coalesce(last_message_at, v_created_at), v_created_at),
           updated_at = now()
     where id = p_conversation_id;

    return v_message_id;
end;
$function$;

revoke all on function public.send_message(uuid, uuid, text)
    from public, anon;
grant execute on function public.send_message(uuid, uuid, text)
    to authenticated;

create or replace function public.delete_message_for_everyone(p_message_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_user_id uuid := auth.uid();
begin
    if v_user_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    update public.messages
       set body = null, deleted_at = now(), deleted_by = v_user_id
     where id = p_message_id
       and sender_id = v_user_id
       and deleted_at is null;

    if not found then
        raise exception 'Message not found or deletion is not authorized'
            using errcode = '42501';
    end if;
end;
$function$;

revoke all on function public.delete_message_for_everyone(uuid)
    from public, anon;
grant execute on function public.delete_message_for_everyone(uuid)
    to authenticated;

create policy messaging_private_channel_receive
    on realtime.messages
    for select to authenticated
    using (
        extension in ('broadcast', 'presence')
        and case
            when (select realtime.topic()) ~
                '^messaging:conversation:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
            then messaging_private.is_conversation_member(
                split_part((select realtime.topic()), ':', 3)::uuid
            )
            else false
        end
    );

create policy messaging_private_channel_send
    on realtime.messages
    for insert to authenticated
    with check (
        extension in ('broadcast', 'presence')
        and case
            when (select realtime.topic()) ~
                '^messaging:conversation:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
            then messaging_private.is_conversation_member(
                split_part((select realtime.topic()), ':', 3)::uuid
            )
            else false
        end
    );

do $publication$
declare
    v_table text;
begin
    foreach v_table in array array[
        'conversations', 'conversation_members', 'messages',
        'message_receipts', 'message_deletions'
    ]
    loop
        if not exists (
            select 1 from pg_publication_tables
             where pubname = 'supabase_realtime'
               and schemaname = 'public'
               and tablename = v_table
        ) then
            execute format('alter publication supabase_realtime add table public.%I', v_table);
        end if;
    end loop;
end;
$publication$;

comment on table public.conversations is
    'Messaging conversation container. Direct pairs are unique; group conversations are reserved for a future phase.';
comment on table public.conversation_members is
    'Conversation participants, membership state and future group roles.';
comment on table public.messages is
    'Durable message records. Client writes must use public.send_message; ordering uses created_at plus id.';
comment on table public.message_receipts is
    'Per-recipient delivery/read acknowledgements; FCM acceptance is not delivery.';
comment on table public.message_deletions is
    'Per-user Delete for Me tombstones; independent of global message deletion.';

commit;
