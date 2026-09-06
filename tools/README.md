# Tools

Small programs for checking on the device, and for the two checks that have to run before
anything is published. They exist for things no unit test can see, because they arise only
from the finished drawn screen, from a phone grown over weeks, or from the git history.

**The reasoning lives in the tool, not here.** Every one of them carries a docstring that
says what it does, what it caught, and where it was wrong before. This page is the index, so
that the same sentence does not stand in two places and drift apart.

Most need nothing but `adb`. `contrast.py` and `contrast-walk.py` need Pillow, `plurals.py`
needs python-babel.

---

## Before publishing

### `tools/opsec.py`

Looks for personal data and secrets - in the working tree **and in the whole history**,
because a push publishes every commit at once.

```sh
tools/opsec.py                # both, exit code 1 on any finding
tools/opsec.py tree           # only the files git knows today
tools/opsec.py history main   # every blob reachable from that branch
tools/opsec.py probe          # does the search still find what it searches for?
```

Searched for: phone numbers, coordinates, e-mail addresses, device ids, mac addresses, access
tokens and home paths, plus files that never belong in (`STATUS.md`, `PLAN.md`,
`SPRACHEN.md`, `local.properties`, signing keys). Every finding has to go or stand in
`ALLOWED` **with a reason**. Both workflows run it, and `OpsecTest` calls the same tool so
that nothing is written twice.

The dirty samples for the counter-check live in `tools/opsec-probe.txt`, the only file the
search skips.

### `tools/rebuild.sh`

Builds the release twice without the build cache and compares the checksums - the proof the
readme claims. **Nothing else may run alongside**, and no file may change; the script watches
for that itself and refuses to answer otherwise.

---

## On the device

### `tools/tap.py`

Taps only once the screen is the expected one: fresh screenshot, `mResumedActivity` checked,
label at that place checked. Also scrolls inside a list instead of into the system's gesture
zone.

```sh
tools/tap.py <device> <x> <y> <expected-activity> [expected-label] [--long]
tools/tap.py <device> row "<Label>" <expected-activity>
tools/tap.py <device> scroll <expected-activity> [times] [--up]
```

### `tools/unreachable.py`

Walks the focus order and reports what is clickable and never gets the focus. **It never
selects** - only the four direction keys, so it starts nothing, dials nothing and sends
nothing, and may run on a phone with a SIM in it.

### `tools/silent-buttons.py`

Clickable areas without a name: the button a screen reader says nothing about.

### `tools/small-buttons.py`

Tappable areas under 48 dp, with clipped rows counted out.

### `tools/same-names.py`

Clickable areas carrying the same name on one screen - for a screen reader they are one
offer.

### `tools/contrast.py`

Measures contrast in a screenshot, for rectangles you name.

### `tools/contrast-walk.py`

Measures **every** labelled area of a screen at once, and always names the worst place - a
measurement that only stays silent does not say whether it looked.

### `tools/screen-watch.sh`

Writes down every change of the screen state, with reason, charge and lock. A quiet night is
a file with one line in it.

---

## Around the configuration

### `tools/real-config.sh`

Runs `RealConfigRoundTripTest` against the real configuration from the phone, keeps it
outside the project and deletes it again. It also checks that the test **ran** - a skipped
test is green too.

### `tools/anonymise-config.py`

Turns a real configuration into one that can be checked in: keeps the mixture, throws the
person away.

```sh
tools/anonymise-config.py /path/config.json > core/model/src/test/resources/grown-config.json
```

---

## Around the texts

### `tools/plurals.py`

Which plural forms a language really needs, from CLDR instead of guessed - and which of them
occur at all below a threshold, which is the number the decision hangs on.

### `tools/dashes.py`

Takes the long dashes out of a text by pattern rather than bluntly, keeping the line breaks
where they were. Its word lists are german, because the prose it was written for is.

```sh
tools/dashes.py file.md          # only count
tools/dashes.py file.md write
```

### `tools/log-entry.py`

Writes a section into the working log and then looks whether it is really there. The log
itself is not in this repository.
