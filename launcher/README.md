# Launcher

Hold OK on an app tile and choose **Use app logo** or **Use banner** to switch its artwork. The choice is saved per app on the TV; `app.netmirror.newtv` defaults to its launcher logo.

The screensaver can use locally stored photos from Bing's daily photo feed, Unsplash, and Picsum. Choose any combination in Launcher Settings > Screensaver. Bing refreshes daily, Unsplash every 12 hours, and Picsum weekly over unmetered Wi-Fi; downloads run sequentially in small batches (up to 8 Bing or 10 Unsplash/Picsum images per job). Unsplash list requests are limited to four per rolling 24 hours. The combined storage limit is 100 MB, 200 MB (default), or 500 MB. When space is needed, the oldest Picsum photos are removed first, then the oldest Unsplash photos, followed by Bing photos older than the 8 most recent Bing days. Downloads stop if the data partition has less than 2 GB free. Slideshow order starts with newest Bing photos, then interleaves Unsplash and Picsum. Unsplash photos display "Photo by <name> on Unsplash" attribution. The app also sends Unsplash's required download tracking request once after each successful photo download. Cached photos can also be selected as Panchang backgrounds.

To enable Unsplash, add `UNSPLASH_ACCESS_KEY=<your Client-ID key>` to `launcher/local.properties`. This local file is not committed. If the key is absent, Unsplash is silently skipped.

Version 2.5 adds an on-TV **Clear app cache** action. For broader cache access on a build that allows it, grant:

```sh
pm grant com.gathin.tvhome android.permission.CLEAR_APP_CACHE
appops set com.gathin.tvhome GET_USAGE_STATS allow
```

The `CLEAR_APP_CACHE` grant may be refused on non-system builds. The usage-stats app-op enables the total app-cache size in the settings row; clearing can still try Android's public storage allocation API without it.

Version 2.6 adds **Restart TV** to the Settings hold-OK menu. It opens Android's power menu so you can choose Restart. Enable **CleanYourTV power menu** in Settings > Accessibility first. To enable it over adb (append it to any existing enabled services):

```sh
settings put secure enabled_accessibility_services "<existing>:com.gathin.tvhome/com.gathin.tvhome.PowerMenuService"
settings put secure accessibility_enabled 1
```

Version 2.9 adds a full Panchang screen with offline timings and date navigation. On Home, focus the Hindu calendar stack with D-pad Up from the first app row or D-pad Right from the clock area, then press OK.

Version 2.10 adds Night mode (on by default), with configurable start and end times in Launcher Settings. Night mode warms and dims Home and screensaver content during the selected time window; matching start and end times disable the window. It also fixes the remote's BACK key on the Panchang screen (BACK returns to Home; with the date picker open, BACK closes only the picker, and OK in the picker now closes it) and adds an on-screen **Back** button as the first button in the Panchang screen's bottom row. The follow-up 2.10 fixes default the screensaver to Photos + clock + calendar and schedule a throttled photo refresh when its selected cache is empty, with refresh diagnostics and last-refresh/error status. Weather now logs and displays fetch status in Launcher Settings, appears on Home and the screensaver, and refreshes every 30 minutes while the screensaver runs. The screensaver clock and calendar align vertically; Home places the date/weather row above a compact clock-divider-calendar row to remove the gap. Launcher Settings can hide quick-action button labels (icons-only by default). Panchang now uses the bundled Outfit font and can cycle through cached photos as a dimmed background with a **Change background** button.

2.10 also renames the launcher to **Raider Launcher** (app label, screensaver label "Raider Launcher screensaver" in TV Settings > Screensaver, and in-app text); the package stays com.gathin.tvhome and signing is unchanged. Screensaver photos are now shuffled on every start (all chosen sources mixed, never the same first photo twice in a row, reshuffled without repeating the photo just shown) and cross-fade between two image views without recycling attached bitmaps, which fixes the black photos after the first one; failed decodes are skipped and logged under TvHomePhotos. The Panchang background starts on a random cached photo.

2.10.1 makes Picsum opt-in (the **All** setting still selects Bing, Unsplash, and Picsum) and limits Unsplash searches to landscape nature photos with `content_filter=high` plus a people/phone keyword filter. On the first 2.10.1 run, cached Picsum photos and older Unsplash travel photos are removed once; filtered Unsplash photos are then refreshed.

2.10.1 (hotfix): the Home quick-action pills show their icons again in Icons-only mode (2.10 removed the icon together with the text); icon-only pills are compact capsules with a centred 26 dp icon, and Show text mode shows icon + text. The pills now sit at the top right of the Home header, level with the date and weather line; D-pad Up from the clock or the Hindu calendar block reaches them. On the screensaver, the calendar box is top-aligned with the visual top of the clock digits (measured from the font at runtime, logged under TvHomeDream).

2.10.1 screensaver: everything is in one rounded semi-transparent black panel (24 dp corners, 30 dp padding) - clock and date on the left, a thin divider, then weather and the Hindu calendar lines on the right, top-aligned - drifting as one block; the photo credit stays small at the bottom right.
