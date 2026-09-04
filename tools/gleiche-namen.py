#!/usr/bin/env python3
"""Sucht anklickbare Flaechen, die auf demselben Bildschirm denselben Namen tragen.

    tools/gleiche-namen.py [geraet]

Zwei Angebote, die gleich heissen, sind fuer jemanden mit einem Vorleseprogramm dasselbe
Angebot. Am Bildschirm unterscheiden sie sich dann durch etwas, das nicht gesagt wird - eine
Farbe, ein Symbol, die Reihenfolge.

Entstanden am 04.09.2026: in der Auswahl des Screen-Hintergrunds standen fuenf Zeilen, jede
in ihrer eigenen Farbe, und alle fuenf hiessen gleich. Im Quelltext stand sogar ein guter
Grund dafuer - man wolle die Farbe *sehen* -, und genau da endet er.

Der Name einer Flaeche ist ihre `content-desc`; hat sie keine, der Text der Knoten **in**
ihr. Das ist dieselbe Regel, nach der ein Vorleseprogramm vorgeht.

Ein Treffer ist ein Anfangsverdacht, kein Urteil. Es gibt gute Doppel:

* Eine Liste von Gleichartigem, die sich durch die Zweitzeile unterscheidet - dann steht die
  Zweitzeile im Namen und der Treffer bleibt aus.
* Zwei Knoten auf **denselben** Massen: Compose legt fuer eine Zeile oft einen anklickbaren
  und einen benannten Knoten an. Solche Paare zaehlt das Werkzeug selbst heraus.
"""
import re
import subprocess
import sys


def masse(text):
    zahlen = [int(z) for z in re.findall(r"-?\d+", text)]
    return tuple(zahlen[:4])


def enthaelt(aussen, innen):
    ax1, ay1, ax2, ay2 = masse(aussen)
    ix1, iy1, ix2, iy2 = masse(innen)
    return ax1 <= ix1 and ay1 <= iy1 and ix2 <= ax2 and iy2 <= ay2


def main(argv):
    geraet = argv[1] if len(argv) > 1 else None
    befehl = ["adb"] + (["-s", geraet] if geraet else []) + [
        "exec-out", "uiautomator", "dump", "/dev/tty",
    ]
    roh = subprocess.run(befehl, capture_output=True, text=True).stdout
    knoten = re.findall(r"<node ([^>]*?)/?>", roh)
    if not knoten:
        print("kein Baum gelesen - ist der Bildschirm an und das Geraet entsperrt?")
        return 2

    def wert(k, name):
        treffer = re.search(r'%s="([^"]*)"' % name, k)
        return treffer.group(1) if treffer else ""

    # Der Name, den ein Vorleseprogramm sagen wuerde: die eigene Beschreibung, sonst der
    # Text der Knoten darin - in der Reihenfolge, in der sie im Baum stehen.
    def gesprochen(k):
        eigene = wert(k, "content-desc").strip()
        if eigene:
            return eigene
        flaeche = wert(k, "bounds")
        teile = []
        for anderer in knoten:
            b = wert(anderer, "bounds")
            if not b or not enthaelt(flaeche, b):
                continue
            text = wert(anderer, "text").strip()
            beschreibung = wert(anderer, "content-desc").strip()
            for stueck in (beschreibung, text):
                if stueck and stueck not in teile:
                    teile.append(stueck)
        return ". ".join(teile)

    nach_namen = {}
    for k in knoten:
        if wert(k, "clickable") != "true":
            continue
        if not wert(k, "package").startswith("org.biglau"):
            continue
        name = gesprochen(k)
        if not name:
            continue  # das ist der Fall fuer stumme-knoepfe.py, nicht fuer diesen hier
        nach_namen.setdefault(name, []).append(wert(k, "bounds"))

    doppelt = []
    fuer_sich = 0
    for name, flaechen in sorted(nach_namen.items()):
        eindeutig = sorted(set(flaechen))
        if len(eindeutig) < 2:
            if len(flaechen) > 1:
                fuer_sich += 1
            continue
        doppelt.append((name, eindeutig))

    nachsatz = (" (%d Doppelknoten auf denselben Massen nicht gezaehlt)" % fuer_sich
                if fuer_sich else "")
    if not doppelt:
        print("Keine zwei Flaechen mit demselben Namen." + nachsatz)
        return 0
    print("Gleich benannte Flaechen:" + nachsatz)
    for name, flaechen in doppelt:
        print("  %dx %s" % (len(flaechen), name))
        for f in flaechen:
            print("      %s" % f)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
