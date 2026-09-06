#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Nimmt die langen Gedankenstriche aus einem Text, nach Muster statt stumpf.

Drei Faelle, in dieser Reihenfolge:

1. Zwei Striche in einer Zeile sind ein Einschub. Der bekommt Klammern.
2. Nach dem Strich steht eine Konjunktion (und, aber, oder): das war nie ein neuer Satz,
   sondern ein Komma.
3. Vor dem Strich kuendigt etwas eine Aufzaehlung an (zwei Dinge, beide, naemlich): dann
   folgt keine Aussage, sondern die Liste. Doppelpunkt.
4. Sonst: der Strich stand zwischen zwei Aussagen. Punkt, und das Folgende gross.

Der Zeilenumbruch bleibt, wo er war. Das ist der Grund, warum hier nicht einfach ersetzt
wird: die Datei ist auf Spaltenbreite umbrochen, und ein verrutschter Umbruch macht aus
einer Aenderung an einem Zeichen eine Aenderung an der halben Datei.
"""
import io, re, sys

D = chr(0x2014)
KONJUNKTION = ('und', 'aber', 'oder', 'sondern', 'denn', 'doch', 'sowie', 'wobei')
ANKUENDIGUNG = re.compile(
    r'(zwei|drei|vier|fünf|sechs|sieben|acht|neun|zehn|beide|beides|folgende[nsr]?|'
    r'nämlich)\b[^.!?]{0,45}$', re.I)

def gross(w: str) -> str:
    return w[:1].upper() + w[1:] if w[:1].islower() else w

def entstrichen(text: str):
    z = {'klammer': 0, 'komma': 0, 'doppelpunkt': 0, 'punkt': 0}

    def paar(m):
        z['klammer'] += 1
        return ' (' + m.group(1) + ') '
    text = re.sub(r' ' + D + r' ([^' + D + r'\n]{3,80}?) ' + D + r' ', paar, text)

    def einzeln(m):
        vor, nach, folgt = m.group(1), m.group(2), m.group(3)
        rest = folgt + m.string[m.end():]
        wort = rest.split(None, 1)[0] if rest.split() else ''
        blank = wort.strip('.,;:!?()„“"*`_')
        davor = m.string[:m.start()]
        # Ein Absatzumbruch dahinter heisst: der Strich kuendigt an, was folgt, meist
        # einen Codeblock. Doppelpunkt, und der Umbruch bleibt unangetastet.
        if '\n\n' in (vor + nach):
            z['doppelpunkt'] += 1
            return ':' + nach + folgt
        # In einer Ueberschrift steht kein Punkt mitten drin.
        zeilenanfang = davor.rfind('\n') + 1
        if davor[zeilenanfang:].lstrip().startswith('#'):
            z['doppelpunkt'] += 1
            zeichen = ':'
        elif blank.lower() in KONJUNKTION:
            z['komma'] += 1
            zeichen = ','
        elif ANKUENDIGUNG.search(davor[-70:]):
            z['doppelpunkt'] += 1
            zeichen = ':'
        else:
            z['punkt'] += 1
            zeichen = '.'
        # Das Satzzeichen klebt am Wort davor. Der Umbruch bleibt, wo er war: stand er
        # vor dem Strich, bleibt er vor dem Folgewort; stand er dahinter, ebenso.
        # Nur der von hier gesetzte Punkt macht das Folgewort gross. Eine pauschale
        # Regel "nach jedem Punkt gross" traf am 04.09.2026 zwanzigmal daneben, vor allem
        # Datumsangaben: aus "seit dem 04.09. ist der" wurde "seit dem 04.09. Ist der".
        weiter = gross(folgt) if zeichen == '.' else folgt
        if '\n' in (vor + nach):
            zeile = (vor + nach).split('\n')[-1]
            return zeichen + '\n' + zeile + weiter
        return zeichen + ' ' + weiter

    text, n = re.subn(r'([ \t\n]*)' + D + r'([ \t\n]*)(\S)', einzeln, text)
    return text, z

if __name__ == '__main__':
    pfad = sys.argv[1]
    roh = io.open(pfad, encoding='utf-8').read()
    neu, z = entstrichen(roh)
    if len(sys.argv) > 2 and sys.argv[2] == 'schreiben':
        io.open(pfad, 'w', encoding='utf-8').write(neu)
    print('Striche %d -> %d, Zeilen %d -> %d'
          % (roh.count(D), neu.count(D), roh.count('\n'), neu.count('\n')))
    print('Klammern %(klammer)d, Komma %(komma)d, Doppelpunkt %(doppelpunkt)d, Punkt %(punkt)d' % z)
