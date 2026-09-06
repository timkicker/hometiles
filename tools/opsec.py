#!/usr/bin/env python3
"""Sucht personenbezogene Daten und Geheimnisse - im Arbeitsbaum und in der ganzen Geschichte.

    tools/opsec.py            # beides, Rueckgabewert 1 bei jedem Fund
    tools/opsec.py baum       # nur die Dateien, die git heute kennt
    tools/opsec.py geschichte # jeden Blob in jedem Commit auf jedem Zweig

Warum die Geschichte mitgeprueft wird: ein Push veroeffentlicht alle Commits auf einmal.
Eine Nummer, die vor 300 Commits drinstand und laengst geloescht ist, steht danach trotzdem
fuer immer im Netz. Am 06.09.2026 hat genau das gefehlt - drei echte Rufnummern aus dem
Adressbuch des Nutzers lagen in 815 Commits, und aufgefallen ist es erst, weil vor dem
Veroeffentlichen von Hand nachgesehen wurde. Von Hand nachsehen ist keine Pruefung.

Jeder Fund muss entweder verschwinden oder unten in ERLAUBT stehen, mit Grund. Eine
Ausnahmeliste ohne Grund ist eine Abschaltung mit Umweg.
"""
import io
import re
import subprocess
import sys

# ---------------------------------------------------------------- was nie hineingehoert

VERBOTENE_DATEIEN = {
    "STATUS.md": "das Arbeitstagebuch. Es protokolliert Messungen am echten Telefon des "
                 "Nutzers, mitsamt dem, was auf dem Bildschirm stand. Es bleibt lokal.",
    "local.properties": "enthaelt lokale Pfade und manchmal Schluessel.",
}

VERBOTENE_ENDUNGEN = {
    ".keystore": "Signierschluessel gehoeren nie in ein Repository.",
    ".jks": "Signierschluessel gehoeren nie in ein Repository.",
    ".pem": "privater Schluessel.",
    ".p12": "privater Schluessel.",
}

# ---------------------------------------------------------------- wonach gesucht wird

MUSTER = [
    # Eine Klammer gehoert nur dann dazu, wenn sie sich schliesst. Die erste Fassung nahm
    # jedes `(` mit und meldete `+44 7700 900123 (7` als eigene Nummer - eine Nummer, die
    # es nicht gibt, und ein Fund, den niemand aufloesen kann.
    ("Rufnummer",
     re.compile(r"\+[0-9]{1,3}[ /-]*(?:\([0-9]{1,5}\)[ /-]*)?[0-9](?:[ /-]?[0-9]){5,16}"
                r"|\b00[0-9]{2}[ /-]?[0-9][0-9 /-]{6,}\b"
                r"|\b0[1-9][0-9]{0,2}[ /-]?[0-9]{6,}\b")),
    ("Koordinate",
     re.compile(r"\b[0-9]{1,3}\.[0-9]{4,}\s*,\s*[0-9]{1,3}\.[0-9]{4,}\b")),
    ("E-Mail",
     re.compile(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b")),
    ("Geraetekennung",
     re.compile(r"\bJELLY[0-9]{6,}\b|\b89[0-9]{17,}\b|\b[0-9]{15}\b")),
    ("MAC-Adresse",
     re.compile(r"\b(?:[0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}\b")),
    ("Geheimnis",
     re.compile(r"\bgh[pousr]_[A-Za-z0-9]{20,}\b|\bgithub_pat_[A-Za-z0-9_]{20,}\b"
                r"|\bAKIA[0-9A-Z]{16}\b|-----BEGIN [A-Z ]*PRIVATE KEY-----"
                r"|\bxox[baprs]-[A-Za-z0-9-]{10,}\b")),
    ("Heimatpfad",
     re.compile(r"/home/[a-z][a-z0-9_-]{1,31}/")),
]

# ---------------------------------------------------------------- was stehen bleiben darf
#
# Der Schluessel ist der **wortwoertliche Fund**, nicht ein Muster: eine Ausnahme, die auf
# ein Muster passt, laesst beim naechsten Mal auch etwas anderes durch.

ERLAUBT = {
    # --- Rufnummern. Der Schluessel ist die reine Ziffernfolge, damit keine Schreibweise
    #     mit anderen Leerzeichen an der Liste vorbeikommt.
    "447700900123": "Ofcom-Bereich fuer Film und Fernsehen (07700 900000-900999). Diese "
                    "Nummern sind niemandem zugeteilt und koennen es nicht werden.",
    "447700900124": "derselbe Bereich, die zweite Probennummer.",
    "447700900125": "derselbe Bereich.",
    "447700900": "abgeschnittener Fund aus demselben Bereich.",
    "15550100": "der nordamerikanische 555-01xx-Bereich, ebenfalls fuer Erfundenes.",
    "15550101": "derselbe Bereich.",
    "43664111001": "erfundene Probennummer dieses Projekts, seit dem ersten SMS-Test. Die "
                   "Ziffernfolge ist absichtlich stumpf (111, 001) und steht nur in Tests.",
    "4366411100": "dieselbe Nummer, an einer Stelle um eine Ziffer beschnitten geprueft.",
    "664111001": "dieselbe Nummer in der Schreibweise ohne Landesvorwahl.",
    "43664000111": "erfundene zweite Probennummer, gleiche Machart.",
    "436649998888": "erfundene Nummer fuer die Sperrliste, reine Wiederholziffern.",
    "436809999999": "erfundene Nummer fuer den Nachrichtenfilter, reine Wiederholziffern.",
    "43512999888": "erfundene Festnetznummer. 0512 ist die Ortsvorwahl von Innsbruck und "
                   "steht hier, weil die Regel das Gruppieren nach Ortsnetz prueft; die "
                   "Teilnehmernummer 999888 ist ausgedacht.",
    "4351299988": "dieselbe Nummer, beschnitten geprueft.",
    "512999888": "dieselbe Nummer ohne Landesvorwahl.",
    "43660123": "erfundenes Bruchstueck aus einem Test zur Eingabe halber Nummern.",
    "43660111001": "erfundene Nummer in `CallBackTest`, gleiche Machart wie 43664111001.",
    "436601234568": "erfundene **zweite** Nummer in `RespondNoticeTest`. Sie steht genau "
                    "eine Ziffer neben der ersten, weil die Regel prueft, dass zwei "
                    "Unterhaltungen verschiedene Meldungsnummern bekommen.",
    "436641234568": "dieselbe Machart in `CallBlockingTest`: eine Nummer, die der "
                    "gesperrten aehnelt und trotzdem nicht gesperrt sein darf.",
    "43664222": "zweite abgeschnittene Nummer in `CallLogGroupingTest`, Gegenstueck zu "
                "43664111. Geprueft wird nur, dass zwei verschiedene Nummern nicht in "
                "dieselbe Gruppe fallen.",
    "49301234": "kein Fund, sondern der Anfang einer erzeugten Nummer: "
                "`tools/anonymise-config.py` haengt vier Ziffern an und ersetzt damit "
                "echte Nummern in einer Sicherung. 030 ist das Berliner Ortsnetz und "
                "1234xxxx eine Reihe, die niemandem gehoert.",
    "4366412345": "abgeschnittene Platzhalternummer aus einem alten Test zur Eingabe "
                  "halber Nummern. Die volle Form ist +43 664 1234567.",
    "43664111": "abgeschnittene erfundene Nummer in `ContactSortTest`. Geprueft wird nur, "
                "ob Leerzeichen und Plus als Teil einer Nummer gelten.",
    "436601234": "dasselbe Bruchstueck, eine Ziffer weiter.",
    "43660124": "das Gegenstueck dazu in einem aelteren `SmsThreadsTest`: zwei Nummern, die "
                "sich in der letzten Ziffer unterscheiden und darum nicht dieselbe "
                "Unterhaltung sein duerfen.",

    # --- Koordinaten. Wahrzeichen, keine Adressen: an einem Wahrzeichen wohnt niemand.
    "33.86880,151.20930": "Opernhaus Sydney, das Lehrbuchbeispiel fuer eine negative Breite.",
    "33.8688,151.2093": "dasselbe, ungerundet.",
    "48.20849,16.37208": "Stephansplatz in Wien. Steht hier, weil der Test einen Ort in "
                         "Europa braucht, an dem das deutsche Komma als Dezimaltrenner "
                         "auffiele. Vorher stand hier eine Innsbrucker Koordinate, die eine "
                         "Wohnadresse haette sein koennen.",

    # --- Adressen
    "tim@kicker.dev": "die oeffentliche Adresse des Autors, steht schon in seinen anderen "
                      "Repositories.",
    "noreply@github.com": "GitHub selbst.",

    # --- Pfade
    "/home/runner/": "der Arbeitsordner der GitHub-Werkstatt, kein Mensch.",
}

ERLAUBTE_ENDEN = (
    "@example.at", "@example.com", "@example.org", "@example.de",
)


def _folge(ziffern: str) -> bool:
    """Ist der Teilnehmerteil eine gerade Ziffernreihe oder eine einzige Ziffer?

    Das ist keine Heuristik, sondern eine Form: die Nummer endet auf mindestens fuenf
    gleiche Ziffern (`...11111`) oder auf mindestens sechs aufeinanderfolgende
    (`...1234567`, `...7654321`). So schreibt man Platzhalter, und so schreibt niemand
    eine Nummer ab.

    Die erste Fassung dieser Pruefung liess alles durch, was hoechstens drei verschiedene
    Ziffern hatte. Das war zu weit: eine echte Nummer mit wenig Abwechslung waere
    mitgegangen. Was hier nicht passt, gehoert in ERLAUBT - eine Zeile, ein Grund.
    """
    if len(ziffern) < 6:
        return False
    if len(set(ziffern[-5:])) == 1:
        return True
    for laenge in range(len(ziffern), 5, -1):
        teil = ziffern[-laenge:]
        schritte = {ord(b) - ord(a) for a, b in zip(teil, teil[1:])}
        if schritte in ({1}, {-1}):
            return True
    return False


def erlaubt(art: str, fund: str) -> bool:
    """Was hier durchkommt, steht in ERLAUBT mit Grund oder hat die Form eines Platzhalters."""
    schmal = fund.strip().rstrip(".,;:)")
    if schmal in ERLAUBT:
        return True
    if art == "E-Mail":
        return any(schmal.endswith(e) for e in ERLAUBTE_ENDEN)
    if art == "Rufnummer":
        ziffern = re.sub(r"\D", "", schmal)
        return (ziffern in ERLAUBT
                or ziffern.lstrip("0") in ERLAUBT
                or _folge(ziffern))
    if art == "Koordinate":
        return re.sub(r"\s", "", schmal) in ERLAUBT
    return False


def pruefe(text: str, wo: str) -> list:
    treffer = []
    for name, muster in MUSTER:
        for m in muster.finditer(text):
            fund = m.group(0)
            if erlaubt(name, fund):
                continue
            zeile = text.count("\n", 0, m.start()) + 1
            treffer.append((name, fund.strip(), wo, zeile))
    return treffer


def git(*args) -> str:
    return subprocess.run(["git", *args], capture_output=True, text=True).stdout


def baum() -> list:
    treffer = []
    for pfad in git("ls-files").splitlines():
        if pfad == PROBENDATEI:
            continue
        for name, grund in VERBOTENE_DATEIEN.items():
            if pfad == name or pfad.endswith("/" + name):
                treffer.append(("Verbotene Datei", pfad, pfad, 0))
        for endung, grund in VERBOTENE_ENDUNGEN.items():
            if pfad.endswith(endung) and "debug" not in pfad:
                treffer.append(("Verbotene Datei", pfad, pfad, 0))
        # von der Platte, nicht aus HEAD: geprueft gehoert, was gleich hineingeht,
        # nicht was zuletzt hineinging.
        try:
            text = io.open(pfad, encoding="utf-8").read()
        except (UnicodeDecodeError, FileNotFoundError, IsADirectoryError):
            continue
        treffer += pruefe(text, pfad)
    return treffer


def geschichte(ref: str = "") -> list:
    """Jeder Blob genau einmal - nicht jeder Commit, das waere dieselbe Datei hundertfach.

    Ohne `ref` wird alles geprueft, was im Objektspeicher liegt. Mit `ref` nur das, was von
    diesem Zweig aus erreichbar ist - das ist die Frage vor einem Push: was geht wirklich
    hinaus. Ein alter Zweig, der liegen bleibt, gehoert nicht dazu.
    """
    if ref:
        zeilen = subprocess.run(["git", "rev-list", "--objects", ref],
                                capture_output=True, text=True).stdout.splitlines()
        kandidaten = [z.split()[0] for z in zeilen if z.strip()]
        arten = subprocess.run(["git", "cat-file", "--batch-check"],
                               input="\n".join(kandidaten), capture_output=True,
                               text=True).stdout.splitlines()
        blobs = [z.split()[0] for z in arten if z.split()[1:2] == ["blob"]]
    else:
        zeilen = subprocess.run(
            ["git", "cat-file", "--batch-check", "--batch-all-objects"],
            capture_output=True, text=True).stdout.splitlines()
        blobs = [z.split()[0] for z in zeilen if z.split()[1:2] == ["blob"]]
    # der Inhalt der Probendatei ist in jedem Commit derselbe und kein Fund
    probentext = io.open(PROBENDATEI, encoding="utf-8").read()
    gesehen, treffer = set(), []
    for h in blobs:
        roh = subprocess.run(["git", "cat-file", "blob", h], capture_output=True).stdout
        if b"\0" in roh[:8000]:
            continue
        try:
            text = roh.decode("utf-8")
        except UnicodeDecodeError:
            continue
        if text == probentext:
            continue
        for name, fund, _, _ in pruefe(text, h):
            if (name, fund) not in gesehen:
                gesehen.add((name, fund))
                treffer.append((name, fund, f"Blob {h[:10]}", 0))
    # welche Dateien je im Baum lagen
    pfade = git("log", ref or "--all", "--pretty=format:", "--name-only").split("\n")
    for pfad in set(pfade):
        for name in VERBOTENE_DATEIEN:
            if pfad == name or pfad.endswith("/" + name):
                treffer.append(("Verbotene Datei", pfad, "in der Geschichte", 0))
    return treffer


# Eine schmutzige Probe, an der sich die Suche selbst messen laesst. Jede Art muss
# gefunden werden; findet eine Art nichts, ist ihr Muster kaputt und die Suche sagt nur
# noch nichts mehr, statt nichts zu finden.
# Die schmutzige Probe steht in einer eigenen Datei, und die ist die **einzige**, die der
# Suchlauf ueberspringt. Stuende sie hier, meldete das Werkzeug sich selbst; stuende sie
# nirgends, koennte niemand pruefen, ob die Suche noch sucht.
PROBENDATEI = "tools/opsec-probe.txt"


def proben() -> dict:
    paare = {}
    for zeile in io.open(PROBENDATEI, encoding="utf-8").read().splitlines():
        if zeile.startswith("#") or "|" not in zeile:
            continue
        art, satz = zeile.split("|", 1)
        paare[art] = satz
    return paare


def probe() -> int:
    """Gegenprobe: findet die Suche noch, wonach sie sucht?"""
    print("\n=== Gegenprobe ===")
    schlecht = 0
    for art, satz in proben().items():
        gefunden = [a for a, _, _, _ in pruefe(satz, "probe") if a == art]
        if gefunden:
            print(f"  {art:18s} gefunden")
        else:
            print(f"  {art:18s} NICHT GEFUNDEN - das Muster ist kaputt")
            schlecht = 1
    return schlecht


def bericht(titel: str, treffer: list) -> int:
    print(f"\n=== {titel} ===")
    if not treffer:
        print("  nichts gefunden")
        return 0
    for art, fund, wo, zeile in sorted(set(treffer)):
        ort = f"{wo}:{zeile}" if zeile else wo
        print(f"  {art:18s} {fund:34s} {ort}")
    print(f"  {len(set(treffer))} Funde")
    return 1


def main() -> int:
    was = sys.argv[1] if len(sys.argv) > 1 else "beides"
    ref = sys.argv[2] if len(sys.argv) > 2 else ""
    schlecht = 0
    if was in ("probe", "beides", "baum"):
        schlecht |= probe()
    if was in ("baum", "beides"):
        schlecht |= bericht("Arbeitsbaum", baum())
    if was in ("geschichte", "beides"):
        schlecht |= bericht(f"Geschichte {ref or 'aller Zweige'}", geschichte(ref))
    if schlecht:
        print("\nNICHT veroeffentlichen. Jeder Fund gehoert weg oder mit Grund in ERLAUBT.")
    else:
        print("\nsauber")
    return schlecht


if __name__ == "__main__":
    sys.exit(main())
