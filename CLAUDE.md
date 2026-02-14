# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spyglass Only HUD is a client-side Fabric mod for Minecraft that hides all HUD elements when a player uses a spyglass. It targets Minecraft snapshot 26.1-snapshot-6 with Fabric Loader and uses Java 21.

## Build Commands

```bash
./gradlew build          # Build mod JAR (output: build/libs/spyglass-only-hud-1.0.0.jar)
./gradlew runClient      # Launch Minecraft client for testing
./gradlew clean          # Clean build artifacts
```

There are no automated tests; manual testing is done via `./gradlew runClient` by equipping and using a spyglass in-game.

## Architecture

The mod is entirely mixin-based with no runtime initialization logic:

- **`SpyglassOnlyHudMod`** (`com.spyglasshud`) — Entry point implementing `ClientModInitializer`; initialization is a no-op since all behavior is in mixins.
- **`InGameHudMixin`** (`com.spyglasshud.mixin`) — The core of the mod. Injects into `net.minecraft.client.gui.hud.InGameHud` at the HEAD of each HUD rendering method with `cancellable=true`, cancelling rendering when `isUsingSpyglass()` returns true. Covers: crosshair, hotbar, status bars, XP bar/level, mount health, status effects, held item tooltip.

Mixin configuration is in `src/main/resources/spyglassonlyhud.mixins.json`. Mod metadata is in `src/main/resources/fabric.mod.json` (client environment only).

## Key Details

- **Mappings**: Yarn mappings — use Yarn-mapped method/class names (not Mojang or intermediary)
- **Mixin pattern**: All injections use `@Inject(at = @At("HEAD"), method = "...", cancellable = true)` with `ci.cancel()` to suppress rendering
- **Client-only**: No server-side components; entry point is `client` in fabric.mod.json
