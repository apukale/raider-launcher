# Launcher

Hold OK on an app tile and choose **Use app logo** or **Use banner** to switch its artwork. The choice is saved per app on the TV; `app.netmirror.newtv` defaults to its launcher logo.

The screensaver can use locally stored photos from Bing's daily photo feed, Picsum, or both. Bing photos refresh daily and Picsum photos refresh weekly over unmetered Wi-Fi; downloads run sequentially in small batches (up to 8 Bing or 10 Picsum images per job). Choose the source and a combined photo storage limit of 100 MB, 200 MB (default), or 500 MB in Launcher Settings > Screensaver. Both sources share the selected limit. When space is needed, the oldest Picsum photos are removed first, followed by Bing photos older than the 8 most recent Bing days. Downloads stop if the data partition has less than 2 GB free. Photos are used only by the screensaver, with captions shown from each source.

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
