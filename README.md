# YT Shortless

This simple Android App tries to offer the closest Youtube experience (based on mobile web) but hiding all Shorts stuff releated in the easyest possible way.
This project is for personal use and with no commercial goals (MIT), and has been created using VibeCoding methodology using Copilot in VSCode.

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
- Play Store icon export (512x512) is available at `assets/branding/play-store-icon-512.png`.

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

## Release Signing (Shareable APK)
1. Set Java for the current PowerShell session:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

2. Generate a release keystore (one-time):

```powershell
keytool -genkeypair -v -keystore release-keystore.jks -alias release -keyalg RSA -keysize 2048 -validity 10000
```

3. Create signing config file from template:
	- Copy `keystore.properties.example` to `keystore.properties`.
	- Fill `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
	- If the keystore is in project root, use: `storeFile=../release-keystore.jks`.

4. (Optional) Validate signing config:

```powershell
.\gradlew.bat :app:validateReleaseSigning
```

5. Build signed release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Release output:
- `app\build\outputs\apk\release\app-release.apk`

## APK Archive Folder
- The project includes an `apk` folder used to keep versioned copies of generated APKs for easy sharing/testing.
- You can browse archived packages here: [apk](apk/).
- Naming convention example: `yt-shortless-1.2.0.apk`.

Important:
- Keep `release-keystore.jks` and `keystore.properties` backed up securely.
- You must keep the same keystore to publish future updates with the same package name.

### Troubleshooting release builds
- Error: `Missing keystore.properties`
	- Fix: copy `keystore.properties.example` to `keystore.properties` and fill all values.
- Error: `Keystore file ... not found`
	- Fix: if keystore is in project root, set `storeFile=../release-keystore.jks`.
- Error: `Get Key failed: Given final block not properly padded`
	- Fix: for PKCS12 keystores, use the same value for `storePassword` and `keyPassword`.
- Error: `JAVA_HOME is not set`
	- Fix: set PowerShell session vars before build:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

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
- Release builds validate signing config with: `.\gradlew.bat :app:validateReleaseSigning`.

## Notes
- If you need to update the Gradle wrapper, download the official Gradle ZIP and copy `gradle-wrapper.jar` from `lib`.
- Shorts selectors can be expanded in `MainActivity.kt` if YouTube updates its DOM.

## License
- This project is licensed under the MIT License.
- See `LICENSE` for full text.
