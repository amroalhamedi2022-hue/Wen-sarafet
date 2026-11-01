#!/usr/bin/env python3
"""Private, offline screenshot parity check. Pillow is optional: pip install Pillow.

Usage:
 python3 tools/glass_visual_diff.py --reference concept.png --actual current.png \
   --reference-crop 36,142,361,720 --output local-diff

Never commit screenshots or outputs containing personal financial information.
Comparing different aspect ratios, balances, fonts or dates is not an identity score.
"""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageStat


def crop_rect(s):
    try:
        box = tuple(map(int, s.split(",")))
        if len(box) != 4 or box[0] < 0 or box[1] < 0 or box[2] <= box[0] or box[3] <= box[1]:
            raise ValueError()
        return box
    except ValueError:
        raise argparse.ArgumentTypeError("Use left,top,right,bottom")


def compare(reference, actual, output):
    reference, actual = reference.convert("RGB"), actual.convert("RGB")
    mismatch = abs(reference.width / reference.height - actual.width / actual.height)
    scaled = reference.resize(actual.size, Image.Resampling.LANCZOS)
    diff = ImageChops.difference(scaled, actual)
    blur_diff = ImageChops.difference(
        scaled.filter(ImageFilter.GaussianBlur(10)),
        actual.filter(ImageFilter.GaussianBlur(10)))
    score = sum(ImageStat.Stat(diff).mean) / (3 * 255)
    large_shapes = sum(ImageStat.Stat(blur_diff).mean) / (3 * 255)
    w, h = actual.size
    board = Image.new("RGB", (w * 4, h + 30), "white")
    for i, (caption, panel) in enumerate(zip(
            ["REFERENCE", "ACTUAL", "OVERLAY", "DIFFERENCE"],
            [scaled, actual, Image.blend(scaled, actual, .5), diff])):
        board.paste(panel, (i * w, 30))
        ImageDraw.Draw(board).text((i * w + 5, 7), caption, fill="black")
    output.mkdir(parents=True, exist_ok=True)
    board.save(output / "comparison.png")
    stats = {
        "pixel_difference_0_to_1": round(score, 5),
        "blurred_shape_difference_0_to_1": round(large_shapes, 5),
        "aspect_ratio_warning": mismatch > .035,
        "note": "Diagnostic only: not a certified visual-similarity metric.",
    }
    (output / "metrics.json").write_text(json.dumps(stats, indent=2))
    print(json.dumps(stats, indent=2))


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--reference", required=True, type=Path)
    ap.add_argument("--actual", required=True, type=Path)
    ap.add_argument("--reference-crop", type=crop_rect)
    ap.add_argument("--actual-crop", type=crop_rect)
    ap.add_argument("--output", type=Path, default=Path("local-diff"))
    args = ap.parse_args()
    with Image.open(args.reference) as im1, Image.open(args.actual) as im2:
        a = im1.crop(args.reference_crop) if args.reference_crop else im1
        b = im2.crop(args.actual_crop) if args.actual_crop else im2
        compare(a, b, args.output)


if __name__ == "__main__":
    main()
