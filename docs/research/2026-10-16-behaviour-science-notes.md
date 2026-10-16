# Behaviour science notes for fitdash

Date: 2026-10-16. Read before proposing a feature. Every feature should name the finding it rests on.

## Findings

1. Engagement in health apps is associated with six techniques: goal setting, self-monitoring, feedback on behaviour, prompts and cues, rewards, social support. The number of techniques does not predict ratings. Imperial systematic review of 28 studies (https://spiral.imperial.ac.uk/entities/publication/aee557cc-5bcb-4ee3-8cdb-011f02770c20); content analysis of 64 apps (https://www.ncbi.nlm.nih.gov/pmc/articles/PMC4132213/).
2. Implementation intentions ("if X, then I do Y") raise goal attainment, d ≈ 0.65 across 94 tests (Gollwitzer and Sheeran 2006); smaller for objectively measured physical activity, g ≈ 0.24–0.31 (Bélanger-Gravel 2013; Presseau 2017). One specific plan beats many vague ones.
3. Sleep trackers can produce sleep-related anxiety and perfectionism ("orthosomnia", Baron et al. 2017, J Clin Sleep Med; Sleep Foundation summary https://www.sleepfoundation.org/how-sleep-works/orthosomnia). Design response: no alarm colour on a single night, copy that de-emphasises one night, trends over nights.
4. Sleep regularity predicts all-cause mortality more strongly than sleep duration: 20–48 % lower risk in the more regular quintiles, UK Biobank, 60 977 adults (Windred et al. 2023, Sleep; https://www.ncbi.nlm.nih.gov/pmc/articles/PMC10782489/). Cribb et al. 2023 (eLife) agree. Observational, so association not cause.
5. Prompts work when delivered in a receptive moment; many just-in-time interventions were built without evidence (Nahum-Shani et al. 2017, https://pmc.ncbi.nlm.nih.gov/articles/PMC5364076; Künzler et al. 2022, https://pmc.ncbi.nlm.nih.gov/articles/PMC9494762).
6. Readiness-style scores read as arbitrary until a personal baseline exists; users treat them as useful but fallible. Show how many days of calibration remain, and present values against the person's own normal first.

## Design rules derived

- Baseline first, raw number second.
- Three colours; red only for 7-day trends and goals the user controls.
- One hero per screen.
- Progressive disclosure: number, then contributors, then formula.
- Plain words: "lower than your usual", not "needs attention".
- No streaks. Praise in the weekly review, against the user's own previous week.

## Daily return loop

The honest version of "hooking" people is a daily reason to open that pays off within seconds, plus a visible path through calibration. No streaks; streak loss is a leading reason people abandon trackers.

1. Calibration as a countdown: "Night 3 of 7, your baseline is forming", with what is already usable listed (hours, steps, regularity after 5 nights). (finding 6)
2. A one-sentence morning readout at the top of Today, under three seconds to read: sleep, one vital against baseline, one suggestion. Feedback on behaviour is the strongest engagement technique in the reviews. (finding 1)
3. Yesterday's if-then plan, checked with one question. Agency, not streaks. (finding 2)
4. Numbers only visible over time: regularity, baselines, fitness-age trend. (findings 4, 6)
5. Praise in the weekly review against the person's own last week. (finding 3)
6. The widget shows the same one-sentence readout, so the app earns the tap.

## Feature queue, in order

1. Sleep regularity card and calm sleep framing (finding 3, 4) — spec `docs/superpowers/specs/2026-10-16-fitdash-sleep-regularity-design.md`.
2. Morning readout sentence on Today and the widget, and calibration countdown (return loop 1, 2, 6).
3. One if-then plan with a next-morning check (finding 2).
4. One receptive-moment reminder, opt-in (finding 5).
5. Today hero rework: one hero, glance tiles (finding 6, design rules).
