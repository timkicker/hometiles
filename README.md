# BigLau

Ein großer, ruhiger Startbildschirm für Android, mit Telefon und SMS in derselben App.

BigLau ist die freie Antwort auf BIG Launcher, das seine Funktionen inzwischen auf drei
gekaufte Apps verteilt. Hier ist alles in einer App, quelloffen, ohne Konto, ohne Werbung,
ohne Netzwerkzugriff.

Entwickelt und geprüft auf einem **Unihertz Jelly 2** (3 Zoll, 480 × 854, 220 dpi), dem
Gerät, auf dem große Kacheln am meisten zählen und am schwersten unterzubringen sind.

## Was drin ist

- **Startbildschirm** aus großen Kacheln, frei belegbar: App, Kontakt, Verknüpfung,
  Widget, Funktion (Taschenlampe, WLAN, Bluetooth, Flugmodus, Klingeln/Vibrieren …) oder Sprung
  auf einen anderen Bildschirm. Raster, Größen, Farben und Beschriftungen einstellbar.
- **Kopfzeile** mit Uhrzeit, Datum, Ladestand und Ladebalken, abschaltbar.
- **Telefon**: Tastenfeld, Kurzwahl auf 2-9, Anrufliste mit Gruppierung, eigener
  Anrufbildschirm. BigLau kann die Telefon-Rolle übernehmen, muss aber nicht.
- **Nachrichten**: Liste, Gespräch, Verfassen. Alle vier Pflichtkomponenten für die
  Standard-SMS-Rolle sind da.
- **Kontakte** mit Suche, Sortierung nach Vor- oder Nachnamen, Favoriten.
- **App-Liste** als große Liste mit Suche und „zuletzt benutzt"; Apps lassen sich
  ausblenden. Ganz am Ende steht **„BigLau-Einstellungen"**. Damit gibt es einen Weg
  dorthin, auch wenn auf keinem Bildschirm eine Einstellungs-Kachel liegt. Die Zeile ist
  auch über die Suche zu finden.
- **SOS**: Notfallknopf mit Countdown und Notfall-SMS an mehrere Nummern, auf Wunsch mit
  Standort. BigLau **wählt dabei nie von selbst**. Nach dem Senden steht ein Knopf da, der
  die Wähltastatur mit der ersten Nummer öffnet.
- **Lesehilfe**: langer Druck liest die Kachel vor oder zeigt ihren Namen bildschirmfüllend;
  Blätterknöpfe statt Wischen für lange Listen.
- **Sicherung**: Konfiguration als Datei exportieren und auf dem nächsten Telefon
  wieder einlesen.
- **Notfall-Auffang**: startet der Launcher zweimal hintereinander nicht, erscheint ein
  einfacher Bildschirm statt eines schwarzen Geräts: ganz oben Telefon und Kontakte (die
  Apps des Systems, nicht die eigenen), darunter noch einmal versuchen, Einstellungen,
  anderen Startbildschirm wählen und Kacheln zurücksetzen.

## Gestaltung

Dunkel als Hauptthema, dazu ein helles, ein Kontrastthema (Schwarz auf Gelb) und die
Einstellung „dem Telefon folgen". Jede Kachel ist eine Fläche
in einer Farbe, das Symbol oben links, die Beschriftung unten links in einer Zone fester
Höhe, dadurch stehen die Grundlinien in einer Reihe, auch wenn eine Beschriftung umbricht.
Alle Farbpaare erfüllen mindestens 4,5:1 (WCAG AAA für große Schrift); ein Test prüft das
für jede Kombination, die die App überhaupt zeichnen kann.

Ausführlich in [PLAN.md](PLAN.md), Abschnitt 3.

## Bauen

```sh
source ./env.sh      # setzt ANDROID_HOME, JAVA_HOME und den Gradle-Pfad
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew installDebug
```

Gebraucht werden das Android SDK (compileSdk 35) und ein JDK 21. Der Pfad zum JDK steht
bewusst **nicht** in `gradle.properties`, er kommt aus `JAVA_HOME`.

### Reproduzierbar

`assembleRelease` liefert ein **unsigniertes** APK. Mit dem Debug-Schlüssel zu signieren
wäre eine Lüge über die Herkunft. Wer selbst veröffentlicht, trägt seinen eigenen Schlüssel
ein; F-Droid signiert ohnehin selbst.

Zwei vollständige Neubauten ohne Build-Cache ergeben dieselbe Datei:

```sh
./gradlew --no-build-cache clean assembleRelease
sha256sum app/build/outputs/apk/release/app-release-unsigned.apk
```

Geprüft am 3. September 2026 mit AGP 8.7.3, Gradle 8.11.1 und JDK 21, zweimal ohne
Build-Cache gebaut, beide Male

```
4f89516a674722eb3a8432036d60150e0468e26296739ec12c67752293f4c1a7   1 789 011 Bytes
```

für **Commit `b753657`**. Die Prüfsumme gehört zu einem Stand des Quelltexts, nicht zum
Projekt: wer sie nachrechnen will, baut diesen Commit. Genau deshalb steht er jetzt dabei,
die vorige Angabe nannte nur ein Datum, und schon der nächste Commit machte sie unprüfbar.
`tools/nachbauen.sh` macht beide Läufe und den Vergleich in einem Aufruf.

## Berechtigungen

Alle sind optional und werden erst gefragt, wenn die zugehörige Funktion benutzt wird.
Ohne SIM oder ohne erteilte Rolle läuft der Launcher vollständig weiter.

| Berechtigung | Wofür |
| --- | --- |
| `CALL_PHONE` | Anrufen aus Kurzwahl, Kontakten und SOS |
| `READ_CALL_LOG`, `WRITE_CALL_LOG` | Anrufliste anzeigen und löschen |
| `READ_CONTACTS`, `WRITE_CONTACTS` | Kontakte und Favoritenkennzeichen |
| `SEND_SMS`, `READ_SMS`, `RECEIVE_SMS` | Nachrichten |
| `ACCESS_FINE_LOCATION` | Standort in der Notfall-SMS |
| `VIBRATE`, `EXPAND_STATUS_BAR`, `SET_WALLPAPER` | Rückmeldung, Schalter, Hintergrund |

Kein `INTERNET`. BigLau sendet nichts, das Betriebssystem lässt es gar nicht zu.

Wer ins Archiv sieht, findet trotzdem `okhttp3/…/publicsuffixes.gz`: die Bildbibliothek
Coil bringt einen HTTP-Client mit, den BigLau nie benutzt (Kontaktfotos kommen über
`content://`). R8 räumt den Code weg, die 41 kB Beilage bleiben. Ohne
`INTERNET`-Berechtigung kann davon nichts ins Netz, siehe `PLAN.md` P8.

## Lizenz

GPL-3.0-or-later, siehe [LICENSE](LICENSE).

Mitgeliefert ist die Schrift **Atkinson Hyperlegible** vom Braille Institute of America
unter der SIL Open Font License 1.1, siehe
[LICENSE-Atkinson-Hyperlegible.txt](LICENSE-Atkinson-Hyperlegible.txt). Sie ist die
Vorgabe, weil sie Buchstaben auseinanderzieht, die sich sonst gleichen.
