# Launcher

Hold OK on an app tile and choose **Use app logo** or **Use banner** to switch its artwork. The choice is saved per app on the TV; `app.netmirror.newtv` defaults to its launcher logo.

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
