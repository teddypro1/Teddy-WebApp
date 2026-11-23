# Teddy-WebApp — Build & Implementation History

Last reconstructed: 2026-11-20. This file records the major implementation path, not every tiny commit. Commit references are shortened for readability; open the linked GitHub commit for the exact diff.

## How to read status

- **Committed** means the code or migration is present in Git history.
- **User-confirmed Build** means the user explicitly reported a successful local Build.
- **User-reported device test** means the user described a real-device test as successful.
- A commit alone is not proof of successful compilation, deployment, migration application or device behavior.

## Foundation and UI architecture

The app began as a native Android application in package `com.teddy.bpositive`, using Kotlin and Jetpack Compose. The initial project was intentionally built as a simple UI before cloud/backend integration. Later implementation separated large screen and utility code into focused files.

Notable early commits recorded in the prior project history:
- Build A foundation — `4d4670aa44`
- Build B UI components — `0cd3b5ead7`
- Home screen extraction — `fa33bee2f7`
- Profile editor extraction — `9ae6ab1242`
- Navigation and deletion dialogs — `b740483140`
- Workspace top app bar — `13376202a1`
- Workspace top app bar fix — `66aeeaf0b8`
- Theme extraction — `de528607f1`
- Note utility extraction — `6a2b0a09f7`
- Note utility fix — `51ab8c81db`
- Warning fixes — `a49fba608e`
- README licensing update — `edb02ddfa7`
- README project overview update — `d43cb3aa4a`

The app subsequently gained Supabase email/password authentication, encrypted session persistence, account-scoped local notes, cloud notes/profile APIs, offline pending sync, realtime subscriptions and conflict detection/recovery. These behaviors are summarized in the master file and README; always inspect live source before modifying them.

## Admin and push notification development (Builds 1–10)

Commit history from 18–19 Nov 2026 shows the following implementation sequence:

### Admin foundation and notification controls
- Added admin-gated notification backend and admin-only notification history endpoint.
- Added Android administrator dashboard and authenticated admin notification repository.
- Integrated Admin Panel navigation, with visibility restricted to verified admins.
- Restricted test-notification controls to verified admins and guarded admin navigation against stale account-switch state.
- Added custom push broadcast preparation and audience migration, then a confirmed all-user custom push composer.
- Added selected-user and all-user audience controls, recipient parsing and confirmation flow.
- Added secure targeted/broadcast push audience support.
- Added searchable admin user directory and server-side directory access; service-role profile-directory read grant followed.
- Relevant commits include `32e21e0d4c`, `ccd2a5fa03`, `0f77c709ab`, `1e110d9019`, `3bc00aaa46`, `906c2b64d1`, `8cd493c679`, `7788f9511a`, `9857722bdd`, `ed36ab4b9d`, `2756d63680`, `5f11905e12`, `a66b0e458a`, `a5ba5f1978`, `41f1afd8c7`.

### FCM token lifecycle and delivery diagnostics
- Added token freshness/rotation handling and synchronization of refreshed tokens with Supabase.
- Added persistent per-device FCM delivery diagnostics, detailed per-device results in Admin Panel and diagnostics Edge Function updates.
- Hardened stale-token cleanup error handling and granted the backend required cleanup permissions.
- Added notification analytics, searchable history and security review in Admin Panel; followed by Material 3 compatibility and safe-area/responsive layout fixes (Builds 5–7).
- Relevant commits include `b2ad8a02ed`, `ae0aa1a030`, `16ed23edc2`, `7927de3a15`, `532fc27d9c`, `bf6f3a8a78`, `4bbf016fb5`, `56954edebb`, `bc59049a11`, `7b00acc883`, `b7b2cbf473`.

### Build 10 — searchable user directory
- Added secure user directory repository, searchable selection UI, load/refresh flow, Edge Function source and service-role read grant.
- Commit group: `2756d63680`, `5f11905e12`, `a66b0e458a`, `a5ba5f1978`, `41f1afd8c7`.

## Build 11 — installation-scoped device registry

Commit group on 19 Nov 2026:
- `4dff58510e`: stable app installation identity.
- `f63f41f365`: register FCM tokens through secure installation registry.
- `75a617b9e0`: authenticated installation device registry.
- `39a3d14420`: installation-scoped registry migration.
- `1751f49309`: token rotation and sign-out cleanup hardening.
- `da54c8bc36`: service-role upsert privilege hotfix.

Purpose: track device tokens per app installation rather than treating one account as having only one device token; improve rotation and sign-out cleanup.

## Build 12 — in-app notification inbox

Commit group:
- `83dc64a3a3`: inbox repository.
- `0294de351b`: inbox screen.
- `711d715b74`: Inbox navigation tab.
- `e41e7598e9`: per-account inbox integration.
- `11c706d4cb`: persist in-app notifications for target accounts.
- `b5f3cd2473`, `2a85205455`, `c07bb37edb`, `111b235c0d`: persistence reporting, secure inbox table and admin feedback count.

Purpose: retain notifications inside the app for each targeted account and expose an Inbox screen.

## Build 13 — FCM topic preferences and broadcast

Commit group:
- `fc6fc146a1`: secure per-user FCM topic preferences.
- `ab8a8dcd2b`: secure topic-management Edge Function.
- `371d4295e3`: allow topic notification history audiences.
- `7369fa582a`: Android topic subscription repository.
- `30a5cb0538`: topic preferences in Settings.
- `532da1a813`: load/update signed-in user's topic preferences.
- `87e8a38ebd`: Admin topic broadcast controls.
- `cd73cfc827`: connect Admin topic broadcast to secure server action.
- `e2c8e12637`: align topic history audience and secure delivery.
- `83f27b0921`, `f8dbad8d5c`, `9155d3dea9`: default preferences to opt-in.
- `fed7e0dfa1`: label topic broadcasts in notification history.
- Later fixes: `26c4b2d4a6` (upsert conflict target), `720b5b3286` (exclude topic receipts from per-device parser), `08faf24d00` (clarify topic history delivery status).

Purpose: user-controlled topic subscriptions default to opt-in, plus secure Admin topic broadcast.

## Build 14 — structured per-device delivery tracking

- `d50a1d4288`: structured per-device FCM delivery tracking.
- `197e0e254b`: align migration filename with applied version.
- `5d098a233e`: retain legacy per-device history details.
- Related migration: `20261120044420_build14_notification_delivery_tracking.sql`.

The diagnostic model distinguishes per-device attempts and results. A successful FCM HTTP 200 means accepted by FCM, not proof of visible device display.

## Security maintenance after notification features

- `2de00653b0`: tighten public grants and indexes.
- `d72ecbf43e`: make notification topic defaults opt-in.
- Earlier security migrations and their applied status are listed in [SUPABASE_HISTORY.md](SUPABASE_HISTORY.md).

## In-app update system (Builds 15–20)

### Build 15
- `2ec4f75f8b`: account-independent update checker.
- `2b5d172547`: correct metadata branch URL.
- `59bcaf27cd`: clean deprecated Android API warnings.
- User-confirmed Build success is recorded in prior project context.

### Build 16
- `289733cbe2`: optional and mandatory update prompts.
- User-confirmed Build success is recorded in prior project context.

### Build 17
- `0e5ef6a0ff`: remove redundant update metadata null check.
- User-confirmed Build success is recorded in prior project context.

### Build 18
- `0ebcc18313`: APK download and SHA-256 verification.
- `8124b59bb8`: verified in-app update download flow.
- `725bcdaf1e`: secure APK installer access.
- `de4cd01fda`: FileProvider cache paths.
- User confirmed Build success and reported successful update download/verification/installer behavior.

### Build 19
- Production metadata URL corrected to the raw `main` branch update.json URL.
- Main merge commit: `6f5621ac45` (PR #3).
- User confirmed the production URL fix was merged; Build confirmation should be tied to the relevant user report rather than inferred from the commit.

### Build 20 and v1.0.1
- Version bumped in `app/module.toml` to 1.0.1 / versionCode 2.
- Merge commit: `706f9d13cf` (PR #4).
- User explicitly confirmed successful Build on branch and after pulling main.
- User published v1.0.1 and tested in-app update flow, reporting download, SHA-256 verification, package installer and installation succeeded.

## Build 21 — Settings manual update check (user-confirmed successful)

- `15826afa4f`: manual check bypasses the same-day Later suppression and holds loading state for at least 1,200 ms.
- `e2e1c5d43e`: AnimatedContent fade feedback and removes obsolete future-download/install message.
- Both changes are on current main history.
- **User-confirmed:** On 2026-11-20, the user confirmed that Build 21 built successfully and all tests were completed.

## Working policy

Only `main` is used. Older commits may have been developed via temporary branches before the user made the final main-only decision; this historical fact does not change the current policy. No new branches should be created.


## Build 22 — full-screen Note Reader & Editor (user-confirmed Build successful)

User-approved scope:
- Keep Note List as a short preview only.
- Tapping a note opens a separate full-screen detail screen in Reading Mode.
- Long content scrolls vertically; tapping title/body or Edit enters Editing Mode.
- Save updates local note and uses the existing queued Supabase sync path.
- Add note color and basic formatting shortcuts.

Implementation on `main`:
- Added backward-compatible local `colorHex` note property and persistence.
- Note cards open the reader; preview remains truncated.
- Added full-screen reader/editor and hidden app top/bottom navigation while viewing a note.
- Removed the old 3,000-character note content guard.
- Added color palette and Markdown insertion templates. The content is still plain text; rendered rich Markdown is not implemented yet.
- Preserve locally stored note color when refreshing note data from Supabase. Color does not sync across devices.
- No Supabase migration or schema modification.

Commits:
- `e28271bf27` note model color field
- `df0e3bbd14` local color persistence
- `9754947d9e` clickable colored note cards
- `afcf5d09dc` preview list
- `282ae2353f` detail screen
- `b8d2e0eab5` app integration
- `cfe9d753a8` preserve color on cloud refresh
- `c9db6b8fd1` remove old length counter
- `2c901c1d60` formatting shortcut templates

### Build 22 — Dark Mode note-card color correction (user-confirmed Build successful)

After the initial Build 22 implementation, the user reported that Note Cards remained white in Dark Mode because the card background continued using the saved light `colorHex`. The focused correction changed `NoteComponents.kt` so:
- Dark Mode Note Cards use `MaterialTheme.colorScheme.surface` and `onSurface`.
- Light Mode retains the selected per-note color.
- No database/schema change was made.

Commits:
- `df684cbd23` — initial text-contrast correction in Note Reader/Editor and Note Card.
- `08b02aec7f` — make Note Card background follow the active Dark Theme surface.

**User-confirmed:** On 2026-11-20, the user reported the color issue was working as expected and explicitly confirmed Build Successful after pulling the correction. This confirms the reported Build and the targeted visual fix only; it does not imply every Build 22 regression scenario (offline sync, realtime, conflicts, long-note persistence) was exhaustively tested.




## Build 22 follow-up — full Reader tap target (user-confirmed)

- Expanded the Reading Mode tap target so tapping the blank area of the reader also enters Editing Mode, not only the title/body text.
- Commit: `7b7bf8bc913c441592dba53843bf0683fa6783ba` is the preceding change-set baseline; follow-up commit recorded in GitHub history as the tap-target fix.
- **User-confirmed:** Build succeeded and the user tested that tapping the note content enters Editing Mode. The user has now approved replacing the current Home and Note List presentation, while leaving the Reader/Editor redesign for a later stage.

## New Home Landing Page and Notes List UI — user-confirmed Build successful

- Replaced the Home Dashboard UI with a feature landing page and Notes feature destination.
- Added a separate Notes list screen based on the user's supplied Google Keep screenshot: large Notes title, Checklist/Search/Settings icons at top-right, same-height rectangular cards, title, two-line ellipsized preview, and date/time format `dd MMM yyyy, hh:mm a`.
- Added a round bottom-right plus FAB. Header icons, FAB and note cards are deliberately inert until their destination screens and actions are designed in a later phase.
- Existing app-wide top bar and Global Bottom Navigation remain. Existing notes data, database, sync, pin/trash/conflict logic and Reader/Editor source remain; Reader/Editor visual redesign is explicitly deferred.
- Note list cards use the current theme's surface/on-surface colors, without per-note custom colors.
- Commits: `e05a48d536`, `61003e446b`, `f1767f7dfc`, `16e202dc4b`, `59da046916`, `fa2dac5102`, `c7aca95602`, `2b558efa03`, `24c79a9b45`, `137f27d293`.
- The dedicated Notes screen hides the workspace top bar so its own title/action controls occupy the reference positions; the Global Bottom Navigation remains.
- **User-confirmed on 2026-11-21:** Build completed successfully and the user tested the screen, supplying two screenshots. These screenshots are retained as the visual baseline for subsequent comparison and refinement.
- No Supabase migration or schema change.


## Build 24 — Source-style Collapsing Notes Header and Scroll Refinement (Build pending)

Reference: the user's 30.35-second source-app recording, sampled throughout. Expanded header shows a large Notes title above right-aligned Checklist/Search/Settings icons. Downward scrolling collapses the title and places the three icons on the same row. Upward scrolling expands the header only after the list reaches its top boundary. The final card should not leave a large artificial bottom gap.

Implemented in `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NotesScreen.kt`:
- Nested-scroll connection collapses on downward scroll and expands on upward unconsumed scroll at the list top.
- Animated transition between expanded and collapsed header.
- Retains 104dp note cards, two-line ellipsized preview, timestamp, visual-only action icons, yellow FAB and global bottom navigation.
- LazyColumn bottom content padding reduced from 100dp to 8dp; Scaffold bottom-bar inset remains.
- No Landing Page, Reader/Editor, database or Supabase changes.

Commits: `05ad4253d023a388750af75820e6bebc1f32e14e`, `7260543d4080bb9026819ee83d770c95a88683a4`.

Source committed; CodeAssist Build and device testing pending user confirmation.


## Build 25 — Continuous Scroll-Driven Header (Build pending)

User confirmed Build 24 successful, then supplied a 24-second device recording showing abrupt header state changes and unintended first-card movement during collapse. Recording inspected at sampled intervals.

Implemented in `NotesScreen.kt`:
- Replaced Boolean header state with continuous `collapseProgress` (0..1).
- Header height, Notes title font size/position and action-row position now interpolate directly with scroll distance, so slow finger movement produces gradual shrinking.
- Nested scroll consumes downward movement to collapse the header before LazyColumn receives the remaining drag; first card should not independently scroll during this phase.
- Upward expansion is consumed progressively from unconsumed upward scroll after the list reaches its top.
- Note cards, icons, FAB and global navigation retained.

Commit: `aea5df26b6af0ef59743fa9e5f329377b37234ff`.

Build and device verification pending user confirmation.


## Build 26 — Smooth scroll-linked Notes title scaling (user-confirmed successful)

After Build 25, the user reported that the header itself moved smoothly but the `Notes` title looked unstable/jittery while shrinking. The user requested that title scaling match the exact scroll speed and remain smooth during slow or fast gestures.

Implemented in `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NotesScreen.kt`:
- Text layout now remains fixed at 34sp with a 40sp line height.
- `graphicsLayer` scales the rendered title continuously from 1.0 to `26/34`, using a top-left transform origin.
- Avoids changing the Text composable's font size on every scroll update, reducing repeated text measurement/layout work that could produce jitter.
- Scale and title position remain driven by the same `collapseProgress`; no time-based animation or easing is inserted, so the visual response remains directly coupled to scroll distance.
- No other screen, card, navigation, FAB, data, database or Supabase behavior changed.

Commit: `e85900fea7872a9d134eb3a35725fe60eb2b8c28`.

User confirmed Build 26 successfully built. Device testing showed the title was somewhat more stable but still not sufficiently smooth, so a deeper Compose phase-optimization was prepared in Build 27.


## Build 27 — Compose phase-optimized scroll-driven Notes header (implementation committed; Build pending)

The user confirmed Build 26 successful but reported only a modest improvement in title smoothness. Inspection of the live source showed `collapseProgress` was read in composition by `NotesScreen`, causing the screen subtree to be recomposed on every nested-scroll update.

Implemented in `NotesScreen.kt`:
- `collapseProgress` remains a `MutableFloatState`, but is passed to the header as `State<Float>` without reading its value during composition.
- Header height reads progress inside a custom `Modifier.layout` measure block.
- Title and action-row positions read progress inside placement lambdas.
- Title scale reads progress inside `graphicsLayer`, preserving fixed Text measurement and updating the rendered layer.
- The intent is to confine invalidation to measure/placement/draw phases and avoid recomposing the Notes screen and LazyColumn for every scroll delta.
- Nested-scroll math and visual geometry, note cards, action icons, FAB and bottom navigation are otherwise unchanged.

Commit: `1ec5ff1f53948e8741d7047900e77b548091fb4b`.

CodeAssist Build/device verification pending. Test gradual and rapid drag, reversing direction mid-motion, repeated collapse/expand, first-card stability and list scroll performance. No database/Supabase changes.


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

Source committed directly to `main`. **User confirmed Build 28 successfully completed**, but reported that the title smoothness still did not reach the expected level. No Supabase/database changes; live Supabase remained ACTIVE_HEALTHY with the existing 18 migrations.


## Build 29 — Header-first two-stage scroll handoff (implementation in progress)

User requirement: scrolling down while the header is not fully collapsed must only collapse the header. The first note card can translate upward because the header above it becomes shorter, but the LazyColumn scroll offset must stay fixed. Even the scroll delta that completes the collapse must not also scroll the list in that same nested-scroll event; list scrolling begins on the next scroll delta after the header is already fully collapsed. Scrolling upward should scroll the list back to its top first, then expand the header.

Implemented in `NotesScreen.kt` by changing `onPreScroll`: while the header is not fully collapsed at the beginning of a downward nested-scroll event, it consumes the entire event and updates collapse progress. If that event completes the collapse, its remaining distance is deliberately discarded instead of being passed to LazyColumn; list scrolling starts only on the next nested-scroll event. Existing upward expansion remains after the list reaches its top. Commit: `0c01a24c37edf48418e3b2aef997b9c402f0e651`. Existing title draw-phase interpolation, header geometry, note-card UI, action icons, FAB and global navigation are preserved. No data, database, Supabase, Home Landing Page or Reader/Editor changes. CodeAssist Build/device verification pending user confirmation.


## Build 30 — Full-screen Note Reader/Editor (implementation committed; Build pending)

User supplied three source-app screenshots and approved a full-screen note experience. The first screenshot shows the editing state with Back, Undo/Redo and Save in the top row, plus a lower editing toolbar. The second shows Reading Mode with Back, Save, note title, date/time and character count, body, and a bottom action row. The third shows the expanded formatting toolbar.

Implemented directly on `main`:
- Replaced the previous generic Reader/Editor layout with a source-inspired full-screen layout.
- Reading Mode is the initial state. Title and body tap targets enter Editing Mode; title tap requests title focus, while body tap opens the body editor with cursor at the end when entering from the blank reading surface.
- The body field fills the remaining content area; normal text taps use BasicTextField cursor placement.
- Editing toolbar includes bullet/checklist/numbered insertion, bold, italic, underline and a local appearance palette.
- Save persists title/body/color through the existing local storage and queued Supabase upsert callback, hides the keyboard and returns to Reading Mode without leaving the open note.
- Notes LazyListState and header collapse progress are held in the parent route so the list can return to its prior scroll/header state after Back.
- No Supabase migration, schema modification, paid upgrade or production database operation.

Commits:
- `c252370c0138591ae45f6c4a98673b0b50d3b5f9`
- `2020243b2cc292a1a6a5279e0fbd3a80214cf334`
- `a27c2a2a8e3ae105ae55c495532a3c050b7b01ac`
- `fea2949e1b9158ba7c985895607ab21cdb7ede84`

Known incomplete secondary actions: reminder, export and share panels are currently placeholders; lock is session-only; delete remains through the existing Notes-list Trash action. These are not yet complete functional workflows.

CodeAssist Build and device verification pending user confirmation. Do not report Build 30 successful until the user confirms.


## Build 30 follow-up — Note Reader/Editor horizontal margin (user-confirmed successful)

- Reduced the Note Reader/Editor content horizontal padding from 24dp to 22dp without changing the visible layout otherwise.
- Commit: `873dead37b946308fc0b7759a54024cbcd01052d`.
- **User-confirmed on 2026-11-21:** CodeAssist Build completed successfully.
- No Supabase/database changes.

## Build 31 — Note Reader/Editor ripple removal and full-width margin tap target (implementation committed; Build pending)

Implemented in `NoteDetailScreen.kt`:
- Suppressed Material 3 ripple feedback and Foundation indication feedback throughout the Note Reader/Editor screen.
- Added tap handling to the full-width content container while retaining the 22dp horizontal visual margin. Taps anywhere in the full content region that are not handled by the title/body controls—including horizontal margins and metadata/spacing areas—enter Editing Mode and place the body cursor at the end; title/body tap handling is retained.
- Editing remains blocked while the note is locked. Header and bottom action controls keep their dedicated actions.
- Commits: `c4c095294e84fd79f5f2a692bab356715723c513`, `970a7926ff3d0abc01851d79f530bf20b5ff4467`.
- CodeAssist Build and device testing are pending user confirmation. A commit is not evidence of a successful Build.
- No database, Supabase, migration or paid-plan changes.


## Build 31 — Note Reader/Editor ripple removal and full-width tap target (user-confirmed successful)

- Removed visible ripple feedback throughout Note Reader/Editor while retaining the 22dp visual content margin.
- Taps across the full content region, including the horizontal margins and spacing areas, now enter Editing Mode; title/body tap behavior remains dedicated.
- CodeAssist initially reported `LocalIndication provides null` as a nullable-type error and a missing `}` at the end of `NoteDetailScreen.kt`. The source was corrected by removing the invalid LocalIndication entry, keeping `LocalRippleConfiguration provides null`, and adding the missing closing brace.
- Final fix commit: `e3800c58e0ea9616a7302719c1c2254c7de99859`.
- **User-confirmed on 2026-11-21:** Build completed successfully; ripple was removed and tapping across the screen entered Editing Mode.
- No Supabase/database/schema changes.


## Build 32 — Note Reader/Editor top-bar alignment and toolbar icons (implementation committed; Build pending)

- Changed the top bar to use the same 22dp horizontal inset as the note content, aligning Back at the left and the action group against the right edge.
- Added custom-drawn outline icons matching the supplied reference: Back arrow, bookmark in Reading Mode, Undo/Redo arrows and yellow Save checkmark in Editing Mode. No external icon dependency was added.
- Kept `.imePadding()` on the screen's root content column. This keeps the Editing toolbar above the keyboard while the IME is visible and lets it return to the bottom when the keyboard is hidden, without exiting Editing Mode.
- Commit: `31da483988dbb89e4271208b72a5b4cba35fea54`.
- **User-confirmed on 2026-11-21:** CodeAssist Build completed successfully. Follow-up device testing exposed alignment, reading-scroll and keyboard-toolbar defects; Build 33 addresses them.
- No Supabase/database/schema changes.


## Build 33 — Note Reader/Editor alignment, reading scroll and keyboard toolbar (single commit; Build pending)

- Replaced Material `IconButton` controls in the top bar with exact 24dp clickable icon bounds. This removes the default minimum touch-target inset from the visual alignment calculation: the Back icon now starts at the 22dp content inset, and right-side icons end at the matching 22dp inset. Tap targets match the icon bounds and do not add visual padding.
- Added vertical scrolling to the Note content region. Scroll gestures move through long notes in Reading Mode; tap gestures remain the entry to Editing Mode, while the existing body text position calculation is retained for cursor placement.
- Removed vertical `weight` constraints from children inside the scroll container and gave the body area a 240dp minimum height so the scrollable column can measure content correctly.
- Added `android:windowSoftInputMode="adjustResize"` to MainActivity. The screen's existing root `.imePadding()` now receives the keyboard resize/inset and keeps the Editing Toolbar immediately above the keyboard. When the keyboard hides while Editing Mode remains active, the toolbar returns to the bottom of the screen.
- Build 32 successful confirmation was recorded in Master Documentation and Build History in this same commit.
- **Build 33 CodeAssist Build and device verification are pending.** Do not mark successful until the user confirms.
- No Supabase/database/schema changes.


## Build 34 — Compose UI Preview additions (single commit; user-confirmed successful Build)

- Added `@Preview` composables at the end of 14 existing UI Kotlin files (17 Preview composables total), covering components, dialogs, screens and light/dark theme previews. Sample data and no-op callbacks are used for static UI inspection.
- Only the 14 UI Kotlin files were changed for the implementation; no non-UI source, Gradle, manifest, Supabase or database files were changed.
- Single implementation commit: `e75317c71ab453cd909012a8e3673ea860b3b835`.
- **User-confirmed on 2026-11-22:** CodeAssist Build completed successfully.
- CodeAssist's Preview Interpreter separately reported unresolved/ambiguous calls while parsing dependencies such as `InboxRepository`, `SupabaseProfilesApi` and `SupabaseAuth` (including `firstOrNull`, `ifBlank`, `contains`, `toRequestBody`, `optJSONObject`, `optString`, `has` and `android.os.Handler`), followed by dependent declarations not being lowered. These diagnostics affect Preview interpretation and do not establish that the Android app Build failed.
- Follow-up Preview isolation commit: `d6f87570ac184def609074db9ce81547f432eed8`.
- Replaced the Preview composables in `NotificationInboxScreen.kt`, `ProfileScreen.kt`, `AdminPanelScreen.kt`, and `WorkspaceScreens.kt` with standalone sample-data UI and no-op callbacks. No runtime/backend code was changed by this follow-up.
- **User-confirmed on 2026-11-22:** All four affected file previews render successfully: Notification Inbox, Profile, Admin Panel, and Workspace (Trash and Settings). Screenshots were supplied for the first two; the user confirmed the other two also work.
- These are static Preview representations and are not proof of exact parity with runtime UI.
- No Supabase/database/schema changes.


## Build 35 — Restore Note Reader/Editor scrolling baseline (Build pending)

- Compared `NoteDetailScreen.kt` at `409c7b16ca020553f654e33d71d7d6c643d7eafc` with the current source. The only intervening code difference was the added conditional bottom padding of 120dp in Editing Mode and 24dp in Reading Mode.
- Removed that extra padding only. The existing IME animation wait, caret rectangle calculation, viewport visibility check and `animateScrollBy()` logic remain unchanged.
- Commit: `1210f4aff094f1b95cb8df286936f3f8037096f5`.
- No unrelated source, database, Supabase, migration or configuration changes.
- CodeAssist Build and device testing pending user confirmation.


## Build 36 — Notes Selection Mode and Dedicated Notes Settings (user-confirmed successful)

- Checklist icon now opens Selection Mode. Each visible Note Card receives a checkbox; tapping a card toggles selection. The selection header shows the selected count and provides All/None, Delete and close actions.
- Bulk Delete is connected to the existing `syncNoteDelete` path for each selected note, removes the selected IDs from the local notes collection and invokes the existing save path.
- The Notes Settings icon opens a dedicated Notes feature Settings screen, separate from the app-wide Settings screen.
- Notes Settings currently provide Compact Cards, Larger Text, Newest First / Oldest First sorting, and Reset to Default. Preferences persist locally using SharedPreferences.
- Search filtering by title/content remains combined with the displayed list and selected sorting order.
- Source commits:
  - `9d5c5643ab76bbe9a5f8d060cffb056a0009c80a` — Selection Mode and dedicated Notes Settings.
  - `ee4a9e22298fcb1ef2a6b6321576754397b534c3` — Bulk Delete connected to existing delete synchronization and save flow.
  - `79406bdcec6d2c40a0284a1195cec0d2383af92d` — Settings navigation/import polish; hide Notes FAB while Settings is open.
- **User-confirmed on 2026-11-23:** “Successfully Build Completed!” This confirms the CodeAssist Build completed. No separate exhaustive device regression test has yet been reported.
- Archive was not implemented because the current Note model does not expose a verified archive workflow.
- No Supabase schema, migration, production database, app-wide Settings, or paid-plan changes.


## Build 37 — Note Card long-press action dialog (user-confirmed successful)

- Added long-press on a Note Card to open a dialog with Pin, Lock / Encryption, Share, Delete and Close options.
- This is presentation-only; the option callbacks currently close the dialog and do not yet perform note actions. Selection Mode behavior remains separate.
- File: `app/src/main/kotlin/com.teddy.bpositive/ui/screens/NotesScreen.kt`.
- Commit: `5045f3c32e82be44505e8862b50551a1f8393e1f`.
- **User-confirmed:** CodeAssist Build completed successfully (2026-11-23).
- No database/Supabase changes.


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
