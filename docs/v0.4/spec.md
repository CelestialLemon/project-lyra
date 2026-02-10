# Project Lyra v0.4 Spec

## Release summary

- Version: `v0.4`
- Goal: deliver a visual system refresh with a new launcher identity and Android system-accent integration while preserving current app behavior.

## Scope

### 1) App icon refresh (launcher + themed icon)

- Replace current launcher icon with the new v0.4 icon source (`2048x2048`).
- Generate/update adaptive icon assets used by launcher surfaces.
- Add/update monochrome themed icon support for Android 13+ launchers.

#### Acceptance criteria

- App launcher icon is updated for both standard and round icon variants.
- Adaptive icon rendering is not clipped or misaligned on common launchers.
- Themed icon appears correctly on Android launchers that support themed icons.
- Existing app behavior and startup flow remain unchanged.

### 2) Dynamic system accent color support (Material You)

- On Android 12+ (`API 31+`), use Android dynamic color so app accents follow the system wallpaper-derived palette.
- Keep current Lyra dark palette as fallback for Android 8-11 (`API 26-30`).

#### Acceptance criteria

- On Android 12+, accent-sensitive UI surfaces (buttons, selected tabs, switches, highlights, etc.) reflect system dynamic colors.
- On Android 8-11, app still uses stable fallback palette with no functional regression.
- Theme initialization does not introduce crashes or startup delays.

### 3) Theme preference control in Settings

- Add a settings toggle to enable/disable dynamic accent usage on supported devices.
- Default behavior: enabled on Android 12+; hidden or disabled on unsupported API levels.

#### Acceptance criteria

- Settings screen exposes a clear control for dynamic accent usage on supported devices.
- Toggling the setting updates app theming predictably.
- Preference persists across app restarts.

### 4) Visual QA and contrast pass

- Validate readability/contrast after dynamic theme integration across key screens.
- Ensure status/error/success states remain distinguishable with dynamic palettes.

#### Acceptance criteria

- Home, Search, My List, Details, and Settings remain readable under multiple dynamic palettes.
- Primary actions and status messaging remain visually clear in both dynamic and fallback themes.
- No major accessibility regressions are found during manual QA.

## Implementation notes

- Theme update target files likely include `app/src/main/java/com/projectlyra/app/ui/theme/Theme.kt`, `app/src/main/java/com/projectlyra/app/data/settings/AppSettings.kt`, `app/src/main/java/com/projectlyra/app/data/settings/SettingsStore.kt`, and `app/src/main/java/com/projectlyra/app/feature/settings/SettingsScreen.kt`.
- Use `dynamicDarkColorScheme(context)` for Android 12+ integration, with existing palette fallback.
- Validate icon assets on at least one Pixel launcher and one non-Pixel launcher/device profile when possible.

## Definition of done

- All v0.4 acceptance criteria are met.
- Manual walkthrough completed for icon appearance and theming behavior across supported API ranges.
- `docs/v0.4/progress-tracker.md` updated with completion status and final notes.
