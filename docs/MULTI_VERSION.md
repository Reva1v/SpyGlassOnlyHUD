# Multi-Version Support

Spyglass Only HUD targets many Minecraft versions; **each version lives on its own git branch**. This file is the authoritative version matrix and the list of per-version API differences.

| Branch | Minecraft | Fabric API | Java | `KeyMapping` category | ID class |
|---|---|---|---|---|---|
| `26.3-snapshot` | 26.3-snapshot-1 | 0.153.1+26.3 | 25 | `KeyMapping.Category.register()` | `Identifier` |
| `26.2` | 26.2-pre-2 | 0.150.1+26.2 | 25 | `KeyMapping.Category.register()` | `Identifier` |
| `26.2-snapshot` | 26.2-snapshot-2 | 0.145.5+26.2 | 25 | `KeyMapping.Category.register()` | `Identifier` |
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
| `1.20.1` | 1.20.1 | 0.92.3+1.20.1 | 17 | String category | `ResourceLocation` |

## Key API differences

- **`KeyMapping.Category`** added in 1.21.9; older versions use a plain `String`.
- **`Identifier`** replaced `ResourceLocation` in 1.21.11.
- **Keybinding registration** (all branches): register via Fabric's keymapping API in `SpyglassOnlyHudMod.onInitializeClient()`, **not** via a raw mixin into `Options.<init>`. The class is `KeyMappingHelper.registerKeyMapping` (package `net.fabricmc.fabric.api.client.keymapping.v1`) on 26.x, and `KeyBindingHelper.registerKeyBinding` (package `net.fabricmc.fabric.api.client.keybinding.v1`) on older branches. Registering via an `Options.<init>` `@At("RETURN")` mixin runs after `Options.load()` and breaks unbind/rebind persistence — see `.wolf/buglog.json` bug-009.
- **26.1+**: unobfuscated, `net.fabricmc.fabric-loom`, `implementation` deps.
- **1.21.x**: obfuscated, `net.fabricmc.fabric-loom-remap`, Mojang mappings (`loom.officialMojangMappings()`), `modImplementation` deps.
- **26.2-snapshot+**: `InGameHudMixin` targets `Hud` (not `Gui`) — all `extract*` methods moved there. Method signatures now include `GuiGraphicsExtractor` (and `DeltaTracker` where applicable) as parameters before `CallbackInfo`. `@ModifyArg` target updated to `Hud;extractSpyglassOverlay`. Requires loom `1.16-SNAPSHOT` and Gradle `9.4.0+`. `fabric.mod.json` minecraft constraint uses range `>=26.2-alpha.1 <26.3-` (game reports itself as `26.2-alpha.X`).
- **26.3-snapshot+**: requires loom `1.17-SNAPSHOT` and Gradle `9.5.0+` (loom 1.17 rejects Gradle 9.4 — `No matching variant ... api-version 9.5.0`). `fabric.mod.json` minecraft constraint uses range `>=26.3-alpha.1 <26.4-` (game reports itself as `26.3-alpha.X`). No source changes were needed vs 26.2 — the `Hud`/`extract*` API is unchanged.
