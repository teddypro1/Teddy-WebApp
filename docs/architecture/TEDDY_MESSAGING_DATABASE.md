# Teddy Messaging — Build 01 Database Architecture

**Status:** Applied to Supabase; structural verification and transactional two-user smoke test passed. Full client/Realtime verification remains pending.  
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
- Messaging clients must join channels with `private = true`. The project's global `Allow public access` setting remains enabled pending compatibility verification of the existing Notes/Profile client.
- Realtime Compatibility Build adds a separate user-scoped private topic for the existing Notes/Profile Postgres Changes subscription (`notes:user:<auth.uid()>`). Its `realtime.messages` SELECT policy permits authenticated channel authorization only when the topic matches the caller's own UUID. It grants no Broadcast/Presence INSERT permission. The existing `notes` and `profiles` table RLS policies continue to control which Postgres Change rows the client can receive.

## 6. Important implementation boundaries

- Username discovery is not implemented here. The existing profile table currently has owner-only SELECT RLS; Build 03 must add a narrowly scoped public profile-search contract without exposing email, auth metadata or private fields.
- Push token schema is not duplicated. Build 14 must inspect the existing device_tokens lifecycle and server-side FCM Edge Function before reuse.
- Presence Last Seen persistence, local encrypted/outbox queue, attachment metadata/storage, group administration, E2EE key management and voice/video signaling are future builds. They must extend this model additively.
- The initial schema is not E2EE. Database administrators/service-role processes can access plaintext message bodies.

## 7. Deployment and verification gate

Build 01's database migration is applied. The Realtime Compatibility migration is also applied additively; the Android client source has been updated to join the Notes/Profile topic as a private channel. Remaining gates:
1. Verify the new migration history entry and exact policy definition.
2. Build the Android app and test Notes/Profile refresh with a real authenticated account while `Allow public access` is still enabled.
3. Test that a second authenticated account cannot join the first user's Notes/Profile private topic.
4. Regression-test notes CRUD, offline sync, conflict recovery, Profile refresh, notifications and Secure Vault.
5. Only after those tests pass, assess and explicitly authorize disabling global `Allow public access`; then re-test all private Realtime paths.
6. Run the full two-user Messaging tests before declaring Build 01 fully complete.

### Applied verification (2026-11-24)
- Supabase migration history includes `20261124110532: build01_messaging_foundation`; the prior 19 entries remain unchanged (20 total).
- All five public tables exist; RLS is enabled on all five; eight public Messaging policies and two Realtime channel policies exist.
- All five tables are in `supabase_realtime`; existing `notes` and `profiles` publication entries remain.
- The membership helper has the safe one-argument signature; the arbitrary-user signature is absent.
- Authenticated table grants match intended SELECT/receipt/deletion operations; no `anon` grants were found.
- A rollback-only SQL smoke test using two existing authenticated user identities passed: direct conversation creation, message send, recipient read, receipt creation, recipient Delete for Me isolation, sender visibility and sender-only Delete for Everyone tombstone. The transaction was rolled back; no test rows remain.
- Health advisor returned no findings. Security advisor reported three expected warnings for intentionally authenticated SECURITY DEFINER RPC endpoints and one unrelated Auth leaked-password-protection setting. Performance advisor reported seven unindexed-FK INFO findings and unused-index INFO findings; review before optimization.
- Realtime `Allow public access` dashboard setting could not be inspected with available tools. Private Broadcast/Presence enforcement remains unverified until the setting is checked and clients use `private: true`.

No existing production rows are copied, transformed or deleted by this migration.


## Build 03 — Username discovery

The profile table remains owner-readable through its original RLS policy. Username discovery is provided by the narrowly scoped authenticated RPC `public.search_public_profiles(search_query, result_limit)`, rather than broadening direct profile-table SELECT access.

The RPC:
- Requires `auth.uid()` and excludes the caller's own profile.
- Searches only non-empty usernames using case-insensitive literal substring matching.
- Requires at least two characters, caps query length at 30 and result count at 20.
- Returns only `id`, `full_name`, `username`, `bio` and `avatar_url`; email and Auth metadata are never returned.
- Uses `SECURITY DEFINER` with an empty search path and fully qualified table references. Execution is revoked from PUBLIC and anon and granted only to authenticated.

The Android client calls the RPC with the active Supabase access token. Build 03 includes username search results and a public profile preview. The Start Chat action is a handoff point only; actual direct conversation creation belongs to Build 04.
