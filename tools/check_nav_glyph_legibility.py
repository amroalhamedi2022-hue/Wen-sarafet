#!/usr/bin/env python3
"""Fail if a first-frame icon-entrance transition leaves the nav unreadable.

Android emulator sometimes defers property animations until input; the old
startup effect set every glyph alpha=0, producing an apparently empty tab bar.
This test checks real RGB pixels, not accessibility metadata alone.
"""
import struct
import sys
from check_nav_lens_pixels import crop

def check(image):
    with open(image, "rb") as fh:
        header=fh.read(24)
    if header[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit("FAIL: expected a PNG screenshot")
    width,height=struct.unpack(">II",header[16:24])
    # Light UI emulator's four icon/text cells, excluding screen-edge shadows.
    rgb=crop(image,int(width*.05),int(height*.92),
             int(width*.95),int(height*.988))
    ink=0
    for i in range(0,len(rgb),3):
        r,g,b=rgb[i],rgb[i+1],rgb[i+2]
        if r<155 and g<175 and b<210:
            ink+=1
    print(f"Nav glyph ink pixels at first draw: {ink}")
    if ink<400:
        raise SystemExit("FAIL: nav icons or labels are invisible before touch")
    print("PASS: navigation labels visible without waiting for a touch")

if __name__=="__main__":
    check(sys.argv[1])
