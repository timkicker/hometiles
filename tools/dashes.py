#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Takes the long dashes out of a text, by pattern rather than bluntly.

Four cases, in this order:

1. Two dashes in one line are an aside. That one gets brackets.
2. After the dash stands a conjunction (and, but, or): that was never a new sentence but
   a comma.
3. Before the dash something announces a list (two things, both, namely): then what
   follows is not a statement but the list. Colon.
4. Otherwise: the dash stood between two statements. Full stop, and what follows is
   capitalised.

The line break stays where it was. That is why this does not simply replace: the file is
wrapped to a column width, and a slipped break turns a change of one character into a
change of half the file.

The word lists are german because the prose this was written for is: it reads the working
notes, not source.
"""
import io, re, sys

D = chr(0x2014)
CONJUNCTION = ('und', 'aber', 'oder', 'sondern', 'denn', 'doch', 'sowie', 'wobei')
ANNOUNCEMENT = re.compile(
    r'(zwei|drei|vier|fünf|sechs|sieben|acht|neun|zehn|beide|beides|folgende[nsr]?|'
    r'nämlich)\b[^.!?]{0,45}$', re.I)

def capitalise(w: str) -> str:
    return w[:1].upper() + w[1:] if w[:1].islower() else w

def undash(text: str):
    n = {'brackets': 0, 'comma': 0, 'colon': 0, 'stop': 0}

    def pair(m):
        n['brackets'] += 1
        return ' (' + m.group(1) + ') '
    text = re.sub(r' ' + D + r' ([^' + D + r'\n]{3,80}?) ' + D + r' ', pair, text)

    def single(m):
        before, after, follows = m.group(1), m.group(2), m.group(3)
        rest = follows + m.string[m.end():]
        word = rest.split(None, 1)[0] if rest.split() else ''
        bare = word.strip('.,;:!?()„“"*`_')
        ahead = m.string[:m.start()]
        # A paragraph break behind it means: the dash announces what follows, usually a
        # code block. Colon, and the break stays untouched.
        if '\n\n' in (before + after):
            n['colon'] += 1
            return ':' + after + follows
        # A heading carries no full stop in the middle.
        line_start = ahead.rfind('\n') + 1
        if ahead[line_start:].lstrip().startswith('#'):
            n['colon'] += 1
            mark = ':'
        elif bare.lower() in CONJUNCTION:
            n['comma'] += 1
            mark = ','
        elif ANNOUNCEMENT.search(ahead[-70:]):
            n['colon'] += 1
            mark = ':'
        else:
            n['stop'] += 1
            mark = '.'
        # The punctuation sticks to the word before it. The break stays where it was:
        # standing before the dash it stays before the following word, standing behind it
        # likewise. Only the full stop set here capitalises what follows. A blanket rule
        # "capital after every full stop" was wrong twenty times on 04.09.2026, mostly on
        # dates: "seit dem 04.09. ist der" became "seit dem 04.09. Ist der".
        onward = capitalise(follows) if mark == '.' else follows
        if '\n' in (before + after):
            line = (before + after).split('\n')[-1]
            return mark + '\n' + line + onward
        return mark + ' ' + onward

    text, _ = re.subn(r'([ \t\n]*)' + D + r'([ \t\n]*)(\S)', single, text)
    return text, n

if __name__ == '__main__':
    path = sys.argv[1]
    raw = io.open(path, encoding='utf-8').read()
    new, n = undash(raw)
    if len(sys.argv) > 2 and sys.argv[2] == 'write':
        io.open(path, 'w', encoding='utf-8').write(new)
    print('dashes %d -> %d, lines %d -> %d'
          % (raw.count(D), new.count(D), raw.count('\n'), new.count('\n')))
    print('brackets %(brackets)d, comma %(comma)d, colon %(colon)d, stop %(stop)d' % n)
