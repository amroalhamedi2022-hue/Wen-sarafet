#!/usr/bin/env python3
"""Compare RGB pixels under the held finger, not whole PNG file bytes."""
import struct
import sys
import zlib

def crop(path, left, top, right, bottom):
    png = open(path, "rb").read()
    if png[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not PNG")
    off, compressed = 8, bytearray()
    width = height = bpp = 0
    while off + 12 <= len(png):
        size = struct.unpack_from(">I", png, off)[0]
        kind = png[off+4:off+8]
        chunk = png[off+8:off+8+size]
        off += size + 12
        if kind == b"IHDR":
            width, height, bits, colour, _, _, interlace = struct.unpack(">IIBBBBB", chunk)
            if bits != 8 or colour not in (2, 6) or interlace:
                raise ValueError("unsupported PNG format")
            bpp = 3 if colour == 2 else 4
        elif kind == b"IDAT":
            compressed.extend(chunk)
        elif kind == b"IEND":
            break
    left, top = max(left, 0), max(top, 0)
    right, bottom = min(right, width), min(bottom, height)
    if left >= right or top >= bottom:
        raise ValueError("invalid lens area")
    raw = memoryview(zlib.decompress(compressed))
    row_bytes = width * bpp
    previous, scanline = bytearray(row_bytes), bytearray(row_bytes)
    out, offset = bytearray(), 0
    for y in range(bottom):
        filtr = raw[offset]
        offset += 1
        scanline[:] = raw[offset:offset+row_bytes]
        offset += row_bytes
        if filtr == 1:
            for i in range(bpp, row_bytes):
                scanline[i] = (scanline[i] + scanline[i-bpp]) & 255
        elif filtr == 2:
            for i in range(row_bytes):
                scanline[i] = (scanline[i] + previous[i]) & 255
        elif filtr == 3:
            for i in range(row_bytes):
                scanline[i] = (scanline[i] + ((scanline[i-bpp] if i>=bpp else 0) + previous[i]) // 2) & 255
        elif filtr == 4:
            for i in range(row_bytes):
                a = scanline[i-bpp] if i>=bpp else 0
                b = previous[i]
                c = previous[i-bpp] if i>=bpp else 0
                predictor = a + b - c
                pa,pb,pc = abs(predictor-a),abs(predictor-b),abs(predictor-c)
                scanline[i] = (scanline[i] + (a if pa<=pb and pa<=pc else b if pb<=pc else c)) & 255
        elif filtr != 0:
            raise ValueError("unsupported PNG filter")
        if y>=top:
            if bpp==4:
                for x in range(left,right):
                    out.extend(scanline[x*4:x*4+3])
            else:
                out.extend(scanline[left*3:right*3])
        previous[:] = scanline
    return out

if __name__ == "__main__":
    before,during,x,y = sys.argv[1:]
    x,y = int(x),int(y)
    area=(x-82,y-66,x+82,y+66)
    a,b = crop(before,*area),crop(during,*area)
    if len(a)!=len(b):
        raise SystemExit("FAIL: screenshot dimensions differ")
    changed=sum(any(abs(a[i+j]-b[i+j])>16 for j in range(3))
                for i in range(0,len(a),3))
    print(f"Held-tab lens pixel difference: {changed}/{len(a)//3}")
    if changed<350:
        raise SystemExit("FAIL: optical lens did not visibly respond while finger held")
    print("PASS: held-tab lens visibly responds during touch")
