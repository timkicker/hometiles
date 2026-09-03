#!/bin/sh
# Baut die Release-Fassung zweimal ohne Build-Cache und vergleicht die Prüfsummen.
#
# Der Nachweis, den das README behauptet: derselbe Quelltext ergibt dieselbe Datei. Wer das
# nachrechnet, will nicht zwei Befehle abtippen und die Zeichenketten mit dem Auge
# vergleichen - dabei übersieht man genau die eine Stelle, an der sie sich unterscheiden.
#
#   tools/nachbauen.sh
#
# Dauert zwei volle Neubauten (auf einem Laptop je rund eine Minute).
set -e
cd "$(dirname "$0")/.."
[ -f env.sh ] && . ./env.sh
APK=app/build/outputs/apk/release/app-release-unsigned.apk

lauf() {
  ./gradlew --no-build-cache clean assembleRelease -q > /dev/null
  sha256sum "$APK" | cut -d' ' -f1
}

echo "erster Lauf ..."
eins=$(lauf)
echo "zweiter Lauf ..."
zwei=$(lauf)

echo
echo "1: $eins"
echo "2: $zwei"
echo "Grösse: $(stat -c%s "$APK") Bytes"
echo "Commit: $(git rev-parse --short HEAD)$(git diff --quiet || echo ' (mit ungespeicherten Änderungen!)')"
if [ "$eins" = "$zwei" ]; then
  echo "gleich - der Bau ist reproduzierbar"
else
  echo "VERSCHIEDEN - der Bau ist nicht reproduzierbar"
  exit 1
fi
