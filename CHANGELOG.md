# Changelog

All notable changes to this project are documented in this file.

The format is based on Keep a Changelog and this project follows Semantic Versioning.

## [Unreleased]
### Added
- Release signing setup via `keystore.properties` (with `keystore.properties.example` template).
- `validateReleaseSigning` Gradle task to ensure release credentials are configured.

### Changed
- Release build now uses configured signing credentials when available.

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
