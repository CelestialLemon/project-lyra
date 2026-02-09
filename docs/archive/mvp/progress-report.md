# Project Lyra - Progress Report and Handoff

Last updated: February 9, 2026
Owner mode: User as Product Lead, Codex as implementation partner
Current status: MVP scaffold running on Pixel 7a

## 1. Project Goal
Build an Android-first personal app to track movies and TV shows with:
- Cinematic poster-first UI
- Local-first storage (no hosted backend/database)
- Metadata from external API
- Episode reminders
- JSON backup/restore

## 2. Locked Product Decisions
- Platform: Android only (personal use first).
- UI direction: Cinematic Poster-First.
- Status buckets:
  - Want to Watch
  - Watching
  - On Hold
  - Dropped
  - Completed
- Reminder scope: notify for `Watching` and `On Hold`.
- Completed show behavior: if new season appears, move to `On Hold`.
- Backup policy: include API key in backup via optional toggle.
- Ratings and rewatch count: out of MVP.
- Cloud sync: out of MVP.

## 3. Current Technical Direction
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- App architecture: MVVM + Repository
- Local data: Room
- Settings: DataStore
- Background jobs: WorkManager
- Network: Retrofit + Moshi (TMDB Home trending + multi-search integration live)
- Image loading: Coil

## 4. Development Environment (Confirmed Working)
- Device: Pixel 7a via USB debugging.
- Android SDK installed with:
  - `platform-tools`
  - `platforms;android-36`
  - `build-tools;36.0.0`
- Build runtime requirement: use JDK 21 (not JDK 25).
- Known-good build command:
  - `./gradlew assembleDebug`
- Known-good install command:
  - `./gradlew installDebug`

## 5. Implementation Progress

## Milestone 1: Project scaffold + dependencies + baseline navigation
Status: Completed
- Root Gradle files created.
- App module created with Compose/Room/DataStore/WorkManager dependencies.
- Main app shell with bottom navigation implemented.
- App launches on phone and displays Home/Search/My List/Settings tabs.

## Milestone 2: Settings + API key secure storage
Status: Completed
- Implemented:
  - Settings screen UI.
  - API key encrypted with Android Keystore (`AES/GCM`) before persistence.
  - Legacy plaintext API key migration to encrypted storage.
  - API key field validation for TMDB v3 key format (32-char hex) with inline error states.
  - Reminder toggle.
  - Include-API-key-in-backup toggle.

## Milestone 3: Home trending feed integration
Status: Completed
- Implemented:
  - Cinematic home layout.
  - Hero card + “Hot Right Now” rail.
  - Live TMDB `trending/all/day` integration.
  - Response mapping from TMDB movie/TV payloads to app domain model.
  - Home loading/error/retry states with user-facing network/auth error messages.
  - Room-backed trending cache table and fallback policy (6-hour cache TTL + stale fallback on fetch failure).

## Milestone 4: Local list/status persistence
Status: Completed
- Implemented:
  - Room entities and DAOs for media + user entries.
  - List screen status chips and filtered view by status.
  - Seed data inserted and displayed in list buckets.
  - Add/update status actions from Home trending cards.
  - Duplicate-safe status upsert semantics from UI flows (single entry per media item).
  - Edit/remove flows from My List items.

## Milestone 5: Detail screen status management
Status: Completed
- Implemented:
  - Dedicated Details navigation route from Home and My List cards.
  - Immersive details screen with backdrop/poster metadata and overview.
  - TMDB movie/TV details API integration with local fallback behavior.
  - Status controls on Details screen wired to tracked-status persistence.
  - TV season/episode summary and season-level list display.

## Milestone 6: Search experience integration
Status: Completed
- Implemented:
  - TMDB `search/multi` endpoint integration for movies/TV results.
  - Debounced query UX with loading/empty/error handling and retry.
  - Search result mapping to shared `TrendingItem` media domain model.
  - Quick status actions from search result cards.
  - Search-result tap navigation into the Details screen flow.

## Milestone 7: Episode reminder worker + notification channel
Status: Completed
- Implemented:
  - Daily periodic work scheduling synced from app settings (enable/disable + reminder time).
  - Worker business logic for tracked TV titles in `Watching`, `On Hold`, and `Completed`.
  - TMDB TV details checks to fetch latest season/episode counts.
  - “New since last check” detection using persisted per-show reminder state in Room.
  - Notification channel creation and episode-update notification payload dispatch.
  - Android 13+ notification permission request flow when reminders are enabled in Settings.
  - `Completed` auto-transition to `On Hold` when a new season is detected.

## Milestone 8: JSON backup/restore
Status: Completed
- Implemented:
  - Versioned JSON backup schema (`schemaVersion = 1`) for media, user entries, reminder state, and app settings.
  - Backup export flow from Settings using Android document picker (`CreateDocument`).
  - Backup import flow from Settings using Android document picker (`OpenDocument`).
  - Strict import validation for schema version, enums, timestamps, duplicates, and cross-record references.
  - Transactional restore into Room tables with trending cache reset.
  - Optional API key inclusion respected on export; API key restore applied only when present in backup.

## 6. Current User-Visible State
- App installs and opens successfully.
- Home shows cinematic cards powered by live TMDB trending data when API key is configured.
- Home gracefully handles loading/errors and can retry fetches.
- Home cards support quick add/update status actions.
- Bottom tabs are functional for navigation.
- My List tab supports status-filtered entries with inline status edit/remove actions.
- Home and My List cards open a full Details screen with metadata and status controls.
- Search tab supports live TMDB movie/TV discovery with quick status tracking and details navigation.
- Settings stores encrypted API key plus reminder/backup toggles.
- Episode reminder checks run daily at configured time and notify for newly available episodes.
- Settings supports JSON backup export and import with success/error status messaging.

## 7. Known Gaps and Risks
- No automated tests yet.

## 8. Source Map (Key Files)
- Product context:
  - `PROJECT_SUMMARY.md`
  - `docs/ui-cinematic-direction.md`
  - `docs/mvp-spec.md`
  - `docs/progress-report.md`
- App shell:
  - `app/src/main/java/com/projectlyra/app/MainActivity.kt`
  - `app/src/main/java/com/projectlyra/app/ui/LyraApp.kt`
- Theme/UI components:
  - `app/src/main/java/com/projectlyra/app/ui/theme/Theme.kt`
  - `app/src/main/java/com/projectlyra/app/ui/components/PosterCard.kt`
  - `app/src/main/java/com/projectlyra/app/ui/components/StatusChip.kt`
- Data and settings:
  - `app/src/main/java/com/projectlyra/app/data/backup/BackupService.kt`
  - `app/src/main/java/com/projectlyra/app/data/backup/LyraBackupDocument.kt`
  - `app/src/main/java/com/projectlyra/app/data/local/LyraDatabase.kt`
  - `app/src/main/java/com/projectlyra/app/data/local/TrendingCacheEntity.kt`
  - `app/src/main/java/com/projectlyra/app/data/local/TrendingCacheDao.kt`
  - `app/src/main/java/com/projectlyra/app/data/repository/LibraryRepository.kt`
  - `app/src/main/java/com/projectlyra/app/data/remote/TmdbApiService.kt`
  - `app/src/main/java/com/projectlyra/app/data/remote/TmdbClientFactory.kt`
  - `app/src/main/java/com/projectlyra/app/data/settings/SettingsStore.kt`
- Features:
  - `app/src/main/java/com/projectlyra/app/feature/details/DetailsScreen.kt`
  - `app/src/main/java/com/projectlyra/app/feature/details/DetailsViewModel.kt`
  - `app/src/main/java/com/projectlyra/app/feature/home/HomeScreen.kt`
  - `app/src/main/java/com/projectlyra/app/feature/mylist/MyListScreen.kt`
  - `app/src/main/java/com/projectlyra/app/feature/search/SearchScreen.kt`
  - `app/src/main/java/com/projectlyra/app/feature/search/SearchViewModel.kt`
  - `app/src/main/java/com/projectlyra/app/feature/settings/SettingsScreen.kt`
  - `app/src/main/java/com/projectlyra/app/ui/navigation/DetailsDestination.kt`
- Reminders:
  - `app/src/main/java/com/projectlyra/app/workers/EpisodeReminderWorker.kt`
  - `app/src/main/java/com/projectlyra/app/workers/ReminderScheduler.kt`
  - `app/src/main/java/com/projectlyra/app/data/local/EpisodeReminderStateEntity.kt`
  - `app/src/main/java/com/projectlyra/app/data/local/EpisodeReminderStateDao.kt`

## 9. Recommended Next Implementation Order
1. Add tests for repository, settings, backup/restore, Home/details/search fetch logic, and worker behavior.

## 10. New Chat Handoff Prompt
Use this when starting a fresh chat:

“Read `docs/progress-report.md`, `docs/mvp-spec.md`, and `docs/ui-cinematic-direction.md`. Continue Project Lyra from current state, starting with automated tests for repository/settings/backup/worker flows while keeping cinematic UI direction and local-first architecture intact.”
