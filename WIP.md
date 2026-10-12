# WIP — handoff

Last updated: 2026-10-12. Released: **v0.4.0** (tag `v0.4.0`, APK + Play AAB on GitHub Releases). Site: https://mayurrawte.is-a.dev/fitdash/

## Not yet checked on a device

- v0.3.0: tap any card to open its detail sheet; calmer Trends colours.
- v0.4.0: water card (+250 / +500, undo), height dialog, BMI tile, snackbar when a Health Connect write fails.
- Play build: the permission screen should list 20 permissions.
- Week strip: tapping a day should switch the date (failed once in testing).
- Before installing the release APK, uninstall the debug build (the signatures differ).

## Waiting on data

- The Fitbit Air has only written Sleep, Steps and raw heart rate so far. After a full night's sleep, open the Fitbit app, then check fitdash's ⓘ Data sources for HRV, SpO2 and resting heart rate. If they come through, tune `metrics/Tuning.kt`.

## Play submission (Mayur)

- Back up `~/.android/fitdash-release.jks` and `~/.android/fitdash-release-credentials.txt` to a password manager.
- Follow `docs/play/PUBLISHING.md` once the friend's developer account is ready. Have someone review the privacy policy before submitting.

## Later

- Splash screen is always teal, so other themes flash teal before the app loads.
- MCP server (v3, opt-in, local Wi-Fi only).

## Release steps

Bump `versionCode` and `versionName` in `app/build.gradle.kts`, add `fastlane/metadata/android/en-GB/changelogs/<code>.txt`, tag `vX.Y.Z` and push the tag. Build with JDK 17.
