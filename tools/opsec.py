#!/usr/bin/env python3
"""Looks for personal data and secrets - in the working tree and in the whole history.

    tools/opsec.py            # both, exit code 1 on any finding
    tools/opsec.py tree       # only the files git knows today
    tools/opsec.py history    # every blob in every commit on every branch
    tools/opsec.py history main   # only what is reachable from that branch
    tools/opsec.py probe      # counter-check: does the search still find what it searches for

Why the history is checked too: a push publishes every commit at once. A number that stood in
three hundred commits ago and was deleted long since is then published for good.

Why the tool exists. On 06.09.2026, one command before `gh repo create --public`, three real
mobile numbers from the owner's address book lay in the repository: in the working log, in a
source comment, and in 815 commits behind them. They were found because somebody looked by
hand. Looking by hand is not a check.

Every finding has to go, or stand in ALLOWED below with a reason. An allow-list without
reasons is a switch-off by detour.
"""
import io
import re
import subprocess
import sys

# ---------------------------------------------------------------- what never belongs in

FORBIDDEN_FILES = {
    "STATUS.md": "the working log. It records measurements taken on the owner's real phone, "
                 "along with what stood on the screen. It stays local.",
    "PLAN.md": "the plan. A german working document, and it names the phone it was written "
               "against.",
    "SPRACHEN.md": "the language handover, german, and a working document like the plan.",
    "local.properties": "holds local paths and sometimes keys.",
}

FORBIDDEN_SUFFIXES = {
    ".keystore": "signing keys never belong in a repository.",
    ".jks": "signing keys never belong in a repository.",
    ".pem": "a private key.",
    ".p12": "a private key.",
}

# ---------------------------------------------------------------- what is searched for

PATTERNS = [
    # A bracket belongs to the number only when it closes. The first version took every `(`
    # along and reported `+44 7700 900123 (7` as a number of its own - a number that does not
    # exist, and a finding nobody can resolve.
    ("phone number",
     re.compile(r"\+[0-9]{1,3}[ /-]*(?:\([0-9]{1,5}\)[ /-]*)?[0-9](?:[ /-]?[0-9]){5,16}"
                r"|\b00[0-9]{2}[ /-]?[0-9][0-9 /-]{6,}\b"
                r"|\b0[1-9][0-9]{0,2}[ /-]?[0-9]{6,}\b")),
    ("coordinate",
     re.compile(r"\b[0-9]{1,3}\.[0-9]{4,}\s*,\s*[0-9]{1,3}\.[0-9]{4,}\b")),
    ("e-mail",
     re.compile(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b")),
    ("device id",
     re.compile(r"\bJELLY[0-9]{6,}\b|\b89[0-9]{17,}\b|\b[0-9]{15}\b")),
    ("mac address",
     re.compile(r"\b(?:[0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}\b")),
    ("secret",
     re.compile(r"\bgh[pousr]_[A-Za-z0-9]{20,}\b|\bgithub_pat_[A-Za-z0-9_]{20,}\b"
                r"|\bAKIA[0-9A-Z]{16}\b|-----BEGIN [A-Z ]*PRIVATE KEY-----"
                r"|\bxox[baprs]-[A-Za-z0-9-]{10,}\b")),
    ("home path",
     re.compile(r"/home/[a-z][a-z0-9_-]{1,31}/")),
]

# ---------------------------------------------------------------- what may stand
#
# The key is the **literal finding**, not a pattern: an exception that matches a pattern
# lets something else through the next time as well.

ALLOWED = {
    # --- Phone numbers. The key is the bare run of digits, so that no spelling with other
    #     spaces slips past the list.
    "447700900123": "the ofcom range for film and television (07700 900000-900999). These "
                    "numbers are assigned to nobody and cannot become assigned.",
    "447700900124": "the same range, the second probe number.",
    "447700900125": "the same range.",
    "447700900": "a truncated finding from the same range.",
    "15550100": "the north american 555-01xx range, likewise for invented things.",
    "15550101": "the same range.",
    "43664111001": "this project's invented probe number, since the first sms test. The run "
                   "of digits is deliberately dull (111, 001) and stands only in tests.",
    "4366411100": "the same number, checked in one place cut by a digit.",
    "664111001": "the same number in the spelling without a country code.",
    "43664000111": "an invented second probe number, same make.",
    "436649998888": "an invented number for the block list, nothing but repeated digits.",
    "436809999999": "an invented number for the message filter, nothing but repeated digits.",
    "43512999888": "an invented landline number. 0512 is the area code of Innsbruck and "
                   "stands here because the rule checks grouping by area; the subscriber "
                   "number 999888 is made up.",
    "4351299988": "the same number, checked truncated.",
    "512999888": "the same number without a country code.",
    "43660123": "an invented fragment from a test about entering half a number.",
    "436601234": "the same fragment, one digit further.",
    "43660111001": "an invented number in `CallBackTest`, same make as 43664111001.",
    "436601234568": "an invented **second** number in `RespondNoticeTest`. It stands exactly "
                    "one digit beside the first, because the rule checks that two "
                    "conversations get different notification ids.",
    "436641234568": "the same make in `CallBlockingTest`: a number that resembles the "
                    "blocked one and must nevertheless not be blocked.",
    "43664222": "a second truncated number in `CallLogGroupingTest`, counterpart to "
                "43664111. Checked is only that two different numbers do not fall into the "
                "same group.",
    "49301234": "no finding but the start of a generated number: "
                "`tools/anonymise-config.py` appends four digits and replaces real numbers "
                "in a backup with it. 030 is the Berlin area code and 1234xxxx a run that "
                "belongs to nobody.",
    "4366412345": "a truncated placeholder number from an older test about entering half a "
                  "number. The full form is +43 664 1234567.",
    "43664111": "a truncated invented number in `ContactSortTest`. Checked is only whether "
                "spaces and a plus count as part of a number.",
    "43660124": "its counterpart in an older `SmsThreadsTest`: two numbers differing in the "
                "last digit, which must therefore not be the same conversation.",

    # --- Coordinates. Landmarks, not addresses: nobody lives at a landmark.
    "33.86880,151.20930": "Sydney Opera House, the textbook example for a negative latitude.",
    "33.8688,151.2093": "the same, unrounded.",
    "48.20849,16.37208": "Stephansplatz in Vienna. It stands here because the test needs a "
                         "place in Europe where a german comma as a decimal separator would "
                         "show. Before it there stood a coordinate in Innsbruck that could "
                         "have been somebody's home address.",

    # --- Addresses
    "tim@kicker.dev": "the author's public address, already in his other repositories.",
    "noreply@github.com": "GitHub itself.",

    # --- Paths
    "/home/runner/": "the working directory of the GitHub workshop, not a person.",
}

ALLOWED_ENDINGS = (
    "@example.at", "@example.com", "@example.org", "@example.de",
)

# The dirty probe stands in a file of its own, and that is the **only** one the search skips.
# Standing here, the tool would report itself; standing nowhere, nobody could check whether
# the search still searches.
PROBE_FILE = "tools/opsec-probe.txt"


def probes() -> dict:
    pairs = {}
    for line in io.open(PROBE_FILE, encoding="utf-8").read().splitlines():
        if line.startswith("#") or "|" not in line:
            continue
        kind, sentence = line.split("|", 1)
        pairs[kind] = sentence
    return pairs


def _run(digits: str) -> bool:
    """Is the subscriber part a straight run of digits, or a single digit?

    That is no heuristic but a shape: the number ends in at least five identical digits
    (`...11111`) or in at least six consecutive ones (`...1234567`, `...7654321`). That is
    how one writes a placeholder, and not how anybody copies a number down.

    The first version of this check let through everything with at most three different
    digits. That was too wide: a real number with little variety would have gone along. What
    does not fit here belongs in ALLOWED - one line, one reason.
    """
    if len(digits) < 6:
        return False
    if len(set(digits[-5:])) == 1:
        return True
    for length in range(len(digits), 5, -1):
        part = digits[-length:]
        steps = {ord(b) - ord(a) for a, b in zip(part, part[1:])}
        if steps in ({1}, {-1}):
            return True
    return False


def allowed(kind: str, finding: str) -> bool:
    """What gets through here stands in ALLOWED with a reason, or has a placeholder shape."""
    narrow = finding.strip().rstrip(".,;:)")
    if narrow in ALLOWED:
        return True
    if kind == "e-mail":
        return any(narrow.endswith(e) for e in ALLOWED_ENDINGS)
    if kind == "phone number":
        digits = re.sub(r"\D", "", narrow)
        return (digits in ALLOWED
                or digits.lstrip("0") in ALLOWED
                or _run(digits))
    if kind == "coordinate":
        return re.sub(r"\s", "", narrow) in ALLOWED
    return False


def examine(text: str, where: str) -> list:
    hits = []
    for kind, pattern in PATTERNS:
        for m in pattern.finditer(text):
            finding = m.group(0)
            if allowed(kind, finding):
                continue
            line = text.count("\n", 0, m.start()) + 1
            hits.append((kind, finding.strip(), where, line))
    return hits


def git(*args) -> str:
    return subprocess.run(["git", *args], capture_output=True, text=True).stdout


def tree() -> list:
    hits = []
    for path in git("ls-files").splitlines():
        if path == PROBE_FILE:
            continue
        for name in FORBIDDEN_FILES:
            if path == name or path.endswith("/" + name):
                hits.append(("forbidden file", path, path, 0))
        for suffix in FORBIDDEN_SUFFIXES:
            if path.endswith(suffix) and "debug" not in path:
                hits.append(("forbidden file", path, path, 0))
        # From disk, not from HEAD: what belongs checked is what goes in next, not what went
        # in last.
        try:
            text = io.open(path, encoding="utf-8").read()
        except (UnicodeDecodeError, FileNotFoundError, IsADirectoryError):
            continue
        hits += examine(text, path)
    return hits


def history(ref: str = "") -> list:
    """Every blob exactly once - not every commit, that would be the same file a hundred times.

    Without `ref` everything in the object store is checked. With `ref` only what is reachable
    from that branch - which is the question before a push: what really goes out. An old
    branch left lying does not belong to it.
    """
    if ref:
        lines = subprocess.run(["git", "rev-list", "--objects", ref],
                               capture_output=True, text=True).stdout.splitlines()
        candidates = [line.split()[0] for line in lines if line.strip()]
        kinds = subprocess.run(["git", "cat-file", "--batch-check"],
                               input="\n".join(candidates), capture_output=True,
                               text=True).stdout.splitlines()
        blobs = [k.split()[0] for k in kinds if k.split()[1:2] == ["blob"]]
    else:
        lines = subprocess.run(
            ["git", "cat-file", "--batch-check", "--batch-all-objects"],
            capture_output=True, text=True).stdout.splitlines()
        blobs = [line.split()[0] for line in lines if line.split()[1:2] == ["blob"]]
    # the content of the probe file is the same in every commit and is no finding
    probe_text = io.open(PROBE_FILE, encoding="utf-8").read()
    seen, hits = set(), []
    for h in blobs:
        raw = subprocess.run(["git", "cat-file", "blob", h], capture_output=True).stdout
        if b"\0" in raw[:8000]:
            continue
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError:
            continue
        if text == probe_text:
            continue
        for kind, finding, _, _ in examine(text, h):
            if (kind, finding) not in seen:
                seen.add((kind, finding))
                hits.append((kind, finding, f"blob {h[:10]}", 0))
    paths = git("log", ref or "--all", "--pretty=format:", "--name-only").split("\n")
    for path in set(paths):
        for name in FORBIDDEN_FILES:
            if path == name or path.endswith("/" + name):
                hits.append(("forbidden file", path, "in the history", 0))
    return hits


def probe() -> int:
    """Counter-check: does the search still find what it searches for?"""
    print("\n=== counter-check ===")
    bad = 0
    for kind, sentence in probes().items():
        found = [k for k, _, _, _ in examine(sentence, "probe") if k == kind]
        if found:
            print(f"  {kind:16s} found")
        else:
            print(f"  {kind:16s} NOT FOUND - the pattern is broken")
            bad = 1
    return bad


def report(title: str, hits: list) -> int:
    print(f"\n=== {title} ===")
    if not hits:
        print("  nothing found")
        return 0
    for kind, finding, where, line in sorted(set(hits)):
        place = f"{where}:{line}" if line else where
        print(f"  {kind:16s} {finding:34s} {place}")
    print(f"  {len(set(hits))} findings")
    return 1


def main() -> int:
    what = sys.argv[1] if len(sys.argv) > 1 else "both"
    ref = sys.argv[2] if len(sys.argv) > 2 else ""
    bad = 0
    if what in ("probe", "both", "tree"):
        bad |= probe()
    if what in ("tree", "both"):
        bad |= report("working tree", tree())
    if what in ("history", "both"):
        bad |= report(f"history of {ref or 'every branch'}", history(ref))
    if bad:
        print("\nDo NOT publish. Every finding goes, or into ALLOWED with a reason.")
    else:
        print("\nclean")
    return bad


if __name__ == "__main__":
    sys.exit(main())
