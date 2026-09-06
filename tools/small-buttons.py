#!/usr/bin/env python3
"""Looks for tappable areas under 48 dp on the screen as it stands.

48 dp is the smallest size for a fingertip. On the Jelly 2 (220 dpi) that is 66 pixels. A
unit test does not see the finished area - it arises only from text, padding and the room
that is left.

    tools/small-buttons.py [device]

A hit is a first suspicion, not a verdict. Two frequent cases are none:

* A row at the edge of the window **or of a list** is **clipped** - what `uiautomator`
  reports is then not what gets drawn. The tool counts such rows out itself and says at the
  end how many there were.
* An input field can be smaller than the row it shows. Then it depends on whether the whole
  row takes the tap (see `BigSearchField`). Check on the device with
  `adb shell dumpsys input_method | grep mInputShown` before and after the tap.
"""
import re
import subprocess
import sys

DENSITY = 1.375           # 220 dpi / 160
SMALLEST = 48 * DENSITY   # 66 pixels


def main(argv):
    device = argv[1] if len(argv) > 1 else None
    command = ["adb"] + (["-s", device] if device else []) + ["exec-out", "uiautomator", "dump", "/dev/tty"]
    xml = subprocess.run(command, capture_output=True).stdout.decode("utf-8", "replace")
    if "<node" not in xml:
        print("No node dump - is the phone attached?")
        return 1
    # The edges of the window: a row sticking to one of them is clipped, not small.
    # `uiautomator` reports the visible bounds, not the drawn ones.
    #
    # **Both** edges, not only the lower one. Until 04.09.2026 only `bottom` stood here, and
    # after scrolling, the emergency screen reported an area of 333 x 6.5 dp at the **top**
    # edge - the first row of the list, pushed out upwards. `silent-buttons.py` has counted
    # with both since 03.09.; the two tools disagreed, and that shows only when they run
    # beside each other.
    edges = [
        (int(m.group(2)), int(m.group(4)))
        for m in re.finditer(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    ]
    bottom = max((y1 for _, y1 in edges), default=0)
    top = min((y0 for y0, _ in edges), default=0)
    # And the same one level down: a list has an edge of its own. The last row in it almost
    # always reaches beyond, and `uiautomator` reports it at **full** height with bounds
    # partly outside. Seen in the contacts on 04.09.2026: a row [11,681][469,733] while the
    # list ends at 700 - reported as 37.8 dp, visible were 19 pixels, drawn it is 72 dp.
    lists = [
        tuple(int(n) for n in m.groups())
        for m in re.finditer(
            r'scrollable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml
        )
    ]

    def hangs_over_a_list(x0, y0, x1, y1):
        for lx0, ly0, lx1, ly1 in lists:
            if x1 <= lx0 or x0 >= lx1:
                continue
            if y0 < ly0 or y1 > ly1:
                return True
        return False
    small = []
    clipped = 0
    for hit in re.finditer(r"<node[^>]*>", xml):
        node = hit.group(0)

        def value(name):
            found = re.search(name + r'="([^"]*)"', node)
            return found.group(1) if found else ""

        if value("clickable") != "true" and value("long-clickable") != "true":
            continue
        if not value("package").startswith("dev.kicker.hometiles"):
            continue
        box = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", value("bounds"))
        if not box:
            continue
        x0, y0, x1, y1 = (int(n) for n in box.groups())
        width, height = x1 - x0, y1 - y0
        if width >= SMALLEST and height >= SMALLEST:
            continue
        # Only the **height** can come from clipping. An area that is too narrow stays a
        # finding, even where it sticks to an edge.
        at_the_edge = y0 <= top or y1 >= bottom or hangs_over_a_list(x0, y0, x1, y1)
        if at_the_edge and width >= SMALLEST:
            clipped += 1
            continue
        small.append((value("bounds"), width, height, value("content-desc") or value("text")))
    note = (" (%d clipped row(s) at the window or list edge not counted)" % clipped
            if clipped else "")
    if not small:
        print("No area under 48 dp." + note)
        return 0
    print("Under 48 dp:" + note)
    for box, width, height, name in small:
        print("%s  %d x %d px = %.1f x %.1f dp  %s"
              % (box, width, height, width / DENSITY, height / DENSITY, name))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
