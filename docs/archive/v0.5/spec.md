# Project Lyra v0.5 Spec

## Release summary

- Version: `v0.5`
- Goal: reshape Home into a quick-return surface for in-progress tracking, followed by personalized Discover recommendations driven by completed-list genre preferences.

## Scope

### 1) Home hero: quick access to likely next action

- Replace the current "Tonight's Trending" hero with a single "Continue Watching" card.
- Card selection priority:
  1. Most recently edited item in `Watching` (`updated_at` descending).
  2. If `Watching` is empty, most recently edited item in `On Hold`.
- Hero actions:
  - Open details.
  - Edit status (by opening details status control).

#### Acceptance criteria

- Home no longer shows "Tonight's Trending" as the top hero content.
- If at least one `Watching` item exists, the most recently edited `Watching` item is shown.
- If `Watching` is empty and `On Hold` has items, the most recently edited `On Hold` item is shown.
- If both lists are empty, Home shows no resume hero and continues with discovery content/fallback state without crash.
- Tapping hero opens details for the selected item.

### 2) Personalized Discover rails under hero

- Add two separate recommendation rails below the hero:
  - Recommended Movies
  - Recommended TV Shows
- Build a user preference profile from genres of `Completed` items.
- Use top 3 genres from that profile for recommendation queries.
- Keep provider agnostic (no watch-provider filtering in this release).

#### Acceptance criteria

- Home displays two distinct recommendation rails (Movies and TV) when recommendation data is available.
- Recommendation queries are genre-driven from the top 3 genres inferred from `Completed` items.
- Recommendations exclude titles already tracked by the user (all statuses).
- Recommendation load/error states are non-blocking and do not break hero rendering.
- If completed-genre profile is unavailable (for example no completed items), fallback behavior is used.

### 3) Fallback behavior and simplicity guardrails

- Keep Home visually simple: one hero + two rails max in primary state.
- Use a deterministic fallback path when personalized recommendations cannot be computed.
- Preserve existing network/cache resilience patterns.

#### Acceptance criteria

- Home does not add extra filter controls or multi-section clutter in v0.5.
- If personalization cannot run, Home still provides useful discovery fallback content.
- Existing offline/error handling patterns remain intact with clear user messaging.

### 4) Data and API additions required for v0.5

- Add TMDB Discover endpoints for movie and TV.
- Add genre-list metadata support and local persistence needed for completed-item genre profiling.
- Add repository/query models for recommendation requests and result mapping.

#### Acceptance criteria

- Discover movie and TV requests are wired through the repository layer with explicit result contracts.
- Genre metadata needed for profile construction is available at runtime and cached locally.
- Home view model can request:
  - Resume hero candidate.
  - Recommended movies.
  - Recommended TV shows.

## Out of scope for v0.5

- Provider-aware discovery and watch-provider filters.
- Account-linked TMDB sync (favorites/watchlist/ratings).
- Complex Discover filter UI (certifications, people/cast/crew filters, advanced boolean builders).
- Full Discover browse screen with editable filter controls.

## Implementation notes

- Reuse existing local status timestamps (`updated_at`) to determine resume priority.
- Add focused repository/store APIs instead of increasing `LibraryRepository` coupling.
- Keep recommendation ranking simple for v0.5:
  - genre match from top 3 completed genres,
  - quality/popularity sort from TMDB response,
  - remove tracked duplicates.
- Maintain current local-first behavior and cache fallback expectations.

## Definition of done

- All v0.5 acceptance criteria are met.
- Manual walkthrough completed for:
  - resume hero priority behavior,
  - personalized movie rail,
  - personalized TV rail,
  - empty-profile and network-fallback states.
- `docs/archive/v0.5/progress-tracker.md` updated with final status and completion notes.
