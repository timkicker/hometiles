#!/usr/bin/env python3
"""Misst den Kontrast in einem Bildschirmfoto - dort, wo er wirklich ankommt.

`SurfaceContrastTest` prueft die Farbkonstanten gegeneinander. Was daraus auf dem
Bildschirm wird, steht auf einem anderen Blatt: eine Kachel kann eine eigene Farbe haben,
ein Foto tragen, unter einem Verlauf liegen, und die Schrift wird mit Kantenglaettung
gezeichnet. Dieses Werkzeug liest die Bildpunkte.

    tools/contrast.py bild.png x0 y0 x1 y1 [x0 y0 x1 y1 ...]

Fuer jedes Rechteck: der hellste und der dunkelste Bildpunkt darin und ihr Verhaeltnis
nach WCAG. In einem Feld mit Text sind das die Schrift und ihr Grund.

    tools/contrast.py bild.png --grund X Y  x0 y0 x1 y1

Vergleicht stattdessen gegen einen festen Punkt - fuer Text auf dem Hintergrund, wo im
Rechteck sonst nur Schrift liegt.

Die Schwellen von HomeTiles (`Tokens.kt`): 4.5 fuer Schrift auf einer Kachel, 3.0 fuer eine
Kachel gegen den Hintergrund, 7.0 fuer Text ausserhalb einer Kachel.
"""
import sys

from PIL import Image

SCHWELLEN = "4.5 Schrift/Kachel, 3.0 Kachel/Grund, 7.0 Text/Grund"


def helligkeit(rgb):
    def kanal(wert):
        wert = wert / 255.0
        return wert / 12.92 if wert <= 0.03928 else ((wert + 0.055) / 1.055) ** 2.4

    rot, gruen, blau = rgb[:3]
    return 0.2126 * kanal(rot) + 0.7152 * kanal(gruen) + 0.0722 * kanal(blau)


def verhaeltnis(eins, zwei):
    hell, dunkel = sorted((helligkeit(eins), helligkeit(zwei)), reverse=True)
    return (hell + 0.05) / (dunkel + 0.05)


def main(argv):
    if len(argv) < 6:
        print(__doc__.strip().split("\n\n")[2].strip())
        return 2
    bild = Image.open(argv[1]).convert("RGB")
    reste = argv[2:]
    grund = None
    if reste[0] == "--grund":
        grund = bild.getpixel((int(reste[1]), int(reste[2])))
        reste = reste[3:]
    if len(reste) % 4 != 0:
        print("Ein Rechteck braucht vier Zahlen: x0 y0 x1 y1")
        return 2
    for i in range(0, len(reste), 4):
        x0, y0, x1, y1 = (int(z) for z in reste[i:i + 4])
        punkte = [bild.getpixel((x, y)) for x in range(x0, x1) for y in range(y0, y1)]
        if not punkte:
            print("Leeres Rechteck: %d %d %d %d" % (x0, y0, x1, y1))
            continue
        hellster = max(punkte, key=helligkeit)
        dunkelster = min(punkte, key=helligkeit)
        if grund is None:
            print("[%d,%d][%d,%d] %s auf %s = %.2f:1"
                  % (x0, y0, x1, y1, hellster, dunkelster, verhaeltnis(hellster, dunkelster)))
        else:
            schrift = hellster if helligkeit(grund) < 0.5 else dunkelster
            print("[%d,%d][%d,%d] %s auf Grund %s = %.2f:1"
                  % (x0, y0, x1, y1, schrift, grund, verhaeltnis(schrift, grund)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
