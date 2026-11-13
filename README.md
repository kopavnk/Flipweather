# FlipWeather

FOSS Android widget: clock | weather (Open-Meteo, metric) | battery.

- Weather tile: day/night temps (max/min) left, symbol centre, wind km/h + pale-yellow triangle (derived severe-weather flag) right.
- Tap ‹ / › beside the location name to flip through locations; returns to the first one after ~10 s.
- Tap the weather symbol: accuweather.com search for that location (10-day page).
- Tap clock: default clock app. Tap battery: system battery usage. Colours follow the system (Material You) palette on Android 12+.
- Transparency + locations: open the "FlipWeather" app icon, or long-press the widget > reconfigure.

## Get an installable APK
Option A, no Android Studio (GitHub): create a new repo, upload this folder, open Actions > "Build APK" > Run workflow.
Download the `FlipWeather-apk` artifact, unzip, copy `app-debug.apk` to the phone and install it
(allow "install unknown apps" for your file manager or browser).

Option B, locally (JDK 17 + Android SDK 34): `./gradlew assembleDebug`
-> `app/build/outputs/apk/debug/app-debug.apk`  (or `adb install` it).

The debug APK is signed with the auto-generated debug key, so it installs directly.
minSdk 26, no Google services, no tracking.
