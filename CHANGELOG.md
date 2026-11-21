# Changelog

This file records significant project milestones only. Detailed chronology is maintained in `docs/history/`.

## 2026-11-21

- **Build 22 follow-up confirmed by user:** The user successfully built and tested the Reader tap-area fix; tapping the note content enters Editing Mode.
- **New Home Landing Page and Notes List UI — user-confirmed Build successful:** Home Dashboard presentation replaced by a feature landing page with a Notes destination. Added a separate screenshot-inspired Notes list with Checklist/Search/Settings glyphs, equal-height rectangular cards, ellipsized content preview, `dd MMM yyyy, hh:mm a` date/time and bottom-right circular plus FAB. These controls and note cards are intentionally inert in this phase. Existing app-wide navigation, note data/backend/sync and current Reader/Editor source are preserved; Reader/Editor redesign is deferred. The Notes screen uses its own reference-style header while retaining the Global Bottom Navigation; the FAB uses the screenshot's yellow circular accent. User confirmed successful Build and supplied two device screenshots on 2026-11-21; these are the baseline references for further UI refinement. No Supabase schema change.

## 2026-11-20

- **Build 22 confirmed successful by user:** Added a full-screen note reader/editor, kept the list as a short preview, removed the 3,000-character content guard, added locally persisted note colors and basic Markdown insertion shortcuts. A follow-up Dark Mode fix makes Note Cards use the app's dark surface/on-surface colors while Light Mode retains selected note colors (`08b02aec7f`). User confirmed the targeted color behavior works and Build Successful on 2026-11-20. No Supabase schema change. Note colors remain device-local; Markdown shortcuts still insert plain text markers rather than rendered rich formatting. Broader offline/realtime/conflict regression coverage is not implied by this confirmation.

- **CodeAssist reference completed:** Screenshot-based documentation now covers Modules, the app module's Settings, Build Features, Compiler plugins, Packaging, Signing/Keystore Manager and Dependencies. The reference records screenshot-visible settings and distinguishes CodeAssist UI configuration from the repository's tracked `app/module.toml`.
- **CodeAssist Build & Run / Privacy reference:** Added screenshot-derived notes for dependency conflict strategy, Preview sandbox/process controls, Build Runtime settings, SDK Manager versions and Privacy & Data controls. User reported removing the three accidental Quick Add dependencies; the later list screenshot no longer shows Firebase BOM, Firebase Analytics or Play Services Auth, while Firebase Messaging remains. No Build result is inferred.
- **Dependency review:** Cross-checked the dependency screenshots against the live `main` source. Firebase Messaging 26.0.2 is required by the current push-notification implementation. Firebase BOM 33.7.0, Firebase Analytics 22.1.2 and Play Services Auth 21.2.0 appeared after Quick Add actions; current source shows no Analytics or Google Sign-In API usage. Recommended removing only those three unused additions while preserving Firebase Messaging. This is a recommendation pending the user's CodeAssist cleanup and Build verification.
- **Documentation:** Added a hybrid documentation structure: concise master context, changelog and dedicated Build, Supabase and Release history files. The canonical Master File is stored at `docs/project/PROJECT_CONTEXT.md`; the root `PROJECT_CONTEXT.md` is a navigation pointer.
- **Current state verified:** GitHub `main` is the only branch; latest HEAD at the beginning of this documentation update was `537c6c3405818cc31c82361c61663e01bd526e3b`.
- **Release metadata verified:** `update.json` advertises Teddy-WebApp 1.0.1 (versionCode 2), with minimum supported versionCode 1.
- **Supabase state verified:** project is `ACTIVE_HEALTHY`, PostgreSQL 17.11.0.019; live migration ledger contains 18 applied migrations.
- **Build 21 confirmed successful:** manual update checks bypass the optional-update Later suppression, retain visible loading for at least 1.2 seconds and animate the Settings button state. User confirmed the Build succeeded and all tests were completed on 2026-11-20.
- **Architecture documentation corrected:** clarified that Android currently accesses Supabase directly; Cloudflare Workers and the separate website remain planned, not part of the deployed app request path.

## Earlier history

See [Build History](docs/history/BUILD_HISTORY_01.md), [Supabase History](docs/history/SUPABASE_HISTORY.md) and [Release History](docs/history/RELEASE_HISTORY.md).


## Build 24 — Collapsing Notes Header (implementation committed; Build pending)

- Added source-reference-style expanded/collapsed Notes header using nested scrolling.
- Downward scroll collapses the title and aligns Checklist/Search/Settings on one row; upward unconsumed scroll at the list top expands it.
- Animated the header transition.
- Reduced artificial bottom padding after the last note card while preserving Scaffold bottom-navigation insets.
- No Landing Page, Reader/Editor, database or Supabase changes.
- Commits: `05ad4253d023a388750af75820e6bebc1f32e14e`, `7260543d4080bb9026819ee83d770c95a88683a4`.
- Build/device verification pending user confirmation.


## Build 25 — Continuous Notes Header (implementation committed; Build pending)

- Replaced abrupt Boolean collapse with continuous scroll-progress-driven interpolation of header height, title size/position and action-row position.
- Header consumes downward drag before the note list, preventing the first card from scrolling independently during collapse.
- Header expands progressively from unconsumed upward scroll at the list top.
- Commit: `aea5df26b6af0ef59743fa9e5f329377b37234ff`.
- CodeAssist Build and device verification pending.


## 2026-11-21 — Build 26: smooth scroll-linked Notes title scaling (implementation committed; Build pending)

- Replaced per-scroll `fontSize` changes with a fixed-size Text layout and a `graphicsLayer` scale driven directly by header collapse progress.
- The title scales from 34sp-equivalent to 26sp-equivalent, anchored at its top-left corner, to reduce text remeasurement and visible jitter while preserving finger/scroll synchronization.
- Existing header geometry, nested-scroll behavior, note cards, FAB and navigation are unchanged.
- Commit: `e85900fea7872a9d134eb3a35725fe60eb2b8c28`.
- CodeAssist Build and device verification pending user confirmation.
