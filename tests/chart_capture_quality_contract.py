#!/usr/bin/env python3
"""Guard two targeted quality fixes: no chart heap allocations, stable capture normalization."""
from pathlib import Path

root = Path(__file__).resolve().parents[1] / "app/src/main/java/app/wensarafet/tracker"
chart = (root / "ExpenseChartView.java").read_text()
capture = (root / "FinanceDbHelper.java").read_text()
drawing = chart.split("void onDraw(Canvas canvas)", 1)[1]
capture_matching = capture.split("boolean hasMatchingCapture(", 1)[1].split(
    "public int getPendingCaptureCount()", 1)[0]
assert "new RectF(" not in drawing
assert "canvas.drawRoundRect(center - barWidth / 2" in drawing
assert "i < labels.length && labels[i] != null" in drawing
assert capture_matching.count(".toLowerCase(Locale.ROOT)") == 2
assert ".toLowerCase()" not in capture_matching
print("ChartCaptureQualityContract passed: allocation-free bar drawing and locale-neutral capture matching.")
