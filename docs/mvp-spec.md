# MVP Spec - Project Lyra

Date: February 9, 2026

## Scope
Android-only personal app for tracking movies and TV shows, with local-first data and no backend hosting.

## Functional Requirements
- Fetch metadata and trending content from external API.
- Search for movies and TV shows.
- Set and manage statuses:
  - Want to Watch
  - Watching
  - On Hold
  - Dropped
  - Completed
- Show reminders for new episodes for items in:
  - Watching
  - On Hold
- If a `Completed` show gets a new season, move status to `On Hold`.
- Export and import app data as JSON.
- API key can be included in backup via optional toggle.

## Screens
- Home
  - Trending movies and TV sections
  - Poster-forward feed
- Search
  - Query input
  - Result list with posters and quick actions
- My Lists
  - Filter/tab by each status bucket
  - Sort by updated time by default
- Details
  - Full metadata, poster/backdrop
  - Status controls
  - Episode/season info for shows
- Settings
  - API key input/update
  - Reminder settings
  - Backup export/import
  - Include API key in backup toggle

## Technical Baseline
- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM + Repository
- Local DB: Room
- Preferences: DataStore
- Background work: WorkManager
- Networking: Retrofit + Kotlin serialization/Moshi
- API key handling: Android Keystore-backed encryption flow

## Data Model (Draft)
- `MediaItem`
  - id (local)
  - tmdbId
  - mediaType (movie/tv)
  - title
  - posterPath
  - backdropPath
  - overview
  - releaseOrAirDate
  - metadataUpdatedAt
- `UserEntry`
  - mediaItemId
  - status
  - addedAt
  - updatedAt
- `EpisodeReminderState`
  - mediaItemId
  - lastCheckedAt
  - lastNotifiedEpisodeAirDate
- `AppSettings`
  - reminderEnabled
  - reminderTime
  - includeApiKeyInBackup

## Non-Goals (MVP)
- Ratings
- Rewatch counts
- Cloud sync/account system
- iOS support

## Milestone Plan
1. Project scaffold + dependencies + baseline navigation. (Completed on Feb 9, 2026)
2. Settings + API key secure storage.
3. Home trending feed integration.
4. Local list/status persistence.
5. Detail screen status management.
6. Search experience integration.
7. Episode reminder worker + notification channel.
8. JSON backup/restore.
