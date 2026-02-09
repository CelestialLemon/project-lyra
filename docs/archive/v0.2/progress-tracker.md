# Project Lyra v0.2 Progress Tracker

## Release info
- Version: `v0.2`
- Start date: `2026-02-09`
- Target: post-MVP UX and interaction refinement
- Source of truth: `docs/v0.2/spec.md`

## Status legend
- `Not Started`
- `In Progress`
- `Blocked`
- `Done`

## Milestone tracker
| Milestone | Status | Notes |
| --- | --- | --- |
| Card UI simplification | Done | Shared card now renders poster + title only across home/search/my-list with no card-level actions |
| Poster aspect ratio update | Done | Poster cards now use portrait (`2:3`) framing with less aggressive image cropping |
| Year-only date formatting | Done | Details textual date metadata now uses `YYYY` and gracefully hides unknown/invalid date values |
| Details-page status dropdown | Done | Details page now uses dropdown with empty default and supports clearing status to untrack/delete stored entries |
| Lists page tabs | Done | My List now uses status tabs with per-tab filtering and per-status empty state messages |
| End-to-end QA | Done | Product walkthrough completed across home/search/details/lists; v0.2 signed off |

## Phased implementation plan
| Phase | Status | Scope | Deliverables | Exit criteria |
| --- | --- | --- | --- | --- |
| Phase 1: Card foundation | Done | Simplify shared card UI and move to portrait poster layout | Poster + title-only card component; chips/description/date removed from cards; portrait ratio applied in home/search/lists | Home/search/lists all render the same simplified card design with no card-level actions |
| Phase 2: Metadata + details status | Done | Standardize date to year-only and move status editing to details page | Year formatter (`YYYY`); status dropdown on details page with empty default for untracked items; persistence wiring | Details page can read/update status; untracked items show empty selection; no regressions in existing list assignment |
| Phase 3: Lists page navigation | Done | Replace chip switching with tabbed list buckets | Tabs on lists page; per-tab filtering; empty state per tab | Tab switching reliably updates visible items and active tab state is clear |
| Phase 4: QA + polish | Done | Validate flows and close visual/interaction gaps | Manual QA pass across home/search/details/lists; bug fixes; tracker completion update | All milestones marked `Done` and v0.2 tracker checklist fully checked |

## Phase checklist
- [x] Phase 1 complete
- [x] Phase 2 complete
- [x] Phase 3 complete
- [x] Phase 4 complete

## Task checklist
- [x] Create/adjust shared media card component for poster + title only
- [x] Remove card-level status chips/actions in all entry points
- [x] Remove card-level description text in all entry points
- [x] Remove card-level release date text in all entry points
- [x] Apply portrait poster ratio (`2:3`) in home/search/list cards
- [x] Tune image scale behavior to reduce cropping
- [x] Implement year-only formatter for release metadata (`YYYY`)
- [x] Add details-page status dropdown UI
- [x] Wire dropdown to existing status persistence/update flow
- [x] Handle empty dropdown value for items outside all buckets
- [x] Handle empty dropdown selection for tracked items by deleting/untracking the stored user entry
- [x] Replace list-switching chips with tabs on lists page
- [x] Add empty state messaging per tab when no items exist
- [x] Manual test: home view
- [x] Manual test: search view
- [x] Manual test: media details view
- [x] Manual test: lists view and tab switching

## Change log
- `2026-02-09`: Tracker created.
- `2026-02-09`: Added phased implementation plan (Phase 1 to Phase 4).
- `2026-02-09`: Completed Phase 1 card foundation updates (poster + title-only cards and portrait poster framing).
- `2026-02-09`: Logged known temporary gap from Phase 1: no in-card untrack action on My List. Planned resolution in Phase 2 via details status dropdown empty option that deletes/untracks entries.
- `2026-02-09`: Completed Phase 2 metadata + details updates (year-only date formatting and dropdown-based tracked status management with empty/untrack support).
- `2026-02-09`: Completed Phase 3 lists navigation updates (tabs replacing chips plus per-status empty-state messaging).
- `2026-02-09`: Completed Phase 4 QA sign-off and marked v0.2 release scope complete.
