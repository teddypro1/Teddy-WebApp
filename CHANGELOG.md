# Changelog

This file records significant project milestones only. Detailed chronology is maintained in `docs/history/`.

## 2026-11-20

- **Build 22 implementation committed (Build/device verification pending):** Added a full-screen note reader/editor, kept the list as a short preview, removed the 3,000-character content guard, added locally persisted note colors and basic Markdown insertion shortcuts. No Supabase schema change. Note colors are device-local; Markdown is currently stored as plain text markers, not rich rendered formatting. User must pull, build and test before success can be recorded.

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
