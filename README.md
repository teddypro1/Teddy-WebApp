# Teddy WebApp

A lightweight full-stack personal workspace application.

> **Current status:** Android UI prototype (v1.0). The current Android app stores notes locally on the device. Cloud synchronization, authentication, the backend API, and the website are planned—not implemented yet.

## Project overview

Teddy WebApp is being developed as a personal workspace that will eventually be accessible from both an Android application and a website, using a shared backend and database.

## Current Android application

The Android project is built with Kotlin and Jetpack Compose.

### Current features

- Create and edit notes
- Pin and unpin notes
- Search notes by title or content
- Filter pinned notes
- Sort notes by most recently updated or oldest updated
- View total and pinned note counts
- Switch between light and dark themes
- Delete notes with confirmation
- Persist notes and the theme preference locally on the device

### Current implementation

- **Application ID / package:** `com.teddy.bpositive`
- **Main screen implementation:** `app/src/main/kotlin/com/teddy/bpositive/MainActivity.kt`
- **UI toolkit:** Jetpack Compose with Material 3
- **Local note storage:** SharedPreferences with JSON serialization

## Planned architecture

The intended architecture is:

| Layer | Planned technology | Responsibility |
|---|---|---|
| Android app | Kotlin, Jetpack Compose | Native mobile experience |
| Website | HTML, CSS, JavaScript | Browser-based experience |
| Backend API | Cloudflare Workers | Shared API and server-side logic |
| Database | Supabase PostgreSQL | Persistent application data |
| Authentication | Supabase Auth | User accounts and sessions |
| Web hosting | Cloudflare Pages | Website deployment |
| Source control | GitHub | Version history and collaboration |

The Android app and website are intended to use the same backend and user data. The final design of the API and authentication flow will be documented before implementation.

## Development roadmap

1. Establish the repository structure and project documentation.
2. Design the Supabase database schema and Row Level Security (RLS) policies.
3. Implement user authentication.
4. Integrate the Android app with the backend while preserving the existing UI.
5. Build the Cloudflare Workers API.
6. Develop and deploy the website using Cloudflare Pages.
7. Test synchronization, access control, error handling, and security.

Roadmap items are goals, not claims that those features are already available.

## Security principles

- Never commit passwords, API secrets, private keys, or environment files.
- Never place a Supabase service-role key in the Android app or browser code.
- Enforce user-level data access with database Row Level Security policies.
- Keep server-only secrets in the appropriate server-side secret store.

## Repository structure

```text
Teddy-WebApp/
├── app/                         # Existing Android application
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/com/teddy/bpositive/
│   │       │   └── MainActivity.kt
│   │       └── res/
│   ├── module.toml
│   └── proguard-rules.pro
├── backend/
│   └── README.md                # Planned Worker API; not implemented
├── docs/
│   └── architecture.md          # Planned system architecture
├── web/
│   └── README.md                # Planned website; not implemented
├── .gitignore
└── README.md
```

The `backend/` and `web/` directories currently contain planning notes only. No backend service or website application has been implemented yet.

## Development notes

- The current UI is being treated as **UI v1.0** and is temporarily frozen while backend planning begins.
- Avoid breaking existing note-taking functionality when adding cloud features.
- Record meaningful changes in Git commits.
- Update this README as implementation status and project structure evolve.

## License

No license has been selected yet. All rights are reserved by the repository owner unless a license is added.
