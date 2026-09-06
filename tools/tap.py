#!/usr/bin/env python3
"""Tippt erst, wenn der Bildschirm der erwartete ist.

    tools/tap.py <geraet> <x> <y> <erwartete-activity> [erwartete-beschriftung] [--lang]
    tools/tap.py <geraet> wischen <erwartete-activity> [wieoft] [--hoch]
    tools/tap.py <geraet> zeile "<Beschriftung>" <erwartete-activity> [--lang]

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

`zeile` sucht die Stelle selbst: es liest den Bildschirm, findet den Knoten mit dieser
Beschriftung und tippt in seine Mitte. Dazugekommen am 03.09.2026 um 23:35, nachdem ich
zweimal die Koordinate in einer Shell-Variablen ausgerechnet hatte - und beim zweiten Mal
fand die Suche nichts, die Variable war leer, und der Aufruf traf eine Zeile weiter oben.
Gefangen hat es die Pruefung; entstanden ist es daneben, in der Zeile, die sie aufruft.

`--lang` haelt den Finger 900 ms statt kurz - der Langdruck, der den Kachel-Editor oeffnet.
Er kam am 03.09.2026 um 21:10 dazu, weil ich ihn sonst von Hand mit `input swipe` geschickt
haette: an genau der Stelle, an der die drei Pruefungen nicht gelten.

Die Regel „vor jedem Tippen ein frisches Foto und ein Blick auf mResumedActivity" stand da
schon seit Stunden. Sie hing an meiner Aufmerksamkeit. Jetzt haengt sie an einem Programm.

`wischen` kam am 03.09.2026 um 09:52 dazu, aus dem naechsten Fehlgriff derselben Art: um in
einer langen Liste nach unten zu kommen, schickte ich zehn schnelle Wischer ab y=750. Auf
diesem Bildschirm (854 px hoch) ist das die **Gestenzone** des Systems. Die
Benachrichtigungsleiste ging auf, der Bildschirm aus, das Telefon war gesperrt, und danach
stand eine fremde App im Vordergrund. Jetzt wischt das Programm: innerhalb der Liste, nicht
zu schnell, und nach jedem Zug sieht es nach, ob noch dieselbe Activity offen ist.
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


def rand(geraet):
    """Hoehe und Breite des Bildschirms."""
    roh = adb(geraet, "shell", "wm size")
    treffer = re.search(r"(\d+)x(\d+)", roh)
    return (int(treffer.group(1)), int(treffer.group(2))) if treffer else (480, 854)


def offen(geraet):
    for zeile in adb(geraet, "shell", "dumpsys activity activities").splitlines():
        if "mResumedActivity" in zeile:
            return zeile.strip()
    return ""


def wischen(geraet, erwartet, wieoft, hoch=False):
    """Rollt in einer Liste nach unten - innerhalb, nicht am Rand.

    Anfang und Ende bleiben im mittleren Drittel: unten sitzt die Gestenzone des Systems
    (Startbildschirm, letzte Apps), oben die Benachrichtigungsleiste. 350 ms statt 80 -
    ein schneller Wisch wird als Geste gelesen, ein langsamer als Rollen.
    """
    breite, hoehe = rand(geraet)
    x, von, nach = breite // 2, int(hoehe * 0.72), int(hoehe * 0.30)
    # Zurueck nach oben: dieselbe Bahn, andere Richtung. Dazugekommen am 04.09.2026,
    # nachdem ich an einer Zeile vorbeigescrollt war und mangels Rueckweg den ganzen
    # Bildschirm neu aufbauen musste.
    if hoch:
        von, nach = nach, von
    for zug in range(wieoft):
        aktiv = offen(geraet)
        if erwartet not in aktiv:
            print(f"NICHT gewischt (Zug {zug + 1}): erwartet war „{erwartet}“, offen ist:\n    {aktiv}")
            return 1
        adb(geraet, "shell", f"input swipe {x} {von} {x} {nach} 350")
    aktiv = offen(geraet)
    if erwartet not in aktiv:
        print(f"gewischt, aber danach ist etwas anderes offen:\n    {aktiv}")
        return 1
    print(f"{wieoft}x gewischt in {erwartet} ({von} → {nach})")
    return 0


def stelle_von(geraet, beschriftung):
    """Wo steht die Zeile mit dieser Beschriftung? Mitte des kleinsten passenden Knotens."""
    roh = adb(geraet, "exec-out", "uiautomator", "dump", "/dev/tty")
    treffer = []
    for knoten in re.findall(r"<node ([^>]*?)/?>", roh):
        grenzen = re.search(r'bounds="([^"]*)"', knoten)
        text = re.search(r'text="([^"]*)"', knoten)
        beschreibung = re.search(r'content-desc="([^"]*)"', knoten)
        name = (text.group(1) if text else "") or (beschreibung.group(1) if beschreibung else "")
        if not grenzen or not name:
            continue
        if beschriftung.lower() not in name.lower():
            continue
        x1, y1, x2, y2 = masse(grenzen.group(1))
        treffer.append(((x2 - x1) * (y2 - y1), (x1 + x2) // 2, (y1 + y2) // 2))
    treffer.sort()
    return (treffer[0][1], treffer[0][2]) if treffer else (None, None)


def main():
    lang = "--lang" in sys.argv
    if lang:
        sys.argv.remove("--lang")
    if len(sys.argv) >= 5 and sys.argv[2] == "zeile":
        beschriftung, erwartet = sys.argv[3], sys.argv[4]
        aktiv = offen(sys.argv[1])
        if erwartet not in aktiv:
            print(f"NICHT gesucht: erwartet war „{erwartet}“, offen ist:\n    {aktiv}")
            return 1
        x, y = stelle_von(sys.argv[1], beschriftung)
        if x is None:
            print(f"NICHT getippt: „{beschriftung}“ steht nicht auf diesem Bildschirm")
            return 1
        return tippen(sys.argv[1], x, y, erwartet, beschriftung, lang)

    if len(sys.argv) >= 4 and sys.argv[2] == "wischen":
        hoch = "--hoch" in sys.argv
        if hoch:
            sys.argv.remove("--hoch")
        wieoft = int(sys.argv[4]) if len(sys.argv) > 4 else 1
        return wischen(sys.argv[1], sys.argv[3], wieoft, hoch)
    if len(sys.argv) < 5:
        print(__doc__.strip().splitlines()[2].strip())
        return 2
    return tippen(
        sys.argv[1],
        int(sys.argv[2]),
        int(sys.argv[3]),
        sys.argv[4],
        sys.argv[5] if len(sys.argv) > 5 else None,
        lang,
    )


def tippen(geraet, x, y, erwartet, beschriftung, lang):
    """Die drei Pruefungen und dann der Tipp. Beide Betriebsarten gehen hier durch."""
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

    if lang:
        # Ein Langdruck ist ein Wisch von der Stelle auf die Stelle. 900 ms liegen ueber
        # jeder Schwelle, die HomeTiles einstellen laesst.
        adb(geraet, "shell", f"input swipe {x} {y} {x} {y} 900")
    else:
        adb(geraet, "shell", f"input tap {x} {y}")
    wie = "lang gedrueckt" if lang else "getippt"
    print(f"{wie} auf ({x},{y}) in {erwartet}" + (f", Beschriftung „{beschriftung}“" if beschriftung else ""))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
