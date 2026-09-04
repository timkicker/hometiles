#!/usr/bin/env python3
"""Misst den Kontrast **jeder** beschrifteten Flaeche eines Bildschirms auf einmal.

    tools/kontrastgang.py [geraet]

`kontrast.py` misst ein Rechteck, das man ihm nennt. Das ist richtig, wenn man eine Stelle
im Verdacht hat, und muehsam, wenn man einen ganzen Bildschirm durchsehen will: am
04.09.2026 waren es sechs Bildschirme mit zusammen ueber achtzig beschrifteten Flaechen.
Von Hand haette das geheissen, achtzig Rechtecke abzutippen - und wer abtippt, laesst welche
aus, meist die unauffaelligen.

Deshalb kommen die Rechtecke hier aus dem Knotenabzug: **jeder Knoten mit Text ist eine
Stelle, an der jemand etwas lesen muss.** Gemeldet wird, was unter 4,5:1 liegt (die Schwelle
fuer Schrift auf einer Kachel, siehe `Tokens.kt`), und am Ende immer die schlechteste
Stelle des Bildschirms - auch wenn sie besteht. Eine Messung, die nur schweigt, sagt nicht,
ob sie hingesehen hat.

Gerechnet wird mit den Funktionen aus `kontrast.py`, damit es nur eine Formel gibt.

**Zwei Grenzen.** Gemessen wird der hellste gegen den dunkelsten Punkt im Rechteck; wo in
einem Feld fast nur Schrift steht, ist das zu streng oder zu milde, und dann ist
`kontrast.py --grund` das genauere Werkzeug. Und abgetastet wird jeder zweite Bildpunkt,
sonst dauert ein Bildschirm eine Minute; eine einzelne helle Kante kann damit durchrutschen.
"""
import importlib.util
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

from PIL import Image

HIER = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location("kontrast", os.path.join(HIER, "kontrast.py"))
kontrast = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(kontrast)

# Die Schwelle fuer Schrift auf einer Kachel. Die anderen beiden aus Tokens.kt (3,0 fuer
# eine Kachel gegen den Grund, 7,0 fuer Text ausserhalb) braucht dieses Werkzeug nicht: es
# sieht nur Knoten mit Text an.
SCHWELLE = 4.5


def main(argv):
    geraet = argv[1] if len(argv) > 1 else None
    vor = ["adb"] + (["-s", geraet] if geraet else [])
    foto = "/tmp/kontrastgang.png"
    with open(foto, "wb") as datei:
        datei.write(subprocess.run(vor + ["exec-out", "screencap", "-p"],
                                   capture_output=True).stdout)
    roh = subprocess.run(vor + ["exec-out", "uiautomator", "dump", "--compressed", "/dev/tty"],
                         capture_output=True).stdout.decode("utf-8", "replace")
    ende = roh.rfind("</hierarchy>")
    if ende < 0:
        print("Kein Knotenabzug - haengt das Geraet?")
        return 1
    wurzel = ET.fromstring(roh[: ende + len("</hierarchy>")])
    bild = Image.open(foto).convert("RGB")

    schlecht = []
    schlimmste = None
    gemessen = 0
    for zweig in wurzel.iter("node"):
        text = (zweig.get("text") or "").strip()
        if not text or not zweig.get("package", "").startswith("org.biglau"):
            continue
        masse = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", zweig.get("bounds", ""))
        if not masse:
            continue
        x0, y0, x1, y1 = (int(z) for z in masse.groups())
        # Ein Streifen von wenigen Bildpunkten ist eine angeschnittene Zeile, kein Text.
        if x1 - x0 < 8 or y1 - y0 < 8:
            continue
        punkte = [bild.getpixel((x, y)) for x in range(x0, x1, 2) for y in range(y0, y1, 2)]
        if not punkte:
            continue
        gemessen += 1
        hell = max(punkte, key=kontrast.helligkeit)
        dunkel = min(punkte, key=kontrast.helligkeit)
        wert = kontrast.verhaeltnis(hell, dunkel)
        if schlimmste is None or wert < schlimmste[0]:
            schlimmste = (wert, text, f"[{x0},{y0}][{x1},{y1}]")
        if wert < SCHWELLE:
            schlecht.append((wert, text, f"[{x0},{y0}][{x1},{y1}]"))

    if gemessen == 0:
        print("Keine beschriftete Flaeche gefunden - steht BigLau ueberhaupt im Vordergrund?")
        return 1
    print("%d beschriftete Flaechen gemessen." % gemessen)
    for wert, text, masse in sorted(schlecht):
        print("  unter %.1f: %.2f:1  %r  %s" % (SCHWELLE, wert, text[:46], masse))
    if not schlecht:
        print("  nichts unter %.1f:1." % SCHWELLE)
    print("  schlechteste Stelle: %.2f:1  %r  %s"
          % (schlimmste[0], schlimmste[1][:46], schlimmste[2]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
