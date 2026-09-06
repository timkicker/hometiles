#!/usr/bin/env python3
"""Welche Mehrzahlformen eine Sprache wirklich braucht, aus CLDR statt geraten.

    tools/plurals.py [sprache ...]

Ohne Argumente nimmt es die Sprachen, die HomeTiles ausliefert, plus ein paar Kandidaten.

Android faellt fuer eine fehlende Form stillschweigend auf `other` zurueck. Kein Absturz,
sondern ein grammatisch falscher Satz, den nur jemand bemerkt, der die Sprache spricht. Wer
eine Sprache anlegt, muss also **vorher** wissen, welche Formen sie verlangt.

Die Liste der Kategorien allein reicht dabei nicht. Franzoesisch, Spanisch und Italienisch
fuehren alle drei ein `many`, und das klingt nach Arbeit; nachgerechnet trifft es aber nur
volle Millionen. HomeTiles zaehlt Kacheln, Kontakte, Sekunden und Bildschirme. Deshalb sagt
dieses Programm nicht nur, welche Kategorien es gibt, sondern welche **unterhalb der
Schwelle** ueberhaupt vorkommen. Das ist die Zahl, an der die Entscheidung haengt.

Entstanden am 04.09.2026 aus der Ansage, in CLDR nachzusehen statt zu raten. Das Nachsehen
hat sofort zwei Dinge geliefert, die ich falsch angenommen hatte: dass Franzoesisch mit
`one` und `other` auskommt (stimmt, aber nur wegen der Schwelle), und dass die Form fuer
eins in romanischen Sprachen auch fuer null gilt (stimmt **nur** fuer Franzoesisch, nicht
fuer Spanisch und Italienisch).

Braucht `python-babel`, das die CLDR-Regeln mitbringt.
"""
import sys

try:
    from babel import Locale
except ImportError:
    print("python-babel fehlt: pip install babel")
    raise SystemExit(2)

SCHWELLE = 1000
VORGABE = ["en", "de", "fr", "es", "it", "pt", "nl", "tr", "pl", "ru", "ar"]


def formen(code: str) -> tuple[set[str], set[str]]:
    """Alle Kategorien der Sprache, und die unterhalb der Schwelle vorkommenden."""
    loc = Locale.parse(code)
    alle = set(loc.plural_form.rules.keys()) | {"other"}
    gebraucht = {loc.plural_form(n) for n in range(0, SCHWELLE)}
    return alle, gebraucht


def main() -> int:
    sprachen = sys.argv[1:] or VORGABE
    print(f"Kategorien laut CLDR, und was davon unter {SCHWELLE} vorkommt:\n")
    for code in sprachen:
        try:
            alle, gebraucht = formen(code)
        except Exception as fehler:  # noqa: BLE001
            print(f"  {code:<3} FEHLER: {fehler}")
            continue
        nur_gross = sorted(alle - gebraucht)
        zusatz = f"   (nur bei grossen Zahlen: {', '.join(nur_gross)})" if nur_gross else ""
        print(f"  {code:<3} braucht: {', '.join(sorted(gebraucht)):<24}{zusatz}")
    print("\nWelche Form null bekommt (die Falle, die man nicht sieht):")
    for code in sprachen:
        try:
            loc = Locale.parse(code)
        except Exception:  # noqa: BLE001
            continue
        marke = "  <== null wie eins" if loc.plural_form(0) == loc.plural_form(1) else ""
        print(f"  {code:<3} 0 -> {loc.plural_form(0):<6} 1 -> {loc.plural_form(1)}{marke}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
