# Werkzeuge fürs Prüfen am Gerät

Kleine Programme fürs Prüfen am Gerät, für Dinge, die kein Unit-Test sehen kann, weil sie erst auf dem fertig
gezeichneten Bildschirm oder auf einem gewachsenen Telefon entstehen. Die ersten beiden
brauchen nur `adb`.

## `opsec.py`

Sucht personenbezogene Daten und Geheimnisse - **vor jeder Veröffentlichung**.

```sh
tools/opsec.py             # Arbeitsbaum und ganze Geschichte
tools/opsec.py baum        # nur die Dateien, die git heute kennt
tools/opsec.py geschichte  # jeder Blob in jedem Commit auf jedem Zweig
tools/opsec.py probe       # Gegenprobe: findet die Suche noch, wonach sie sucht?
```

Gesucht wird nach Rufnummern, Koordinaten, E-Mail-Adressen, Gerätekennungen,
MAC-Adressen, Zugangsschlüsseln und Heimatpfaden, dazu nach Dateien, die nie hineingehören
(`STATUS.md`, `local.properties`, Signierschlüssel). Rückgabewert 1 bei jedem Fund.

**Warum die Geschichte mitgeprüft wird:** ein Push veröffentlicht alle Commits auf einmal.
Eine Nummer, die vor dreihundert Commits drinstand und längst gelöscht ist, steht danach
trotzdem für immer im Netz.

**Warum es das Werkzeug gibt.** Am 06.09.2026, einen Befehl vor `gh repo create --public`,
lagen drei echte Mobilnummern aus dem Adressbuch des Nutzers im Repository: im Arbeitstagebuch,
in einem Kommentar im Quelltext und in 815 Commits dahinter. Gefunden wurden sie, weil von
Hand nachgesehen wurde. Von Hand nachsehen ist keine Prüfung.

Jeder Fund muss verschwinden oder in `ERLAUBT` stehen, **mit Grund**. Eine Ausnahmeliste
ohne Grund ist eine Abschaltung mit Umweg. Rufnummern in Platzhalterform (`...1234567`,
`...99999`) kommen ohne Eintrag durch; was diese Form nicht hat, braucht eine Zeile.

`OpsecTest` im Regelwerk ruft dasselbe Werkzeug auf, damit nichts zweimal geschrieben steht,
und prüft mit `probe` zugleich, dass die Suche nicht bloß nichts mehr findet.

## `screen-watch.sh`

Schreibt jede Änderung des Bildschirmzustands mit, mit Grund, Ladestand und Schloss.

```sh
tools/screen-watch.sh <seriennummer> wache.log
```

Entstanden aus einer Beschwerde, die sich nicht nachstellen ließ: „es wird immer wieder
dunkel." Ein Bildschirmfoto beantwortet das nicht, denn im Moment des Fotos sieht man ja
hin. Die Wache sieht alle fünfzehn Sekunden nach und schreibt **nur die Änderungen** auf,
eine ruhige Nacht ist damit eine Datei mit einer Zeile.

Zwei Angaben mehr, beide aus dem Gebrauch entstanden: `runden = 0` läuft, bis jemand die
Wache abbricht (die Vorgabe hört nach einer Stunde auf), und alle hundert Runden schreibt
sie ein Lebenszeichen. Ohne das sieht eine abgestürzte Wache genauso aus wie eine ruhige
Nacht, beide Male steht nichts da. Am 3.9.2026 wurde deshalb eine laufende Wache für tot
gehalten.

```sh
tools/screen-watch.sh <seriennummer> wache.log 0 20   # ganze Nacht, alle 20 s
```

## `same-names.py`

Sucht anklickbare Flächen, die auf demselben Bildschirm **denselben Namen** tragen.

```sh
tools/same-names.py emulator-5554
```

Zwei Angebote, die gleich heissen, sind für jemanden mit einem Vorleseprogramm dasselbe
Angebot; unterschieden werden sie dann durch etwas, das nicht gesagt wird: eine Farbe, ein
Symbol, die Reihenfolge. Der Name einer Fläche ist ihre `content-desc`, sonst der Text der
Knoten darin; das ist dieselbe Regel, nach der ein Vorleseprogramm vorgeht.

Ein Treffer ist ein Anfangsverdacht. Doppelknoten auf **denselben** Massen, die Compose
für eine Zeile oft anlegt (einen anklickbaren und einen benannten), rechnet das Werkzeug selbst
heraus und sagt am Ende, wie viele es waren.

## `silent-buttons.py`

Sucht anklickbare Flächen ohne Namen auf dem gerade sichtbaren Bildschirm.

```sh
tools/silent-buttons.py <seriennummer>
```

Das ist der Knopf, den ein Mensch mit einem Vorleseprogramm findet und über den nichts
gesagt wird. Ein Treffer ist ein Anfangsverdacht, kein Urteil: der Name kann auf einem
Knoten *um* die Fläche herum liegen oder auf einem zweiten mit **denselben** Maßen, Compose
legt für eine Kachel beide an. Deshalb gehört zu jedem Treffer ein Blick aufs Bildschirmfoto.
(Die erste Fassung dieses Skripts liess den Fall der gleichen Maße nicht zu und meldete
daraufhin drei Kacheln des Nutzers als stumm. Am Bild gesehen, dass die Meldung falsch war.)

Am 03.09.2026 über sechs Bildschirme gelaufen (Startbildschirm, Ordner, Kontakte,
Wähltastatur, Anrufliste, App-Liste): kein stummer Knopf. Um 05:45 dazu die
**Einstellungen**: Wurzelseite, Aussehen, App-Liste, PIN-Schutz, Anrufliste, Nachrichten,
Kontakte, Sicherung, Diagnose, jede über ihre ganze Länge geblättert: ebenfalls keiner. Sie
waren die letzte ungeprüfte Gruppe, und zwar weil man von diesem Telefon aus gar nicht
hinkam (siehe `STATUS.md`, 05:25).

Dabei hat das Werkzeug zweimal falsch angeschlagen, beide Male auf halb hereingeblätterte
Zeilen: eine Fläche am Rand ist abgeschnitten, ihre Beschriftung liegt gar nicht mehr im
Baum. Solche Flächen stehen jetzt getrennt unter „am Rand abgeschnitten". Der Maßstab dafür
ist nicht eine feste Pixelzahl (die erste Fassung hatte 33 und liess den zweiten Fall mit 60
Pixeln durch), sondern die mittlere Höhe der anklickbaren Zeilen dieses Bildschirms.

## `small-buttons.py`

Sucht antippbare Flächen unter 48 dp auf dem gerade sichtbaren Bildschirm.

```sh
tools/small-buttons.py <seriennummer>
```

48 dp ist das Mindestmaß für einen Fingertipp; auf dem Jelly 2 (220 dpi) sind das 66
Bildpunkte. Kein Unit-Test sieht die fertige Fläche, sie entsteht erst aus Schrift, Füllung
und dem Platz, der übrig bleibt.

Zeilen am Rand zählt das Werkzeug **nicht** mit, sie sind angeschnitten, nicht klein, und
was `uiautomator` meldet, ist dort nicht das Gezeichnete. Zwei Ränder gibt es: den des
**Fensters** (oben wie unten) und den einer **Liste**, die letzte Zeile darin ragt fast
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

## `contrast.py`

Misst den Kontrast dort, wo er ankommt: in einem Bildschirmfoto.

```sh
adb -s <seriennummer> exec-out screencap -p > home.png
tools/contrast.py home.png 20 284 130 314        # die Beschriftungszone einer Kachel
tools/contrast.py home.png --grund 240 45 28 59 200 103   # Text gegen den Hintergrund
```

`SurfaceContrastTest` prüft die Farbkonstanten gegeneinander, jedes Flächenpaar jedes
Themas. Was daraus auf dem Bildschirm wird, steht auf einem anderen Blatt: eine Kachel kann
eine eigene Farbe tragen, ein Foto, einen Verlauf darüber, und die Schrift wird mit
Kantenglättung gezeichnet. Dieses Werkzeug liest die Bildpunkte und rechnet nach WCAG.

Für jedes Rechteck gibt es den hellsten und den dunkelsten Bildpunkt darin, in einem Feld
mit Text sind das die Schrift und ihr Grund. Wo im Rechteck fast nur Schrift liegt, nennt
man mit `--grund` einen Punkt daneben.

Die Schwellen stehen in `Tokens.kt`: **4,5** für Schrift auf einer Kachel, **3,0** für eine
Kachel gegen den Hintergrund, **7,0** für Text außerhalb einer Kachel.

Am 04.09.2026 damit am Jelly 2 nachgemessen, alle acht Kacheln des Startbildschirms:
dunkel 5,61-5,67:1, hell 7,66-7,87:1, Kontrast-Thema 17,20:1 für alle acht. Die Kopfzeile
liegt bei 18,1 / 15,7 / 17,2. Damit ist die Zusage aus `PLAN.md` („alle sechs liegen
absichtlich auf demselben Kontrastniveau") nicht nur in den Konstanten wahr, sondern auch
auf dem Glas.

## `contrast-walk.py`

Misst den Kontrast **jeder** beschrifteten Fläche eines Bildschirms auf einmal.

```sh
tools/contrast-walk.py emulator-5554
```

`contrast.py` misst ein Rechteck, das man ihm nennt. Das ist richtig, wenn man eine Stelle im
Verdacht hat, und mühsam, wenn man einen ganzen Bildschirm durchsehen will: am 04.09.2026
waren es sechs Bildschirme mit zusammen über achtzig beschrifteten Flächen. Von Hand hieße
das, achtzig Rechtecke abzutippen, und wer abtippt, lässt welche aus, meist die
unauffälligen.

Deshalb kommen die Rechtecke aus dem Knotenabzug: jeder Knoten mit Text ist eine Stelle, an
der jemand etwas lesen muss. Gerechnet wird mit den Funktionen aus `contrast.py`, damit es
nur eine Formel gibt.

Am Ende steht immer die schlechteste Stelle, auch wenn sie besteht. Eine Messung, die nur
schweigt, sagt nicht, ob sie hingesehen hat.

Zwei Grenzen: gemessen wird der hellste gegen den dunkelsten Punkt im Rechteck, was bei
Feldern mit fast nur Schrift zu streng oder zu milde sein kann; dort ist `contrast.py
--grund` genauer. Und abgetastet wird jeder zweite Bildpunkt, sonst dauert ein Bildschirm
eine Minute.

Erster Durchgang am 04.09.2026 über sechs Bildschirme: Startbildschirm, Nachrichten,
Telefon, Kontakte, App-Liste, Einstellungen. Nichts unter 4,5:1, schlechteste Stelle 5,61:1.

## `tap.py`

Tippt erst, wenn der Bildschirm der erwartete ist.

```sh
tools/tap.py <seriennummer> 123 715 MainActivity Apps
```

Drei Prüfungen vor jedem Tipp: ein frisches Bildschirmfoto wird geholt, `mResumedActivity`
muss den erwarteten Namen enthalten, und, wenn angegeben, muss die Beschriftung an genau
dieser Stelle stehen. Passt etwas nicht, wird **nicht** getippt, und es steht da, was
stattdessen offen ist.

Entstanden am 03.09.2026 aus einem Fehlgriff: nach zwei Mal „Zurück" war statt der
Einstellungsseite die App-Liste offen. `mResumedActivity` sagte das auch, gelesen, nicht
beachtet, alte Koordinate benutzt, fremde App geöffnet. Die Regel „vor jedem Tippen ein
frisches Foto und ein Blick auf mResumedActivity" gab es da schon; sie hing nur an der
Aufmerksamkeit dessen, der tippt. Jetzt hängt sie an einem Programm.

Nachgestellt: derselbe Griff mit diesem Werkzeug verweigert den Tipp.

### Wischen

```sh
tools/tap.py <seriennummer> wischen AppDrawerActivity 14
```

Rollt in einer langen Liste nach unten und sieht vor **jedem** Zug nach, ob noch dieselbe
Activity offen ist. Anfang und Ende bleiben im mittleren Drittel des Bildschirms, die Dauer
ist 350 ms.

Der Grund steht in beiden Zahlen: unten sitzt die Gestenzone des Systems, oben die
Benachrichtigungsleiste, und ein schneller Wisch wird als Geste gelesen statt als Rollen.
Am 03.09.2026 um 09:52 schickte ich zehn Wischer ab y=750 mit 80 ms, die
Benachrichtigungsleiste ging auf, der Bildschirm aus, das Telefon sperrte sich, und danach
stand eine fremde App im Vordergrund.

## `log-entry.py`

Schreibt einen Abschnitt in `STATUS.md`, und sieht nach, ob er wirklich dasteht. `STATUS.md`
liegt in `.gitignore` und bleibt lokal - im Protokoll stehen Messungen vom echten Telefon.

```sh
echo "Rumpf des Abschnitts" | tools/log-entry.py "## 🔧 Ueberschrift (03.09.2026, 14:05)"
```

Der Abschnitt kommt direkt unter die Marke `<!-- chronik:` in `STATUS.md`; dort fängt die
Chronik an. Danach liest das Skript die Datei **neu** und zählt die Überschrift: genau
einmal, sonst Rückgabewert 1 und kein Wort davon, dass es geklappt hätte. Es weigert sich
auch bei leerem Rumpf, bei einer Überschrift ohne `##`, bei einer Überschrift, die schon
dasteht, und wenn die Marke fehlt.

Entstanden am 03.09.2026 um 14:05 aus einem stillen Verlust: ein Bash-Aufruf mit deutschen
Anführungszeichen in einem Python-Schnipsel scheiterte an der Shell (`unmatched '`),
**bevor irgendetwas lief**. Danach nur den Rest wiederholt und committet; dabei fehlten zwei
README-Korrekturen und ein ganzer STATUS-Abschnitt. Aufgefallen erst einen Takt später, weil
eine Überschrift nicht zu finden war. Ein fehlgeschlagener Befehl sieht einem erledigten zum
Verwechseln ähnlich, wenn man nur auf den Commit schaut.

Die erste Fassung suchte nach dem „ersten datierten Abschnitt" statt nach einer Marke und
traf daneben: die bleibenden Abschnitte oben (Sperre, Geräteliste, Übergabe) tragen selbst
Daten. Eine ausdrückliche Marke ist langweiliger und richtig.

## `real-config.sh`

Prüft die **echte** Konfiguration vom Telefon gegen den Import-Weg.

```sh
tools/real-config.sh <seriennummer>
```

`RealConfigRoundTripTest` braucht eine gewachsene Konfiguration: mehrere Bildschirme, ein
Ordner, eigene Beschriftungen, Farben. Die entsteht nicht im Testquelltext, sondern über
Wochen auf einem Telefon, und sie darf nicht ins Repository: sie enthält die App-Liste und
die Bildschirmnamen eines Menschen. Das Skript holt sie sich, legt sie **ausserhalb** des
Projekts ab (`mktemp`) und löscht sie wieder.

Am Ende sieht es im Testbericht nach `skipped="0"`. Das ist der eigentliche Punkt: ein
übersprungener Test ist auch grün. Bis zum 03.09.2026 hat dieser sich seit seiner
Entstehung jedes Mal selbst übersprungen, ohne dass es auffiel. Der Umzug auf ein neues
Telefon, der Grund für die ganze Sicherungsfunktion, war ungeprüft. Am 03.09.2026 um 11:10
zum ersten Mal wirklich gelaufen, mit der Fassung des Jelly 2 (13 194 Bytes, drei
Bildschirme, vierzehn Kacheln): bestanden.

## `rebuild.sh`

Baut die Release-Fassung zweimal ohne Build-Cache und vergleicht die Prüfsummen.

```sh
tools/rebuild.sh
```

Der Nachweis, den der README des Projekts behauptet: derselbe Quelltext ergibt dieselbe
Datei. Wer das nachrechnet, will nicht zwei Befehle abtippen und Zeichenketten mit dem Auge
vergleichen, dabei übersieht man genau die eine Stelle, an der sie sich unterscheiden. Am
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

## `anonymise-config.py`

Macht aus der Konfiguration eines echten Telefons eine Prüfdatei fürs Repository.

```sh
tools/anonymise-config.py ~/config.json > core/model/src/test/resources/gewachsene-fassung.json
```

Der Umzugstest braucht eine *gewachsene* Konfiguration: mehrere Bildschirme, ein Ordner,
gemischte Kachelarten. Die synthetischen Prüfungen daneben decken jedes Feld ab, aber nicht
die Mischung, die über Wochen entsteht. Die echte Datei darf nicht ins Repository: sie
verrät, welche Programme auf dem Telefon liegen. Dieses Werkzeug behält die Mischung
(Anzahl, Lage und Größe der Kacheln, Farben, alle Schalterstellungen) und ersetzt
Programmnamen, Nummern, Kurzwahl und PIN.

Vorher gab es dafür nur `RealConfigRoundTripTest`, der sich ohne die echte Datei
stillschweigend übersprang, und das tat er seit seiner Entstehung. Die Prüfdatei läuft bei
jedem Lauf mit (`GrownConfigTest`).

## `plurals.py`

Sagt, welche Mehrzahlformen eine Sprache wirklich braucht, aus CLDR statt geraten.

```sh
tools/plurals.py
tools/plurals.py es it pl
```

Android füllt eine fehlende Form stillschweigend aus `other` auf. Kein Absturz, sondern ein
grammatisch falscher Satz, den nur jemand bemerkt, der die Sprache spricht. Wer eine Sprache
anlegt, muss deshalb vorher wissen, welche Formen sie verlangt.

Die Liste der Kategorien allein reicht dabei nicht, und genau das macht dieses Werkzeug
anders. Französisch, Spanisch und Italienisch führen alle ein `many`, was nach Arbeit klingt;
nachgerechnet trifft es nur volle Millionen, und HomeTiles zählt Kacheln und Kontakte. Umgekehrt
brauchen Polnisch und Russisch `few`, `many` und `one` genau bei den alltäglichen Zahlen.
Deshalb steht hier nicht nur, welche Kategorien es gibt, sondern welche unterhalb von tausend
überhaupt vorkommen.

Die zweite Ausgabe ist die Falle, die man nicht sieht: welche Form die Null bekommt. Im
Französischen und Portugiesischen ist das dieselbe wie für eins, im Spanischen und
Italienischen nicht. Ich hatte das Gegenteil aufgeschrieben, bevor ich nachgesehen habe.

Braucht `python-babel`, das die CLDR-Regeln mitbringt.

## `dashes.py`

Nimmt die langen Gedankenstriche aus einer Textdatei, nach Muster statt Zeichen gegen Zeichen.

```sh
tools/dashes.py STATUS.md          # nur zaehlen
tools/dashes.py STATUS.md schreiben
```

Gedacht war es für einen einzigen Einsatz: 1470 Striche in 361 alten Einträgen von
`STATUS.md`. Von Hand wäre das tagelang gegangen, stumpf ersetzt wäre es falsch geworden.
Vier Fälle, in dieser Reihenfolge: zwei Striche in einer Zeile sind ein Einschub und bekommen
Klammern; steht dahinter eine Konjunktion, war es nie ein neuer Satz, sondern ein Komma;
kündigt davor etwas eine Aufzählung an, oder folgt ein Codeblock, oder steht das Ganze in
einer Überschrift, dann ein Doppelpunkt; sonst ein Punkt, und das folgende Wort groß.

Zwei Dinge sind daran wichtiger als die Regeln selbst. Der **Zeilenumbruch bleibt, wo er
war**: die Datei ist auf Spaltenbreite umbrochen, und ein verrutschter Umbruch macht aus
einer Änderung an einem Zeichen eine Änderung an der halben Datei. Und die Großschreibung
hängt nur an den selbst gesetzten Punkten. Eine pauschale Regel „nach jedem Punkt groß" traf
beim ersten Anlauf zwanzigmal daneben, meist bei Datumsangaben: aus „seit dem 04.09. ist der"
wurde „seit dem 04.09. Ist der".

Geprüft wurde an einer Stichprobe und daran, dass die Zeilenzahl gleich bleibt, nicht an
jedem der 1470 Fälle. Ein Teil liest sich danach als Ellipse statt als ganzer Satz. Für ein
Arbeitstagebuch reicht das; für Bildschirmtexte wäre es zu grob.

## `unreachable.py`

Läuft die Fokusreihenfolge des sichtbaren Bildschirms ab und meldet, was anklickbar ist und
dabei nie den Fokus bekommt.

```sh
tools/unreachable.py emulator-5554
```

Das Gegenstück zu `small-buttons.py`. Dort heißt die Frage *groß genug*, hier heißt sie
**erreichbar**. Mit dem Finger fällt der Unterschied nie auf: was ein Rechteck hat, nimmt
einen Tipp an. Mit Tasten kommt man nur dorthin, wohin der Fokus läuft.

**Es wählt nie aus.** Nur die vier Richtungstasten, niemals Auswahl oder eine Ziffer. Es
startet also nichts, wählt keine Nummer und verschickt nichts; deshalb darf es auch am
Telefon mit gesteckter SIM laufen. Es lässt den Bildschirm allerdings dort stehen, wo der
Lauf geendet hat, und blättert auf dem Startbildschirm zwischen den Screens.

Zwei Befunde mit verschiedenen Ursachen: *nie fokussierbar* meldet `focusable="false"` und
kann den Fokus gar nicht bekommen, das ist der harte Fall. *Nie erreicht* könnte, wurde aber in
diesem Durchlauf nicht erreicht, also ein Anfangsverdacht.

Gezählt wird nach **Beschriftungen**, nicht nach Maßen: eine Liste scrollt, und dabei wandern
die Rechtecke; derselbe Knopf hieße in jedem Abzug anders. Der Preis ist, dass zwei Zeilen
mit demselben Wort verschmelzen. Dafür gibt es `same-names.py`.

Die Falle dabei ist, dass `uiautomator` eine scrollende Zeile nur zum Teil meldet, mit halber
Höhe **und halbem Text**. In den Einstellungen kam die zweizeilige Zeile „HomeTiles ist Ihr
Startbildschirm" so als eigene Fläche namens „Antippen, um das in den Systemeinstellungen zu
ändern" heraus. Der erste Versuch, das über die Geometrie zu lösen, war falsch: wer jede
Zeile wegwirft, die an einer Kante klebt, wirft die unterste Zeile jeder Liste mit weg, und
genau die ist beim Durchlaufen der interessante Fall. Von zwölf Einstellungszeilen blieben
fünf übrig. Richtig ist, es als Namensproblem zu nehmen: von jeder Fläche wird gemerkt,
welche Texte in ihr stehen, und was nur die Nebenzeile einer anderen ist, fällt am Ende
heraus.

Der Lauf beginnt mit vierzig Drücken nach oben und links, um in eine Ecke zu kommen. Ohne
das hängt das Ergebnis davon ab, wo der Fokus zufällig stand: ein Lauf über den
Startbildschirm meldete alle acht Flächen erreicht, der nächste vom selben Bildschirm zwei
als unerreichbar.

Von der Ecke aus wird gemerkt, **welche Richtung an welcher Stelle schon probiert wurde**,
und von jeder Stelle jede der vier genau einmal genommen. Zwei einfachere Muster davor
haben beide versagt. Eine Richtung beibehalten, bis sie nichts Neues bringt: der Weg von
unten zurück nach oben läuft durch lauter schon Gesehenes, und die Geduld war aufgebraucht,
bevor der Lauf die zweite Spalte probiert hatte. Zusätzlich wechseln, wenn sich der Fokus
nicht bewegt: dann lief er im Kreis, weil eine Sackgasse die Rotation jedes Mal auf
derselben Richtung ankommen ließ. Der Fehler war beide Male, die Richtung an der Zeit
festzumachen statt am Ort.

Nach neunzig Sekunden bricht der Lauf ab und sagt es. Ein Abzug dauert über eine Sekunde,
und eine Messung, die man nicht abwartet, macht niemand zweimal.

Beim ersten Lauf über einen offenen Ordner hat es sofort etwas gefunden, das von Hand
zweimal übersehen worden war: dort hatte **gar nichts** den Fokus, auch nach vier
Tastendrücken nicht.
