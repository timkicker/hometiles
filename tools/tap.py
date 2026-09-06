#!/usr/bin/env python3
"""Taps only once the screen is the expected one.

    tools/tap.py <device> <x> <y> <expected-activity> [expected-label] [--long]
    tools/tap.py <device> scroll <expected-activity> [times] [--up]
    tools/tap.py <device> row "<Label>" <expected-activity> [--long]

Checks three things before it taps:

1. **A fresh screenshot** is fetched - not to look at, but so that there is one at all and
   the screen is awake.
2. `mResumedActivity` **must** hold the expected name.
3. Where a label is given, it has to stand at exactly this place: the node under the finger
   (or one around it) carries it.

Written on 03.09.2026 at 07:30 after a misfire: after two taps on "back" the app list was
open instead of the settings page. `mResumedActivity` said so too - it had been read and the
old coordinate used anyway. The tap hit a foreign app, which opened its consent dialog.

`row` finds the place itself: it reads the screen, finds the node with this label and taps
its middle. Added on 03.09.2026 at 23:35, after the coordinate had twice been computed in a
shell variable - and the second time the search found nothing, the variable was empty, and
the call hit a row further up. The check caught it; it arose beside the check, in the line
that calls it.

`--long` holds the finger for 900 ms instead of briefly - the long press that opens the tile
editor. It came on 03.09.2026 at 21:10, because otherwise it would have been sent by hand
with `input swipe`: at exactly the place where the three checks do not apply.

The rule "a fresh photo and a look at mResumedActivity before every tap" had stood for hours
by then. It hung on attention. Now it hangs on a program.

`scroll` came on 03.09.2026 at 09:52, out of the next misfire of the same kind: to get down a
long list, ten fast swipes were sent from y=750. On this screen (854 px high) that is the
system's **gesture zone**. The notification shade opened, the screen went off, the phone was
locked, and afterwards a foreign app stood in front. Now the program scrolls: inside the
list, not too fast, and after every pull it looks whether the same activity is still open.
"""
import re
import subprocess
import sys


def adb(device, *args, binary=False):
    command = ["adb"] + (["-s", device] if device else []) + list(args)
    done = subprocess.run(command, capture_output=True)
    return done.stdout if binary else done.stdout.decode("utf-8", "replace")


def bounds(text):
    x1, y1, x2, y2 = map(int, re.findall(r"-?\d+", text))
    return x1, y1, x2, y2


def label_at(device, x, y):
    """The label standing at this place - from the smallest node outwards."""
    raw = adb(device, "exec-out", "uiautomator", "dump", "/dev/tty")
    hits = []
    for node in re.findall(r"<node ([^>]*?)/?>", raw):
        box = re.search(r'bounds="([^"]*)"', node)
        if not box:
            continue
        x1, y1, x2, y2 = bounds(box.group(1))
        if not (x1 <= x <= x2 and y1 <= y <= y2):
            continue
        text = re.search(r'text="([^"]*)"', node)
        description = re.search(r'content-desc="([^"]*)"', node)
        name = (text.group(1) if text else "") or (description.group(1) if description else "")
        if name:
            hits.append(((x2 - x1) * (y2 - y1), name))
    hits.sort()
    return [name for _, name in hits]


def screen(device):
    """Height and width of the screen."""
    raw = adb(device, "shell", "wm size")
    hit = re.search(r"(\d+)x(\d+)", raw)
    return (int(hit.group(1)), int(hit.group(2))) if hit else (480, 854)


def in_front(device):
    for line in adb(device, "shell", "dumpsys activity activities").splitlines():
        if "mResumedActivity" in line:
            return line.strip()
    return ""


def scroll(device, expected, times, up=False):
    """Rolls down inside a list - inside it, not at the edge.

    Start and end stay in the middle third: below sits the system's gesture zone (home
    screen, recent apps), above the notification shade. 350 ms instead of 80 - a fast swipe
    is read as a gesture, a slow one as scrolling.
    """
    width, height = screen(device)
    x, start, end = width // 2, int(height * 0.72), int(height * 0.30)
    # Back up: same path, other direction. Added on 04.09.2026, after scrolling past a row
    # and having to rebuild the whole screen for lack of a way back.
    if up:
        start, end = end, start
    for pull in range(times):
        active = in_front(device)
        if expected not in active:
            print(f"NOT scrolled (pull {pull + 1}): expected {expected!r}, in front is:\n    {active}")
            return 1
        adb(device, "shell", f"input swipe {x} {start} {x} {end} 350")
    active = in_front(device)
    if expected not in active:
        print(f"scrolled, but afterwards something else is in front:\n    {active}")
        return 1
    print(f"scrolled {times}x in {expected} ({start} -> {end})")
    return 0


def place_of(device, label):
    """Where is the row with this label? Middle of the smallest matching node."""
    raw = adb(device, "exec-out", "uiautomator", "dump", "/dev/tty")
    hits = []
    for node in re.findall(r"<node ([^>]*?)/?>", raw):
        box = re.search(r'bounds="([^"]*)"', node)
        text = re.search(r'text="([^"]*)"', node)
        description = re.search(r'content-desc="([^"]*)"', node)
        name = (text.group(1) if text else "") or (description.group(1) if description else "")
        if not box or not name:
            continue
        if label.lower() not in name.lower():
            continue
        x1, y1, x2, y2 = bounds(box.group(1))
        hits.append(((x2 - x1) * (y2 - y1), (x1 + x2) // 2, (y1 + y2) // 2))
    hits.sort()
    return (hits[0][1], hits[0][2]) if hits else (None, None)


def main():
    long_press = "--long" in sys.argv
    if long_press:
        sys.argv.remove("--long")
    if len(sys.argv) >= 5 and sys.argv[2] == "row":
        label, expected = sys.argv[3], sys.argv[4]
        active = in_front(sys.argv[1])
        if expected not in active:
            print(f"NOT searched: expected {expected!r}, in front is:\n    {active}")
            return 1
        x, y = place_of(sys.argv[1], label)
        if x is None:
            print(f"NOT tapped: {label!r} does not stand on this screen")
            return 1
        return tap(sys.argv[1], x, y, expected, label, long_press)

    if len(sys.argv) >= 4 and sys.argv[2] == "scroll":
        up = "--up" in sys.argv
        if up:
            sys.argv.remove("--up")
        times = int(sys.argv[4]) if len(sys.argv) > 4 else 1
        return scroll(sys.argv[1], sys.argv[3], times, up)
    if len(sys.argv) < 5:
        print(__doc__.strip().splitlines()[2].strip())
        return 2
    return tap(
        sys.argv[1],
        int(sys.argv[2]),
        int(sys.argv[3]),
        sys.argv[4],
        sys.argv[5] if len(sys.argv) > 5 else None,
        long_press,
    )


def tap(device, x, y, expected, label, long_press):
    """The three checks and then the tap. Both modes go through here."""
    shot = adb(device, "exec-out", "screencap", "-p", binary=True)
    if len(shot) < 1000:
        print("no screenshot - is the screen on?")
        return 2

    active = ""
    for line in adb(device, "shell", "dumpsys activity activities").splitlines():
        if "mResumedActivity" in line:
            active = line.strip()
            break
    if expected not in active:
        print(f"NOT tapped: expected {expected!r}, in front is:\n    {active}")
        return 1

    if label:
        names = label_at(device, x, y)
        if not any(label.lower() in n.lower() for n in names):
            print(f"NOT tapped: at ({x},{y}) stands {names or 'nothing named'}, expected {label!r}")
            return 1

    if long_press:
        # A long press is a swipe from the place to the place. 900 ms lies above every
        # threshold HomeTiles lets one set.
        adb(device, "shell", f"input swipe {x} {y} {x} {y} 900")
    else:
        adb(device, "shell", f"input tap {x} {y}")
    how = "long pressed" if long_press else "tapped"
    print(f"{how} at ({x},{y}) in {expected}" + (f", label {label!r}" if label else ""))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
