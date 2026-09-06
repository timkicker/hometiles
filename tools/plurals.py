#!/usr/bin/env python3
"""Which plural forms a language really needs, from CLDR instead of guessed.

    tools/plurals.py [language ...]

Without arguments it takes the languages HomeTiles ships, plus a few candidates.

Android falls back silently to `other` for a missing form. No crash, but a grammatically
wrong sentence that only somebody who speaks the language notices. Whoever adds a language
therefore has to know **beforehand** which forms it demands.

The list of categories alone is not enough for that. French, Spanish and Italian all three
carry a `many`, and that sounds like work; computed, it only hits full millions. HomeTiles
counts tiles, contacts, seconds and screens. So this program does not only say which
categories exist but which ones occur at all **below the threshold**. That is the number the
decision hangs on.

Written on 04.09.2026 after being told to look it up in CLDR instead of guessing. Looking it
up delivered two things at once that had been assumed wrongly: that French gets by with
`one` and `other` (true, but only because of the threshold), and that the form for one also
covers zero in romance languages (true **only** for French, not for Spanish and Italian).

Needs `python-babel`, which brings the CLDR rules with it.
"""
import sys

try:
    from babel import Locale
except ImportError:
    print("python-babel is missing: pip install babel")
    raise SystemExit(2)

THRESHOLD = 1000
DEFAULT = ["en", "de", "fr", "es", "it", "pt", "nl", "tr", "pl", "ru", "ar"]


def forms(code: str) -> tuple[set[str], set[str]]:
    """All categories of the language, and the ones occurring below the threshold."""
    loc = Locale.parse(code)
    every = set(loc.plural_form.rules.keys()) | {"other"}
    used = {loc.plural_form(n) for n in range(0, THRESHOLD)}
    return every, used


def main() -> int:
    languages = sys.argv[1:] or DEFAULT
    print(f"Categories according to CLDR, and which of them occur under {THRESHOLD}:\n")
    for code in languages:
        try:
            every, used = forms(code)
        except Exception as error:  # noqa: BLE001
            print(f"  {code:<3} ERROR: {error}")
            continue
        large_only = sorted(every - used)
        note = f"   (only for large numbers: {', '.join(large_only)})" if large_only else ""
        print(f"  {code:<3} needs: {', '.join(sorted(used)):<24}{note}")
    print("\nWhich form zero gets (the trap one cannot see):")
    for code in languages:
        try:
            loc = Locale.parse(code)
        except Exception:  # noqa: BLE001
            continue
        mark = "  <== zero like one" if loc.plural_form(0) == loc.plural_form(1) else ""
        print(f"  {code:<3} 0 -> {loc.plural_form(0):<6} 1 -> {loc.plural_form(1)}{mark}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
