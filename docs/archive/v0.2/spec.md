# Project Lyra v0.2 Spec

## Release summary
- Version: `v0.2`
- Goal: clean up card UI, improve poster presentation, and move status controls into more appropriate views.

## Scope

### 1) Card UI simplification (home, search, list results)
- Show only poster image and media title/name.
- Remove status chips/actions from cards.
- Remove description text from cards.
- Remove release date text from cards.

#### Acceptance criteria
- Every media card in home/search/list views renders only poster + title.
- No status update controls are visible on cards.
- No description snippets are visible on cards.
- No date text is visible on cards.

### 2) Poster aspect ratio and image framing
- Move card posters to a portrait presentation to match streaming conventions.
- Preferred baseline ratio: `2:3` (portrait).
- Adjust image rendering to avoid aggressive center-cropping where possible and show more of the original poster.

#### Acceptance criteria
- Poster cards render in a portrait ratio consistently across home, search, and list views.
- Image framing shows significantly more of the full poster than current cropped behavior.
- Cards remain visually aligned in grids/lists on common device widths.

### 3) Release date display format
- Replace full date display with year-only where date is shown in textual metadata.
- Year format: `YYYY`.

#### Acceptance criteria
- Any UI that previously displayed full release date now displays only the year.
- Missing/unknown date gracefully renders an empty or fallback year value without crashing.

### 4) Status update on media details page
- Add a status dropdown on the media details screen.
- Dropdown allows assigning/changing current status bucket.
- If media is not in any bucket, dropdown defaults to empty value.

#### Acceptance criteria
- Opening media details shows a status dropdown.
- Existing status is pre-selected when present.
- Untracked media opens with empty dropdown state.
- Changing dropdown value updates persisted status and reflects in lists.

### 5) Lists page interaction redesign
- Replace chip-based list switching with tab-based navigation.
- Each tab corresponds to one list/bucket and filters visible items accordingly.

#### Acceptance criteria
- Lists page uses tabs instead of chips.
- Selecting each tab updates the item set to that list only.
- Active tab state is visually clear and persists during page interaction.

## Out of scope for v0.2
- Major backend/domain model redesign.
- New recommendation/discovery features.
- Notifications or social features.

## Implementation notes
- Keep behavior changes incremental and testable per screen.
- Prefer reusable card components so home/search/list stay visually consistent.
- Validate empty states for each tab and for details-page status selection.

## Definition of done
- All acceptance criteria above are met.
- Manual QA completed on home, search, details, and lists screens.
- `docs/v0.2/progress-tracker.md` updated with final status and completion date.
