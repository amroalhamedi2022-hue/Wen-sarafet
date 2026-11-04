#!/usr/bin/env python3
"""Contracts for the reusable visual-only, battery-aware Glass Engine pilot."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
engine = (root / "glass-engine/src/main/java/app/wensarafet/glass/WenGlassDrawable.java").read_text(encoding="utf-8")
ui = (root / "app/src/main/java/app/wensarafet/tracker/Ui.java").read_text(encoding="utf-8")
main = (root / "app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")

assert "package app.wensarafet.glass;" in engine
assert 'implementation project(":glass-engine")' in (root / "app/build.gradle").read_text(encoding="utf-8")
assert 'include(":app", ":license-generator", ":glass-engine")' in (root / "settings.gradle").read_text(encoding="utf-8")
assert not (root / "app/src/main/java/app/wensarafet/glass/WenGlassDrawable.java").exists(), "Engine must be sourced only from the reusable module"
assert "tracker." not in engine, "Engine must remain independent of finance app"
assert "power.isPowerSaveMode()" in engine
assert "activity.isLowRamDevice()" in engine
assert "if (lowPower)" in engine
assert "canvas.drawRoundRect(surface, radiusPx, radiusPx, fill);" in engine
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
assert "card.setBackground(Ui.glassBalanceBackground(this, 22));" in main
assert "chartCard.setBackground(Ui.glassChartBackground(this, 22));" in main
assert "card.setBackground(Ui.glassAccentBackground(this, 20));" in main

assert "glassNavigationBackground(this, 26)" in main
assert main.count("if (Ui.isLiquidTheme(this))") >= 3, "All primary tab bars need Liquid-only floating insets"
assert "fab.setBackground(Ui.isLiquidTheme(this) ?" in main
assert "debtNavParams.setMargins" in main
assert "analyticsNavParams.setMargins" in main
assert "glassSelectedNavigationBackground(this, 18)" in main
assert "glassActionBackground(this, 16, primary)" in main
for block in [
    "card.setBackground(Ui.glassListBackground(this, 18));",
    "card.setBackground(Ui.glassListBackground(this,18));",
    "chartCard.setBackground(Ui.glassChartBackground(this, 22));",
    "layoutCard.setBackground(Ui.glassListBackground(this, 20));",
    "lockCard.setBackground(Ui.glassListBackground(this, 20));",
    "reminderCard.setBackground(Ui.glassListBackground(this, 20));",
]:
    assert block in main, f"Missing glass component: {block}"
for helper in ["glassListBackground", "glassNavigationBackground",
               "glassSelectedNavigationBackground", "glassActionBackground"]:
    assert " " + helper + "(" in ui
assert "new GradientDrawable(GradientDrawable.Orientation.TL_BR" in ui
assert "glassBalanceBackground(Context context" in ui
assert "glassChartBackground(Context context" in ui
assert "int first = mix(SURFACE, BRAND_SOFT, DARK ? .10f : .64f);" in ui
assert main.count("chartCard.setBackground(Ui.glassChartBackground(this, 22));") == 2
assert "if (!isLiquidTheme(context))" in ui
assert "return index >= 16 && index < THEMES.length;" in ui
assert "DARK = theme == 20 ||" in ui
for name in ["زجاج سائل أزرق", "فقاعات أكوا", "أورورا ليلية", "لؤلؤي متجمد"]:
    assert name in ui

# Optical parity contract: cached radial bloom and double glass rim, zero redraw loop.
backdrop = (root / "glass-engine/src/main/java/app/wensarafet/glass/WenGlassBackdropDrawable.java").read_text(encoding="utf-8")
assert "new RadialGradient" in backdrop
assert "onBoundsChange" in backdrop
assert "if (!lowPower)" in backdrop
assert "if (!lowPower) {" in backdrop.split("@Override public void draw(Canvas canvas)",1)[1]
assert "new " not in backdrop.split("@Override public void draw(Canvas canvas)",1)[1].split("@Override public void setAlpha",1)[0]
assert "glassScreenBackground(this)" in main
assert main.count("setBackground(Ui.glassScreenBackground(this))") >= 4, "Original four scene backgrounds must remain"
assert "scroll.setBackground(Ui.glassScreenBackground(this));" in main, "Frosted footer reuses scene background"
assert "new RadialGradient" in engine
assert "new LinearGradient" in engine
assert "innerBorder.set(surface)" in engine
assert "showLens = false;" in engine, "No unrelated ornament orb may appear on glass cards"
assert "prismRim.setShader(new LinearGradient" in engine
assert "crown.setShader(new LinearGradient" in engine
assert "final int bodyAlpha = strong ? 202 : 221;" in engine, "Glass surfaces must transmit background"
assert "canvas.drawCircle(lensX, lensY, lensRadius, bubble);" in engine, "Legacy conditional fallback stays safe"
assert "canvas.drawRect(surface, ambient);" in engine
assert "if (!lowPower && strong)" in engine
# Liquid press behavior now lives in the reusable engine; legacy themes are untouched.
motion = (root / "glass-engine/src/main/java/app/wensarafet/glass/GlassMotion.java").read_text(encoding="utf-8")
assert "GlassMotion.bindPress(view, SKY)" in ui
assert "new OvershootInterpolator(.76f)" in motion
assert "ValueAnimator.areAnimatorsEnabled()" in motion
assert "if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return false;" in ui
assert "ViewPropertyAnimator" not in engine
print("PASS: component-wide Liquid glass surfaces are passive; other themes retain fallbacks")
