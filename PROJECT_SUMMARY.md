# Project Lyra - Discussion Summary

Date: February 9, 2026

## Product Goal
Build a personal Android app to track movies and TV shows being watched, with a good-looking UI and snappy local-first experience.

## Core Constraints
- Android support is required; cross-platform is not required.
- Avoid backend hosting and database costs.
- Keep primary app data on-device.
- Personal-use first, with potential open-source release later.

## Confirmed Feature Requirements
- Fetch metadata from an external API.
- Home page with trending movies and shows.
- Separate list/status page with:
  - Want to Watch
  - Watching
  - On Hold
  - Dropped
  - Completed
- New-episode reminders for TV series.
- JSON backup/restore support for data portability.

## Metadata/API Decisions
- External metadata API usage is approved.
- API key will be user-provided in-app (not hardcoded/shared).
- Key exposure cannot be fully prevented without a backend; user-key approach is accepted.
- API key inclusion in backup will be an optional toggle.

## Episode Reminder Behavior
- Notifications should be sent for both `Watching` and `On Hold`.
- If a show is `Completed` and a new season appears, it should move to `On Hold` (not `Watching`).

## Proposed Technical Stack
- Kotlin
- Jetpack Compose (UI)
- MVVM architecture
- Repository pattern
- Room (local database)
- DataStore (app preferences/settings)
- WorkManager (background reminder jobs)
- Android Keystore-based secure handling for API key material

## Why This Stack
- Best fit for Android-only performance and responsiveness.
- Strong support for offline/local-first behavior.
- Reliable background scheduling and notifications.
- Compose supports modern, high-quality UI with state-driven rendering.

## Local Development Setup Status
Environment is configured and ready to scaffold:
- Android SDK command-line tools installed.
- `platform-tools`, `platforms;android-36`, `build-tools;36.0.0` installed.
- Device connected: Pixel 7a (`adb` authorized).
- `local.properties` configured with SDK path.

Note: Project runtime JDK should be pinned to 21 (or 17) for Android build stability, even if Java 25 is installed globally.

## UI Direction (Pending Final Choice)
Candidate visual directions:
- Cinematic Poster-First
- Editorial Clean
- Neo Dashboard
- Hybrid (Cinematic Home + Editorial Lists)

## UI Direction (Finalized)
- Selected: `Cinematic Poster-First`
- Priorities:
  - Large visual poster cards
  - Rich gradient surfaces and atmospheric backgrounds
  - Motion accents for feed/list transitions
  - Strong title hierarchy with minimal clutter

## Open Questions (Still Pending)
- Final API choice: TMDB (recommended, not yet hard-locked in doc)
- Typography pair and color palette fine-tuning
- Default reminder schedule (time and frequency defaults)

## Next Steps
1. Freeze API provider and finalize data model entities.
2. Scaffold Android project structure.
3. Implement first vertical slice:
   - Settings (API key storage + validation)
   - Home (trending feed)
   - My Lists (status buckets + local persistence)
4. Add episode reminder worker for `Watching` + `On Hold`.
