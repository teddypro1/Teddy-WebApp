-- Build: Realtime compatibility for private Notes/Profile channel.
-- Grants authenticated users channel-join read authorization only for their own
-- user-scoped topic. Postgres Changes payloads remain protected by the existing
-- notes/profiles table RLS policies. No Broadcast/Presence INSERT permission is added.

create policy notes_private_user_channel_receive
    on realtime.messages
    for select
    to authenticated
    using (
        extension in ('broadcast', 'presence')
        and (select realtime.topic()) =
            'notes:user:' || (select auth.uid())::text
    );
