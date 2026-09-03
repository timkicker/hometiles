# Werkzeuge fürs Prüfen am Gerät

Kleine Programme fürs Prüfen am Gerät — für Dinge, die kein Unit-Test sehen kann, weil sie erst auf dem fertig
gezeichneten Bildschirm oder auf einem gewachsenen Telefon entstehen. Die ersten beiden
brauchen nur `adb`.

## `bildschirm-wache.sh`

Schreibt jede Änderung des Bildschirmzustands mit — mit Grund, Ladestand und Schloss.

```sh
tools/bildschirm-wache.sh <seriennummer> wache.log
```

Entstanden aus einer Beschwerde, die sich nicht nachstellen ließ: „es wird immer wieder
dunkel." Ein Bildschirmfoto beantwortet das nicht, denn im Moment des Fotos sieht man ja
hin. Die Wache sieht alle fünfzehn Sekunden nach und schreibt **nur die Änderungen** auf —
eine ruhige Nacht ist damit eine Datei mit einer Zeile.

Zwei Angaben mehr, beide aus dem Gebrauch entstanden: `runden = 0` läuft, bis jemand die
Wache abbricht (die Vorgabe hört nach einer Stunde auf), und alle hundert Runden schreibt
sie ein Lebenszeichen. Ohne das sieht eine abgestürzte Wache genauso aus wie eine ruhige
Nacht — beide Male steht nichts da. Am 3.9.2026 wurde deshalb eine laufende Wache für tot
gehalten.

```sh
tools/bildschirm-wache.sh <seriennummer> wache.log 0 20   # ganze Nacht, alle 20 s
```

## `stumme-knoepfe.py`

Sucht anklickbare Flächen ohne Namen auf dem gerade sichtbaren Bildschirm.

```sh
tools/stumme-knoepfe.py <seriennummer>
```

Das ist der Knopf, den ein Mensch mit einem Vorleseprogramm findet und über den nichts
gesagt wird. Ein Treffer ist ein Anfangsverdacht, kein Urteil: der Name kann auf einem
Knoten *um* die Fläche herum liegen oder auf einem zweiten mit **denselben** Maßen — Compose
legt für eine Kachel beide an. Deshalb gehört zu jedem Treffer ein Blick aufs Bildschirmfoto.
(Die erste Fassung dieses Skripts liess den Fall der gleichen Maße nicht zu und meldete
daraufhin drei Kacheln des Nutzers als stumm. Am Bild gesehen, dass die Meldung falsch war.)

Am 03.09.2026 über sechs Bildschirme gelaufen (Startbildschirm, Ordner, Kontakte,
Wähltastatur, Anrufliste, App-Liste): kein stummer Knopf. Um 05:45 dazu die
**Einstellungen** — Wurzelseite, Aussehen, App-Liste, PIN-Schutz, Anrufliste, Nachrichten,
Kontakte, Sicherung, Diagnose, jede über ihre ganze Länge geblättert: ebenfalls keiner. Sie
waren die letzte ungeprüfte Gruppe, und zwar weil man von diesem Telefon aus gar nicht
hinkam (siehe `STATUS.md`, 05:25).

Dabei hat das Werkzeug zweimal falsch angeschlagen, beide Male auf halb hereingeblätterte
Zeilen: eine Fläche am Rand ist abgeschnitten, ihre Beschriftung liegt gar nicht mehr im
Baum. Solche Flächen stehen jetzt getrennt unter „am Rand abgeschnitten". Der Maßstab dafür
ist nicht eine feste Pixelzahl — die erste Fassung hatte 33 und liess den zweiten Fall (60
Pixel) durch —, sondern die mittlere Höhe der anklickbaren Zeilen dieses Bildschirms.

## `tippen.py`

Tippt erst, wenn der Bildschirm der erwartete ist.

```sh
tools/tippen.py <seriennummer> 123 715 MainActivity Apps
```

Drei Prüfungen vor jedem Tipp: ein frisches Bildschirmfoto wird geholt, `mResumedActivity`
muss den erwarteten Namen enthalten, und — wenn angegeben — muss die Beschriftung an genau
dieser Stelle stehen. Passt etwas nicht, wird **nicht** getippt, und es steht da, was
stattdessen offen ist.

Entstanden am 03.09.2026 aus einem Fehlgriff: nach zwei Mal „Zurück" war statt der
Einstellungsseite die App-Liste offen. `mResumedActivity` sagte das auch — gelesen, nicht
beachtet, alte Koordinate benutzt, fremde App geöffnet. Die Regel „vor jedem Tippen ein
frisches Foto und ein Blick auf mResumedActivity" gab es da schon; sie hing nur an der
Aufmerksamkeit dessen, der tippt. Jetzt hängt sie an einem Programm.

Nachgestellt: derselbe Griff mit diesem Werkzeug verweigert den Tipp.

## `fassung-anonymisieren.py`

Macht aus der Konfiguration eines echten Telefons eine Prüfdatei fürs Repository.

```sh
tools/fassung-anonymisieren.py ~/config.json > core/model/src/test/resources/gewachsene-fassung.json
```

Der Umzugstest braucht eine *gewachsene* Konfiguration — mehrere Bildschirme, ein Ordner,
gemischte Kachelarten. Die synthetischen Prüfungen daneben decken jedes Feld ab, aber nicht
die Mischung, die über Wochen entsteht. Die echte Datei darf nicht ins Repository: sie
verrät, welche Programme auf dem Telefon liegen. Dieses Werkzeug behält die Mischung
(Anzahl, Lage und Größe der Kacheln, Farben, alle Schalterstellungen) und ersetzt
Programmnamen, Nummern, Kurzwahl und PIN.

Vorher gab es dafür nur `RealConfigRoundTripTest`, der sich ohne die echte Datei
stillschweigend übersprang — und das tat er seit seiner Entstehung. Die Prüfdatei läuft bei
jedem Lauf mit (`GewachseneFassungTest`).
