#!/usr/bin/env python3
"""Hard fail on ANY functional alteration while porting the visual lens onto signed 1.8.8 source."""
from pathlib import Path
import subprocess
import re

ROOT = Path(__file__).resolve().parents[1]
BASE = "a78ce29603daf3f64efb52f15aca17e17c38a8b9"
MAIN = "app/src/main/java/app/wensarafet/tracker/MainActivity.java"
APP_GRADLE = "app/build.gradle"
SETTINGS = "settings.gradle"
ALLOWED = {
    MAIN, APP_GRADLE, SETTINGS, "gradle.properties", "proguard-rules.pro",
    "app/src/main/java/app/wensarafet/tracker/Ui.java",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/app/wensarafet/tracker/NavigationVisualDecorator.java",
    "app/src/main/java/app/wensarafet/tracker/NavigationLiquidMaterial.java",
    "app/src/main/java/app/wensarafet/tracker/SplitRemainderUi.java",
    "app/src/main/java/app/wensarafet/tracker/SplitRemainderRules.java",
    "tests/SplitRemainderRulesTest.java",
    "app/src/androidTest/java/app/wensarafet/tracker/SplitRemainderInstrumentedTest.java",
    ".github/workflows/wen-188-split-ui-emulator.yml",
    "glass-engine/src/main/java/app/wensarafet/glass/GlassMotion.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenNavigationTrack.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenNavigationLensDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenOpticalNavigationOverlay.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenLiquidRuntimeOptics.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenReferenceGlassDrawable.java",
    "app/src/androidTest/java/app/wensarafet/tracker/ReferenceThemesInstrumentedTest.java",
    "docs/licenses/QWEA0_LiquidGlass_MIT.txt",
    "docs/design/WEN_188_VISUAL_REVIEW_EXECUTION_20261104.md",
    ".github/workflows/wen-188-visual-safe-review.yml",
    "tests/wen_188_visual_safezone_contract.py",
    ".github/workflows/unsigned-release-candidate.yml",
    "scripts/verify_upgrade_apk.sh",
    "tests/original_180_upgrade_contract.py",
    "tests/upgrade_gate_contract.py",
    "docs/design/WEN_190_REFERENCE_THEMES_20261105.md",
    "tests/glass_energy_contract.py",
    "tests/batch22_category_budget_icon_contract.py",
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
# Approved split EDIT display fix: cents are never passed through binary double.
# Restore this one presentation-only line before comparing MainActivity to
# signed 1.8.8. Persisted values, debt ledger, DB and save handlers stay frozen.
exact_display = "            row.amount.setText(SplitRemainderUi.formatMinor(split.amountMinor));\n"
previous_display = '            row.amount.setText(String.format(Locale.ROOT, "%.2f", split.amountMinor / 100.0));\n'
assert current_main.count(exact_display) == 1, "SAFE ZONE: split edit should display exact stored minor units"
# The sole production MainActivity presentation change for reference themes:
# same original FAB click handler, position and dimensions, new drawable only.
old_fab = """        fab.setBackground(Ui.isLiquidTheme(this) ? Ui.glassAccentBackground(this, 24)
                : Ui.gradient(Ui.BRAND, Ui.VIOLET, 24, this));"""
new_fab = """        // Two new reference themes render a circular optical material here.
        // The original add callback, size, position and hold feedback are unchanged.
        fab.setBackground(Ui.isReferenceGlassTheme(this)
                ? Ui.glassFloatingAddBackground(this)
                : (Ui.isLiquidTheme(this) ? Ui.glassAccentBackground(this, 24)
                : Ui.gradient(Ui.BRAND, Ui.VIOLET, 24, this)));"""
assert current_main.count(new_fab) == 1, "SAFE ZONE: only add-button appearance is approved"
cleaned_main = current_main.replace(new_fab, old_fab).replace(visual_hook, "")
for hook in split_ui_hooks:
    assert cleaned_main.count(hook) == 1, "Unauthorized/missing split hook: " + repr(hook)
    cleaned_main = cleaned_main.replace(hook, "", 1)
assert cleaned_main.count(exact_display) == 1
cleaned_main = cleaned_main.replace(exact_display, previous_display, 1)
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
assert "BigDecimal.valueOf(minorUnits, 2).toPlainString()" in split_ui, "Exact edit display must avoid doubles"
assert "entry.getKey().removeTextChangedListener(entry.getValue());" in split_ui, "Removed split fields must detach stale observers"
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
test_runner = (
    '        // Test APK only; the release applicationId, permissions and signing stay identical.\n'
    '        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"\n'
)
test_deps = (
    '    // Android tests only: none of these dependencies enter the production APK.\n'
    '    androidTestImplementation "androidx.test:core:1.6.1"\n'
    '    androidTestImplementation "androidx.test:runner:1.6.2"\n'
    '    androidTestImplementation "androidx.test.ext:junit:1.2.1"\n'
)
assert current_gradle.count(test_runner) == 1 and current_gradle.count(test_deps) == 1, (
    "Instrumentation changes must be test APK only"
)
# Packaging-only release bump: remove exact approved version deltas before
# comparing the entire Gradle signing/package/dependency configuration.
restored_gradle = current_gradle.replace(test_runner, "").replace(test_deps, "").replace(dependency, "")
assert restored_gradle.count('versionCode 27') == 1
assert restored_gradle.count('versionName "1.9"') == 1
restored_gradle = restored_gradle.replace("versionCode 27", "versionCode 24").replace('versionName "1.9"', 'versionName "1.8.8"')
assert restored_gradle == baseline(APP_GRADLE), (
    "SAFE ZONE: unexpected build, package, version, dependency or signing mutation"
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
assert 'versionName "1.9"' in current_gradle
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
assert "versionCode 27" in current_gradle
assert 'applicationId "app.wensarafet.tracker"' in current_gradle

# Release workflow identity must advance in lockstep with the package.
release_workflow = (ROOT / ".github/workflows/unsigned-release-candidate.yml").read_text(encoding="utf-8")
expected_release = baseline(".github/workflows/unsigned-release-candidate.yml")
expected_release = expected_release.replace("Wen-Sarafet-1.8.8-aligned-UNSIGNED.apk", "Wen-Sarafet-1.9-aligned-UNSIGNED.apk")
expected_release = expected_release.replace("versionCode='24' versionName='1.8.8'", "versionCode='27' versionName='1.9'")
expected_release = expected_release.replace("wen-1-8-8-original-package-unsigned", "wen-1-9-original-package-unsigned")
assert release_workflow == expected_release, "SAFE ZONE: unexpected production release workflow mutation"
review_workflow = (ROOT / ".github/workflows/wen-188-visual-safe-review.yml").read_text(encoding="utf-8")
assert review_workflow.count("versionCode='27' versionName='1.9'") == 1

# No change to authentication/signing data; only permit predecessor 1.8.8
# into the existing static signed-APK upgrade checker.
upgrade = (ROOT / "scripts/verify_upgrade_apk.sh").read_text(encoding="utf-8")
expected_upgrade = baseline("scripts/verify_upgrade_apk.sh")
expected_upgrade = expected_upgrade.replace("INSTALLED_1.8.0_TO_1.8.7.apk SIGNED_CANDIDATE_1.8.8.apk", "INSTALLED_SIGNED_1.8.x.apk NEW_SIGNED_UPDATE.apk")
expected_upgrade = expected_upgrade.replace('"$old_version" -ne 23 )', '"$old_version" -ne 23 && "$old_version" -ne 24 )')
expected_upgrade = expected_upgrade.replace("or 1.8.7 (23), and update must have a higher versionCode", "1.8.7 (23), or 1.8.8 (24), and update must have a higher versionCode")
assert upgrade == expected_upgrade, "SAFE ZONE: changed signing upgrade checks beyond allowed predecessor"

# Permit only the exact test-fixture updates needed for a code26 original
# signer upgrade from code24; the actual signature and package checker is fixed.
expected_tests = baseline("tests/upgrade_gate_contract.py")
expected_tests = expected_tests.replace('assert parsed.stdout == "24|1.8.8", parsed', 'assert parsed.stdout == "27|1.9", parsed')
expected_tests = expected_tests.replace("      *current-187*|*same-as-current*) code=23 ;;", "      *current-187*|*same-as-current*) code=23 ;;\n      *current-188*) code=24 ;;")
expected_tests = expected_tests.replace("      *) code=24 ;;", "      *) code=27 ;;")
expected_tests = expected_tests.replace("versionName='1.8.8'", "versionName='1.9'")
expected_tests = expected_tests.replace('    stale = home / "same-as-current.apk"', """    trusted188 = home / "current-188.apk"
    trusted188.write_bytes(b"fake owner original signed version 1.8.8")
    verified188 = subprocess.run(
        ["bash", str(checker), str(trusted188), str(home / "candidate.apk")],
        env=env, capture_output=True, text=True,
    )
    assert verified188.returncode == 0 and "24 -> 27" in verified188.stdout, (
        verified188.stdout, verified188.stderr
    )
    stale = home / "same-as-current.apk" """.rstrip())
assert (ROOT / "tests/upgrade_gate_contract.py").read_text(encoding="utf-8") == expected_tests, "SAFE ZONE: release upgrade test changed beyond original-signer and version coverage"

# The trusted upgrade contract may only advance the expected version; it
# must still validate the ORIGINAL app signer, license and install identity.
old_guard = baseline("tests/original_180_upgrade_contract.py")
expected_guard = old_guard.replace('assert re.search(r"versionCode\\s+24\\b", gradle)', 'assert re.search(r"versionCode\\s+27\\b", gradle)')
expected_guard = expected_guard.replace("""assert 'versionName "1.8.8"' in gradle""", """assert 'versionName "1.9"' in gradle""")
expected_guard = expected_guard.replace("original certificate, package, keystore identity, license and version retained", "original certificate, package, keystore identity and license retained; audited version 1.9")
assert (ROOT / "tests/original_180_upgrade_contract.py").read_text(encoding="utf-8") == expected_guard, "SAFE ZONE: unapproved original signing contract change"

# Protect legacy themes and every persisted preference index:
# only two brand-new palette rows may follow the original 22 names/rows.
original_ui = baseline("app/src/main/java/app/wensarafet/tracker/Ui.java")
new_ui = (ROOT / "app/src/main/java/app/wensarafet/tracker/Ui.java").read_text(encoding="utf-8")
def palette_block(text, label):
    start = text.index(label)
    return text[start:].split("};", 1)[0]
def palette_rows(text, label):
    return re.findall(r"\{([^{}]+)\}", palette_block(text, label))
old_names = re.findall(r'"([^"]+)"', palette_block(original_ui, "public static final String[] THEME_NAMES"))
new_names = re.findall(r'"([^"]+)"', palette_block(new_ui, "public static final String[] THEME_NAMES"))
assert len(old_names) == 22, "Unexpected source baseline for themes"
assert len(new_names) == 24 and new_names[:22] == old_names, "SAFE ZONE: legacy theme names or indices changed"
for name in ("private static final int[][] THEMES", "private static final int[][] THEME_LAYERS"):
    old_rows, new_rows = palette_rows(original_ui, name), palette_rows(new_ui, name)
    assert len(old_rows) == 22 and len(new_rows) == 24
    assert new_rows[:22] == old_rows, "SAFE ZONE: original theme palette changed: " + name
assert "Prism Glow" in new_names[22] and "Golden Bloom" in new_names[23]
assert 'getSharedPreferences("appearance", Context.MODE_PRIVATE)' in new_ui
assert "isReferenceGlassTheme(context)" in new_ui
draw_source = (ROOT / "glass-engine/src/main/java/app/wensarafet/glass/WenReferenceGlassDrawable.java").read_text(encoding="utf-8")
assert "PRISM = 22" in draw_source and "BLOOM = 23" in draw_source
assert "PowerManager" in draw_source and "isLowRamDevice" in draw_source
for forbidden in ("FinanceDbHelper", "AppLockManager", "SQLiteDatabase", "OnClickListener", "LicenseManager", "SharedPreferences", "Handler(", "Choreographer.getInstance("):
    assert forbidden not in draw_source, "Theme renderer enters protected logic: " + forbidden

# The archived Batch22 suite must still check ALL 22 old IDs in place;
# only its expected array tail and total length are extended to 24.
old_batch22 = baseline("tests/batch22_category_budget_icon_contract.py")
expected_batch22 = old_batch22.replace(
    'assert theme_names[18:] == ["زجاج سائل أزرق", "فقاعات أكوا", "أورورا ليلية", "لؤلؤي متجمد"], theme_names',
    'assert theme_names[18:22] == ["زجاج سائل أزرق", "فقاعات أكوا", "أورورا ليلية", "لؤلؤي متجمد"], theme_names\n'
    'assert theme_names[22:] == ["Prism Glow • زجاج نيوني", "Golden Bloom • زجاج شمسي"], theme_names')
expected_batch22 = expected_batch22.replace(
    'assert len(rows) == len(theme_names) == 22, (name, len(rows))',
    'assert len(rows) == len(theme_names) == 24, (name, len(rows))')
expected_batch22 = expected_batch22.replace(
    '18 preserved IDs plus four new glass themes passed',
    '22 preserved IDs plus two additional reference glass themes passed')
assert (ROOT / "tests/batch22_category_budget_icon_contract.py").read_text(encoding="utf-8") == expected_batch22, (
    "SAFE ZONE: original category and layout contracts changed beyond two new theme indices"
)

# Preserve the legacy passive-glass assertions while updating ONLY the
# original FAB opt-in predicate to the new two-theme visual adapter.
old_energy = baseline("tests/glass_energy_contract.py")
expected_energy = old_energy.replace(
    'assert "fab.setBackground(Ui.isLiquidTheme(this) ?" in main',
    '''assert "fab.setBackground(Ui.isReferenceGlassTheme(this)" in main
assert "? Ui.glassFloatingAddBackground(this)" in main
assert ": (Ui.isLiquidTheme(this) ? Ui.glassAccentBackground(this, 24)" in main''')
assert (ROOT / "tests/glass_energy_contract.py").read_text(encoding="utf-8") == expected_energy, (
    "SAFE ZONE: original glass energy regression changed beyond new FAB opt-in"
)

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
print("PASS: original package, schema and signing unchanged; audited version 1.9/code27")
print("PASS: protected finance, debt, SMS, backup, auth and notification trees untouched")
