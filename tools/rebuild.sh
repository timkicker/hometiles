#!/bin/sh
# Baut die Release-Fassung zweimal ohne Build-Cache und vergleicht die Prüfsummen.
#
# Der Nachweis, den das README behauptet: derselbe Quelltext ergibt dieselbe Datei. Wer das
# nachrechnet, will nicht zwei Befehle abtippen und die Zeichenketten mit dem Auge
# vergleichen - dabei übersieht man genau die eine Stelle, an der sie sich unterscheiden.
#
#   tools/rebuild.sh
#
# Dauert zwei volle Neubauten (auf einem Laptop je rund eine Minute).
#
# WICHTIG: waehrenddessen darf **kein anderer Gradle-Lauf** an diesem Projekt arbeiten, und
# es darf keine Datei geaendert werden. Am 03.09.2026 um 10:57 habe ich beides missachtet -
# nebenher liefen Tests und ich habe zur Gegenprobe kurz das README verbogen. Ergebnis:
# "VERSCHIEDEN - der Bau ist nicht reproduzierbar". Allein wiederholt, mit demselben Commit:
# zweimal dieselbe Pruefsumme. Ein falsches "nicht reproduzierbar" ist die schlimmste Antwort
# von allen - sie laesst an einer Zusage zweifeln, die stimmt. Deshalb passt das Skript jetzt
# selbst darauf auf.
set -e
cd "$(dirname "$0")/.."
[ -f env.sh ] && . ./env.sh
APK=app/build/outputs/apk/release/app-release-unsigned.apk

lauf() {
  ./gradlew --no-build-cache clean assembleRelease -q > /dev/null
  sha256sum "$APK" | cut -d' ' -f1
}

zustand() { git rev-parse HEAD; git status --porcelain; }

vorher=$(zustand)
echo "erster Lauf ..."
eins=$(lauf)
echo "zweiter Lauf ..."
zwei=$(lauf)
nachher=$(zustand)

echo
echo "1: $eins"
echo "2: $zwei"
echo "Grösse: $(stat -c%s "$APK") Bytes"
echo "Commit: $(git rev-parse --short HEAD)$(git diff --quiet || echo ' (mit ungespeicherten Änderungen!)')"
if [ "$vorher" != "$nachher" ]; then
  echo
  echo "ACHTUNG: waehrend der beiden Laeufe hat sich der Arbeitsbaum geaendert."
  echo "Das Ergebnis sagt nichts aus - weder so noch so. Nochmal, und nichts nebenher tun:"
  echo "  keine Datei bearbeiten, keinen zweiten ./gradlew starten."
  exit 2
fi

if [ "$eins" = "$zwei" ]; then
  echo "gleich - der Bau ist reproduzierbar"
else
  echo "VERSCHIEDEN - der Bau ist nicht reproduzierbar"
  echo
  echo "Bevor du das glaubst: lief nebenher ein anderer ./gradlew? Dann zaehlt es nicht."
  echo "Wiederhole es allein. Genau daran bin ich am 03.09.2026 hereingefallen."
  exit 1
fi
