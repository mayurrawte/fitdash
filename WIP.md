# WIP — handoff

Last updated: 2026-10-13 (00:30). Everything below up to "Release steps" is committed as one commit on `main` and verified on a Pixel unless stated. Released: **v0.4.0** (tag `v0.4.0`, APK + Play AAB on GitHub Releases). Site: https://mayurrawte.is-a.dev/fitdash/

## v0.5.0 in the working tree, uncommitted

Spec: `docs/superpowers/specs/2026-10-12-fitdash-v0.5-design.md`. Implemented and unit-tested, not yet run on a device: workouts (rows, sheet, active minutes from sessions), heart-rate card and sheet, `READ_HEALTH_DATA_HISTORY` with a 6M trends view, blood pressure with diastolic, sleep efficiency / awakenings / latency, partial-day Movement delta, 11 sp type floor, 48 dp tap targets, drawn icons instead of text glyphs, 2-minute reload window on resume. Version bumped to 0.5.0 (code 5) with `changelogs/5.txt`.

Toolchain on this Mac: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`, SDK at `/opt/homebrew/share/android-commandlinetools` (platform 37.0, build-tools 37.0.0). `local.properties` is git-ignored and points there.

Later the same evening, also in the working tree: Pastel, Techno and Sunrise removed (stored values fall back to Teal), redesigned sleep and steps headers (smaller arc, score colour, glowing tip, count-up), a stepped hypnogram with lanes, last-7-days week strip, press-scale and haptics on every tappable surface, animated trio numbers and water bar, theme crossfade, and new settings (24-hour time, lb, °F, reduce motion, haptics, which cards to show). Splash exit guarded against a null icon view (the v0.4.0 launch crash in logcat).

Device-checked so far on the Pixel (debug build, Graphite): first-run permission flow, Today, Sleep, Activity, Trends, Settings, workout rows and heart-rate card render with real Fitbit data; no crashes from the new build. After the UI pass, also checked on the Pixel: the new Sleep header and lane hypnogram, the steps gauge, the heart-rate card with sleep and workout shading, the Settings toggles. Still unchecked: 1.3x font scale, the workout sheet, Trends 6M after granting history.

Known: a full Health Connect read takes 20–30 s on this phone with 30 days of Fitbit minute-level heart rate (the loading orb shows that long on a cold start). Worth caching the snapshot on disk or using the changes API in v0.5.1.

Also in the tree now: an on-disk snapshot cache (instant open after the first run; incremental reads from two days before the last read, parallel per type, full read once a day), a "Data up to … · Sync Fitbit" row on Today that opens the wearable app and refreshes on return, a hero orb that shows sleep or steps while recovery calibrates, a four-tile glance grid replacing the trio, Settings → Feedback rows that open prefilled GitHub issues, and a backlog of 14 issues in `docs/issues/`, with `docs/issues/ISSUES.md` (all in one), `LINKS.md` (prefilled new-issue links) and `scripts/file-issues.sh` for a laptop where gh is logged in.

Latest pass (13 Oct, early): numbers typeface setting (Inter / Space Grotesk / Instrument Serif, OFL, bundled), Graphite mint accent, tiles without hairlines, Today restructure (orb 172 dp, action card under the glance grid, workouts folded into one expandable row, compact water row), and chart touch: tap shows a value for 2.5 s, press-and-hold then drag scrubs, on the heart-rate line and every bar chart.

Final pass (13 Oct): sleep score is now four parts (duration 50, efficiency 15, restoration 20, restfulness 15; duration alone without stages) and the sleep sheet shows them. All 7-day, 30-day and 6-month charts are smooth monotone curves with a gradient, dots, a goal line and tap readouts; hourly steps are pill bars. Calibration shows seven dots filling night by night. Status words are 12 sp. A one-night chart says "one day so far".

Not done: fresh Play screenshots from this build (`fastlane/.../phoneScreenshots` still shows 0.4 mockups; pad a 1080×2424 capture to 9:16 with `sips --padToHeightWidth 2424 1363 --padColor 050909`), the typeface picker, workout sheet and Trends 6M were not seen on the phone, and nothing is pushed.

## Soft theme (13 Oct evening, uncommitted, not run on a device)

A light pastel theme modelled on two Dribbble references, now the default for fresh installs (existing installs keep their saved theme and Inter numbers). It has a `#F2F3F0` ground, white 28 dp cards, tiles tinted sky, sage, lavender and peach (`Tint` + `TintScope` recolour the text inside), a dark icon-only dock in place of the glass nav, a peach week-strip pill and date chip, and a white hero card with a ring in place of the orb. Outfit (OFL, bundled) is used for body and labels on Soft and is a fourth numbers option. Status-bar icons go dark on Soft, and on Android 13+ the splash colour follows the theme from the next cold start. Two Soft previews are in `PagerScreen.kt`. To check on the Pixel: every page, every sheet (they use `bgMid`), the share card, chart dots and gauge tips on white, and labels that are still uppercased in code.

## Device check, 16 Oct (Pixel 9a, Play debug build of v0.7.0)

Checked and fine: first-run Health Connect flow (history prompt, then the permission sheet; 23 of 24 granted, background off by design), Today with the calibration countdown, Sleep page with the one-night line and the calibrating Regularity card ("4 of 5 night pairs"), Trends Fitness tiles (VO2 estimate after setting age, sleep-debt strip, training load marker), Settings Gender dropdown and Show sleep score toggle, score hidden on Sleep (hours with "calibrating" chip), the theme menu readable on Soft in dark mode (the v0.5 bug), Soft light and Soft dark, Midnight Teal.

Not checked: Fitness age and Resting kcal (no weight in Health Connect on this phone), the Regularity card past calibration and its sheet (needs a fifth night pair), Reduce motion, Graphite.

Noted: the "Grant access" button can look stuck after the Health Connect sheet returns, because the first full read takes 20–30 s with no change on screen until it finishes. The loading screen should take over as soon as permissions return. Also the Height tile on Trends shows "1.5 M" (a Health Connect value) while BMI says "Add height in Settings"; worth reconciling.

## Not yet checked on a device

- v0.5.0: everything above. Watch the first-run permission screen (now asks for "read older data"), the heart-rate card with Fitbit's 1-minute samples, the workout sheet, Trends 6M after granting history, and every page at 1.3x font scale with the larger labels.

- v0.3.0: tap any card to open its detail sheet; calmer Trends colours.
- v0.4.0: water card (+250 / +500, undo), height dialog, BMI tile, snackbar when a Health Connect write fails.
- Play build: the permission screen should list 20 permissions.
- Week strip: tapping a day should switch the date (failed once in testing).
- Before installing the release APK, uninstall the debug build (the signatures differ).

## Waiting on data

- The Fitbit Air has only written Sleep, Steps and raw heart rate so far. After a full night's sleep, open the Fitbit app, then check fitdash's ⓘ Data sources for HRV, SpO2 and resting heart rate. If they come through, tune `metrics/Tuning.kt`.

## Play (15 Oct)

Own personal developer account (SNX Studio). App `app.fitdash` created, all declarations and the store listing done, v0.5.0 (code 5) live on internal testing and on the closed "Alpha" track with the "fitdash testers" list (14 people added). Everything sent to Google for review on 15 Oct; reviews take up to 7 days. Production needs 12 testers opted in to the closed track for 14 days, then "Apply for production".

- Play App Signing uses a Google-generated key; the local keystore is the upload key. Play and GitHub builds therefore have different signatures.
- Back up `~/.android/fitdash-release.jks` and `~/.android/fitdash-release-credentials.txt` to a password manager.
- Do not edit the listing or declarations while the review runs; each edit restarts it.
- v0.7.0 (code 7, this tree): sleep regularity card, calm single-night framing, show sleep score toggle (`docs/superpowers/specs/2026-10-16-fitdash-sleep-regularity-design.md`). Research behind the roadmap: `docs/research/2026-10-16-behaviour-science-notes.md`.
- v0.6.0 (code 6, tagged, on GitHub): fitness cards (`docs/superpowers/specs/2026-10-15-fitdash-fitness-cards-design.md`), gender setting, No data screen with app buttons, device-neutral copy, Soft dropdown fix. Upload to the closed track after the review approves.

## Later

- Splash screen is always teal, so other themes flash teal before the app loads.
- MCP server (v3, opt-in, local Wi-Fi only).

## Release steps

Bump `versionCode` and `versionName` in `app/build.gradle.kts`, add `fastlane/metadata/android/en-GB/changelogs/<code>.txt`, tag `vX.Y.Z` and push the tag. Build with JDK 17.
