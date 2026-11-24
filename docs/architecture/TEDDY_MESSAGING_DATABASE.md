# Teddy Messaging — Build 01 Database Architecture

**Status:** Security-reviewed and committed in Git; production migration pending.  
**Scope:** Additive database foundation for Phase 1 private text chat, with explicit extension points for later capabilities.

## 1. Existing-system compatibility

The live Supabase project was inspected before design. Build 01 adds five new public tables and a non-exposed helper schema. It does not alter or rename existing profile, device-token, Notes, notification or Secure Vault tables, columns, indexes, policies, functions or data.

The existing profile username case-insensitive unique index remains the source for future username discovery. The existing device-token registry remains the candidate FCM token store; Build 01 deliberately does not duplicate it.

## 2. Tables and contracts

### conversations
- UUID primary key.
- conversation_type supports direct and future group.
- Direct pair uses canonical low/high auth user UUIDs and a partial unique index; the same two users cannot create duplicate direct threads under concurrent requests.
- Group rows reserve the pair columns as NULL. Memberships, not a hard-coded two-user column set, represent participants.
- last_message_at and updated_at support conversation ordering without storing message previews.

### conversation_members
- Composite primary key (conversation_id, user_id).
- Role (owner, admin, member) and state (active, left, removed) are independent.
- No fixed two-member limit in the membership table.
- Future group membership management can extend the role/state contract without replacing the conversation model.

### messages
- UUID identity, conversation and sender foreign keys.
- Client-generated client_message_id is unique per sender for idempotent retries.
- Server-generated created_at plus UUID tie-breaker define stable cursor order.
- Text body has a 10,000-character upper bound in this first text-only contract.
- edited_at, deleted_at, and deleted_by are explicit lifecycle fields.
- Delete for Everyone nulls body and leaves a durable row/tombstone; it does not physically remove history.

### message_receipts
- One row per message and recipient, uniquely keyed by (message_id, user_id).
- Composite foreign keys ensure the receipt points to the correct conversation and a real member.
- delivered_at and read_at are separate timestamps. Read requires a delivery timestamp.
- FCM provider acceptance is not treated as Delivered.

### message_deletions
- Per-user tombstone keyed by (message_id, user_id).
- Supports Delete for Me without removing the message for other participants.
- Composite foreign keys bind the deletion to the same conversation and a real member.

## 3. Database-enforced operations

The migration defines narrow authenticated RPCs:
- create_direct_conversation(other_user_id): rejects unauthenticated/self/nonexistent targets, canonicalizes the pair, handles concurrent duplicate creation, and ensures both memberships exist.
- send_message(conversation_id, client_message_id, body): requires active membership, validates text, deduplicates retries, creates recipient receipt rows, and updates conversation ordering metadata.
- delete_message_for_everyone(message_id): only the original sender can invoke it; it preserves the row and replaces body with a deletion tombstone.

These RPCs are SECURITY DEFINER because they perform tightly scoped multi-row writes while direct client table writes are withheld. Each uses an empty search path, fully qualified object references, auth.uid() checks, revoked default/public execution, and explicit authenticated execution grants. The membership helper is kept in messaging_private, a schema not intended for Data API exposure, and its execute grant is restricted to authenticated callers.

## 4. RLS and grants

All five new public tables have RLS enabled. Authenticated clients receive only the minimum table privileges:
- Read conversations, memberships and messages only when they are active members.
- Messages hidden for the current user when a matching Delete for Me tombstone exists.
- Read receipts only within conversations they belong to.
- Insert/update only their own recipient receipt rows, never sender receipts.
- Insert and read only their own deletion tombstones.
- No direct client insert/update/delete on conversations or messages. Conversation creation, sending and global deletion use the narrow RPCs.

No anon table grants are introduced. The migration explicitly grants required authenticated access because Supabase's 2026 Data API exposure defaults no longer automatically expose newly created public tables.

## 5. Realtime contract

- The five durable tables are added to the existing supabase_realtime publication without removing Notes or Profiles.
- Durable message/history state remains in PostgreSQL. Postgres Changes are a notification/refresh mechanism, not the sole source of truth.
- Typing and Online/Presence are ephemeral Realtime Broadcast/Presence events on messaging:conversation:<uuid> private channels; they are not written into message history.
- realtime.messages policies authorize authenticated members for Broadcast/Presence on a correctly formatted conversation topic.
- Messaging clients must join channels with private = true. The project's global Allow public access dashboard setting is intentionally not changed in this migration because it may affect existing Notes/Profile Realtime channels; assess that setting separately before changing it.

## 6. Important implementation boundaries

- Username discovery is not implemented here. The existing profile table currently has owner-only SELECT RLS; Build 03 must add a narrowly scoped public profile-search contract without exposing email, auth metadata or private fields.
- Push token schema is not duplicated. Build 14 must inspect the existing device_tokens lifecycle and server-side FCM Edge Function before reuse.
- Presence Last Seen persistence, local encrypted/outbox queue, attachment metadata/storage, group administration, E2EE key management and voice/video signaling are future builds. They must extend this model additively.
- The initial schema is not E2EE. Database administrators/service-role processes can access plaintext message bodies.

## 7. Deployment and verification gate

This migration is security-reviewed and committed for the user's approved application. It has not yet been applied to Supabase. Before production application:
1. Review the exact SQL and confirm the migration.
2. The live project reports 19 applied migrations while the checked-in directory contains only a subset. Do not reset, rewrite or fabricate historical records. Apply this single reviewed additive migration through the Supabase migration action, then verify its new history entry and schema.
3. Confirm the project's Realtime `Allow public access` setting. RLS policies alone do not enforce private-only channels while public access is enabled. Do not change the global setting without assessing existing Notes/Profile channels and explicit approval; until disabled, do not claim Messaging Broadcast/Presence privacy is enforced.
4. Verify all five tables, indexes, foreign keys, RLS policies, grants, RPC privileges, Realtime publication entries and Realtime channel policies.
5. Run security and performance advisors, then test as two real authenticated users: direct conversation deduplication, cross-user isolation, idempotent send, receipt ownership, per-user deletion and sender-only global deletion.
6. Confirm existing Notes, Profile Realtime, notification and Secure Vault workflows remain intact.

No existing production rows are copied, transformed or deleted by this migration.
