# Project Lyra v0.5 Progress Tracker

## Release info
- Version: `v0.5`
- Start date: `2026-02-10`
- Completion date: `TBD`
- Target: Home quick-return hero + genre-personalized movie/TV discover rails
- Source of truth: `docs/v0.5/spec.md`

## Status legend
- `Not Started`
- `In Progress`
- `Blocked`
- `Done`

## Milestone tracker
| Milestone | Status | Notes |
| --- | --- | --- |
| Resume hero (Watching/On Hold priority) | Not Started | Replace trending hero with most recently edited in-progress item |
| Personalized movie recommendations rail | Not Started | Top 3 genres from completed items; exclude tracked items |
| Personalized TV recommendations rail | Not Started | Top 3 genres from completed items; exclude tracked items |
| Discover/genre data plumbing | Not Started | Add discover endpoints, genre metadata flow, and repository contracts |
| Automated tests + regression validation | Not Started | Add v0.5 automated coverage and run full validation gates before sign-off |
| QA + release sign-off | Not Started | Manual walkthrough for priority logic, personalization, fallback, and errors |

## Phased implementation plan
| Phase | Status | Scope | Deliverables | Exit criteria |
| --- | --- | --- | --- | --- |
| Phase 1: Resume hero behavior | Not Started | Implement top Home card selection from user activity state | DAO/repository/view-model support for latest `Watching` fallback to latest `On Hold`; updated Home hero UI | Hero always resolves correct priority item or cleanly hides when no candidate exists |
| Phase 2: Genre profile foundation | Not Started | Enable completed-item genre profiling prerequisites | Discover + genre endpoints integrated; genre metadata persistence/model wiring added | App can derive top 3 completed genres reliably |
| Phase 3: Personalized rails | Not Started | Render personalized movie + TV rails under hero | Two independent recommendation flows and UI sections with loading/error/empty behavior | Both rails return relevant, de-duplicated, untracked recommendations |
| Phase 4: Automated validation + hardening | Not Started | Add and execute automated tests + regression checks | New v0.5 test coverage, test/lint command results, regression notes | Automated gates pass and no blocking regressions remain |
| Phase 5: QA + release sign-off | Not Started | Validate UX simplicity and resilience | Manual QA report and tracker completion | All milestones marked `Done` and release checklist complete |

## Phase checklist
- [ ] Phase 1 complete
- [ ] Phase 2 complete
- [ ] Phase 3 complete
- [ ] Phase 4 complete
- [ ] Phase 5 complete

## Task checklist
- [ ] Add query path for most recently edited `Watching` item
- [ ] Add fallback query path for most recently edited `On Hold` item
- [ ] Update Home hero UI copy/content to "Continue Watching" semantics
- [ ] Keep hero actions focused on open/edit details flow
- [ ] Add TMDB discover movie and discover TV client methods
- [ ] Add TMDB genre metadata client methods
- [ ] Persist/cache genre metadata required for profile construction
- [ ] Derive top 3 genres from `Completed` user items
- [ ] Add personalized recommendation request model for movie rail
- [ ] Add personalized recommendation request model for TV rail
- [ ] Filter out already tracked items from recommendation outputs
- [ ] Add loading/error/empty states for both rails
- [ ] Ensure fallback content is shown when personalization profile is unavailable
- [ ] Add automated tests for resume hero selection priority (`Watching` first, `On Hold` fallback)
- [ ] Add automated tests for top-3 completed-genre profile derivation
- [ ] Add automated tests for personalized recommendation filtering of already tracked items
- [ ] Add automated tests for personalization fallback behavior (no completed profile / API failure)
- [ ] Run and pass automated unit/integration suite (`./gradlew testDebugUnitTest`)
- [ ] Run and pass lint baseline checks (`./gradlew lintDebug`) with no new blocking findings
- [ ] Run existing feature regression smoke checks (Search, My List status updates, Details loading, reminders scheduling path)
- [ ] Manual test: `Watching` hero candidate takes priority
- [ ] Manual test: `On Hold` fallback hero appears when `Watching` is empty
- [ ] Manual test: both recommendation rails render and open details correctly
- [ ] Manual test: no completed items fallback path
- [ ] Manual test: network error fallback path

## Release validation gates
- [ ] Gate 1: New v0.5 automated tests are present and passing.
- [ ] Gate 2: Existing automated tests continue to pass (`./gradlew testDebugUnitTest`).
- [ ] Gate 3: Lint passes with no new blocking issues (`./gradlew lintDebug`).
- [ ] Gate 4: Existing core feature smoke checks pass after v0.5 changes.
- [ ] Gate 5: Manual QA scenarios in this tracker are complete and signed off.

## Risks / watch items
- Completed items may lack fresh genre metadata at first-run, affecting recommendation quality.
- Personalized requests can increase API usage compared with the current single trending feed.
- Existing repository architecture hotspot can make feature integration riskier without bounded additions.

## Change log
- `2026-02-10`: Tracker created for v0.5 planning kickoff.
- `2026-02-11`: Added explicit automated-testing and regression-validation tasks, including release validation gates.
