#!/bin/sh
# Writes down every change of the screen state - with reason, charge and lock.
#
# Grown out of a complaint that could not be reproduced: "it keeps going dark". A screenshot
# does not answer that, because at the moment of the shot somebody is looking. This watch
# looks every few seconds and writes down **only the changes**; a quiet night is then a file
# with one line in it.
#
#   tools/screen-watch.sh [device] [logfile] [rounds] [seconds]
#
# Every 100 rounds it also writes a sign of life. Without that a watch that crashed long ago
# looks exactly like a quiet night - both times there is nothing there.
#
# Without arguments: first adb device, watch.log in the working directory, one hour in steps
# of 15 seconds. `rounds = 0` means: until somebody stops it - that is what a whole night is
# for. With the default the watch ends after an hour and the rest of the night would stand
# nowhere; since it only writes changes, a finished watch looks exactly like a quiet one.
DEVICE="${1:-$(adb devices | awk 'NR==2 {print $1}')}"
LOG="${2:-watch.log}"
ROUNDS="${3:-240}"
PAUSE="${4:-15}"
[ -n "$DEVICE" ] || { echo "no device found"; exit 1; }

before=""
i=0
since=0
while [ "$ROUNDS" -eq 0 ] || [ "$i" -lt "$ROUNDS" ]; do
  st=$(adb -s "$DEVICE" shell dumpsys display 2>/dev/null | grep -oE "mScreenState=[A-Z]+" | head -1)
  if [ "$st" != "$before" ]; then
    reason=$(adb -s "$DEVICE" shell dumpsys power 2>/dev/null |
      grep -oE "mLastSleepReason=[a-z_]+|mLastWakeReason=[a-zA-Z_ ]+" | tr '\n' ' ')
    battery=$(adb -s "$DEVICE" shell dumpsys battery 2>/dev/null | grep -E "^  level|USB powered" | tr -d ' \n')
    lock=$(adb -s "$DEVICE" shell dumpsys trust 2>/dev/null | grep -oE "deviceLocked=[01]" | head -1)
    echo "$(date +%H:%M:%S) $st $reason $battery $lock" >> "$LOG"
    before="$st"
    since=0
  fi
  since=$((since + 1))
  if [ "$since" -ge 100 ]; then
    echo "$(date +%H:%M:%S) still here, unchanged $before" >> "$LOG"
    since=0
  fi
  sleep "$PAUSE"
  i=$((i + 1))
done
echo "$(date +%H:%M:%S) watch ended" >> "$LOG"
