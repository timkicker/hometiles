#!/usr/bin/env python3
"""Aus einer echten Konfiguration eine Prüffassung machen.

Der Umzugstest braucht eine *gewachsene* Konfiguration - mehrere Bildschirme, ein
Ordner, gemischte Kachelarten. Die echte Datei darf nicht ins Repository: sie verrät,
welche Programme auf dem Telefon liegen. Dieses Werkzeug behält die Mischung und wirft
die Person weg.

    tools/anonymise-config.py /pfad/config.json > core/model/src/test/resources/gewachsene-fassung.json

Ersetzt werden: Programmnamen, zuletzt benutzte Programme, Kurzwahl, gesperrte und
versteckte Nummern, SOS-Nummern, die PIN und der Zeitstempel der zuletzt gesehenen
verpassten Anrufe. Alles andere - Anzahl, Lage und Grösse der Kacheln, Farben,
Schalterstellungen - bleibt Zeichen für Zeichen stehen.
"""
import json
import sys

paket_nummern = {}


def paket(name):
    if name not in paket_nummern:
        paket_nummern[name] = f"com.example.programm{len(paket_nummern) + 1}"
    return paket_nummern[name]


def nummer(index):
    return f"+49301234{index:04d}"


def anonymisieren(d):
    for schirm in d.get("screens", []):
        for zelle in schirm.get("cells", []):
            aktion = zelle["button"]["action"]
            if aktion.get("type") == "app":
                alt = aktion["packageName"]
                aktion["packageName"] = paket(alt)
                if aktion.get("activityName"):
                    aktion["activityName"] = paket(alt) + ".MainActivity"
            if aktion.get("type") in ("call", "sms", "contact"):
                for feld in ("number", "uri", "lookupKey"):
                    if aktion.get(feld):
                        aktion[feld] = nummer(len(paket_nummern) + 1)
    apps = d.get("apps", {})
    apps["recent"] = [
        paket(e.split("/")[0]) + "/" + paket(e.split("/")[0]) + ".MainActivity"
        for e in apps.get("recent", [])
    ]
    apps["hidden"] = [paket(e) for e in apps.get("hidden", [])]
    telefon = d.get("phone", {})
    telefon["speedDial"] = {k: nummer(int(k) if str(k).isdigit() else 9) for k in telefon.get("speedDial", {})}
    telefon["blockedNumbers"] = [nummer(i) for i, _ in enumerate(telefon.get("blockedNumbers", []))]
    telefon["lastSeenMissedAt"] = 0
    sms = d.get("sms", {})
    sms["hiddenNumbers"] = [nummer(i) for i, _ in enumerate(sms.get("hiddenNumbers", []))]
    sms["hiddenWords"] = ["wort" for _ in sms.get("hiddenWords", [])]
    d.get("sos", {})["numbers"] = [nummer(i) for i, _ in enumerate(d.get("sos", {}).get("numbers", []))]
    d.get("security", {})["pin"] = None
    return d


if __name__ == "__main__":
    quelle = json.load(open(sys.argv[1], encoding="utf-8"))
    print(json.dumps(anonymisieren(quelle), indent=1, ensure_ascii=False))
