# Teddy WebApp

A personal workspace application currently implemented as an Android app, with notes, profile management, cloud synchronization, and offline support.

> **Current status:** Android application with Supabase authentication and cloud-backed notes is implemented. The separate website and Cloudflare Workers API are still planned and have not been implemented.

## Project overview

Teddy WebApp is intended to provide a personal workspace across Android and the web. The Android client is currently the active application. It connects directly to Supabase for authentication, database operations, profile data, and realtime updates.

The website and a shared Cloudflare Workers API remain future work; they are not part of the current running architecture.

## Android application

### Technology

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Design system:** Material 3
- **Backend services:** Supabase Auth and Supabase PostgreSQL
- **Networking:** OkHttp
- **Profile images:** Coil Compose
- **Local persistence:** SharedPreferences
- **Session protection:** Android Keystore-backed AES-GCM encryption

### Implemented features

#### Notes and workspace
- Create and edit notes
- Pin and unpin notes
- Search notes by title and content
- Filter pinned notes
- Sort notes by latest or oldest update
- View note statistics
- Delete notes to Trash
- Restore notes from Trash
- Permanently delete notes with confirmation
- Light and dark themes
- Home, Trash, Profile, and Settings navigation

#### Accounts and profiles
- Email/password sign-up and sign-in through Supabase Auth
- Restore an existing authenticated session
- Refresh expired access tokens and retry eligible unauthorized requests
- Sign out and clear account-specific visible state
- View and edit profile information
- Profile name, username, bio, and avatar URL

#### Cloud synchronization and offline support
- Per-account local note storage
- Queue local note changes while offline
- Synchronize pending changes when validated internet connectivity returns
- Realtime note updates through Supabase Realtime
- Profile refresh when relevant realtime updates arrive
- Conflict detection for concurrent edits, with separate conflict copies
- Restore a conflict copy as a new note
- Preserve local account data between sign-outs while keeping accounts isolated

### Important implementation notes

- Local note data is scoped by authenticated user ID. One account's local notes must not be displayed or uploaded as another account's data.
- Supabase Row Level Security (RLS) is used to restrict user-owned records.
- The Android application must use only the Supabase publishable/anon client key. Never embed a Supabase service-role key in the app.
- Session credentials are protected using Android Keystore-backed encryption.
- The app currently communicates with Supabase directly. A Cloudflare Workers API is not currently in the request path.

## Android source structure

Main source root: `app/src/main/kotlin/com.teddy.bpositive/`

```text
com.teddy.bpositive/
├── MainActivity.kt
├── data/
│   ├── LocalNotesStorage.kt
│   ├── NoteSyncRepository.kt
│   ├── NoteSyncStore.kt
│   ├── SupabaseAuth.kt
│   ├── SupabaseAuthRepository.kt
│   ├── SupabaseClient.kt
│   ├── SupabaseConfig.kt
│   ├── SupabaseNotesApi.kt
│   ├── SupabaseProfilesApi.kt
│   ├── SupabaseRealtimeSubscription.kt
│   └── SupabaseSessionManager.kt
├── model/
│   └── Note.kt
├── ui/
│   ├── components/
│   │   ├── AppBottomNavigationBar.kt
│   │   ├── NoteComponents.kt
│   │   └── WorkspaceTopBar.kt
│   ├── dialogs/
│   │   ├── NoteDeletionDialogs.kt
│   │   └── ProfileEditorDialog.kt
│   ├── screens/
│   │   ├── AuthScreen.kt
│   │   ├── HomeScreen.kt
│   │   ├── ProfileScreen.kt
│   │   └── WorkspaceScreens.kt
│   └── theme/
│       └── TeddyAppTheme.kt
└── utils/
    ├── DateTimeUtils.kt
    ├── NetworkUtils.kt
    └── NoteUtils.kt
```

### Package responsibilities

| Package | Responsibility |
|---|---|
| `data/` | Local storage, Supabase APIs, authentication, session handling, sync queue, conflict handling, and realtime subscription |
| `model/` | Core note data model |
| `ui/components/` | Reusable UI components and workspace navigation/top bar |
| `ui/dialogs/` | Note deletion and profile editing dialogs |
| `ui/screens/` | Authentication, Home, Profile, Trash, and Settings screens |
| `ui/theme/` | Material 3 light and dark theme |
| `utils/` | Date/time, network, filtering, sorting, and note transformation helpers |

`MainActivity.kt` remains the Android entry point and coordinates app-level state and screen composition. The codebase has been separated into focused packages, but it is not yet a fully ViewModel-based architecture.

## Supabase data and access control

The Android app uses Supabase Auth and PostgreSQL. The implemented data model includes:

- `notes`: user-owned notes, client IDs, timestamps, pin state, and soft-delete state
- `profiles`: user profile information
- `note_conflicts`: preserved copies of conflicting note edits

Row Level Security policies are used to scope user data to the authenticated user. Database schema, indexes, triggers, and policies are managed in the Supabase project.

The notes synchronization flow uses a per-user local queue and client identifiers to reconcile local and cloud records. Realtime events are filtered by the active user's ID.

## Planned work

The following are future directions, not current implemented features:

1. Build the separate web application.
2. Decide whether a Cloudflare Workers API is needed for the shared Android/web backend contract.
3. Document and test a stable cross-platform API contract if the Worker layer is introduced.
4. Expand automated testing for synchronization, conflict handling, session lifecycle, and account isolation.
5. Consider a ViewModel-based state-management layer as the application grows.

## Security principles

- Never commit passwords, access tokens, private keys, or environment files.
- Never include a Supabase service-role key in Android or browser code.
- Keep Row Level Security enabled for user-owned data.
- Validate the authenticated user before accessing account-specific data.
- Keep session handling and local account data isolated by user.
- Use HTTPS for remote service communication.

## Repository layout

```text
Teddy-WebApp/
├── app/                 # Android application
├── backend/             # Planning notes for a possible Worker API
├── docs/                # Architecture documentation
├── web/                 # Website planning notes; app not implemented
├── .gitignore
└── README.md
```

## Development workflow

1. Open the Android project in CodeAssist or a compatible Android development environment.
2. Pull the latest changes from the `main` branch.
3. Sync Gradle dependencies.
4. Build and test the Android application.
5. Make related source changes together in focused commits.
6. Update this README when implemented features, architecture, or repository structure materially change.

## License

No license has been selected yet. All rights are reserved by the repository owner unless a license is added.
