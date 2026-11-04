#!/usr/bin/env python3
"""Catch bank-logo bleed through the navigation material on light-theme QA.

Unlike the existing motion test, this checks an actual VISUAL failure shown
in November 3 emulator screenshots: huge magenta bank art across tab labels.
No account text, private data, or screenshot is uploaded by this test.
"""
import struct
import sys
from check_nav_lens_pixels import crop


def examine(image):
    with open(image, "rb") as source:
        header = source.read(24)
    if header[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit("FAIL: invalid optical QA PNG")
    width, height = struct.unpack(">II", header[16:24])
    # Bottom 10%, including the complete translucent tab container.
    rgb = crop(image, 0, int(height * .90), width, height)
    total = len(rgb) // 3
    if total == 0:
        raise SystemExit("FAIL: empty nav crop")
    magenta = 0
    for i in range(0, len(rgb), 3):
        r, g, b = rgb[i], rgb[i + 1], rgb[i + 2]
        if r > 170 and r > g + 35 and r > b + 18 and g < 195:
            magenta += 1
    percent = 100. * magenta / total
    print(f"Nav-background magenta contamination: {magenta}/{total} "
          f"({percent:.2f}%)")
    # Brand logos are not a legitimate large part of the NAV material. A
    # few emoji pixels are fine; broad opaque pink cards are a regression.
    if percent >= 1.5:
        raise SystemExit("FAIL: background brand card bleeds through glass nav")
    print("PASS: legible frosted navigation without bank-logo ghosting")


if __name__ == "__main__":
    examine(sys.argv[1])
