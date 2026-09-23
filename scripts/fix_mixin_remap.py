#!/usr/bin/env python3
"""
Adds `remap = false` to mixin annotations whose targets are not Minecraft members (e.g. Create or Flywheel methods),
driven by the Mixin annotation processor's messages from a Forge build:

    ./gradlew :forge:compileJava --console=plain -q 2>&1 | tee build.log
    python3 scripts/fix_mixin_remap.py build.log [path-substring-filter]

Forge 1.20.1 runs on SRG names, so the AP must map every Minecraft member a mixin references and must *not* try to
map anything else. For each annotation the AP could not map:
  - injector/@Overwrite/@Shadow/@Accessor/@Invoker errors -> `remap = false` on that annotation
  - @At target warnings -> `remap = false` on that @At
Then every @At inside an unremapped injector that targets a Minecraft class gets an explicit `remap = true`.
"""
import re
import sys
from collections import defaultdict

ERR = re.compile(r"^(/\S+\.java):(\d+): error: Unable to locate obfuscation mapping for @(\w+)")
WARN = re.compile(r"^(/\S+\.java):(\d+): warning: Unable to locate (?:method|field) mapping for @At\((\w+)\.<target>\) '([^']+)'")
MC_TARGET = re.compile(r'target\s*=\s*"L(net/minecraft/|com/mojang/blaze3d/|com/mojang/math/)')


def annotation_span(src, start):
    """Returns (start, end) of the annotation beginning at src[start] == '@', including its argument list."""
    i = start + 1
    while i < len(src) and (src[i].isalnum() or src[i] in "_."):
        i += 1
    if i >= len(src) or src[i] != "(":
        return start, i
    depth = 0
    in_str = False
    while i < len(src):
        c = src[i]
        if in_str:
            if c == "\\":
                i += 1
            elif c == '"':
                in_str = False
        elif c == '"':
            in_str = True
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return start, i + 1
        i += 1
    raise ValueError("unbalanced annotation")


def set_remap(text, value):
    """Sets remap = value on a single annotation's text."""
    val = "true" if value else "false"
    name_end = 1
    while name_end < len(text) and (text[name_end].isalnum() or text[name_end] in "_."):
        name_end += 1
    if name_end == len(text):
        return f"{text}(remap = {val})"
    args = text[name_end + 1:-1]
    # Only look at the top level of this annotation for an existing remap
    depth = 0
    in_str = False
    for idx, c in enumerate(args):
        if in_str:
            if c == '"' and args[idx - 1] != "\\":
                in_str = False
            continue
        if c == '"':
            in_str = True
        elif c in "({":
            depth += 1
        elif c in ")}":
            depth -= 1
        elif depth == 0 and args.startswith("remap", idx) and re.match(r"remap\s*=", args[idx:]):
            m = re.match(r"remap\s*=\s*(true|false)", args[idx:])
            return text[:name_end + 1] + args[:idx] + f"remap = {val}" + args[idx + len(m.group(0)):] + ")"
    if not args.strip():
        return f"{text[:name_end]}(remap = {val})"
    if "=" not in args.split(",")[0] and not args.strip().startswith("{"):
        # single unnamed value, e.g. @Shadow("x") is not a thing, but @Accessor("name")
        return f"{text[:name_end]}(value = {args}, remap = {val})"
    return f"{text[:name_end + 1]}{args}, remap = {val})"


def find_annotation_on_line(src, line_no, kinds=None):
    lines = src.split("\n")
    offset = sum(len(l) + 1 for l in lines[:line_no - 1])
    line = lines[line_no - 1]
    for m in re.finditer(r"@(\w+)", line):
        if kinds is None or m.group(1) in kinds:
            return offset + m.start()
    return None


def main(log_path, path_filter=""):
    edits = defaultdict(list)  # file -> [(kind, line, extra)]
    for raw in open(log_path, encoding="utf-8", errors="replace"):
        if path_filter and path_filter not in raw.split(":")[0]:
            continue
        m = ERR.match(raw)
        if m:
            edits[m.group(1)].append(("error", int(m.group(2)), m.group(3)))
            continue
        m = WARN.match(raw)
        if m:
            edits[m.group(1)].append(("warn", int(m.group(2)), m.group(4)))

    for path, items in edits.items():
        src = open(path, encoding="utf-8").read()
        # Apply bottom-up so offsets stay valid
        for kind, line_no, extra in sorted(set(items), key=lambda x: -x[1]):
            if kind == "error":
                pos = None
                # The AP points at the annotated element; the annotation may sit on a preceding line
                for back in range(0, 4):
                    if line_no - back < 1:
                        break
                    pos = find_annotation_on_line(src, line_no - back, {extra})
                    if pos is not None:
                        break
                if pos is None:
                    print(f"skip {path}:{line_no} ({extra})")
                    continue
                start, end = annotation_span(src, pos)
                ann = set_remap(src[start:end], False)
                # Minecraft @At targets inside must still be remapped
                out = []
                i = 0
                while True:
                    j = ann.find("@At(", i)
                    if j < 0:
                        out.append(ann[i:])
                        break
                    s2, e2 = annotation_span(ann, j)
                    at = ann[s2:e2]
                    if MC_TARGET.search(at) and not re.search(r"remap\s*=", at):
                        at = set_remap(at, True)
                    out.append(ann[i:s2])
                    out.append(at)
                    i = e2
                src = src[:start] + "".join(out) + src[end:]
            else:
                pos = find_annotation_on_line(src, line_no)
                if pos is None:
                    continue
                start, end = annotation_span(src, pos)
                ann = src[start:end]
                j = ann.find(extra)
                if j < 0:
                    continue
                at_start = ann.rfind("@At(", 0, j)
                s2, e2 = annotation_span(ann, at_start)
                at = ann[s2:e2]
                if not re.search(r"remap\s*=", at):
                    ann = ann[:s2] + set_remap(at, False) + ann[e2:]
                src = src[:start] + ann + src[end:]
        open(path, "w", encoding="utf-8").write(src)
        print(f"updated {path}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else "")
