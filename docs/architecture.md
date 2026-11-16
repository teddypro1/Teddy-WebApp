# Architecture

## Status

This document describes the **planned** architecture. It is not a description of an already deployed backend. The Android application currently stores notes locally.

## High-level design

```text
Android app (Jetpack Compose) ─┐
                              ├── HTTPS API (Cloudflare Workers)
Website (Cloudflare Pages) ───┘             │
                                            ├── Supabase Auth
                                            └── Supabase PostgreSQL
```

Both clients are intended to use the same backend contract and access the same user-owned data.

## Responsibilities

### Android client

- Present the native Android interface.
- Handle user interaction and client-side validation.
- Send authenticated requests to the backend.
- Never contain server-only secrets.

### Website client

- Present the browser-based interface.
- Call the same backend API as Android.
- Never contain server-only secrets.

### Cloudflare Workers

- Provide the shared HTTPS API.
- Validate requests and authentication context.
- Apply server-side business rules.
- Keep server-side secrets in Worker secret bindings.

### Supabase

- Supabase Auth manages user identity and sessions.
- PostgreSQL stores application data.
- Row Level Security (RLS) restricts access to rows owned by the authenticated user.

## Initial notes data model (proposal)

The first database design will evaluate a `notes` table with these fields:

| Column | Proposed type | Purpose |
|---|---|---|
| id | uuid | Unique note identifier |
| user_id | uuid | Owner; references the authenticated user |
| title | text | Note title |
| content | text | Note body |
| is_pinned | boolean | Pin state |
| created_at | timestamptz | Creation timestamp |
| updated_at | timestamptz | Last update timestamp |

This is a proposal only. Confirm constraints, indexes, defaults, and RLS policies before creating the production table.

## Security requirements

- Enable RLS on user data tables.
- Restrict reads, inserts, updates, and deletes to the authenticated owner.
- Do not ship the Supabase service-role key in Android or website code.
- Store server-only credentials as Cloudflare Worker secrets.
- Use HTTPS for all remote communication.
- Validate input and handle expired or missing sessions safely.

## Implementation sequence

1. Finalize the database schema and ownership rules.
2. Configure Supabase Auth and RLS.
3. Define and implement the Worker API.
4. Integrate Android without changing the frozen UI unnecessarily.
5. Build the website client.
6. Test cross-client synchronization and authorization boundaries.
