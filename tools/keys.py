#!/usr/bin/env python3
"""Plays every screen through with keys only, the way a keypad phone is used.

    tools/keys.py [emulator-serial]

Written on 19.10.2026 after a report from someone with a Qin F21 and a Titan: on the Qin the
d-pad worked on the home screen and nowhere else, the dialler ignored the digit keys and on
the Titan the keyboard typed nothing anywhere. All three had passed `tools/unreachable.py`,
which only asks whether the focus gets somewhere.

What it does, screen by screen:

* **focus** - walks down and up with `tools/focus-visible.py` and checks on the drawn pixels
  that the focus shows every time,
* **dialler** - types digits, star, pound and a numpad digit with the hardware keys while
  nothing on the screen has the focus, deletes with the delete key and checks that the
  green key only moves the focus to the call row and dials nothing,
* **search** - types letters with a hardware keyboard while the list has the focus and
  checks that they arrive in the search field, in that order,
* **way back** - that the back key leads to the home screen again.

**Emulator only.** Unlike the two walking tools this one presses select and digits: it opens
screens, flips switches in the settings, and in the dialler a wrong step could place a call.
It refuses any serial that is not `emulator-*`. Use an AVD with `hw.dPad=yes` and
`hw.keyboard=yes`.

**The configuration is put back.** The settings pages get entered and their switches pressed,
so the tool saves the app's configuration first (debug build, `run-as`) and writes it back
before every page and at the end.

Not covered: digits a full keyboard reaches through alt. `adb shell input` sends no modifier
on android 11, so that path is held by `HardwareKeysTest` instead.
"""
import importlib.machinery
import io
import os
import re
import subprocess
import sys
import time

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
# loading the sibling would leave a __pycache__ in tools/, and git would see it.
sys.dont_write_bytecode = True
fv =importlib.machinery.SourceFileLoader("fv", os.path.join(HERE, "focus-visible.py")).load_module()

PACKAGE = "dev.kicker.hometiles.debug"
# how many settings rows get entered; the list is longer, but past this the pages repeat.
SETTINGS_ROWS = 14


class Phone:
    def __init__(self, serial):
        self.serial = serial
        self.dp = fv.density(serial)

    def sh(self, *args):
        return fv.adb(self.serial, "shell", *args)

    def key(self, *names, pause=0.6):
        for name in names:
            self.sh("input", "keyevent", name if name.startswith("KEYCODE_") else "KEYCODE_" + name)
            time.sleep(pause)

    def activity(self):
        hit = re.search(r"mResumedActivity: ActivityRecord\{\S+ \S+ (\S+)",
                        self.sh("dumpsys", "activity", "activities"))
        return hit.group(1) if hit else "?"

    def ours(self):
        return self.activity().startswith(PACKAGE + "/")

    def dump(self):
        self.sh("uiautomator", "dump", "/data/local/tmp/keys.xml")
        return self.sh("cat", "/data/local/tmp/keys.xml")

    def config(self):
        return subprocess.run(["adb", "-s", self.serial, "exec-out", "run-as", PACKAGE, "cat",
                               "files/config.json"], capture_output=True).stdout

    def put_config(self, data):
        subprocess.run(["adb", "-s", self.serial, "shell",
                        f"run-as {PACKAGE} sh -c 'cat > files/config.json'"], input=data)
        self.sh("am", "force-stop", PACKAGE)
        time.sleep(0.5)

    def home(self):
        self.key("HOME", pause=1.5)


def walk(phone, steps):
    """the focus walk of focus-visible.py on the screen that is open now."""
    findings, seen = [], 0
    for key in ["DPAD_DOWN", "DPAD_UP"]:
        last, still = None, 0
        for _ in range(steps):
            phone.key(key, pause=0.4)
            hit = fv.focused(phone.serial)
            if not hit:
                findings.append(f"{key}: nothing has the focus")
                continue
            label, boxes, field = hit
            image = Image.open(io.BytesIO(fv.adb(phone.serial, "exec-out", "screencap", "-p",
                                                 binary=True))).convert("RGB")
            visible, why = fv.shows(image, boxes, field, phone.dp)
            if not visible:
                findings.append(f"'{label}': {why}")
            seen += 1
            still = still + 1 if (label, boxes[0]) == last else 0
            last = (label, boxes[0])
            if still >= 2:
                break
    # a screen with nothing to press has nothing to focus either: an empty message list.
    pressable = [n for n in re.findall(r"<node [^>]*>", phone.dump()) if 'clickable="true"' in n]
    if seen == 0 and not pressable:
        return 0, []
    return seen, sorted(set(findings))


def typed_number(xml):
    """the digits in the dialler's number line, the biggest text with a digit at the top."""
    texts = re.findall(r'text="([^"]*\d[^"]*)"[^>]*bounds="\[\d+,(\d+)\]', xml)
    top = [t for t, y in texts if int(y) < 130]
    return re.sub(r"\s", "", top[0]) if top else ""


def check_dialler(phone):
    results = []
    phone.home()
    phone.key("2", pause=1.5)
    if "DialerActivity" not in phone.activity():
        return [("dialler", "open with 2", "FAIL", phone.activity())]
    # a tap first: touch mode, nothing has the focus, exactly the case that failed.
    phone.sh("input", "tap", "240", "60")
    time.sleep(0.5)
    phone.key("1", "2", "3", "STAR", "POUND", "NUMPAD_4")
    got = typed_number(phone.dump())
    results.append(("dialler", "1 2 3 * # numpad-4", "ok" if got == "123*#4" else "FAIL", got))
    phone.key("DEL", "DEL")
    got = typed_number(phone.dump())
    results.append(("dialler", "delete twice", "ok" if got == "123*" else "FAIL", got))
    phone.key("CALL", pause=1.0)
    hit = fv.focused(phone.serial)
    label = hit[0] if hit else ""
    where = phone.activity()
    calls = phone.sh("dumpsys", "telecom").count("Call [")
    stayed = "DialerActivity" in where and calls == 0
    note = (f"focus on '{label}', no call placed" if stayed
            else f"{calls} call(s) in telecom" if calls else f"left the dialler for {where}")
    results.append(("dialler", "green key", "ok" if "Call" in label and stayed else "FAIL", note))
    phone.key("DEL", "DEL", "DEL", "DEL")
    seen, findings = walk(phone, 8)
    results.append(("dialler", f"focus walk ({seen} steps)", "FAIL" if findings else "ok",
                    "; ".join(findings)))
    return results


def check_search(phone, digit, name):
    phone.home()
    phone.key(digit, pause=1.5)
    if not phone.ours():
        return [(name, "open", "FAIL", phone.activity())]
    phone.key("DPAD_DOWN", "DPAD_DOWN")
    phone.key("C", "A", "L")
    fields = [n for n in re.findall(r"<node [^>]*>", phone.dump()) if 'class="android.widget.EditText"' in n]
    text = re.search(r' text="([^"]*)"', fields[0]).group(1) if fields else ""
    return [(name, "letters from the list", "ok" if text.lower() == "cal" else "FAIL", repr(text))]


def check_screen(phone, route, name, steps=12):
    phone.home()
    phone.key(*route, pause=1.2)
    if not phone.ours():
        where = phone.activity()
        phone.key("BACK", pause=1.0)
        return [(name, "open", "skip", f"leaves the app ({where})")]
    seen, findings = walk(phone, steps)
    rows = [(name, f"focus walk ({seen} steps)", "FAIL" if findings else "ok", "; ".join(findings))]
    phone.key("BACK", "BACK", pause=1.0)
    phone.home()
    rows.append((name, "back to home", "ok" if "MainActivity" in phone.activity() else "FAIL",
                 phone.activity()))
    return rows


def main():
    serial = sys.argv[1] if len(sys.argv) > 1 else "emulator-5554"
    if not serial.startswith("emulator-"):
        sys.exit(f"{serial} is not an emulator. this tool presses select and digits; "
                 "on a real phone use tools/focus-visible.py, which only walks.")
    phone = Phone(serial)
    saved = phone.config()
    if not saved.strip().startswith(b"{"):
        sys.exit("no configuration readable through run-as. is the debug build installed?")

    results = []
    try:
        results += check_screen(phone, ["DPAD_DOWN"], "home screen", steps=6)
        results += check_dialler(phone)
        for digit, name in [("1", "messages"), ("3", "contacts"), ("4", "apps"), ("6", "missed calls")]:
            phone.put_config(saved)
            results += check_screen(phone, [digit], name)
        for digit, name in [("4", "apps"), ("3", "contacts")]:
            results += check_search(phone, digit, name)
        phone.put_config(saved)
        results += check_screen(phone, ["5"], "settings", steps=SETTINGS_ROWS + 2)
        for row in range(SETTINGS_ROWS):
            phone.put_config(saved)
            results += check_screen(phone, ["5"] + ["DPAD_DOWN"] * row + ["DPAD_CENTER"],
                                    f"settings row {row + 1}")
        phone.put_config(saved)
        results += check_screen(phone, ["DPAD_DOWN", "MENU"], "tile menu", steps=6)
    finally:
        phone.put_config(saved)
        phone.home()

    width = max(len(r[0]) for r in results)
    for screen, what, verdict, note in results:
        print(f"{screen:{width}}  {verdict:4}  {what}  {note}")
    failed = [r for r in results if r[2] == "FAIL"]
    print(f"\n{len(results)} checks, {len(failed)} failed")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
