# CLAUDE.md

Spyglass Only HUD — client-side Fabric mod that hides all HUD elements while using a spyglass. Multi-version: each Minecraft version has its own git branch.

## Multi-Version Support

| Branch | Minecraft | Fabric API | Java | `KeyMapping` category | ID class |
|---|---|---|---|---|---|
| `26.2-snapshot` | 26.2-snapshot-1 | 0.145.5+26.2 | 25 | `KeyMapping.Category.register()` | `Identifier` |
| `26.1.2` | 26.1.2 | 0.145.4+26.1.2 | 25 | `KeyMapping.Category.register()` | `Identifier` |
| `26.1` | 26.1 | 0.145.0+26.1 | 25 | `KeyMapping.Category.register()` | `Identifier` |
| `1.21.11` | 1.21.11 | 0.141.1+1.21.11 | 21 | `KeyMapping.Category.register()` | `Identifier` |
| `1.21.10` | 1.21.10 | 0.138.4+1.21.10 | 21 | `KeyMapping.Category.register()` | `ResourceLocation` |
| `1.21.9` | 1.21.9 | 0.134.0+1.21.9 | 21 | `KeyMapping.Category.register()` | `ResourceLocation` |
| `1.21.8` | 1.21.8 | 0.136.0+1.21.8 | 21 | String category | `ResourceLocation` |
| `1.21.7` | 1.21.7 | 0.129.0+1.21.7 | 21 | String category | `ResourceLocation` |
| `1.21.6` | 1.21.6 | 0.128.1+1.21.6 | 21 | String category | `ResourceLocation` |
| `1.21.5` | 1.21.5 | 0.122.0+1.21.5 | 21 | String category | `ResourceLocation` |
| `1.21.4` | 1.21.4 | 0.119.2+1.21.4 | 21 | String category | `ResourceLocation` |
| `1.21.3` | 1.21.3 | 0.108.0+1.21.3 | 21 | String category | `ResourceLocation` |
| `1.21.2` | 1.21.2 | 0.106.1+1.21.2 | 21 | String category | `ResourceLocation` |
| `1.21.1` | 1.21.1 | 0.116.6+1.21.1 | 21 | String category | `ResourceLocation` |
| `1.21` | 1.21 | 0.100.1+1.21 | 21 | String category | `ResourceLocation` |

Key API differences:
- **`KeyMapping.Category`** added in 1.21.9; older versions use a plain `String`.
- **`Identifier`** replaced `ResourceLocation` in 1.21.11.
- **26.1+**: unobfuscated, `net.fabricmc.fabric-loom`, `implementation` deps.
- **1.21.x**: obfuscated, `net.fabricmc.fabric-loom-remap`, Mojang mappings (`loom.officialMojangMappings()`), `modImplementation` deps.

## Build Commands

```bash
./gradlew build -Dorg.gradle.java.home="<JDK_PATH>"
./gradlew clean
./gradlew modrinth   # Publish to Modrinth (requires MODRINTH_TOKEN env var)
```

JDK paths (Reva1v desktop):
- **JDK 25** (`26.1` branch): `C:/Users/Reva1v/jdk25/jdk-25.0.2`
- **JDK 21** (`1.21.x` branches): `C:/Users/Reva1v/AppData/Local/Programs/IntelliJ IDEA/jbr`

Testing: no automated tests — use `./gradlew runClient` and test in-game with a spyglass.

## JAR Naming

Output JARs: `spyglass-only-hud-{mod_version}-{mc_version}.jar` (e.g. `spyglass-only-hud-1.0.3-26.1.jar`).
Achieved via `archiveVersion = "${mod_version}-${minecraft_version}"` in `build.gradle` (already set up on all branches).

## Architecture

Mixin-based, client-only:

- **`SpyglassOnlyHudMod`** — `ClientModInitializer` entry point; loads config.
- **`InGameHudMixin`** — Cancels rendering of crosshair, hotbar, health/hunger/armor/XP, mount health, status effects, held item tooltip while scoping. Also `@ModifyArg` for overlay scale.
- **`SpyglassZoom`** — Scroll zoom state: range ×1–×50, default ×10, step 3.0/scroll; resets on unscope.
- **`GameRendererMixin`** — Applies `fov * (10.0 / currentZoom)`. Target differs: `26.1` uses `Camera.calculateFov(float)` RETURN; `1.21.x` uses `GameRenderer.getFov(Camera, float, boolean)` RETURN. Note: Loom doesn't catch wrong target at compile time — only fails at runtime.
- **`MouseHandlerMixin`** — Intercepts `MouseHandler.onScroll()`; while scoping cancels event and calls `SpyglassZoom.adjust(yDelta)`.
- **`ClientTickMixin`** — Calls `SpyglassZoom.onTick()` each tick; handles K keybinding to open config screen.
- **`KeyBindingMixin`** — Injects into `Options.<init>` to register the custom keybinding.
- **`SpyglassKeyBinding`** — Registers "Open Settings" key (default: K) and keybinding category.
- **`SpyglassConfigScreen`** — Settings screen: HUD hide toggle + overlay scale slider.
- **`SpyglassConfig`** — JSON config persistence.
- **`ModMenuIntegration`** — Mod Menu hook.

Config: `spyglassonlyhud.mixins.json` · Metadata: `fabric.mod.json` · Langs: `assets/spyglass-only-hud/lang/` (en_us, ru_ru, uk_ua).

## Versioning

Always bump `mod_version` in `gradle.properties`. Semver: patch = fixes, minor = features, major = breaking.

## Porting to a New Minecraft Version

1. Branch from the closest existing version.
2. Update `gradle.properties`: `minecraft_version`, `fabric_version`, `mod_version`.
3. Update `fabric.mod.json`: `"minecraft": "~X.XX.X"`.
4. Check `SpyglassKeyBinding.java` for API differences (see table).
5. Build and fix compilation errors, then commit and push.
