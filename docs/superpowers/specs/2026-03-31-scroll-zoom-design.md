# Spyglass Mouse Wheel Zoom — Design Spec

**Date:** 2026-03-31
**Branch:** feature/scroll-zoom (to be created from `26.1`)
**Minecraft version:** 26.1 (unobfuscated)

---

## Summary

Add dynamic FOV-based zoom to the spyglass: while scoping, scrolling the mouse wheel zooms in/out by adjusting the FOV multiplier. Zoom resets to the vanilla default each time the player stops scoping.

---

## Zoom Parameters

| Parameter | Value |
|---|---|
| Minimum zoom | ×2 |
| Maximum zoom | ×15 |
| Default zoom | ×10 (vanilla spyglass value) |
| Step per scroll tick | ×1 |
| Persistence | Resets to default when player stops scoping |

---

## Architecture

### New class: `SpyglassZoom`

Holds zoom state and logic. No config persistence — zoom is session-transient, resets on each scoping exit.

```java
public class SpyglassZoom {
    public static final double MIN_ZOOM = 2.0;
    public static final double MAX_ZOOM = 15.0;
    public static final double DEFAULT_ZOOM = 10.0;
    public static final double STEP = 1.0;

    private static double currentZoom = DEFAULT_ZOOM;
    private static boolean wasScoping = false;

    public static double get() { return currentZoom; }

    public static void adjust(double delta) {
        currentZoom = Math.clamp(currentZoom + Math.signum(delta) * STEP, MIN_ZOOM, MAX_ZOOM);
    }

    public static void onTick(boolean isScoping) {
        if (wasScoping && !isScoping) {
            currentZoom = DEFAULT_ZOOM;
        }
        wasScoping = isScoping;
    }
}
```

### New mixin: `MouseHandlerMixin`

Targets `net.minecraft.client.MouseHandler.onScroll(long, double, double)`.

- Injected at `HEAD`, cancellable
- When player is scoping: calls `SpyglassZoom.adjust(yDelta)` and cancels the event (prevents hotbar slot change)
- When not scoping: passes through normally

### New mixin: `GameRendererMixin`

Targets `net.minecraft.client.renderer.GameRenderer.getFov(Camera, float, boolean)`.

- Uses `@Inject` at `RETURN` with `CallbackInfoReturnable<Float>` (standard Mixin, no extra libraries)
- When player is scoping: multiplies the returned FOV by `(10.0 / SpyglassZoom.get())` via `cir.setReturnValue(...)`
  - At zoom ×10: multiplier = 1.0 → vanilla behavior unchanged
  - At zoom ×15: multiplier = 0.667 → stronger zoom (smaller FOV)
  - At zoom ×2: multiplier = 5.0 → near-normal view (larger FOV)
- When not scoping: returns value unchanged

### Updated: `ClientTickMixin`

Add a call to `SpyglassZoom.onTick(isScoping)` at the start of the existing `onTick` inject, before the keybinding logic.

### Mixin registration

Add both new mixins to `src/main/resources/spyglassonlyhud.mixins.json`:
- `mixin.MouseHandlerMixin`
- `mixin.GameRendererMixin`

---

## Data Flow

```
Mouse scroll event
  → MouseHandlerMixin.onScroll()
      → player.isScoping()? yes
          → SpyglassZoom.adjust(yDelta)
          → ci.cancel()  // suppress hotbar change

Each game tick
  → ClientTickMixin.onTick()
      → SpyglassZoom.onTick(isScoping)
          → if wasScoping && !isScoping → reset zoom to 10.0

Camera render
  → GameRendererMixin (ModifyReturnValue on getFov)
      → player.isScoping()? yes
          → return fov * (10.0 / SpyglassZoom.get())
```

---

## Out of Scope

- No config option to enable/disable scroll zoom (can be added later if needed)
- No min/max zoom config (can be added later)
- No zoom indicator in HUD
- No changes to the overlay scale slider (remains independent)
- No port to other version branches (this feature targets `26.1` only initially)

---

## Version Bump

`mod_version` in `gradle.properties` must be bumped from `1.0.2` → `1.0.3` as part of this change.
