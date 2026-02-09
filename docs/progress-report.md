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
- Network: Retrofit + Moshi (TMDB Home trending integration live; Search integration pending)
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
Status: Partially completed
- Implemented:
  - Room entities and DAOs for media + user entries.
  - List screen status chips and filtered view by status.
  - Seed data inserted and displayed in list buckets.
- Pending:
  - Add/update status actions from Home/Details.
  - Prevent duplicate entries and enforce upsert semantics from UI flows.
  - Edit/remove flows.

## Milestone 5: Detail screen status management
Status: Not started
- Pending full details screen with:
  - poster/backdrop metadata
  - status controls
  - season/episode display

## Milestone 6: Search experience integration
Status: Not started
- Pending:
  - Implement TMDB multi-search endpoint integration for movies/TV.
  - Build query input UX with debounce and loading/empty/error states.
  - Map search results to shared media domain model.
  - Add quick status actions from search results.
  - Wire result tap into details flow.

## Milestone 7: Episode reminder worker + notification channel
Status: Scaffold only
- Implemented:
  - Worker class scaffold.
  - Daily periodic work scheduling hook.
- Pending:
  - Fetch airing info for tracked TV shows.
  - Detect “new since last check”.
  - Notification channel + notification UI payload.
  - Respect app reminder toggles/time.

## Milestone 8: JSON backup/restore
Status: Not started
- Pending:
  - JSON export schema design
  - import validation
  - file picker flow
  - optional inclusion of API key

## 6. Current User-Visible State
- App installs and opens successfully.
- Home shows cinematic cards powered by live TMDB trending data when API key is configured.
- Home gracefully handles loading/errors and can retry fetches.
- Bottom tabs are functional for navigation.
- My List tab shows status-filtered seeded entries.
- Settings stores encrypted API key plus reminder/backup toggles.

## 7. Known Gaps and Risks
- Search tab is placeholder content.
- Reminder worker does not yet execute business logic.
- No backup/restore implementation yet.
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
  - `app/src/main/java/com/projectlyra/app/data/local/LyraDatabase.kt`
  - `app/src/main/java/com/projectlyra/app/data/local/TrendingCacheEntity.kt`
  - `app/src/main/java/com/projectlyra/app/data/local/TrendingCacheDao.kt`
  - `app/src/main/java/com/projectlyra/app/data/repository/LibraryRepository.kt`
  - `app/src/main/java/com/projectlyra/app/data/remote/TmdbApiService.kt`
  - `app/src/main/java/com/projectlyra/app/data/remote/TmdbClientFactory.kt`
  - `app/src/main/java/com/projectlyra/app/data/settings/SettingsStore.kt`
- Features:
  - `app/src/main/java/com/projectlyra/app/feature/home/HomeScreen.kt`
  - `app/src/main/java/com/projectlyra/app/feature/mylist/MyListScreen.kt`
  - `app/src/main/java/com/projectlyra/app/feature/search/SearchScreen.kt`
  - `app/src/main/java/com/projectlyra/app/feature/settings/SettingsScreen.kt`
- Reminders:
  - `app/src/main/java/com/projectlyra/app/workers/EpisodeReminderWorker.kt`
  - `app/src/main/java/com/projectlyra/app/workers/ReminderScheduler.kt`

## 9. Recommended Next Implementation Order
1. Complete Milestone 4 status actions from Home/Details (upsert semantics, duplicate prevention, edit/remove flows).
2. Build Milestone 5 full Details screen with metadata + status controls + season/episode info.
3. Implement Milestone 6 search experience integration (API, UX states, quick actions, details navigation).
4. Implement Milestone 7 reminder business logic + notifications.
5. Implement Milestone 8 JSON backup/restore.
6. Add tests for repository, settings, Home fetch/cache logic, and worker behavior.

## 10. New Chat Handoff Prompt
Use this when starting a fresh chat:

“Read `docs/progress-report.md`, `docs/mvp-spec.md`, and `docs/ui-cinematic-direction.md`. Continue Project Lyra from current state, starting with Milestone 4 (local list/status persistence actions) while keeping cinematic UI direction and local-first architecture intact.”
