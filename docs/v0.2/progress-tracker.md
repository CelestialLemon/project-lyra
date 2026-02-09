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
| Card UI simplification | Not Started | Remove chips, description, and date from cards; keep poster + title only |
| Poster aspect ratio update | Not Started | Shift to portrait (`2:3`) and reduce crop severity |
| Year-only date formatting | Not Started | Show `YYYY` instead of full release date |
| Details-page status dropdown | Not Started | Empty default when untracked; updates persisted status |
| Lists page tabs | Not Started | Replace chips with tabs and filtered list views |
| End-to-end QA | Not Started | Validate all affected screens and edge states |

## Phased implementation plan
| Phase | Status | Scope | Deliverables | Exit criteria |
| --- | --- | --- | --- | --- |
| Phase 1: Card foundation | Not Started | Simplify shared card UI and move to portrait poster layout | Poster + title-only card component; chips/description/date removed from cards; portrait ratio applied in home/search/lists | Home/search/lists all render the same simplified card design with no card-level actions |
| Phase 2: Metadata + details status | Not Started | Standardize date to year-only and move status editing to details page | Year formatter (`YYYY`); status dropdown on details page with empty default for untracked items; persistence wiring | Details page can read/update status; untracked items show empty selection; no regressions in existing list assignment |
| Phase 3: Lists page navigation | Not Started | Replace chip switching with tabbed list buckets | Tabs on lists page; per-tab filtering; empty state per tab | Tab switching reliably updates visible items and active tab state is clear |
| Phase 4: QA + polish | Not Started | Validate flows and close visual/interaction gaps | Manual QA pass across home/search/details/lists; bug fixes; tracker completion update | All milestones marked `Done` and v0.2 tracker checklist fully checked |

## Phase checklist
- [ ] Phase 1 complete
- [ ] Phase 2 complete
- [ ] Phase 3 complete
- [ ] Phase 4 complete

## Task checklist
- [ ] Create/adjust shared media card component for poster + title only
- [ ] Remove card-level status chips/actions in all entry points
- [ ] Remove card-level description text in all entry points
- [ ] Remove card-level release date text in all entry points
- [ ] Apply portrait poster ratio (`2:3`) in home/search/list cards
- [ ] Tune image scale behavior to reduce cropping
- [ ] Implement year-only formatter for release metadata (`YYYY`)
- [ ] Add details-page status dropdown UI
- [ ] Wire dropdown to existing status persistence/update flow
- [ ] Handle empty dropdown value for items outside all buckets
- [ ] Replace list-switching chips with tabs on lists page
- [ ] Add empty state messaging per tab when no items exist
- [ ] Manual test: home view
- [ ] Manual test: search view
- [ ] Manual test: media details view
- [ ] Manual test: lists view and tab switching

## Change log
- `2026-02-09`: Tracker created.
- `2026-02-09`: Added phased implementation plan (Phase 1 to Phase 4).
