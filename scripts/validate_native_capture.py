#!/usr/bin/env python3
"""Strict verification for ACTUAL Android framebuffer PNGs.

This is deliberately not a screenshot generator. It rejects partial ADB
streams, false-success 0-byte artifacts, broken CRCs, invalid/decompressed
scanlines, and Android SplashScreen-sized placeholder images.
Dependencies: Python 3 standard library only.
"""
from __future__ import annotations

import pathlib
import struct
import sys
import zlib

SIGNATURE = b"\x89PNG\r\n\x1a\n"
MAX_UNCOMPRESSED = 64 * 1024 * 1024
CHANNELS = {0: 1, 2: 3, 4: 2, 6: 4}


def verify(path: pathlib.Path, *, min_bytes: int = 100_000,
           min_width: int = 720, min_height: int = 1280) -> tuple[int, int]:
    data = path.read_bytes()
    if len(data) < min_bytes or not data.startswith(SIGNATURE):
        raise ValueError("missing/undersized PNG framebuffer")
    pos = len(SIGNATURE)
    saw_ihdr = False
    saw_idat = False
    saw_iend = False
    width = height = channels = 0
    decoder = zlib.decompressobj()
    decoded_size = 0
    while pos < len(data):
        if pos + 12 > len(data):
            raise ValueError("truncated PNG chunk")
        length = struct.unpack_from(">I", data, pos)[0]
        kind = data[pos + 4:pos + 8]
        if length > len(data) - pos - 12:
            raise ValueError("PNG chunk exceeds file")
        payload = data[pos + 8:pos + 8 + length]
        expected_crc = struct.unpack_from(">I", data, pos + 8 + length)[0]
        actual_crc = zlib.crc32(payload, zlib.crc32(kind)) & 0xffffffff
        if expected_crc != actual_crc:
            raise ValueError("invalid PNG CRC: " + repr(kind))
        pos += length + 12
        if not saw_ihdr:
            if kind != b"IHDR" or length != 13:
                raise ValueError("PNG must begin with IHDR")
            width, height, depth, color, compression, filtering, interlace = (
                struct.unpack(">IIBBBBB", payload))
            if width < min_width or height < min_height:
                raise ValueError(f"framebuffer too small: {width}x{height}")
            if depth != 8 or color not in CHANNELS or compression or filtering or interlace:
                raise ValueError("unsupported screenshot pixel format")
            channels = CHANNELS[color]
            if height * (1 + width * channels) > MAX_UNCOMPRESSED:
                raise ValueError("framebuffer exceeds bounded decode size")
            saw_ihdr = True
        elif kind == b"IDAT":
            if saw_iend:
                raise ValueError("IDAT after IEND")
            saw_idat = True
            decoded_size += len(decoder.decompress(
                payload, MAX_UNCOMPRESSED - decoded_size + 1))
            if decoded_size > MAX_UNCOMPRESSED:
                raise ValueError("PNG decompression bomb")
        elif kind == b"IEND":
            if length != 0 or not saw_idat or pos != len(data):
                raise ValueError("invalid final PNG IEND")
            saw_iend = True
            break
        elif kind == b"IHDR":
            raise ValueError("duplicate IHDR")
    if not saw_iend or not decoder.eof or decoder.unused_data:
        raise ValueError("incomplete PNG compressed stream")
    decoded_size += len(decoder.flush())
    expected_pixels = height * (1 + width * channels)
    if decoded_size != expected_pixels:
        raise ValueError(f"invalid scanline length: {decoded_size} != {expected_pixels}")
    return width, height


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: validate_native_capture.py <real-framebuffer.png>", file=sys.stderr)
        sys.exit(2)
    try:
        size = verify(pathlib.Path(sys.argv[1]))
    except (OSError, ValueError, zlib.error) as exc:
        print("FAIL: invalid native Android capture:", exc, file=sys.stderr)
        sys.exit(1)
    print(f"PASS: complete {size[0]}x{size[1]} native PNG (CRC + IDAT + IEND)")
