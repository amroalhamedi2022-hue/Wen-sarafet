#!/usr/bin/env python3
"""Strict SOURCE-ONLY visual-host test. Never assembles, signs or installs APKs."""
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
trusted_main = "caf2f6eeac3a5a34fc688b21544902d7ccd13e71"
main_path = "app/src/main/java/app/wensarafet/tracker/MainActivity.java"
motion_path = "app/src/main/java/app/wensarafet/tracker/WenScreenVisualMotion.java"
fab_path = "glass-engine/src/main/java/app/wensarafet/glass/WenFabOpticalCrownDrawable.java"
bubble_path = "glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java"
ui_path = "app/src/main/java/app/wensarafet/tracker/Ui.java"


def git(*args):
    return subprocess.check_output(["git", *args], cwd=root, text=True).strip()


main = (root / main_path).read_text(encoding="utf-8")
motion = (root / motion_path).read_text(encoding="utf-8")
fab = (root / fab_path).read_text(encoding="utf-8")
bubble = (root / bubble_path).read_text(encoding="utf-8")
ui = (root / ui_path).read_text(encoding="utf-8")

# Pin original v1.9 production host exactly. The only approved changes are the
# reviewed view-only hooks in the new MainActivity blob and this new motion file.
assert git("hash-object", main_path) == (
    "491158404d66c1c2f0f0be688132cc0089e2a4f4"
), "MainActivity changed beyond reviewed host-level visual hooks"
assert git("hash-object", motion_path) == (
    "860c1a2187d7c3474b00f86d42c8500bcc189c67"
), "View-local animation or gesture safety contract changed"
assert git("show", f"{trusted_main}:app/build.gradle") == (
    root / "app/build.gradle").read_text(encoding="utf-8").rstrip(), (
    "Never change version, package, permissions or signing in a visual-only batch"
)

# All original 1.8.8 navigation/financial callbacks keep their exact targets.
for old_callback in (
    "home.setOnClickListener(v -> showMain(false));",
    "tx.setOnClickListener(v -> showMain(true));",
    "debts.setOnClickListener(v -> showDebtsScreen(FinanceRules.DEBT_RECEIVABLE));",
    "analytics.setOnClickListener(v -> showAnalyticsScreen());",
):
    assert main.count(old_callback) == 1, old_callback
assert main.count("showAddTransaction(); // exact same original action, still immediate") == 1
assert "currentTab" in main and "visualMotion.captureOriginalTabs(" in main

# The new visual layer delegates to original clicks, not transaction/DB routes.
for required in (
    "ViewAnimationUtils.createCircularReveal(", "fabOpening",
    "currentIsFabPage && targetPrimary", "introIcons(next);",
    "revealTopChrome(next);", "visualGeneration",
    "resetSwipe(screen);", "protectedHorizontalTarget(",
    "child.isClickable() || child.isLongClickable()",
    "child.canScrollHorizontally(-1) || child.canScrollHorizontally(1)",
    "child instanceof EditText", "child instanceof HorizontalScrollView",
    "child instanceof SeekBar", "destination.performClick();",
    "cancelChild.accept(cancel);", "event.getActionMasked()",
    "ValueAnimator.areAnimatorsEnabled()", "power.isPowerSaveMode()",
    "showImmediately(host, previous, next);",
):
    assert required in motion, "Missing visual safety or parity: " + required

for forbidden in (
    "FinanceDbHelper", "SQLiteDatabase", "BudgetAlertManager",
    "DebtLedgerRules", "AppLockManager", "SmsCaptureReceiver",
    "LicenseManager", "SandboxMode", "setOnClickListener(",
    "setOnLongClickListener(", "getSharedPreferences(", "showMain(",
    "showDebtsScreen(", "showAddTransaction(", "showAnalyticsScreen(",
    "postDelayed(", "while (true)", "TimerTask",
):
    assert forbidden not in motion, "Motion layer cannot reach business logic: " + forbidden

# The FAB optical crown is a pure drawable mounted ABOVE the plus glyph via
# Android ViewOverlay; it has no touch target or recurring render loop.
assert "extends Drawable" in fab
assert "public void setPress(float fraction)" in fab
assert "new LinearGradient" in fab
assert "private final Matrix shift = new Matrix();" in fab
draw = fab.split("@Override public void draw(Canvas canvas)", 1)[1].split(
    "@Override public void setAlpha", 1)[0]
assert "new " not in draw, "No allocations on the 120Hz drawing hot path"
for forbidden in (
    "View.OnTouchListener", "setOnTouchListener", "OnClickListener",
    "FinanceDbHelper", "AppLockManager", "SQLiteDatabase",
    "Handler", "Timer", "postDelayed", "Thread", "Choreographer",
):
    assert forbidden not in fab, "Crown cannot receive touches or touch protected features"

assert "WenFabOpticalCrownDrawable" in ui
assert "view.getOverlay().add(crown);" in ui
assert "v.getOverlay().remove(crown);" in ui
assert "crown.setPress(t);" in ui
assert "return false; // Never steal existing FAB click/hold actions." in ui
assert "WenFabBubbleDrawable" in bubble
assert "public void setPress(float fraction)" in bubble

# This review branch retains the verified 2.0 release candidate build identity.
assert 'versionCode 28' in (root / "app/build.gradle").read_text(encoding="utf-8")
assert 'versionName "2.0"' in (root / "app/build.gradle").read_text(encoding="utf-8")
print("PASS: Sandbox circular reveal, tab swipe, top-chrome/icon and over-glyph "
      "FAB optics are source-staged with finance, security, signing and old clicks frozen")
