# Spyglass Zoom Settings — Design

> Date: 2026-06-19
> Branch: `26.1` (implement here first, then port to other version branches)

## Goal

Add three zoom-related capabilities to Spyglass Only HUD:

1. **Disable zoom** — a settings toggle that turns off the mod's scroll-zoom entirely; the spyglass then behaves like vanilla.
2. **Smooth zoom** — the zoom transitions smoothly toward its target instead of snapping instantly.
3. **Configurable sensitivity** — a slider controlling how much the zoom changes per scroll notch.
4. **Configurable smoothness** — a slider controlling how fast the smooth transition is.

HUD hiding and overlay scale remain independent features and keep working regardless of the zoom toggle.

## Scope

- Implement on the `26.1` branch only for now. Porting to other branches is a separate follow-up.
- No automated tests (project has none); verification is in-game with a spyglass.

## Configuration (`SpyglassConfig`)

Three new persisted fields (JSON, alongside existing `hideHud` / `overlayScale`):

| Field | Type | Default | Range | Meaning |
|---|---|---|---|---|
| `zoomEnabled` | boolean | `true` | — | When `false`, scroll-zoom is fully off; spyglass = vanilla. |
| `zoomSensitivity` | double | `3.0` | `1.0`–`10.0` | Zoom step applied to the target zoom per scroll notch. |
| `zoomSmoothness` | double | `0.3` | `0.05`–`1.0` | Per-tick interpolation factor toward the target. `1.0` = instant (old behavior), `0.05` = very slow/smooth. |

Standard getters/setters; values default in if missing from an older config file.

## Smooth zoom mechanics (`SpyglassZoom`)

Rewrite to track three values (all `volatile double`):

- `targetZoom` — where the scroll wheel is aiming (snaps by `zoomSensitivity`).
- `currentZoom` — the actual displayed zoom at the current tick.
- `prevZoom` — `currentZoom` from the previous tick, for sub-tick interpolation.

Constants: `MIN_ZOOM = 1.0`, `MAX_ZOOM = 50.0`, `DEFAULT_ZOOM = 10.0`.

### `adjust(double delta)`
Move the **target** (not the displayed value):
```
targetZoom = clamp(targetZoom + signum(delta) * config.zoomSensitivity, MIN_ZOOM, MAX_ZOOM)
```

### `onTick(boolean isScoping)`
- On the transition from scoping → not scoping: reset `targetZoom`, `currentZoom`, `prevZoom` to `DEFAULT_ZOOM`.
- While scoping: `prevZoom = currentZoom`, then
  `currentZoom += (targetZoom - currentZoom) * config.zoomSmoothness`.
- If `config.zoomEnabled == false`: do nothing zoom-related (the values stay at default; the FOV mixin will not apply them anyway).

### `get(float partialTick)`
Return `lerp(partialTick, prevZoom, currentZoom)` for frame-smooth zoom between ticks. `lerp(t, a, b) = a + (b - a) * t`.

## Mixins

### `MouseHandlerMixin`
Only intercept/cancel the scroll when `config.zoomEnabled` is `true` (and player is scoping with no screen open). When zoom is disabled, the scroll passes through to vanilla (hotbar selection, etc.).

### `GameRendererMixin` (`Camera.calculateFov` RETURN)
When `config.zoomEnabled` is `false`, leave the FOV untouched (vanilla spyglass zoom). When enabled, apply `fov * (10.0 / SpyglassZoom.get(partialTick))` as today — now reading a smoothed value.

## UI (`SpyglassConfigScreen`)

Add to the existing options list, after the current two:

- **Zoom enabled** — boolean toggle (`OptionInstance.createBoolean`).
- **Zoom sensitivity** — slider mapping `0.0–1.0` UI range to `1.0–10.0`, label shows the step value (e.g. `3.0`).
- **Zoom smoothness** — slider mapping `0.0–1.0` UI range to `0.05–1.0` factor; label shows a friendly value (e.g. percentage or the raw factor). At max it is effectively instant.

Persist all three in `removed()` alongside the existing options, then `SpyglassConfig.save()`.

## Localization

Add keys to `en_us`, `ru_ru`, `uk_ua`:

- `spyglass-only-hud.config.zoomEnabled`
- `spyglass-only-hud.config.zoomSensitivity`
- `spyglass-only-hud.config.zoomSmoothness`

## Versioning

Bump `mod_version` in `gradle.properties` — minor bump (new feature).

## Verification

Build with JDK 25, then `runClient`:
1. Default config: scroll while scoping → zoom now eases in/out smoothly.
2. Sensitivity slider low/high → smaller/larger zoom step per scroll.
3. Smoothness at max → instant snap (old feel); at min → slow smooth ramp.
4. Disable zoom → scrolling does not zoom, spyglass shows vanilla zoom; HUD hiding still works.
5. Reopen settings → values persisted to `spyglass-only-hud.json`.

## Out of scope

- Porting to other Minecraft version branches (separate task).
- Changing `MAX_ZOOM` from the UI (sensitivity controls step only).
