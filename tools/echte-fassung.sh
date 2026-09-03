#!/bin/sh
# Prueft die **echte** Konfiguration vom Telefon gegen den Import-Weg.
#
#   tools/echte-fassung.sh [geraet]
#
# `RealConfigRoundTripTest` braucht eine gewachsene Konfiguration - mehrere Bildschirme, ein
# Ordner, eigene Beschriftungen, Farben. Genau die entsteht nicht im Testquelltext, sondern
# ueber Wochen auf einem Telefon. Sie darf **nicht** ins Repository: sie enthaelt die
# App-Liste und die Bildschirmnamen eines Menschen.
#
# Deshalb holt dieses Skript sie sich, legt sie **ausserhalb** des Projekts ab und loescht
# sie wieder. Bis zum 03.09.2026 hat der Test sich stattdessen jedesmal selbst uebersprungen
# - seit seiner Entstehung, ohne dass es auffiel. Der Umzug auf ein neues Telefon ist der
# Grund, aus dem es die Sicherung ueberhaupt gibt; er war ungeprueft.
set -e
cd "$(dirname "$0")/.."
[ -f env.sh ] && . ./env.sh

GERAET=${1:-}
ADB="adb"
[ -n "$GERAET" ] && ADB="adb -s $GERAET"

ZIEL=$(mktemp -t biglau-config-XXXXXX.json)
trap 'rm -f "$ZIEL"' EXIT

echo "hole die Konfiguration vom Telefon ..."
$ADB shell 'run-as org.biglau.debug cat files/config.json' > "$ZIEL"
if [ ! -s "$ZIEL" ]; then
  echo "nichts bekommen - haengt das Telefon dran, und ist die Debug-Fassung installiert?"
  exit 2
fi
echo "$(wc -c < "$ZIEL") Bytes, $(md5sum "$ZIEL" | cut -d' ' -f1)"

BIGLAU_REAL_CONFIG="$ZIEL" ./gradlew :core:model:test --tests '*RealConfigRoundTripTest*' \
  --rerun-tasks -q

# Grün allein genuegt nicht: ein uebersprungener Test ist auch gruen. Deshalb nachsehen,
# ob er wirklich gelaufen ist.
BERICHT=core/model/build/test-results/test/TEST-org.biglau.data.RealConfigRoundTripTest.xml
if grep -q 'skipped="0"' "$BERICHT" 2>/dev/null; then
  echo "gelaufen und bestanden - die echte Fassung uebersteht den Umzug"
else
  echo "ACHTUNG: der Test hat sich uebersprungen, er hat nichts geprueft."
  exit 1
fi
