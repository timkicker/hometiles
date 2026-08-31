# BigLau

Ein großer, ruhiger Startbildschirm für Android — mit Telefon und SMS in derselben App.

BigLau ist die freie Antwort auf BIG Launcher, das seine Funktionen inzwischen auf drei
gekaufte Apps verteilt. Hier ist alles in einer App, quelloffen, ohne Konto, ohne Werbung,
ohne Netzwerkzugriff.

Entwickelt und geprüft auf einem **Unihertz Jelly 2** (3 Zoll, 480 × 854, 220 dpi) — dem
Gerät, auf dem große Kacheln am meisten zählen und am schwersten unterzubringen sind.

## Was drin ist

- **Startbildschirm** aus großen Kacheln, frei belegbar: App, Kontakt, Verknüpfung,
  Widget, Funktion (Taschenlampe, WLAN, Bluetooth, Flugmodus, Lautstärke …) oder Sprung
  auf einen anderen Bildschirm. Raster, Größen, Farben und Beschriftungen einstellbar.
- **Kopfzeile** mit Uhrzeit, Datum, Ladestand und Ladebalken — abschaltbar.
- **Telefon**: Tastenfeld, Kurzwahl auf 2–9, Anrufliste mit Gruppierung, eigener
  Anrufbildschirm. BigLau kann die Telefon-Rolle übernehmen, muss aber nicht.
- **Nachrichten**: Liste, Gespräch, Verfassen. Alle vier Pflichtkomponenten für die
  Standard-SMS-Rolle sind da.
- **Kontakte** mit Suche, Sortierung nach Vor- oder Nachnamen, Favoriten.
- **SOS**: Notfallknopf mit Countdown, Rundruf an mehrere Nummern und Notfall-SMS.
- **Lesehilfe**: langer Druck liest die Kachel vor oder zeigt ihren Namen bildschirmfüllend;
  Blätterknöpfe statt Wischen für lange Listen.
- **Sicherung**: Konfiguration als Datei exportieren und auf dem nächsten Telefon
  wieder einlesen.
- **Notfall-Auffang**: stürzt der Launcher beim Start ab, erscheint ein einfacher
  Bildschirm mit Telefon, Kontakten und Einstellungen statt eines schwarzen Geräts.

## Gestaltung

Dunkel als Hauptthema, dazu ein helles und ein Kontrastthema. Jede Kachel ist eine Fläche
in einer Farbe, das Symbol oben links, die Beschriftung unten links in einer Zone fester
Höhe — dadurch stehen die Grundlinien in einer Reihe, auch wenn eine Beschriftung umbricht.
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
bewusst **nicht** in `gradle.properties` — er kommt aus `JAVA_HOME`.

### Reproduzierbar

`assembleRelease` liefert ein **unsigniertes** APK — mit dem Debug-Schlüssel zu signieren
wäre eine Lüge über die Herkunft. Wer selbst veröffentlicht, trägt seinen eigenen Schlüssel
ein; F-Droid signiert ohnehin selbst.

Zwei vollständige Neubauten ohne Build-Cache ergeben dieselbe Datei:

```sh
./gradlew --no-build-cache clean assembleRelease
sha256sum app/build/outputs/apk/release/app-release-unsigned.apk
```

Geprüft am 31. August 2026 mit AGP 8.7.3, Gradle 8.11.1 und JDK 21: zweimal
`3012f967…0cb788a9`.

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

Kein `INTERNET`. BigLau sendet nichts.

## Lizenz

GPL-3.0-or-later, siehe [LICENSE](LICENSE).
