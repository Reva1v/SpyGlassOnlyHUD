# CLAUDE.md

Spyglass Only HUD — client-side Fabric mod that hides all HUD elements while using a spyglass. Multi-version: each Minecraft version has its own git branch.

## Multi-Version Support

Each Minecraft version lives on its own git branch. The full version matrix (branch → Minecraft / Fabric API / Java / API style) and all per-version API differences live in **[docs/MULTI_VERSION.md](docs/MULTI_VERSION.md)** — keep that file in sync when porting to a new version. Latest: `26.3-snapshot` (26.3-snapshot-1, loom `1.17-SNAPSHOT`, Gradle `9.5.0`).

## Build Commands

```bash
./gradlew build -Dorg.gradle.java.home="<JDK_PATH>"
./gradlew clean
./gradlew modrinth   # Publish to Modrinth (requires MODRINTH_TOKEN env var)
```

JDK paths (Reva1v desktop):
- **JDK 25** (`26.x` branches): `C:/Program Files/Amazon Corretto/jdk25.0.2_10`
- **JDK 21** (`1.21.x` branches): `C:/Users/Reva1v/AppData/Local/Programs/IntelliJ IDEA/jbr`
- **JDK 17** (`1.20.1` branch): any JDK 17 install

Testing: no automated tests — use `./gradlew runClient` and test in-game with a spyglass.

## JAR Naming

Output JARs: `spyglass-only-hud-{mod_version}-{mc_version}.jar` (e.g. `spyglass-only-hud-1.0.3-26.1.jar`).
Achieved via `archiveVersion = "${mod_version}-${minecraft_version}"` in `build.gradle` (already set up on all branches).

## Architecture

Mixin-based, client-only:

- **`SpyglassOnlyHudMod`** — `ClientModInitializer` entry point; loads config.
- **`InGameHudMixin`** — Cancels rendering of crosshair, hotbar, health/hunger/armor/XP, mount health, status effects, held item tooltip while scoping. Also `@ModifyArg` for overlay scale. Target class: `Gui` on 26.1.x and earlier; `Hud` on 26.2-snapshot+.
- **`SpyglassZoom`** — Scroll zoom state: range ×1–×50, default ×10, step 3.0/scroll; resets on unscope.
- **`GameRendererMixin`** — Applies `fov * (10.0 / currentZoom)`. Target differs: `26.1` uses `Camera.calculateFov(float)` RETURN; `1.21.x` uses `GameRenderer.getFov(Camera, float, boolean)` RETURN. Note: Loom doesn't catch wrong target at compile time — only fails at runtime.
- **`MouseHandlerMixin`** — Intercepts `MouseHandler.onScroll()`; while scoping cancels event and calls `SpyglassZoom.adjust(yDelta)`.
- **`ClientTickMixin`** — Calls `SpyglassZoom.onTick()` each tick; handles K keybinding to open config screen.
- **`SpyglassKeyBinding`** — Defines the "Open Settings" key (default: K) and keybinding category. Registered at runtime via Fabric's `KeyMappingHelper.registerKeyMapping()` in `SpyglassOnlyHudMod.onInitializeClient()`. **Do NOT register keybindings via a raw mixin into `Options.<init>`** — that runs after `Options.load()`, so saved/unbound key values never apply and the key resets to default every launch (fixed in 1.2.1). The Fabric API registers before `Options` is constructed, and its `OptionsMixin.loadHook` (`@At("HEAD")` of `Options.load()`) merges modded keymappings before saved values are read.
- **`SpyglassConfigScreen`** — Settings screen: HUD hide toggle + overlay scale slider.
- **`SpyglassConfig`** — JSON config persistence.
- **`ModMenuIntegration`** — Mod Menu hook.

Config: `spyglassonlyhud.mixins.json` · Metadata: `fabric.mod.json` · Langs: `assets/spyglass-only-hud/lang/` (en_us, ru_ru, uk_ua).

## Versioning

Always bump `mod_version` in `gradle.properties`. Semver: patch = fixes, minor = features, major = breaking.

## Porting to a New Minecraft Version

1. Branch from the closest existing version.
2. Update `gradle.properties`: `minecraft_version`, `loader_version`, `fabric_version`, `mod_version`.
3. Update `build.gradle`: loom version if needed (loom `1.16-SNAPSHOT` requires Gradle `9.4.0+`; loom `1.17-SNAPSHOT` requires Gradle `9.5.0+` → also update `gradle-wrapper.properties`).
4. Update `fabric.mod.json`: `"minecraft"` constraint. For snapshot versions use a range (e.g. `>=26.3-alpha.1 <26.4-`) since the game may report a different version string at runtime than the Maven artifact name.
5. Check `SpyglassKeyBinding.java` for API differences (see [docs/MULTI_VERSION.md](docs/MULTI_VERSION.md)).
6. Check `InGameHudMixin.java`: if `extract*` methods moved to a different class (e.g. `Gui` → `Hud` in 26.2), update `@Mixin` target and method parameter signatures.
7. Build and fix compilation errors, then commit and push.
8. Add the new branch's row to **[docs/MULTI_VERSION.md](docs/MULTI_VERSION.md)**.
