#!/usr/bin/env python3
"""No emulator required: prove truncated/corrupted screenshot artifacts fail."""
from __future__ import annotations

import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest
import zlib


ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location(
    "capture_png", ROOT / "scripts" / "validate_native_capture.py")
capture_png = importlib.util.module_from_spec(spec)
assert spec and spec.loader
spec.loader.exec_module(capture_png)


def chunk(kind: bytes, data: bytes) -> bytes:
    return (struct.pack(">I", len(data)) + kind + data
            + struct.pack(">I", zlib.crc32(data, zlib.crc32(kind)) & 0xffffffff))


def png(width=8, height=6, decoded=None):
    scanlines = decoded
    if scanlines is None:
        scanlines = b"".join(
            b"\x00" + bytes((x * 19 + y * 7) % 256 for x in range(width * 4))
            for y in range(height))
    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return (capture_png.SIGNATURE + chunk(b"IHDR", header)
            + chunk(b"IDAT", zlib.compress(scanlines))
            + chunk(b"IEND", b""))


class NativeFramebufferIntegrityTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)
        self.path = Path(self.dir.name) / "real.png"

    def check(self, payload, min_bytes=1, min_width=1, min_height=1):
        self.path.write_bytes(payload)
        return capture_png.verify(
            self.path, min_bytes=min_bytes,
            min_width=min_width, min_height=min_height)

    def test_valid_real_png_structure(self):
        self.assertEqual(self.check(png()), (8, 6))

    def test_zero_byte_artifact_rejected(self):
        with self.assertRaises(ValueError):
            self.check(b"")

    def test_truncated_idat_rejected(self):
        with self.assertRaises(ValueError):
            self.check(png()[:-12])

    def test_crc_corruption_rejected(self):
        b = bytearray(png())
        b[45] ^= 0x50
        with self.assertRaises(ValueError):
            self.check(bytes(b))

    def test_appended_garbage_rejected(self):
        with self.assertRaises(ValueError):
            self.check(png() + b"GARBAGE")

    def test_invalid_scanline_length_rejected(self):
        wrong_scanlines = b"\x00" + b"x" * (8 * 4)
        with self.assertRaises(ValueError):
            self.check(png(decoded=wrong_scanlines))

    def test_undersized_native_image_rejected(self):
        with self.assertRaises(ValueError):
            self.check(png(), min_width=720, min_height=1280)

    def test_tiny_file_never_accepted_as_real_screenshot(self):
        with self.assertRaises(ValueError):
            self.check(png(), min_bytes=100_000)


if __name__ == "__main__":
    unittest.main()
