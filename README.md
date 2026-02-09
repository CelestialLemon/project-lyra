# Project Lyra

Android-first, local-first movie and TV tracking app.

## Current State
This repository contains the MVP scaffold with:
- Kotlin + Jetpack Compose app shell
- Bottom navigation: Home, Search, My List, Settings
- Cinematic poster-first Home with live TMDB trending feed
- Home loading/error/retry handling plus Room-backed trending cache fallback
- Room database entities/DAOs for tracked items
- Settings with Keystore-encrypted TMDB API key + DataStore toggles
- Episode reminder worker scaffold

## Prerequisites
- JDK 21 (recommended for Android build stability)
- Android SDK with `platform-tools`, `platforms;android-36`, `build-tools;36.0.0`
- Connected Android device (Pixel 7a works)

## One-Time: Generate Gradle Wrapper
This repo was scaffolded in a terminal environment without global Gradle. Run once:

```bash
brew install gradle
gradle wrapper --gradle-version 8.10.2
```

## Build + Install
```bash
./gradlew assembleDebug
./gradlew installDebug
adb devices
adb logcat
```

## Next Milestones
1. Build full Details screen with status controls and season/episode metadata.
2. Implement Search experience integration (API + UX states + quick actions).
3. Implement daily WorkManager episode checks + notifications.
4. Implement JSON backup/restore flow.
