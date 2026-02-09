# Project Lyra

Android-first, local-first movie and TV tracking app.

## Current State
This repository contains the MVP scaffold with:
- Kotlin + Jetpack Compose app shell
- Bottom navigation: Home, Search, My List, Settings
- Cinematic poster-first Home with live TMDB trending feed
- Search tab with live TMDB multi-search, debounced query UX, and quick status actions
- Home loading/error/retry handling plus Room-backed trending cache fallback
- Full Details screen with TMDB metadata, status controls, and TV season/episode info
- Room database entities/DAOs for tracked items
- Settings with Keystore-encrypted TMDB API key + DataStore toggles
- Daily episode reminder worker with notification channel + payloads
- Reminder-state persistence for "new since last check" detection
- Auto move from `Completed` to `On Hold` when a new TV season is detected

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
1. Implement JSON backup/restore flow.
2. Add automated tests for repository, settings, and feature flows.
