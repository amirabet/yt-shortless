# YT Shortless

This simple Android tries to offer the closest Youtube experience (basded on mobile web) but hiding all Shorts stuff releated in the easyest possible way.

## Requirements
- Android Studio (Giraffe+ recommended)
- Android SDK with API 34
- Java 17 (Android Studio includes a JDK)
- Gradle wrapper included (no need to install Gradle globally)

## Features
- Opens `https://m.youtube.com` in a WebView
- Persistent login (cookies + DOM storage enabled)
- CSS + MutationObserver to hide Shorts shelves/entries

## Icon Setup
- Adaptive and legacy launcher icons are present in all required mipmap folders.
- Place your 432x432 foreground/background PNGs in `mipmap-xxxhdpi` as `ic_launcher_foreground.png` and `ic_launcher_background.png`.
- Icons are auto-resized for all densities.
- Manifest and adaptive icon XML are wired for launcher and round icons.

## Run in Android Studio
1. Open the project folder `c:\Projects\yt-shortless`.
2. Let Gradle sync.
3. Run the `app` configuration on a device or emulator.

## Build from PowerShell
- Set JAVA_HOME to Android Studio's JDK:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd c:\Projects\yt-shortless
.\gradlew :app:assembleDebug
```

APK output:
- `app\build\outputs\apk\debug\app-debug.apk`

## Versioning and Releases
- Versioning follows Semantic Versioning (`MAJOR.MINOR.PATCH`).
- Update values in `gradle.properties`:
	- `VERSION_MAJOR`
	- `VERSION_MINOR`
	- `VERSION_PATCH`
- `versionName` is generated as `MAJOR.MINOR.PATCH`.
- `versionCode` is generated as `MAJOR * 10000 + MINOR * 100 + PATCH`.
- Add release notes in `CHANGELOG.md` before each release.
- Print current values with: `./gradlew :app:printVersion` (PowerShell: `.\gradlew.bat :app:printVersion`).

## Notes
- If you need to update the Gradle wrapper, download the official Gradle ZIP and copy `gradle-wrapper.jar` from `lib`.
- Shorts selectors can be expanded in `MainActivity.kt` if YouTube updates its DOM.
