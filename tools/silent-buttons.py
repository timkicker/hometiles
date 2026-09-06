#!/usr/bin/env python3
"""Sucht anklickbare Flaechen ohne Namen auf dem gerade sichtbaren Bildschirm.

Das ist der Knopf, den ein Mensch mit einem Vorleseprogramm findet und ueber den nichts
gesagt wird. Ein Unit-Test sieht ihn nicht: er entsteht erst aus dem fertig gezeichneten
Bildschirm, und genau den liest `uiautomator dump` aus.

    tools/silent-buttons.py [geraet]

Ausgegeben werden die Masse der stummen Flaechen. Ein Treffer ist ein Anfangsverdacht, kein
Urteil - dazu gehoert ein Blick aufs Bildschirmfoto: manchmal traegt ein groesserer Knoten
*um* die Flaeche herum den Namen, und dann ist alles in Ordnung.

Flaechen, die am oberen oder unteren Rand haengen und **niedriger sind als eine gewoehnliche
Zeile dieses Bildschirms**, werden getrennt gemeldet: das ist eine halb hereingeblaetterte
Zeile. Ihre Beschriftung ist dann aus dem Baum geschnitten, und die Zeile sieht stumm aus,
obwohl sie es nicht ist. Am 03.09.2026 zweimal passiert - einmal fuenf Pixel hoch am unteren
Rand ("Use as phone app"), einmal sechzig Pixel am oberen. Die erste Fassung hatte dafuer
eine feste Grenze von 33 Pixeln und liess den zweiten Fall durch; der Maszstab ist deshalb
jetzt der Bildschirm selbst - die mittlere Hoehe seiner benannten Zeilen.
"""
import re
import subprocess
import sys


def masse(text: str) -> tuple[int, int, int, int]:
    x1, y1, x2, y2 = map(int, re.findall(r"-?\d+", text))
    return x1, y1, x2, y2


def enthaelt(aussen: str, innen: str) -> bool:
    ax1, ay1, ax2, ay2 = masse(aussen)
    ix1, iy1, ix2, iy2 = masse(innen)
    return ax1 <= ix1 and ay1 <= iy1 and ix2 <= ax2 and iy2 <= ay2


def main() -> int:
    geraet = sys.argv[1] if len(sys.argv) > 1 else None
    befehl = ["adb"] + (["-s", geraet] if geraet else []) + ["exec-out", "uiautomator", "dump", "/dev/tty"]
    roh = subprocess.run(befehl, capture_output=True, text=True).stdout
    knoten = re.findall(r"<node ([^>]*?)/?>", roh)
    if not knoten:
        print("kein Baum gelesen - ist der Bildschirm an und das Geraet entsperrt?")
        return 2

    def wert(k: str, name: str) -> str:
        treffer = re.search(r'%s="([^"]*)"' % name, k)
        return treffer.group(1) if treffer else ""

    benannt = [wert(k, "bounds") for k in knoten if wert(k, "text") or wert(k, "content-desc")]
    # Der untere Rand des Baums: alles, was daran klebt, kann abgeschnitten sein.
    unten = max((masse(wert(k, "bounds"))[3] for k in knoten if wert(k, "bounds")), default=0)
    oben = min((masse(wert(k, "bounds"))[1] for k in knoten if wert(k, "bounds")), default=0)
    # Wie hoch ist eine gewoehnliche Zeile hier? Der Median der benannten anklickbaren
    # Flaechen. Eine feste Zahl taugt nicht: die Zeilen wachsen mit der Schriftgroesse des
    # Nutzers, und beim Zweiten der beiden Faelle am 03.09. waren es sechzig Pixel.
    # Ueber **alle** anklickbaren Flaechen, nicht nur die benannten: in Compose sind der
    # anklickbare und der benannte Knoten zwei verschiedene, und die Liste der benannten
    # anklickbaren war auf der Einstellungsseite schlicht leer.
    hoehen = sorted(
        masse(wert(k, "bounds"))[3] - masse(wert(k, "bounds"))[1]
        for k in knoten
        if wert(k, "clickable") == "true"
    )
    zeilenhoehe = hoehen[len(hoehen) // 2] if hoehen else 66
    duenn = int(zeilenhoehe * 0.9)
    abgeschnitten = []
    stumm = []
    for k in knoten:
        if wert(k, "clickable") != "true":
            continue
        if wert(k, "text") or wert(k, "content-desc"):
            continue
        flaeche = wert(k, "bounds")
        # Ein Name darf innerhalb liegen (die Beschriftung im Knopf), aussen herum (der
        # Knopf ist ein Teilstueck einer benannten Kachel) oder **auf denselben Massen**:
        # Compose legt fuer eine Kachel zwei Knoten an, den anklickbaren und den benannten.
        # Wer das nicht zulaesst, meldet jede Kachel der App als stumm - erst gemacht, dann
        # am Bildschirmfoto gesehen, dass die Meldung falsch war.
        if any(enthaelt(flaeche, b) or enthaelt(b, flaeche) for b in benannt):
            continue
        x1, y1, x2, y2 = masse(flaeche)
        haengt_am_rand = y1 <= oben or y2 >= unten
        if haengt_am_rand and (y2 - y1) < duenn:
            abgeschnitten.append(flaeche)
            continue
        stumm.append(flaeche)

    print(f"stumme anklickbare Flaechen: {len(stumm)}")
    for flaeche in stumm:
        print("   ", flaeche)
    if abgeschnitten:
        print(f"am Rand abgeschnitten (kein Befund, weiterblaettern): {len(abgeschnitten)}")
        for flaeche in abgeschnitten:
            print("   ", flaeche)
    return 1 if stumm else 0


if __name__ == "__main__":
    raise SystemExit(main())
