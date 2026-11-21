# Teddy-WebApp — Project Context & Progress Log

সর্বশেষ হালনাগাদ: ২০২৬-১১-২০  
Repository: https://github.com/teddypro1/Teddy-WebApp  
এই নথি AI development assistant-এর জন্য canonical project handoff। কোনো কাজ শুরুর আগে এই নথি পড়বে এবং live GitHub/Supabase state যাচাই করবে। নথির কোনো তথ্য পুরোনো হতে পারে; live state সর্বদা অগ্রাধিকার পাবে।

## 1. Project identity

- Project: Teddy-WebApp (My First App)
- Android application namespace / application ID: `com.teddy.bpositive`
- GitHub repository: `teddypro1/Teddy-WebApp`
- Sole working branch: `main`
- Supabase project ref: `axklmzuepqcbxsbxvtdn`
- Supabase URL: `https://axklmzuepqcbxsbxvtdn.supabase.co`
- Last verified Supabase status: `ACTIVE_HEALTHY`; PostgreSQL 17.11.0.019.
- User is based in Bangladesh; use Bangladesh-local date/time when reporting schedules.

## 2. User's development environment and preferences

- Android IDE: CodeAssist 3.46.0.
- Kotlin + Jetpack Compose; Java 17.
- Compile SDK 36, Target SDK 36, Min SDK 26.
- User's test phone: Vivo Y02A, Android 12, 3 GB RAM / 32 GB storage.
- Explain steps in Bengali, with precise file paths, branch names and build numbers.
- User manually fetches/pulls source in CodeAssist and builds/tests on device.
- Work on one Build/change-set at a time.
- Never say a Build succeeded until the user explicitly confirms.
- User wants all future development directly on `main`; do not create feature/release/develop branches unless the user changes this decision.
- The user deleted all other branches. Last live branch check showed only `main`.
- Never publish a GitHub Release, merge a PR, or make destructive production changes without explicit approval. The user's earlier approval to publish v1.0.1 does not grant blanket approval for future releases.

## 3. Application purpose and architecture

Teddy-WebApp is an Android notes and personal-workspace application. Major capabilities include:
- Supabase email/password authentication (email confirmation enabled).
- User profiles and profile editing.
- Local note storage and account-scoped cache.
- Offline note creation/editing and later synchronization.
- Realtime updates.
- Concurrent/offline edit conflict detection, conflict copies and restore-as-note flow.
- Pinning and Trash.
- Settings and user notification inbox.
- Admin panel and push notification management.
- In-app update checking, APK download, checksum verification and Android package installation.

Architecture notes:
- Android client communicates directly with Supabase APIs for supported data operations.
- Supabase Edge Functions are used for privileged backend workflows, including device-token registration, notification topic management and sending push notifications. Do not describe the current project as having no backend functions.
- No Cloudflare Worker is part of the currently verified app request path.
- Supabase Free Plan is the current budget constraint.

## 4. Current source layout (verify against live tree before relying on it)

Main Kotlin package: `app/src/main/kotlin/com.teddy.bpositive/`

Known important files include:
- `MainActivity.kt`
- `data/AdminRepository.kt`
- `data/LocalNotesStorage.kt`
- `data/NoteSyncRepository.kt`
- `data/NoteSyncStore.kt`
- `data/SupabaseAuth.kt`
- `data/SupabaseAuthRepository.kt`
- `data/SupabaseClient.kt`
- `data/SupabaseConfig.kt`
- `data/SupabaseNotesApi.kt`
- `data/SupabaseProfilesApi.kt`
- `data/SupabaseRealtimeSubscription.kt`
- `data/SupabaseSessionManager.kt`
- `ui/screens/AdminPanelScreen.kt`
- `ui/screens/AuthScreen.kt`
- `ui/screens/HomeScreen.kt`
- `ui/screens/NotificationInboxScreen.kt`
- `ui/screens/ProfileScreen.kt`
- `ui/screens/WorkspaceScreens.kt`
- `ui/components/AppBottomNavigationBar.kt`
- `ui/components/NoteComponents.kt`
- `ui/components/WorkspaceTopBar.kt`
- `ui/dialogs/NoteDeletionDialogs.kt`
- `ui/dialogs/ProfileEditorDialog.kt`
- `ui/theme/TeddyAppTheme.kt`
- `utils/DateTimeUtils.kt`, `NetworkUtils.kt`, `NoteUtils.kt`

Other important paths:
- `app/module.toml`: Android module configuration and versionCode/versionName.
- `update.json`: public in-app update metadata.
- `backend/supabase/functions/`: Edge Functions.
- `backend/supabase/migrations/` and `supabase/migrations/`: migration history; inspect both before database changes.
- `docs/architecture.md`: architecture notes.

## 5. Implemented app/data behaviors (historically recorded; verify live source when modifying)

- Supabase email/password authentication; email confirmation enabled.
- Android Keystore AES-GCM session persistence.
- Local note cache uses account-specific key `notes_data_<userId>`.
- User-scoped local/cloud data separation.
- Offline note create/edit and pending sync after reconnect.
- Realtime subscriptions for notes/profiles with user filtering.
- Conflict detection for concurrent/offline edits; Conflict Copies UI and Restore as Note.
- Avoid unnecessary cloud UPDATE for unchanged notes.
- Unauthorized response retry and token refresh coalescing.
- Sign-out clears visible account UI while retaining account-scoped local cache.
- Asynchronous callbacks guarded against stale/changed sessions.
- Profile editing requires verified internet connectivity.

## 6. Supabase schema and security context

Previously recorded public tables:
- `notes`
- `profiles`
- `note_conflicts`

All three were previously verified with RLS enabled. Notification features later added related tables; do not assume the above list is exhaustive. Inspect current live schema before SQL changes.

Previously recorded `notes` fields:
`id`, `created_at`, `user_id`, `title`, `content`, `is_pinned`, `updated_at`, `client_id`, `deleted_at`; unique index on `(user_id, client_id)`.

Previously recorded `profiles` fields:
`id`, `full_name`, `username`, `bio`, `avatar_url`, `created_at`, `updated_at`; case-insensitive unique username index, owner RLS, update timestamp trigger and auth-user profile creation trigger.

Previously recorded `note_conflicts` fields:
`user_id`, `original_client_id`, `title`, `content`, `is_pinned`, `base_cloud_updated_at`, `observed_cloud_updated_at`, `created_at`, `resolved_at`; owner RLS policies.

### Previously applied security migrations

With user approval:
1. `harden_public_function_execute_permissions`: revoked EXECUTE for PUBLIC/anon/authenticated on selected internal public functions:
   - `public.handle_new_user_profile()`
   - `public.rls_auto_enable()`
   - `public.set_notes_updated_at()`
   - `public.set_profile_updated_at()`
2. `optimize_rls_and_index_conflicts`:
   - Set search_path of `handle_new_user_profile()` and `set_profile_updated_at()` to `pg_catalog`.
   - Rewrote eight RLS policies to use `(SELECT auth.uid())`.
   - Added `idx_note_conflicts_user_id` on `public.note_conflicts(user_id)`.

Historical follow-up found policies/index as expected, function-execute findings cleared; performance advisor showed the new index as unused INFO at that time.

### Pending Supabase verification (do not mark complete without evidence)

1. Inspect live SQL definitions of `handle_new_user_profile()` and `set_profile_updated_at()`.
2. Verify schema-qualified references work with `search_path=pg_catalog`.
3. Perform test signup and verify automatic profile-row creation.
4. Regression-test authentication, profile update, notes CRUD, offline sync, realtime and conflict recovery.
5. Re-run Supabase Security, Performance and Health advisors.

## 7. Push notification system — implemented and tested

The repository contains Supabase Edge Functions:
- `register-device-token`
- `manage-notification-topics`
- `send-push-notification`

Relevant migration history includes:
- `20261119052417_add_fcm_delivery_diagnostics.sql`
- `20261119123000_grant_service_role_device_token_cleanup.sql`
- `20261119140000_expand_notification_audience.sql`
- `20261119143000_grant_service_role_profiles_read.sql`
- `20261119160000_build11_installation_scoped_device_registry.sql`
- `20261119170000_build11_fix_device_registry_service_role_upsert.sql`
- `20261119180000_build12_user_notification_inbox.sql`
- `20261119230000_build13_user_topic_subscriptions.sql`
- `20261119231000_build13_allow_topic_notification_history.sql`
- `20261120044420_build14_notification_delivery_tracking.sql`
- `20261120130000_security_hardening_maintenance.sql`
- `20261120133000_default_topic_subscriptions_opt_in.sql`

### Latest historical delivery check (20 Nov 2026)

A read-only query of notification history, per-device delivery attempts, auth users/profiles and active device tokens showed:
- Latest test: 4 FCM delivery attempts; 3 successful HTTP 200, 1 failed HTTP 404.
- The single failure was an old/stale FCM registration token with FCM error `UNREGISTERED` / `NotRegistered`.
- Backend marked that stale token for removal and removed it (`stale_token_removed = true`).
- The same account also had a separate current token that succeeded. Therefore the failure was a stale device token, not an account-level failure.
- Three active device tokens remained after cleanup, one per account; each current token had a successful send in the latest attempt set.
- Earlier tests on 20 Nov had 3/3 successful attempts.
- HTTP 200 means FCM accepted the send request; it does not prove the notification was displayed on the user's screen.
- Do not store user emails, raw FCM tokens, token suffixes or other personal delivery identifiers in this public project context file. Re-query Supabase delivery records if account-level diagnostics are needed.

## 8. In-app update system

### Requirements established by user
- Check for updates on every app launch, regardless of login state.
- Continue app normally if offline or update check fails.
- Optional update prompt: user can choose Later; suppress repeated prompt at most once per calendar day per version. No Skip option.
- Mandatory update when installed versionCode is below `minimumSupportedVersionCode`; block app use until update.
- Download APK inside app with progress and total size.
- Verify SHA-256 before installation.
- Use Android Package Installer.
- Host APKs on GitHub Releases.
- Manually update `update.json` after each release.
- No Supabase DB changes for update metadata.

### Build milestones
- Build 15, commit `2ec4f75f8b362a271681a272c8ec3de812bf4b73`: update checker, Settings status, metadata model/repository, baseline update.json. User confirmed successful.
- Build 16, commit `289733cbe2cecf97d94d1f1663b44d2ec7820d43`: optional/mandatory prompts and initial download action. User confirmed successful.
- Build 17, commit `0e5ef6a0ffcce78cba1aef7da46f94bd6e8e8c31`: Kotlin warning fix. User confirmed successful.
- Build 18, commit `de4cd01fda7c32b799e7652c1a27e65973a81663`: in-app APK download, progress, SHA-256 verification, Android installer and FileProvider. User confirmed successful and reported it was working.
- Build 19: production metadata URL fix. Commit on feature branch `27a12dbfa0685e4d844ad0f1659c847c8134c4a0`; merged PR #3. Main after merge was `6f5621ac454f214783028b0f00882bd13a2f0315`. Production URL changed to raw main-branch update.json:
  `https://raw.githubusercontent.com/teddypro1/Teddy-WebApp/main/update.json`.
- Build 20: version bump to `versionName = "1.0.1"`, `versionCode = 2` in `app/module.toml`. Commit `88cba51eced361403aee3a357c45a01256484f19`; PR #4 squash-merged. Merge commit `706f9d13cf94a4547899b66c790b191391520015`. User confirmed Build Successful on release branch and again after pulling/building main.
- v1.0.1 release: user published GitHub Release tag `v1.0.1`; update was physically tested from installed v1.0.0. User reported successful download, SHA-256 verification, installer prompt and successful installation/update with no issue. Screenshots showed 9.3 MB APK download and successful SHA-256 verification.
- User explicitly identified a product limitation: v1.0.0 was released before admin panel, push notifications and in-app updater existed. Existing v1.0.0 installs therefore cannot be expected to receive in-app update prompts for v1.0.1, because the updater client was absent in v1.0.0. Users installing v1.0.1 can receive future in-app updates, assuming update metadata and release remain correctly maintained.
- Existing v1.0.0 Release must remain unchanged.

### Release metadata / signing
- v1.0.0 Release URL: `https://github.com/teddypro1/Teddy-WebApp/releases/tag/v1.0.0`
- v1.0.0 APK URL: `https://github.com/teddypro1/Teddy-WebApp/releases/download/v1.0.0/app-release.apk`
- v1.0.0 SHA-256: `a4f47c0af1b3e6a13cb788d7e2f7d0c4f7e523a49b33632de5bdd65e18e08892`
- v1.0.0 asset size: 8,318,017 bytes (~8.3 MB).
- Signing keystore file: `teddywebapprelease.jks`; keystore name `TeddyWebAppRelease`; alias `teddywebapp`; 25-year validity; Organization Teddy Universe; Country BD.
- Keystore password is private. Never ask for, reveal, upload or commit keystore/password/signing secrets. Preserve same signing key for in-place upgrades.
- v1.0.1 release APK was shown downloading as 9.3 MB; do not invent its hash or exact asset size. Obtain live GitHub release asset and verify actual checksum if needed.

### update.json
Historical v1.0.0 metadata:
```json
{
  "enabled": true,
  "latestVersionName": "1.0.0",
  "latestVersionCode": 1,
  "minimumSupportedVersionCode": 1,
  "downloadUrl": "https://github.com/teddypro1/Teddy-WebApp/releases/download/v1.0.0/app-release.apk",
  "sha256": "a4f47c0af1b3e6a13cb788d7e2f7d0c4f7e523a49b33632de5bdd65e18e08892",
  "releaseNotes": "Initial release."
}
```
This is historical, not current. Always fetch live `update.json` from main before editing or stating its current contents. After publishing a release, set latestVersionName/code, downloadUrl, verified SHA-256 and release notes. Keep minimumSupportedVersionCode at 1 unless user explicitly chooses to force a mandatory update.

## 9. Build 21 — Settings manual update check improvement (USER-CONFIRMED SUCCESSFUL)

User reported:
- After tapping Later in the update prompt, Settings showed that a new update was available.
- Tapping Settings > Check for Updates appeared to do nothing.
- Button changed/faded too quickly; user wanted visible loading feedback for about 1–1.5 seconds and a clear result.

Changes committed directly to `main`:
1. Commit `15826afa4fee6cdb1078e9ab829213edb5ced50d` — `MainActivity.kt`
   - Manual update check path added via `checkForAppUpdates(manualCheck: Boolean = false)`.
   - Manual check keeps loading state visible for at least 1,200 ms.
   - When an update is available, a manual check bypasses the Later prompt suppression and opens the update dialog.
   - Automatic startup checks retain Later-per-day behavior.
2. Commit `e2e1c5d43e65c793842fd07fa691f557788de9bc` — `WorkspaceScreens.kt`
   - Added Compose AnimatedContent fade transition for Checking/Check for Updates button content.
   - Removed obsolete Settings message claiming in-app download/installation would be enabled in a future build.

Latest verified main HEAD as of 20 Nov 2026:
`e2e1c5d43e65c793842fd07fa691f557788de9bc`

**User confirmation (2026-11-20):** The user explicitly reported that Build 21 was successful and all tests were completed. Do not ask for Build 21 confirmation again. The source commits above and the user's confirmation are both recorded.

## 10. Branch and pull-request policy

User's final decision:
- Keep only `main`.
- All future builds and changes happen directly on `main`.
- No feature branch, release branch or develop branch.
- As of latest live verification, GitHub branch list contained exactly one branch: `main`, at `e2e1c5d43e65c793842fd07fa691f557788de9bc`.
- User personally deleted all other branches.
- Before every write, freshly verify current main HEAD and target file state.
- No open PR was part of the last recorded workflow; verify live if relevant.

## 11. Release history and production decisions

- v1.0.0 is the first release. It predates admin panel, push notifications and in-app update client. It should not be modified/deleted.
- v1.0.1 is the second release, versionCode 2. It includes the in-app update system and newer app capabilities. User tested in-app update from v1.0.0-capable test environment and confirmed successful installation; note that actual old v1.0.0 public users cannot receive updater prompt if v1.0.0 binary lacks updater code.
- Publishing a release and updating `update.json` are separate steps. Confirm actual APK, signing compatibility, asset URL and SHA-256 before metadata update.
- Never state future updates will work “100%” without end-to-end testing of the installed release and current metadata.

## 12. Known remaining work / next priorities

Priority A — Supabase verification:
1. Inspect live SQL function definitions and schema-qualified references with `search_path=pg_catalog`.
2. Test auth signup and automatic profile row creation.
3. Regression-test profile editing, notes CRUD, offline sync, realtime, conflict copy/restore.
4. Re-run advisors.

Priority B — operational:
1. Inspect live SQL function definitions and schema-qualified references with `search_path=pg_catalog`.
2. Test auth signup and automatic profile row creation.
3. Regression-test profile editing, notes CRUD, offline sync, realtime, conflict copy/restore.
4. Re-run advisors.

Priority C — operational:
1. Verify latest release asset and `update.json` after future releases.
2. Periodically inspect notification delivery diagnostics and stale-token cleanup.
3. Keep this document current after significant milestone; never store secrets, raw tokens, passwords or private user email addresses here.

## 13. Security, cost and data-safety rules

- Supabase Free Plan only unless user explicitly approves a paid upgrade.
- No destructive Production Database changes without explicit user approval.
- Inspect before database changes; verify after.
- Never ask for or expose passwords, keystore credentials, service-role keys, FCM tokens, private keys or other secrets.
- Never commit APK/AAB, keystore, `.env`, local.properties or signing credentials.
- Repository is public and README declares Proprietary / All Rights Reserved; public visibility does not prevent copying/forking. Do not call it open source unless a valid open-source license is added.
- Do not store personal account emails or raw push tokens in this public context file.
- Clearly distinguish verified facts, user-reported results, inferred behavior and pending tests.

## 14. Standard workflow for the assistant

Before any source change:
1. Read this context document.
2. Fetch current GitHub branch list and latest `main` commit.
3. Fetch exact source file and current blob SHA before editing.
4. Check Supabase live project status; inspect database only if relevant.
5. Make one focused change-set directly on `main`.
6. Report commit SHA, changed files and exact user-side build/test steps.
7. Wait for user's build confirmation; never assume success.
8. Update this context file after meaningful milestones, using fresh GitHub state and preserving the latest history.

For database work:
1. Inspect schema, policies, functions and migrations first.
2. Obtain explicit approval for destructive or production-impacting changes.
3. Apply only the approved minimal change.
4. Verify database state and report exact outcome.

## 15. New-chat handoff prompt

> আমরা Teddy-WebApp (My First App) Android app নিয়ে কাজ করছি। Repository-এর `PROJECT_CONTEXT.md` আগে পড়ো এবং বর্তমান live GitHub `main` ও প্রয়োজনমতো Supabase state যাচাই করো। সব কাজ শুধু `main` branch-এ হবে; অন্য branch তৈরি করবে না। একবারে একটি Build/change-set নিয়ে কাজ করবে এবং আমি Build সফল নিশ্চিত না করা পর্যন্ত সফল বলবে না। Supabase Free Plan বজায় রাখবে; আমার অনুমতি ছাড়া paid upgrade বা destructive Production Database পরিবর্তন করবে না। Secret, password, keystore বা service-role key চাইবে/প্রকাশ করবে না। বাংলায় উত্তর দেবে।


---

## 16. Documentation system and historical records

This repository uses a hybrid documentation structure. `PROJECT_CONTEXT.md` is the concise master handoff: current verified state, architecture, decisions, active work and next priorities. Detailed chronological records live under `docs/history/`; `CHANGELOG.md` records only significant user-visible or operational milestones. Avoid duplicating full implementation detail in every file.

- [CHANGELOG](../../CHANGELOG.md)
- [Build and implementation history](../history/BUILD_HISTORY_01.md)
- [Supabase migration and security history](../history/SUPABASE_HISTORY.md)
- [Release and in-app update history](../history/RELEASE_HISTORY.md)
- [Architecture](../architecture.md)

History is reconstructed from repository commits, migration records and prior user-confirmed reports available to the assistant. A commit proves that source or migration files were committed; it does not by itself prove that an Android Build was successful or that a migration was applied. User-reported device tests are labeled as such. Pending verification remains pending until fresh evidence is obtained.

## 17. Current live snapshot — verified 2026-11-20

- GitHub repository: public, not archived; default branch `main`.
- Live branch search returned exactly one branch: `main`.
- Latest main commit before this documentation update: `67d3089856858efff228b25bfd8810c893441946` (`Record Build 21 successful Build and test confirmation`).
- Master file blob SHA before this update: refreshed immediately before the documentation commit; see the resulting commit and blob SHA in the handoff report.
- Current `app/module.toml`: versionName `1.0.1`, versionCode `2`, namespace `com.teddy.bpositive`, Java 17, SDK 36/36/26.
- Live `update.json`: enabled; latest version `1.0.1` (code 2); minimum supported code 1; download points to the v1.0.1 GitHub Release APK. The live SHA-256 is recorded in `update.json`; always fetch it before future changes.
- Supabase project status: `ACTIVE_HEALTHY`; PostgreSQL `17.11.0.019`.
- Live Supabase migration ledger returned 18 applied migrations, from `20261118100817` through `20261120061216`. Exact migration names and chronology are in [SUPABASE_HISTORY.md](../history/SUPABASE_HISTORY.md).
- Build 21 manual Settings update-check improvements are committed to main. On 2026-11-20, the user explicitly confirmed that Build 21 built successfully and all tests were completed.
- Live GitHub branch search returned exactly one server-side branch: `main`.

The earlier `docs/architecture.md` was a proposal describing Cloudflare Workers as the planned API. The README and current project context clarify that this proposal is no longer the deployed request path: Android currently talks directly to Supabase for supported operations; Supabase Edge Functions handle privileged notification workflows. The separate website and shared Cloudflare Workers API remain future/planned work only. See the corrected architecture document.

## 18. Historical implementation map

The detailed chronology is maintained in [BUILD_HISTORY_01.md](../history/BUILD_HISTORY_01.md). At a high level:
- Initial Android foundation and UI decomposition: Builds A/B and subsequent extraction/fix commits.
- Notes workspace, authentication, account-scoped persistence, offline synchronization, realtime and conflict recovery.
- Admin Panel and secured push-notification operations (Builds 1–10).
- Installation-scoped FCM token registry, user notification inbox and opt-in topic preferences (Builds 11–13).
- Per-device FCM delivery diagnostics and security maintenance (Build 14).
- In-app update checker, prompts, APK download, SHA-256 verification and installer (Builds 15–18).
- Production update metadata URL and v1.0.1 release preparation (Builds 19–20).
- Settings manual update-check UX/logic refinement (Build 21; user-confirmed successful Build and tests on 2026-11-20).

## 19. Documentation maintenance rules

1. Update this master file when project identity, architecture, key decisions, live version, current active Build or priorities change.
2. Add a concise `CHANGELOG.md` entry for significant completed or implemented milestones; label implementation vs user-confirmed testing.
3. Append detailed Build, migration or release events to the corresponding history file.
4. Keep old history rather than deleting it; correct mistakes with dated notes when necessary.
5. Do not copy secrets, passwords, private account emails, raw FCM tokens or service-role credentials into public documentation.
6. Before documenting a live status, fetch the relevant GitHub/Supabase state again. The snapshot above is dated and will become historical.


## 20. CodeAssist IDE reference documentation

The user supplied screenshots documenting CodeAssist 3.46.0's Git/Source Control interface. Detailed screenshot-derived documentation is maintained separately to keep this master handoff concise:

- [CodeAssist Reference Index](../../reference/codeassist/README.md)
- [Git and Source Control walkthrough](../../reference/codeassist/source-control.md)
- [Modules and Android Module Settings](../../reference/codeassist/modules-and-android-settings.md)

The reference covers the Branches screen and stale/local refs, repository three-dot menu, History, Stashes, GitHub integration, Manage > Accounts, and observed Push status. The user clarified that the actual GitHub repository has only one branch, `main`; old branch entries shown in CodeAssist are not to be treated as active server branches. Continue verifying live GitHub state before changes and work only on `main`. Do not store credentials or personal email addresses in the reference files.


## 21. CodeAssist Modules and Android Module Settings reference

On 2026-11-20, the user supplied screenshots of the CodeAssist 3.46.0 Modules screen and the `app` Android Application module's Settings tab. The screenshot-derived details are maintained in:

- [Modules and Android Module Settings](../../reference/codeassist/modules-and-android-settings.md)

The screenshots show one `app` module, Java 17 selected (choices 8/11/17/21), Platform SDK `Auto (android)`, source sets, namespace `com.teddy.bpositive`, Compile/Target/Min SDK 36/36/26, manifest path, versionCode 2/versionName 1.0.1, enabled Non Transitive RClass/Is Application/Compose toggles, and debug/release Build Types. The reference records visible configuration only; it is not evidence of a successful Build. Build Features, Compiler plugins and Packaging tabs are still awaiting screenshots.


## 22. CodeAssist Build & Run and Privacy settings reference

Additional user-supplied CodeAssist 3.46.0 screenshots dated 2026-11-20 are documented in [Modules and Android Module Settings](../../reference/codeassist/modules-and-android-settings.md). New material covers:
- Dependency conflict resolution options (Newest selected), dependency scopes/variants and user-reported removal of three accidental Quick Add dependencies.
- Preview Sandbox protections and separate-process preview setting (all shown On).
- Build Runtime selections and numeric settings, including separate build process, Forked R8 VM, Auto low-memory mode, automatic prebuilt libraries, forwarded debug logs, Compiler VM, 1536 MB heap and Advanced dexing/compiler VM values.
- SDK Manager's visible Android platform/source versions and JDK source options.
- System > Privacy & Data: ads toggles On, performance analytics Off, cache clearing, logs and project ZIP backup actions.

These are screenshot-derived IDE preferences only. They do not establish that downloads, cache clearing, backups or a successful Build occurred. The user reported removing three accidentally added Quick Add dependencies; the subsequent list screenshot no longer showed Firebase BOM, Firebase Analytics or Play Services Auth, while Firebase Messaging remained. Build verification is still separate.


## 23. Build 22 — Note reader/editor redesign (user-confirmed Build successful)

On 2026-11-20, the user approved Build 22 to replace direct inline note editing with a dedicated full-screen note experience:
- Note List remains a preview only (title + short content excerpt); tapping a note opens a separate screen.
- Note detail opens in Reading Mode by default. The complete content is vertically scrollable.
- Tapping the title/body or Edit action enters Editing Mode; Save writes the updated note to local storage and queues the existing Supabase sync path.
- Note colors are stored in account-scoped local note JSON via a backward-compatible `colorHex` field with a default of `#FFFFFF`; no production DB migration was made. Color is device-local and is not synchronized to other devices.
- The old 3,000-character input guard was removed for note content; existing notes remain readable.
- The note card remains a short preview, with pin/delete controls.
- Editing UI includes basic Markdown text shortcuts (bold, italic, heading and bullet templates). This version inserts Markdown markers into plain text; rich rendered Markdown preview is not yet implemented.

Files changed:
- `app/src/main/kotlin/com.teddy.bpositive/model/Note.kt`
- `app/src/main/kotlin/com.teddy.bpositive/data/LocalNotesStorage.kt`
- `app/src/main/kotlin/com.teddy.bpositive/ui/components/NoteComponents.kt`
- `app/src/main/kotlin/com.teddy.bpositive/ui/screens/HomeScreen.kt`
- `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NoteDetailScreen.kt` (new)
- `app/src/main/kotlin/com.teddy.bpositive/MainActivity.kt`

Implementation commits (directly on `main`):
- `e28271bf2766ace2285b8fee4491e5625b823a24` — note color field
- `df0e3bbd14a62bd57191809ce66008b7399e9f56` — local persistence
- `9754947d9e0d38c907b17e4464b2de8636440ba8` — clickable colored note cards
- `afcf5d09dc858714ebf2b2f4ad8268919215e4ef` — list remains preview
- `282ae2353f69988f657527790f48eb8a05772180` — full-screen detail screen
- `b8d2e0eab551ea3a896e3782c5c947a174a07544` — MainActivity integration
- `cfe9d753a86d593483625c3ee5edb80f444a111c` — preserve local color during cloud refresh
- `c9db6b8fd113446b0d6e5d0cea9d11613ec5ccd0` — remove obsolete 3,000-character indicator
- `2c901c1d60504578885c8bf003c6596574d6e268` — improve Markdown shortcut templates

No Supabase schema/migration or paid plan change was made. Supabase remained `ACTIVE_HEALTHY` at pre-change verification, with 18 applied migrations and existing `public.notes` columns unchanged. GitHub branch verification showed only `main`.

### Dark Mode Note Card correction and reader tap-area fix — user-confirmed Build success

The first Build 22 UI showed white Note Cards in Dark Mode because the saved light `colorHex` continued to control the card background. The focused correction in `NoteComponents.kt` now uses `MaterialTheme.colorScheme.surface` and `onSurface` in Dark Mode, while Light Mode retains the selected per-note color.

- `df684cbd23` — initial text-contrast correction in Note Reader/Editor and Note Card.
- `08b02aec7f` — Note Card background follows the active Dark Theme surface.
- **User-confirmed on 2026-11-20:** the user reported the targeted color behavior works and explicitly confirmed Build Successful.

The user subsequently confirmed that the follow-up reader tap-area correction also built successfully and was tested: tapping the note text enters Editing Mode. This confirms the tested interaction and Build, not exhaustive regression coverage of every offline sync, realtime, conflict recovery or long-note scenario. No Supabase schema/migration or paid plan change was made.



## 24. Home Landing Page and Notes List UI (user-confirmed Build successful) 

User approved a new two-level Home experience based on the supplied Google Keep Notes screenshot. The screenshot is the reference for both the Notes screen header/action-icon arrangement and the note-list card layout; no Reader/Editor redesign was requested yet.

Implementation committed directly to `main`:
- Replaced the old Home Dashboard presentation with a feature-oriented Landing Page. The first feature card introduces Notes and has an Open Notes destination button; the structure can later host other feature cards.
- Added a separate Notes screen with a large top-left Notes title, top-right Checklist/Search/Settings glyphs, equal-height rectangular note cards, title, two-line content preview with ellipsis, and date/time formatted as `dd MMM yyyy, hh:mm a` (example: `21 Nov 2026, 08:30 AM`).
- Added a circular bottom-right plus FAB. The three header icons, FAB and note cards are intentionally presentation-only and do not navigate or trigger actions in this phase.
- Cards use the active Material Theme surface/on-surface colors. No per-note card color is applied in this new list.
- The existing app-wide top bar and Global Bottom Navigation remain. The old dashboard data, local storage, Supabase schema, sync logic, pin/trash/conflict data and existing Reader/Editor implementation are not deleted. The Reader/Editor UI has not been redesigned.
- This is a first implementation pass from the screenshot; exact visual fidelity, spacing and icon geometry remain subject to the user's CodeAssist device review and any screenshot-based adjustments.

Commits:
- `e05a48d536` — replace dashboard with feature landing page
- `61003e446b` — add Notes list screen
- `f1767f7dfc` — route Home and Notes feature screens
- `16e202dc4b` — remove obsolete dashboard route
- `59da046916`, `fa2dac5102`, `c7aca95602` — correct Compose theme color capture in Canvas-drawn icons
- `2b558efa03` — align Notes title and action icon row vertically to the reference screenshot
- `24c79a9b45` — hide the app's WorkspaceTopBar while the dedicated Notes screen is open, allowing the Notes screen to use the screenshot-style header while preserving the Global Bottom Navigation
- `137f27d293` — match the screenshot's yellow circular FAB with a white plus glyph

**User-confirmed on 2026-11-21:** the user reported the new Home Landing Page and Notes List UI Build completed successfully and supplied two device screenshots for review. The screenshots show the Notes title/action row, dark surface cards, title/preview/date-time, yellow FAB and Global Bottom Navigation. The two screenshots are the visual baseline for the next UI refinement phase. No Supabase migration, schema change, destructive database action or paid upgrade was made.


## 25. Build 24 — Source-style Collapsing Notes Header and Scroll Refinement (implementation committed; Build pending)

The user supplied a 30.35-second source-app screen recording on 2026-11-21. It was inspected at sampled timestamps throughout the recording. Reference behavior: expanded header has a large top-left Notes title and Checklist/Search/Settings icons on a separate right-aligned row; scrolling down collapses the title and moves all three icons onto the same row; scrolling upward expands the header after the list reaches its upper boundary; the final note should not leave a large artificial bottom gap. The global bottom navigation remains.

Implementation committed directly to `main` in `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NotesScreen.kt`:
- Nested-scroll behavior collapses on downward scrolling and expands on upward unconsumed scroll at the list top.
- Animated transition between expanded and collapsed header layouts.
- Kept note cards at 104dp height, two-line ellipsized preview, timestamp format, action icons, yellow FAB and global bottom navigation.
- Reduced LazyColumn bottom content padding from 100dp to 8dp; Scaffold bottom-bar padding remains.
- No Home Landing Page, Reader/Editor, navigation action, database schema, Supabase migration or paid-plan changes.

Commits: `05ad4253d023a388750af75820e6bebc1f32e14e` (implementation), `7260543d4080bb9026819ee83d770c95a88683a4` (import cleanup). Source is committed but not compiled or device-tested by the assistant. User CodeAssist Build and device verification are pending; do not mark successful until the user confirms.


## Build 25 — Continuous Scroll-Driven Notes Header (implementation committed; Build pending)

After user confirmed Build 24 successful, they supplied a new device recording showing the header transition was abrupt and the first note moved upward during collapse. The recording was inspected frame-by-frame at sampled intervals.

Build 25 refinement committed directly to `main`:
- Replaced Boolean expanded/collapsed state with continuous `collapseProgress` (0..1).
- Title font size, title position, action-row position and header height interpolate continuously from the finger's scroll distance; this is not a time-based snap animation.
- Nested scroll consumes downward drag to collapse the header before passing remaining movement to the LazyColumn, preventing the first note from independently scrolling during header collapse.
- Upward expansion is consumed progressively only from unconsumed upward scroll after the list reaches its top boundary.
- Existing note card layout, action icon positions, FAB and global bottom navigation retained.

Commit: `aea5df26b6af0ef59743fa9e5f329377b37234ff`.

No database, Supabase, Landing Page or Reader/Editor changes. Source committed; CodeAssist Build and device verification pending. Verify collapse/expand smoothness, first-card stability, list scrolling after full collapse, and bottom-of-list spacing before marking successful.
