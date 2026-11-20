# Teddy-WebApp — Release & In-App Update History

Last reconstructed: 2026-11-20.

## Release policy

- Releases are published only after explicit user approval.
- Preserve the same signing key for Android in-place updates.
- Verify the actual APK, application ID, versionCode, signing compatibility, download URL and SHA-256 before changing public update metadata.
- Never invent an APK checksum or asset size.
- Keep `minimumSupportedVersionCode` unchanged unless the user explicitly approves a mandatory-update policy change.
- v1.0.0 must not be edited or deleted.

## v1.0.0 — first public release

- Release: https://github.com/teddypro1/Teddy-WebApp/releases/tag/v1.0.0
- APK: https://github.com/teddypro1/Teddy-WebApp/releases/download/v1.0.0/app-release.apk
- Recorded size: 8,318,017 bytes (about 8.3 MB).
- Recorded SHA-256: `a4f47c0af1b3e6a13cb788d7e2f7d0c4f7e523a49b33632de5bdd65e18e08892`.
- User reported physical-device installation test successful.
- Product limitation: v1.0.0 predates the in-app updater, Admin Panel and push notifications. An installed v1.0.0 binary cannot be expected to show an in-app update prompt for v1.0.1 when the updater code was not included in that binary. Users on that version need an appropriate manual upgrade path.

## v1.0.1 — second public release

- VersionName: `1.0.1`; VersionCode: `2`.
- Config source: `app/module.toml`.
- Main version bump merge commit: `706f9d13cf94a4547899b66c790b191391520015` (PR #4).
- Metadata publication commit: `ffa9e12e05afcc06abc99aae1f190968425a3bb7`.
- Release tag: v1.0.1 (user-published).
- User-reported test: download, SHA-256 verification, Android Package Installer prompt and installation/update all succeeded.
- A screenshot showed approximately 9.3 MB download; exact size and checksum should be fetched from live release/update.json rather than copied from an estimate.

## In-app update client requirements

- Check on every app launch regardless of authentication state.
- If offline or the check fails, allow normal app use.
- Optional update: show Later; suppress repeat prompt no more than once per calendar day per version; no Skip option.
- Mandatory update: when installed versionCode is below `minimumSupportedVersionCode`, block use until update.
- Download APK in-app with progress and total size.
- Verify SHA-256 before invoking Android Package Installer.
- APK hosted on GitHub Releases; metadata hosted in root `update.json` on main.
- No Supabase database change is required for the update metadata feature.

## In-app update implementation Builds 15–21

| Build | Main implementation | Commit / merge | Status |
|---|---|---|---|
| 15 | Account-independent update checker, Settings status and metadata model/repository | `2ec4f75f8b` | User-confirmed Build |
| 16 | Optional/mandatory prompts and initial download action | `289733cbe2` | User-confirmed Build |
| 17 | Kotlin warning/null-check cleanup | `0e5ef6a0ff` | User-confirmed Build |
| 18 | In-app APK download, progress, SHA-256, Package Installer, FileProvider | `0ebcc18313`, `8124b59bb8`, `725bcdaf1e`, `de4cd01fda` | User-confirmed Build and reported working |
| 19 | Production metadata URL points to raw main/update.json | `6f5621ac45` (PR #3 merge) | Merged; verify current source when changing |
| 20 | Version bump to 1.0.1 / code 2 | `706f9d13cf` (PR #4 merge) | User-confirmed Build |
| 21 | Manual Settings check bypasses Later suppression, visible loading and fade transition | `15826afa4f`, `e2e1c5d43e` | **Awaiting user Build/test confirmation** |

## Current live update metadata (verified 2026-11-20)

Fetched from `main/update.json`:
- `enabled`: true
- `latestVersionName`: `1.0.1`
- `latestVersionCode`: 2
- `minimumSupportedVersionCode`: 1
- `downloadUrl`: v1.0.1 GitHub Release APK
- `sha256`: current metadata contains the checksum; fetch live file before reusing or editing it.
- Production metadata URL: `https://raw.githubusercontent.com/teddypro1/Teddy-WebApp/main/update.json`.

Do not store a stale checksum in this history file; the live update.json is authoritative.

## Signing safety

Historical keystore identity (non-secret metadata only):
- File: `teddywebapprelease.jks`
- Keystore name: `TeddyWebAppRelease`
- Alias: `teddywebapp`

Never request, expose, commit or upload the keystore password, keystore file, signing credentials or private keys. Preserve a safe backup. Never publish a release or replace update metadata without explicit user approval and verified artifact details.
