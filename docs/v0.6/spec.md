# Project Lyra v0.6 Spec

## Release summary

- Version: `v0.6`
- Goal: expand TV details into an episode-centric experience with precise watched/unwatched progress tracking.
- Scope boundary: Details screen UX + supporting API/data/storage/backup plumbing only.

## Locked product decisions

1. Show-level status remains manual (`WatchStatus` does not auto-sync from episode progress).
2. Episode unwatch action rewinds from selected episode to season end.
3. Episode progress is persisted in Room and included in backup export/import.
4. "Mark season as complete" marks aired episodes only.
5. Seasons are sorted ascending by season number and default selection is index `0` (including season `0`).
6. Episode watch actions are exposed via overflow menu on episode cards.
7. Visual direction is a refined version of the current cinematic details look.
8. Setting show status to `Not tracked` keeps episode progress.
9. Episode metadata is loaded on-demand by selected season (no persistent season-episode metadata cache in `v0.6`).

## Scope

### 1) Header and metadata polish

- Keep current cinematic header elements: dimming gradient, title text, media type, and year.
- Fix banner framing by using an aspect-ratio-driven container (replace fixed large height behavior that causes over-crop on many devices).
- Keep status dropdown in current location and behavior.
- Keep poster-left / metadata-right section with visual polish:
  - Genre separator changes from dot (`•`) to pipe (`|`).
  - Improved spacing/typography hierarchy.
  - Seasons/episodes totals remain here as the single summary source.
- Overview section remains unchanged.

#### Acceptance criteria

- Banner no longer appears over-zoomed/cropped on common phone widths.
- Existing readability gradient and top content hierarchy remain intact.
- Status dropdown placement and behavior are unchanged.
- Metadata section shows pipe-separated genres and improved styling.

### 2) Seasons section overhaul (selector + episodes)

- Replace season card list with:
  - Season dropdown selector.
  - "Mark season as complete" button below selector.
  - Episode list for active season.
- Default active season:
  - Sort by `seasonNumber` ascending.
  - Select first item in sorted list (index `0`).
- Episode cards use a horizontal layout and must show:
  - Episode image/still (fallback if unavailable).
  - Episode title.
  - Episode number.
  - Release date (fallback if unavailable).
  - Runtime when available.
- Remove duplicate season/episode totals from this section.

#### Acceptance criteria

- TV details shows a season dropdown and episode list, not season summary cards.
- Default season follows ascending + index `0` rule.
- Episode cards gracefully handle missing image/date/runtime.
- No repeated season/episode totals below overview.

### 3) Episode watched/unwatched tracking

- Episode card includes:
  - Visual watched indicator.
  - Overflow menu with context action.
- Overflow menu behavior:
  - Unwatched episode: `Mark watched up to this episode` marks all episodes `1..N` in active season watched.
  - Watched episode: `Mark unwatched from this episode` marks selected and later episodes unwatched.
- Season action:
  - `Mark season as complete` marks all eligible episodes watched.
  - Eligibility rule: `airDate <= today` OR missing `airDate`.
  - `today` is evaluated as local device calendar date (`LocalDate`) to avoid time-of-day cutoff errors.
- Show-level status remains independent from episode progress.
- Clearing show status to `Not tracked` does not delete watched-episode progress.

#### Acceptance criteria

- Watch/unwatch cascade behavior matches rules above.
- Season-complete excludes future-dated episodes.
- Watched indicator updates immediately after mutation success.
- Episode progress remains after clearing show status.

## Public interfaces and data changes

### TMDB API additions

- Add TV season details endpoint:
  - `GET /tv/{tv_id}/season/{season_number}`
- Add DTOs for season/episode payload fields:
  - Episode id
  - Episode number
  - Episode name
  - Still path
  - Air date
  - Runtime

### Domain/model additions

- Introduce dedicated TV season-episode domain models (separate from `MediaDetails` summary model).

### Repository contract additions

- Add season episode fetch contract (on-demand by selected season).
- Add watched-episode progress contracts:
  - Observe watched episode numbers for show + season.
  - Mark watched up to episode.
  - Mark unwatched from episode.
  - Mark season complete with eligible episode set.

### Room schema additions

- Add new watched-episodes table keyed by:
  - `media_item_id`
  - `season_number`
  - `episode_number`
- Add DAO for observe/query/upsert/delete operations required by cascade rules.
- Bump DB version `4 -> 5` with migration `MIGRATION_4_5`.
- Keep foreign key cascade to `media_items`.

### Backup schema additions

- Bump backup schema version `1 -> 2`.
- Add watched-episode payload list to backup document.
- Update validator and restore-plan normalization:
  - validate media references,
  - validate season/episode bounds,
  - reject duplicates per media+season+episode key.
- Ensure deterministic export ordering.

## Details screen state/data flow

1. Load existing media details as current behavior.
2. For TV with seasons:
   - Sort seasons ascending.
   - Select default season (index `0`).
   - Fetch selected season episodes.
   - Observe watched episodes for selected season.
3. On season change:
   - Cancel old watcher.
   - Load new season episodes on demand.
   - If a season was already loaded during this details session, reuse the in-memory season payload instead of refetching.
   - Observe watched set for the newly selected season.
4. On episode action:
   - Apply repository mutation (watch-up-to or unwatched-from).
   - Refresh watched indicator state via observed watched set.
5. On season-complete:
   - Compute eligible aired/missing-date episodes from loaded season payload.
   - Persist watched entries for eligible episodes.
6. On season-load failure:
   - Show inline error + retry in seasons section.
   - Keep details screen functional (no full-screen failure).

## Test plan

### Automated tests

1. `DetailsViewModel`:
   - default season selection,
   - season switch fetch path,
   - watch-up-to cascade,
   - unwatched-from cascade,
   - season-complete aired-only rule.
2. Repository/store:
   - watched-episode persistence correctness,
   - observe watched set by season,
   - clear status keeps episode progress,
   - season fetch success/error contracts.
3. Database migrations:
   - migration SQL test coverage for `MIGRATION_4_5`,
   - integration migration from older versions to v5 preserving existing data.
4. Backup:
   - schema v2 export contains watched episodes,
   - import restores watched episodes,
   - validation rejects duplicates/invalid keys.
5. Mapper:
   - TV season episode DTO mapping and null handling.

### Manual QA scenarios

1. Header framing validation on small/large phones.
2. Show with season `0` defaults correctly.
3. Episode overflow actions apply cascade rules.
4. Season-complete skips future-dated episodes.
5. Watched indicator consistency after mutations and navigation.
6. Set show to `Not tracked`, reopen details, episode progress remains.
7. Missing API key/offline season load shows section-level error/retry state.

## Out of scope for v0.6

- Home/My List episode-progress badges or summaries.
- Auto-sync from episode progress to show-level `WatchStatus`.
- Full offline cache of TMDB season episode metadata.
- Cross-device sync or external account sync for episode progress.

## Definition of done

- All acceptance criteria in this spec are met.
- Automated tests listed above are implemented and passing.
- `docs/v0.6/progress-tracker.md` is fully updated to final release state.
- Manual QA scenarios are executed and signed off.
