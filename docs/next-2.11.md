# Next: launcher 2.11 (paused)

Status: shipped as launcher-v2.11 (apukale said go, 1:33 PM IST, 20 Oct 2026). Earlier paused at 1:24 PM IST. Work in progress is stashed in C:\ai\cleanyourtv as `2.11-wip (redesign, paused)` (git stash list). Version 2.11 / versionCode 14 (2.10.1 took 12, hotfix 2.10.2 took 13). Also merge branch hotfix-2.10.1 into rearrange-apps first.

Additions requested after the spec below: Panchang restyle with the same design system (indigo palette + accent, frosted cards over the shuffled photo background with scrim, Outfit hierarchy, Tithi/Nakshatra/Yoga/Karana cards with till times, timings card with the current one in accent, Sun/Moon card with moon icon, pill button row with focus glow; keep all 2.10 D-pad/BACK/date-picker behaviour).

## Full Codex spec used

Launcher 2.11 for "Raider Launcher" (repo cleanyourtv, module launcher/, package com.gathin.tvhome; branch rearrange-apps, HEAD 4263257 = 2.10). Bump versionCode 12 / versionName 2.11. Keep EVERY existing setting, behaviour, pref key and default; this release is a visual redesign + reorganisation only. Target: Android TV 11 (API 30), 2 GB RAM, D-pad only. minSdk 26 / compileSdk 34.

PART A - Launcher settings redesign (LauncherSettingsActivity + its layout(s)).
1. Two-pane TV layout:
   - Left category rail ~280dp wide: icon + label per category: Home, Screensaver, Calendar, Weather, Apps, Network / DNS, Night mode, Kids lock, About. Icons: simple vector drawables you create (res/drawable/ic_cat_*.xml, 24dp, white, simple paths) - no downloads.
   - Right pane: the selected category's settings as frosted-glass cards (one card per group, rows inside).
   - Kids lock is a placeholder card: "Coming soon - see docs/kids-lock-plan.md" (not focusable beyond the card; no behaviour).
   - About: "Raider Launcher" + version (BuildConfig.VERSION_NAME / code), package, a short credits line (Weather data by Open-Meteo.com; Photos from Bing, Picsum, Unsplash), plus the existing Home app / restore note rows if they fit better here.
   - Put every existing row into the most sensible category, e.g.:
     * Home: apps per row, animations, follow theme, button labels, greeting (if exposed), module cards, reset order, unhide apps, default Home app (+ restore note).
     * Screensaver: style, photo interval, photo sources, photo storage, refresh photos now (with counts/last refresh), start after, start screensaver now.
     * Calendar: Hindu calendar on/off, calendar text size, screensaver calendar on/off.
     * Weather: weather toggle + status.
     * Apps: clear app cache (free space/cache size), app-related items.
     * Network / DNS: the DNS picker row(s).
     * Night mode: toggle, start, end.
     If a row clearly fits elsewhere, use judgement; nothing may be lost. Keep all existing click handlers/dialogs/pickers working (reuse the existing methods; move views, do not rewrite logic).
2. Background: the Home background (same Config background colour/wallpaper Home uses), downsampled heavily (e.g. inSampleSize so the longer edge is <= 480 px), blurred on a background thread with a cheap pure-Java box blur (3 passes, radius ~12 px at that size) - NOT RenderEffect - then shown scaled up (centerCrop) with a dim overlay. Pure-Java blur in a testable helper (BoxBlur on int[] ARGB). If no wallpaper, use the indigo gradient only. Free the bitmap in onDestroy.
3. Palette (res/values/colors_raider.xml, shared with Panchang): base gradient #12132A -> #1C1E3F (top-left to bottom-right), card #CC1A1B2E (~80% of #1A1B2E) with a 1dp border #26FFFFFF and 16dp radius, primary text #F4F1EA, secondary text #A9ABC7, accent default marigold #F2A93B.
4. Accent colour option (new row in Home or About category - put it in Home as "Accent colour"): Marigold #F2A93B (default), Teal #2EC4B6, Rose #F06A8A, Silver #C9CED6. Pref "accent_color" (store the name). Accent applies to: focus outline/glow, toggle pill when on, selected rail item indicator/background tint, and the header title accent (e.g. a small accent bar or the version chip). Apply at runtime (GradientDrawable/ColorStateList built in code from the accent) so no recreation of resources per accent is needed; re-apply immediately when changed.
5. Typography: Outfit everywhere (res/font/outfit_regular, outfit_medium, outfit_light exist): header 28sp outfit_medium, row titles 20sp, values/subtitles 15sp secondary colour, rail labels 18sp.
6. Rows: 72dp tall, 16dp rounded, icon left (category icon or a small row icon - optional), title + current-value subtitle (e.g. "Large", "10 PM - 6 AM" with an en dash, "AdGuard"), control on the right: large pill toggle (52x30dp-ish, accent fill when on, #3A3C5A when off, white knob) for booleans; a chevron ">" for rows opening dialogs/sub-screens. Focus: scale 1.03, 2dp accent outline + soft glow (an outer accent stroke at ~35% alpha, or elevation-free layered GradientDrawable), background lightened (+8% white); 150 ms animations (ViewPropertyAnimator). No shadows that cost memory.
7. D-pad: Right from the rail enters the content (first row of the current category); Left from content returns focus to the selected rail item; BACK from content -> focus rail; BACK from rail -> finish(). Moving focus up/down in the rail switches the category (content updates without stealing focus). Keep focus memory per category if cheap.
8. Header: "Raider Launcher" (28sp) + version chip (accent outline) at top-left of the right pane or across the top, and a small TextClock (h:mm a) top-right.
9. Memory: no RecyclerView needed (rows are few); create views once; only one blurred bitmap; RGB_565 where possible.

PART B - Panchang screen restyle (PanchangActivity), same design system and shared colors/styles:
- Background: the existing shuffled cached photo (keep 2.10 logic: random initial photo, Change background, safe decode, no recycle while attached) with a dark scrim; fallback to the indigo gradient (instead of plain Home background) when no photo.
- Header: large date (Outfit medium ~34sp) "Tuesday, 20 October 2026", subtitle with Hindu month + paksha, Vikram Samvat / Shaka, weekday Hindi name.
- Cards (frosted, same card style), in a 2- or 3-column grid that fits 1920x1080 without scrolling if possible (ScrollView still allowed):
  * Tithi, Nakshatra, Yoga, Karana: each card = title (small caps, secondary), current value (20-22sp primary), "till 12:50 PM" (+ "then Dashami till ..." line if it changes during the day).
  * Timings card: Rahu Kalam, Yamaganda, Gulika, Abhijit - rows "Rahu Kalam  2:56 - 4:24 PM"; when viewing today, the one in progress highlighted with the accent (accent text + small accent dot), past ones dimmed.
  * Sun & Moon card: sunrise/sunset, moonrise/moonset, moon phase icon (existing drawable, larger ~36dp) + "Moon in Makara till ...", Sun rashi.
  * Festivals card only when there is a festival / ekadashi / purnima / amavasya.
- Button row: Back, Change background, Previous day, Today, Pick date, Next day as pill buttons (44dp tall, 22dp radius, card fill, Outfit 16sp), same focus glow/scale/accent outline as settings. (Order: keep Back first and Change background next to it, as in 2.10.)
- Keep ALL 2.10 behaviour: D-pad Left/Right = previous/next day when focus is not in the button row; BACK closes to Home (dispatchKeyEvent backDown logic + goHome()); date picker dialog (restyle the dialog panel with the card style if easy; keep its key handling: UP/DOWN change, LEFT/RIGHT move, OK applies and dismisses, BACK cancels only the dialog); background compute on the worker; today refresh scheduling.

PART C - shared resources and code:
- res/values/colors_raider.xml, res/values/styles_raider.xml (text appearances, card/row/pill styles), drawables for card/row/pill backgrounds; a small Java helper RaiderTheme (accent lookup from prefs, builders for focus/toggle drawables, applyFocusAnimation(View)) used by both activities.
- Unit tests (JUnit, @Test(timeout=5000), pure Java): accent name -> colour mapping incl. unknown/empty -> marigold; BoxBlur on a tiny int[] image (uniform image stays uniform; a single bright pixel spreads and total brightness is roughly conserved; handles 1x1 and edges; loops bounded); time-range formatting "2:56 - 4:24 PM" style helper if you add one; category-of-row mapping if implemented as a pure table.
- README (launcher/README.md): 2.11 note with before/after: before = single long list in default styling; after = two-pane rail with categories, indigo palette, frosted cards, accent colour option, Outfit typography, Panchang cards.

Rules: edit only files under launcher/ (and READMEs); do NOT touch docs/, backups/, releases/, tools/, launcher/local.properties; no gradle, adb or git commands. ASCII-only Java string literals (use \u escapes, e.g. \u2013 en dash, \u00B7 middle dot). Real newlines between statements (never literal `r`n or \n text in code). Named constants only (LinearLayout.VERTICAL, Typeface.BOLD, Gravity.*, View.*) to keep lint clean. XML style items that are floats (shadowRadius, letterSpacing) have no units. Every helper method you call must exist (e.g. dp(int) in each activity). Compile mentally against API 34 framework only (check app/build.gradle dependencies before using androidx). Print a concise summary listing what went where (category -> rows) and files changed.
