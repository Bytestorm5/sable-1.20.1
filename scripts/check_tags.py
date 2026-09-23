#!/usr/bin/env python3
"""
Lists (or, with --fix, makes optional) tag entries that don't exist on Minecraft 1.20.1. A required entry that doesn't
resolve makes the whole tag fail to load, so 1.21-only vanilla ids must be optional on 1.20.1.

    python3 scripts/check_tags.py <1.20.1 vanilla sources dir> <1.20.1 vanilla data dir> <mod data dir> [--fix]
"""
import json
import re
import sys
from pathlib import Path

src, vanilla_data, mod_data = map(Path, sys.argv[1:4])
fix = "--fix" in sys.argv


def ids(java, pattern):
    return set(re.findall(pattern, (src / java).read_text()))


blocks = ids("net/minecraft/world/level/block/Blocks.java", r'register\("([a-z0-9_/.]+)"')
items = ids("net/minecraft/world/item/Items.java", r'registerItem\("([a-z0-9_/.]+)"') | \
        ids("net/minecraft/world/item/Items.java", r'register\("([a-z0-9_/.]+)"')
# Block items registered from blocks: registerBlock(Blocks.FOO) -> id of FOO
block_fields = dict(re.findall(r'public static final Block (\w+) = register\("([a-z0-9_/.]+)"',
                               (src / "net/minecraft/world/level/block/Blocks.java").read_text()))
for field in re.findall(r'registerBlock\((?:new \w+\()?Blocks\.(\w+)', (src / "net/minecraft/world/item/Items.java").read_text()):
    if field in block_fields:
        items.add(block_fields[field])
entities = ids("net/minecraft/world/entity/EntityType.java", r'register\("([a-z0-9_/.]+)"')
registries = {"blocks": blocks, "items": items, "entity_types": entities}


def vanilla_tags(kind):
    base = vanilla_data / "minecraft" / "tags" / kind
    return {str(p.relative_to(base).with_suffix("")) for p in base.rglob("*.json")}


def mod_tags(kind):
    out = set()
    for ns in mod_data.iterdir():
        base = ns / "tags" / kind
        if base.exists():
            out |= {f"{ns.name}:{p.relative_to(base).with_suffix('')}" for p in base.rglob("*.json")}
    return out


problems = 0
for ns in mod_data.iterdir():
    for kind, registry in registries.items():
        base = ns / "tags" / kind
        if not base.exists():
            continue
        vtags = vanilla_tags(kind)
        mtags = mod_tags(kind)
        for path in sorted(base.rglob("*.json")):
            data = json.loads(path.read_text())
            changed = False
            for key in ("values", "remove"):
                entries = data.get(key, [])
                for i, entry in enumerate(entries):
                    if isinstance(entry, dict):
                        if not entry.get("required", True) is False:
                            ident = entry["id"]
                        else:
                            continue
                    else:
                        ident = entry
                    is_tag = ident.startswith("#")
                    name = ident.lstrip("#")
                    namespace, _, value = name.partition(":")
                    if namespace != "minecraft" and not (is_tag and name in mtags):
                        if is_tag and namespace in ("c", "forge"):
                            print(f"{path}: convention tag {ident} (not populated by Forge 1.20.1)")
                            problems += 1
                            if fix:
                                entries[i] = {"id": ident, "required": False}
                                changed = True
                        continue
                    ok = (value in vtags) if is_tag else (value in registry or namespace != "minecraft")
                    if is_tag and namespace != "minecraft":
                        ok = name in mtags
                    if not ok:
                        print(f"{path}: {ident} doesn't exist on 1.20.1")
                        problems += 1
                        if fix:
                            entries[i] = {"id": ident, "required": False}
                            changed = True
            if changed:
                path.write_text(json.dumps(data, indent=2) + "\n")
print(f"{problems} problem(s)")
