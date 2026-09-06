#!/usr/bin/env python3
"""Laeuft die Fokusreihenfolge ab und meldet, was anklickbar ist und nie den Fokus bekommt.

Das Gegenstueck zu `tools/kleine-knoepfe.py`. Dort lautet die Frage *gross genug*, hier
lautet sie **erreichbar**. Mit dem Finger faellt der Unterschied nie auf: was ein Rechteck
hat, nimmt einen Tipp an. Mit Tasten kommt man nur dorthin, wohin der Fokus laeuft, und das
ist eine ganz andere Menge.

    tools/unerreichbar.py [geraet]

Am 04.09.2026 haette dieses Werkzeug an einem Abend dreimal gespart:

* Im Ordner blieb der Fokus auf der einen belegten Kachel stehen, acht Tastendruecke lang.
  Die sieben leeren Plaetze daneben oeffnen den Editor und waren mit Tasten unerreichbar.
* Das Suchfeld ueber den Listen nahm den Fokus und gab ihn nicht weiter; darunter kam man in
  zwei Listen nicht an.
* Die beiden Loeschtafeln im Kachel-Editor hoerten nicht auf die Zurueck-Taste.

Gefunden wurden alle drei von Hand, jedes Mal einen Zug spaeter als noetig.

**Es wird nie ausgewaehlt.** Das Werkzeug drueckt ausschliesslich die vier Richtungstasten,
niemals die Auswahl- oder eine Zifferntaste. Es startet also nichts, waehlt keine Nummer und
verschickt nichts. Deshalb darf es auch am Telefon mit gesteckter SIM laufen.

**Der Beruehrungsmodus.** Nach einem Tipp auf den Bildschirm hat in Android *nichts* den
Fokus, und `requestFocus` wird stillschweigend verworfen. Erst ein Tastendruck holt das
Fenster aus diesem Zustand. Der erste Druck des Werkzeugs tut genau das.

**Zwei verschiedene Befunde**, mit verschiedenen Ursachen:

* *nie fokussierbar* - der Knoten meldet `focusable="false"`. Er kann den Fokus gar nicht
  bekommen, egal wie man laeuft. Das ist der harte Fall.
* *nie erreicht* - der Knoten koennte, wurde aber in diesem Durchlauf nie erreicht. Ein
  Anfangsverdacht: vielleicht fuehrt ein Weg hin, den das Werkzeug nicht gegangen ist.

**Warum nach Beschriftungen gezaehlt wird und nicht nach Massen:** eine Liste scrollt, und
dabei wandern die Rechtecke. Derselbe Knopf hiesse in jedem Abzug anders. Der Preis ist,
dass zwei Zeilen mit demselben Wort zu einer verschmelzen - dafuer gibt es
`tools/gleiche-namen.py`.

**Die Falle dabei, am 04.09.2026 zweimal hineingetappt:** waehrend eine Liste scrollt, meldet
`uiautomator` eine Zeile nur zum Teil - mit halber Hoehe *und halbem Text*. In den
Einstellungen kam die zweizeilige Zeile `HomeTiles ist Ihr Startbildschirm` so als eigene
Flaeche namens `Antippen, um das in den Systemeinstellungen zu aendern` heraus, und die
bekam nie den Fokus, weil es sie nicht gibt.

Der erste Versuch, das ueber die Geometrie zu loesen, war falsch: wer jede Zeile wegwirft,
die an einer Kante klebt, wirft die unterste Zeile jeder Liste mit weg - und genau die ist
beim Durchlaufen der interessante Fall. Von zwoelf Einstellungszeilen blieben fuenf uebrig.

Richtig ist, es als das zu nehmen, was es ist: ein **Namensproblem**. Von jeder Flaeche wird
gemerkt, welche Texte in ihr stehen. Was nur eine Nebenzeile einer anderen Flaeche ist, kann
kein eigener Befund sein und faellt am Ende heraus.
"""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

RICHTUNGEN = ["DPAD_DOWN", "DPAD_RIGHT", "DPAD_UP", "DPAD_LEFT"]
# So viele Druecke in einer Richtung ohne einen neuen Fund, bis die naechste probiert wird.
# Gross genug, dass eine lange Liste am Stueck durchlaufen wird.
GEDULD = 12
# Eine harte Obergrenze, damit eine sehr lange Liste nicht ewig laeuft.
HOECHSTENS = 400
# Und eine in Sekunden. Ein Abzug dauert ueber eine Sekunde; ohne Frist laeuft eine lange
# Liste eine Viertelstunde, und eine Messung, die man nicht abwartet, macht niemand zweimal.
FRIST = 90
# Vierzig Druecke, um in die obere linke Ecke zu kommen. Genug fuer jede Liste in dieser
# App und billig genug, um es immer zu tun.


def abzug(geraet):
    """Der Knotenabzug des gerade sichtbaren Bildschirms."""
    befehl = ["adb"] + (["-s", geraet] if geraet else [])
    befehl += ["exec-out", "uiautomator", "dump", "--compressed", "/dev/tty"]
    roh = subprocess.run(befehl, capture_output=True).stdout.decode("utf-8", "replace")
    # Hinter dem letzten schliessenden Element steht noch eine Zeile Text von uiautomator
    # selbst. Ohne den Schnitt bricht jeder Parser ab.
    ende = roh.rfind("</hierarchy>")
    return roh[: ende + len("</hierarchy>")] if ende >= 0 else roh


def baum(xml):
    """Der Abzug als Baum, oder None wenn nichts Brauchbares kam."""
    try:
        return ET.fromstring(xml)
    except ET.ParseError:
        return None


def texte(zweig):
    """Alle Beschriftungen in dieser Flaeche, von oben nach unten.

    Compose haengt den Text fast immer an ein Kind, deshalb der ganze Teilbaum. Der Reihe
    nach, sodass die Hauptzeile vor der Nebenzeile steht.
    """
    gefunden = []
    for tiefer in zweig.iter("node"):
        for feld in ("content-desc", "text"):
            wert = tiefer.get(feld)
            if wert and wert not in gefunden:
                gefunden.append(wert)
    return gefunden


def name(zweig):
    """Woran ein Knoten wiederzuerkennen ist: seine erste Beschriftung."""
    gefunden = texte(zweig)
    return gefunden[0] if gefunden else zweig.get("bounds", "?")


def wie_masse(text):
    """Sieht dieser Name nach einem Rechteck aus, also nach gar keiner Beschriftung?"""
    return bool(re.fullmatch(r"\[\d+,\d+\]\[\d+,\d+\]", text))


def main(argv):
    geraet = argv[1] if len(argv) > 1 else None
    if "<node" not in abzug(geraet):
        print("Kein Knotenabzug - haengt das Geraet?")
        return 1

    anklickbar = {}   # Name -> ob der Knoten den Fokus ueberhaupt bekommen kann
    zeilen = {}       # Name -> die laengste Textliste, die je zu ihm gemeldet wurde
    erreicht = set()  # Name jedes Knotens, der im Lauf den Fokus hatte
    steht = [None]    # wo der Fokus beim letzten Abzug stand
    druecke = 0

    def sammeln():
        wurzel = baum(abzug(geraet))
        if wurzel is None:
            return
        for zweig in wurzel.iter("node"):
            if not zweig.get("package", "").startswith("dev.kicker.hometiles"):
                continue
            if zweig.get("clickable") == "true":
                titel = name(zweig)
                # Einmal fokussierbar gesehen zaehlt: derselbe Knoten kann in einem
                # spaeteren Abzug am Rand haengen und dann anders gemeldet werden.
                anklickbar[titel] = (
                    anklickbar.get(titel, False) or zweig.get("focusable") == "true"
                )
                gefunden = texte(zweig)
                if len(gefunden) > len(zeilen.get(titel, [])):
                    zeilen[titel] = gefunden
            if zweig.get("focused") == "true":
                steht[0] = name(zweig)
                erreicht.add(steht[0])

    def druecken(taste, warten=0.35):
        befehl = ["adb"] + (["-s", geraet] if geraet else [])
        subprocess.run(befehl + ["shell", "input", "keyevent", taste], capture_output=True)
        time.sleep(warten)

    sammeln()
    # Erst in die obere linke Ecke. Ohne das haengt das Ergebnis davon ab, wo der Fokus
    # zufaellig stand, als das Werkzeug anfing: am 04.09.2026 auf dem Startbildschirm
    # gesehen, ein Lauf meldete alle acht Flaechen erreicht, der naechste vom selben
    # Bildschirm zwei als unerreichbar. Von einer Ecke aus laeuft die Schlangenlinie unten
    # durch das ganze Raster.
    # Dabei wird nicht abgezogen: vierzig Abzuege kosten eine Minute und sagen nichts, was
    # der Lauf danach nicht auch sieht.
    for taste in ["DPAD_UP"] * 30 + ["DPAD_LEFT"] * 10:
        druecken(taste, warten=0.12)
        druecke += 1
    sammeln()

    # Der Lauf selbst: von jeder Stelle jede Richtung einmal.
    #
    # Zwei Anlaeufe davor waren zu schwach, beide am 04.09.2026 im Ordner gemessen, als der
    # Streifen `Ordner schliessen` gerade erreichbar geworden war:
    #
    # * Eine Richtung beibehalten, bis sie nichts Neues bringt, dann die naechste. Der Weg
    #   von unten zurueck nach oben laeuft durch lauter schon Gesehenes, und die Geduld war
    #   aufgebraucht, bevor der Lauf die zweite Spalte ueberhaupt probiert hatte.
    # * Dasselbe, aber zusaetzlich wechseln, wenn sich der Fokus nicht bewegt. Damit lief er
    #   im Kreis: der Streifen ist eine Sackgasse, und die Rotation kam dort jedes Mal auf
    #   derselben Richtung an, die dort nichts tut.
    #
    # Der Fehler war beide Male, die Richtung an der Zeit festzumachen statt am Ort. Gemerkt
    # wird jetzt, **welche Richtung an welcher Stelle schon probiert wurde**. Damit ist der
    # Lauf eine Suche im Graphen und kein Muster, das an einer Sackgasse haengenbleibt: von
    # jeder Stelle wird jede der vier Richtungen genau einmal genommen, und Schluss ist,
    # wenn es keine ungenutzte mehr gibt.
    versucht = {}
    vergeblich = 0
    schluss = time.monotonic() + FRIST
    while druecke < HOECHSTENS and time.monotonic() < schluss:
        stelle = steht[0]
        offen = [r for r in RICHTUNGEN if r not in versucht.get(stelle, set())]
        if offen:
            vergeblich = 0
            taste = offen[0]
            versucht.setdefault(stelle, set()).add(taste)
        else:
            # Von hier ist alles probiert. Ein Stueck weitergehen und sehen, ob anderswo noch
            # etwas offen ist; eine andere Bewegung als die vier hat das Werkzeug nicht.
            # Kommt dabei nichts Offenes mehr, ist der Bildschirm durch.
            vergeblich += 1
            if vergeblich > 2 * len(RICHTUNGEN):
                break
            taste = RICHTUNGEN[vergeblich % len(RICHTUNGEN)]
        druecken(taste)
        druecke += 1
        sammeln()

    # Jede Nebenzeile einer Flaeche. Was hier drinsteht, ist keine eigene Flaeche, sondern
    # der halbe Text einer anderen - siehe oben.
    nebenzeilen = {t for liste in zeilen.values() for t in liste[1:]}

    def echte_befunde(bedingung):
        return sorted(
            n for n, kann in anklickbar.items()
            if bedingung(kann) and n not in erreicht
            and n not in nebenzeilen and not wie_masse(n)
        )

    stumm = sum(1 for n in anklickbar if wie_masse(n))
    halbe = sum(1 for n in anklickbar if n in nebenzeilen)
    print("%d Tastendruecke, %d anklickbare Flaechen gesehen, %d davon erreicht."
          % (druecke, len(anklickbar), len(erreicht & set(anklickbar))))
    if time.monotonic() >= schluss:
        print("(nach %d Sekunden abgebrochen - der Bildschirm ist groesser als die Frist)"
              % FRIST)
    if halbe:
        print("(%d halb gemeldete Zeilen zusammengelegt)" % halbe)
    if stumm:
        print("(%d Flaechen ohne jede Beschriftung - ein Fall fuer stumme-knoepfe.py)" % stumm)

    nie_fokussierbar = echte_befunde(lambda kann: not kann)
    nie_erreicht = echte_befunde(lambda kann: kann)
    if nie_fokussierbar:
        print("\nAnklickbar, aber nie fokussierbar - mit Tasten unerreichbar:")
        for n in nie_fokussierbar:
            print("  " + n)
    if nie_erreicht:
        print("\nFokussierbar, aber in diesem Lauf nie erreicht - Anfangsverdacht:")
        for n in nie_erreicht:
            print("  " + n)
    if not nie_fokussierbar and not nie_erreicht:
        print("\nAlles Anklickbare hat einmal den Fokus gehabt.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
