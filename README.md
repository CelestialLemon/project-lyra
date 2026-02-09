# Project Lyra

Android-first, local-first movie and TV tracking app.

## Current State
This repository contains the MVP scaffold with:
- Kotlin + Jetpack Compose app shell
- Bottom navigation: Home, Search, My List, Settings
- Cinematic poster-first UI baseline
- Room database entities/DAOs for tracked items
- DataStore-backed settings (API key + reminder + backup toggle)
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
1. Replace seed trending with live TMDB integration.
2. Add secure API key storage with Android Keystore-backed encryption.
3. Add list status updates from Home/Details actions.
4. Implement daily WorkManager episode checks + notifications.
5. Implement JSON backup/restore flow.
