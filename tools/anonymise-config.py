#!/usr/bin/env python3
"""Turns a real configuration into one that can be checked in.

The migration test needs a *grown* configuration - several screens, a folder, mixed kinds
of tile. The real file must not go into the repository: it gives away which programs lie
on the phone. This tool keeps the mixture and throws the person away.

    tools/anonymise-config.py /path/config.json > core/model/src/test/resources/grown-config.json

Replaced are: package names, recently used programs, speed dial, blocked and hidden
numbers, SOS numbers, the PIN and the timestamp of the last seen missed calls. Everything
else - number, place and size of the tiles, colours, switch positions - stays character
for character.
"""
import json
import sys

package_numbers = {}


def package_for(name):
    if name not in package_numbers:
        package_numbers[name] = f"com.example.program{len(package_numbers) + 1}"
    return package_numbers[name]


def number(index):
    # 030 is the Berlin area code and 1234xxxx a run that belongs to nobody, see
    # tools/opsec.py.
    return f"+49301234{index:04d}"


def anonymise(d):
    for screen in d.get("screens", []):
        for cell in screen.get("cells", []):
            action = cell["button"]["action"]
            if action.get("type") == "app":
                old = action["packageName"]
                action["packageName"] = package_for(old)
                if action.get("activityName"):
                    action["activityName"] = package_for(old) + ".MainActivity"
            if action.get("type") in ("call", "sms", "contact"):
                for field in ("number", "uri", "lookupKey"):
                    if action.get(field):
                        action[field] = number(len(package_numbers) + 1)
    apps = d.get("apps", {})
    apps["recent"] = [
        package_for(e.split("/")[0]) + "/" + package_for(e.split("/")[0]) + ".MainActivity"
        for e in apps.get("recent", [])
    ]
    apps["hidden"] = [package_for(e) for e in apps.get("hidden", [])]
    phone = d.get("phone", {})
    phone["speedDial"] = {k: number(int(k) if str(k).isdigit() else 9) for k in phone.get("speedDial", {})}
    phone["blockedNumbers"] = [number(i) for i, _ in enumerate(phone.get("blockedNumbers", []))]
    phone["lastSeenMissedAt"] = 0
    sms = d.get("sms", {})
    sms["hiddenNumbers"] = [number(i) for i, _ in enumerate(sms.get("hiddenNumbers", []))]
    sms["hiddenWords"] = ["word" for _ in sms.get("hiddenWords", [])]
    d.get("sos", {})["numbers"] = [number(i) for i, _ in enumerate(d.get("sos", {}).get("numbers", []))]
    d.get("security", {})["pin"] = None
    return d


if __name__ == "__main__":
    source = json.load(open(sys.argv[1], encoding="utf-8"))
    print(json.dumps(anonymise(source), indent=1, ensure_ascii=False))
