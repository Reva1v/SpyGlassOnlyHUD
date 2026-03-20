#!/usr/bin/env python3
"""
Mixin Conflict Analyzer for Spyglass Only HUD

Downloads top Fabric mods from Modrinth and analyzes their mixin configurations
to find potential conflicts with our mod's mixin targets.

Usage:
    python tools/mixin_conflict_analyzer.py [options]

Options:
    --game-version VERSION   Minecraft version to check (default: from gradle.properties)
    --limit N                Number of mods to check (default: 100)
    --output FILE            Output report file (default: stdout)
    --keep-jars              Don't delete downloaded JARs after analysis
    --jar-dir DIR            Directory for downloaded JARs (default: temp dir)
"""

import argparse
import json
import os
import re
import struct
import sys
import tempfile
import time
import urllib.request
import urllib.parse
import zipfile
from pathlib import Path
from collections import defaultdict

# --- Our mod's mixin targets ---
# Maps target class names (both unmapped and Mojang-mapped) to the methods we inject into.
# This lets us detect conflicts regardless of which mapping the other mod uses.
OUR_TARGETS = {
    # Gui / InGameHud — our main target
    "net/minecraft/client/gui/Gui": {
        "class_aliases": [
            "net/minecraft/client/gui/hud/InGameHud",  # Yarn mapping
            "net.minecraft.client.gui.Gui",
            "net.minecraft.client.gui.hud.InGameHud",
        ],
        "methods": [
            "renderCrosshair",
            "renderHotbarAndDecorations",
            "renderPlayerHealth",
            "renderVehicleHealth",
            "renderEffects",
            "renderSelectedItemName",
            "renderCameraOverlays",
            "renderSpyglassOverlay",
            # Yarn-mapped method names
            "renderCrosshairGuiGraphics",
        ],
    },
    # Minecraft — we inject into tick()
    "net/minecraft/client/Minecraft": {
        "class_aliases": [
            "net/minecraft/client/MinecraftClient",  # Yarn
            "net.minecraft.client.Minecraft",
            "net.minecraft.client.MinecraftClient",
        ],
        "methods": ["tick"],
    },
    # Options — we inject into <init>
    "net/minecraft/client/Options": {
        "class_aliases": [
            "net/minecraft/client/option/GameOptions",  # Yarn
            "net.minecraft.client.Options",
            "net.minecraft.client.option.GameOptions",
        ],
        "methods": ["<init>"],
    },
}


def build_class_lookup():
    """Build a reverse lookup: class_name -> canonical_name for all our targets."""
    lookup = {}
    for canonical, info in OUR_TARGETS.items():
        # Add the canonical name in both slash and dot forms
        lookup[canonical] = canonical
        lookup[canonical.replace("/", ".")] = canonical
        for alias in info["class_aliases"]:
            lookup[alias] = canonical
            lookup[alias.replace(".", "/")] = canonical
            lookup[alias.replace("/", ".")] = canonical
    return lookup


CLASS_LOOKUP = build_class_lookup()


def read_gradle_properties():
    """Read minecraft_version from gradle.properties."""
    props_path = Path(__file__).parent.parent / "gradle.properties"
    if not props_path.exists():
        return None
    with open(props_path, "r") as f:
        for line in f:
            line = line.strip()
            if line.startswith("minecraft_version"):
                return line.split("=", 1)[1].strip()
    return None


def modrinth_api(endpoint, params=None):
    """Make a request to the Modrinth API."""
    base = "https://api.modrinth.com/v2"
    url = f"{base}{endpoint}"
    if params:
        url += "?" + urllib.parse.urlencode(params)

    req = urllib.request.Request(url)
    req.add_header("User-Agent", "SpyglassOnlyHUD-MixinAnalyzer/1.0 (github.com/spyglass-only-hud)")

    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode())
    except Exception as e:
        print(f"  [!] API error for {url}: {e}", file=sys.stderr)
        return None


def search_mods(game_version, limit=100):
    """Search for top Fabric mods on Modrinth for the given game version."""
    print(f"[*] Searching Modrinth for top {limit} Fabric mods (Minecraft {game_version})...")

    facets = json.dumps([
        ["project_type:mod"],
        ["categories:fabric"],
        [f"versions:{game_version}"],
    ])

    results = []
    offset = 0
    per_page = min(limit, 100)

    while len(results) < limit:
        data = modrinth_api("/search", {
            "facets": facets,
            "limit": per_page,
            "offset": offset,
            "index": "downloads",
        })
        if not data or not data.get("hits"):
            break

        results.extend(data["hits"])
        offset += per_page

        if len(data["hits"]) < per_page:
            break

        # Rate limit
        time.sleep(0.3)

    results = results[:limit]
    print(f"[*] Found {len(results)} mods")
    return results


def get_mod_version(project_id, game_version):
    """Get the latest version of a mod for the given game version."""
    data = modrinth_api(f"/project/{project_id}/version", {
        "game_versions": json.dumps([game_version]),
        "loaders": json.dumps(["fabric"]),
    })
    if not data or len(data) == 0:
        return None
    # Return the first (latest) version
    return data[0]


def download_jar(url, dest_path):
    """Download a JAR file."""
    req = urllib.request.Request(url)
    req.add_header("User-Agent", "SpyglassOnlyHUD-MixinAnalyzer/1.0")
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            with open(dest_path, "wb") as f:
                f.write(resp.read())
        return True
    except Exception as e:
        print(f"  [!] Download failed: {e}", file=sys.stderr)
        return False


def find_mixin_configs(jar_path):
    """Find all mixin config JSON files in a JAR."""
    configs = []
    try:
        with zipfile.ZipFile(jar_path, "r") as zf:
            # Look for fabric.mod.json to find mixin config references
            mixin_files = set()
            if "fabric.mod.json" in zf.namelist():
                try:
                    fmj = json.loads(zf.read("fabric.mod.json"))
                    for m in fmj.get("mixins", []):
                        if isinstance(m, str):
                            mixin_files.add(m)
                        elif isinstance(m, dict):
                            mixin_files.add(m.get("config", ""))
                except (json.JSONDecodeError, KeyError):
                    pass

            # Also scan for any *.mixins.json files
            for name in zf.namelist():
                if name.endswith(".mixins.json") or name.endswith(".mixin.json"):
                    mixin_files.add(name)

            # Parse each mixin config
            for mixin_file in mixin_files:
                if not mixin_file or mixin_file not in zf.namelist():
                    continue
                try:
                    config = json.loads(zf.read(mixin_file))
                    configs.append((mixin_file, config))
                except (json.JSONDecodeError, KeyError):
                    pass

    except (zipfile.BadZipFile, Exception) as e:
        print(f"  [!] Failed to read JAR: {e}", file=sys.stderr)

    return configs


def scan_class_for_targets(class_bytes):
    """
    Scan a .class file's constant pool for references to our target classes.
    Returns set of canonical class names found.
    """
    found = set()
    # Simple approach: scan for UTF-8 strings in the constant pool
    # that match our target class names
    try:
        text = class_bytes.decode("utf-8", errors="ignore")
        for class_name in CLASS_LOOKUP:
            if class_name in text:
                found.add(CLASS_LOOKUP[class_name])
    except Exception:
        pass
    return found


def analyze_jar(jar_path):
    """
    Analyze a mod JAR for mixin conflicts.
    Returns a list of findings: (mixin_config, mixin_class, target_class)
    """
    findings = []
    configs = find_mixin_configs(jar_path)

    if not configs:
        return findings

    try:
        with zipfile.ZipFile(jar_path, "r") as zf:
            for config_name, config in configs:
                package = config.get("package", "").replace(".", "/")

                # Collect all mixin class names
                mixin_classes = []
                for key in ("mixins", "client", "server"):
                    mixin_classes.extend(config.get(key, []))

                for mixin_class in mixin_classes:
                    # Build the class file path
                    class_path = f"{package}/{mixin_class.replace('.', '/')}.class"

                    if class_path not in zf.namelist():
                        # Try without package
                        class_path = f"{mixin_class.replace('.', '/')}.class"
                        if class_path not in zf.namelist():
                            continue

                    class_bytes = zf.read(class_path)
                    targets = scan_class_for_targets(class_bytes)

                    for target in targets:
                        findings.append({
                            "mixin_config": config_name,
                            "mixin_class": mixin_class,
                            "target_class": target,
                        })

    except (zipfile.BadZipFile, Exception) as e:
        print(f"  [!] Failed to analyze JAR: {e}", file=sys.stderr)

    return findings


def format_report(conflicts, game_version):
    """Format the conflict report."""
    lines = []
    lines.append("=" * 70)
    lines.append(f"  Mixin Conflict Analysis Report — Minecraft {game_version}")
    lines.append("=" * 70)
    lines.append("")

    if not conflicts:
        lines.append("No potential conflicts found!")
        return "\n".join(lines)

    # Group by target class
    by_target = defaultdict(list)
    for c in conflicts:
        by_target[c["target_class"]].append(c)

    # Summary
    unique_mods = set(c["mod_name"] for c in conflicts)
    lines.append(f"Found {len(conflicts)} potential conflicts across {len(unique_mods)} mods")
    lines.append(f"Targeting {len(by_target)} of our mixin target classes")
    lines.append("")

    # Critical conflicts: same class as InGameHud/Gui
    gui_key = "net/minecraft/client/gui/Gui"
    if gui_key in by_target:
        lines.append("-" * 70)
        lines.append("!! HIGH PRIORITY — Mods targeting Gui / InGameHud:")
        lines.append("-" * 70)
        for c in sorted(by_target[gui_key], key=lambda x: x["mod_downloads"], reverse=True):
            lines.append(f"  [{c['mod_downloads']:>10,} downloads] {c['mod_name']}")
            lines.append(f"    Slug:   {c['mod_slug']}")
            lines.append(f"    Mixin:  {c['mixin_class']}")
            lines.append(f"    Config: {c['mixin_config']}")
            lines.append(f"    URL:    https://modrinth.com/mod/{c['mod_slug']}")
            lines.append("")

    # Other targets
    for target, items in sorted(by_target.items()):
        if target == gui_key:
            continue
        lines.append("-" * 70)
        lines.append(f"Mods targeting {target}:")
        lines.append("-" * 70)
        for c in sorted(items, key=lambda x: x["mod_downloads"], reverse=True):
            lines.append(f"  [{c['mod_downloads']:>10,} downloads] {c['mod_name']}")
            lines.append(f"    Slug:   {c['mod_slug']}")
            lines.append(f"    Mixin:  {c['mixin_class']}")
            lines.append(f"    URL:    https://modrinth.com/mod/{c['mod_slug']}")
            lines.append("")

    return "\n".join(lines)


def safe_print(*args, **kwargs):
    """Print with fallback for encoding errors (e.g. emoji in mod names on Windows)."""
    try:
        print(*args, **kwargs)
    except UnicodeEncodeError:
        text = " ".join(str(a) for a in args)
        text = text.encode("ascii", errors="replace").decode("ascii")
        print(text, **kwargs)


def main():
    parser = argparse.ArgumentParser(description="Analyze Fabric mods for mixin conflicts")
    parser.add_argument("--game-version", default=None, help="Minecraft version (default: from gradle.properties)")
    parser.add_argument("--limit", type=int, default=100, help="Number of mods to check (default: 100)")
    parser.add_argument("--output", default=None, help="Output report file (default: stdout)")
    parser.add_argument("--keep-jars", action="store_true", help="Keep downloaded JARs")
    parser.add_argument("--jar-dir", default=None, help="Directory for downloaded JARs")
    args = parser.parse_args()

    game_version = args.game_version or read_gradle_properties()
    if not game_version:
        print("Error: Could not determine game version. Use --game-version.", file=sys.stderr)
        sys.exit(1)

    # Set up JAR directory
    if args.jar_dir:
        jar_dir = Path(args.jar_dir)
        jar_dir.mkdir(parents=True, exist_ok=True)
        cleanup_dir = False
    else:
        jar_dir = Path(tempfile.mkdtemp(prefix="mixin_analysis_"))
        cleanup_dir = not args.keep_jars

    print(f"[*] Game version: {game_version}")
    print(f"[*] JAR directory: {jar_dir}")
    print()

    # Search for mods
    mods = search_mods(game_version, args.limit)
    if not mods:
        print("No mods found! Try a different game version.", file=sys.stderr)
        sys.exit(1)

    all_conflicts = []
    analyzed = 0
    skipped = 0

    for i, mod in enumerate(mods):
        slug = mod.get("slug", "unknown")
        title = mod.get("title", slug)
        project_id = mod["project_id"]
        downloads = mod.get("downloads", 0)

        safe_print(f"[{i+1}/{len(mods)}] {title} ({slug})...", end=" ", flush=True)

        # Get version info
        version_info = get_mod_version(project_id, game_version)
        if not version_info:
            print("no compatible version, skipping")
            skipped += 1
            time.sleep(0.2)
            continue

        # Find the primary JAR file
        jar_file = None
        for f in version_info.get("files", []):
            if f.get("primary", False) and f["filename"].endswith(".jar"):
                jar_file = f
                break
        if not jar_file:
            for f in version_info.get("files", []):
                if f["filename"].endswith(".jar"):
                    jar_file = f
                    break

        if not jar_file:
            print("no JAR file, skipping")
            skipped += 1
            continue

        # Download
        jar_path = jar_dir / jar_file["filename"]
        if not jar_path.exists():
            if not download_jar(jar_file["url"], jar_path):
                skipped += 1
                continue

        # Analyze
        findings = analyze_jar(jar_path)
        analyzed += 1

        if findings:
            print(f"CONFLICT ({len(findings)} targets)")
            for f in findings:
                f["mod_name"] = title
                f["mod_slug"] = slug
                f["mod_downloads"] = downloads
                all_conflicts.append(f)
        else:
            print("OK")

        # Clean up JAR if not keeping
        if not args.keep_jars and not args.jar_dir:
            try:
                jar_path.unlink()
            except OSError:
                pass

        # Rate limit
        time.sleep(0.3)

    # Clean up temp directory
    if cleanup_dir:
        try:
            jar_dir.rmdir()
        except OSError:
            pass

    print()
    print(f"[*] Analyzed {analyzed} mods, skipped {skipped}")
    print()

    # Generate report
    report = format_report(all_conflicts, game_version)

    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(report)
        print(f"[*] Report saved to {args.output}")
    else:
        print(report)


if __name__ == "__main__":
    main()
