# YT Shortless

Android APK project that loads the mobile YouTube website (`m.youtube.com`), keeps login state, and hides Shorts-related UI via injected CSS.

## Features
- Opens `https://m.youtube.com` in a WebView
- Persistent login (cookies + DOM storage enabled)
- CSS + MutationObserver to hide Shorts shelves/entries

## Requirements
- Android Studio (Giraffe+ recommended)
- Android SDK with API 34
- Java 17
- Gradle 8.x (or use Android Studio’s bundled Gradle)

## Run in Android Studio
1. Open the project folder `\yt-shortless`.
2. Let Gradle sync.
3. Run the `app` configuration on a device or emulator.

## Build from PowerShell
If you have Gradle installed and `ANDROID_HOME` set:

```powershell
cd c:\Projects\yt-shortless
gradle :app:assembleDebug
```

APK output:
- `app\build\outputs\apk\debug\app-debug.apk`

## Notes
- If you want a Gradle wrapper, run `gradle wrapper` to generate it.
- Shorts selectors can be expanded in `MainActivity.kt` if YouTube updates its DOM.
