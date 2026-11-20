# Changelog

This file records significant project milestones only. Detailed chronology is maintained in `docs/history/`.

## 2026-11-20

- **CodeAssist reference:** Added `reference/codeassist/` with screenshot-based Git/Source Control and Modules/Android Module Settings guides. The latter records the visible app module, Java/SDK/version settings, source sets, Build Types and feature options; Build Features, Compiler plugins and Packaging screenshots are still pending.

- **Documentation:** Added a hybrid documentation structure: concise master context, changelog and dedicated Build, Supabase and Release history files. The canonical Master File is stored at `docs/project/PROJECT_CONTEXT.md`; the root `PROJECT_CONTEXT.md` is a navigation pointer.
- **Current state verified:** GitHub `main` is the only branch; repository HEAD at the start of this documentation update was `c5a51767d6153d6b9fe7ce164943c41e958769e9`.
- **Release metadata verified:** `update.json` advertises Teddy-WebApp 1.0.1 (versionCode 2), with minimum supported versionCode 1.
- **Supabase state verified:** project is `ACTIVE_HEALTHY`, PostgreSQL 17.11.0.019; live migration ledger contains 18 applied migrations.
- **Build 21 confirmed successful:** manual update checks bypass the optional-update Later suppression, retain visible loading for at least 1.2 seconds and animate the Settings button state. User confirmed the Build succeeded and all tests were completed on 2026-11-20.
- **Architecture documentation corrected:** clarified that Android currently accesses Supabase directly; Cloudflare Workers and the separate website remain planned, not part of the deployed app request path.

## Earlier history

See [Build History](docs/history/BUILD_HISTORY_01.md), [Supabase History](docs/history/SUPABASE_HISTORY.md) and [Release History](docs/history/RELEASE_HISTORY.md).
