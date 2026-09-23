#!/usr/bin/env python3
"""
Converts access transformer entries written with Mojang (official) member names into the SRG names Forge 1.20.1
expects, keeping the Mojang name as a trailing comment. Lines already in SRG form are left alone. Entries whose
member doesn't exist on 1.20.1 are reported and left unchanged.

    python3 scripts/at_to_srg.py common/src/main/resources/META-INF/accesstransformer.cfg \
        common/build/moddev/artifacts/namedToIntermediate.tsrg
"""
import re
import sys


def load_tsrg(path):
    classes = {}
    current = None
    with open(path) as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            if not line.startswith("\t"):
                named, _ = line.split(" ", 1)
                current = {"fields": {}, "methods": {}}
                classes[named] = current
                continue
            parts = line.strip().split(" ")
            if len(parts) == 2:
                current["fields"][parts[0]] = parts[1]
            elif len(parts) == 3:
                current["methods"][(parts[0], parts[1])] = parts[2]
    return classes


LINE_RE = re.compile(r"^(?P<access>\S+)\s+(?P<cls>[\w.$]+)(?:\s+(?P<member>[^\s#]+))?\s*(?P<comment>#.*)?$")


def main(at_path, tsrg_path):
    classes = load_tsrg(tsrg_path)
    out = []
    missing = []
    for raw in open(at_path).read().split("\n"):
        m = LINE_RE.match(raw.strip())
        if not raw.strip() or raw.strip().startswith("#") or not m:
            out.append(raw)
            continue
        cls = m.group("cls").replace(".", "/")
        member = m.group("member")
        if cls not in classes:
            missing.append(raw)
            out.append(raw)
            continue
        if member is None or re.match(r"^[fm]_\d+_", member) or member.startswith("<init>"):
            out.append(raw)
            continue
        info = classes[cls]
        if "(" in member:
            name, desc = member[:member.index("(")], member[member.index("("):]
            srg = info["methods"].get((name, desc))
            if srg is None:
                missing.append(raw)
                out.append(raw)
                continue
            new_member = srg + desc
        else:
            srg = info["fields"].get(member)
            if srg is None:
                missing.append(raw)
                out.append(raw)
                continue
            new_member = srg
        comment = f" # {member.split('(')[0]}"
        out.append(f"{m.group('access')} {m.group('cls')} {new_member}{comment}")
    open(at_path, "w").write("\n".join(out))
    for line in missing:
        print("no 1.20.1 target:", line)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
