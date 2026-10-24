# Raider Launcher

Raider Launcher is a home screen for Android TV. It is built for a TCL on Android TV 11: a clock, the Hindu calendar, weather, world clocks, and your apps, without a sponsored row in the way.

It started as a fork of [CleanYourTV](https://github.com/Gathin23/cleanyourtv). The package is still `com.gathin.tvhome`. This page is about Raider.

**Current build:** 2.11.22

## Screenshots

Placeholders until the TCL shots are attached.

| Home | Settings | World clocks |
|---|---|---|
| Clock, weather, and a focused app over a tinted screensaver photo | Appearance, with the eight colour themes | India and the four city slots, Back, Change background |

## What you see on Home

- The time is focusable. OK opens **World clocks**: India plus up to four cities, a Back button, and Change background (the same photo cycle as Panchang, with its own picture).
- Home does not label the clock "India time". The cities live on the world clocks screen.
- Weather and the Hindu calendar (Panchang) are focusable the same way. OK opens each full screen.
- Focusing an app tile puts a screensaver photo behind Home, darkened with the theme colour, plus the app's own colour glow. If the photo cache is empty, Home stays on the solid theme colour.
- The focused tile scales slightly (about 1.05), with a soft glow and an outline in the theme accent. The backdrop photo is blurred once at a small size. The rest of the screen is not blurred, so a Mali-G31 on 2 GB can keep up.
- A recommendations row appears only when the TV's Watch Next list has something to show, and only when the setting is on. An empty list falls back to the photo and tint. Catalog art from YouTube, Netflix, or Prime is not part of this build.

## Settings

The left rail is nine rows, each with an icon: Home Layout, Appearance, Screensaver, Photos, Calendar & Clocks, Weather, Night Mode, Advanced, About. The right-hand rows are text only.

Appearance switches eight colour themes:

| Theme | Background | Accent |
|---|---|---|
| Indigo + Marigold | `#12142B` | `#F6C453` |
| Midnight + Ice | `#070B14` | `#9FD6FF` |
| Espresso + Amber | `#1A120C` | `#E8A04A` |
| Forest + Mint | `#0C1612` | `#7DDCB0` |
| Plum + Orchid | `#160E18` | `#E29AD4` |
| Onyx + Crimson | `#0C0C0E` | `#E24B5A` |
| True Black + Silver | `#000000` | `#D5D8DE` |
| Porcelain + Espresso | `#F4EFE8` | `#4A2E22` |

Porcelain is the light theme. These hexes were chosen for a dark TV because the preview notes did not include a finished palette. Graphite + Teal and the old second accent are not in the list. A TV that still has Teal, Rose, Silver, or Marigold saved is mapped onto Forest, Plum, True Black, or Indigo.

Calendar & Clocks keeps India as the fixed local zone. Each of the four city slots can pick any IANA timezone and a separate display name, so `America/New_York` can read as NYC.

Advanced stays a normal left-menu item. There is no PIN. Reset app order and reset theme choices ask first. Clear cache asks first, then tells you what changed. The Resources line shows launcher RAM, TV RAM, and free storage. It does not start a background sampler.

## Story

The TCL was spending the first screen on ads and a clock you could not open. Raider is the home screen Amit is trying on that set: the time, the panchang, weather, and the apps he actually uses, with the colours and the world clocks chosen in settings rather than baked in.

Maya installs each 2.11 build on the TCL with a local debug signature. 3.0 waits until this one has been lived with.

## Build

The Android project is in [`launcher/`](launcher/). It is Java, `minSdk` 26, and release is minified. No extra native libraries were added. `launcher/local.properties` points at the SDK and is not committed.

```sh
cd launcher
./gradlew testDebugUnitTest assembleRelease
```

The release APK is signed with the debug key in this environment so it can be installed over a previous debug build. On the TCL, sign with the key that set is already using.

## Also in this repo

The debloat lists, the install prompt, and the per-model notes came along with the fork. They live in [`examples/`](examples/), [`PROMPT.md`](PROMPT.md), and [`launcher/README.md`](launcher/README.md). Those pages describe that older workflow. They are not the Raider home-screen story above.
