#!/usr/bin/env python3
"""Contracts for the reusable visual-only, battery-aware Glass Engine pilot."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
engine = (root / "app/src/main/java/app/wensarafet/glass/WenGlassDrawable.java").read_text(encoding="utf-8")
ui = (root / "app/src/main/java/app/wensarafet/tracker/Ui.java").read_text(encoding="utf-8")
main = (root / "app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")

assert "package app.wensarafet.glass;" in engine
assert "tracker." not in engine, "Engine must remain independent of finance app"
assert "power.isPowerSaveMode()" in engine
assert "activity.isLowRamDevice()" in engine
assert "if (!lowPower)" in engine
assert "onBoundsChange" in engine and "new LinearGradient" in engine
draw = engine.split("@Override public void draw(Canvas canvas)", 1)[1].split("private void applyAlpha", 1)[0]
assert "new " not in draw, "Do not allocate objects in a drawable's hot path"
for forbidden in ["Handler", "Timer", "Thread", "Choreographer",
                  "ValueAnimator", "postDelayed", "while (true)", "RenderEffect"]:
    assert forbidden not in engine, f"Expensive or ongoing work found: {forbidden}"
assert engine.count("invalidateSelf()") == 2, "Only explicit alpha/filter changes may invalidate"
for entry in ["glassHeaderBackground", "glassSoftBackground", "glassAccentBackground"]:
    assert "public static android.graphics.drawable.Drawable " + entry in ui
assert ui.count("if (themeIndex(context) < 16)") == 3, "Non-Liquid themes must remain unchanged"
assert "background.setBackground(Ui.glassHeaderBackground(this, 24));" in main
assert "card.setBackground(Ui.glassSoftBackground(this, 22));" in main
assert "chartCard.setBackground(Ui.glassSoftBackground(this, 22));" in main
assert "card.setBackground(Ui.glassAccentBackground(this, 20));" in main

assert "glassNavigationBackground(this, 26)" in main
assert "glassSelectedNavigationBackground(this, 18)" in main
assert "glassActionBackground(this, 16, primary)" in main
for block in [
    "card.setBackground(Ui.glassListBackground(this, 18));",
    "card.setBackground(Ui.glassListBackground(this,18));",
    "chartCard.setBackground(Ui.glassSoftBackground(this, 22));",
    "layoutCard.setBackground(Ui.glassListBackground(this, 20));",
    "lockCard.setBackground(Ui.glassListBackground(this, 20));",
    "reminderCard.setBackground(Ui.glassListBackground(this, 20));",
]:
    assert block in main, f"Missing glass component: {block}"
for helper in ["glassListBackground", "glassNavigationBackground",
               "glassSelectedNavigationBackground", "glassActionBackground"]:
    assert " " + helper + "(" in ui
assert "new GradientDrawable(GradientDrawable.Orientation.TL_BR" in ui
assert "if (!isLiquidTheme(context))" in ui
print("PASS: component-wide Liquid glass surfaces are passive; other themes retain fallbacks")
