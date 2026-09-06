#!/usr/bin/env python3
"""Measures contrast in a screenshot - where it actually arrives.

`SurfaceContrastTest` checks the colour constants against each other. What becomes of them
on the screen is another matter: a tile can carry a colour of its own, hold a photo, lie
under a gradient, and the text is drawn with anti-aliasing. This tool reads the pixels.

    tools/contrast.py picture.png x0 y0 x1 y1 [x0 y0 x1 y1 ...]

For each rectangle: the brightest and the darkest pixel in it and their WCAG ratio. In a
field with text those are the letters and their ground.

    tools/contrast.py picture.png --ground X Y  x0 y0 x1 y1

Compares against a fixed point instead - for text on the background, where the rectangle
holds nothing but letters.

The thresholds of HomeTiles (`Tokens.kt`): 4.5 for text on a tile, 3.0 for a tile against
the background, 7.0 for text outside a tile.
"""
import sys

from PIL import Image

THRESHOLDS = "4.5 text/tile, 3.0 tile/ground, 7.0 text/ground"


def luminance(rgb):
    def channel(value):
        value = value / 255.0
        return value / 12.92 if value <= 0.03928 else ((value + 0.055) / 1.055) ** 2.4

    red, green, blue = rgb[:3]
    return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)


def ratio(one, two):
    bright, dark = sorted((luminance(one), luminance(two)), reverse=True)
    return (bright + 0.05) / (dark + 0.05)


def main(argv):
    if len(argv) < 6:
        print(__doc__.strip().split("\n\n")[2].strip())
        return 2
    picture = Image.open(argv[1]).convert("RGB")
    rest = argv[2:]
    ground = None
    if rest[0] == "--ground":
        ground = picture.getpixel((int(rest[1]), int(rest[2])))
        rest = rest[3:]
    if len(rest) % 4 != 0:
        print("A rectangle needs four numbers: x0 y0 x1 y1")
        return 2
    for i in range(0, len(rest), 4):
        x0, y0, x1, y1 = (int(n) for n in rest[i:i + 4])
        pixels = [picture.getpixel((x, y)) for x in range(x0, x1) for y in range(y0, y1)]
        if not pixels:
            print("Empty rectangle: %d %d %d %d" % (x0, y0, x1, y1))
            continue
        brightest = max(pixels, key=luminance)
        darkest = min(pixels, key=luminance)
        if ground is None:
            print("[%d,%d][%d,%d] %s on %s = %.2f:1"
                  % (x0, y0, x1, y1, brightest, darkest, ratio(brightest, darkest)))
        else:
            text = brightest if luminance(ground) < 0.5 else darkest
            print("[%d,%d][%d,%d] %s on ground %s = %.2f:1"
                  % (x0, y0, x1, y1, text, ground, ratio(text, ground)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
