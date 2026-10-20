# Kids lock — design plan (draft, no code yet)

Status: plan only, for apukale to review. Target: a later launcher version (after 2.10).
Scope: com.gathin.tvhome only. No new permissions beyond what the launcher already has
(PowerMenuService accessibility service, WRITE_SECURE_SETTINGS); fully offline.

## 1. Parent PIN
- One parent PIN (4–6 digits, length to be decided) entered on a D-pad number pad
  (3x4 grid of digits plus Back and OK; digits are never echoed, only dots).
- Stored as a salted hash (PBKDF2-SHA256, random salt) in the launcher's private
  SharedPreferences. The PIN itself is never stored.
- Wrong-PIN back-off: after 5 wrong tries, a 1-minute wait that doubles each time
  (up to 15 min). A counter shows what's left. No lock-out that needs adb.
- "Unlocked" grace: after a correct PIN, locks stay open for N minutes (default 5)
  or until Home is shown again / screen off, whichever first.

## 2. Per-tile lock
- Hold OK on a tile → new menu item **Lock with PIN** / **Unlock** (asks for the PIN).
- Locked tiles show a small padlock badge. Launching one asks for the PIN first;
  a correct PIN opens it (and starts the grace period).
- Stored per package, like order / hide / logo mode. Hidden apps stay hidden;
  "Unhide apps" and "Reset order" also require the PIN when the lock is on.

## 3. Locking the launcher's own controls
- Option **Lock launcher settings** (on by default once a PIN exists): opening Launcher
  settings, the hold-OK menu on Settings (Launcher settings / Start screensaver now /
  Restart TV), Module cards, DNS, Clear app cache, and the tile hold-OK menu
  (Move / Hide / Lock) all ask for the PIN.
- The **Settings** pill (system TV Settings) and **Inputs** pill can each be set to
  "Ask for PIN". This is the main way to close the obvious bypasses (see §7).

## 4. Bedtime (optional)
- Two schedules: **School nights** (default Sun–Thu) and **Weekend** (Fri–Sat), each
  with a bedtime and a wake time. Which days count as school nights is configurable.
- 10 minutes before bedtime: the launcher brings a small full-screen "Bedtime in 10 min"
  notice to the front once (also over another app), dismissible with OK, so the show can
  be wrapped up. No system overlay permission is needed: Android 10+ blocks background
  activity starts, but an app whose AccessibilityService is bound by the system is
  exempt, so this relies on PowerMenuService staying enabled (it came back null after the
  2.7 update; the bedtime code must detect that and show a warning in Launcher settings).
- At bedtime: the launcher brings up a full-screen **Bedtime** screen (clock, countdown
  "TV turns off in 60 s", calm night-mode colours). OK / Back cannot dismiss it.
  - **Extend 30 min** → asks for the PIN, then reschedules bedtime +30 min (can be
    repeated).
  - After the countdown: put the TV to sleep. Preferred route: the existing
    PowerMenuService using `GLOBAL_ACTION_LOCK_SCREEN` (API 28+) or, if that does not
    blank the TV, open the power dialog (`GLOBAL_ACTION_POWER_DIALOG`) — the latter
    still needs a click, so it is a fallback only. Alternative: the screen-off timeout
    trick (WRITE_SETTINGS) is not reliable on Android TV and is not planned.
  - Until wake time, pressing Home / turning the TV on again shows the Bedtime screen
    straight away (launcher is Home, so this works for anything that returns Home).
- Scheduling: one exact alarm per transition (AlarmManager setExactAndAllowWhileIdle
  from the launcher; no polling), re-armed on boot, time/date/timezone change.
  Needs RECEIVE_BOOT_COMPLETED (normal permission, auto-granted) — the only new
  manifest entry. Exact alarms on Android 11 need no extra permission.

## 5. PIN setup and reset
- Setup: Launcher settings → **Kids lock** → **Set PIN** (enter twice). Turning the
  kids lock off, changing the PIN, or editing bedtimes all require the current PIN.
- No on-TV "forgot PIN" (that would defeat the lock). Reset only from Raider via adb:
  `adb -s <TV_IP>:5555 shell am broadcast -a com.gathin.tvhome.RESET_PIN -n com.gathin.tvhome/.PinResetReceiver`
  The receiver is protected with `android:permission="android.permission.DUMP"`
  (held by the adb shell, not by normal apps), so only adb can trigger it. It clears the
  PIN hash and turns the lock off, then shows a "PIN was reset" notice on Home.

## 6. Tests
- Pure-Java tests for: PIN hashing/verify, back-off timing, grace period, schedule
  selection (school night vs weekend, bedtime crossing midnight, +30 min extensions),
  next-alarm computation. Same capped-loop rules as the Panchang code.

## 7. Limits (honest)
- A launcher lock is a speed bump, not parental-control software. It can be bypassed:
  - via **TV Settings** (Apps → open any app, or switch the default Home back) unless the
    Settings pill is PIN-locked; even then, the remote's own Settings / Input buttons
    (if the remote has them) open system screens directly, which the launcher cannot
    intercept;
  - via **Inputs** (HDMI devices are not controlled at all);
  - via **casting** (Chromecast built-in / YouTube "Watch on TV" start apps directly,
    without the launcher) — these are not blocked;
  - via voice search / Assistant deep links, notifications, and apps that start other
    apps;
  - by anyone with adb access or who factory-resets the TV.
- Bedtime "sleep" depends on what the TV's global actions do; if lock-screen does not
  turn the panel off on this TV, bedtime falls back to the Bedtime screen staying up
  (dim, no apps) until wake time.
- Locked-tile protection only applies to launches from the launcher.
- For stronger control, the real options are Google Family Link (needs Google TV Home,
  which is disabled here) or a router-level schedule / DNS filtering (e.g. AdGuard
  Family DNS, already in the DNS picker).

## 8. Decisions apukale needs to make
1. Which tiles get locked (e.g. Netflix, Prime Video, JioHotstar, NetMirror, Cricfy,
   YouTube/SmartTube, all of them except a kids list)?
2. PIN length: 4 digits or 6 digits?
3. Bedtime on? If yes: school-night bedtime and wake time, weekend bedtime and wake
   time, and which days are school nights (default Sun–Thu nights).
4. At bedtime: actually put the TV to sleep, or just show the Bedtime screen?
5. Should the Settings pill and Inputs pill also ask for the PIN?
6. Grace period after a correct PIN: 5 min, 15 min, or until the TV goes back to Home?
7. Should the "Extend 30 min" be limited (e.g. max 2 extensions per night)?
