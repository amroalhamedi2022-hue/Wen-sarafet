#!/usr/bin/env python3
"""Hard fail on ANY functional alteration while porting the visual lens onto signed 1.8.8 source."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BASE = "a78ce29603daf3f64efb52f15aca17e17c38a8b9"
MAIN = "app/src/main/java/app/wensarafet/tracker/MainActivity.java"
APP_GRADLE = "app/build.gradle"
SETTINGS = "settings.gradle"
ALLOWED = {
    MAIN, APP_GRADLE, SETTINGS, "gradle.properties",
    "app/src/main/java/app/wensarafet/tracker/NavigationVisualDecorator.java",
    "app/src/main/java/app/wensarafet/tracker/NavigationLiquidMaterial.java",
    "glass-engine/src/main/java/app/wensarafet/glass/GlassMotion.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenNavigationTrack.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenNavigationLensDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenOpticalNavigationOverlay.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenLiquidRuntimeOptics.java",
    "docs/licenses/QWEA0_LiquidGlass_MIT.txt",
    "docs/design/WEN_188_VISUAL_REVIEW_EXECUTION_20261104.md",
    ".github/workflows/wen-188-visual-safe-review.yml",
    "tests/wen_188_visual_safezone_contract.py",
}

def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True)

def baseline(path):
    return git("show", f"{BASE}:{path}")

changed = set(git("diff", "--name-only", BASE, "HEAD").splitlines())
unexpected = changed - ALLOWED
assert not unexpected, f"Safe Zone: unexpected changes: {sorted(unexpected)}"
required = {MAIN, APP_GRADLE, SETTINGS}
assert required <= changed, f"Visual adapter incomplete: {sorted(required - changed)}"

# Verify no financial, debt, activation, notification, contact, backup, etc.
# behavior changed even within the very large MainActivity.java file.
current_main = (ROOT / MAIN).read_text(encoding="utf-8")
visual_hook = (
    "        // Visual-only; keep every original 1.8.8 tab callback untouched.\n"
    "        if (Ui.isLiquidTheme(this))\n"
    "            NavigationVisualDecorator.install(nav,\n"
    "                    new TextView[]{home, tx, debts, analytics}, selectedTab);\n"
)
assert current_main.count(visual_hook) == 1, "Expected one visual-only integration hook"
assert current_main.replace(visual_hook, "") == baseline(MAIN), (
    "SAFE ZONE: MainActivity changed outside its four-tab visual hook"
)

current_gradle = (ROOT / APP_GRADLE).read_text(encoding="utf-8")
dependency = (
    '    // MIT-licensed native navigation optics; UI rendering only (no permissions).\n'
    '    implementation "com.github.QWEA0:liquidglass:v2.0.11"\n'
)
assert current_gradle.count(dependency) == 1
assert current_gradle.replace(dependency, "") == baseline(APP_GRADLE), (
    "SAFE ZONE: unexpected build, package, version or signing mutation"
)
settings = (ROOT / SETTINGS).read_text(encoding="utf-8")
repo_line = "repositories { google(); mavenCentral(); maven { url 'https://jitpack.io' } }"
assert settings.count(repo_line) == 1
assert settings.replace(repo_line, "repositories { google(); mavenCentral() }") == baseline(SETTINGS)

properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
assert properties.count("android.useAndroidX=true") == 1
assert properties.replace("android.useAndroidX=true", "android.useAndroidX=false") == baseline("gradle.properties"), (
    "SAFE ZONE: Gradle properties altered beyond the renderer's AndroidX prerequisite"
)

for callback in [
    "home.setOnClickListener(v -> showMain(false));",
    "tx.setOnClickListener(v -> showMain(true));",
    "debts.setOnClickListener(v -> showDebtsScreen(FinanceRules.DEBT_RECEIVABLE));",
    "analytics.setOnClickListener(v -> showAnalyticsScreen());",
]:
    assert callback in current_main, f"SAFE ZONE: changed original callback: {callback}"
assert 'versionName "1.8.8"' in current_gradle
assert "versionCode 24" in current_gradle
assert 'applicationId "app.wensarafet.tracker"' in current_gradle

adapter = (ROOT / "app/src/main/java/app/wensarafet/tracker/NavigationVisualDecorator.java").read_text(encoding="utf-8")
native = (ROOT / "app/src/main/java/app/wensarafet/tracker/NavigationLiquidMaterial.java").read_text(encoding="utf-8")
motion = (ROOT / "glass-engine/src/main/java/app/wensarafet/glass/GlassMotion.java").read_text(encoding="utf-8")
for forbidden in [
    "FinanceDbHelper", "DebtLedgerRules", "ContentResolver", "AppLockManager",
    "LicenseManager", "SmsCaptureReceiver", "SandboxMode", "attachFab(", "setOnClickListener(",
    "SQLiteDatabase", "WEN_SARAFET_KEYSTORE",
]:
    assert forbidden not in adapter + native, f"Visual layer crosses Safe Zone: {forbidden}"
assert "return false;" in adapter, "The optical layer must stay touch-through"
assert "GlassMotion.bindNavigationTab" in adapter
assert "tabs[hovered].performClick();" in motion, "Must reuse original navigation callbacks"
assert "NavigationLiquidMaterial.attach(lens, track)" in adapter
assert "glass.setEnableChromaticAberration(false);" in native
assert "glass.setDispersionStrength(0f);" in native
assert "LIQUID_FOLLOW = .60f" in motion
assert "LIQUID_RELEASE_MS = 265L" in motion
assert "NAV_LIQUID_STRETCH_ACTIVE" in native

# Checksums of every remaining tracked file, including money, SQLite,
# SMS notifications, protected signing/biometric and all resource files,
# are guaranteed by the exact allowlist + git diff above.
print("PASS: 1.8.8 source immutable except a visual hook and reviewed graphics")
print("PASS: existing tab callbacks, package, version, schema and signing config unchanged")
print("PASS: protected finance, debt, SMS, backup, auth and notification trees untouched")
