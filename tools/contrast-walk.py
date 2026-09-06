#!/usr/bin/env python3
"""Measures the contrast of **every** labelled area of a screen at once.

    tools/contrast-walk.py [device]

`contrast.py` measures a rectangle it is told about. That is right when one place is under
suspicion, and laborious when a whole screen is to be gone through: on 04.09.2026 there were
six screens with over eighty labelled areas between them. By hand that would have meant
typing eighty rectangles - and whoever types leaves some out, usually the inconspicuous
ones.

So here the rectangles come out of the node dump: **every node with text is a place where
somebody has to read something.** Reported is whatever lies under 4.5:1 (the threshold for
text on a tile, see `Tokens.kt`), and at the end always the worst place on the screen - even
when it passes. A measurement that only stays silent does not say whether it looked.

The arithmetic comes from `contrast.py`, so that there is only one formula.

**Two limits.** Measured is the brightest against the darkest pixel in the rectangle; where a
field holds almost nothing but text, that is too strict or too lenient, and then
`contrast.py --ground` is the more precise tool. And every second pixel is sampled, otherwise
a screen takes a minute; a single bright edge can slip through that way.
"""
import importlib.util
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location("contrast", os.path.join(HERE, "contrast.py"))
contrast = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(contrast)

# The threshold for text on a tile. The other two from Tokens.kt (3.0 for a tile against the
# ground, 7.0 for text outside) this tool does not need: it only looks at nodes with text.
THRESHOLD = 4.5


def main(argv):
    device = argv[1] if len(argv) > 1 else None
    prefix = ["adb"] + (["-s", device] if device else [])
    shot = "/tmp/contrast-walk.png"
    with open(shot, "wb") as file:
        file.write(subprocess.run(prefix + ["exec-out", "screencap", "-p"],
                                  capture_output=True).stdout)
    raw = subprocess.run(prefix + ["exec-out", "uiautomator", "dump", "--compressed", "/dev/tty"],
                         capture_output=True).stdout.decode("utf-8", "replace")
    end = raw.rfind("</hierarchy>")
    if end < 0:
        print("No node dump - is the device attached?")
        return 1
    root = ET.fromstring(raw[: end + len("</hierarchy>")])
    picture = Image.open(shot).convert("RGB")

    poor = []
    worst = None
    measured = 0
    for branch in root.iter("node"):
        text = (branch.get("text") or "").strip()
        if not text or not branch.get("package", "").startswith("dev.kicker.hometiles"):
            continue
        box = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", branch.get("bounds", ""))
        if not box:
            continue
        x0, y0, x1, y1 = (int(n) for n in box.groups())
        # A strip of a few pixels is a clipped row, not text.
        if x1 - x0 < 8 or y1 - y0 < 8:
            continue
        pixels = [picture.getpixel((x, y)) for x in range(x0, x1, 2) for y in range(y0, y1, 2)]
        if not pixels:
            continue
        measured += 1
        bright = max(pixels, key=contrast.luminance)
        dark = min(pixels, key=contrast.luminance)
        value = contrast.ratio(bright, dark)
        if worst is None or value < worst[0]:
            worst = (value, text, f"[{x0},{y0}][{x1},{y1}]")
        if value < THRESHOLD:
            poor.append((value, text, f"[{x0},{y0}][{x1},{y1}]"))

    if measured == 0:
        print("No labelled area found - is HomeTiles in front at all?")
        return 1
    print("%d labelled areas measured." % measured)
    for value, text, box in sorted(poor):
        print("  under %.1f: %.2f:1  %r  %s" % (THRESHOLD, value, text[:46], box))
    if not poor:
        print("  nothing under %.1f:1." % THRESHOLD)
    print("  worst place: %.2f:1  %r  %s" % (worst[0], worst[1][:46], worst[2]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
