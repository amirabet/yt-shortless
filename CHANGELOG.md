# Changelog

All notable changes to this project are documented in this file.

The format is based on Keep a Changelog and this project follows Semantic Versioning.

## [Unreleased]

## [1.5.0] - 2026-02-28
### Fixed
- Infinite-scroll stabilization in `MainActivity.kt` (`injectScrollFix`) to prevent YouTube feed scroll from getting stuck at the bottom when new content is appended.

## [1.4.0] - 2026-02-28
### Added
- Additional Shorts shelf cleanup by hiding section headers/shelves that contain "shorts" text in dynamic page blocks.

## [1.3.1] - 2026-02-28
### Fixed
- Restored sharing to external services by allowing non-YouTube `intent://` links to open their target apps.
- Kept YouTube "open in app" blocked by filtering only YouTube-specific app-launch intents (`mweb_c3_open_app` / YouTube package intents).
- Unified URL override handling in WebView to avoid duplicate override logic and improve consistency across Android API levels.
- Preserved external sharing behavior while still blocking YouTube "open in app" intent redirects.

## [1.3.0] - 2026-02-24
### Fixed
- Hide YouTube mobile web `ytm-open-app-promo-renderer` ("Open in app") button to prevent `intent://` navigation errors (`net::ERR_UNKNOWN_URL_SCHEME`) in WebView.

## [1.2.1] - 2026-02-27

### Fixed
- Share buttons using intent

## [1.2.0] - 2026-02-23
### Added
- Keep-screen-on behavior while fullscreen video is active to prevent device auto-lock during playback.

## [1.1.0] - 2026-02-23
### Added
- Release signing workflow with `keystore.properties` + `keystore.properties.example`.
- Gradle tasks for release/versioning support: `validateReleaseSigning` and `printVersion`.
- Fullscreen YouTube video support in WebView via `WebChromeClient` custom view handling.
- Immersive fullscreen behavior (hide/show system bars) and landscape lock while fullscreen video is active.
- Play Store icon export at `assets/branding/play-store-icon-512.png`.

### Changed
- Updated app version to `1.1.0` (`versionCode` `10100`).
- Regenerated launcher icon densities from `mipmap-xxxhdpi` source assets.
- Improved WebView rendering consistency with browser-like viewport/user-agent settings.

### Fixed
- Prevented unwanted WebView reload/content reset across activity recreation scenarios by saving/restoring WebView state.
- Enabled YouTube fullscreen button behavior in-app.

## [1.0.1] - 2026-02-22
### Added
- Semantic versioning configuration via `gradle.properties` (`VERSION_MAJOR`, `VERSION_MINOR`, `VERSION_PATCH`).
- Computed `versionCode` strategy in Gradle: `major * 10000 + minor * 100 + patch`.
- `CHANGELOG.md` to track release history.

### Changed
- Improved Shorts CSS selector robustness in `MainActivity.kt`.
- Updated round launcher icon handling to add more safe padding and black canvas.

## [1.0.0] - 2026-02-22
### Added
- Initial Android WebView app loading `https://m.youtube.com`.
- Shorts-hiding CSS injection with DOM mutation observer.
- Launcher icon resources and adaptive icon setup.
