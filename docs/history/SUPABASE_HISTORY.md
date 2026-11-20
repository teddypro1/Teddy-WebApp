# Teddy-WebApp — Supabase Migration & Security History

Last verified against the live Supabase migration ledger: 2026-11-20. Project ref: `axklmzuepqcbxsbxvtdn`. Project status at verification: `ACTIVE_HEALTHY`; PostgreSQL 17.11.0.019.

This file distinguishes the live migration ledger from historical implementation notes. Migration presence in the ledger means Supabase reports it applied; it does not replace functional testing.

## Applied migration ledger (live)

| Version | Migration |
|---|---|
| 20261118100817 | `harden_public_function_execute_permissions` |
| 20261118100836 | `optimize_rls_and_index_conflicts` |
| 20261118142154 | `create_device_tokens_for_fcm` |
| 20261118153414 | `build1_single_admin_and_notification_history` |
| 20261118162915 | `build3_custom_push_broadcast` |
| 20261118164138 | `build3_grant_service_role_device_token_read` |
| 20261119052417 | `add_fcm_delivery_diagnostics` |
| 20261119070414 | `grant_service_role_device_token_cleanup` |
| 20261119073336 | `expand_notification_audience` |
| 20261119081406 | `grant_service_role_profiles_read` |
| 20261119160406 | `build11_installation_scoped_device_registry` |
| 20261119162214 | `build11_fix_device_registry_service_role_upsert` |
| 20261119163020 | `build12_user_notification_inbox` |
| 20261119170639 | `build13_user_topic_subscriptions` |
| 20261119170718 | `build13_allow_topic_notification_history` |
| 20261120044420 | `build14_notification_delivery_tracking` |
| 20261120061115 | `security_hardening_maintenance` |
| 20261120061216 | `default_topic_subscriptions_opt_in` |

Total reported applied migrations: **18**.

## Core notes/profile/conflict schema (historical verified state; inspect live before DDL)

- `public.notes`: user-owned notes with UUID/id, user_id, title/content, pin state, created/updated timestamps, client_id and soft-delete `deleted_at`; unique key on `(user_id, client_id)`.
- `public.profiles`: profile row keyed to auth user; full_name, username, bio, avatar_url and timestamps; case-insensitive unique username index; owner-scoped access and timestamp/profile creation triggers.
- `public.note_conflicts`: conflict copies with user_id, original_client_id, title/content, pin state, base/observed cloud timestamps, created_at and resolved_at.
- RLS was previously verified enabled on these core tables. Notification work introduced additional tables; this is not an exhaustive current schema inventory.

## Security migrations

### 20261118100817 — harden_public_function_execute_permissions
With user approval, revoked EXECUTE from PUBLIC, anon and authenticated for internal functions:
- `public.handle_new_user_profile()`
- `public.rls_auto_enable()`
- `public.set_notes_updated_at()`
- `public.set_profile_updated_at()`

### 20261118100836 — optimize_rls_and_index_conflicts
- Set `search_path` of `handle_new_user_profile()` and `set_profile_updated_at()` to `pg_catalog`.
- Rewrote eight RLS policies to use scalar subquery form `(SELECT auth.uid())`.
- Added `idx_note_conflicts_user_id` on `public.note_conflicts(user_id)`.

Historical advisor follow-up recorded the function-execute findings cleared and the new index appearing as unused INFO at that time.

## Notification backend migrations

### 18 Nov — initial notification/admin backend
- `create_device_tokens_for_fcm`: device token storage foundation.
- `build1_single_admin_and_notification_history`: single-admin and notification-history foundation.
- `build3_custom_push_broadcast`: custom push broadcast audience schema.
- `build3_grant_service_role_device_token_read`: server-side token read permissions.

### 19 Nov — delivery diagnostics and targeted audience
- `add_fcm_delivery_diagnostics`: persistent diagnostics for FCM delivery.
- `grant_service_role_device_token_cleanup`: backend cleanup permissions for stale tokens.
- `expand_notification_audience`: expanded notification targeting.
- `grant_service_role_profiles_read`: secure server-side profile directory access.

### 19 Nov — installation registry, inbox and topic preferences
- `build11_installation_scoped_device_registry`: installation-scoped device registration.
- `build11_fix_device_registry_service_role_upsert`: upsert privilege correction.
- `build12_user_notification_inbox`: per-user in-app notification inbox.
- `build13_user_topic_subscriptions`: user topic preferences.
- `build13_allow_topic_notification_history`: topic broadcast history audience support.

### 20 Nov — delivery tracking and hardening
- `build14_notification_delivery_tracking`: structured per-device delivery attempt tracking.
- `security_hardening_maintenance`: security maintenance for public grants/indexes.
- `default_topic_subscriptions_opt_in`: topic preferences default to opt-in.

## FCM delivery diagnostic note (20 Nov 2026; historical read-only check)

A recorded test showed 4 per-device attempts: 3 FCM HTTP 200 acceptances and 1 HTTP 404 due to a stale/unregistered token. Backend removed the stale token; a separate current token for that account succeeded. Three active device tokens remained, each with a successful send in the recorded test. HTTP 200 confirms FCM accepted the request, not that Android displayed the notification.

Never put emails, raw tokens, token suffixes or account-identifying delivery details in this public repository.

## Pending verification

1. Inspect live SQL definitions for `handle_new_user_profile()` and `set_profile_updated_at()`.
2. Confirm every object reference is schema-qualified where required under `search_path=pg_catalog`.
3. Perform a controlled test signup and verify automatic profile creation.
4. Regression-test authentication, profile update, notes CRUD, offline sync, realtime and conflict-copy recovery.
5. Re-run Security, Performance and Health advisors and record the dated results.
6. Before any schema change, inspect live schema and policies; obtain explicit approval for destructive production changes.

## Safety and cost constraints

- Keep the project on Supabase Free Plan unless the user explicitly approves a paid upgrade.
- Do not apply destructive production DDL without explicit user approval.
- Do not place service-role keys, passwords, access tokens or private user information in repository documentation.
