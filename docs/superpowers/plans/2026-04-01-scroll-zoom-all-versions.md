# Plan: Port Scroll Zoom to All Versions + Modrinth Release

**Date:** 2026-04-01  
**Branch:** `feature/scroll-zoom` (to be merged into `26.1`, then ported to all `1.21.x`)

## Context

Scroll zoom is fully implemented on `feature/scroll-zoom` (branched from `26.1`):
- `SpyglassZoom.java` — zoom state (×1–×50, default ×10, step 3.0)
- `GameRendererMixin.java` — intercepts `Camera.calculateFov(float)` RETURN
- `MouseHandlerMixin.java` — intercepts `MouseHandler.onScroll()` HEAD
- `ClientTickMixin.java` — modified to call `SpyglassZoom.onTick()`
- `spyglassonlyhud.mixins.json` — updated with new mixins

**Versioning strategy:** All branches get the same `mod_version = 1.1.0`.  
JAR name already includes MC version: `spyglass-only-hud-1.1.0-26.1.jar`, `spyglass-only-hud-1.1.0-1.21.1.jar`, etc.  
This makes it immediately clear that all releases are the same feature set, just for different Minecraft versions.

| Branch | Current version | New version |
|--------|----------------|-------------|
| `26.1` | 1.0.2 | **1.1.0** |
| `1.21.11` | 1.1.1 | **1.1.0** |
| `1.21.10` | 1.2.1 | **1.1.0** |
| `1.21.9` | 1.3.1 | **1.1.0** |
| `1.21.8` | 1.4.1 | **1.1.0** |
| `1.21.7` | 1.5.1 | **1.1.0** |
| `1.21.6` | 1.6.1 | **1.1.0** |
| `1.21.5` | 1.7.1 | **1.1.0** |
| `1.21.4` | 1.8.1 | **1.1.0** |
| `1.21.3` | 1.9.1 | **1.1.0** |
| `1.21.2` | 1.10.1 | **1.1.0** |
| `1.21.1` | 1.11.1 | **1.1.0** |
| `1.21` | 1.12.1 | **1.1.0** |

---

## Phase 1: Merge scroll zoom into `26.1`

1. Checkout `26.1`
2. Merge `feature/scroll-zoom` into `26.1` (fast-forward or merge commit)
3. Bump `mod_version` in `gradle.properties`: `1.0.3` → `1.1.0` (scroll zoom is a feature, not a patch)
4. Build: `./gradlew build -Dorg.gradle.java.home="C:/Users/Reva1v/jdk25/jdk-25.0.2"`
5. Verify JAR: `build/libs/spyglass-only-hud-1.1.0-26.1.jar`
6. Commit: `feat: add scroll zoom (×1–×50, default ×10, step 3.0)`

---

## Phase 2: Port to each `1.21.x` branch

### Files to add/modify per branch

**Add (copy from `26.1` — identical across all versions):**
- `src/main/java/com/spyglasshud/SpyglassZoom.java`
- `src/main/java/com/spyglasshud/mixin/MouseHandlerMixin.java`

**Add (may need adjustment — see FOV note below):**
- `src/main/java/com/spyglasshud/mixin/GameRendererMixin.java`

**Modify:**
- `src/main/java/com/spyglasshud/mixin/ClientTickMixin.java` — add `SpyglassZoom.onTick(isScoping)` call
- `src/main/resources/spyglassonlyhud.mixins.json` — add `GameRendererMixin` and `MouseHandlerMixin` to client array
- `gradle.properties` — bump `mod_version`

### FOV injection point (important!)

In `26.1`, FOV is computed in `Camera.calculateFov(float partialTick)`.  
In `1.21.x`, the same logic may live in `GameRenderer.getFov(Camera, float, boolean)` instead.

**Rule:** Try `Camera.calculateFov` first. If the build fails with "could not find target", switch to:
```java
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void modifySpyglassFov(Camera camera, float partialTick, boolean optical,
                                    CallbackInfoReturnable<Float> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.isScoping() && client.screen == null) {
            cir.setReturnValue(cir.getReturnValue() * (float)(10.0 / SpyglassZoom.get(partialTick)));
        }
    }
}
```

### Per-branch steps

For each branch in order: `1.21.11` → `1.21.10` → `1.21.9` → `1.21.8` → `1.21.7` → `1.21.6` → `1.21.5` → `1.21.4` → `1.21.3` → `1.21.2` → `1.21.1` → `1.21`:

```bash
git checkout <branch>
# 1. Copy SpyglassZoom.java and MouseHandlerMixin.java from 26.1
git show 26.1:src/main/java/com/spyglasshud/SpyglassZoom.java > src/main/java/com/spyglasshud/SpyglassZoom.java
git show 26.1:src/main/java/com/spyglasshud/mixin/MouseHandlerMixin.java > src/main/java/com/spyglasshud/mixin/MouseHandlerMixin.java
git show 26.1:src/main/java/com/spyglasshud/mixin/GameRendererMixin.java > src/main/java/com/spyglasshud/mixin/GameRendererMixin.java

# 2. Patch ClientTickMixin.java — add zoom tick reset (3 lines total to add):
#    import com.spyglasshud.SpyglassZoom;
#    boolean isScoping = client.player != null && client.player.isScoping();
#    SpyglassZoom.onTick(isScoping);

# 3. Update mixins.json — add "GameRendererMixin" and "MouseHandlerMixin" to client array

# 4. Bump mod_version in gradle.properties

# 5. Build
./gradlew build -Dorg.gradle.java.home="C:/Users/Reva1v/AppData/Local/Programs/IntelliJ IDEA/jbr"

# 6. Commit
git add -A && git commit -m "feat: add scroll zoom (×1–×50, default ×10, step 3.0)"
```

> **Note on `compatibilityLevel`:** Each `1.21.x` mixins.json uses `JAVA_21` — keep it, do not change to JAVA_25.

---

## Phase 3: Update Modrinth description

Add to the **Features** section on the Modrinth project page:

```
- **Scroll zoom** — Scroll the mouse wheel while using a spyglass to zoom in/out (×1–×50). Resets to default (×10) when you put the spyglass away.
```

Update via the Modrinth web interface (Edit project → Description).

No code change needed; the description is stored on Modrinth, not in the repo.

---

## Phase 4: Publish all updated versions to Modrinth

Each branch needs a `MODRINTH_TOKEN` env var set. Run per branch:

```bash
git checkout <branch>
MODRINTH_TOKEN=<token> ./gradlew modrinth -Dorg.gradle.java.home="<jdk_path>"
```

Or set `MODRINTH_TOKEN` globally in the shell session first:
```bash
export MODRINTH_TOKEN=<your_token>
```

Publish order (newest to oldest is fine):
`26.1`, `1.21.11`, `1.21.10`, `1.21.9`, `1.21.8`, `1.21.7`, `1.21.6`, `1.21.5`, `1.21.4`, `1.21.3`, `1.21.2`, `1.21.1`, `1.21`

---

## Checklist

- [ ] Phase 1: Merge + build `26.1` → `spyglass-only-hud-1.1.0-26.1.jar`
- [ ] Phase 2: Port to `1.21.11` → `spyglass-only-hud-1.1.0-1.21.11.jar`
- [ ] Phase 2: Port to `1.21.10` → `spyglass-only-hud-1.1.0-1.21.10.jar`
- [ ] Phase 2: Port to `1.21.9` → `spyglass-only-hud-1.1.0-1.21.9.jar`
- [ ] Phase 2: Port to `1.21.8` → `spyglass-only-hud-1.1.0-1.21.8.jar`
- [ ] Phase 2: Port to `1.21.7` → `spyglass-only-hud-1.1.0-1.21.7.jar`
- [ ] Phase 2: Port to `1.21.6` → `spyglass-only-hud-1.1.0-1.21.6.jar`
- [ ] Phase 2: Port to `1.21.5` → `spyglass-only-hud-1.1.0-1.21.5.jar`
- [ ] Phase 2: Port to `1.21.4` → `spyglass-only-hud-1.1.0-1.21.4.jar`
- [ ] Phase 2: Port to `1.21.3` → `spyglass-only-hud-1.1.0-1.21.3.jar`
- [ ] Phase 2: Port to `1.21.2` → `spyglass-only-hud-1.1.0-1.21.2.jar`
- [ ] Phase 2: Port to `1.21.1` → `spyglass-only-hud-1.1.0-1.21.1.jar`
- [ ] Phase 2: Port to `1.21` → `spyglass-only-hud-1.1.0-1.21.jar`
- [ ] Phase 3: Update Modrinth description
- [ ] Phase 4: Publish all 13 versions to Modrinth
