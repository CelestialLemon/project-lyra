# Project Lyra v0.5 Progress Tracker

## Release info
- Version: `v0.5`
- Start date: `2026-02-10`
- Completion date: `2026-02-11` (release sign-off)
- Target: Home quick-return hero + genre-personalized movie/TV discover rails
- Source of truth: `docs/archive/v0.5/spec.md`

## Status legend
- `Not Started`
- `In Progress`
- `Blocked`
- `Done`

## Milestone tracker
| Milestone | Status | Notes |
| --- | --- | --- |
| Resume hero (Watching/On Hold priority) | Done | Home now resolves latest `Watching` item, then latest `On Hold` item when `Watching` is empty |
| Personalized movie recommendations rail | Done | Movie rail wired to top-3 completed-genre profile with tracked-item filtering and fallback |
| Personalized TV recommendations rail | Done | TV rail wired to top-3 completed-genre profile with tracked-item filtering and fallback |
| Discover/genre data plumbing | Done | Discover movie/TV + genre list API paths, genre metadata cache table, repository contracts, and migration `3 -> 4` landed |
| Automated tests + regression validation | Done | New v0.5 tests added and full unit/lint gates passed |
| QA + release sign-off | Done | Manual device walkthrough completed; release sign-off received |

## Phased implementation plan
| Phase | Status | Scope | Deliverables | Exit criteria |
| --- | --- | --- | --- | --- |
| Phase 1: Resume hero behavior | Done | Implement top Home card selection from user activity state | DAO/repository/view-model support for latest `Watching` fallback to latest `On Hold`; updated Home hero UI | Hero always resolves correct priority item or cleanly hides when no candidate exists |
| Phase 2: Genre profile foundation | Done | Enable completed-item genre profiling prerequisites | Discover + genre endpoints integrated; genre metadata persistence/model wiring added | App can derive top 3 completed genres reliably |
| Phase 3: Personalized rails | Done | Render personalized movie + TV rails under hero | Two independent recommendation flows and UI sections with loading/error/empty behavior | Both rails return relevant, de-duplicated, untracked recommendations |
| Phase 4: Automated validation + hardening | Done | Add and execute automated tests + regression checks | New v0.5 test coverage, test/lint command results, regression notes | Automated gates pass and no blocking regressions remain |
| Phase 5: QA + release sign-off | Done | Validate UX simplicity and resilience | Device walkthrough + final sign-off | All milestones marked `Done` and release checklist complete |

## Phase checklist
- [x] Phase 1 complete
- [x] Phase 2 complete
- [x] Phase 3 complete
- [x] Phase 4 complete
- [x] Phase 5 complete

## Task checklist
- [x] Add query path for most recently edited `Watching` item
- [x] Add fallback query path for most recently edited `On Hold` item
- [x] Update Home hero UI copy/content to "Continue Watching" semantics
- [x] Keep hero actions focused on open/edit details flow
- [x] Add TMDB discover movie and discover TV client methods
- [x] Add TMDB genre metadata client methods
- [x] Persist/cache genre metadata required for profile construction
- [x] Derive top 3 genres from `Completed` user items
- [x] Add personalized recommendation request model for movie rail
- [x] Add personalized recommendation request model for TV rail
- [x] Filter out already tracked items from recommendation outputs
- [x] Add loading/error/empty states for both rails
- [x] Ensure fallback content is shown when personalization profile is unavailable
- [x] Add automated tests for resume hero selection priority (`Watching` first, `On Hold` fallback)
- [x] Add automated tests for top-3 completed-genre profile derivation
- [x] Add automated tests for personalized recommendation filtering of already tracked items
- [x] Add automated tests for personalization fallback behavior (no completed profile / API failure)
- [x] Run and pass automated unit/integration suite (`./gradlew testDebugUnitTest`)
- [x] Run and pass lint baseline checks (`./gradlew lintDebug`) with no new blocking findings
- [x] Run existing feature regression smoke checks (Search, My List status updates, Details loading, reminders scheduling path)
- [x] Manual test: `Watching` hero candidate takes priority
- [x] Manual test: `On Hold` fallback hero appears when `Watching` is empty
- [x] Manual test: both recommendation rails render and open details correctly
- [x] Manual test: no completed items fallback path
- [x] Manual test: network error fallback path

## Release validation gates
- [x] Gate 1: New v0.5 automated tests are present and passing.
- [x] Gate 2: Existing automated tests continue to pass (`./gradlew testDebugUnitTest`).
- [x] Gate 3: Lint passes with no new blocking issues (`./gradlew lintDebug`).
- [x] Gate 4: Existing core feature smoke checks pass after v0.5 changes.
- [x] Gate 5: Manual QA scenarios in this tracker are complete and signed off.

## Validation notes
- Automated validation run on `2026-02-11`:
  - `./gradlew testDebugUnitTest --no-daemon` -> pass
  - `./gradlew lintDebug --no-daemon` -> pass (`0 errors, 24 warnings`, no new blocking findings)
  - Regression-targeted checks:
    - `./gradlew testDebugUnitTest --tests "*SearchViewModelTest*" --tests "*MyListViewModelTest*" --tests "*DetailsViewModelTest*" --tests "*EpisodeReminderWorker*"` -> pass
- Manual device QA + user sign-off completed on `2026-02-11`.

## Risks / watch items
- Completed items may lack fresh genre metadata at first-run, affecting recommendation quality.
- Personalized requests can increase API usage compared with the current single trending feed.
- Existing repository architecture hotspot can make feature integration riskier without bounded additions.

## Change log
- `2026-02-10`: Tracker created for v0.5 planning kickoff.
- `2026-02-11`: Added explicit automated-testing and regression-validation tasks, including release validation gates.
- `2026-02-11`: Implemented v0.5 Home resume hero, personalized rails, discover/genre repository plumbing, Room migration to v4, and added automated coverage.
- `2026-02-11`: Validation gates 1-4 completed.
- `2026-02-11`: Manual QA scenarios completed on device; Gate 5 completed and release sign-off received.
- `2026-02-11`: v0.5 docs archived under `docs/archive/v0.5`.
