#!/usr/bin/env python3
"""Sucht antippbare Flaechen unter 48 dp auf dem gerade sichtbaren Bildschirm.

48 dp ist das Mindestmass fuer einen Fingertipp. Auf dem Jelly 2 (220 dpi) sind das 66
Bildpunkte. Ein Unit-Test sieht die fertige Flaeche nicht - sie entsteht erst aus Schrift,
Fuellung und dem Platz, der uebrig bleibt.

    tools/small-buttons.py [geraet]

Ein Treffer ist ein Anfangsverdacht, kein Urteil. Zwei haeufige Faelle sind keine:

* Eine Zeile am Rand des Fensters **oder einer Liste** ist **angeschnitten** - was
  `uiautomator` meldet, ist dann nicht das, was gezeichnet wird. Solche Zeilen zaehlt das
  Werkzeug selbst heraus und sagt am Ende, wie viele es waren.
* Ein Eingabefeld kann kleiner sein als die Zeile, die es zeigt. Dann kommt es darauf an,
  ob die ganze Zeile den Tipp annimmt (siehe `BigSearchField`). Am Geraet pruefen mit
  `adb shell dumpsys input_method | grep mInputShown` vor und nach dem Tipp.
"""
import re
import subprocess
import sys

DICHTE = 1.375           # 220 dpi / 160
MINDEST = 48 * DICHTE    # 66 Bildpunkte


def main(argv):
    geraet = argv[1] if len(argv) > 1 else None
    befehl = ["adb"] + (["-s", geraet] if geraet else []) + ["exec-out", "uiautomator", "dump", "/dev/tty"]
    xml = subprocess.run(befehl, capture_output=True).stdout.decode("utf-8", "replace")
    if "<node" not in xml:
        print("Kein Knotenabzug - haengt das Telefon?")
        return 1
    # Die Raender des Fensters: eine Zeile, die daran klebt, ist abgeschnitten und nicht
    # klein. `uiautomator` meldet die sichtbaren Masse, nicht die gezeichneten.
    #
    # **Beide** Raender, nicht nur der untere. Bis zum 04.09.2026 stand hier nur `unten`,
    # und der Notmodus-Bildschirm meldete nach dem Blaettern eine Flaeche von 333 x 6,5 dp
    # am **oberen** Rand - die erste Zeile der Liste, nach oben hinausgeschoben.
    # `silent-buttons.py` rechnet seit dem 03.09. mit beiden; die beiden Werkzeuge waren
    # sich uneins, und das faellt nur auf, wenn man sie nebeneinander laufen laesst.
    raender = [
        (int(m.group(2)), int(m.group(4)))
        for m in re.finditer(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    ]
    unten = max((y1 for _, y1 in raender), default=0)
    oben = min((y0 for y0, _ in raender), default=0)
    # Und dasselbe eine Ebene tiefer: eine Liste hat ihren eigenen Rand. Die letzte Zeile
    # darin ragt fast immer darueber hinaus, und `uiautomator` meldet sie in **voller**
    # Hoehe mit Massen, die teils ausserhalb liegen. Am 04.09.2026 in den Kontakten
    # gesehen: eine Zeile [11,681][469,733], waehrend die Liste bei 700 endet - gemeldet
    # als 37,8 dp, sichtbar waren 19 Bildpunkte, gezeichnet sind es 72 dp.
    listen = [
        tuple(int(z) for z in m.groups())
        for m in re.finditer(
            r'scrollable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml
        )
    ]

    def haengt_ueber_eine_liste(x0, y0, x1, y1):
        for lx0, ly0, lx1, ly1 in listen:
            if x1 <= lx0 or x0 >= lx1:
                continue
            if y0 < ly0 or y1 > ly1:
                return True
        return False
    klein = []
    abgeschnitten = 0
    for treffer in re.finditer(r"<node[^>]*>", xml):
        knoten = treffer.group(0)

        def wert(name):
            gefunden = re.search(name + r'="([^"]*)"', knoten)
            return gefunden.group(1) if gefunden else ""

        if wert("clickable") != "true" and wert("long-clickable") != "true":
            continue
        if not wert("package").startswith("dev.kicker.hometiles"):
            continue
        masse = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", wert("bounds"))
        if not masse:
            continue
        x0, y0, x1, y1 = (int(z) for z in masse.groups())
        breite, hoehe = x1 - x0, y1 - y0
        if breite >= MINDEST and hoehe >= MINDEST:
            continue
        # Nur die **Hoehe** kann vom Abschneiden kommen. Eine Flaeche, die zu schmal ist,
        # bleibt ein Befund, auch wenn sie am Rand klebt.
        am_rand = y0 <= oben or y1 >= unten or haengt_ueber_eine_liste(x0, y0, x1, y1)
        if am_rand and breite >= MINDEST:
            abgeschnitten += 1
            continue
        klein.append((wert("bounds"), breite, hoehe, wert("content-desc") or wert("text")))
    nachsatz = (" (%d angeschnittene Zeile(n) am Fenster- oder Listenrand nicht gezaehlt)" % abgeschnitten
                if abgeschnitten else "")
    if not klein:
        print("Keine Flaeche unter 48 dp." + nachsatz)
        return 0
    print("Unter 48 dp:" + nachsatz)
    for bounds, breite, hoehe, name in klein:
        print("%s  %d x %d px = %.1f x %.1f dp  %s"
              % (bounds, breite, hoehe, breite / DICHTE, hoehe / DICHTE, name))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
