# YT Shortless

This simple Android App tries to offer the closest Youtube experience (based on mobile web) but hiding all Shorts stuff releated in the easyest possible way.
This project is for personal use and with no commercial goals (MIT), and has been created using VibeCoding methodology using Copilot in VSCode.

## Requirements
- Android Studio (Giraffe+ recommended) — only needed for local development
- Android SDK with API 34
- Java 17 (Android Studio includes a JDK)
- Gradle wrapper included (no need to install Gradle globally)
- For CI/CD: a GitHub repository with the 4 signing secrets configured (see [Release Signing](#release-signing-shareable-apk))

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

From **v1.5.1 onwards**, release APKs are built and signed automatically by GitHub Actions when a pull request is merged into `main`. No manual signing steps are needed.

### GitHub Actions signing setup (one-time)

1. Generate a JKS keystore (use the **same password** for store and key — required for JKS format):

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
& "$env:JAVA_HOME\bin\keytool" -genkeypair -v `
  -keystore release-keystore.jks `
  -storetype JKS `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -alias release `
  -storepass YOUR_PASSWORD `
  -keypass YOUR_PASSWORD `
  -dname "CN=YourName, O=YourOrg, C=US"
```

2. Encode the keystore to base64 (copies to clipboard):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\release-keystore.jks")) | Set-Clipboard
```

3. Add these 4 secrets to your GitHub repo (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Paste from clipboard |
| `RELEASE_KEYSTORE_PASSWORD` | Your keystore/key password |
| `RELEASE_KEY_ALIAS` | `release` |
| `RELEASE_KEY_PASSWORD` | Same as `RELEASE_KEYSTORE_PASSWORD` |

### Local release build (optional)

For local testing only. Requires `keystore.properties` to be present:

1. Copy `keystore.properties.example` → `keystore.properties` and fill in the values.
2. Build:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleRelease
```

Release output: `app\build\outputs\apk\release\app-release.apk`

## APK Archive Folder
- From **v1.5.1 onwards**, release APKs are published automatically as **GitHub Releases** and attached to the corresponding tag (`v<version>`).
- The `apk/` folder is kept as a **legacy archive** for versions prior to `1.5.1`. No new APKs will be added there manually.
- You can browse legacy packages here: [apk](apk/).

Important:
- Keep `release-keystore.jks` backed up securely (outside the repo).
- You must use the same keystore for all future releases — losing it prevents publishing updates under the same package name.

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

### CI/CD Workflow
- Merging a PR into `main` → builds a **signed release APK**, creates a **GitHub Release** with changelog notes, and attaches the APK.
- Merging a PR into `devel` → builds a **debug APK** and uploads it as a workflow artifact.

## Notes
- If you need to update the Gradle wrapper, download the official Gradle ZIP and copy `gradle-wrapper.jar` from `lib`.
- Shorts selectors can be expanded in `MainActivity.kt` if YouTube updates its DOM.

## License
- This project is licensed under the MIT License.
- See `LICENSE` for full text.
