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

## `gleiche-namen.py`

Sucht anklickbare Flächen, die auf demselben Bildschirm **denselben Namen** tragen.

```sh
tools/gleiche-namen.py emulator-5554
```

Zwei Angebote, die gleich heissen, sind für jemanden mit einem Vorleseprogramm dasselbe
Angebot; unterschieden werden sie dann durch etwas, das nicht gesagt wird — eine Farbe, ein
Symbol, die Reihenfolge. Der Name einer Fläche ist ihre `content-desc`, sonst der Text der
Knoten darin; das ist dieselbe Regel, nach der ein Vorleseprogramm vorgeht.

Ein Treffer ist ein Anfangsverdacht. Doppelknoten auf **denselben** Massen — Compose legt
für eine Zeile oft einen anklickbaren und einen benannten an — rechnet das Werkzeug selbst
heraus und sagt am Ende, wie viele es waren.

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

## `kleine-knoepfe.py`

Sucht antippbare Flächen unter 48 dp auf dem gerade sichtbaren Bildschirm.

```sh
tools/kleine-knoepfe.py <seriennummer>
```

48 dp ist das Mindestmaß für einen Fingertipp; auf dem Jelly 2 (220 dpi) sind das 66
Bildpunkte. Kein Unit-Test sieht die fertige Fläche — sie entsteht erst aus Schrift, Füllung
und dem Platz, der übrig bleibt.

Zeilen am Rand zählt das Werkzeug **nicht** mit — sie sind angeschnitten, nicht klein, und
was `uiautomator` meldet, ist dort nicht das Gezeichnete. Zwei Ränder gibt es: den des
**Fensters** (oben wie unten) und den einer **Liste** — die letzte Zeile darin ragt fast
immer darüber hinaus. Zu **schmale** Flächen bleiben trotzdem ein Befund, auch am Rand: die
Breite kann vom Anschneiden nicht kommen. Wie viele herausgerechnet wurden, steht als
Nachsatz dabei.

Was übrig bleibt, ist ein Anfangsverdacht, kein Urteil. Der häufigste Fall, der keiner ist:
ein Eingabefeld, das kleiner ist als die Zeile, die es zeigt. Dann kommt es darauf an, ob die
ganze Zeile den Tipp annimmt. Am Gerät prüfen:
`adb shell dumpsys input_method | grep mInputShown` vor und nach dem Tipp.

Am 04.09.2026 damit die Suchzeile der App-Liste gefunden: gezeichnet 64 dp, Feld darin 48 dp
(mit Trefferzahl darunter nur 38), und ein Tipp auf die oberen vierzehn Bildpunkte tat
nichts. Seitdem nimmt die ganze Zeile den Tipp an.

## `kontrast.py`

Misst den Kontrast dort, wo er ankommt: in einem Bildschirmfoto.

```sh
adb -s <seriennummer> exec-out screencap -p > home.png
tools/kontrast.py home.png 20 284 130 314        # die Beschriftungszone einer Kachel
tools/kontrast.py home.png --grund 240 45 28 59 200 103   # Text gegen den Hintergrund
```

`SurfaceContrastTest` prüft die Farbkonstanten gegeneinander — jedes Flächenpaar jedes
Themas. Was daraus auf dem Bildschirm wird, steht auf einem anderen Blatt: eine Kachel kann
eine eigene Farbe tragen, ein Foto, einen Verlauf darüber, und die Schrift wird mit
Kantenglättung gezeichnet. Dieses Werkzeug liest die Bildpunkte und rechnet nach WCAG.

Für jedes Rechteck gibt es den hellsten und den dunkelsten Bildpunkt darin — in einem Feld
mit Text sind das die Schrift und ihr Grund. Wo im Rechteck fast nur Schrift liegt, nennt
man mit `--grund` einen Punkt daneben.

Die Schwellen stehen in `Tokens.kt`: **4,5** für Schrift auf einer Kachel, **3,0** für eine
Kachel gegen den Hintergrund, **7,0** für Text außerhalb einer Kachel.

Am 04.09.2026 damit am Jelly 2 nachgemessen, alle acht Kacheln des Startbildschirms:
dunkel 5,61–5,67:1, hell 7,66–7,87:1, Kontrast-Thema 17,20:1 für alle acht. Die Kopfzeile
liegt bei 18,1 / 15,7 / 17,2. Damit ist die Zusage aus `PLAN.md` („alle sechs liegen
absichtlich auf demselben Kontrastniveau") nicht nur in den Konstanten wahr, sondern auch
auf dem Glas.

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

### Wischen

```sh
tools/tippen.py <seriennummer> wischen AppDrawerActivity 14
```

Rollt in einer langen Liste nach unten und sieht vor **jedem** Zug nach, ob noch dieselbe
Activity offen ist. Anfang und Ende bleiben im mittleren Drittel des Bildschirms, die Dauer
ist 350 ms.

Der Grund steht in beiden Zahlen: unten sitzt die Gestenzone des Systems, oben die
Benachrichtigungsleiste, und ein schneller Wisch wird als Geste gelesen statt als Rollen.
Am 03.09.2026 um 09:52 schickte ich zehn Wischer ab y=750 mit 80 ms — die
Benachrichtigungsleiste ging auf, der Bildschirm aus, das Telefon sperrte sich, und danach
stand eine fremde App im Vordergrund.

## `status-eintrag.py`

Schreibt einen Abschnitt in `STATUS.md` — und sieht nach, ob er wirklich dasteht.

```sh
echo "Rumpf des Abschnitts" | tools/status-eintrag.py "## 🔧 Ueberschrift (03.09.2026, 14:05)"
```

Der Abschnitt kommt direkt unter die Marke `<!-- chronik:` in `STATUS.md`; dort fängt die
Chronik an. Danach liest das Skript die Datei **neu** und zählt die Überschrift: genau
einmal, sonst Rückgabewert 1 und kein Wort davon, dass es geklappt hätte. Es weigert sich
auch bei leerem Rumpf, bei einer Überschrift ohne `##`, bei einer Überschrift, die schon
dasteht, und wenn die Marke fehlt.

Entstanden am 03.09.2026 um 14:05 aus einem stillen Verlust: ein Bash-Aufruf mit deutschen
Anführungszeichen in einem Python-Schnipsel scheiterte an der Shell (`unmatched '`) —
**bevor irgendetwas lief**. Danach nur den Rest wiederholt und committet; dabei fehlten zwei
README-Korrekturen und ein ganzer STATUS-Abschnitt. Aufgefallen erst einen Takt später, weil
eine Überschrift nicht zu finden war. Ein fehlgeschlagener Befehl sieht einem erledigten zum
Verwechseln ähnlich, wenn man nur auf den Commit schaut.

Die erste Fassung suchte nach dem „ersten datierten Abschnitt" statt nach einer Marke und
traf daneben: die bleibenden Abschnitte oben (Sperre, Geräteliste, Übergabe) tragen selbst
Daten. Eine ausdrückliche Marke ist langweiliger und richtig.

## `echte-fassung.sh`

Prüft die **echte** Konfiguration vom Telefon gegen den Import-Weg.

```sh
tools/echte-fassung.sh <seriennummer>
```

`RealConfigRoundTripTest` braucht eine gewachsene Konfiguration — mehrere Bildschirme, ein
Ordner, eigene Beschriftungen, Farben. Die entsteht nicht im Testquelltext, sondern über
Wochen auf einem Telefon, und sie darf nicht ins Repository: sie enthält die App-Liste und
die Bildschirmnamen eines Menschen. Das Skript holt sie sich, legt sie **ausserhalb** des
Projekts ab (`mktemp`) und löscht sie wieder.

Am Ende sieht es im Testbericht nach `skipped="0"`. Das ist der eigentliche Punkt: ein
übersprungener Test ist auch grün. Bis zum 03.09.2026 hat dieser sich seit seiner
Entstehung jedes Mal selbst übersprungen, ohne dass es auffiel — der Umzug auf ein neues
Telefon, der Grund für die ganze Sicherungsfunktion, war ungeprüft. Am 03.09.2026 um 11:10
zum ersten Mal wirklich gelaufen, mit der Fassung des Jelly 2 (13 194 Bytes, drei
Bildschirme, vierzehn Kacheln): bestanden.

## `nachbauen.sh`

Baut die Release-Fassung zweimal ohne Build-Cache und vergleicht die Prüfsummen.

```sh
tools/nachbauen.sh
```

Der Nachweis, den der README des Projekts behauptet: derselbe Quelltext ergibt dieselbe
Datei. Wer das nachrechnet, will nicht zwei Befehle abtippen und Zeichenketten mit dem Auge
vergleichen — dabei übersieht man genau die eine Stelle, an der sie sich unterscheiden. Am
Ende steht ausserdem der Commit, zu dem die Zahl gehört, und ob im Arbeitsverzeichnis noch
ungespeicherte Änderungen liegen.

Seit dem 03.09.2026 passt das Skript auf sich selbst auf: es merkt sich vor dem ersten Lauf
`HEAD` und `git status --porcelain` und vergleicht danach. Hat sich dazwischen etwas
geändert, sagt es „das Ergebnis sagt nichts aus" statt einer Prüfsumme. Und wenn die beiden
Läufe wirklich verschieden sind, fragt es zurück, ob nebenher ein anderer `./gradlew` lief.

Der Grund steht in der Datei: um 10:57 meldete es „nicht reproduzierbar", weil ich daneben
Tests laufen liess und zur Gegenprobe kurz das README verbog. Allein wiederholt, derselbe
Commit, zweimal dieselbe Prüfsumme
(`cd262f2a9f451c605b49eca93a039e98ca9fb0ad5badc58dbfd3df433f8c6ec2`, 1 790 879 Bytes für
`0c3915e`). Ein falsches „nicht reproduzierbar" ist die schlimmste Antwort von allen: sie
lässt an einer Zusage zweifeln, die stimmt.

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
