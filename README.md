# Raider Launcher

An Android TV and Google TV home screen. **3.0.0** (versionCode 51) is the version running on our TCL.

![Home on a TCL, with the clock, weather, Panchang, and app rows over a full-bleed photo](docs/screenshots/home.png)

Raider is the launcher we use every day. Home is your apps, a clock, the weather, and the Hindu calendar. There is no recommendations row. Photos can fill the background. Settings are built for a remote on a 1080p set.

## Where it comes from

[CleanYourTV](https://github.com/Gathin23/cleanyourtv) by Gathin23 is the original project. It is a small native launcher, plus a careful way to disable manufacturer bloat without uninstalling it. Raider is a fork in the ordinary sense: the same launcher, carried on for home use. GitHub does not list this repository as a formal fork. The connection is this credit, the “Fork of CleanYourTV” line in About, and a git remote.

To follow upstream, add that remote once:

```bash
git remote add upstream https://github.com/Gathin23/cleanyourtv.git
```

In the app, About says **Made by apukale**.

Stock Android TV and Google TV put a recommendations row, and often an ad, between you and your apps. Raider replaces that home screen. It does not uninstall the apps that were already on the TV.

## Features

Shots below are from a TCL Android TV 11 running 3.0.0 (51).

### Home

The top of Home is the time, the date, a one-line weather reading, and a short Panchang line (tithi, nakshatra, and yoga when the Hindu calendar is on). Under that, apps sit in rows. The number of apps in a row is a setting. Hold OK on a tile to choose the app logo or the banner for that app.

Inputs, Wi-Fi, and Settings sit in a short row on the right. Full-bleed photos rotate behind everything while Photos is on. While Photos is off, Home uses one gradient and does not advance.

### Home Layout

![Home Layout settings, with the Weather, World clocks, Panchang, and Screensaver blocks above apps per row and photo controls](docs/screenshots/settings-home-layout.png)

Settings → Home Layout opens with four blocks: Weather, World clocks, Panchang, and Screensaver. Each one opens that screen. They are not drawn on Home itself.

Under the blocks: apps per row (4, 5, 6, or 7 on this set), button labels for the Inputs / Wi-Fi / Settings row, unhide apps, Photos on or off, and how often the background changes. The same interval is used by the screensaver slideshow. Home shuffles the order, so a new session does not replay the same sequence. The interval row rests while Photos is off.

### Appearance

![Appearance settings, Indigo + Marigold selected, with fonts and animation switches](docs/screenshots/settings-appearance.png)

Seven colour themes:

- Indigo + Marigold
- Midnight + Ice
- Espresso + Amber
- Forest + Mint
- Plum + Orchid
- Onyx + Crimson
- True Black + Silver

Fonts can be set separately for Home, launcher settings, the screensaver, and Panchang. Outfit is the default. The chooser also has Sora, Sofia Sans, Nunito, Work Sans, Lexend, Atkinson Hyperlegible, Space Grotesk, Inter, Source Serif 4, Roboto, Poppins, Roboto Flex, and Manrope.

Animations can be turned off. Advanced effects add a lifted tile, icon zoom, and smoother colour changes. The settings screen says they may use more memory, and that the difference depends on the TV. Turning animations off also turns advanced effects off.

### Screensaver

![Screensaver settings: start now, Clock with date, and a five-minute idle delay](docs/screenshots/settings-screensaver.png)

![The screensaver, clock and date over a photo, with the Panchang box and world clocks along the bottom](docs/screenshots/screensaver.png)

Start it now, or let it start after the TV has been idle (1 to 30 minutes, or never). The style is Clock, Clock with date, or Photos.

Photos come from the library in Photos settings. World clocks can be shown along the bottom. The Hindu calendar can stay on Home only, or on Home and the screensaver. A weather line can sit on the screensaver as well.

### Photos

![Photos settings, with sources, a two-minute rotation, a 500 MB cap, and 60 cached photos](docs/screenshots/settings-photos.png)

Photos are downloaded on the TV and shown from local storage. The master switch is here. Sources you can turn on:

- Bing
- Unsplash
- NASA APOD
- NASA Images
- Wikimedia POTD
- Pexels
- Pixabay
- Paintings (public-domain works from the Art Institute of Chicago and The Met)

Unsplash, Pexels, and Pixabay appear only when their keys are in `launcher/local.properties`. The file is not committed. Leave a key out and that source is skipped. Do not put a real key in the README or in git.

Each source refreshes about once a day on unmetered Wi-Fi, one download at a time. Storage can be capped at 100 MB, 200 MB, or 500 MB. Downloads stop if the data partition has less than 2 GB free. Unsplash photos keep the photographer credit.

### Calendar & Clocks

![Calendar & Clocks settings, with the Hindu calendar and City 1–4 in a 2×2](docs/screenshots/settings-calendar-clocks.png)

![Panchang for a day in Bengaluru, with tithi, nakshatra, yoga, karana, and the daytime periods](docs/screenshots/panchang.png)

![World clocks: India in the header, and Los Angeles, New York, London, and Dubai in a 2×2](docs/screenshots/world-clocks.png)

Open Panchang for the full Hindu day: tithi, nakshatra, yoga, karana, moon and sun rashis, and the Rahu Kala, Yamaganda, Gulika, and Abhijit periods. It is computed on the TV. The place follows the weather city.

The Hindu calendar can be turned off. Text size is a setting. Show is either Only Home, or Home and Screensaver.

World clocks keep India as the home clock. City 1–4 are a 2×2 in settings (Los Angeles, New York, London, and Dubai until you change them). Each city can be given another timezone or a shorter name. The World clocks screen shows India beside the title and the four cities in a 2×2, with the local time and the GMT offset. “Show world clocks on Screensaver” is its own switch.

### Weather

![Weather settings: show on Home and Screensaver, open the forecast, city Bengaluru, degrees Celsius](docs/screenshots/settings-weather.png)

![The weather screen: current conditions and the next hours on the left, an India cloud map on the right](docs/screenshots/weather.png)

The forecast comes from Open-Meteo for the city you pick. Search by name. That city is also the Panchang place. Units are °C or °F. The line on Home can be turned off.

The map is an India frame. Cloud cover on that map uses OpenWeatherMap when `OPENWEATHERMAP_API_KEY` is set in `launcher/local.properties` at build time. With no key, the map still draws and the corner note says the key is missing. The key stays in that local file.

### Night Mode

![Night Mode settings: on, 45% dim, 10:00 PM to 6:00 AM](docs/screenshots/settings-night-mode.png)

Night mode lowers the panel brightness on a schedule. You set the dim amount and the start and end times. Start now overrides the schedule until you stop it.

### Advanced

![Advanced settings: Clear cache, Free RAM, TV settings, Private DNS, and Default Home](docs/screenshots/settings-advanced.png)

Clear cache frees cached data for apps on the TV. Free RAM closes background apps. Neither one removes apps, photos, or launcher settings. The same two actions sit at the top right of every settings screen.

Private DNS can be left off, set to Automatic, or pointed at a provider (AdGuard, Google, Cloudflare, Quad9, or a hostname you type). Writing that setting needs `android.permission.WRITE_SECURE_SETTINGS`, granted with adb if the TV asks.

Default Home sets this launcher as the home screen. You can also open the TV’s own settings, reset the app order, and send apps-per-row and animation choices back to the theme defaults. These rows open directly. There is no PIN. Kids is listed as coming soon.

### About

![About Raider Launcher 3.0.0 (51), made by apukale, with the GitHub link and a QR code](docs/screenshots/settings-about.png)

About shows the version and version code, “Made by apukale”, the GitHub link, a QR code for that link, and “Fork of CleanYourTV”. It also shows free RAM and storage.

## Tested on

TCL G03_4K, Android TV 11, 2 GB RAM, 32-bit (`armeabi-v7a`). The launcher targets that class of set: a 1080p screen and a remote, with settings pages that fit one screen.

## Build and install

The Android project is in `launcher/`. You need JDK 17 and Android SDK 34. Android Studio can open that folder, or from a terminal:

```bash
cd launcher
./gradlew :app:assembleRelease
```

The release APK is signed with the debug key so a sideload can replace a debug install. It is not a Play Store build. The APK is `launcher/app/build/outputs/apk/release/app-release.apk`.

Optional keys belong in `launcher/local.properties`, which is gitignored:

- `OPENWEATHERMAP_API_KEY` for the cloud layer
- `UNSPLASH_ACCESS_KEY`, `PEXELS_API_KEY`, `PIXABAY_API_KEY` for those photo sources
- `NASA_API_KEY` if you have one; NASA calls fall back to the public demo key when it is absent

Never commit those values.

## Credits and license

Raider Launcher is derived from [CleanYourTV](https://github.com/Gathin23/cleanyourtv) by Gathin23. Thank you for the launcher this one is built on. Track that repository with a git remote named `upstream`, as shown above.

The app credits **apukale**.

Licensed under the MIT License. See [LICENSE](LICENSE). Copyright (c) 2026 Gathin. The Outfit typeface is under the SIL Open Font License; see `launcher/FONT-LICENSE-OFL.txt`.
