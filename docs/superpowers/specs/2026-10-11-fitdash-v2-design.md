# fitdash v2 — design

Date: 2026-10-11
Status: approved in brainstorming, awaiting spec review
Builds on: `2026-10-11-fitdash-design.md` (v1). Everything in v1 stays unless changed here.

## Goal

Make fitdash feel closer to Ultrahuman: swipeable pages, three score rings, simple daily suggestions. Move the Data sources list out of the main page into an info sheet. Add a support link and an "Ask AI" share button.

## Decisions

| Topic | Decision |
|---|---|
| Navigation | Four full pages you swipe between: Today → Sleep → Activity → Trends. Page dots top-left. |
| Info | ⓘ top-right on every page opens a bottom sheet: Data sources, privacy note, Support link, version. |
| Hero | Orb always shows Recovery (the v1 readiness score). The 16:00 switch is removed. |
| Rings | Recovery, Sleep, Movement under the orb; tapping one jumps to its page (Recovery stays on Today). |
| Suggestions | 2–3 rule-based "Do this today" lines on Today. |
| Ask AI | Button on Today shares a plain-text summary of the last 30 days through Android's share sheet. |
| Support | Link in the ⓘ sheet, shown only when a build flag is on (off for a Play build). |
| Sign-in | None. |
| Licence | GPL-3.0 (done). |

Out of scope: MCP server (possible v3, opt-in, separate), Pro features, widgets, notifications.

## Pages

All pages share the v1 look (dark, orb gradient colours, cards fade up, charts draw in, count-ups) and pull-to-refresh. Each page scrolls vertically; swiping horizontally changes page.

### Top bar (every page)

Left: four page dots, active dot wider. Right: ⓘ button.

### ① Today

1. Orb with the Recovery score (v1 readiness: Ready / Calibrating / NoData states unchanged), band word, one line of advice, "Based on …" line when inputs are missing.
2. Three rings, each a small animated arc coloured by its score:
   - **Recovery** = readiness score.
   - **Sleep** = last night's sleep score.
   - **Movement** = today's steps as % of the 9,000 goal, capped at 100.
   A ring with no data shows a grey empty arc and "—". Tapping Sleep or Movement swipes to that page.
3. **Do this today** card: up to 3 suggestions from the rules below. Hidden when no rule fires.
4. **Ask AI about my data** button.

### ② Sleep

Last night: duration, sleep score, hypnogram, stage totals (v1 Sleep card). Overnight vitals (v1 card). **Sleep · 7 nights**: bars of hours asleep, coloured by each night's sleep score. Empty state when there is no sleep data.

### ③ Activity

Today: steps with a goal ring, hourly bars, active minutes / km / kcal (v1 Activity card, now always today). **Steps · 7 days**: bars, coloured by % of goal. Empty state when there is no activity data.

### ④ Trends

HRV · 14 days and Resting HR · 14 days line charts. Readiness · 7 days bars (v1 Week card). Every generic data type with data, as v1 generic cards. Empty state when nothing qualifies.

### ⓘ sheet

Modal bottom sheet:
- **Data sources**: the v1 list (type, ✓ app / no data / no access).
- One line: "fitdash has no internet access. Your data never leaves this phone unless you share it."
- **Support fitdash** row opening the support URL in the browser, only when `BuildConfig.SHOW_SUPPORT` is true. The URL is a single constant.
- App version.

The Data sources card is removed from the scrolling page.

## Suggestions (rules)

Evaluated in this order, first 3 that fire are shown:

| Condition | Text |
|---|---|
| Readiness band Primed | "Good day for a hard workout." |
| Readiness band Steady | "Moderate effort today. Keep workouts under an hour." |
| Readiness band Recover | "Rest or an easy walk today." |
| Last night asleep < 7h | "Get to bed 30 minutes earlier tonight." |
| HRV ≥ 10% below baseline | "Go easy on caffeine and alcohol today." |
| Hour ≥ 16 and steps < goal | "N steps to go. A 15-minute walk does it." (N formatted with thousands separators) |

Calibrating or NoData readiness fires none of the band rules.

## Ask AI summary

Plain text, shared with `Intent.ACTION_SEND` (`text/plain`) through the system chooser. Content:

```
My health data from fitdash (Health Connect), generated <date time>.
Please help me understand it. Baselines are my 30-day medians.

Today: recovery <score/band or calibrating>, sleep <h m> (score <n>), HRV <n> ms (baseline <n>), resting HR <n> bpm (baseline <n>), steps so far <n>.

Last 30 days (date, recovery, sleep_hours, hrv_ms, resting_hr, steps):
2026-09-12, 71, 7.4, 44, 60, 8120
...
Other data (latest): Weight 72.4 kg, ...
```

Missing values are blank. Nothing is sent anywhere by fitdash itself; the user picks the app.

## Code changes

- `metrics/Today.kt`: remove `HeroMode` and the card plan; `TodayData` gains `sleepWeek`, `stepsWeek`, `movementScore`, `suggestions`, `restingHrTrend` (in `HeartSummary`), and the activity summary is always today. `buildToday` still takes `now`.
- New `metrics/Suggestions.kt`: pure rules above.
- New `metrics/AiSummary.kt`: pure text builder.
- `ui/today/` replaced by `ui/pages/`: `PagerScreen` (HorizontalPager + top bar + pull-to-refresh), `TodayPage`, `SleepPage`, `ActivityPage`, `TrendsPage`, `InfoSheet`, `Rings`. Existing cards are reused and the Sources card moves into `InfoSheet`.
- `app/build.gradle.kts`: `buildConfig = true`, `SHOW_SUPPORT` true for debug and release (flip to false for a Play build).

## Testing

- Unit tests: suggestion rules (each row, order, cap of 3, calibrating), AI summary (header, a full row, blanks for missing values, generic line), `movementScore` cap, 7-day sleep/steps series, resting-HR trend.
- Compose previews for each page with `PreviewData`.
- On device: swipe between pages, ring taps jump to pages, ⓘ sheet opens with Data sources, Ask AI opens the share sheet with the summary.

## Open item

The Support URL (Buy Me a Coffee / GitHub Sponsors / UPI link) — Mayur to provide.
