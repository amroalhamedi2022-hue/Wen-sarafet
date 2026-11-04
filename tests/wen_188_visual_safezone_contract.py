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
    MAIN, APP_GRADLE, SETTINGS, "gradle.properties", "proguard-rules.pro",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/app/wensarafet/tracker/NavigationVisualDecorator.java",
    "app/src/main/java/app/wensarafet/tracker/NavigationLiquidMaterial.java",
    "app/src/main/java/app/wensarafet/tracker/SplitRemainderUi.java",
    "app/src/main/java/app/wensarafet/tracker/SplitRemainderRules.java",
    "tests/SplitRemainderRulesTest.java",
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
# Exactly ONE approved functionality exception: calculate the final split UI
# amount. Restore all five additive UI hooks before byte-comparing MainActivity
# against the original 1.8.8; no DB, Save, ledger, or callback edits allowed.
split_ui_hooks = [
    (
        "        // Auto-calculate ONLY the last split entry; original save logic is unchanged.\n"
        "        SplitRemainderUi.bind(rowsBox, () -> tx.amountMinor, null);\n"
    ),
    (
        "        SplitRemainderUi.bind(splitRowsBox, () -> {\n"
        "            Long total = Money.parseMinor(amount.getText().toString());\n"
        "            return total == null ? -1L : total;\n"
        "        }, amount);\n"
    ),
    "        amount.setTag(SplitRemainderUi.AMOUNT_FIELD_TAG);\n",
    "            SplitRemainderUi.refresh(container);\n",
    "        SplitRemainderUi.refresh(container);\n",
]
cleaned_main = current_main.replace(visual_hook, "")
for hook in split_ui_hooks:
    assert cleaned_main.count(hook) == 1, "Unauthorized/missing split hook: " + repr(hook)
    cleaned_main = cleaned_main.replace(hook, "", 1)
assert cleaned_main == baseline(MAIN), (
    "SAFE ZONE: MainActivity changed outside nav presentation + the five approved split UI hooks"
)
split_ui = (ROOT / "app/src/main/java/app/wensarafet/tracker/SplitRemainderUi.java").read_text(encoding="utf-8")
split_calculation = (ROOT / "app/src/main/java/app/wensarafet/tracker/SplitRemainderRules.java").read_text(encoding="utf-8")
for forbidden in ["FinanceDbHelper", "SQLiteDatabase", "db.", "insert", "deleteTransaction",
                  "reclassifyTransaction", "setOnClickListener", "SmsCapture", "AppLockManager"]:
    assert forbidden not in split_ui + split_calculation, (
        "SAFE ZONE: split auto-remainder must not mutate financial or security logic: " + forbidden
    )
assert "Money.parseMinor" in split_ui
assert "SplitRemainderRules.lastPart" in split_ui
assert "Math.addExact" in split_calculation and "Math.subtractExact" in split_calculation
assert "derived.setKeyListener(null)" in split_ui, "Last split amount must be read-only"
assert "if (becameEarlierPart) derived.setText(\"\");" in split_ui, "Former automatic remainder must become a blank editable entry"
assert "derived.setContentDescription(\"مبلغ الجزء\");" in split_ui, "Accessibility label must be restored after adding a part"
assert "SplitRemainderUi.refresh(container);" in current_main, "Add/remove must recalculate final amount"


# The source-only overlay REMOVES (never grants) AndroidX's automatic
# signature compatibility permission. Reversing this exact removal must
# recover the 1.8.8 manifest byte-for-byte; the built APK is also audited.
manifest_path = "app/src/main/AndroidManifest.xml"
manifest = (ROOT / manifest_path).read_text(encoding="utf-8")
plain_header = '<manifest xmlns:android="http://schemas.android.com/apk/res/android"\n    package="app.wensarafet.tracker">'
guarded_header = """<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="app.wensarafet.tracker">
    <!-- Visual dependency adds a compatibility-only internal permission.
         minSdk 33 uses platform receiver flags. Remove that merged permission
         so the effective 1.8.8 APK permission set remains IDENTICAL. -->
    <permission
        android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        tools:node="remove" />
    <uses-permission
        android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        tools:node="remove" />"""
assert manifest.count(guarded_header) == 1, "Manifest overlay must only remove the added renderer permission"
assert manifest.replace(guarded_header, plain_header) == baseline(manifest_path), (
    "SAFE ZONE: production AndroidManifest changed beyond permission removal"
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
rules = (ROOT / "proguard-rules.pro").read_text(encoding="utf-8")
optional_platform_warnings = (
    "\n# Optional API 36 optical classes in the MIT lens library; guarded at runtime.\n"
    "# Narrowly suppress R8's unresolved-platform warning (no finance/security keep rule).\n"
    "-dontwarn android.graphics.RuntimeColorFilter\n"
    "-dontwarn android.graphics.RuntimeXfermode\n"
)
assert rules.count(optional_platform_warnings) == 1
assert rules.replace(optional_platform_warnings, "") == baseline("proguard-rules.pro"), (
    "SAFE ZONE: ProGuard rules changed beyond the two optional visual APIs"
)
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
assert "if (tapEligible && hovered == originIndex)" in motion, "Selected tab tap must preserve 1.8.8 refresh action"
assert "tapEligible=false;" in motion, "Dragging away and back cannot trigger accidental refresh"
assert "tabs[hovered].performClick();" in motion.split("if (hovered == selected)", 1)[1].split("} else {",1)[0], "Selected tap no longer invokes original callback"
assert "NavigationLiquidMaterial.attach(lens, track)" in adapter
assert "glass.setEnableChromaticAberration(false);" in native
assert "glass.setDispersionStrength(0f);" in native
assert "LIQUID_FOLLOW = .60f" in motion
assert "LIQUID_RELEASE_MS = 265L" in motion
assert "NAV_LIQUID_STRETCH_ACTIVE" in native

# Checksums of every remaining tracked file, including money, SQLite,
# SMS notifications, protected signing/biometric and all resource files,
# are guaranteed by the exact allowlist + git diff above.
print("PASS: 1.8.8 source immutable except visual hook + five exact split remainder UI hooks")
print("PASS: existing tab callbacks, package, version, schema and signing config unchanged")
print("PASS: protected finance, debt, SMS, backup, auth and notification trees untouched")
