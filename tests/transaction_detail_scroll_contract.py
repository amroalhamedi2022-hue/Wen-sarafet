#!/usr/bin/env python3
"""Transaction details and the debt editor remain reachable on short screens."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / "app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")

details = source.split("private void showTransactionDetails(", 1)[1].split(
    "private void showTransactionEditMenu(", 1)[0]
assert "ScrollView scroll = new ScrollView(this);" in details
assert "scroll.addView(content" in details
assert '.setTitle("تفاصيل العملية")' in details
assert ".setView(scroll)" in details

editor = source.split("private void showEditDebtTransactionDialog(", 1)[1].split(
    "private void showCategoryPicker(", 1)[0]
assert "ScrollView scroll=new ScrollView(this)" in editor
assert "scroll.addView(form)" in editor
assert 'setTitle("تعديل حركة الدين").setView(scroll)' in editor

print("TransactionDetailScrollContract PASS: details and debt editor are vertically scrollable")
