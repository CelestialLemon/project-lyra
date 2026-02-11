# Project Lyra v0.6 Progress Tracker

## Release info

- Version: `v0.6`
- Start date: `2026-02-11`
- Completion date: `TBD (awaiting manual QA + sign-off)`
- Target: TV Details overhaul + episode-level watched progress tracking
- Source of truth: `docs/v0.6/spec.md`

## Status legend

- `Not Started`
- `In Progress`
- `Blocked`
- `Done`

## Milestone tracker

| Milestone | Status | Notes |
| --- | --- | --- |
| Details header + metadata polish | Done | Aspect-ratio header, preserved gradient hierarchy, metadata polish + `|` genre separator |
| Seasons selector + episode card UI | Done | Dropdown selector, season-complete action, horizontal episode cards with fallback states |
| Episode watched/unwatched interactions | Done | Overflow actions + watched indicator + cascade mutations + aired-only season-complete logic |
| TV season API + repository contracts | Done | Added TMDB `/tv/{id}/season/{season_number}` and repository contracts for season fetch/progress ops |
| Watched episodes Room schema + migration v5 | Done | New `watched_episodes` table/DAO, DB version `5`, `MIGRATION_4_5`, migration test coverage |
| Backup schema v2 support | Done | Backup schema `2` with watched episodes export/import + validation and restore-plan support |
| Automated tests + regression validation | Done | Added/updated unit and integration tests for v0.6 flows; `testDebugUnitTest` + `lintDebug` passing |
| QA + release sign-off | In Progress | Manual scenarios and final sign-off pending device walkthrough |

## Phased implementation plan

| Phase | Status | Scope | Deliverables | Exit criteria |
| --- | --- | --- | --- | --- |
| Phase 1: UI layout polish | Done | Header framing and metadata styling | Details header/container updates, genre separator change | Banner readability retained and crop issue resolved |
| Phase 2: Seasons to episodes UX | Done | Season dropdown and episode list | Selector state flow, episode cards, section-level loading/error states | TV details renders episode-centric flow with stable season switching |
| Phase 3: Episode progress logic | Done | Watched/unwatched and season-complete behavior | Overflow actions, cascade rules, aired-only completion logic | Cascade behavior verified for watched/unwatched and season complete |
| Phase 4: Data contracts and persistence | Done | TMDB season endpoint + Room + backup | API DTOs, repository methods, watched table/DAO, migration v5, backup v2 | Episode progress persists across app restarts and backup restore |
| Phase 5: Validation and sign-off | In Progress | Automated and manual validation | Test additions, run logs, QA checklist completion | All gates pass and release is signed off |

## Phase checklist

- [x] Phase 1 complete
- [x] Phase 2 complete
- [x] Phase 3 complete
- [x] Phase 4 complete
- [ ] Phase 5 complete

## Task checklist

- [x] Update details header container to aspect-ratio-driven layout and preserve gradient readability.
- [x] Keep top status dropdown placement and behavior unchanged.
- [x] Update metadata styling and switch genre separator to `|`.
- [x] Replace seasons list cards with season dropdown selector.
- [x] Add "Mark season as complete" action below season selector.
- [x] Implement episode cards with image/title/episode number/date/runtime.
- [x] Add watched indicator visual state on episode cards.
- [x] Add episode overflow menu actions for watch-up-to and unwatched-from.
- [x] Implement watched-up-to cascade mutation (`1..N`) for active season.
- [x] Implement unwatched-from cascade mutation (`N..end`) for active season.
- [x] Implement aired-only season-complete eligibility logic (missing date treated as eligible).
- [x] Ensure aired-only eligibility uses local device calendar date boundary (`LocalDate`).
- [x] Add TMDB TV season endpoint and DTOs.
- [x] Add season-episode domain models and mapper coverage.
- [x] Add repository contracts for season fetch and episode progress mutations/observation.
- [x] Add per-details-session in-memory season payload cache to avoid refetching already opened seasons.
- [x] Add Room watched-episodes entity and DAO.
- [x] Add Room migration `4 -> 5` and migration tests.
- [x] Update backup schema version to `2` and include watched episodes in export/import.
- [x] Update backup validator/restore normalization for watched-episode records.
- [x] Add `DetailsViewModel` unit tests for season selection and episode progress flows.
- [x] Add repository/store tests for watched-episode persistence and cascade behavior.
- [x] Add season API fetch success/error tests.
- [x] Run and pass full unit test suite (`./gradlew testDebugUnitTest`).
- [x] Run and pass lint checks (`./gradlew lintDebug`) with no blocking regressions.
- [ ] Execute manual QA scenarios from `docs/v0.6/spec.md`.

## Release validation gates

- [x] Gate 1: v0.6 feature-specific tests are present and passing.
- [x] Gate 2: existing test suite continues to pass.
- [x] Gate 3: lint passes with no blocking issues.
- [ ] Gate 4: manual QA scenarios complete with no open blockers.
- [ ] Gate 5: release sign-off captured.

## Validation notes

- `2026-02-11`: `./gradlew testDebugUnitTest` passed.
- `2026-02-11`: `./gradlew lintDebug` passed.
- Manual QA scenarios remain pending for device walkthrough/sign-off.

## Risks / watch items

- On-demand season fetch may create noticeable loading states on poor networks.
- TMDB payload variability (missing still/runtime/air date) can impact episode card consistency without robust fallback handling.
- Manual QA coverage is pending and required before release sign-off.

## Change log

- `2026-02-11`: Implemented v0.6 TV details episode-centric UX, season fetch contract, watched-episode persistence, backup schema v2, and automated test coverage.
- `2026-02-11`: Validation pass completed for unit tests + lint; manual QA/sign-off left open.
