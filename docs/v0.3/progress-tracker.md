# Project Lyra v0.3 Progress Tracker

## Release info
- Version: `v0.3`
- Start date: `2026-02-09`
- Target: post-v0.2 UX polish and mobile layout improvements
- Source of truth: `docs/v0.3/spec.md`

## Status legend
- `Not Started`
- `In Progress`
- `Blocked`
- `Done`

## Milestone tracker
| Milestone | Status | Notes |
| --- | --- | --- |
| Poster card corner consistency | Done | Shared `PosterCard` now applies full-corner image clipping aligned to card radius |
| Details status control redesign | Done | Details page now uses a true select-style dropdown control with preselected current value |
| My List mobile tab readability | Done | My List now uses scrollable tabs with single-line labels to avoid wrapped/squished text on phones |
| End-to-end QA | Not Started | Validate target flows and edge states before sign-off |

## Phased implementation plan
| Phase | Status | Scope | Deliverables | Exit criteria |
| --- | --- | --- | --- | --- |
| Phase 1: Card corner polish | Done | Resolve poster corner inconsistency in shared card UI | Poster image and card corner treatment aligned across list usages | No visible top-only poster rounding remains |
| Phase 2: Details dropdown polish | Done | Redesign status selector to read as a true dropdown | Dropdown/select-style status UI on details page, behavior unchanged | Control is visually clear as single-select and status persistence still works |
| Phase 3: Tab layout polish | Done | Fix My List tab label fit on mobile | Readable tab row on phone widths without wrapped/squished labels | Tab labels remain legible and tab switching/filtering still works |
| Phase 4: QA + release sign-off | Not Started | Validate all v0.3 polish updates | Manual walkthrough and tracker completion | All milestones marked `Done` and checklist fully checked |

## Phase checklist
- [x] Phase 1 complete
- [x] Phase 2 complete
- [x] Phase 3 complete
- [ ] Phase 4 complete

## Task checklist
- [x] Fix poster image rounding so corners are consistent in card views
- [x] Ensure shared card remains visually aligned after corner fix
- [x] Replace details status button-like control with dropdown/select-styled UI
- [x] Preserve existing status update/untrack persistence behavior
- [x] Update My List tabs to avoid wrapped labels on phone widths
- [x] Preserve existing per-tab item filtering and active state behavior
- [ ] Manual test: home view card appearance
- [ ] Manual test: search view card appearance
- [ ] Manual test: media details status control
- [ ] Manual test: lists view tab readability and switching

## Change log
- `2026-02-09`: Phase 3 completed by switching My List to a scrollable tab row with single-line labels for phone readability.
- `2026-02-09`: Phase 2 completed by replacing the details status control with a select-style dropdown while preserving tracked/untracked persistence behavior.
- `2026-02-09`: Phase 1 completed by aligning shared `PosterCard` image clipping with card corner radius.
- `2026-02-09`: Tracker created for v0.3 scope kickoff.
