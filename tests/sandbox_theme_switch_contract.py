#!/usr/bin/env python3
"""Source-only regression check for sandbox palette switches seen in the 2026-11-02 video.

The sandbox must not restart the Activity for light-to-light theme changes.
This does NOT replace device frame timing, animation, or UI automation tests.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
main = (root / "app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
ui = (root / "app/src/main/java/app/wensarafet/tracker/Ui.java").read_text(encoding="utf-8")

settings = main.split("private void showSettings() {", 1)[1].split(
    "private void showHomeLayoutSettings()", 1)[0]
assert "if (index == Ui.themeIndex(this)) return;" in settings
assert "boolean previousDark = Ui.DARK;" in settings
assert "Ui.saveTheme(this, index);" in settings
assert "if (!SandboxMode.isSandbox(this) || previousDark != Ui.DARK)" in settings
assert "recreate();" in settings, "Original app / dark mode still uses Activity recreation"
assert "pendingThemeScrollX = themeScroll.getScrollX();" in settings
assert "themeScroll.post(() -> themeScroll.scrollTo(restoreX, 0));" in settings
assert "pendingPaletteSwitch = true;" in settings
assert "showSettings();" in settings
assert "screenHost.setBackgroundColor(Ui.BACKGROUND);" in settings

navigation = main.split("@Override public void setContentView(View next) {", 1)[1].split(
    "@Override protected void onCreate(", 1)[0]
assert "boolean paletteSwitch = pendingPaletteSwitch;" in navigation
assert "pendingPaletteSwitch = false;" in navigation
assert "SandboxMode.isSandbox(this) && (Ui.isLiquidTheme(this) || paletteSwitch)" in navigation
assert "if (fromNavGesture || paletteSwitch)" in navigation
assert "next.setAlpha(1f);" in navigation

assert "public static void saveTheme(Context context, int index)" in ui
assert "applyTheme(context);" in ui

# Prevent unintended edits to the finance / database modules during this UI-only task.
print("PASS: sandbox palette keeps Activity + carousel; production and night mode retain fallback")
