#!/usr/bin/env python3
"""Looks for clickable areas carrying the same name on the same screen.

    tools/same-names.py [device]

Two offers that are called the same are, for somebody using a screen reader, the same offer.
On the screen they then differ by something that is never said - a colour, an icon, the
order.

Written on 04.09.2026: the screen background chooser held five rows, each in its own colour,
and all five were called the same. The source even carried a good reason for it - one wants
to *see* the colour - and that is exactly where it ends.

The name of an area is its `content-desc`; if it has none, the text of the nodes **inside**
it. That is the same rule a screen reader goes by.

A hit is a first suspicion, not a verdict. There are good doubles:

* A list of like things that differ by their second line - then the second line stands in the
  name and no hit arises.
* Two nodes on the **same** bounds: compose often lays out one clickable and one named node
  for a single row. The tool counts such pairs out itself.
"""
import re
import subprocess
import sys


def bounds(text):
    numbers = [int(n) for n in re.findall(r"-?\d+", text)]
    return tuple(numbers[:4])


def contains(outer, inner):
    ax1, ay1, ax2, ay2 = bounds(outer)
    ix1, iy1, ix2, iy2 = bounds(inner)
    return ax1 <= ix1 and ay1 <= iy1 and ix2 <= ax2 and iy2 <= ay2


def main(argv):
    device = argv[1] if len(argv) > 1 else None
    command = ["adb"] + (["-s", device] if device else []) + [
        "exec-out", "uiautomator", "dump", "/dev/tty",
    ]
    raw = subprocess.run(command, capture_output=True, text=True).stdout
    nodes = re.findall(r"<node ([^>]*?)/?>", raw)
    if not nodes:
        print("no tree read - is the screen on and the device unlocked?")
        return 2

    def value(node, name):
        hit = re.search(r'%s="([^"]*)"' % name, node)
        return hit.group(1) if hit else ""

    # The name a screen reader would say: the description of its own, otherwise the text of
    # the nodes inside it - in the order they stand in the tree.
    def spoken(node):
        own = value(node, "content-desc").strip()
        if own:
            return own
        area = value(node, "bounds")
        parts = []
        for other in nodes:
            b = value(other, "bounds")
            if not b or not contains(area, b):
                continue
            text = value(other, "text").strip()
            description = value(other, "content-desc").strip()
            for piece in (description, text):
                if piece and piece not in parts:
                    parts.append(piece)
        return ". ".join(parts)

    by_name = {}
    for node in nodes:
        if value(node, "clickable") != "true":
            continue
        if not value(node, "package").startswith("dev.kicker.hometiles"):
            continue
        name = spoken(node)
        if not name:
            continue  # that is the case for silent-buttons.py, not for this one
        by_name.setdefault(name, []).append(value(node, "bounds"))

    doubled = []
    on_themselves = 0
    for name, areas in sorted(by_name.items()):
        unique = sorted(set(areas))
        if len(unique) < 2:
            if len(areas) > 1:
                on_themselves += 1
            continue
        doubled.append((name, unique))

    note = (" (%d double nodes on the same bounds not counted)" % on_themselves
            if on_themselves else "")
    if not doubled:
        print("No two areas with the same name." + note)
        return 0
    print("Areas named alike:" + note)
    for name, areas in doubled:
        print("  %dx %s" % (len(areas), name))
        for a in areas:
            print("      %s" % a)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
