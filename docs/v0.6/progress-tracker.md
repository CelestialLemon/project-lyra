# Project Lyra v0.6 Progress Tracker

## Release info

- Version: `v0.6`
- Start date: `2026-02-11`
- Completion date: `TBD`
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
| Details header + metadata polish | Not Started | Banner framing fix + metadata styling updates |
| Seasons selector + episode card UI | Not Started | Replace season list with selector and episode cards |
| Episode watched/unwatched interactions | Not Started | Overflow menu actions + watched indicator + season complete |
| TV season API + repository contracts | Not Started | TMDB `/tv/{id}/season/{season_number}` integration |
| Watched episodes Room schema + migration v5 | Not Started | New table/DAO and migration `4 -> 5` |
| Backup schema v2 support | Not Started | Export/import + validator + restore plan updates |
| Automated tests + regression validation | Not Started | Unit/integration coverage for v0.6 contracts |
| QA + release sign-off | Not Started | Manual walkthrough and release confirmation |

## Phased implementation plan

| Phase | Status | Scope | Deliverables | Exit criteria |
| --- | --- | --- | --- | --- |
| Phase 1: UI layout polish | Not Started | Header framing and metadata styling | Details header/container updates, genre separator change | Banner readability retained and crop issue resolved |
| Phase 2: Seasons to episodes UX | Not Started | Season dropdown and episode list | Selector state flow, episode cards, section-level loading/error states | TV details renders episode-centric flow with stable season switching |
| Phase 3: Episode progress logic | Not Started | Watched/unwatched and season-complete behavior | Overflow actions, cascade rules, aired-only completion logic | Cascade behavior verified for watched/unwatched and season complete |
| Phase 4: Data contracts and persistence | Not Started | TMDB season endpoint + Room + backup | API DTOs, repository methods, watched table/DAO, migration v5, backup v2 | Episode progress persists across app restarts and backup restore |
| Phase 5: Validation and sign-off | Not Started | Automated and manual validation | Test additions, run logs, QA checklist completion | All gates pass and release is signed off |

## Phase checklist

- [ ] Phase 1 complete
- [ ] Phase 2 complete
- [ ] Phase 3 complete
- [ ] Phase 4 complete
- [ ] Phase 5 complete

## Task checklist

- [ ] Update details header container to aspect-ratio-driven layout and preserve gradient readability.
- [ ] Keep top status dropdown placement and behavior unchanged.
- [ ] Update metadata styling and switch genre separator to `|`.
- [ ] Replace seasons list cards with season dropdown selector.
- [ ] Add "Mark season as complete" action below season selector.
- [ ] Implement episode cards with image/title/episode number/date/runtime.
- [ ] Add watched indicator visual state on episode cards.
- [ ] Add episode overflow menu actions for watch-up-to and unwatched-from.
- [ ] Implement watched-up-to cascade mutation (`1..N`) for active season.
- [ ] Implement unwatched-from cascade mutation (`N..end`) for active season.
- [ ] Implement aired-only season-complete eligibility logic (missing date treated as eligible).
- [ ] Add TMDB TV season endpoint and DTOs.
- [ ] Add season-episode domain models and mapper coverage.
- [ ] Add repository contracts for season fetch and episode progress mutations/observation.
- [ ] Add Room watched-episodes entity and DAO.
- [ ] Add Room migration `4 -> 5` and migration tests.
- [ ] Update backup schema version to `2` and include watched episodes in export/import.
- [ ] Update backup validator/restore normalization for watched-episode records.
- [ ] Add `DetailsViewModel` unit tests for season selection and episode progress flows.
- [ ] Add repository/store tests for watched-episode persistence and cascade behavior.
- [ ] Add season API fetch success/error tests.
- [ ] Run and pass full unit test suite (`./gradlew testDebugUnitTest`).
- [ ] Run and pass lint checks (`./gradlew lintDebug`) with no blocking regressions.
- [ ] Execute manual QA scenarios from `docs/v0.6/spec.md`.

## Release validation gates

- [ ] Gate 1: v0.6 feature-specific tests are present and passing.
- [ ] Gate 2: existing test suite continues to pass.
- [ ] Gate 3: lint passes with no blocking issues.
- [ ] Gate 4: manual QA scenarios complete with no open blockers.
- [ ] Gate 5: release sign-off captured.

## Validation notes

- `TBD`

## Risks / watch items

- On-demand season fetch may create noticeable loading states on poor networks.
- TMDB payload variability (missing still/runtime/air date) can impact episode card consistency without robust fallback handling.
- Episode progress persistence + backup schema changes increase migration/restore surface area.
- Details screen complexity increases; state flow must avoid stale-season UI bugs.

## Change log

- `2026-02-11`: Tracker created for v0.6 planning handoff.
