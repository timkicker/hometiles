#!/bin/sh
# Checks the **real** configuration from the phone against the import path.
#
#   tools/real-config.sh [device]
#
# `RealConfigRoundTripTest` needs a grown configuration - several screens, a folder, labels
# of one's own, colours. Exactly that does not arise in test source but over weeks on a
# phone. It must **not** go into the repository: it holds a person's app list and screen
# names.
#
# So this script fetches it, puts it **outside** the project and deletes it again. Until
# 03.09.2026 the test skipped itself every time instead - since the day it was written,
# without anybody noticing. Moving to a new phone is the reason the backup exists at all,
# and it was unchecked.
set -e
cd "$(dirname "$0")/.."
[ -f env.sh ] && . ./env.sh

DEVICE=${1:-}
ADB="adb"
[ -n "$DEVICE" ] && ADB="adb -s $DEVICE"

TARGET=$(mktemp -t hometiles-config-XXXXXX.json)
trap 'rm -f "$TARGET"' EXIT

echo "fetching the configuration from the phone ..."
$ADB shell 'run-as dev.kicker.hometiles.debug cat files/config.json' > "$TARGET"
if [ ! -s "$TARGET" ]; then
  echo "got nothing - is the phone attached, and is the debug build installed?"
  exit 2
fi
echo "$(wc -c < "$TARGET") bytes, $(md5sum "$TARGET" | cut -d' ' -f1)"

HOMETILES_REAL_CONFIG="$TARGET" ./gradlew :core:model:test --tests '*RealConfigRoundTripTest*' \
  --rerun-tasks -q

# Green alone is not enough: a skipped test is green too. So look whether it really ran.
REPORT=core/model/build/test-results/test/TEST-dev.kicker.hometiles.data.RealConfigRoundTripTest.xml
if grep -q 'skipped="0"' "$REPORT" 2>/dev/null; then
  echo "ran and passed - the real configuration survives the move"
else
  echo "CAREFUL: the test skipped itself, it checked nothing."
  exit 1
fi
