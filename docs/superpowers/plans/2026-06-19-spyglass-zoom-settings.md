# Spyglass Zoom Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a zoom on/off toggle, configurable scroll sensitivity, and smooth (eased) zoom transitions to Spyglass Only HUD.

**Architecture:** Three new persisted fields in `SpyglassConfig`. `SpyglassZoom` is rewritten to track a `targetZoom` (moved by scroll) and a smoothed `currentZoom`/`prevZoom` pair that eases toward the target each tick and interpolates per-frame by `partialTick`. The two zoom mixins read `zoomEnabled` so they no-op when zoom is disabled (spyglass = vanilla). The config screen exposes a toggle plus two sliders.

**Tech Stack:** Java 25, Fabric Loom (Minecraft 26.1, unobfuscated), Mixin, Gson, Minecraft `OptionInstance` UI.

## Global Constraints

- Branch: `26.1` only. Do NOT port to other branches in this plan.
- No automated test harness exists in this project. The per-task gate is a successful Gradle build (compile). Manual in-game verification happens in the final task.
- Build command (always use this exact form):
  `./gradlew build -Dorg.gradle.java.home="C:/Users/Reva1v/jdk25/jdk-25.0.2"`
- `Identifier` is the ID class on 26.1; `KeyMapping.Category.register()` is the keybinding API (not relevant to these tasks but do not alter).
- Zoom constants are fixed: `MIN_ZOOM = 1.0`, `MAX_ZOOM = 50.0`, `DEFAULT_ZOOM = 10.0`. Sensitivity controls step only; `MAX_ZOOM` is not configurable.
- Config field semantics: `zoomSensitivity` ∈ [1.0, 10.0] (default 3.0); `zoomSmoothness` ∈ [0.05, 1.0] (default 0.3) where 1.0 = instant snap and 0.05 = slowest/smoothest.

---

### Task 1: Config fields

**Files:**
- Modify: `src/main/java/com/spyglasshud/SpyglassConfig.java`

**Interfaces:**
- Consumes: nothing (foundation task).
- Produces: on `SpyglassConfig.get()` — `boolean isZoomEnabled()`, `void setZoomEnabled(boolean)`, `double getZoomSensitivity()`, `void setZoomSensitivity(double)`, `double getZoomSmoothness()`, `void setZoomSmoothness(double)`. New persisted JSON fields `zoomEnabled` (default `true`), `zoomSensitivity` (default `3.0`), `zoomSmoothness` (default `0.3`). Gson runs the no-arg constructor, so a pre-existing config file missing these keys keeps the field-initializer defaults — no migration code needed.

- [ ] **Step 1: Add the three fields**

In `SpyglassConfig.java`, after the existing `private double overlayScale = 0.90;` line, add:

```java
    private boolean zoomEnabled = true;
    private double zoomSensitivity = 3.0;
    private double zoomSmoothness = 0.3;
```

- [ ] **Step 2: Add getters and setters**

After the existing `setOverlayScale(...)` method (before `public static void load()`), add:

```java
    public boolean isZoomEnabled() {
        return zoomEnabled;
    }

    public void setZoomEnabled(boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
    }

    public double getZoomSensitivity() {
        return zoomSensitivity;
    }

    public void setZoomSensitivity(double zoomSensitivity) {
        this.zoomSensitivity = zoomSensitivity;
    }

    public double getZoomSmoothness() {
        return zoomSmoothness;
    }

    public void setZoomSmoothness(double zoomSmoothness) {
        this.zoomSmoothness = zoomSmoothness;
    }
```

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew build -Dorg.gradle.java.home="C:/Users/Reva1v/jdk25/jdk-25.0.2"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/spyglasshud/SpyglassConfig.java
git commit -m "feat: add zoom config fields (enabled, sensitivity, smoothness)"
```

---

### Task 2: Smooth zoom logic

**Files:**
- Modify (rewrite): `src/main/java/com/spyglasshud/SpyglassZoom.java`

**Interfaces:**
- Consumes (Task 1): `SpyglassConfig.get().getZoomSensitivity()`, `SpyglassConfig.get().getZoomSmoothness()`.
- Produces: `double SpyglassZoom.get(float partialTick)` (now returns the per-frame smoothed zoom), `void SpyglassZoom.adjust(double delta)` (moves the target by sensitivity), `void SpyglassZoom.onTick(boolean isScoping)` (eases current → target, resets on unscope). Public constants `MIN_ZOOM`, `MAX_ZOOM`, `DEFAULT_ZOOM` unchanged.

- [ ] **Step 1: Replace the file contents**

Replace the entire body of `SpyglassZoom.java` with:

```java
package com.spyglasshud;

public class SpyglassZoom {
    public static final double MIN_ZOOM = 1.0;
    public static final double MAX_ZOOM = 50.0;
    public static final double DEFAULT_ZOOM = 10.0;

    private static volatile double targetZoom = DEFAULT_ZOOM;
    private static volatile double currentZoom = DEFAULT_ZOOM;
    private static volatile double prevZoom = DEFAULT_ZOOM;
    private static volatile boolean wasScoping = false;

    /**
     * Per-frame smoothed zoom: interpolates between the previous and current
     * tick values by partialTick so the zoom is smooth above 20 FPS.
     */
    public static double get(float partialTick) {
        return prevZoom + (currentZoom - prevZoom) * partialTick;
    }

    /**
     * Called by MouseHandlerMixin when the player scrolls while scoping.
     * delta > 0 = scroll up (zoom in), delta < 0 = scroll down (zoom out).
     * Moves the target; the displayed zoom eases toward it each tick.
     */
    public static void adjust(double delta) {
        double step = SpyglassConfig.get().getZoomSensitivity();
        targetZoom = Math.clamp(targetZoom + Math.signum(delta) * step, MIN_ZOOM, MAX_ZOOM);
    }

    /**
     * Called every tick by ClientTickMixin.
     * Eases currentZoom toward targetZoom; resets all zoom state when the
     * player stops scoping.
     */
    public static void onTick(boolean isScoping) {
        if (wasScoping && !isScoping) {
            targetZoom = DEFAULT_ZOOM;
            currentZoom = DEFAULT_ZOOM;
            prevZoom = DEFAULT_ZOOM;
        } else if (isScoping) {
            double smoothness = Math.clamp(SpyglassConfig.get().getZoomSmoothness(), 0.05, 1.0);
            prevZoom = currentZoom;
            currentZoom += (targetZoom - currentZoom) * smoothness;
        }
        wasScoping = isScoping;
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew build -Dorg.gradle.java.home="C:/Users/Reva1v/jdk25/jdk-25.0.2"`
Expected: `BUILD SUCCESSFUL`. (`GameRendererMixin` already calls `get(partialTick)` and `MouseHandlerMixin` already calls `adjust(yDelta)`, so signatures stay compatible.)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/spyglasshud/SpyglassZoom.java
git commit -m "feat: smooth eased zoom toward a scroll-driven target"
```

---

### Task 3: Mixin guards for zoom-disabled

**Files:**
- Modify: `src/main/java/com/spyglasshud/mixin/MouseHandlerMixin.java`
- Modify: `src/main/java/com/spyglasshud/mixin/GameRendererMixin.java`

**Interfaces:**
- Consumes (Task 1): `SpyglassConfig.get().isZoomEnabled()`.
- Produces: behavior change only. When `zoomEnabled` is false, scroll passes through to vanilla and the spyglass FOV is untouched.

- [ ] **Step 1: Guard the scroll interception**

In `MouseHandlerMixin.java`, add the import (with the other `com.spyglasshud` imports):

```java
import com.spyglasshud.SpyglassConfig;
```

Replace the `if` condition in `onScroll` with:

```java
        if (client.player != null && client.player.isScoping() && client.screen == null
                && SpyglassConfig.get().isZoomEnabled()) {
            SpyglassZoom.adjust(yDelta);
            ci.cancel();
        }
```

- [ ] **Step 2: Guard the FOV modification**

In `GameRendererMixin.java`, add the import:

```java
import com.spyglasshud.SpyglassConfig;
```

Replace the `if` condition in `modifySpyglassFov` with:

```java
        if (client.player != null && client.player.isScoping() && client.screen == null
                && SpyglassConfig.get().isZoomEnabled()) {
            float fov = cir.getReturnValue();
            cir.setReturnValue(fov * (float) (10.0 / SpyglassZoom.get(partialTick)));
        }
```

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew build -Dorg.gradle.java.home="C:/Users/Reva1v/jdk25/jdk-25.0.2"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/spyglasshud/mixin/MouseHandlerMixin.java src/main/java/com/spyglasshud/mixin/GameRendererMixin.java
git commit -m "feat: skip zoom scroll and FOV change when zoom is disabled"
```

---

### Task 4: Config screen options + localization

**Files:**
- Modify: `src/main/java/com/spyglasshud/SpyglassConfigScreen.java`
- Modify: `src/main/resources/assets/spyglass-only-hud/lang/en_us.json`
- Modify: `src/main/resources/assets/spyglass-only-hud/lang/ru_ru.json`
- Modify: `src/main/resources/assets/spyglass-only-hud/lang/uk_ua.json`

**Interfaces:**
- Consumes (Task 1): `isZoomEnabled()`/`setZoomEnabled(boolean)`, `getZoomSensitivity()`/`setZoomSensitivity(double)`, `getZoomSmoothness()`/`setZoomSmoothness(double)`.
- Produces: three new UI controls and their lang keys. Slider math: **sensitivity** maps UI `[0,1]` → `1.0 + v*9.0` (= 1.0–10.0); **smoothness** is inverted so higher slider = smoother: factor `= 1.0 - v*0.95` (v=1 → 0.05 smoothest, v=0 → 1.0 instant), and the label shows `round(v*100)%`.

- [ ] **Step 1: Add option fields**

In `SpyglassConfigScreen.java`, after the existing field `private OptionInstance<Double> overlayScaleOption;`, add:

```java
    private OptionInstance<Boolean> zoomEnabledOption;
    private OptionInstance<Double> zoomSensitivityOption;
    private OptionInstance<Double> zoomSmoothnessOption;
```

- [ ] **Step 2: Build the options in `addOptions()`**

In `addOptions()`, immediately before the `this.list.addBig(hideHudOption);` line, add:

```java
        zoomEnabledOption = OptionInstance.createBoolean(
                "spyglass-only-hud.config.zoomEnabled",
                config.isZoomEnabled()
        );

        zoomSensitivityOption = new OptionInstance<>(
                "spyglass-only-hud.config.zoomSensitivity",
                OptionInstance.noTooltip(),
                (caption, value) -> {
                    double actual = 1.0 + value * 9.0; // map 0.0-1.0 to 1.0-10.0
                    return Component.translatable("spyglass-only-hud.config.zoomSensitivity")
                            .append(": " + String.format("%.1f", actual));
                },
                OptionInstance.UnitDouble.INSTANCE,
                Codec.doubleRange(0.0, 1.0),
                (config.getZoomSensitivity() - 1.0) / 9.0, // map 1.0-10.0 to 0.0-1.0
                value -> {}
        );

        zoomSmoothnessOption = new OptionInstance<>(
                "spyglass-only-hud.config.zoomSmoothness",
                OptionInstance.noTooltip(),
                (caption, value) ->
                        Component.translatable("spyglass-only-hud.config.zoomSmoothness")
                                .append(": " + Math.round(value * 100) + "%"),
                OptionInstance.UnitDouble.INSTANCE,
                Codec.doubleRange(0.0, 1.0),
                (1.0 - config.getZoomSmoothness()) / 0.95, // factor -> slider (higher = smoother)
                value -> {}
        );
```

- [ ] **Step 3: Register the options in the list**

Replace the two existing `this.list.addBig(...)` lines at the end of `addOptions()` with:

```java
        this.list.addBig(hideHudOption);
        this.list.addBig(overlayScaleOption);
        this.list.addBig(zoomEnabledOption);
        this.list.addBig(zoomSensitivityOption);
        this.list.addBig(zoomSmoothnessOption);
```

- [ ] **Step 4: Persist the new options in `removed()`**

In `removed()`, after the existing `config.setOverlayScale(...)` line and before `SpyglassConfig.save();`, add:

```java
        config.setZoomEnabled(zoomEnabledOption.get());
        config.setZoomSensitivity(1.0 + zoomSensitivityOption.get() * 9.0);
        config.setZoomSmoothness(1.0 - zoomSmoothnessOption.get() * 0.95);
```

- [ ] **Step 5: Add English lang keys**

Replace the contents of `en_us.json` with:

```json
{
    "key.category.spyglass-only-hud.settings": "Spyglass Only HUD",
    "spyglass-only-hud.key.openConfig": "Open Settings",
    "spyglass-only-hud.config.title": "Spyglass Only HUD Settings",
    "spyglass-only-hud.config.hideHud": "Hide HUD When Scoping",
    "spyglass-only-hud.config.overlayScale": "Overlay Scale",
    "spyglass-only-hud.config.zoomEnabled": "Scroll Zoom",
    "spyglass-only-hud.config.zoomSensitivity": "Zoom Sensitivity",
    "spyglass-only-hud.config.zoomSmoothness": "Zoom Smoothness"
}
```

- [ ] **Step 6: Add Russian lang keys**

Replace the contents of `ru_ru.json` with:

```json
{
    "key.category.spyglass-only-hud.settings": "Spyglass Only HUD",
    "spyglass-only-hud.key.openConfig": "Открыть настройки",
    "spyglass-only-hud.config.title": "Настройки Spyglass Only HUD",
    "spyglass-only-hud.config.hideHud": "Скрывать HUD при использовании подзорной трубы",
    "spyglass-only-hud.config.overlayScale": "Масштаб наложения",
    "spyglass-only-hud.config.zoomEnabled": "Зум колёсиком",
    "spyglass-only-hud.config.zoomSensitivity": "Чувствительность зума",
    "spyglass-only-hud.config.zoomSmoothness": "Плавность зума"
}
```

- [ ] **Step 7: Add Ukrainian lang keys**

Replace the contents of `uk_ua.json` with:

```json
{
    "key.category.spyglass-only-hud.settings": "Spyglass Only HUD",
    "spyglass-only-hud.key.openConfig": "Відкрити налаштування",
    "spyglass-only-hud.config.title": "Налаштування Spyglass Only HUD",
    "spyglass-only-hud.config.hideHud": "Приховувати HUD при використанні підзорної труби",
    "spyglass-only-hud.config.overlayScale": "Масштаб накладення",
    "spyglass-only-hud.config.zoomEnabled": "Зум колесом",
    "spyglass-only-hud.config.zoomSensitivity": "Чутливість зуму",
    "spyglass-only-hud.config.zoomSmoothness": "Плавність зуму"
}
```

- [ ] **Step 8: Build to verify it compiles**

Run: `./gradlew build -Dorg.gradle.java.home="C:/Users/Reva1v/jdk25/jdk-25.0.2"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/spyglasshud/SpyglassConfigScreen.java src/main/resources/assets/spyglass-only-hud/lang/
git commit -m "feat: add zoom toggle, sensitivity and smoothness settings UI"
```

---

### Task 5: Version bump and in-game verification

**Files:**
- Modify: `gradle.properties`

**Interfaces:**
- Consumes: the complete feature from Tasks 1–4.
- Produces: released artifact `spyglass-only-hud-1.2.0-26.1.jar`.

- [ ] **Step 1: Bump the mod version**

In `gradle.properties`, change:

```
mod_version=1.1.0
```

to:

```
mod_version=1.2.0
```

- [ ] **Step 2: Build the release jar**

Run: `./gradlew build -Dorg.gradle.java.home="C:/Users/Reva1v/jdk25/jdk-25.0.2"`
Expected: `BUILD SUCCESSFUL`, and `build/libs/spyglass-only-hud-1.2.0-26.1.jar` exists.

- [ ] **Step 3: Manual in-game verification**

Run: `./gradlew runClient -Dorg.gradle.java.home="C:/Users/Reva1v/jdk25/jdk-25.0.2"`
Then, holding a spyglass, verify each:

1. Default config — scroll while scoping eases the zoom in/out smoothly (no instant snap).
2. Open settings (K). Set **Zoom Sensitivity** low, then high — the zoom step per scroll notch is smaller / larger accordingly.
3. Set **Zoom Smoothness** to max (100%) — zoom ramps very slowly/smoothly; set to min (≈0%/5%) — zoom is effectively instant.
4. Turn **Scroll Zoom** off — scrolling no longer zooms (it controls the hotbar again) and the spyglass shows the plain vanilla zoom; HUD is still hidden while scoping; overlay scale still applies.
5. Close and reopen settings — all three values persisted (check `<config dir>/spyglass-only-hud.json`).

- [ ] **Step 4: Commit**

```bash
git add gradle.properties
git commit -m "chore: bump mod_version to 1.2.0 for zoom settings"
```

---

## Self-Review

- **Spec coverage:** disable-zoom toggle → Tasks 1, 3, 4. Smooth zoom → Task 2. Configurable sensitivity → Tasks 1, 2, 4. Configurable smoothness → Tasks 1, 2, 4. Localization → Task 4. Version bump → Task 5. Verification checklist → Task 5 Step 3. All spec sections covered.
- **Placeholder scan:** no TBD/TODO; every code step shows complete code.
- **Type consistency:** getter/setter names (`isZoomEnabled`/`getZoomSensitivity`/`getZoomSmoothness`) defined in Task 1 are used verbatim in Tasks 2–4. `get(float)`/`adjust(double)`/`onTick(boolean)` signatures in Task 2 match existing call sites in the unmodified mixins. Slider forward map (`setZoom*` in Task 4 Step 4) is the exact inverse of the init map (Task 4 Step 2).
```
