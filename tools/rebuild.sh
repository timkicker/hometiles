#!/bin/sh
# Builds the release twice without the build cache and compares the checksums.
#
# The proof the readme claims: the same source gives the same file. Whoever recomputes that
# does not want to type two commands and compare the strings by eye - that is exactly how
# one misses the single place where they differ.
#
#   tools/rebuild.sh
#
# Takes two full rebuilds (about a minute each on a laptop).
#
# IMPORTANT: while it runs, **no other gradle run** may work on this project, and no file may
# change. On 03.09.2026 at 10:57 both were ignored - tests were running alongside and the
# readme was bent for a counter-check. Result: "DIFFERENT - the build is not reproducible".
# Repeated alone, on the same commit: the same checksum twice. A wrong "not reproducible" is
# the worst answer of all - it casts doubt on a promise that holds. So the script now watches
# for that itself.
set -e
cd "$(dirname "$0")/.."
[ -f env.sh ] && . ./env.sh
APK=app/build/outputs/apk/release/app-release-unsigned.apk

run() {
  ./gradlew --no-build-cache clean assembleRelease -q > /dev/null
  sha256sum "$APK" | cut -d' ' -f1
}

state() { git rev-parse HEAD; git status --porcelain; }

before=$(state)
echo "first run ..."
one=$(run)
echo "second run ..."
two=$(run)
after=$(state)

echo
echo "1: $one"
echo "2: $two"
echo "size: $(stat -c%s "$APK") bytes"
echo "commit: $(git rev-parse --short HEAD)$(git diff --quiet || echo ' (with unsaved changes!)')"
if [ "$before" != "$after" ]; then
  echo
  echo "CAREFUL: the working tree changed during the two runs."
  echo "The result says nothing, either way. Again, and nothing alongside:"
  echo "  edit no file, start no second ./gradlew."
  exit 2
fi

if [ "$one" = "$two" ]; then
  echo "same - the build is reproducible"
else
  echo "DIFFERENT - the build is not reproducible"
  echo
  echo "Before believing that: was another ./gradlew running alongside? Then it does not count."
  echo "Repeat it alone. That is exactly what went wrong on 03.09.2026."
  exit 1
fi
