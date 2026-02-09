# Project Lyra v0.3 Spec

## Release summary
- Version: `v0.3`
- Goal: polish core UI affordances after v0.2 sign-off, with focus on visual quality and mobile readability.

## Scope

### 1) Poster card corner consistency
- Fix poster card rendering so poster image corners are consistently rounded.
- Remove the current visual mismatch where top corners are rounded and bottom corners appear square.

#### Acceptance criteria
- Poster images in card views render with visually consistent rounded corners.
- No card variant shows top-only rounding on the poster image.
- Card layout spacing/title alignment remains unchanged from v0.2 behavior.

### 2) Details status control visual redesign
- Keep current status-change behavior, but present it as a true single-select dropdown control.
- Replace button-like appearance with dropdown/select styling that matches user expectation.

#### Acceptance criteria
- Details status control is visually recognizable as a dropdown/select input.
- Existing status is still preselected when present.
- Untracked state remains available and selectable.
- Selecting a status (or untracked) preserves v0.2 persistence behavior.

### 3) My List tab readability on phones
- Fix tab labels in My List so mobile layouts do not wrap/squish long status names.
- Ensure navigation remains clear and usable on narrow widths.

#### Acceptance criteria
- My List tabs are readable on typical phone widths.
- Tab labels no longer wrap into multi-line cramped text.
- Tab switching/filter behavior from v0.2 remains unchanged.

## Out of scope for v0.3
- New recommendation/discovery features.
- Domain model/backend redesign.
- New list buckets or status taxonomy changes.

## Implementation notes
- Preserve v0.2 behavior; target visual/interaction refinement only.
- Prefer component-level changes so fixes apply consistently across screens.
- Validate mobile layout behavior for both compact and larger phone widths.

## Definition of done
- All acceptance criteria above are met.
- Manual product walkthrough completed for cards, details status control, and My List tabs.
- `docs/v0.3/progress-tracker.md` updated with final status and completion date.
