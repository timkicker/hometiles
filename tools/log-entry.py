#!/usr/bin/env python3
"""Writes a section into the working log - and then looks whether it is really there.

    tools/log-entry.py "## Heading (03.09.2026, 14:05)" < text.md
    echo "Body" | tools/log-entry.py "## Heading"

The new section goes directly under the marker `<!-- chronik:` in `STATUS.md` - that is
where the chronicle begins. Above it stand the lasting sections, and some of those carry a
date of their own; searching for the "first dated section" therefore hit the wrong place.
An explicit marker is duller and correct. Afterwards the file is read again and checked:
the heading has to appear exactly once. Otherwise exit code 1 and not a word about it
having worked.

The marker is german because it stands in a german file that is not in this repository -
the log records measurements taken on a real phone and stays on the machine it was written
on.

Written on 03.09.2026 at 14:05 after a silent loss: a bash call with german quotation marks
inside a python snippet failed in the shell (`unmatched '`) **before anything ran**. Only
the rest was repeated and committed - two readme corrections and a whole log section were
missing. It came out one round later, because a heading could not be found.

A failed command looks exactly like a finished one if you only look at the commit. So this
one looks.
"""
import sys
from pathlib import Path

MARKER = "<!-- chronik:"


def main():
    if len(sys.argv) < 2:
        print(__doc__.strip().splitlines()[2].strip())
        return 2
    heading = sys.argv[1].rstrip()
    if not heading.startswith("## "):
        print(f"The heading has to start with '## ', not: {heading[:40]}")
        return 2
    body = sys.stdin.read().strip("\n")
    if not body:
        print("No body on standard input - the section would be empty.")
        return 2

    file = Path(__file__).resolve().parent.parent / "STATUS.md"
    if not file.is_file():
        print("There is no STATUS.md beside this repository - nothing to write into.")
        return 1
    lines = file.read_text(encoding="utf-8").split("\n")
    if sum(1 for line in lines if line.rstrip() == heading) > 0:
        print(f"This heading is already in STATUS.md: {heading}")
        return 1

    marker = next((i for i, line in enumerate(lines) if line.startswith(MARKER)), None)
    if marker is None:
        print(f"The marker {MARKER} is missing in STATUS.md - where should the section go?")
        return 1
    # To the end of the comment, then the empty line behind it.
    end = marker
    while end < len(lines) and "-->" not in lines[end]:
        end += 1
    place = end + 2

    lines[place:place] = [heading, ""] + body.split("\n") + [""]
    file.write_text("\n".join(lines), encoding="utf-8")

    # And now look. That is what this program is for.
    written = file.read_text(encoding="utf-8").split("\n")
    hits = sum(1 for line in written if line.rstrip() == heading)
    if hits != 1:
        print(f"NOT written: the heading stands {hits} times in STATUS.md.")
        return 1
    line = written.index(heading) + 1
    print(f"written: STATUS.md:{line}  {heading}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
