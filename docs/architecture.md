# Teddy-WebApp Architecture

Last reviewed: 2026-11-20.

## Current implementation status

Teddy-WebApp is currently an Android notes and personal-workspace application. The Android client uses Kotlin, Jetpack Compose and OkHttp. It communicates directly with Supabase for supported authentication, database, profile and realtime operations.

Supabase Edge Functions provide privileged server-side workflows for push notification operations, including device-token registration, topic management and notification sending. They are not a replacement for the Android client's direct Supabase data access; they are used where privileged server-side execution is required.

The separate website client and a shared Cloudflare Workers API remain future/planned work. They are not currently part of the deployed Android app request path. This corrects the earlier proposal below that described Workers as the active shared API.

## Current high-level request paths

```text
Android app (Kotlin + Jetpack Compose)
  ├── Supabase Auth
  ├── Supabase PostgreSQL / REST APIs
  ├── Supabase Realtime
  └── Supabase Edge Functions
        ├── FCM device-token registration
        ├── FCM topic preference management
        └── privileged push notification sending

Future / not currently deployed:
Website client ──> possible shared API (Cloudflare Workers) ──> Supabase
```

## Android client

- Native Android UI built with Jetpack Compose and Material 3.
- Local note persistence and account-scoped cache.
- Supabase email/password authentication and session handling.
- Cloud notes/profile APIs, offline pending synchronization and realtime updates.
- Conflict detection and recovery for concurrent/offline note edits.
- Admin Panel, notification inbox and in-app update UI.
- Networking uses OkHttp; profile images use Coil Compose.
- Session persistence uses Android Keystore-backed AES-GCM encryption.

## Supabase responsibilities

- Auth manages user identity and sessions.
- PostgreSQL stores notes, profiles, conflict records and notification-related data.
- RLS protects user-owned records.
- Realtime publishes supported database changes.
- Edge Functions perform privileged notification workflows without embedding service-role credentials in the Android app.

The exact live schema and migration ledger must be inspected before database changes. Historical table names and migration records are summarized in `docs/history/SUPABASE_HISTORY.md`.

## Security boundaries

- Android client must use only the publishable/anon client key; never embed service-role keys.
- Service-role credentials and FCM server credentials remain server-side secrets.
- RLS must enforce ownership for user data.
- Privileged notification operations must be authenticated and authorized server-side.
- Do not rely on UI-only checks for administrator authorization.
- Preserve account isolation in local cache, sync queues and realtime subscriptions.
- Keep secrets, tokens, passwords, signing files and private account information out of Git.

## Planned work (not implemented as current production request path)

- A browser-based website client.
- A shared API layer, potentially using Cloudflare Workers, if the project later requires a common contract across Android and web.
- Cross-client web/Android synchronization through the agreed backend contract.

These are roadmap concepts, not claims about the currently deployed architecture.

## Maintenance rule

When implementation changes, update this document from verified live source and deployed configuration. Distinguish planned architecture from implemented architecture.
