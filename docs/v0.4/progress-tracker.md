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
| App icon refresh | In Progress | New adaptive icon foreground + Android 13+ monochrome assets integrated from `app-icon-2048.png`; launcher-profile validation pending |
| Dynamic system accent support | In Progress | `ProjectLyraTheme` now applies dynamic dark color on Android 12+ when enabled, with static Lyra fallback retained |
| Settings theme preference | In Progress | Dynamic accent preference added to DataStore + Settings UI (Android 12+ visibility), pending manual behavior validation |
| Visual QA + contrast validation | Not Started | Validate readability and state clarity across key screens |
| End-to-end QA + release sign-off | Not Started | Manual walkthrough + tracker completion |

## Phased implementation plan
| Phase | Status | Scope | Deliverables | Exit criteria |
| --- | --- | --- | --- | --- |
| Phase 1: Icon pipeline | In Progress | Integrate new icon into launcher assets | Updated adaptive icon resources, round icon compatibility, themed icon support | Icon renders correctly in launcher surfaces without clipping/misalignment |
| Phase 2: Theme engine update | In Progress | Add dynamic color support and fallback behavior | Theme wiring for Android 12+ dynamic colors with existing palette fallback | UI reflects system accent on supported devices and remains stable on older APIs |
| Phase 3: Settings control | In Progress | Expose dynamic accent toggle and persist preference | New settings control, DataStore persistence, app restart consistency | User can enable/disable dynamic accent behavior predictably |
| Phase 4: Validation and sign-off | Not Started | Manual visual QA and release checks | QA notes for Home/Search/My List/Details/Settings, contrast checks, tracker completion | All milestones marked `Done` and release checklist complete |

## Phase checklist
- [ ] Phase 1 complete
- [ ] Phase 2 complete
- [ ] Phase 3 complete
- [ ] Phase 4 complete

## Task checklist
- [x] Import and prepare new icon source (`2048x2048`) for Android launcher assets
- [x] Update `ic_launcher`/`ic_launcher_round` outputs as needed
- [x] Add/update monochrome icon resource for themed icon support (Android 13+)
- [ ] Manual test icon rendering on at least two launcher profiles/device configs
- [x] Implement dynamic color theme path for Android 12+ devices
- [x] Preserve existing static palette fallback on Android 8-11
- [x] Add dynamic accent preference to app settings model + store
- [x] Add settings UI control for dynamic accent preference (supported devices)
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
- `2026-02-10`: Phase 1 implementation started; launcher icon resources updated and Android 13+ monochrome icon support added (manual launcher QA pending).
- `2026-02-10`: Phase 2 and Phase 3 implementation started; dynamic theme engine + persisted settings toggle added (manual QA pending).
