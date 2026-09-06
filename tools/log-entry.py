#!/usr/bin/env python3
"""Schreibt einen Abschnitt in STATUS.md - und sieht nach, ob er wirklich dasteht.

    tools/log-entry.py "## 🔧 Ueberschrift (03.09.2026, 14:05)" < text.md
    echo "Rumpf" | tools/log-entry.py "## 🔧 Ueberschrift"

Der neue Abschnitt kommt direkt unter die Marke `<!-- chronik:` in STATUS.md - dort faengt
die Chronik an. Oben darueber stehen die bleibenden Abschnitte (die Sperre, die Geraeteliste,
der Morgenstand), und die tragen teils selbst ein Datum; nach dem "ersten datierten
Abschnitt" zu suchen traf deshalb daneben. Eine ausdrueckliche Marke ist langweiliger und
richtig. Danach wird die Datei neu gelesen und geprueft: die
Ueberschrift muss genau einmal vorkommen. Sonst Rueckgabewert 1 und kein Wort darueber, dass
es geklappt haette.

Entstanden am 03.09.2026 um 14:05 aus einem stillen Verlust: ein Bash-Aufruf mit deutschen
Anfuehrungszeichen in einem Python-Schnipsel scheiterte an der Shell (`unmatched '`), **bevor
irgendetwas lief**. Ich habe danach nur den Rest wiederholt und committet - dabei fehlten
zwei README-Korrekturen und ein ganzer STATUS-Abschnitt. Aufgefallen ist es erst einen Takt
spaeter, weil eine Ueberschrift nicht zu finden war.

Ein fehlgeschlagener Befehl sieht einem erledigten zum Verwechseln aehnlich, wenn man nur auf
den Commit schaut. Also schaut das hier nach.
"""
import sys
from pathlib import Path

MARKE = "<!-- chronik:"


def main():
    if len(sys.argv) < 2:
        print(__doc__.strip().splitlines()[2].strip())
        return 2
    ueberschrift = sys.argv[1].rstrip()
    if not ueberschrift.startswith("## "):
        print(f"Die Ueberschrift muss mit '## ' anfangen, nicht: {ueberschrift[:40]}")
        return 2
    rumpf = sys.stdin.read().strip("\n")
    if not rumpf:
        print("Kein Rumpf auf der Standardeingabe - der Abschnitt waere leer.")
        return 2

    datei = Path(__file__).resolve().parent.parent / "STATUS.md"
    zeilen = datei.read_text(encoding="utf-8").split("\n")
    if sum(1 for z in zeilen if z.rstrip() == ueberschrift) > 0:
        print(f"Diese Ueberschrift steht schon in STATUS.md: {ueberschrift}")
        return 1

    marke = next((i for i, z in enumerate(zeilen) if z.startswith(MARKE)), None)
    if marke is None:
        print(f"Die Marke {MARKE} fehlt in STATUS.md - wo soll der neue Abschnitt hin?")
        return 1
    # Bis zum Ende des Kommentars, dann die Leerzeile dahinter.
    ende = marke
    while ende < len(zeilen) and "-->" not in zeilen[ende]:
        ende += 1
    stelle = ende + 2

    zeilen[stelle:stelle] = [ueberschrift, ""] + rumpf.split("\n") + [""]
    datei.write_text("\n".join(zeilen), encoding="utf-8")

    # Und jetzt nachsehen. Genau dafuer gibt es dieses Programm.
    neu = datei.read_text(encoding="utf-8").split("\n")
    treffer = sum(1 for z in neu if z.rstrip() == ueberschrift)
    if treffer != 1:
        print(f"NICHT geschrieben: die Ueberschrift steht {treffer}-mal in STATUS.md.")
        return 1
    zeile = neu.index(ueberschrift) + 1
    print(f"geschrieben: STATUS.md:{zeile}  {ueberschrift}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
