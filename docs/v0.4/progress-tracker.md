# Project Lyra v0.4 Progress Tracker

## Release info
- Version: `v0.4`
- Start date: `2026-02-10`
- Completion date: `TBD`
- Target: post-v0.3 visual system refresh (icon + dynamic accent theming)
- Source of truth: `docs/v0.4/spec.md`

## Status legend
- `Not Started`
- `In Progress`
- `Blocked`
- `Done`

## Milestone tracker
| Milestone | Status | Notes |
| --- | --- | --- |
| App icon refresh | Not Started | Replace launcher assets and verify adaptive + themed icon behavior |
| Dynamic system accent support | Not Started | Implement Material You dynamic theme on Android 12+ with fallback palette |
| Settings theme preference | Not Started | Add persistent toggle for dynamic accent usage |
| Visual QA + contrast validation | Not Started | Validate readability and state clarity across key screens |
| End-to-end QA + release sign-off | Not Started | Manual walkthrough + tracker completion |

## Phased implementation plan
| Phase | Status | Scope | Deliverables | Exit criteria |
| --- | --- | --- | --- | --- |
| Phase 1: Icon pipeline | Not Started | Integrate new icon into launcher assets | Updated adaptive icon resources, round icon compatibility, themed icon support | Icon renders correctly in launcher surfaces without clipping/misalignment |
| Phase 2: Theme engine update | Not Started | Add dynamic color support and fallback behavior | Theme wiring for Android 12+ dynamic colors with existing palette fallback | UI reflects system accent on supported devices and remains stable on older APIs |
| Phase 3: Settings control | Not Started | Expose dynamic accent toggle and persist preference | New settings control, DataStore persistence, app restart consistency | User can enable/disable dynamic accent behavior predictably |
| Phase 4: Validation and sign-off | Not Started | Manual visual QA and release checks | QA notes for Home/Search/My List/Details/Settings, contrast checks, tracker completion | All milestones marked `Done` and release checklist complete |

## Phase checklist
- [ ] Phase 1 complete
- [ ] Phase 2 complete
- [ ] Phase 3 complete
- [ ] Phase 4 complete

## Task checklist
- [ ] Import and prepare new icon source (`2048x2048`) for Android launcher assets
- [ ] Update `ic_launcher`/`ic_launcher_round` outputs as needed
- [ ] Add/update monochrome icon resource for themed icon support (Android 13+)
- [ ] Manual test icon rendering on at least two launcher profiles/device configs
- [ ] Implement dynamic color theme path for Android 12+ devices
- [ ] Preserve existing static palette fallback on Android 8-11
- [ ] Add dynamic accent preference to app settings model + store
- [ ] Add settings UI control for dynamic accent preference (supported devices)
- [ ] Validate buttons/tabs/switches/status text for readability under dynamic palettes
- [ ] Manual test: Home screen theme behavior
- [ ] Manual test: Search screen theme behavior
- [ ] Manual test: My List screen theme behavior
- [ ] Manual test: Details screen theme behavior
- [ ] Manual test: Settings screen theme behavior
- [ ] Final QA walkthrough and v0.4 sign-off

## Risks / watch items
- Dynamic accent palettes can reduce contrast for some color combinations; requires explicit readability checks.
- Launcher behavior for themed icons can vary across OEM launchers.
- Theme toggling should avoid jarring UI transitions or stale UI state.

## Change log
- `2026-02-10`: Tracker created for v0.4 planning kickoff.
