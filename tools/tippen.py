#!/usr/bin/env python3
"""Tippt erst, wenn der Bildschirm der erwartete ist.

    tools/tippen.py <geraet> <x> <y> <erwartete-activity> [erwartete-beschriftung]

Prueft drei Dinge, bevor es tippt:

1. **Ein frisches Bildschirmfoto** wird geholt - nicht zum Ansehen, sondern damit es
   ueberhaupt eines gibt und der Bildschirm wach ist.
2. `mResumedActivity` **muss** den erwarteten Namen enthalten.
3. Ist eine Beschriftung angegeben, muss sie an genau dieser Stelle stehen: der Knoten unter
   dem Finger (oder einer um ihn herum) traegt sie.

Entstanden am 03.09.2026 um 07:30 aus einem Fehlgriff: nach zwei Mal „Zurueck" war statt der
Einstellungsseite die App-Liste offen. `mResumedActivity` sagte das auch - ich hatte es
gelesen und trotzdem die alte Koordinate benutzt. Getroffen hat der Tipp eine fremde App,
die daraufhin ihren Zustimmungsdialog aufmachte.

Die Regel „vor jedem Tippen ein frisches Foto und ein Blick auf mResumedActivity" stand da
schon seit Stunden. Sie hing an meiner Aufmerksamkeit. Jetzt haengt sie an einem Programm.
"""
import re
import subprocess
import sys


def adb(geraet, *args, binaer=False):
    befehl = ["adb"] + (["-s", geraet] if geraet else []) + list(args)
    fertig = subprocess.run(befehl, capture_output=True)
    return fertig.stdout if binaer else fertig.stdout.decode("utf-8", "replace")


def masse(text):
    x1, y1, x2, y2 = map(int, re.findall(r"-?\d+", text))
    return x1, y1, x2, y2


def beschriftung_bei(geraet, x, y):
    """Die Beschriftung, die an dieser Stelle steht - vom kleinsten Knoten nach aussen."""
    roh = adb(geraet, "exec-out", "uiautomator", "dump", "/dev/tty")
    treffer = []
    for knoten in re.findall(r"<node ([^>]*?)/?>", roh):
        grenzen = re.search(r'bounds="([^"]*)"', knoten)
        if not grenzen:
            continue
        x1, y1, x2, y2 = masse(grenzen.group(1))
        if not (x1 <= x <= x2 and y1 <= y <= y2):
            continue
        text = re.search(r'text="([^"]*)"', knoten)
        beschreibung = re.search(r'content-desc="([^"]*)"', knoten)
        name = (text.group(1) if text else "") or (beschreibung.group(1) if beschreibung else "")
        if name:
            treffer.append(((x2 - x1) * (y2 - y1), name))
    treffer.sort()
    return [name for _, name in treffer]


def main():
    if len(sys.argv) < 5:
        print(__doc__.strip().splitlines()[2].strip())
        return 2
    geraet, x, y, erwartet = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), sys.argv[4]
    beschriftung = sys.argv[5] if len(sys.argv) > 5 else None

    foto = adb(geraet, "exec-out", "screencap", "-p", binaer=True)
    if len(foto) < 1000:
        print("kein Bildschirmfoto - ist der Bildschirm an?")
        return 2

    aktiv = ""
    for zeile in adb(geraet, "shell", "dumpsys activity activities").splitlines():
        if "mResumedActivity" in zeile:
            aktiv = zeile.strip()
            break
    if erwartet not in aktiv:
        print(f"NICHT getippt: erwartet war „{erwartet}“, offen ist:\n    {aktiv}")
        return 1

    if beschriftung:
        namen = beschriftung_bei(geraet, x, y)
        if not any(beschriftung.lower() in n.lower() for n in namen):
            print(f"NICHT getippt: an ({x},{y}) steht {namen or 'nichts Benanntes'}, erwartet war „{beschriftung}“")
            return 1

    adb(geraet, "shell", f"input tap {x} {y}")
    print(f"getippt auf ({x},{y}) in {erwartet}" + (f", Beschriftung „{beschriftung}“" if beschriftung else ""))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
