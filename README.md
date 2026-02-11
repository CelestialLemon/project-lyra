# Project Lyra

Project Lyra is an Android app for tracking movies and TV shows with a local-first workflow.

## Highlights
- Kotlin + Jetpack Compose UI with Home, Search, My List, Details, and Settings flows
- TMDB-powered discovery, search, details, and recommendations
- Local persistence with Room, including watched-episode tracking and reminder state
- Keystore-encrypted TMDB API key storage
- WorkManager-based daily episode reminders
- JSON backup and restore with validation and optional API key export

## Tech Stack
- Kotlin
- Jetpack Compose + Material 3
- Room
- DataStore
- WorkManager
- Retrofit + Moshi

## Requirements
- macOS/Linux/Windows with Android tooling
- JDK 17 or newer
- Android SDK for API 36
- Android Studio (recommended) or CLI Android SDK tools

## Quick Start
1. Clone the repository.
2. Build the app:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install on a connected device/emulator:
   ```bash
   ./gradlew installDebug
   ```
4. Open the app and set your TMDB v3 API key in `Settings`.

## TMDB API Key
Project Lyra requires your own TMDB API key to fetch online metadata.

1. Create an account at [TMDB](https://www.themoviedb.org/).
2. Generate a v3 API key.
3. Enter the key in the app settings.

The key is encrypted with Android Keystore before persistence.

## Build and Test Commands
- Build debug APK: `./gradlew assembleDebug`
- Run unit tests: `./gradlew testDebugUnitTest`
- Run lint: `./gradlew lintDebug`
- Run instrumentation tests: `./gradlew connectedDebugAndroidTest`

## Project Layout
- App code: `app/`
- Build configuration: `build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts`
- Documentation: `docs/`

## Documentation
- Current planning/release docs live under `docs/<release_version>/` when active.
- Historical release docs are in `docs/archive/`.

## Privacy and Data
- Core tracking data is stored locally on device.
- Backups are user-initiated.
- API key export in backups is opt-in.

## TMDB Attribution
This product uses the TMDB API but is not endorsed or certified by TMDB.

## License
Released under the MIT License. See `LICENSE`.
