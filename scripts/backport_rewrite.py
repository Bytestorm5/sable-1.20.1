#!/usr/bin/env python3
"""
Mechanical source rewrites for the 1.21.1 -> 1.20.1 backport.

Rewrites imports of 1.21-only utility types to Sable's backport package and replaces a handful of 1.21 call forms with
their 1.20.1 equivalents. Idempotent; run from the repository root:

    python3 scripts/backport_rewrite.py common/src neoforge/src
"""
import re
import sys
from pathlib import Path

BACKPORT = "dev.ryanhcode.sable.backport"

# Fully-qualified 1.21 name -> replacement fully-qualified name
IMPORTS = {
    "net.minecraft.network.codec.StreamCodec": f"{BACKPORT}.network.codec.StreamCodec",
    "net.minecraft.network.codec.StreamDecoder": f"{BACKPORT}.network.codec.StreamDecoder",
    "net.minecraft.network.codec.StreamEncoder": f"{BACKPORT}.network.codec.StreamEncoder",
    "net.minecraft.network.codec.StreamMemberEncoder": f"{BACKPORT}.network.codec.StreamMemberEncoder",
    "net.minecraft.network.codec.ByteBufCodecs": f"{BACKPORT}.network.codec.ByteBufCodecs",
    "net.minecraft.client.DeltaTracker": f"{BACKPORT}.client.DeltaTracker",
    # 1.20.1 has no registry-aware buffer; packets use FriendlyByteBuf
    "net.minecraft.network.RegistryFriendlyByteBuf": "net.minecraft.network.FriendlyByteBuf",
    # Moved packages
    "net.minecraft.world.level.chunk.status.ChunkStatus": "net.minecraft.world.level.chunk.ChunkStatus",
    "net.minecraft.client.gui.screens.options.OptionsScreen": "net.minecraft.client.gui.screens.OptionsScreen",
    "net.minecraft.client.gui.screens.options.OptionsSubScreen": "net.minecraft.client.gui.screens.OptionsSubScreen",
    "net.minecraft.world.level.block.entity.EnchantingTableBlockEntity": "net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity",
    # NeoForge -> Forge
    "net.neoforged.neoforge.common.ModConfigSpec": "net.minecraftforge.common.ForgeConfigSpec",
}

# Simple-name renames that accompany the import rewrites above
SIMPLE_RENAMES = {
    r"\bRegistryFriendlyByteBuf\b": "FriendlyByteBuf",
    r"\bEnchantingTableBlockEntity\b": "EnchantmentTableBlockEntity",
    r"\bModConfigSpec\b": "ForgeConfigSpec",
}

EXPRESSIONS = [
    # ResourceLocation factories added in 1.21
    (re.compile(r"\bResourceLocation\.fromNamespaceAndPath\("), "new ResourceLocation("),
    (re.compile(r"\bResourceLocation\.parse\("), "new ResourceLocation("),
    (re.compile(r"\bResourceLocation\.withDefaultNamespace\("), "new ResourceLocation("),
    # Stream codec constants that live on vanilla types in 1.21
    (re.compile(r"\bUUIDUtil\.STREAM_CODEC\b"), "ByteBufCodecs.UUID"),
    (re.compile(r"\bResourceLocation\.STREAM_CODEC\b"), "ByteBufCodecs.RESOURCE_LOCATION"),
    # ModConfigSpec values implement BooleanSupplier/IntSupplier/...; ForgeConfigSpec values are plain Suppliers
    (re.compile(r"(Config\.[A-Z0-9_]+)\.getAs(?:Boolean|Int|Double|Long)\(\)"), r"\1.get()"),
    # DataFixerUpper 6 (1.20.1) has no argument-less getOrThrow
    (re.compile(r"\.getOrThrow\(\)"), ".getOrThrow(false, error -> { })"),
    # Java 21 APIs
    (re.compile(r"\bThread\.currentThread\(\)\.threadId\(\)"), "Thread.currentThread().getId()"),
    (re.compile(r"\bMath\.clamp\("), "Mth.clamp("),
    # 1.21's DeltaTracker; on 1.20.1 Minecraft keeps the (pause-aware) partial tick itself
    (re.compile(r"\.getTimer\(\)\.getGameTimeDeltaPartialTick\((?:true|false)\)"), ".getFrameTime()"),
    (re.compile(r"\.getTimer\(\)\.getGameTimeDeltaTicks\(\)"), ".getDeltaFrameTime()"),
    # NBT accounting
    (re.compile(r"\bNbtAccounter\.unlimitedHeap\(\)"), "NbtAccounter.UNLIMITED"),
    (re.compile(r"\bNbtAccounter\.create\("), "new NbtAccounter("),
    # Component helpers added in 1.20.3+
    (re.compile(r"\bComponent\.translationArg\(([^()]*(?:\([^()]*\))*[^()]*)\)"), r"Component.literal(String.valueOf(\1))"),
    (re.compile(r"(?<!style)\.withColor\((0x[0-9a-fA-F]+|\d+)\)"), r".withStyle(style -> style.withColor(\1))"),
]

IMPORT_RE = re.compile(r"^import\s+(static\s+)?([\w.$]+)(\.\*)?\s*;\s*$", re.M)


def add_import(src: str, fqn: str) -> str:
    if re.search(rf"^import\s+{re.escape(fqn)}\s*;", src, re.M):
        return src
    m = list(IMPORT_RE.finditer(src))
    line = f"import {fqn};\n"
    if m:
        pos = m[-1].end() + 1
        return src[:pos] + line + src[pos:]
    pkg = re.search(r"^package .*;\s*$", src, re.M)
    pos = pkg.end() + 1 if pkg else 0
    return src[:pos] + "\n" + line + src[pos:]


def rewrite(src: str) -> str:
    original = src
    for old, new in IMPORTS.items():
        src = re.sub(rf"^import\s+{re.escape(old)}\s*;", f"import {new};", src, flags=re.M)
    for old, new in SIMPLE_RENAMES.items():
        src = re.sub(old, new, src)
    for pattern, new in EXPRESSIONS:
        src = pattern.sub(new, src)
    if "ByteBufCodecs." in src and "ByteBufCodecs;" not in src and "package dev.ryanhcode.sable.backport.network.codec;" not in src:
        src = add_import(src, f"{BACKPORT}.network.codec.ByteBufCodecs")
    if "Mth.clamp(" in src and "import net.minecraft.util.Mth;" not in src:
        src = add_import(src, "net.minecraft.util.Mth")
    if src == original:
        return src
    # Drop duplicate imports produced by the rewrites
    seen = set()
    out = []
    for line in src.split("\n"):
        m = IMPORT_RE.match(line)
        if m:
            if line.strip() in seen:
                continue
            seen.add(line.strip())
        out.append(line)
    src = "\n".join(out)
    # Drop now-unused UUIDUtil imports
    if "import net.minecraft.core.UUIDUtil;" in src and not re.search(r"\bUUIDUtil\.", src):
        src = src.replace("import net.minecraft.core.UUIDUtil;\n", "")
    return src


def main(roots):
    changed = 0
    for root in roots:
        for path in Path(root).rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            new = rewrite(text)
            if new != text:
                path.write_text(new, encoding="utf-8")
                changed += 1
    print(f"rewrote {changed} files")


if __name__ == "__main__":
    main(sys.argv[1:] or ["common/src", "neoforge/src"])
