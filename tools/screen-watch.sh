#!/bin/sh
# Schreibt jede Aenderung des Bildschirmzustands mit - mit Grund, Ladestand und Schloss.
#
# Entstanden aus einer Beschwerde, die sich nicht nachstellen liess: "es wird immer wieder
# dunkel". Ein Bildschirmfoto beantwortet das nicht, weil man im Moment des Fotos hinsieht.
# Diese Wache sieht alle paar Sekunden nach und schreibt **nur die Aenderungen** auf; eine
# ruhige Nacht ist dann eine Datei mit einer Zeile.
#
#   tools/screen-watch.sh [geraet] [logdatei] [runden] [sekunden]
#
# Alle 100 Runden schreibt sie ausserdem ein Lebenszeichen. Ohne das sieht eine Wache, die
# laengst abgestuerzt ist, genauso aus wie eine ruhige Nacht - beide Male steht nichts da.
#
# Ohne Angaben: erstes adb-Geraet, wache.log im Arbeitsverzeichnis, eine Stunde in
# 15-Sekunden-Schritten. `runden = 0` heisst: bis jemand sie abbricht - fuer eine ganze
# Nacht ist das gemeint. Mit der Vorgabe endet die Wache nach einer Stunde, und der Rest
# der Nacht stuende nirgends; da sie nur Aenderungen aufschreibt, sieht eine beendete
# Wache genauso aus wie eine ruhige.
GERAET="${1:-$(adb devices | awk 'NR==2 {print $1}')}"
LOG="${2:-wache.log}"
RUNDEN="${3:-240}"
PAUSE="${4:-15}"
[ -n "$GERAET" ] || { echo "kein Geraet gefunden"; exit 1; }

vorher=""
i=0
seit=0
while [ "$RUNDEN" -eq 0 ] || [ "$i" -lt "$RUNDEN" ]; do
  st=$(adb -s "$GERAET" shell dumpsys display 2>/dev/null | grep -oE "mScreenState=[A-Z]+" | head -1)
  if [ "$st" != "$vorher" ]; then
    grund=$(adb -s "$GERAET" shell dumpsys power 2>/dev/null |
      grep -oE "mLastSleepReason=[a-z_]+|mLastWakeReason=[a-zA-Z_ ]+" | tr '\n' ' ')
    akku=$(adb -s "$GERAET" shell dumpsys battery 2>/dev/null | grep -E "^  level|USB powered" | tr -d ' \n')
    schloss=$(adb -s "$GERAET" shell dumpsys trust 2>/dev/null | grep -oE "deviceLocked=[01]" | head -1)
    echo "$(date +%H:%M:%S) $st $grund $akku $schloss" >> "$LOG"
    vorher="$st"
    seit=0
  fi
  seit=$((seit + 1))
  if [ "$seit" -ge 100 ]; then
    echo "$(date +%H:%M:%S) noch da, unveraendert $vorher" >> "$LOG"
    seit=0
  fi
  sleep "$PAUSE"
  i=$((i + 1))
done
echo "$(date +%H:%M:%S) Wache beendet" >> "$LOG"
