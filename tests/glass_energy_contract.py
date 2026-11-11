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

assert "glassNavigationBackground(this," in main
assert "WenGoldGlassSurface.enabled(this) ? 28 : 26" in main
assert main.count("if (Ui.isLiquidTheme(this))") >= 3, "All primary tab bars need Liquid-only floating insets"
assert "fab.setBackground(Ui.glassFloatingAddBackground(this));" in main, "FAB must use colourless glass on all themes"
assert "view.getBackground() instanceof app.wensarafet.glass.WenFabBubbleDrawable" in ui
assert "new app.wensarafet.glass.WenFabBubbleDrawable(" in ui
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
assert main.count("setBackground(Ui.glassScreenBackground(this))") == 4
assert "new RadialGradient" in engine
assert "new LinearGradient" in engine
assert "innerBorder.set(surface)" in engine
assert "showLens = false;" in engine, "Sandbox cards must not render unrelated floating orbs"
assert "prismRim.setShader(new LinearGradient" in engine, "Restore fine optical rim"
assert "crown.setShader(new LinearGradient" in engine, "Restore pearly crown"
assert "final int bodyAlpha = strong ? 202 : 221;" in engine, "Keep actual background visible through glass"
assert "canvas.drawCircle(lensX, lensY, lensRadius, bubble);" in engine
assert "canvas.drawRect(surface, ambient);" in engine
assert "if (!lowPower && strong)" in engine
assert "GlassMotion.bindPress(view, SKY)" in ui, "Restore sandbox finite pressure bubble"
assert "WenFabBubbleDrawable(context, SKY, DARK)" in ui, "Restore circular original-Liquid FAB"
assert "liquid ? 2 : 10" in ui and "liquid ? 125 : 300" in ui, "Fast staged Liquid entrances"
assert "setDuration(115)" in ui, "Gentle sandbox-style screen reveal"
assert "if (isLiquidTheme(view.getContext()))" in ui
assert "if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return false;" in ui
assert "ViewPropertyAnimator" not in engine
fab_bubble = (root / "glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java").read_text(encoding="utf-8")
assert "public float getPress()" in fab_bubble and "public void setPress(float fraction)" in fab_bubble
assert "new RadialGradient" in fab_bubble, "Real pearly FAB depth must be restored"
assert "Timer" not in fab_bubble and "postDelayed" not in fab_bubble
assert "return false; // Never steal existing FAB click/hold actions." in ui

print("PASS: component-wide Liquid glass surfaces are passive; other themes retain fallbacks")
