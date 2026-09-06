#!/usr/bin/env python3
"""Walks the focus order and reports what is clickable and never gets the focus.

The counterpart to `tools/small-buttons.py`. There the question is *large enough*, here it is
**reachable**. With a finger the difference never shows: whatever has a rectangle takes a
tap. With keys one only gets where the focus runs, and that is a quite different set.

    tools/unreachable.py [device]

On 04.09.2026 this tool would have saved three rounds in one evening:

* In a folder the focus stayed on the one filled tile, for eight key presses. The seven empty
  places beside it open the editor and could not be reached by key.
* The search field above the lists took the focus and did not pass it on; below it two lists
  could not be entered.
* The two delete panels in the tile editor did not listen to the back key.

All three were found by hand, every time one round later than necessary.

**It never selects.** The tool presses the four direction keys and nothing else, never the
select key or a digit. So it starts nothing, dials no number and sends nothing. That is why
it may also run on a phone with a SIM in it.

**Touch mode.** After a tap on the screen *nothing* has the focus in android, and
`requestFocus` is silently discarded. Only a key press pulls the window out of that state.
The tool's first press does exactly that.

**Two different findings**, with different causes:

* *never focusable* - the node reports `focusable="false"`. It cannot get the focus at all,
  however one walks. That is the hard case.
* *never reached* - the node could, but was never reached in this run. A first suspicion:
  perhaps a way leads there that the tool did not take.

**Why counting goes by labels and not by bounds:** a list scrolls, and the rectangles wander
with it. The same button would be called something else in every dump. The price is that two
rows with the same word melt into one - for that there is `tools/same-names.py`.

**The trap in it, walked into twice on 04.09.2026:** while a list scrolls, `uiautomator`
reports a row only in part - at half height *and with half its text*. In the settings the
two-line row `HomeTiles is your home screen` came out that way as an area of its own called
`Tap to change that in the system settings`, and that one never got the focus, because it
does not exist.

The first attempt to solve it through the geometry was wrong: whoever throws away every row
sticking to an edge throws away the bottom row of every list - and that is exactly the
interesting case when walking. Of twelve settings rows, five were left.

Right is to take it for what it is: a **naming problem**. For every area it is remembered
which texts stand in it. What is only a second line of another area cannot be a finding of
its own and drops out at the end.
"""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

DIRECTIONS = ["DPAD_DOWN", "DPAD_RIGHT", "DPAD_UP", "DPAD_LEFT"]
# A hard upper bound, so that a very long list does not run forever.
AT_MOST = 400
# And one in seconds. A dump takes over a second; without a deadline a long list runs for a
# quarter of an hour, and a measurement nobody waits out is one nobody repeats.
DEADLINE = 90


def dump(device):
    """The node dump of the screen as it stands."""
    command = ["adb"] + (["-s", device] if device else [])
    command += ["exec-out", "uiautomator", "dump", "--compressed", "/dev/tty"]
    raw = subprocess.run(command, capture_output=True).stdout.decode("utf-8", "replace")
    # Behind the last closing element stands another line of text from uiautomator itself.
    # Without the cut every parser gives up.
    end = raw.rfind("</hierarchy>")
    return raw[: end + len("</hierarchy>")] if end >= 0 else raw


def tree(xml):
    """The dump as a tree, or None when nothing usable came."""
    try:
        return ET.fromstring(xml)
    except ET.ParseError:
        return None


def texts(branch):
    """Every label in this area, top to bottom.

    Compose hangs the text on a child almost always, hence the whole subtree. In order, so
    that the main line stands before the second line.
    """
    found = []
    for deeper in branch.iter("node"):
        for field in ("content-desc", "text"):
            value = deeper.get(field)
            if value and value not in found:
                found.append(value)
    return found


def name_of(branch):
    """What a node is recognised by: its first label."""
    found = texts(branch)
    return found[0] if found else branch.get("bounds", "?")


def looks_like_bounds(text):
    """Does this name look like a rectangle, that is, like no label at all?"""
    return bool(re.fullmatch(r"\[\d+,\d+\]\[\d+,\d+\]", text))


def main(argv):
    device = argv[1] if len(argv) > 1 else None
    if "<node" not in dump(device):
        print("No node dump - is the device attached?")
        return 1

    clickable = {}   # name -> whether the node can get the focus at all
    rows = {}        # name -> the longest text list ever reported for it
    reached = set()  # name of every node that had the focus during the run
    stands = [None]  # where the focus stood at the last dump
    presses = 0

    def collect():
        root = tree(dump(device))
        if root is None:
            return
        for branch in root.iter("node"):
            if not branch.get("package", "").startswith("dev.kicker.hometiles"):
                continue
            if branch.get("clickable") == "true":
                title = name_of(branch)
                # Seen focusable once counts: the same node can hang at an edge in a later
                # dump and be reported differently then.
                clickable[title] = (
                    clickable.get(title, False) or branch.get("focusable") == "true"
                )
                found = texts(branch)
                if len(found) > len(rows.get(title, [])):
                    rows[title] = found
            if branch.get("focused") == "true":
                stands[0] = name_of(branch)
                reached.add(stands[0])

    def press(key, wait=0.35):
        command = ["adb"] + (["-s", device] if device else [])
        subprocess.run(command + ["shell", "input", "keyevent", key], capture_output=True)
        time.sleep(wait)

    collect()
    # First into the top left corner. Without that the result depends on where the focus
    # happened to stand when the tool began: seen on the home screen on 04.09.2026, one run
    # reported all eight areas reached, the next from the same screen reported two as
    # unreachable. From a corner the snake line runs through the whole grid.
    # No dump is taken while doing it: forty dumps cost a minute and say nothing the run
    # afterwards does not also see.
    for key in ["DPAD_UP"] * 30 + ["DPAD_LEFT"] * 10:
        press(key, wait=0.12)
        presses += 1
    collect()

    # The run itself: from every place every direction once.
    #
    # Two attempts before it were too weak, both measured in the folder on 04.09.2026, when
    # the strip `Close the folder` had just become reachable:
    #
    # * Keep one direction until it brings nothing new, then the next. The way from the
    #   bottom back up runs through nothing but things already seen, and the patience was
    #   used up before the run had even tried the second column.
    # * The same, but switching also when the focus does not move. With that it ran in a
    #   circle: the strip is a dead end, and the rotation arrived there every time on the
    #   same direction, which does nothing there.
    #
    # The fault both times was to tie the direction to the time instead of to the place. What
    # is remembered now is **which direction has already been tried at which place**. With
    # that the run is a search in a graph and not a pattern that gets stuck at a dead end:
    # from every place each of the four directions is taken exactly once, and it is over when
    # no unused one is left.
    tried = {}
    fruitless = 0
    stop_at = time.monotonic() + DEADLINE
    while presses < AT_MOST and time.monotonic() < stop_at:
        place = stands[0]
        open_ones = [d for d in DIRECTIONS if d not in tried.get(place, set())]
        if open_ones:
            fruitless = 0
            key = open_ones[0]
            tried.setdefault(place, set()).add(key)
        else:
            # From here everything has been tried. Move on a step and see whether something
            # is still open elsewhere; the tool has no movement other than the four. If
            # nothing open turns up, the screen is done.
            fruitless += 1
            if fruitless > 2 * len(DIRECTIONS):
                break
            key = DIRECTIONS[fruitless % len(DIRECTIONS)]
        press(key)
        presses += 1
        collect()

    # Every second line of an area. What stands in here is no area of its own but half the
    # text of another - see above.
    second_lines = {t for lines in rows.values() for t in lines[1:]}

    def real_findings(condition):
        return sorted(
            n for n, can in clickable.items()
            if condition(can) and n not in reached
            and n not in second_lines and not looks_like_bounds(n)
        )

    silent = sum(1 for n in clickable if looks_like_bounds(n))
    halves = sum(1 for n in clickable if n in second_lines)
    print("%d key presses, %d clickable areas seen, %d of them reached."
          % (presses, len(clickable), len(reached & set(clickable))))
    if time.monotonic() >= stop_at:
        print("(stopped after %d seconds - the screen is larger than the deadline)" % DEADLINE)
    if halves:
        print("(%d half reported rows merged)" % halves)
    if silent:
        print("(%d areas without any label - a case for silent-buttons.py)" % silent)

    never_focusable = real_findings(lambda can: not can)
    never_reached = real_findings(lambda can: can)
    if never_focusable:
        print("\nClickable but never focusable - unreachable by key:")
        for n in never_focusable:
            print("  " + n)
    if never_reached:
        print("\nFocusable but never reached in this run - a first suspicion:")
        for n in never_reached:
            print("  " + n)
    if not never_focusable and not never_reached:
        print("\nEverything clickable has had the focus once.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
