# Scroll Zoom Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add mouse wheel zoom (×2–×15 FOV-based) to the spyglass; zoom resets to vanilla ×10 each time the player stops scoping.

**Architecture:** A new `SpyglassZoom` class holds the transient zoom state. A `MouseHandlerMixin` intercepts scroll events while scoping and updates zoom instead of changing hotbar slots. A `GameRendererMixin` modifies the returned FOV value to reflect the current zoom level. `ClientTickMixin` detects when scoping stops and resets zoom.

**Tech Stack:** Java 25, Fabric Loom (unobfuscated), SpongePowered Mixin, Minecraft 26.1

**Branch:** `feature/scroll-zoom`

**Build command (desktop):**
```bash
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build -Dorg.gradle.java.home="C:/Users/Reva1v/jdk25/jdk-25.0.2"
```

---

## File Map

| Action | File | Responsibility |
|---|---|---|
| Create | `src/main/java/com/spyglasshud/SpyglassZoom.java` | Zoom state: current value, min/max, step, reset logic |
| Create | `src/main/java/com/spyglasshud/mixin/MouseHandlerMixin.java` | Intercept scroll while scoping |
| Create | `src/main/java/com/spyglasshud/mixin/GameRendererMixin.java` | Modify returned FOV while scoping |
| Modify | `src/main/java/com/spyglasshud/mixin/ClientTickMixin.java` | Call `SpyglassZoom.onTick()` each tick |
| Modify | `src/main/resources/spyglassonlyhud.mixins.json` | Register two new mixins |
| Modify | `gradle.properties` | Bump `mod_version` 1.0.2 → 1.0.3 |

---

## Task 1: Create `SpyglassZoom`

**Files:**
- Create: `src/main/java/com/spyglasshud/SpyglassZoom.java`

- [ ] **Step 1: Create the file**

```java
package com.spyglasshud;

public class SpyglassZoom {
    public static final double MIN_ZOOM = 2.0;
    public static final double MAX_ZOOM = 15.0;
    public static final double DEFAULT_ZOOM = 10.0;
    public static final double STEP = 1.0;

    private static double currentZoom = DEFAULT_ZOOM;
    private static boolean wasScoping = false;

    public static double get() {
        return currentZoom;
    }

    /**
     * Called by MouseHandlerMixin when the player scrolls while scoping.
     * delta > 0 = scroll up (zoom in), delta < 0 = scroll down (zoom out).
     */
    public static void adjust(double delta) {
        currentZoom = Math.clamp(currentZoom + Math.signum(delta) * STEP, MIN_ZOOM, MAX_ZOOM);
    }

    /**
     * Called every tick by ClientTickMixin.
     * Resets zoom to DEFAULT_ZOOM when the player stops scoping.
     */
    public static void onTick(boolean isScoping) {
        if (wasScoping && !isScoping) {
            currentZoom = DEFAULT_ZOOM;
        }
        wasScoping = isScoping;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/spyglasshud/SpyglassZoom.java
git commit -m "feat: add SpyglassZoom state class"
```

---

## Task 2: Create `MouseHandlerMixin`

**Files:**
- Create: `src/main/java/com/spyglasshud/mixin/MouseHandlerMixin.java`
- Modify: `src/main/resources/spyglassonlyhud.mixins.json`

- [ ] **Step 1: Create the mixin**

```java
package com.spyglasshud.mixin;

import com.spyglasshud.SpyglassZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double xDelta, double yDelta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.isScoping()) {
            SpyglassZoom.adjust(yDelta);
            ci.cancel();
        }
    }
}
```

- [ ] **Step 2: Register the mixin in `spyglassonlyhud.mixins.json`**

Current content:
```json
{
    "required": true,
    "minVersion": "0.8",
    "package": "com.spyglasshud.mixin",
    "compatibilityLevel": "JAVA_25",
    "client": [
        "ClientTickMixin",
        "InGameHudMixin",
        "KeyBindingMixin"
    ],
    "injectors": {
        "defaultRequire": 1
    }
}
```

New content (add `MouseHandlerMixin` and `GameRendererMixin` to the list):
```json
{
    "required": true,
    "minVersion": "0.8",
    "package": "com.spyglasshud.mixin",
    "compatibilityLevel": "JAVA_25",
    "client": [
        "ClientTickMixin",
        "GameRendererMixin",
        "InGameHudMixin",
        "KeyBindingMixin",
        "MouseHandlerMixin"
    ],
    "injectors": {
        "defaultRequire": 1
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/spyglasshud/mixin/MouseHandlerMixin.java
git add src/main/resources/spyglassonlyhud.mixins.json
git commit -m "feat: add MouseHandlerMixin to intercept scroll while scoping"
```

---

## Task 3: Create `GameRendererMixin`

**Files:**
- Create: `src/main/java/com/spyglasshud/mixin/GameRendererMixin.java`

The vanilla `GameRenderer.getFov()` already applies the spyglass zoom (multiplies FOV by ~0.1, i.e. ÷10). We intercept the return value and rescale it relative to ×10:

- At `SpyglassZoom.get() == 10.0`: multiplier = `10.0/10.0 = 1.0` → FOV unchanged (vanilla)
- At `SpyglassZoom.get() == 15.0`: multiplier = `10.0/15.0 ≈ 0.667` → smaller FOV = more zoom
- At `SpyglassZoom.get() == 2.0`: multiplier = `10.0/2.0 = 5.0` → larger FOV = less zoom

- [ ] **Step 1: Create the mixin**

```java
package com.spyglasshud.mixin;

import com.spyglasshud.SpyglassZoom;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void modifySpyglassFov(Camera camera, float partialTick, boolean useFovSetting,
                                    CallbackInfoReturnable<Float> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.isScoping()) {
            float fov = cir.getReturnValue();
            cir.setReturnValue(fov * (float) (10.0 / SpyglassZoom.get()));
        }
    }
}
```

> **Note:** `GameRendererMixin` was already added to `spyglassonlyhud.mixins.json` in Task 2. No further registration needed.

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/spyglasshud/mixin/GameRendererMixin.java
git commit -m "feat: add GameRendererMixin to apply dynamic FOV zoom"
```

---

## Task 4: Update `ClientTickMixin`

**Files:**
- Modify: `src/main/java/com/spyglasshud/mixin/ClientTickMixin.java`

- [ ] **Step 1: Add `SpyglassZoom.onTick()` call**

Current `onTick`:
```java
@Inject(method = "tick", at = @At("HEAD"))
private void onTick(CallbackInfo ci) {
    Minecraft client = (Minecraft) (Object) this;
    if (SpyglassKeyBinding.OPEN_CONFIG.consumeClick()) {
        if (client.screen instanceof SpyglassConfigScreen) {
            client.setScreen(null);
        } else if (client.screen == null) {
            client.setScreen(new SpyglassConfigScreen(null));
        }
    }
}
```

New `onTick` (add import `com.spyglasshud.SpyglassZoom` and two lines at the top of the method body):
```java
@Inject(method = "tick", at = @At("HEAD"))
private void onTick(CallbackInfo ci) {
    Minecraft client = (Minecraft) (Object) this;
    boolean isScoping = client.player != null && client.player.isScoping();
    SpyglassZoom.onTick(isScoping);
    if (SpyglassKeyBinding.OPEN_CONFIG.consumeClick()) {
        if (client.screen instanceof SpyglassConfigScreen) {
            client.setScreen(null);
        } else if (client.screen == null) {
            client.setScreen(new SpyglassConfigScreen(null));
        }
    }
}
```

Also add the import at the top of the file:
```java
import com.spyglasshud.SpyglassZoom;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/spyglasshud/mixin/ClientTickMixin.java
git commit -m "feat: reset zoom in ClientTickMixin when player stops scoping"
```

---

## Task 5: Bump version and build

**Files:**
- Modify: `gradle.properties`

- [ ] **Step 1: Bump `mod_version`**

In `gradle.properties`, change:
```
mod_version=1.0.2
```
to:
```
mod_version=1.0.3
```

- [ ] **Step 2: Build**

```bash
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build -Dorg.gradle.java.home="C:/Users/Reva1v/jdk25/jdk-25.0.2"
```

Expected: `BUILD SUCCESSFUL`. JAR output: `build/libs/spyglass-only-hud-1.0.3-26.1.jar`

If the build fails with a mixin error like `Cannot find method getFov`, the method name or signature differs in this snapshot. To find the correct name:
- Open the project in IntelliJ, navigate to `GameRenderer`, search for the method that returns a float FOV and has a `Camera` parameter
- Update the `method = "getFov"` value in `GameRendererMixin` accordingly and rebuild

If the build fails with `Cannot find method onScroll` for `MouseHandler`:
- Navigate to `MouseHandler` in IntelliJ, find the GLFW scroll callback method
- Update `method = "onScroll"` in `MouseHandlerMixin` accordingly

- [ ] **Step 3: Test in-game**

Run the client and test:
1. Equip a spyglass and right-click to scope
2. Scroll up — objects should appear closer (zoom in)
3. Scroll down — objects should appear farther (zoom out)
4. Zoom should stop at ×2 (scroll down limit) and ×15 (scroll up limit)
5. Stop scoping and re-scope — zoom should reset to vanilla ×10
6. Confirm hotbar slot does NOT change while scrolling in scope

- [ ] **Step 4: Commit**

```bash
git add gradle.properties
git commit -m "chore: bump version to 1.0.3"
```
