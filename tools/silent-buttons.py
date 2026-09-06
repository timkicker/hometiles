#!/usr/bin/env python3
"""Looks for clickable areas without a name on the screen as it stands.

That is the button somebody using a screen reader finds and about which nothing is said. A
unit test does not see it: it arises only from the finished drawn screen, and that is
exactly what `uiautomator dump` reads out.

    tools/silent-buttons.py [device]

Printed are the bounds of the silent areas. A hit is a first suspicion, not a verdict - a
look at the screenshot belongs with it: sometimes a larger node *around* the area carries
the name, and then everything is in order.

Areas hanging at the top or bottom edge and **lower than an ordinary row of this screen**
are reported separately: that is a row half scrolled in. Its label is then cut out of the
tree, and the row looks silent although it is not. It happened twice on 03.09.2026 - once
five pixels high at the bottom edge ("Use as phone app"), once sixty at the top. The first
version had a fixed bound of 33 pixels for that and let the second case through; the measure
is therefore now the screen itself - the median height of its named rows.
"""
import re
import subprocess
import sys


def bounds(text: str) -> tuple[int, int, int, int]:
    x1, y1, x2, y2 = map(int, re.findall(r"-?\d+", text))
    return x1, y1, x2, y2


def contains(outer: str, inner: str) -> bool:
    ax1, ay1, ax2, ay2 = bounds(outer)
    ix1, iy1, ix2, iy2 = bounds(inner)
    return ax1 <= ix1 and ay1 <= iy1 and ix2 <= ax2 and iy2 <= ay2


def main() -> int:
    device = sys.argv[1] if len(sys.argv) > 1 else None
    command = ["adb"] + (["-s", device] if device else []) + ["exec-out", "uiautomator", "dump", "/dev/tty"]
    raw = subprocess.run(command, capture_output=True, text=True).stdout
    nodes = re.findall(r"<node ([^>]*?)/?>", raw)
    if not nodes:
        print("no tree read - is the screen on and the device unlocked?")
        return 2

    def value(node: str, name: str) -> str:
        hit = re.search(r'%s="([^"]*)"' % name, node)
        return hit.group(1) if hit else ""

    named = [value(n, "bounds") for n in nodes if value(n, "text") or value(n, "content-desc")]
    # The lower edge of the tree: whatever sticks to it may be cut off.
    bottom = max((bounds(value(n, "bounds"))[3] for n in nodes if value(n, "bounds")), default=0)
    top = min((bounds(value(n, "bounds"))[1] for n in nodes if value(n, "bounds")), default=0)
    # How high is an ordinary row here? The median of the clickable areas. A fixed number is
    # no good: rows grow with the user's text size, and in the second of the two cases on
    # 03.09. they were sixty pixels.
    # Over **all** clickable areas, not only the named ones: in compose the clickable and the
    # named node are two different ones, and the list of named clickable areas was simply
    # empty on the settings page.
    heights = sorted(
        bounds(value(n, "bounds"))[3] - bounds(value(n, "bounds"))[1]
        for n in nodes
        if value(n, "clickable") == "true"
    )
    row_height = heights[len(heights) // 2] if heights else 66
    thin = int(row_height * 0.9)
    cut_off = []
    silent = []
    for node in nodes:
        if value(node, "clickable") != "true":
            continue
        if value(node, "text") or value(node, "content-desc"):
            continue
        area = value(node, "bounds")
        # A name may lie inside (the label in the button), around it (the button is a piece
        # of a named tile) or **on the same bounds**: compose lays out two nodes for a tile,
        # the clickable and the named one. Whoever does not allow that reports every tile of
        # the app as silent - done first, then seen on the screenshot that the report was
        # wrong.
        if any(contains(area, b) or contains(b, area) for b in named):
            continue
        x1, y1, x2, y2 = bounds(area)
        at_the_edge = y1 <= top or y2 >= bottom
        if at_the_edge and (y2 - y1) < thin:
            cut_off.append(area)
            continue
        silent.append(area)

    print(f"silent clickable areas: {len(silent)}")
    for area in silent:
        print("   ", area)
    if cut_off:
        print(f"cut off at the edge (no finding, scroll on): {len(cut_off)}")
        for area in cut_off:
            print("   ", area)
    return 1 if silent else 0


if __name__ == "__main__":
    raise SystemExit(main())
