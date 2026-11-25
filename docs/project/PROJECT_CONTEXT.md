# Teddy-WebApp — Project Context & Progress Log

সর্বশেষ হালনাগাদ: ২০২৬-১১-২৫
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

- Android IDE: CodeAssist 3.48.2 (user-reported update on 2026-11-24).
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


## 25. Build 24 — Source-style Collapsing Notes Header and Scroll Refinement (implementation committed; Build verified later)

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

No database, Supabase, Landing Page or Reader/Editor changes. User subsequently confirmed Build 25 successfully built and reported that the continuous header collapse and first-card behavior were substantially improved. A remaining issue was title-text jitter during font-size changes; that issue is addressed in the next focused refinement.


## 26. Build 26 — Smooth scroll-linked Notes title scaling (user-confirmed successful)

The user reported that the continuous header collapse was smooth but the `Notes` title visibly jittered/lagged while shrinking. The requested behavior is for title size to track finger/scroll movement continuously, at the same pace, without visible jumps.

Implementation in `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NotesScreen.kt`:
- Keep the title Text composable at a fixed 34sp layout size and fixed 40sp line height.
- Apply a scroll-progress-driven `graphicsLayer` scale from 1.0 to 26/34, anchored at the top-left transform origin.
- This avoids changing `fontSize` and triggering repeated text remeasurement on every scroll delta, which is the suspected source of the jitter.
- Title position, header height and action-row position remain tied to the same continuous `collapseProgress`; the existing nested-scroll behavior and note list are unchanged.
- No database, Supabase, Home Landing Page, note card, FAB, navigation or Reader/Editor changes.

Commit: `e85900fea7872a9d134eb3a35725fe60eb2b8c28`.

User confirmed Build 26 successfully built. Device testing showed modest stability improvement, but the title still did not meet the desired smoothness; a deeper Compose phase-optimization refinement follows in Build 27.


## 27. Build 27 — Compose phase-optimized scroll-driven header (implementation committed; Build pending)

Following Build 26 device testing, the user reported that title scaling was somewhat more stable but still not sufficiently smooth. The current implementation was found to read `collapseProgress` during composition, causing the `NotesScreen` composition scope (including the LazyColumn subtree) to be invalidated for every nested-scroll update.

Build 27 moves scroll-state reads into Compose's rendering phases:
- `collapseProgress` is retained as a `MutableFloatState`, but the header receives it as `State<Float>`.
- Header height reads the state inside a custom `Modifier.layout` measure block.
- Title and action-row offsets read the state inside placement lambdas (`Modifier.offset { ... }`).
- Title scale reads the state inside `graphicsLayer`, keeping the fixed Text layout and updating only the graphics layer.
- The main screen no longer reads the changing float during composition, avoiding per-pixel recomposition of the screen and LazyColumn as the intended optimization.
- Existing nested-scroll consumption logic, card layout, icons, FAB and navigation remain unchanged.

Commit: `1ec5ff1f53948e8741d7047900e77b548091fb4b`.

Source committed on `main`; CodeAssist Build and device verification pending. No Supabase/database changes. Do not mark Build 27 successful until user confirmation.


## Build 28 — Draw-phase Notes title interpolation (user-confirmed successful)

After the user tested Build 27, they reported that overall movement was acceptable but the title still lacked the desired continuous smoothness. The uploaded 59-frame GIF was reviewed. Its frame delays are irregular (including long pauses), so it is not a reliable fixed-frame-rate performance capture; it does show the title/header/list states across the scroll recording.

Build 28 replaces the title's `graphicsLayer` scaling with draw-phase text layout:
- Uses `rememberTextMeasurer()` and Canvas `drawText`.
- Interpolates the actual font size continuously from 34sp to 26sp using the current scroll progress.
- Interpolates line height and vertical title position from the same progress value.
- Glyphs are re-shaped at the interpolated font size rather than scaling an already-laid-out Text layer, targeting the remaining visual jitter/softness.
- Keeps scroll progress as the sole driver; no independent time-based animation or spring is added, so the title should remain directly coupled to finger movement.
- Existing header measure behavior, action icons, list, note cards, FAB and bottom navigation are otherwise unchanged.

Commit: `ecbad69bf6925e52f4b1dd3b593d6d9fcb1e5164`.

Source committed directly to `main`. **User confirmed Build 28 successfully completed** and reported that the result did not materially improve the title smoothness enough. No Supabase/database changes; live Supabase remained ACTIVE_HEALTHY with the existing 18 migrations.


## Build 29 — Header-first two-stage scrolling (implementation in progress)

The user clarified the desired interaction: while the header is expanded or partially collapsed, downward scroll must be dedicated to collapsing the header. Note cards may move upward only as a consequence of the header's shrinking layout, but the LazyColumn's own scroll position must remain unchanged until the header reaches its fully collapsed state. Only subsequent scroll input should scroll the note list. This is a strict two-stage interaction, including when one drag event crosses the collapse threshold: the threshold-crossing delta is consumed by the header and must not also move the list in that same event. Upward scrolling should first return the list to its top, then expand the header.

Build 29 changes the `onPreScroll` ownership rule in `NotesScreen.kt`: if the header is not fully collapsed at the start of a downward nested-scroll event, the header consumes that entire event and updates collapse progress, even if it reaches 1.0 during that event. The residual distance from that same event is intentionally not forwarded to LazyColumn; list scrolling starts with the next nested-scroll event. Existing upward expansion remains in `onPostScroll` after the list cannot consume more at its top boundary. Commit: `0c01a24c37edf48418e3b2aef997b9c402f0e651`. Existing visual geometry, draw-phase title interpolation, note cards, icons, FAB, Home Landing Page, global navigation and note/data behavior are unchanged. No Supabase/database changes. CodeAssist Build and device verification are pending; do not mark Build 29 successful until user confirmation.


## 26. Build 30 — Full-screen Note Reader/Editor (source committed; Build pending)

User-approved reference behavior:
- Opening a note displays a dedicated full-screen screen inspired by the supplied source-app screenshots.
- Reader mode is the initial state. The top row has Back and a yellow Save/check action; note title, modified date/time and character count appear above the body.
- Tapping the title enters editing with title focus. Tapping the body enters editing with the body field focused. The body editor fills the remaining content surface, so tapping blank space below a short note should place the cursor at the end of the content; tapping text should retain the text field's tap-based cursor placement.
- Editing mode shows Undo/Redo affordances, Save, and a bottom formatting toolbar for bullet/checklist/numbered insertion, bold, italic, underline and appearance palette.
- Save updates the local note, queues the existing cloud upsert, hides the keyboard and returns to Reading Mode without closing the detail screen or navigating to the Notes list.
- The Notes screen's LazyListState and header collapse progress are hoisted above the route switch, so opening and closing a note retains the previous list scroll position and expanded/collapsed header state.
- No Supabase schema or migration change; note color remains local-only.

Implementation files:
- `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NoteDetailScreen.kt`
- `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NotesScreen.kt`
- `app/src/main/kotlin/com.teddy.bpositive/MainActivity.kt`

Implementation commits on `main`:
- `c252370c0138591ae45f6c4a98673b0b50d3b5f9` — first full-screen reader/editor implementation
- `2020243b2cc292a1a6a5279e0fbd3a80214cf334` — connect note-card taps and retain Notes LazyListState
- `a27c2a2a8e3ae105ae55c495532a3c050b7b01ac` — defer focus request until edit field is composed
- `fea2949e1b9158ba7c985895607ab21cdb7ede84` — retain header collapse progress across reader navigation

Verification status:
- Source is committed directly to `main`; no assistant-side Android compilation or device test has been performed.
- CodeAssist Build and user device verification are pending. Do not mark Build 30 successful until the user confirms.
- The reminder, export and share panels are currently visual placeholders; the note-list Trash action remains the existing delete workflow. The lock toggle is in-memory only. These secondary actions need their own functional implementation before they can be described as complete.
- Review the Back behavior, title/body focus, blank-area tap-to-end cursor, text cursor placement, Save-without-navigation, keyboard dismissal, and restoration of list scroll/header state.


## Build 30 follow-up — Note Reader/Editor horizontal margin (user-confirmed successful)

- Reduced the Note Reader/Editor content area's horizontal inset from 24dp to 22dp. The visible content layout is otherwise unchanged.
- Commit: `873dead37b946308fc0b7759a54024cbcd01052d`.
- **User-confirmed on 2026-11-21:** CodeAssist Build completed successfully after this change.
- No database, Supabase, navigation or other screen changes.

## Build 31 — Note Reader/Editor tap feedback and full-width margin touch target (source committed; Build pending)

Implemented in `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NoteDetailScreen.kt`:
- Disabled visible ripple/indication feedback for Note Reader/Editor interactions, including Material 3 controls and Foundation clickable elements.
- Preserved the existing 22dp horizontal visual inset while adding a tap handler to the content area's outer width. Tapping any unhandled area of the full content region—including both horizontal margins and the metadata/spacing areas—enters Editing Mode and places the body cursor at the end; title/body taps retain their dedicated focus and cursor behavior.
- The lock state still prevents entering Editing Mode. Header and bottom action controls retain their own actions.
- Commits: `c4c095294e84fd79f5f2a692bab356715723c513`, `970a7926ff3d0abc01851d79f530bf20b5ff4467`.
- CodeAssist Build and device verification are pending. Do not mark Build 31 successful until the user explicitly confirms.
- No Supabase/database/schema changes.


## Build 31 — Note Reader/Editor ripple removal and full-width tap target (user-confirmed successful)

- Removed visible ripple feedback throughout the Note Reader/Editor using the screen's ripple configuration.
- Preserved the 22dp visual horizontal inset and enabled taps across the full content region, including margin and spacing areas, to enter Editing Mode. Title/body controls retain their dedicated focus and cursor behavior.
- The initial source attempt was corrected after CodeAssist reported a nullable `LocalIndication` type error and a missing closing brace. The final source removes the invalid `LocalIndication provides null` entry, retains `LocalRippleConfiguration provides null`, and closes the composable scopes correctly.
- Final source fix commit: `e3800c58e0ea9616a7302719c1c2254c7de99859`.
- **User-confirmed on 2026-11-21:** CodeAssist Build completed successfully; user also verified that ripple feedback was gone and taps across the content area entered Editing Mode.
- No Supabase/database/schema changes.


## Build 32 — Note Reader/Editor top-bar alignment and toolbar icons (source committed; Build pending)

- Aligned the top app-bar controls to the same 22dp horizontal inset used by the note title/content. The Back control is on the left; right-side actions are grouped and right-aligned.
- Replaced text glyphs with custom-drawn outline icons: Back arrow, Reading Mode bookmark, Editing Mode Undo/Redo arrows, and yellow Save checkmark. No new dependency was added.
- Preserved the root `.imePadding()` behavior. The editing toolbar remains attached to the bottom of the available app area: it moves immediately above the software keyboard while the keyboard is open, and returns to the bottom when the keyboard is hidden without leaving Editing Mode.
- Source file: `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NoteDetailScreen.kt`.
- Commit: `31da483988dbb89e4271208b72a5b4cba35fea54`.
- **User-confirmed on 2026-11-21:** CodeAssist Build completed successfully. User tested the screen and reported remaining alignment, reading-scroll and keyboard-toolbar behavior defects, addressed in Build 33.
- No Supabase/database/schema changes.


## Build 33 — Note Reader/Editor alignment, reading scroll and keyboard toolbar (single commit; Build pending)

- Replaced Material `IconButton` controls in the top bar with exact 24dp clickable icon bounds. This removes the default minimum touch-target inset from the visual alignment calculation: the Back icon now starts at the 22dp content inset, and right-side icons end at the matching 22dp inset. Tap targets match the icon bounds and do not add visual padding.
- Added vertical scrolling to the Note content region. Scroll gestures move through long notes in Reading Mode; tap gestures remain the entry to Editing Mode, while the existing body text position calculation is retained for cursor placement.
- Removed vertical `weight` constraints from children inside the scroll container and gave the body area a 240dp minimum height so the scrollable column can measure content correctly.
- Added `android:windowSoftInputMode="adjustResize"` to MainActivity. The screen's existing root `.imePadding()` now receives the keyboard resize/inset and keeps the Editing Toolbar immediately above the keyboard. When the keyboard hides while Editing Mode remains active, the toolbar returns to the bottom of the screen.
- Build 32 successful confirmation was recorded in Master Documentation and Build History in this same commit.
- **Build 33 CodeAssist Build and device verification are pending.** Do not mark successful until the user confirms.
- No Supabase/database/schema changes.


## Build 34 — Compose UI Preview additions (user-confirmed successful Build; Preview Interpreter limitations remain)

- Added Preview composables at the ends of 14 existing UI Kotlin source files, covering reusable components, dialogs, application screens and theme previews. The additions use sample/static data and no-op callbacks where applicable.
- Preview additions were limited to the UI source files; no Supabase, database, repository, Gradle, manifest, or other non-UI source files were changed by this task.
- Commit: `e75317c71ab453cd909012a8e3673ea860b3b835`.
- **User-confirmed on 2026-11-22:** CodeAssist Build completed successfully after the Preview additions.
- During Preview rendering (separate from the successful app Build), CodeAssist reported interpreter errors in dependency/data-layer declarations, including Kotlin standard-library calls, OkHttp extension functions, `org.json` methods, `android.os.Handler`, and functions that could not be lowered after earlier parse failures. These are Preview Interpreter diagnostics and are not, by themselves, evidence of Android compilation failures.
- Initial Preview rendering exposed CodeAssist Interpreter limitations in backend/data dependencies. Follow-up commit `d6f87570ac184def609074db9ce81547f432eed8` replaced the affected files' Preview composables with self-contained sample UI so the Preview Interpreter no longer needs to instantiate those backend models/repositories.
- **User-confirmed on 2026-11-22:** Preview rendering now works in all four checked files: `NotificationInboxScreen.kt`, `ProfileScreen.kt`, `AdminPanelScreen.kt`, and `WorkspaceScreens.kt` (Trash and Settings previews). The user supplied screenshots for Notification Inbox and Profile; the user separately confirmed Admin Panel and Workspace previews also work.
- These previews are static/sample representations with no-op actions; they do not establish that every preview exactly matches the live runtime UI.
- No Supabase/database/schema changes.


## Build 35 — Restore the existing Note Reader/Editor caret auto-scroll baseline (Build pending)

- Compared the current Note Reader/Editor source with commit `409c7b16ca020553f654e33d71d7d6c643d7eafc`. GitHub confirmed the only intervening source change was a four-line conditional bottom-padding addition (`120.dp` while editing, `24.dp` while reading); the cursor measurement, IME-settle wait, viewport comparison and `animateScrollBy()` logic were otherwise unchanged.
- Removed only that extra conditional bottom padding, restoring the earlier content layout while preserving the existing cursor-position calculation and automatic scroll implementation.
- Source commit: `1210f4aff094f1b95cb8df286936f3f8037096f5`.
- No other UI, editor behavior, data, database, Supabase, migration or configuration files changed.
- **CodeAssist Build and device verification are pending user confirmation.**


## Build 36 — Notes Selection Mode and Dedicated Notes Settings (user-confirmed successful)

User confirmed on 2026-11-23 that CodeAssist Build completed successfully after the Notes actions implementation.

Source commits on `main`:
- `9d5c5643ab76bbe9a5f8d060cffb056a0009c80a` — Added Notes selection mode, per-note selection controls, a selection count, select-all/clear selection, bulk Delete action, and a dedicated Notes Settings screen.
- `ee4a9e22298fcb1ef2a6b6321576754397b534c3` — Connected bulk deletion to the existing note-delete synchronization callback and local save path.
- `79406bdcec6d2c40a0284a1195cec0d2383af92d` — Added the vertical-scroll import and prevented the Notes FAB from appearing while the dedicated Notes Settings screen is open.

Implemented in `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NotesScreen.kt`:
- Checklist action opens Selection Mode.
- Visible note cards gain checkboxes; tapping a card toggles its selection while Selection Mode is active.
- The selection header displays the selected count, offers All/None selection for the currently displayed list, and provides bulk Delete and close actions.
- Notes Settings opens as a separate screen within the Notes feature, not the app-wide Settings screen.
- Settings currently include Compact Cards, Larger Text, Newest First / Oldest First sorting, and Reset to Default.
- Notes feature preferences are persisted locally using SharedPreferences.
- Existing Search title/content matching remains integrated with the visible note list and sorting.

Bulk deletion is wired to the existing `syncNoteDelete` callback for each selected note, removes selected IDs from the current local notes collection, and calls the existing save path. No new archive state or Archive action was added because the current Note model does not expose a verified archive workflow.

No Supabase schema/migration, production database, paid plan, or app-wide Settings changes were made.

**User-confirmed on 2026-11-23:** “Successfully Build Completed!” The user explicitly confirmed the CodeAssist Build after these changes. This confirms compilation/build completion; no separate exhaustive device regression test has been reported yet.


## Build 37 — Note Card long-press action dialog (user-confirmed successful)

- Added a long-press gesture to each Note Card (disabled while Selection Mode is active).
- Long press opens an action dialog showing Pin, Lock / Encryption, Share, Delete and Close options.
- This milestone is UI-only: the action rows currently dismiss the dialog and do not execute the corresponding note operations. Existing Selection Mode bulk Delete remains unchanged.
- Implementation file: `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NotesScreen.kt`.
- Commit: `5045f3c32e82be44505e8862b50551a1f8393e1f`.
- **User-confirmed on 2026-11-23:** CodeAssist Build completed successfully.
- No Supabase/database/schema changes.


## Build 38 — Replace Material Icons Extended with lightweight Canvas glyphs (user-confirmed successful)

- User confirmed on 2026-11-23 that CodeAssist Build completed successfully and supplied a screenshot showing the Note actions dialog with the custom icons.
- Removed `androidx.compose.material:material-icons-extended` from `app/module.toml` to avoid packaging the large extended icon library.
- Replaced the five Material icon vector imports/usages in `NotesScreen.kt` with local Canvas-drawn glyphs for Bookmark, Lock, Share, Delete and Close.
- Source commits: `069c7f6433621778e59ec0685dbead6197639dff` (Canvas glyphs) and `9104acd83e287ddbc01873b8a96e9aaa03379a13` (dependency removal). A later local sync commit was `bdfab93888f6c4424b298d4f3b883d9b578c03dd`; merge commit `a391b9322df967bdac3592014161445c8ff2ee1b`.
- User reported the icons looked good. The dialog actions remain UI-only; action rows still only dismiss the dialog.
- No Supabase/database/schema changes.


## Build 39 — Notes vault cryptography foundation (user-confirmed successful Build; feature integration pending)

- User confirmed CodeAssist Build completed successfully after adding `app/src/main/kotlin/com.teddy.bpositive/security/NotesEncryption.kt`.
- Added client-side cryptographic primitives only; no UI, note storage, sync, or Supabase integration has been connected yet.
- Uses AES-256-GCM for authenticated encryption, PBKDF2-HMAC-SHA256 for deriving wrapping keys, a random 256-bit master key, and a generated 256-bit URL-safe Recovery Key.
- The master key is independently wrapped for the Master Password and Recovery Key. The serialized vault envelope contains salts, nonces and wrapped master-key ciphertext; it does not contain the plaintext password, recovery key, or master key.
- Includes password unlock, recovery-key unlock, password re-wrapping, and note-field encrypt/decrypt primitives.
- Implementation commit: `cfdc0cdcbfd186985d0d0b60c01c9bfbbaed11b8`.
- User-confirmed Build success is not a cryptographic security audit or end-to-end test. The primitives are not yet wired into the app, local persistence, Supabase, or the Note action UI. Do not represent notes as encrypted or the Lock feature as functional yet.
- No Supabase schema, migration, production database, or paid-plan changes.

## Build 40 — Note actions: Pin, Delete-to-Trash and Share (user-confirmed successful)

- **User-confirmed on 2026-11-24:** “Successfully Build Completed!” The user confirmed that the CodeAssist Build for this change-set completed successfully.
- Connected the Note Card long-press action dialog to real note actions:
  - Pin / Unpin calls the existing pin-toggle and save/synchronization flow.
  - Pinned Note Cards display a compact custom-drawn pin indicator; no additional icon dependency was introduced.
  - Delete asks for confirmation (“Move note to Trash?”), then uses the existing note-delete synchronization path, removes the note from the active local list and saves the change.
  - Share opens Android's native text-sharing chooser with the note title and content.
- The Lock option is deliberately **not active yet**. Selecting it displays a message that Secure Vault setup is not connected and explicitly states that the note has not been encrypted. Do not claim note encryption or functional locking.
- Source files:
  - `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NotesScreen.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/MainActivity.kt`
- Source commits on `main`:
  - `1338e68d7f4446af57974b1f21449baffb6370e0` — Wire action dialog callbacks, delete confirmation and pinned-card indicator.
  - `cb77c777dc95d8a181a6c7a563ef6e9a79a6581b` — Connect pin/delete/share to the app's existing state, sync and Android share flows.
  - `162af7489945d26e94cd51eeb05092b053d56903` — Clarify that Vault setup is required before note locking.
  - `a89526142c6c6cefaddaee5cd8d88f0737b32a1b` — Show an explicit inactive-vault notice when Lock is selected.
- No Supabase schema, migration, production database, or paid-plan changes. Live Supabase inspection before this change confirmed the existing `public.notes.is_pinned` and `deleted_at` columns and RLS remained in place; no database write was needed.
- Build confirmation establishes successful CodeAssist compilation/build as reported by the user. It does not establish exhaustive device regression testing or encryption security. The earlier cryptography primitives remain groundwork only; Secure Vault persistence, setup/unlock/recovery UI, note encryption integration and sync strategy are still pending.


## Build 41 — Pinned Note Card indicator and tap-to-unpin (user-confirmed successful)

- **User-confirmed on 2026-11-24:** “Successfully Build Completed!” CodeAssist Build completed successfully, as explicitly reported by the user.
- Refined the pinned-note indicator to a custom Canvas pushpin glyph (replacing the previous bookmark-style appearance). No external icon dependency was added.
- The pinned indicator on a Note Card is now clickable; tapping it invokes the existing pin-toggle callback and unpins the note. The Note action dialog's Unpin option remains available as a second way to unpin.
- File changed: `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NotesScreen.kt`.
- Source commits:
  - `20990cab635c128a19516288978d21e5edf04438` — Refine pinned-note icon and tap-to-unpin.
  - `cd7f2fc68c0fd78be2a2da89b473311f1c539a1c` — Keep pushpin Canvas drawing non-composable.
- No Supabase schema, migration, production database or paid-plan changes.
- Secure Vault setup, persistence, recovery and end-to-end note encryption remain unimplemented; Lock remains inactive. Build success confirms compilation only, not a security audit or exhaustive device regression test.

## Build 42 — Secure Vault setup and local envelope persistence (user-confirmed successful)

- **User-confirmed on 2026-11-24:** “Successfully Build Completed!” CodeAssist Build completed successfully, as explicitly reported by the user.
- Added Secure Vault setup and entry from app Settings:
  - Master Password setup requires at least 12 characters and confirmation.
  - Generates a Recovery Key and requires the user to enter it again before saving; the key is shown during setup for the user to copy/store.
  - Existing Vault can be verified/unlocked with the Master Password from the screen.
  - Temporary password character arrays and the setup-returned master-key byte array are cleared after use.
- Added account-scoped local Vault envelope persistence in an app-private no-backup file. The serialized VaultEnvelope is encrypted at rest with Android Keystore AES-GCM; save uses a temporary file/rename strategy and reports storage errors without intentionally replacing the previous file on a failed write.
- Files added/changed:
  - `app/src/main/kotlin/com.teddy.bpositive/security/NotesVaultStorage.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/ui/screens/SecureVaultScreen.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/ui/screens/WorkspaceScreens.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/MainActivity.kt`
- Source commits on `main`:
  - `ebd3b6` — Initial account-scoped Vault storage.
  - `651b43` — Secure Vault setup screen.
  - `af57223b6c1d5986259eb280f3b3d5cc008aa90a` — Secure Vault Settings entry.
  - `03d957e8bc3a53d481f11a832da255ff0859647f` — Secure Vault route/navigation.
  - `63617766e7c06802db268613ec39bc7a585ab177` — Encrypted no-backup local file storage.
  - `fc4e6a214bed8ba7006f94e9d413cab5cc67bcce` — Clear temporary password arrays.
  - `2cb4e81e4c27f60767b81b2dd9c55ea6eb94217e` — Preserve existing vault file if save fails.
  - `48c7a08ab99dafcb0a041142211ad0b93b6a9854` — Handle storage failure safely; latest implementation commit.
- The user confirmed the CodeAssist Build completed successfully. This is not a cryptographic audit or proof of hardware-backed Keystore storage.
- Recovery-Key unlock UI, multi-device envelope sync, password-change/recovery flows and Note Lock integration are not yet implemented. Existing notes are **not encrypted** by this milestone; the Note Lock action remains inactive. Do not claim end-to-end encryption or functional note locking.
- No Supabase schema, migration, production database or paid-plan changes. The existing Supabase Free Plan remains the budget constraint.

## Build 43 — Secure Vault Recovery Key Unlock (user-confirmed successful Build)

- **User-confirmed on 2026-11-24:** “Successfully Build Completed!” CodeAssist Build completed successfully, as explicitly reported by the user.
- Added a Recovery Key unlock mode to the existing Secure Vault unlock card. The user can switch between Master Password and Recovery Key modes; Recovery Key input is masked.
- Recovery Key verification uses the existing `NotesEncryption.unlockWithRecoveryKey()` path. On successful verification, the returned master-key byte array is cleared and the input state is cleared. Failed verification also clears the input state and displays a generic failure message.
- Switching back to Master Password clears the Recovery Key input; the existing Master Password verification path remains available and clears its returned key bytes after verification.
- Updated `deriveRecoveryKey()` to clear its trimmed temporary character array in a `finally` block after key derivation.
- Files changed:
  - `app/src/main/kotlin/com.teddy.bpositive/security/NotesEncryption.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/ui/screens/SecureVaultScreen.kt`
- Implementation commits:
  - `f9c83d5a5c67c0a232553c024c82ae4cac498903` — Clear temporary Recovery Key character array after derivation.
  - `5e27be90bd23cff2247662f7067894d9448a9cb1` — Add Recovery Key unlock mode and retain Master Password fallback.
- **Follow-up user report (2026-11-24):** The user confirmed that Recovery Key Unlock is working. A separate wrong-key rejection test has not been explicitly reported.
- Scope limitations remain: existing notes are not encrypted; Note Lock is not integrated; multi-device Vault-envelope sync and password-change/recovery workflows remain pending. Recovery Key unlock verifies access to the existing Vault master key but does not itself encrypt notes.
- No Supabase schema, migration, production database or paid-plan changes.

## Build 44 — Secure Vault Account Envelope Sync (user-confirmed successful Build)

- User confirmed CodeAssist Build success on 2026-11-24. The user subsequently confirmed that pressing the Account Sync action made the Vault available to the same account on another device.
- Added account-scoped `public.vault_envelopes` storage and client API to synchronize only the encrypted Vault envelope. The Master Password, Recovery Key and unwrapped master key are not uploaded.
- New device behavior: when no local Vault exists, the app checks the signed-in account; if an envelope exists, it restores it into local Android-Keystore-protected storage. If the account check fails, setup is blocked rather than risking creation of a second independent Vault.
- Existing local Vault behavior in this milestone required the user to press the Account Sync button once. A different remote envelope is never overwritten; duplicate/simultaneous first-sync attempts are checked without upsert/overwrite.
- Production Supabase migration applied (additive table only): `backend/supabase/migrations/20261124044324_build44_secure_vault_envelopes.sql`. Table has a user_id primary key referencing auth.users, JSONB envelope, timestamps, RLS, owner-only authenticated SELECT/INSERT, and no client UPDATE/DELETE grant or policy. Live checks verified authenticated SELECT/INSERT, no UPDATE, no anon SELECT, RLS enabled. No paid plan change.
- Files added/changed: `data/SupabaseVaultApi.kt`, `ui/screens/SecureVaultScreen.kt`, `MainActivity.kt`, and the migration above.
- Source commits: `8a03c471b9a76d41c87a957bebca02a16b665560` (migration), `e31639f9bc9014ddc32f899026093b921f4a1239` (API), `5929680d6b5c2141cac856226cfed078526fca1a` (screen), `f4f44fc252fd285700af1d89a387736004364475` and `f63b971c0a9312de5717da4600296504f8a6714e` (session wiring/guard).
- User confirmed successful CodeAssist Build and successful account sync on a second device. This is functional user-reported verification, not a cryptographic audit or exhaustive multi-device/network test.
- Limitations: only the Vault envelope syncs; notes themselves are not encrypted or synchronized by this feature. No remote envelope replacement, password-change synchronization, Vault deletion workflow or continuous background sync. The user initially had to trigger sync manually; Build 45 removes that requirement.

## Build 45 — Automatic Secure Vault Account Sync (user-confirmed successful Build)

- User confirmed CodeAssist Build success on 2026-11-24 and reported that the automatic sync change works.
- When a local Vault is found on Secure Vault screen entry, the app now automatically calls the safe check-before-create sync flow. If the same envelope already exists remotely, it reports already synced; if no envelope exists, it creates the account row; if a different envelope exists, it refuses to overwrite it.
- Removed the manual `Sync Vault to Account` button and its unused callback. The screen now displays automatic sync status/progress text.
- When no local Vault exists, the existing account-first restore/check behavior remains unchanged.
- File changed: `app/src/main/kotlin/com.teddy.bpositive/ui/screens/SecureVaultScreen.kt`.
- Commits: `b8f60cec15a2faacd70d2cb8b4e64d8684a7294f` (automatic sync on local Vault discovery), `d425d700919e644b8d9444994982d6895aa32511` (remove manual sync action).
- User confirmed Build success and earlier verified that the same account's Vault becomes available on the second device after Account Sync. Automatic invocation is now included; do not claim exhaustive offline/race/device testing beyond what the user reported.
- No new database migration or production schema change. The existing Build 44 table and RLS remain in use.
- Limitations remain: only the encrypted Vault envelope syncs, not notes; different remote envelopes are never overwritten; no password-change/replacement/delete workflow or continuous background synchronization.


## Latest user-confirmed state

- Build 45 CodeAssist Build completed successfully (user-confirmed 2026-11-24).
- User reports automatic Vault account sync works. The live Supabase `public.vault_envelopes` table was read-only checked after the two-device test and contained one row.
- Latest source HEAD before documentation update: `d425d700919e644b8d9444994982d6895aa32511`.

## Build 46 — In-memory Vault Unlock Session Foundation (CodeAssist Build pending)

- Source implementation is committed to `main`; the user has not yet built or confirmed this Build.
- Added `security/VaultSessionManager.kt`, which holds a defensive copy of the unlocked 256-bit Vault master key in process memory only, associates it with one user ID, exposes defensive key copies to future consumers, and zeroes the held key on lock/replacement.
- Secure Vault Master Password and Recovery Key verification now place the successfully unwrapped key into the in-memory session manager, then zero the temporary returned byte array. No key is persisted by this manager.
- `MainActivity.onStop()` clears the in-memory Vault key when the app leaves the foreground; sign-out also clears it explicitly.
- Files changed/added:
  - `app/src/main/kotlin/com.teddy.bpositive/security/VaultSessionManager.kt` (new)
  - `app/src/main/kotlin/com.teddy.bpositive/ui/screens/SecureVaultScreen.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/MainActivity.kt`
- Commits: `14cfaf952dfff9c04c53c4a63838186dc25fc773`, `929784b47c6115d54080f1c6b776ce1dfdab0b4c`, `9cca657531fd6cfc3d05c3a1a999704190542e10`, `6e33afa529d71984a50c19c6a3d417e3b2cc91a8`.
- Current implementation is groundwork only: Note Lock is not yet wired; note fields remain plaintext in local storage, pending sync metadata and Supabase. No note encryption, note unlock UI, auto-lock timer, or database change is included in this Build.
- CodeAssist Build and device verification are pending user confirmation. Do not mark Build 46 successful until the user reports it.

## Build 46 — In-memory Vault Unlock Session Foundation (USER-CONFIRMED SUCCESSFUL)

- User explicitly confirmed CodeAssist Build success on 2026-11-24.
- Added `security/VaultSessionManager.kt`: holds a defensive copy of the unlocked 256-bit Vault master key in process memory only, scopes it to one user ID, exposes defensive copies for consumers, and zeroes the held key on lock/replacement.
- Master Password and Recovery Key unlock flows now store the unwrapped key in this in-memory session manager and clear the temporary returned byte array.
- `MainActivity.onStop()` clears the in-memory Vault key when the app leaves the foreground; sign-out clears it explicitly.
- Files: `security/VaultSessionManager.kt` (new), `ui/screens/SecureVaultScreen.kt`, `MainActivity.kt`.
- Commits: `14cfaf952dfff9c04c53c4a63838186dc25fc773`, `929784b47c6115d54080f1c6b776ce1dfdab0b4c`, `9cca657531fd6cfc3d05c3a1a999704190542e10`, `6e33afa529d71984a50c19c6a3d417e3b2cc91a8`.
- User-reported Build compilation success. No separate on-device lifecycle/key-memory test report was supplied.
- Limitations: Note Lock is not wired. Note title/content remain plaintext in local storage, pending sync metadata and Supabase. No note encryption, unlock UI, timed auto-lock or database change in Build 46.

## Build 47 — Versioned Encrypted Note Payload Codec (CodeAssist Build pending)

- Added `security/NoteEncryptionCodec.kt` as a small, versioned serialization boundary for encrypted note title/content fields.
- The codec requires an unlocked Vault session for the matching user ID, obtains a defensive master-key copy from `VaultSessionManager`, encrypts title and content independently with the existing AES-256-GCM field primitive (fresh nonce per field), and clears its key copy in `finally`.
- Decode authenticates both fields and fails closed on unsupported format, malformed ciphertext, wrong key or tampering. The JSON format stores `format_version`, encrypted `title`, and encrypted `content` only.
- New file: `app/src/main/kotlin/com.teddy.bpositive/security/NoteEncryptionCodec.kt`.
- Commit: `a0a73eab627b2f23d55df9ca09e3359d89e684b4`.
- Important: this codec is not yet connected to local note storage, sync queue, Supabase notes/conflict APIs, realtime, search or UI. Existing notes remain plaintext and Note Lock is not active. This Build must not be described as enabling note encryption.
- CodeAssist Build and runtime tests pending user confirmation.

## Build 47 — Versioned Encrypted Note Payload Codec (USER-CONFIRMED SUCCESSFUL)

- User explicitly confirmed CodeAssist Build success on 2026-11-24.
- Added `security/NoteEncryptionCodec.kt` as a versioned serialization boundary for encrypted note title/content fields.
- Codec requires an unlocked Vault session for the matching user ID, obtains a defensive master-key copy, encrypts title and content independently with the existing AES-256-GCM field primitive (fresh nonce per field), and clears its key copy in `finally`.
- Decode authenticates both fields and fails closed on unsupported format, malformed ciphertext, wrong key or tampering. JSON contains `format_version`, encrypted `title`, and encrypted `content`.
- New file: `app/src/main/kotlin/com.teddy.bpositive/security/NoteEncryptionCodec.kt`.
- Commit: `a0a73eab627b2f23d55df9ca09e3359d89e684b4`.
- User-reported Build compilation success. No separate runtime encryption/decryption or tamper test report was supplied.
- Limitations: codec is not connected to local note storage, pending sync, Supabase, realtime, conflict handling, search or UI. Existing notes remain plaintext; Note Lock is not active. No database change.

## Build 48 — Encrypted Local Notes Storage Migration Foundation (CodeAssist Build pending)

- Added `data/EncryptedLocalNotesStorage.kt` as an account-scoped encrypted snapshot store, separate from the currently active plaintext `LocalNotesStorage`.
- Uses `NoteEncryptionCodec` to encrypt each note title/content independently. Note ID, pin state, timestamps and color remain metadata in the local JSON snapshot; do not claim all note metadata is encrypted.
- Includes `stageLegacyMigration()`: copies the existing plaintext note cache into the encrypted snapshot, reads it back and checks full Note equality before reporting staged. It deliberately does not delete or overwrite the original plaintext cache.
- Includes read/write and encrypted-snapshot existence APIs. Decryption requires the matching account's unlocked Vault session.
- New file: `app/src/main/kotlin/com.teddy.bpositive/data/EncryptedLocalNotesStorage.kt`.
- Commit: `b017c8af3d52d1bb0e8713ce8826a361b85c10e7`.
- Important: this is a migration foundation only and is not yet connected to the active Notes UI, normal local save/load, pending sync queue, Supabase, realtime or conflict handling. Active notes remain in the existing plaintext cache and cloud table. The plaintext source is intentionally preserved.
- No Supabase schema/migration or paid-plan changes. CodeAssist Build and runtime migration tests pending user confirmation.

## Build 47 — Versioned Encrypted Note Payload Codec (USER-CONFIRMED SUCCESSFUL)

- User explicitly confirmed CodeAssist Build success on 2026-11-24.
- Added `security/NoteEncryptionCodec.kt` as a versioned serialization boundary for encrypted note title/content fields.
- Codec requires an unlocked Vault session for the matching user ID, obtains a defensive master-key copy, encrypts title and content independently with existing AES-256-GCM field primitive (fresh nonce per field), and clears its key copy in `finally`.
- Decode authenticates both fields and fails closed on unsupported format, malformed ciphertext, wrong key or tampering. JSON contains `format_version`, encrypted `title`, and encrypted `content`.
- New file: `app/src/main/kotlin/com.teddy.bpositive/security/NoteEncryptionCodec.kt`; commit `a0a73eab627b2f23d55df9ca09e3359d89e684b4`.
- User-reported Build compilation success; no separate runtime/tamper test report. Codec not connected to active storage/sync/UI; notes remain plaintext. No database change.


## Build 48 — Encrypted Local Notes Storage Migration Foundation (USER-CONFIRMED SUCCESSFUL)

- User explicitly confirmed CodeAssist Build success on 2026-11-24.
- Added `data/EncryptedLocalNotesStorage.kt`, an account-scoped encrypted snapshot store using `NoteEncryptionCodec` for note title/content. Note ID, pin state, timestamps and color remain metadata.
- `stageLegacyMigration()` copies the existing plaintext cache into encrypted storage, decrypts it again and checks complete Note equality before reporting staged. It does not delete or overwrite the original plaintext cache.
- New file: `app/src/main/kotlin/com.teddy.bpositive/data/EncryptedLocalNotesStorage.kt`; commit `b017c8af3d52d1bb0e8713ce8826a361b85c10e7`.
- User-reported Build compilation success only; no migration runtime result reported. The class is not yet connected to active Notes UI/save/load/sync. Active local and cloud notes remain plaintext. No Supabase migration or paid-plan change.


## Build 49 — Secure Vault Local Notes Migration Action (USER-CONFIRMED SUCCESSFUL)

- Added an explicit action to the Secure Vault Unlock screen: `Prepare Encrypted Local Copy`.
- The action requires the matching account's Vault to be unlocked in `VaultSessionManager`, then invokes `EncryptedLocalNotesStorage.stageLegacyMigration()` for the current user.
- Displays progress and result states: verified encrypted copy with note count, existing snapshot, no legacy local notes, or safely stopped error.
- Original plaintext local notes are not deleted or overwritten. This Build does not cut over the active Notes UI or change the Supabase notes table; cloud notes remain plaintext.
- File changed: `app/src/main/kotlin/com.teddy.bpositive/ui/screens/SecureVaultScreen.kt`.
- Commit: `b1188192b3f43078d5d84e1d1d758755a06a22f2`.
- User confirmed successful CodeAssist Build and reported that 10 notes were staged in the encrypted local copy. Original plaintext notes remain preserved.

## Build 50 — Encrypted Offline Sync Queue Migration Preparation (USER-CONFIRMED BUILD SUCCESS)

- Added `data/EncryptedNoteSyncQueue.kt`, an account-scoped encrypted snapshot for pending note sync records. Note title/content are encrypted through `NoteEncryptionCodec`; operation, local/client IDs, pin state and cloud metadata remain queue metadata.
- Added `stageLegacyMigration()`: requires the matching user's Vault to be unlocked, copies pending legacy queue records, decrypts and compares each title/content and key metadata against the original, persists the encrypted snapshot, then reads it back and authenticates it again.
- Added `loadStagedRecords()` to read/decrypt the staged snapshot only while the matching Vault is unlocked.
- Added a Secure Vault screen action, `Prepare Encrypted Offline Queue`, with progress/result feedback.
- The original plaintext queue is deliberately preserved. This is a staged migration only: `NoteSyncRepository` and existing queue producers/consumers still use the legacy queue. No active queue cutover, local notes storage cutover, Vault startup gate, cloud note encryption, or Supabase schema change is included.
- Files added/changed: `app/src/main/kotlin/com.teddy.bpositive/data/EncryptedNoteSyncQueue.kt`, `app/src/main/kotlin/com.teddy.bpositive/ui/screens/SecureVaultScreen.kt`.
- Implementation commits: `6a533217663e4c8698e3dfa7b435dcc19e7f0b16` (encrypted queue migration class), `c6dce0f91ac66dd744b6adbdfd94d66c595b88e9` (Vault UI action), `db6a4e124b8f19f3563519d1764b94108976baef` (strict comparison against legacy records).
- No Supabase migration, production database mutation or paid-plan change.
- User explicitly confirmed CodeAssist Build success on 2026-11-24. Runtime encrypted-queue migration execution/count has not yet been reported.
- The active sync queue is still plaintext; this milestone only prepares a separate encrypted snapshot. Do not describe active sync as encrypted until a later cutover is implemented.

## Build 51 — Encrypted Offline Queue Migration Integrity Hardening (USER-CONFIRMED SUCCESSFUL)

- Hardened `EncryptedNoteSyncQueue.stageLegacyMigration()` so an existing encrypted snapshot is compared against the complete current legacy pending queue. If queue records have changed, the encrypted snapshot is refreshed instead of incorrectly reporting it as already present.
- Equality verification covers decrypted title/content, local note ID, client ID, operation, pin state, cloud note ID and cloud updated timestamp, both before persistence and after reading the snapshot back.
- Snapshot validation rejects missing/invalid metadata, unsupported operation values, invalid IDs, and unauthenticated or malformed encrypted fields. `loadStagedRecords()` validates the snapshot before decoding records.
- The legacy plaintext queue remains active and unchanged; the encrypted queue remains a staged migration copy. No active queue cutover, local note-storage cutover, Supabase schema change, production DB mutation or paid-plan change.
- File changed: `app/src/main/kotlin/com.teddy.bpositive/data/EncryptedNoteSyncQueue.kt`.
- Implementation commit: `c741818ab6c1551586cbe0b0f651c62c0fe1a8a8`.
- **User confirmation (2026-11-24):** “Successfully Build Completed!” CodeAssist Build completed successfully. Runtime migration execution/count was not separately reported.

## Build 52 — Encrypted Local Notes Migration Integrity Hardening (USER-CONFIRMED SUCCESSFUL)

- Updated `EncryptedLocalNotesStorage.stageLegacyMigration()` to compare an existing encrypted snapshot against the complete current plaintext note cache, including title, content, ID, pin state, timestamps and color.
- If the source notes changed, the encrypted snapshot is refreshed and read back for full Note equality verification. The UI distinguishes a newly staged copy from a refreshed copy.
- Added strict snapshot format, required-field, metadata-type, ID and color validation plus authenticated encrypted-field decoding before a snapshot is loaded or accepted as matching.
- If the plaintext source is empty but an encrypted snapshot already exists, the existing snapshot is retained rather than replaced with an empty copy. The plaintext source is not modified.
- Original plaintext notes remain preserved. This is still a staged migration only: active Notes UI/storage, sync queue, Supabase notes, realtime and conflict flows have not been switched to encrypted storage.
- Files changed:
  - `app/src/main/kotlin/com.teddy.bpositive/data/EncryptedLocalNotesStorage.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/ui/screens/SecureVaultScreen.kt`
- Implementation commits: `6ff3797f74c06381c9270b96eadd7a625386d436`, `042390c568159a0875f1a01ccc35f308cb88a6ce`.
- **User confirmation (2026-11-24):** After updating CodeAssist to 3.48.2, the user reported “Successfully Build Completed!” This confirms the CodeAssist Build completed; encrypted migration runtime checks were not separately reported.
- No Supabase migration, production database mutation or paid-plan change.

## Build 53 — Complete Local Note Snapshot Encryption (USER-CONFIRMED SUCCESSFUL)

- Extended `NoteEncryptionCodec` with full-`Note` encode/decode operations. The complete note object—including ID, title, content, pin state, creation/update timestamps and color—is serialized into one AES-256-GCM authenticated encrypted payload.
- Updated `EncryptedLocalNotesStorage` to write snapshot format version 2. Each stored note entry now contains only the encrypted full-note payload; note metadata is no longer exposed as plaintext in the staged encrypted snapshot.
- Kept format-version-1 snapshot reading and validation for backward compatibility. When the plaintext legacy cache is available, running the migration refreshes a valid version-1 snapshot into version 2 and verifies the full decoded `Note` list after persistence.
- Existing encrypted snapshots are authenticated before replacement. If the legacy plaintext cache is empty, an existing valid snapshot is preserved rather than overwritten.
- The plaintext legacy cache remains unchanged. This is still migration preparation only: active Notes UI/storage, pending sync queue, Supabase notes, realtime and conflict flows are not switched to encrypted storage.
- Files changed:
  - `app/src/main/kotlin/com.teddy.bpositive/security/NoteEncryptionCodec.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/data/EncryptedLocalNotesStorage.kt`
- Implementation commits:
  - `3fde3d500663ea184d67278a79d239fef4a5324b` — full-note encryption codec.
  - `602b9b5301a11f72ef53bb885e9f3b03378c2240` — version-2 encrypted local snapshot and version-1 compatibility.
  - `712099641e95c605c1a9caacdce4f8f6c44978d1` — clarify full-note encryption behavior in the storage documentation comment.
- No Supabase migration, production database mutation or paid-plan change.
- **User confirmation (2026-11-24):** The user explicitly reported “Successfully Build Completed!” This confirms the CodeAssist Build compiled successfully.
- Runtime migration execution, encrypted snapshot version-2 contents, version-1 upgrade behavior and repeated-run idempotency were not separately reported or verified; do not claim those runtime tests passed.


## Build 54 — Encrypted Local Snapshot Atomic Write & Integrity Hardening (USER-CONFIRMED SUCCESSFUL)

- Hardened `EncryptedLocalNotesStorage.saveNotes()` so a new encrypted snapshot is first written to a temporary per-account preference key, read back, authenticated, decrypted and compared with the complete input Note list before it replaces the last known-good snapshot.
- The final promotion writes the verified snapshot and removes the temporary key in one SharedPreferences commit. If staging or verification fails, the temporary key is cleaned up and the existing canonical snapshot is not intentionally replaced.
- Added pre-write validation for positive note IDs, nonblank colors and duplicate IDs.
- Strengthened snapshot validation for both format version 2 and legacy version 1: exact document/entry structure, authenticated payload decoding, valid note identity/color and duplicate-ID rejection.
- This remains encrypted snapshot storage/migration hardening only. The active Notes UI/storage, sync queue, Supabase notes, realtime and conflict flows are not switched to encrypted storage.
- File changed: `app/src/main/kotlin/com.teddy.bpositive/data/EncryptedLocalNotesStorage.kt`.
- Implementation commit: `5b5bfc620a1663db061bc172044680e246434a06`.
- No Supabase migration, production database mutation or paid-plan change.
- **User confirmation (2026-11-24):** The user explicitly reported “Successfully Build Completed!” after pulling Builds 53 and 54 together. This confirms CodeAssist compilation for both Builds.
- Device/runtime migration checks remain unreported; do not claim the atomic-write failure paths or on-device migration were runtime-tested.


## Builds 55–57 — Read-only Encrypted Migration Audit (USER-CONFIRMED CODEASSIST BUILD SUCCESS)

These three closely related milestones are intentionally grouped into one source/documentation commit and one CodeAssist Build to reduce repeated build cycles.

### Build 55 — Local encrypted snapshot audit
- Added `EncryptedLocalNotesStorage.inspectMigration()`, a read-only audit that requires the matching Vault to be unlocked, authenticates/decrypts the stored snapshot, and compares the complete decoded Note list with the current legacy plaintext source.
- Reports matching source, mismatch counts, missing encrypted snapshot, or an empty legacy source where source comparison is unavailable. It does not stage, refresh, overwrite or delete data.

### Build 56 — Encrypted offline queue audit
- Added `EncryptedNoteSyncQueue.inspectMigration()`, a read-only audit that authenticates/decrypts the staged queue and compares complete SyncRecord values with the current legacy pending queue.
- Reports matching source, mismatch counts, missing encrypted snapshot, or an empty legacy queue where source comparison is unavailable. It does not stage, refresh, overwrite or delete data.

### Build 57 — Secure Vault verification UI
- Added a `Verify Encrypted Copies` action to Secure Vault. It runs both audits and displays separate local-note and offline-queue results.
- The action requires the matching Vault to be unlocked. It is read-only; it does not activate encrypted storage or queue cutover.
- Files changed:
  - `app/src/main/kotlin/com.teddy.bpositive/data/EncryptedLocalNotesStorage.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/data/EncryptedNoteSyncQueue.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/ui/screens/SecureVaultScreen.kt`
- No Supabase migration, production database mutation, paid-plan change, active Notes storage cutover or active sync-queue cutover.
- **User confirmation (2026-11-24):** The user explicitly reported “Successfully Build Completed!” after pulling this grouped commit. This confirms CodeAssist compilation for Builds 55–57.
- On-device audit execution and its actual result remain unreported; do not claim the local-note or queue comparison passed at runtime.


## Builds 58–59 — Background Audit Execution & Strict Queue Validation (single grouped commit; USER-CONFIRMED CODEASSIST BUILD SUCCESS)

### Build 58 — Run encrypted migration audits off the UI thread
- Moved the Secure Vault's read-only local-note and encrypted-queue audit work to `Dispatchers.IO` using the Compose coroutine scope.
- Preserves progress feedback and reports unexpected audit execution errors; the coroutine restores the loading state in `finally`.
- This prevents snapshot authentication/decryption and full-list comparisons from blocking Compose UI rendering during the audit.
- Audit remains read-only and requires the matching Vault to be unlocked.

### Build 59 — Strict encrypted queue snapshot validation
- Requires the encrypted queue snapshot root and each record to contain exactly the supported fields.
- Rejects duplicate local note IDs and duplicate client IDs.
- Requires a supported non-null UPSERT/DELETE operation, valid IDs, nonblank client IDs, valid optional cloud IDs/timestamps, and authenticated encrypted fields.
- The stricter validation is used by both staged-record loading and migration comparison; no existing queue data is rewritten by this change.
- Files changed:
  - `app/src/main/kotlin/com.teddy.bpositive/ui/screens/SecureVaultScreen.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/data/EncryptedNoteSyncQueue.kt`
- No Supabase migration, production database mutation, paid-plan change, active Notes storage cutover or active sync-queue cutover.
- **User confirmation (2026-11-24):** The user explicitly reported successful CodeAssist Build after pulling the grouped commit. Runtime audit execution/results were not separately reported.


## 15. Build 60 — Per-note Secure Vault Lock/Unlock integration (USER-CONFIRMED CODEASSIST BUILD SUCCESS)

- Connects the Note Card Action Dialog to the existing Vault master key and AES-GCM full-note codec.
- Locked notes are saved locally as encrypted full-note payloads; plaintext title/content and note metadata are not written into the local note snapshot.
- Cloud sync uses existing `notes.title` and `notes.content` text fields with the versioned `teddy-note-encrypted-v1:` ciphertext envelope. No schema migration or bulk rewrite is needed.
- Cloud hydration recognizes encrypted envelopes and displays a masked placeholder.
- Editing and pinning protected notes refresh the encrypted payload before persistence and sync.
- Protected note content is masked in Compose state when the activity enters the background.
- **User confirmation (2026-11-24):** The user explicitly confirmed that all relevant functionality works in runtime, including per-note Lock/Unlock, encrypted cloud synchronization, and the other Secure Vault workflows.


## 16. Builds 61–63 — Per-note Secure Vault recovery, safety and sync integration (single grouped commit; USER-CONFIRMED CODEASSIST BUILD SUCCESS)

These three milestones are grouped into one source commit so the user pulls once and runs one CodeAssist Build.

### Build 61 — Protected-note action and lifecycle safeguards
- Masked notes no longer expose Pin or Share actions in the Note Card Action Dialog; Delete and Unlock remain available.
- MainActivity rejects pin changes, edits and sharing for masked notes or protected notes when the matching Vault session is not unlocked. This prevents placeholder text from being re-encrypted over the original protected payload.
- When the app enters the background, protected notes are masked and an open protected-note editor is closed with its plaintext editor state cleared before the local note snapshot is saved.

### Build 62 — Encrypted Trash and conflict recovery
- Trash and conflict-copy cards detect the versioned encrypted cloud envelope and display a safe generic description instead of ciphertext.
- Restoring a protected note from Trash or a protected conflict copy reconstructs a masked local Note with its encrypted payload, rather than treating the envelope string as ordinary plaintext note content.
- Existing unencrypted Trash and conflict-copy records retain their previous restore behavior.

### Build 63 — Secure Vault navigation and accurate migration status
- Android system Back is handled inside Secure Vault and invokes the same callback as the visible Back button. This allows a pending per-note Lock/Unlock action to resume after the user returns from Vault.
- Updated Secure Vault explanations to distinguish individually locked notes from the separate legacy encrypted-snapshot migration and the staged encrypted queue copy.
- No new dependency or Supabase schema change.

- Files changed:
  - `app/src/main/kotlin/com.teddy.bpositive/MainActivity.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NotesScreen.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/ui/components/NoteComponents.kt`
  - `app/src/main/kotlin/com.teddy.bpositive/ui/screens/SecureVaultScreen.kt`
- No Supabase migration, production database mutation, destructive operation or paid-plan change.
- **User confirmation (2026-11-24):** The user confirmed that all work is functioning. This includes the protected-note safeguards, background masking, Trash/conflict recovery and encrypted cloud synchronization.


## 17. Build 64 — Final Secure Vault account-session isolation (USER-CONFIRMED CODEASSIST BUILD SUCCESS)

This is the final planned implementation build for the current per-note Secure Vault task.

- Wipes the in-memory Vault master key when the authenticated account changes or signs out. The previous account's key cannot remain resident for a later account session.
- On account-ID changes, clears any pending per-note Lock/Unlock action and resets open-note/editor state, including plaintext title/content. This prevents a pending action or editor draft from an earlier account being applied to a different account with a colliding local note ID.
- Reuses the existing `VaultSessionManager.lock()` implementation; no new cryptographic primitive, dependency, storage format or cloud schema is introduced.
- Existing app-background key clearing remains unchanged.
- File changed: `app/src/main/kotlin/com.teddy.bpositive/MainActivity.kt`.
- No Supabase migration, production database mutation, destructive operation or paid-plan change.
- **User confirmation (2026-11-24):** The user explicitly confirmed that all work is functioning, including the final Secure Vault/account-session behavior. Runtime verification is considered user-confirmed.
- **Final status (2026-11-24):** Builds 60–64 are CodeAssist-build-confirmed and the user has confirmed that all work functions correctly. The planned per-note Secure Vault implementation and verification task is complete.


## 18. Teddy Messaging — Phase 1: Private Text Chat (PLANNING BASELINE; NOT IMPLEMENTED)

### 18.1 User-approved product scope

The user has approved planning for a new first-class Messaging capability in Teddy-WebApp. Initial release scope:
- One-to-one private text conversations only; group chat is not part of Phase 1.
- Discover registered users through username search.
- Any registered user may start a conversation with another registered user; no message-request/approval gate.
- Message states: Sent, Delivered and Read.
- Online status, Last Seen and live Typing Indicator.
- Delete for Me and Delete for Everyone.
- Push notifications when the recipient app is backgrounded or closed.
- Offline messages remain Pending locally and are sent automatically after connectivity returns.
- Message history is retained indefinitely unless a user deletes it.
- Initial transport/storage security is Supabase Auth + strict Row Level Security (RLS), not end-to-end encryption (E2EE). E2EE is a separately planned future capability.
- Visual direction: original, premium Teddy-WebApp messaging UI informed by WhatsApp/Telegram usability, without copying either product.

### 18.2 Mandatory architecture direction — future-ready foundation

**User's explicit requirement (2026-11-24): design the foundational architecture with future expansion in mind, so later features do not require rebuilding or damaging the core architecture.** Treat this as a standing acceptance criterion for every Messaging Build and future design decision.

Apply these rules:
1. Establish stable domain boundaries and clear separation between UI, domain/use-case logic, repositories/data sources, local persistence, network/realtime transport and notification delivery.
2. Model conversations and participants independently of the current one-to-one UI. Phase 1 exposes only direct conversations, but the data model must not make group conversations, multiple participants, roles or future conversation types structurally impossible.
3. Use stable UUID identities, explicit constraints, foreign keys, idempotency keys and deterministic message ordering. Never rely on client timestamps or UI-only validation for integrity.
4. Prefer additive, forward-compatible database migrations. Do not rename/drop/reinterpret existing production columns or tables to accommodate Messaging. Do not rewrite Notes, Secure Vault or notification data.
5. Version externally persisted payloads and local queue records where format evolution is possible. Define compatibility and migration behavior before changing a persisted format.
6. Keep ephemeral realtime signals (typing/presence) separate from durable message history. Presence/typing transport must be replaceable without changing the message domain model.
7. Keep delivery receipts, read state, per-user deletion and global deletion semantics explicit and independently extensible. Avoid encoding multiple meanings in one status field.
8. Keep push delivery server-side through Edge Functions/FCM; never put service credentials in the Android client. Reuse the existing device-token infrastructure only after checking its actual contracts.
9. Keep local outbox/retry behavior durable, account-scoped and idempotent. Account switching/sign-out must not leak queued plaintext or send one user's pending messages under another account.
10. Apply strict RLS and database constraints as the security boundary. Client-side checks are UX only. Verify every policy against the intended participant-level access model.
11. Plan indexes, cursor pagination and retention semantics from the first schema version. History has no automatic TTL under the approved product scope.
12. Design for future media attachments, voice/video calls, group conversations, message reactions/replies/forwarding, multi-device sessions and E2EE as possible later capabilities, but do not prematurely implement them in Phase 1. Use extensible metadata/attachment boundaries only where justified; avoid speculative columns or complex abstractions without a concrete use.
13. Every Build must preserve existing Notes, Secure Vault, authentication, profile, notification and update workflows. Include regression checks appropriate to touched shared components.
14. Before each implementation Build, inspect the live source and schema. The plan is a design baseline, not permission to assume the existing repository matches it.

### 18.3 Verified current Supabase baseline (2026-11-24; read-only inspection)

Project: `axklmzuepqcbxsbxvtdn`, region `ap-northeast-1`, status `ACTIVE_HEALTHY`, PostgreSQL `17.11.0.019`.

Current public tables observed:
- `notes`
- `note_conflicts`
- `profiles`
- `device_tokens`
- `admin_users`
- `notification_history`
- `user_notifications`
- `user_topic_subscriptions`
- `notification_delivery_attempts`
- `vault_envelopes`

RLS was reported enabled for each table in the live table listing. Existing `profiles` fields include `id`, `full_name`, `username`, `bio`, `avatar_url`, `created_at`, `updated_at`; the live listing showed five profile rows and a case-insensitive unique username index is recorded in the existing project context. Username search must use only approved public profile fields and must not expose email, auth metadata or private account data.

Latest migration observed at this inspection: `20261124044324_build44_secure_vault_envelopes`. No Messaging table, migration or feature was observed in this public-table inventory. This was a read-only inspection; no Supabase schema, data, policy, function, billing or plan changes were made.

### 18.4 Proposed domain and data model (design proposal; not yet migrated)

The following is the starting design to validate against the live schema and Supabase documentation before Build 01. It is not yet a final SQL contract.

- `conversations`: conversation identity, type and lifecycle timestamps. Direct conversations need a database-enforced canonical participant-pair identity so two clients cannot create duplicate one-to-one threads. The design should leave a safe path for future group conversations.
- `conversation_members`: participant membership, membership role/state and per-user read/delivery cursor metadata. Unique membership constraint on conversation + user.
- `messages`: durable message identity, conversation, sender, text body, server timestamps, client-generated idempotency key, edit/delete metadata and future-compatible message kind/version fields. Add deterministic cursor-pagination indexes.
- `message_receipts`: per-recipient delivered/read timestamps, uniquely keyed by message + recipient. Do not confuse FCM acceptance with message delivery.
- `message_deletions`: per-user Delete for Me tombstones, independent of global Delete for Everyone.
- `user_push_tokens`: **do not create by default**; inspect and reuse the existing `device_tokens` installation-scoped registry if it meets messaging notification requirements. Extend it only through a safe additive migration if necessary.

Design details that must be resolved before migration:
- A robust canonical direct-pair uniqueness strategy and transaction-safe conversation creation.
- Whether membership cursors are sufficient for efficient Delivered/Read progression or individual receipt rows are required for every recipient/message; the selected semantics must remain exact.
- Sender-only authorization and recipient visibility for Delete for Everyone, with a durable tombstone/placeholder so message ordering and synchronization remain stable.
- Safe profile search query/index and minimum public fields.
- Exact RLS policies for conversation membership, message reads/inserts/updates, receipts and deletion records; no broad authenticated-only policies.
- Realtime publication/channel authorization and lifecycle cleanup.
- Local database/outbox structure and client-message deduplication.
- Push event design, Edge Function authorization and FCM payload privacy.
- Migration rollback/recovery strategy that does not require destructive production changes.

### 18.5 Realtime and message semantics

- Sent: server confirms the message row was committed.
- Delivered: recipient device receives and durably acknowledges the message; merely publishing a Realtime event or receiving FCM acceptance is not sufficient.
- Read: recipient opens the conversation and advances the read state for eligible messages.
- Typing: ephemeral Realtime Broadcast with throttling/debounce and expiry; do not persist every keystroke.
- Online/Last Seen: Realtime Presence plus app lifecycle/connectivity handling; persist only the minimum Last Seen data required by product behavior.
- Offline send: persist an account-scoped local outbox item with a stable client-generated idempotency key; retry on reconnect and after process restart; reconcile server acknowledgement without duplicate messages.
- Delete for Me: create a per-user tombstone; other participants retain their message.
- Delete for Everyone: authorize the sender's action, preserve a synchronized deleted-message placeholder/tombstone, and do not physically remove data in a way that breaks other participants' history.
- Push: use existing server-side FCM infrastructure where compatible; suppress self-notifications and avoid leaking message body in notification payloads unless the product later explicitly chooses that behavior.

### 18.6 Messaging implementation roadmap (15 Builds)

**Phase A — Foundation**
- Build 01: finalize and document the future-ready Messaging database contract, constraints, indexes, RLS and Realtime strategy; prepare migration for review before applying it.
- Build 02: Messaging navigation, conversation-list and chat-screen UI foundation.
- Build 03: username-based user search, safe public profile preview and start-chat entry point (source + Supabase RPC deployed; CodeAssist Build confirmation pending).

**Phase B — Core private text chat**
- Build 04: transaction-safe one-to-one conversation creation and membership.
- Build 05: text composer, message bubble UI, timestamps, keyboard and scroll behavior.
- Build 06: durable message sending/receiving, Realtime updates, optimistic UI, deduplication and network error handling.

**Phase C — Delivery and presence**
- Build 07: Sent/Delivered/Read receipts.
- Build 08: Online status and Last Seen.
- Build 09: Typing Indicator.

**Phase D — History and controls**
- Build 10: conversation list ordering, previews and unread counts.
- Build 11: indefinite message history with cursor pagination and date grouping.
- Build 12: Delete for Me and Delete for Everyone.

**Phase E — Reliability and notifications**
- Build 13: durable offline outbox and automatic retry.
- Build 14: background/closed-app push notifications using FCM and Edge Functions.

**Phase F — Hardening**
- Build 15: RLS/security audit, multi-account isolation, realtime lifecycle, offline/online regression, large-history performance and final documentation.

Build grouping may be adjusted to reduce repeated CodeAssist build cycles, but each logical change-set must be reviewed and user-confirmed before being marked complete. Do not report a CodeAssist Build successful until the user confirms it.

### 18.7 Build 01 — Messaging database foundation (APPLIED; DATABASE SMOKE TEST PASSED; CLIENT/REALTIME VERIFICATION PENDING)

- User approved the exact Build 01 migration after security review. The membership helper derives caller identity exclusively from `auth.uid()`; it accepts no arbitrary user UUID.
- Applied migration: `20261124110532_build01_messaging_foundation`. Supabase migration history now has 20 entries; the prior 19 entries remain unchanged. No history reset, repair or fabrication occurred.
- All five additive tables exist: `conversations`, `conversation_members`, `messages`, `message_receipts`, `message_deletions`. RLS is enabled on all five; eight public Messaging policies and two Realtime channel policies are present.
- All five tables are in `supabase_realtime`; existing `notes` and `profiles` publication entries remain.
- Helper signature verified safe; no old arbitrary-user signature. Authenticated grants match intended table operations; no `anon` grants on the five Messaging tables.
- A rollback-only SQL smoke test using two existing authenticated user identities passed: direct conversation creation, message send, recipient read, receipt row, Delete for Me isolation, sender visibility and sender-only Delete for Everyone. Transaction rolled back; no test data remains.
- Supabase Health advisor: no findings. Security advisor: three expected warnings for intentionally authenticated SECURITY DEFINER RPC endpoints and one unrelated leaked-password-protection setting. Performance advisor: seven unindexed-FK INFO findings plus unused-index INFO findings; review before optimization.
- Realtime project-wide `Allow public access` setting could not be inspected with available tools. Private Broadcast/Presence enforcement is not verified. Do not change this global setting without assessing Notes/Profile channels and explicit approval; Messaging clients must use `private: true`.
- Full separate-session Android client tests, Realtime channel authorization tests, and Notes/Profile/notification/Vault regression tests remain pending. Build 01 is database-applied, but the overall Messaging foundation is not marked fully complete until these gates pass.
- No Android source code changed; no CodeAssist Build has been run or claimed.
## 18.9 Messaging Build 02 — Navigation and UI foundation (USER-CONFIRMED BUILD SUCCESSFUL)

- Added a dedicated **Chats** destination to the existing bottom navigation. The destination is named `Messages` internally to keep routing explicit.
- Added `app/src/main/kotlin/com.teddy.bpositive/ui/screens/MessagingScreens.kt` with:
  - Messaging conversation-list landing UI and empty state.
  - Search-field visual placeholder (not connected to search yet).
  - Reusable one-to-one chat-screen UI shell with participant header, message area and composer callback; it is not connected to a backend or a conversation route yet.
- Connected the Messages destination in `MainActivity.kt`. The empty-state Find people action currently explains that username search arrives in Build 03; it does not perform a search.
- Updated `AppBottomNavigationBar.kt` to show Chats alongside the existing destinations. Admin remains conditional on verified admin access.
- Source commits, in order:
  - `6fe7102a5517ebd164b7fde27ad5fc528be2a795` — add Messaging UI foundation.
  - `07e0d7346ee42f571a4522865db233367bd71648` — connect Messages route.
  - `d80f0fd51e46d8fe5718ddad6635b80d8229e7df` — add Chats navigation item.
  - `6a92639453ffc126bb27bb4ca5961df8c574c22f` — adjust chat screen content sizing.
- No Supabase schema, data, policies, Realtime settings, Edge Functions, billing or plan changes were made.
- **Build status:** **User confirmed CodeAssist Build completed successfully on 2026-11-24.**
- **Verification approach:** The remaining Notes/Profile/notification/Vault regression checks are deferred to the consolidated integration/hardening pass rather than being used to block forward progress on this UI-only Build. The earlier two-account Notes/Profile private-channel isolation test remains user-confirmed as passed. Keep global Realtime **Allow public access to channels** Enabled until the relevant private-channel migration and regression coverage is complete.

## 18.8 Realtime Compatibility Build — Notes/Profile private channel migration (BUILD AND BASIC DEVICE REALTIME TEST CONFIRMED; CROSS-ACCOUNT/REGRESSION TESTS PENDING)

- User confirmed the Supabase Dashboard setting **Realtime → Allow public access to channels** is currently Enabled. It has deliberately not been toggled because the existing Android Notes/Profile Realtime client previously joined one shared public topic.
- Live GitHub main before this change was 40575b4a69fb55cd79f349de1b6c495858240a9c. Supabase project was ACTIVE_HEALTHY; Build 01's five Messaging tables and migration remained present.
- Added additive migration supabase/migrations/20261124130000_build_realtime_private_notes_channel.sql. Applied successfully to Supabase as migration 20261124112553: build_realtime_private_notes_channel.
- The migration adds one SELECT-only policy, realtime.messages.notes_private_user_channel_receive, allowing authenticated channel authorization only when realtime.topic() = 'notes:user:' || auth.uid() and the extension is Broadcast/Presence. It adds no INSERT permission. Existing public.notes and public.profiles table RLS policies are unchanged; Postgres Changes row delivery continues to be governed by those table policies.
- Updated app/src/main/kotlin/com.teddy.bpositive/data/SupabaseRealtimeSubscription.kt: channel config now sets private = true; topic changed from realtime:public:notes to realtime:notes:user:<current-user-id>. Notes and Profiles remain subscribed together in the same user-scoped private channel.
- Source commit: 599785722b470b39c84e4c4800873b93bb798737.
- Migration-file commit: 8daaabc9516aed30538b92a236a975a28a7c6377.
- Architecture documentation commit: 615816ee4e896ff59675b81986ac0bbf6f665451.
- Live policy verification confirms the new policy exists alongside the two existing Messaging policies. Migration history shows version 20261124112553. No existing Notes/Profile policies, publication entries, application data, or global Realtime settings were changed.
- **User confirmation (2026-11-24):** CodeAssist Build completed successfully. This confirms the Android source compiles/builds in the user's environment; it does not yet prove live Realtime authorization or event delivery.
- **User confirmation (2026-11-24):** After the successful CodeAssist Build, the user tested the app and confirmed the Notes/Profile Realtime behavior works on-device. Treat this as a basic functional device test, not proof of cross-account isolation or every regression path.
- **User confirmation (2026-11-24):** The user completed the two-account isolation test and confirmed the expected result: Account B could not enter Account A's private channel, while Account B's own Notes and Profile continued to work normally. Record this as a user-confirmed cross-account isolation pass for the Notes/Profile private Realtime channel.
- **Pending regression checks:** Notes CRUD and offline sync/conflict recovery; Profile refresh/edit; notifications; Secure Vault regression. Keep global Allow public access Enabled until these regression checks pass and the remaining private Realtime paths have been assessed.
- **Do not disable global Allow public access yet.** The cross-account isolation test passed, but the remaining regression checks are still pending. Do not mark Messaging Build 01 fully complete before those checks.


## 18.10 Messaging Build 03 — Username Search & User Discovery (USER-CONFIRMED BUILD SUCCESSFUL)

- Live pre-change GitHub main HEAD: `c040e4bf3ac986beeb9fcc8fa53606b90fed239b`; Supabase project status: ACTIVE_HEALTHY.
- Inspected live public.profiles schema: id, full_name, username, bio, avatar_url, created_at, updated_at. Existing owner-only SELECT RLS policy and case-insensitive unique username index were preserved.
- Added authenticated RPC public.search_public_profiles(search_query, result_limit):
  - Requires auth.uid(); unauthenticated calls are rejected.
  - Requires at least two query characters; normalizes case and caps query length at 30.
  - Uses literal substring matching via strpos, not user-controlled SQL wildcard patterns.
  - Excludes the current user, returns at most 20 results, exact username matches first.
  - Returns only profile preview fields: id, full_name, username, bio, avatar_url; no email or auth metadata.
  - SECURITY DEFINER uses an empty search_path. EXECUTE revoked from PUBLIC, anon and authenticated before granting only to authenticated.
- Applied migration 20261124125311_build03_username_search. Verified function definition and ACL: authenticated execute=true; anon/public execute=false. Existing profile RLS policies were not changed.
- Added Android data/UserSearchApi.kt using the authenticated session and RPC endpoint, with one token-refresh retry.
- Added MessagingUserSearchScreen: username input, result cards, profile preview dialog and Start Chat entry point. Connected it to Messages. Start Chat identifies the selected user and points to Build 04; conversation creation is not yet wired.
- Source commits:
  - e102af55eb40f137a1544485de3bbac2b2655a4d — authenticated username search API.
  - 241e8e8ecb39bb6ce24085ce7b2230e1b3a7d3b9 — search and profile preview UI.
  - 387cb10b7980af5ee8b13e070842ee5b9127790f — connect search to Messaging navigation.
  - 4ea238d7f0112d07bb1e8a181acaf1554a7e3c37 — add applied migration file.
  - ab5b709b58caf6901651608bd0534e8e9cfee109 — Messaging database architecture documentation.
  - 5115783279e8c2eb852a7125e268465c8f8765e0 — changelog update.
- Supabase Health advisor returned no findings. Security advisor reports this authenticated SECURITY DEFINER RPC in the same expected warning category as existing Messaging RPCs, plus the pre-existing leaked-password-protection warning. Performance advisor still reports seven Messaging foreign-key index INFO findings and unused-index INFO findings.
- No existing profile rows, username values, owner policies, Messaging rows, Realtime settings, billing or plan settings were changed.
- **Build status:** User confirmed CodeAssist Build completed successfully on 2026-11-24. Username-search on-device functional verification is not separately recorded; do not infer it from compilation success.

## 18.11 Messaging Build 04 — One-to-One Private Conversation (USER-CONFIRMED BUILD SUCCESSFUL)

- User confirmed Messaging Build 03's CodeAssist Build completed successfully on 2026-11-24. This is a build confirmation only; it does not independently certify all username-search runtime cases.
- Verified live GitHub main before Build 04: HEAD `23fbc2ad992295f094d542da7f6b78104ff01856`; canonical project context and Messaging source were read before edits.
- Verified Supabase project `axklmzuepqcbxsbxvtdn` is `ACTIVE_HEALTHY`; migrations include Build 01, the private Notes/Profile Realtime migration and Build 03 username search.
- Inspected live `public.create_direct_conversation(p_other_user_id uuid)` definition:
  - Derives caller only from `auth.uid()`; rejects unauthenticated calls, null/self target and non-existent target users.
  - Canonicalizes the user pair and uses the existing partial unique index `conversations_direct_pair_unique`, preventing duplicate direct conversations.
  - Inserts/reactivates both memberships in the same database transaction and returns the existing or newly created conversation UUID.
  - Verified EXECUTE: authenticated=true, anon=false, PUBLIC=false.
- No database migration was needed. Build 01 already provides the required RPC, membership table, uniqueness constraint and RLS model. No production rows, schema, policies, Realtime settings, billing or plan settings were changed.
- Added `app/src/main/kotlin/com.teddy.bpositive/data/DirectConversationApi.kt`: authenticated RPC call with access-token refresh retry and response/error handling.
- Updated `MainActivity.kt`: selecting **Start chat** now calls the RPC; on success it opens the existing one-to-one Chat screen with the selected user's name and username. Returning from Chat goes back to Messages. The composer remains a UI shell; actual message sending is Build 05.
- Source commits:
  - `b398a1067b68836a9ee7217a6c34aebb0e3742ed` — add direct conversation creation API.
  - `752ba36d71bfdcd288efb9f6bacf189f14f009ec` — connect username discovery to private conversations.
- **Build status:** User confirmed CodeAssist Build completed successfully on 2026-11-24. Device-level conversation creation/opening remains to be verified separately.

## 18.12 Messaging Build 05 + Build 06 — Text Composer, Sending, History Load and Realtime Receive (ONE COMBINED COMMIT; USER-CONFIRMED BUILD SUCCESSFUL)

- User confirmed Build 04 completed successfully and explicitly requested Build 05 and Build 06 to be delivered together in a single Git commit.
- Live pre-change GitHub main HEAD: `3bcd5c77ea3228f2f7e9b578351d2b77fb2ea405`; latest Build 04 source and canonical project context were fetched before editing.
- Live Supabase project `axklmzuepqcbxsbxvtdn` remains `ACTIVE_HEALTHY`; current migration history includes Build 01 messaging foundation, private Notes/Profile channel migration and Build 03 username search.
- Reused the existing authenticated `public.send_message(p_conversation_id uuid, p_client_message_id uuid, p_body text)` RPC. Live definition verified: caller identity from `auth.uid()`, active conversation membership required, body length 1–10,000, sender/client UUID idempotency, creates recipient receipt row and advances conversation last-message timestamp. No database schema migration or production data change required.
- Existing `messages` table has RLS-enabled member-only SELECT policy, excluding messages hidden by the requesting user's Delete-for-Me tombstone. The table is already included in `supabase_realtime`.
- Supabase Realtime changelog reviewed. No relevant current breaking change prevents this implementation. Reused Build 01's existing private-channel authorization policies: the client joins `realtime:messaging:conversation:<conversation-uuid>` with `private=true`; the database authorization policy verifies active conversation membership. Message row visibility also relies on the existing messages-table RLS policy. No global Realtime setting or database policy was changed.
- Added `data/MessagingRepository.kt`:
  - Authenticated REST read of the latest 100 messages in ascending order, protected by existing table RLS.
  - Authenticated `send_message` RPC call with a fresh client UUID for idempotency.
  - Access-token refresh retry and network/API error reporting.
- Added `data/MessagingRealtimeSubscription.kt`:
  - Conversation-filtered Postgres Changes INSERT listener on `public.messages`.
  - Heartbeat, reconnect backoff, session refresh handling and stop-on-screen-exit lifecycle.
  - Realtime events trigger an RLS-protected REST refresh rather than trusting event payload as authoritative message content.
- Updated `MessagingScreens.kt`: message bubbles aligned by sender, timestamps, loading/empty states, manual refresh, text input up to 10,000 characters and sending progress.
- Updated `MainActivity.kt`: loads message history when a conversation opens, starts/stops the scoped Realtime subscription, sends through the RPC and refreshes history after success. Session/conversation guards discard stale callbacks.
- Offline durable outbox/retry is deliberately deferred to Build 13. Delivered/Read receipts, typing, presence and push notifications remain later builds.
- **Build status (2026-11-24): USER-CONFIRMED CODEASSIST BUILD SUCCESSFUL.** The user explicitly confirmed that the combined Build 05 + Build 06 completed successfully in CodeAssist. This confirms compilation/build in the user's environment only; it does not by itself prove two-account message send/receive, Realtime authorization/event delivery, or delivery receipts. Two-account runtime send/receive verification remains pending.


## 18.13 Messaging Amendments Build — Start Chat UI-to-Backend Wiring (CODEASSIST BUILD PENDING)

User reported that after searching a username and tapping Start chat, the opened screen showed only the empty-state text and did not expose a usable composer. Live source inspection confirmed the defect: MainActivity still passed a placeholder onSendMessage callback that only displayed “Message sending will be enabled in Build 05.” The chat screen also rendered a static conversation placeholder. In addition, the documented MessagingRepository.kt file was not present in the live repository despite being described in the prior Build 05/06 notes.

Amendments delivered directly to main:
- Commit `8a47bce3ed1f9ed88a1062d8013ceb3f66853842`: added the missing `data/MessagingRepository.kt` with authenticated RLS-protected message history loading and `send_message` RPC integration, client UUID idempotency, access-token refresh retry and error reporting.
- Commit `9ebbf55ba9a84b4c656c3b86635dd2c4ae2df1c1`: replaced the static chat placeholder with a working conversation UI, message bubbles, timestamps, text composer, send/loading state, initial history load and conversation-scoped Realtime subscription lifecycle.
- Commit `b413a6fd102ed11dbac5b41c64f56e6f9356ec0d`: removed the placeholder callback from MainActivity and wired Start Chat to the actual active session and conversation ID.

Verification after source changes:
- Latest main HEAD: `b413a6fd102ed11dbac5b41c64f56e6f9356ec0d`.
- Live source check confirms the old Build 05 placeholder callback is absent.
- Live source check confirms MessagingScreens calls MessagingRepository.loadMessages and sendMessage.
- Live source check confirms MessagingRepository.kt now exists on main.
- Supabase project remains `ACTIVE_HEALTHY`; no SQL, migration, RLS, Realtime policy, public-channel setting, billing or production data changes were made.
- CodeAssist compilation and two-account runtime message send/receive are pending user confirmation. Do not mark this Amendments Build successful until the user confirms the CodeAssist build. After build, request a brief two-account test: send from account A to B and reply from B to A.


### Amendments follow-up — Scaffold inset correction (2026-11-24)

User-provided screenshot after the first Amendments Build showed the empty-state message but the conversation header and composer were obscured. Root cause: `MessagingChatScreen` did not consume the Scaffold `PaddingValues`, so its top conversation header and bottom composer were laid out behind the persistent workspace top bar and bottom navigation.

Correction committed:
- `347d58dcbc77baf2da840de054a7b6258fe0097c`: added `contentPadding` to `MessagingChatScreen` and applied it to the root layout.
- `33388a30953b189d3c6987058448a0f32af414c4`: passed the Scaffold `padding` from MainActivity into the chat screen.

This is a UI layout correction only. Supabase was not changed. CodeAssist Build and visual confirmation that the conversation header, message input and send button are now visible remain pending user confirmation.


## 18.14 Messaging UI-First Decision and Ordered Design Roadmap (USER DECISION — 2026-11-25)

The user explicitly decided to complete the Messaging user interface first, following the exact sequence below, before resuming the remaining Messaging backend/build roadmap. This supersedes the previous immediate next-step plan to start Build 07. Do not start Build 07 or other remaining backend work until the user has completed/approved the UI-first sequence.

**UI-first sequence (follow this order; one step at a time):**
1. **Chat List** — create a polished WhatsApp/Telegram-inspired conversation list.
2. **Chat Header** — profile picture, display name, online status, and call/menu actions (actions may remain UI-only until their backend milestones).
3. **Message Bubbles** — polished incoming/outgoing message presentation.
4. **Message Composer** — emoji, attachment, text input, send, and voice-message controls (unimplemented actions remain clearly UI-only until backend support exists).
5. **Message Details** — timestamp and Sent/Delivered/Read indicators (show real states only when backend receipt work is completed; do not fake status).
6. **User Search** — polished username search and user profile preview.
7. **Empty States** — designed empty states for no conversations/no messages/search results.
8. **UI Polish** — animation, spacing, typography, responsive layout and dark mode.

**Product/navigation direction:** Keep the existing Teddy-WebApp workspace bottom navigation intact for now. Improve the Chats destination into the dedicated Messaging experience; do not introduce a separate global app navigation structure unless the user later changes this decision. Use a consistent Teddy-WebApp visual identity while taking interaction/design cues from WhatsApp and Telegram; do not copy either app pixel-for-pixel.

**Current state and safeguards:**
- Existing messaging backend foundations, Supabase schema/RPC/RLS and current send/history/Realtime implementation must be preserved. UI-first means defer remaining backend milestones, not remove or break existing functionality.
- The user has confirmed the Amendments Build and Scaffold inset correction CodeAssist Build completed; latest screenshot now shows message history, participant header and bottom composer/send control visible. This confirms the current screen is usable at a basic level, not that the full UI roadmap is complete.
- Latest live GitHub main HEAD before this documentation update: `2a9423831b903f27451a72524b7f517a20b51ce3`.
- Supabase project `axklmzuepqcbxsbxvtdn` was rechecked and is `ACTIVE_HEALTHY`. This documentation-only update makes no database changes.
- **Next task: UI Step 01 — Chat List.** Before source edits, re-fetch current main HEAD, the exact relevant UI/navigation files, and current Supabase project status. Keep work to this one UI step and one change-set; do not proceed to Step 02 until the user reviews/approves Step 01.
- The user will manually Pull `main` in CodeAssist, build and review on device. Never mark a Build successful without explicit user confirmation.


### UI Step 01 — Chat List Header component created (2026-11-25)

User instructed that only the currently active Kotlin file should be added to the project tree; later segment files must not be created in advance. Therefore, only this file has been added:
- `app/src/main/kotlin/com.teddy.bpositive/ui/screens/messaging/chatlist/ChatListHeader.kt`
- Git commit: `37c935021c19f53b49c4e5b839ae91d2efd106e4`

Implementation scope:
- Static Compose header based on the supplied WhatsApp Chats screenshot.
- Green “WhatsApp” title aligned left and vertical three-dot glyph aligned right.
- White Material surface, 64dp header height, screenshot-inspired horizontal padding and typography.
- Includes a `@Preview` composable wrapped in `TeddyAppTheme(darkMode = false)`.
- UI-only: no navigation callbacks, menu behavior, state, backend, API, repository or Supabase integration.
- This is the first component implementation, not yet user-reviewed or build-confirmed. Do not mark Step 01 complete until the user inspects the CodeAssist Preview and approves it.
- No other Chat List segment files have been created. Continue one file at a time; only create the next file after the current segment is reviewed and accepted.
- Supabase was checked before the source change and was `ACTIVE_HEALTHY`; no database changes were made.

Next action: user opens/pulls `main` in CodeAssist and inspects the `ChatListHeaderPreview`. Wait for feedback before modifying this component or creating `ChatListSearchBar.kt`.


### ChatListHeader — top-position and proportion refinement (2026-11-25)

User requested that the header render at its actual top-of-screen position rather than centered in the Preview canvas, and that every visual element be measured/considered before implementation.

Updated only `ChatListHeader.kt`:
- Header height set to 48dp; it occupies the top of the app content area immediately below the system status bar.
- Horizontal content insets set to 12dp on both sides.
- Title typography refined to 18sp bold with slightly tightened letter spacing and one-line constraint.
- Three-dot menu glyph refined to 24sp, vertically centered with the title.
- Header surface explicitly white with zero elevation.
- Preview now uses a 360dp × 800dp screen canvas and a 24dp top inset to represent the Android status-bar area; header is top-aligned rather than vertically centered.
- No other Kotlin files were added or modified. Still UI-only; no backend, state, navigation or actions.
- Source commit: `b822ae36cd7d5ab8c5a65c3e1f6e42b5fb9bf7ca`.
- User has not yet reviewed this revised Preview; do not mark ChatListHeader accepted until screenshot/approval.

The original reference screenshot has a 720×1600 raster size; dp/sp values above are an initial calibrated implementation based on the visible proportions and should be checked against CodeAssist Preview by the user. No Supabase changes.


### ChatListHeader — subtle menu icon refinement (2026-11-25)

User approved the proposed small visual refinements and specifically requested Canvas drawing instead of adding a Material Icons dependency, because that dependency causes Build problems in their CodeAssist environment.

Updated only `ChatListHeader.kt`:
- Replaced the bold text glyph `⋮` with three individually drawn circles using Compose `Canvas`; no Material Icons dependency was added.
- Each dot uses a 1.5dp radius, centered horizontally in a 24dp drawing area, with vertical centers at 5dp, 12dp and 19dp.
- Increased the trailing header inset from 12dp to 16dp as planned.
- Preserved the 48dp header height, 12dp leading inset, title color, 18sp bold title typography, top-aligned Preview and 24dp status-bar simulation. No changes to the main title treatment or overall component structure.
- Source commit: `34e919a47493177f4798b6b927db3968235c0afc`.
- No other Kotlin files were changed or created. No backend or Supabase changes.
- Await the user's CodeAssist Preview review and build confirmation before considering this refinement accepted or creating the next UI segment.


### ChatListHeader — three-dot icon geometry correction (2026-11-25)

User reviewed the previous Preview and identified that the dots should be slightly larger, not smaller. User specified increasing radius by 0.2dp from 1.5dp to 1.7dp. The previous implementation also had excessive vertical spacing and placed the icon too far inward because the 24dp-wide Canvas plus 16dp trailing padding were not accounted for as a combined geometry.

Updated only `ChatListHeader.kt`:
- Dot radius: 1.7dp (diameter 3.4dp), following the user's explicit correction.
- Canvas size: 12dp wide × 24dp high, replacing 24dp × 24dp.
- Dot center X: Canvas horizontal center.
- Dot center Y positions: 8dp, 12dp, 16dp (4dp center-to-center spacing).
- Header trailing padding: 12dp, placing the dots closer to the right edge while accounting for the Canvas width and circle radius.
- Header height (48dp), leading padding (12dp), title typography/color, white surface and top-positioned Preview preserved.
- Used `androidx.compose.ui.geometry.Offset` import; no Material Icons dependency.
- Source commit: `2028a7ab3cabeac27659ee8db29fbce55a37a84d`.
- Supabase checked before change: project `ACTIVE_HEALTHY`; no database changes.
- This is a geometry refinement based on the user's screenshot observations; wait for the next CodeAssist Preview screenshot and user approval. Do not create another UI segment yet.


### ChatListHeader — menu dot visible gap set to 1dp (2026-11-25)

User explicitly requested a 1dp visible gap between adjacent menu dots (not 0.6dp).
- Dot radius remains 1.7dp; diameter remains 3.4dp.
- Required center-to-center spacing is therefore 4.4dp (3.4dp diameter + 1dp visible gap).
- Vertically centered positions in the 24dp Canvas are 7.6dp, 12dp and 16.4dp.
- No other geometry or Header styling changed.
- Source commit: `660710033bc0c26c9ca3c13f64d4b20d812e229f`.
- Supabase project was checked and remains ACTIVE_HEALTHY; no database changes.
- Await the user's CodeAssist Preview review.


### ChatListHeader — menu dot visible gap increased to 1.4dp (2026-11-25)

User reviewed the 1dp gap in Preview and requested a further 0.4dp increase.
- Dot radius remains 1.7dp (diameter 3.4dp).
- Visible gap is now 1.4dp.
- Center-to-center spacing is 4.8dp.
- Vertical dot centers in the 24dp Canvas: 7.2dp, 12dp and 16.8dp.
- Only the three vertical center positions changed; all other Header geometry and styling remain unchanged.
- Source commit: `d586758b82035f299cd17a5bd16785f092eb64fa`.
- Supabase status checked before update: ACTIVE_HEALTHY; no database changes.
- Await user Preview review before further adjustments or creating the next segment.


### ChatListHeader — temporary in-app inspection route (2026-11-25)

The user clarified that the existing CodeAssist Live Preview workflow must remain unchanged. Because the Header component is nearly finished, the user requested a one-time real-app inspection before approving the component. This temporary route does not replace or disable the normal `@Preview` composable.

Implemented directly on `main`:
- `2fefd771ed977af157bd17bda1276e00389de68e` — added `ChatListHeaderAppPreview` to the existing `ChatListHeader.kt`. It displays the exact `ChatListHeader()` component at the top of a white app-content surface, followed by a small temporary explanation and a **Continue to Messages** action.
- `ea457cd36f8956f22fde6ac3bfab333d992265df` — updated `MainActivity.kt` so selecting the existing **Messages** bottom-navigation destination opens this temporary Header inspection view first. The workspace top bar is hidden only while this preview is displayed, allowing the Header to be inspected at the top of the content area. Tapping **Continue to Messages** opens the existing `MessagingHomeScreen` without changing its implementation. The existing bottom navigation remains present.
- The original `ChatListHeaderPreview` CodeAssist Live Preview remains intact and usable as before.
- No Supabase schema, data, RLS, Realtime, billing or project settings were changed. Live Supabase status rechecked after source changes: `ACTIVE_HEALTHY`.
- Latest main HEAD after the two source commits: `ea457cd36f8956f22fde6ac3bfab333d992265df`.
- **Build status: pending user-side CodeAssist build.** The assistant cannot run the user's Android CodeAssist build/device installation from GitHub. Do not mark this build successful until the user confirms.

User steps:
1. Pull the latest `main` in CodeAssist.
2. Build and install the app.
3. Open the existing **Messages** bottom-navigation tab. The temporary Header preview should appear first.
4. Inspect the Header in the real app. Tap **Continue to Messages** to return to the existing Messages home screen.
5. The normal CodeAssist Live Preview remains available in `ChatListHeader.kt`.

After the user confirms the app inspection and approves the Header, remove this temporary in-app preview route in a later focused change before proceeding to the next Chat List component. Do not treat the Header as approved until the user explicitly says so.


### ChatListHeader — dot gap and rendering refinement (2026-11-25)

User requested another 0.1dp increase in visible dot spacing and reported that the Canvas-rendered icon still appeared unclear in Preview.
- Dot diameter remains 3.8dp (equivalent to the previous 1.9dp radius).
- Visible gap increased from 1.7dp to 1.8dp; center-to-center spacing is 5.6dp.
- Replaced Canvas drawCircle rendering with three Compose circular Box shapes in a vertically centered Column. This avoids the previous custom Canvas coordinate calculations and uses standard Compose shape rendering; visual sharpness still requires the user's Preview/device confirmation.
- Only ChatListHeader.kt changed for the UI implementation. @Preview and the temporary in-app inspection composable remain present.
- Implementation commits: dcace4b76d3ab450156555f6eba345e0f1d6753e and import cleanup 8876f276bd3285c233b9bcc1ab67bbd565a14781.
- Supabase was checked before the source change and remains ACTIVE_HEALTHY; no database changes.
- Build is not requested. User should Pull main and inspect CodeAssist Live Preview. Do not mark the icon accepted until the user confirms its appearance.


### ChatListHeader — menu dot gap increased to 1.9dp (2026-11-25)

User requested another 0.1dp increase and asked for a new app Build to inspect the Header in-app.
- Dot diameter remains 3.8dp.
- Visible gap is now 1.9dp (Compose `Arrangement.spacedBy(1.9.dp)`); center-to-center spacing is 5.7dp.
- Existing temporary `ChatListHeaderAppPreview` route remains in place; the normal CodeAssist `@Preview` is unchanged.
- Source commit: d4a7c08ad59dd72a936c48a8deb64ce3d37fac8c.
- UI-only change; no Supabase database changes. Supabase project was checked before the update and reported `ACTIVE_HEALTHY`.
- Build request acknowledged. Build execution is not available through the connected GitHub/Supabase tools; do not mark Build successful. The user must pull `main`, build/install in CodeAssist, and confirm the result.


### UI Step 01 — ChatListHeader accepted; ChatListSearchBar scaffold created (2026-11-25)

The user reviewed the actual app Chat List screenshot and explicitly decided the `ChatListHeader.kt` work is finished. The screenshot confirms the header is displayed at the top of the Chat List, with the WhatsApp title and three-dot menu, followed by the search field.

Created only the next empty Kotlin file in the same package directory:
- `app/src/main/kotlin/com.teddy.bpositive/ui/screens/messaging/chatlist/ChatListSearchBar.kt`
- Commit: `49ec793d352f31f883bf68eef1deefd7df85349f`
- The file is intentionally empty. No package declaration, imports, composables, functions, UI code, or dependencies have been added; the user wants to discuss the screenshot and design before implementation.
- Current ChatListSearchBar design discussion has not yet been finalized. Reference screenshot: 720×1600; visible search bar is a light warm-gray rounded pill directly below the header, with a leading search icon and placeholder text “Ask Meta AI or Search”. Exact dp dimensions, spacing, typography, colors and behavior are to be agreed before coding.
- No other new Chat List segment files were created. Continue one file at a time.
- Supabase was checked before this UI-only operation and reported `ACTIVE_HEALTHY`; no database changes were made.
- The temporary in-app Header inspection route remains pending removal as a separate cleanup step before the next implementation work, as recorded above.


### UI Step 01 — ChatListSearchBar first implementation (2026-11-25)

User authorized implementation after asking for careful measurement of position, padding, margins, color, height and subtle visual details from the supplied 720×1600 Chat List screenshot.

Implemented only `app/src/main/kotlin/com.teddy.bpositive/ui/screens/messaging/chatlist/ChatListSearchBar.kt`:
- Outer horizontal inset: 9dp on each side.
- Search pill height: 34dp; fully rounded `RoundedCornerShape(percent = 50)`.
- Background: `#F5F4F2` (soft warm gray, visually sampled/estimated from screenshot).
- Content starts 12dp inside the pill; trailing content inset 14dp.
- Custom Canvas search glyph, 18dp drawing area, 1.8dp stroke, muted gray `#62686D`; avoids Material Icons dependency.
- Placeholder: `Ask Meta AI or Search`, 16sp normal weight, zero letter spacing, same muted gray.
- No text input, click behavior, search logic, state, navigation, repository, API or backend integration yet; static visual component only.
- Added a top-positioned `@Preview` that composes the existing `ChatListHeader()` above `ChatListSearchBar()` on a 360×800dp white canvas with 24dp status-bar inset, matching the screenshot's layout context.
- Source commit: `72ab418fd867d922c9a8533da5a4202c76fc3bc0`.
- This is a first calibrated implementation; exact visual approval remains pending user Preview/device review. Adjust only from user feedback/screenshot.
- Supabase was checked before implementation and reported `ACTIVE_HEALTHY`; no database, migration, RLS, Realtime, billing or backend changes were made.
- No Build has been run or claimed successful. User should Pull `main` and inspect CodeAssist `ChatListSearchBarPreview` before further UI changes.


### ChatListSearchBar visual review and import cleanup (2026-11-25)

User supplied a CodeAssist Live Preview screenshot after the initial implementation. Visual review found no material layout mismatch: pill width/insets, height, warm-gray fill, rounded ends, search glyph and placeholder alignment are broadly consistent with the reference WhatsApp screenshot. The preview is currently 360×800dp at 69% zoom; this zoom is not a device-scale measurement.
- Removed one unused `androidx.compose.material3.Surface` import.
- No visual dimensions, colors, layout, icon geometry, text or behavior changed.
- Cleanup commit: 0ac4031f8c298377f821bc5186754e9cf358e46f.
- User has not yet approved the final Search Bar design; continue to wait for feedback before changing measurements or adding behavior.
- Supabase remains `ACTIVE_HEALTHY`; no backend/database changes.
- No Build was run or claimed successful.


### UI Step 03 — FeaturedChatItem scaffold created (2026-11-25)

The user approved `ChatListSearchBar.kt` and instructed that the next file be created in the project tree only; implementation must wait until the main WhatsApp Chat List screenshot is carefully observed together with the user.

Created only this empty file in `main`:
- `app/src/main/kotlin/com.teddy.bpositive/ui/screens/messaging/chatlist/FeaturedChatItem.kt`
- Creation commit: `f9d4b7c61c0cb194bd1d6ff8380d7f449c31bc57`
- File content is intentionally empty: no package declaration, imports, composables, functions, preview or UI code.
- Based on the user's previously shared UI segmentation plan, this component is the Featured / Draft Conversation Row. The screenshot-based details (avatar/profile image, display name, date, message preview, message status icon, badge/chevron as applicable) are not yet implemented or finalized. Re-observe the actual reference screenshot before writing code; do not infer dimensions or styling from the plan alone.
- Current Chat List directory contains only `ChatListHeader.kt`, `ChatListSearchBar.kt`, and this new `FeaturedChatItem.kt`; create later files only after the active component is approved.
- Supabase project was checked before this UI-only change and reported `ACTIVE_HEALTHY`; no database, migration, RLS, Realtime or billing changes were made.
- No Build was requested or run.


### UI Step 03 — FeaturedChatItem first screenshot-based implementation (2026-11-25)

User authorized writing code after asking to inspect the original WhatsApp Chat List screenshot in detail. Implemented the featured group/draft row visible in the screenshot (the `Tedd Music` row with layered group avatar and right chevron):
- Public composable `FeaturedChatItem(title, sender, draftPreview, modifier)` with screenshot-derived defaults `Tedd Music`, `Ted Music`, and truncated URL-like draft preview.
- Row height 56dp; horizontal inset 9dp; 34×36dp layered avatar region; text block start gap 8dp.
- Title uses 16sp medium dark text; preview uses 14sp secondary gray; `Draft:` is green `#159B58` and semibold.
- Sender is followed by a small custom Canvas chevron; another custom Canvas chevron sits at the far right, avoiding Material Icons dependencies.
- Layered cream rounded-square group avatar is drawn with Canvas group glyph as a placeholder; actual profile images are not wired.
- Added isolated `@Preview` for the single row on a white background.
- This is visual-only: no click handling, real avatar/photo loading, date, message status, badge, conversation model, state, repository, API or backend integration. Those elements are not visible on this specific featured group/draft row in the supplied reference screenshot; do not add them without a further reference/approval.
- Source commit: `ddb361b1ec217750595d2ef329a9867c7493c956`.
- User must inspect CodeAssist Live Preview and provide feedback; visual acceptance is pending. Do not proceed to `ConversationListItem.kt` until this file is approved.
- Supabase status checked before implementation: `ACTIVE_HEALTHY`; no database/schema/RLS/Realtime/billing changes.
- No Build was requested or run.


### Correction — FeaturedChatItem now represents Riya Apu personal conversation row (2026-11-25)

User clarified that the previous `Tedd Music` group/channel-like row was the wrong item. The intended component is the first personal conversation row in the original screenshot: `Riya Apu`.
- Replaced the previous group/draft row entirely; removed `Tedd Music`, `Ted Music`, Draft label, layered group avatar and chevrons.
- New default content: name `Riya Apu`, date `11/21/26`, message preview `Kora to ace`.
- Layout: 34dp circular illustrative avatar placeholder, 8dp gap to text, horizontal row padding 8dp, vertical padding 7dp, min row height 56dp; title 16sp medium, date 11sp, message preview 14sp.
- Avatar uses a sunflower emoji on a pale warm background as a temporary placeholder. The original screenshot contains a real circular profile photo; the actual photo asset has not been provided/extracted, so do not claim this is a faithful photo reproduction. Replace with the exact image only when a usable asset is supplied.
- Preview name updated to `Featured Chat Item - Riya Apu`, fixed 360×120dp white preview.
- Source correction commit: `59efe7831544db156b1f2364d8f033404d92b1bd`.
- User must inspect Live Preview; no Build was run. Do not proceed to the next file until user approves this corrected component.
- Supabase status checked before the change: `ACTIVE_HEALTHY`; no database/backend changes.


### FeaturedChatItem — multi-row WhatsApp-style preview density pass (2026-11-25)

User reviewed the single-row preview and correctly noted that the large blank area below it did not resemble a populated WhatsApp Chat List. User requested several independently invented sample rows in the same Kotlin file so the preview resembles a real list.
- Kept `FeaturedChatItem` as a reusable single-row composable with configurable name, date, message preview, avatar placeholder/background and optional Draft label.
- Reduced row height to 53dp, avatar diameter 34dp, horizontal inset 8dp, avatar-to-text gap 8dp; title 16sp medium, date 11sp, message preview 14sp. No separators were added, matching the clean continuous white-list appearance in the reference screenshot.
- Added private `FeaturedChatListSample()` with eight fictional sample conversations: Riya Apu, Nusrat Jahan, Shakila apu, Teddy Ahsan (You), Mim Akter, Study Group, Soma Bhabi and Tasdit. Includes one Draft example and varied simple avatar placeholders/backgrounds.
- Changed the `@Preview` to show the whole 360×480dp multi-row list, rather than a single 360×120dp row. This should make row density and vertical rhythm easier to judge in CodeAssist Live Preview.
- Sample avatars are illustrative emoji/initial placeholders; no actual personal photos or external image assets are bundled.
- Source commit: `6215ab29e7fd2ef1e0a082585c6b43dd4a13ac7f`.
- This is still visual-only sample content: no click behavior, navigation, real data model, persistence, network, API or backend integration.
- Supabase was checked before this UI-only change and reported `ACTIVE_HEALTHY`; no database/schema/RLS/Realtime/billing changes.
- No Build was requested or run. User should Pull `main`, inspect Live Preview and provide feedback. Do not proceed to `ConversationListItem.kt` until this component is approved.


### Messages tab — integrated WhatsApp-style Chat List preview (2026-11-25)

User approved the multi-row sample list and asked to see it inside the app's actual Messages interface. Integrated the current visual components into the existing Messages destination:
- `MessagingScreens.kt`: replaced the old generic Messages landing card/search placeholder with `ChatListHeader`, `ChatListSearchBar`, and the reusable `FeaturedChatList`; added a green FloatingActionButton wired to the existing `onFindPeople` callback so user-search functionality remains accessible.
- `FeaturedChatItem.kt`: exposed `FeaturedChatList(modifier)` as a reusable scrolling `LazyColumn`; retained the eight fictional sample conversations and the standalone `@Preview`.
- `MainActivity.kt`: removed the temporary Header-only in-app inspection route and its state/import. The Messages tab now opens the integrated list directly. WorkspaceTopBar is hidden on the Messages list (the custom WhatsApp-style Header takes its place), but remains on the chat and user-search screens. Existing bottom navigation remains untouched.
- This is a UI integration using local fictional sample rows only. It does not connect the sample list to Supabase conversations/messages, does not create chat records, and does not change schema, RLS, Realtime, Edge Functions, or billing. Existing Find People flow is preserved through the green FAB.
- Source commits: `0fb62c269b1ec61820fc41229e06c78ac8904b77` (reusable lazy list), `558b70c3a36244483f80b2bfd2e6863ab2baa620` (Messages screen composition), `6871c168f021dfc44b434d46c718e84420bcec85` (MainActivity route).
- Supabase status before change: `ACTIVE_HEALTHY`; no database changes.
- No Build was run or claimed successful. User must pull `main`, build on CodeAssist and confirm success, then inspect the integrated Messages screen. Visual acceptance and build success remain pending.


### Build failure correction — Messages screen malformed Kotlin source (2026-11-25)

User supplied CodeAssist Build screenshots showing 142 Kotlin errors. Root cause confirmed in live `MessagingScreens.kt`: previous automated source replacement corrupted the import/function boundary (`import com.tedd@Composable`), left the tail of the old `MessagingHomeScreen` after the new function, and imported `androidx.compose.foundation.layout.weight` (an internal scope extension). These parser/import errors caused cascading unresolved-reference diagnostics (`Modifier`, `dp`, `ChatListSearchBar`, `FeaturedChatList`, `Alignment`, `FontWeight`, etc.).
- Rewrote the import block cleanly with explicit Compose and chatlist imports.
- Replaced only the `MessagingHomeScreen` declaration and removed the orphaned old function tail; preserved the following `MessagingUserSearchScreen` and subsequent screen implementations.
- Removed the invalid explicit `weight` import; `Modifier.weight` is resolved inside the `Column` scope.
- Correction commit: `d3946c673180767a9ccffefdf024bf4fbaa6e6e2`.
- Verified current source contains clean imports, valid function boundary, explicit `ChatListSearchBar`/`FeaturedChatList` imports, no malformed `com.tedd@Composable`, and no internal `weight` import. MainActivity still has no temporary Header-only route; `FeaturedChatList` remains a public lazy list.
- Supabase status checked before correction: `ACTIVE_HEALTHY`; this is a Kotlin-only correction, no backend/database changes.
- No Build was run by assistant. User must Pull latest `main` and run Build again; success remains unconfirmed.


### Follow-up Build correction — missing TextAlign import (2026-11-25)

After the prior Messages screen syntax/import repair, the user's next CodeAssist Build reported one remaining error: `Unresolved reference 'TextAlign'` at `MessagingScreens.kt:275`. Confirmed that `TextAlign.Center` was used but `androidx.compose.ui.text.style.TextAlign` was not imported. Added the missing import only. Source correction commit: `c09f39a52cf2a4a85f8c3e0e01dfbeb37bd8abd1`. Re-fetched the source and verified the import is present. Supabase was checked and remained `ACTIVE_HEALTHY`; no database changes. No Build was run by assistant; user confirmation is still required.


### Chat List visual refinement — Search Bar and conversation rows (2026-11-25)

User compared the integrated app screenshot with the WhatsApp reference and approved a refinement pass for sizing, vertical rhythm and typography. Updated only the active Chat List components and the spacing between them:
- `ChatListSearchBar.kt`: pill height increased from 34dp to 40dp; inner horizontal content inset adjusted to 14dp start / 16dp end; search glyph drawing area increased from 18dp to 20dp, stroke 2dp and lens radius 5.7dp; placeholder increased from 16sp to 17sp. Kept outer horizontal inset 9dp, warm-gray fill `#F5F4F2`, rounded pill and static-only behavior.
- `FeaturedChatItem.kt`: row height increased from 53dp to 64dp; avatar from 34dp to 40dp; avatar/text gap from 8dp to 10dp; name from 16sp to 17sp with 21sp line height and subtle -0.1sp letter spacing; date from 11sp to 12sp; message preview and Draft label from 14sp to 15sp, preview line height 19sp; preview top gap from 2dp to 3dp. Kept horizontal row inset, sample content, avatar placeholder approach, white list background and no separators.
- `MessagingScreens.kt`: gap between Search Bar and conversation list increased from 5dp to 8dp.
- No changes to ChatListHeader, MainActivity navigation, bottom navigation, floating action button behavior, data sources, repository/API integration or Supabase.
- Source commits: `c4dc08f99559913c2684ac82fc339639b5a0b06f`, `9b068588134268511fe775e389dfd69e9986a87b`, `d7468d55786520c31c4b2662ff1c75b227e8f9a6`.
- Supabase project was checked before the UI-only work and reported `ACTIVE_HEALTHY`; no database/schema/RLS/Realtime/billing changes.
- This is a calibrated visual refinement, not yet user-approved. User must Pull `main`, inspect CodeAssist Live Preview and the actual app screenshot. No Build was run or claimed successful.


### Chat List second visual calibration — Search Bar and conversation rows (2026-11-25)

User confirmed the previous Search Bar/row refinement had been pulled and built, but the resulting app still looked noticeably more compact than the WhatsApp reference. Rechecked the live active call path: `MessagingHomeScreen()` renders `ChatListHeader()`, `ChatListSearchBar()` and `FeaturedChatList()`; the list is rendered from `FeaturedChatItem.kt`. No alternate/legacy Chat List renderer was found in the active screen composition.

Updated directly on `main`:
- `ChatListSearchBar.kt`: height increased from 40dp to 46dp; drawing area for the search icon increased from 20dp to 22dp, with a slightly larger 5.9dp lens radius. Existing 9dp outer horizontal inset, pill fill and placeholder styling remain.
- `FeaturedChatItem.kt`: row height increased from 64dp to 76dp; horizontal content inset changed from 8dp to 10dp; name increased from 17sp to 18sp (22sp line height); date from 12sp to 13sp; preview/Draft text from 15sp to 16sp (20sp preview line height); preview top gap from 3dp to 4dp. Avatar remains 40dp because its rendered size was already close to the reference screenshot.
- Added a row-level `Modifier.clickable` around the full row bounds so the press/ripple feedback covers the complete conversation item. The sample rows use the default no-op callback for visual demonstration; no chat navigation or message action is implied.
- Kept fictional sample data, white background, no separators, existing screen navigation and FAB behavior unchanged.
- Source commits: `de253efa562a7f666a298ee4c8c37393c0e766c8` (Search Bar), `41b353a6eb47034a0b5072a2388d3d822552ecb0` (conversation rows).
- Supabase was checked before the UI-only changes and reported `ACTIVE_HEALTHY`; no database/schema/RLS/Realtime/Edge Function/billing changes.
- No Build was run by the assistant and no Build success is claimed. User must Pull the latest `main`, build in CodeAssist, then compare the app screenshot. Visual acceptance remains pending.
