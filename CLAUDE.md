# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spyglass Only HUD is a client-side Fabric mod for Minecraft that hides all HUD elements when a player uses a spyglass, leaving only the spyglass overlay visible. The mod supports multiple Minecraft versions via separate git branches.

## Multi-Version Support

Each Minecraft version has its own branch. The build setup and Java source differ slightly per version.

| Branch | Minecraft | Fabric API | Java | Loom Plugin | `KeyMapping` category | ID class |
|---|---|---|---|---|---|---|
| `26.1` | 26.1-snapshot-6 | 0.143.4+26.1 | 25 | `net.fabricmc.fabric-loom` | `KeyMapping.Category.register()` | `Identifier` |
| `1.21.11` | 1.21.11 | 0.141.1+1.21.11 | 21 | `net.fabricmc.fabric-loom-remap` | `KeyMapping.Category.register()` | `Identifier` |
| `1.21.10` | 1.21.10 | 0.138.4+1.21.10 | 21 | `net.fabricmc.fabric-loom-remap` | `KeyMapping.Category.register()` | `ResourceLocation` |
| `1.21.9` | 1.21.9 | 0.134.0+1.21.9 | 21 | `net.fabricmc.fabric-loom-remap` | `KeyMapping.Category.register()` | `ResourceLocation` |
| `1.21.8` | 1.21.8 | 0.136.0+1.21.8 | 21 | `net.fabricmc.fabric-loom-remap` | String category | `ResourceLocation` |
| `1.21.7` | 1.21.7 | 0.129.0+1.21.7 | 21 | `net.fabricmc.fabric-loom-remap` | String category | `ResourceLocation` |
| `1.21.6` | 1.21.6 | 0.128.1+1.21.6 | 21 | `net.fabricmc.fabric-loom-remap` | String category | `ResourceLocation` |
| `1.21.5` | 1.21.5 | 0.122.0+1.21.5 | 21 | `net.fabricmc.fabric-loom-remap` | String category | `ResourceLocation` |
| `1.21.4` | 1.21.4 | 0.119.2+1.21.4 | 21 | `net.fabricmc.fabric-loom-remap` | String category | `ResourceLocation` |
| `1.21.3` | 1.21.3 | 0.108.0+1.21.3 | 21 | `net.fabricmc.fabric-loom-remap` | String category | `ResourceLocation` |
| `1.21.2` | 1.21.2 | 0.106.1+1.21.2 | 21 | `net.fabricmc.fabric-loom-remap` | String category | `ResourceLocation` |
| `1.21.1` | 1.21.1 | 0.116.6+1.21.1 | 21 | `net.fabricmc.fabric-loom-remap` | String category | `ResourceLocation` |
| `1.21` | 1.21 | 0.100.1+1.21 | 21 | `net.fabricmc.fabric-loom-remap` | String category | `ResourceLocation` |

Key API differences across versions:
- **`KeyMapping.Category`** was added in 1.21.9. Versions 1.21–1.21.8 use a plain `String` for the category parameter.
- **`Identifier`** replaced `ResourceLocation` in 1.21.11. Versions 1.21–1.21.10 use `ResourceLocation`.
- **26.1+** is unobfuscated — no mappings needed, uses `net.fabricmc.fabric-loom` plugin with `implementation` dependencies.
- **1.21.x** is obfuscated — requires Mojang mappings via `loom.officialMojangMappings()`, uses `net.fabricmc.fabric-loom-remap` plugin with `modImplementation` dependencies.

## Build Commands

```bash
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build -Dorg.gradle.java.home="C:/Users/Reva1v/jdk25/jdk-25.0.2"
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain clean    # Clean build artifacts
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain modrinth  # Publish to Modrinth
```

JDK 25 is installed at `C:/Users/Reva1v/jdk25/jdk-25.0.2`. Pass `-Dorg.gradle.java.home` or set `org.gradle.java.home` in `gradle.properties` to use it.

There are no automated tests; manual testing is done via `runClient` by equipping and using a spyglass in-game.

## Architecture

The mod is mixin-based with a config screen and keybinding:

- **`SpyglassOnlyHudMod`** (`com.spyglasshud`) — Entry point implementing `ClientModInitializer`; loads config on init.
- **`InGameHudMixin`** (`com.spyglasshud.mixin`) — The core of the mod. Injects into `net.minecraft.client.gui.hud.InGameHud` (`Gui`) at the HEAD of each HUD rendering method with `cancellable=true`, cancelling rendering when the player is scoping. Covers: crosshair, hotbar, status bars, mount health, status effects, held item tooltip. Also has a `@ModifyArg` to adjust spyglass overlay scale.
- **`SpyglassKeyBinding`** (`com.spyglasshud`) — Registers a custom keybinding category and an "Open Settings" key (default: K). Implementation differs per version (see table above).
- **`SpyglassConfigScreen`** (`com.spyglasshud`) — Settings screen built on `OptionsSubScreen` with hide HUD toggle and overlay scale slider.
- **`SpyglassConfig`** (`com.spyglasshud`) — JSON-based config persistence.
- **`ModMenuIntegration`** (`com.spyglasshud`) — Mod Menu integration for opening the config screen.
- **`ClientTickMixin`** (`com.spyglasshud.mixin`) — Handles keybinding press in `Minecraft.tick()` to open config screen.
- **`KeyBindingMixin`** (`com.spyglasshud.mixin`) — Injects into `Options.<init>` to register custom keybinding into the key mappings array.

Mixin configuration is in `src/main/resources/spyglassonlyhud.mixins.json`. Mod metadata is in `src/main/resources/fabric.mod.json` (client environment only).

## Translations

Language files are in `src/main/resources/assets/spyglass-only-hud/lang/`:
- `en_us.json` — English
- `ru_ru.json` — Russian
- `uk_ua.json` — Ukrainian

The keybinding category translation key is `key.category.spyglass-only-hud.settings`.

**Important**: Fabric API (`fabric-resource-loader`) is required for language files to load from mod JARs.

## Versioning

When making any changes to the mod (bug fixes, new features, translation updates, etc.), **always bump the version** in `gradle.properties` (`mod_version`). Follow semantic versioning: patch for fixes/small changes (1.0.0 -> 1.0.1 -> 1.0.2), minor for new features (1.0.2 -> 1.1.0), major for breaking changes (1.1.0 -> 2.0.0).

## Porting to a New Minecraft Version

1. Create a new branch from the closest existing version branch.
2. Update `gradle.properties`: `minecraft_version`, `fabric_version`, `mod_version`.
3. Update `fabric.mod.json`: `"minecraft": "~X.XX.X"`.
4. Check if `SpyglassKeyBinding.java` needs changes (see API differences table above).
5. Build and fix any compilation errors.
6. Commit and push the new branch.

## Key Details

- **Mixin pattern**: All injections use `@Inject(at = @At("HEAD"), method = "...", cancellable = true)` with `ci.cancel()` to suppress rendering
- **Client-only**: No server-side components; entry point is `client` in fabric.mod.json
- **Modrinth publishing**: Configured via `com.modrinth.minotaur` plugin; set `modrinth_project_id` in `gradle.properties` and `MODRINTH_TOKEN` env var
