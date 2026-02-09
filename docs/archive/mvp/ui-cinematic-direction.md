# UI Direction - Cinematic Poster-First

Date: February 9, 2026

## Intent
Design a media-tracking app that feels like a premium streaming catalog while staying lightweight and fast.

## Visual Principles
- Poster-led layout over text-led layout.
- High contrast hierarchy: poster -> title -> status/actions.
- Minimal chrome; content should carry the interface.
- Motion used for continuity, not decoration.

## Core UI Building Blocks
- Hero carousel for top trending titles (Home).
- Horizontal poster rails per content group.
- Status chips for `Want to Watch`, `Watching`, `On Hold`, `Dropped`, `Completed`.
- Immersive details screen with backdrop + metadata overlay.
- Dense but elegant list mode for personal statuses.

## Motion Direction
- Soft parallax on hero posters.
- Fade/slide transitions between feed and details.
- Subtle staggered reveals for rows on first load.
- Respect reduced motion accessibility settings.

## Color + Theme Direction
- Deep neutral base with cinematic accents derived from poster palette.
- Accent color is dynamic from artwork, but controls remain readable.
- Avoid noisy gradients; use broad atmospheric gradients only.

## Typography Direction
- Display style for titles/headlines.
- Clean sans-serif for body and controls.
- Tight vertical rhythm to keep screens compact on mobile.

## Performance Guardrails
- Prefetch next posters in visible rails.
- Aggressive image caching with downsampled thumbnails.
- Avoid heavy blur chains and nested lazy lists.
- Keep first meaningful paint under 1 second on warm start.

## Accessibility Baseline
- Minimum touch target 48dp.
- All poster cards have title/status semantics.
- Dynamic type scaling support.
- Contrast checks for chip text on gradient surfaces.
