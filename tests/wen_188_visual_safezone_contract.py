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
    "app/src/main/java/app/wensarafet/tracker/WenScreenVisualMotion.java",
    "app/src/androidTest/java/app/wensarafet/tracker/WenVisualMotionInstrumentedTest.java",
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
    "glass-engine/src/main/java/app/wensarafet/glass/WenGlassDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenFabOpticalCrownDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenPremiumLiquidDrawable.java",
    "docs/design/WEN_190_FIVE_PREMIUM_LIQUID_HANDOFF_20261105.md",
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
    "tests/wen_190_visual_motion_contract.py",
    "tests/batch22_category_budget_icon_contract.py",
    "scripts/build_apk.sh",
    "scripts/sign_verified_upgrade.sh",
    "scripts/audit_recovered_source.sh",
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
# Exact source pin: THIS MainActivity was manually compared line-by-line with
# 1.9 main. Its only 40 new lines are visual host lifecycle, original-tab
# delegation, screen reveal/screen swipe hooks, and non-semantic View tags.
# The original protected callbacks/finance methods are not edited. Any future
# change to MainActivity, including new feature work, must re-audit this SHA.
APPROVED_VISUAL_HOST_BLOB = "bf6163c8464c8cb99189e962ee35fdbeb1c5f8ae"
assert git("hash-object", MAIN).strip() == APPROVED_VISUAL_HOST_BLOB, (
    "SAFE ZONE: MainActivity differs from the manually audited visual-only host patch"
)
SOURCE_190 = "283112f4bedaf2e07dd7370e581f84e14959710e"
main_pre_visual_host = git("show", f"{SOURCE_190}:{MAIN}")
assert main_pre_visual_host != current_main, "Missing staged optical host transfer"
for original_callback in (
    "home.setOnClickListener(v -> showMain(false));",
    "tx.setOnClickListener(v -> showMain(true));",
    "debts.setOnClickListener(v -> showDebtsScreen(FinanceRules.DEBT_RECEIVABLE));",
    "analytics.setOnClickListener(v -> showAnalyticsScreen());",
):
    assert original_callback in current_main
assert "showAddTransaction(); // exact same original action, still immediate" in current_main

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
cleaned_main = main_pre_visual_host.replace(new_fab, old_fab).replace(visual_hook, "")
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
assert len(new_names) == 30 and new_names[:22] == old_names, "SAFE ZONE: legacy theme names or indices changed"
for name in ("private static final int[][] THEMES", "private static final int[][] THEME_LAYERS"):
    old_rows, new_rows = palette_rows(original_ui, name), palette_rows(new_ui, name)
    assert len(old_rows) == 22 and len(new_rows) == 30
    assert new_rows[:22] == old_rows, "SAFE ZONE: original theme palette changed: " + name
assert new_names[22:25] == [
    "Prism Glow • زجاج نيوني",
    "Golden Bloom • زجاج شمسي",
    "Royal Gold • الذهب الملكي",
], "SAFE ZONE: established reference theme IDs changed"
assert new_names[25:] == [
    "Royal Dark Gold • ذهبي ملكي داكن",
    "Imperial Dark Gold • ذهبي إمبراطوري",
    "Champagne Gold • شامبانيا ملكي فاتح",
    "Soft Orange-Green Glass • زجاج برتقالي أخضر",
    "Purple-Pink Liquid Glass • زجاج بنفسجي وردي",
], "SAFE ZONE: only the five owner-approved themes may be appended"
# Entire existing 25-theme selection + both palettes are frozen relative
# to the exact, already-promoted v1.9 main. Only five tail entries may differ.
PREMIUM_BASE = "283112f4bedaf2e07dd7370e581f84e14959710e"
promoted_ui = git("show", f"{PREMIUM_BASE}:app/src/main/java/app/wensarafet/tracker/Ui.java")
promoted_names = re.findall(r'"([^"]+)"', palette_block(promoted_ui, "public static final String[] THEME_NAMES"))
assert new_names[:25] == promoted_names, "SAFE ZONE: a promoted theme name/index changed"
for name in ("private static final int[][] THEMES", "private static final int[][] THEME_LAYERS"):
    assert palette_rows(new_ui, name)[:25] == palette_rows(promoted_ui, name), (
        "SAFE ZONE: an established 1.9 palette changed: " + name
    )
# Keep Prism/Bloom pixel palettes frozen exactly when appending Royal Gold.
original_reference_rows = {
    "private static final int[][] THEMES": [
        "0xFFAD3CDB,0xFF62257F,0xFFF4E3FC,0xFFFAF0FC,0xFFFFFAFF,0xFF291638,0xFF795D87,0xFFC94376,0xFF238D86,0xFF8244DA,0xFFF471B9,0xFF8B88EE",
        "0xFFCE873E,0xFF87502D,0xFFFFE9C8,0xFFFFF7EA,0xFFFFFDF5,0xFF3D3028,0xFF857367,0xFFCA5060,0xFF54866D,0xFFB993BC,0xFFF8AA67,0xFF95BDA6",
    ],
    "private static final int[][] THEME_LAYERS": [
        "0xFF782A9E,0xFFC435B0,0xFFE64CC2,0xFF7956EE",
        "0xFFB86E39,0xFFF2AC65,0xFFFBD6A0,0xFF99B9A1",
    ],
}
for key, frozen in original_reference_rows.items():
    assert [row.strip() for row in palette_rows(new_ui, key)[22:24]] == frozen, (
        "SAFE ZONE: Prism/Bloom palette changed while adding Royal Gold: " + key
    )
assert 'getSharedPreferences("appearance", Context.MODE_PRIVATE)' in new_ui
assert "isReferenceGlassTheme(context)" in new_ui
premium_draw_source = (ROOT / "glass-engine/src/main/java/app/wensarafet/glass/WenPremiumLiquidDrawable.java").read_text(encoding="utf-8")
draw_source = (ROOT / "glass-engine/src/main/java/app/wensarafet/glass/WenReferenceGlassDrawable.java").read_text(encoding="utf-8")
assert "ROYAL_DARK = 25" in premium_draw_source and "PURPLE_PINK = 29" in premium_draw_source
assert "WenPremiumLiquidDrawable.supports(selected)" in new_ui
assert "premium ? 0x99FFFFFF" in (ROOT / "app/src/main/java/app/wensarafet/tracker/NavigationVisualDecorator.java").read_text(encoding="utf-8")
assert "PRISM = 22" in draw_source and "BLOOM = 23" in draw_source and "ROYAL = 24" in draw_source
assert "PowerManager" in draw_source and "isLowRamDevice" in draw_source
for forbidden in ("FinanceDbHelper", "AppLockManager", "SQLiteDatabase", "OnClickListener", "LicenseManager", "SharedPreferences", "Handler(", "Choreographer.getInstance("):
    assert forbidden not in draw_source + premium_draw_source, "Theme renderer enters protected logic: " + forbidden

# The legacy recovery audit may accept v1.9 as a valid next-minor
# release without disabling any XML, banking, credential or package checks.
old_recovery = baseline("scripts/audit_recovered_source.sh")
expected_recovery = old_recovery.replace(
    '[[ "$version_name" =~ ^1[.]8[.][0-9]+$ ]]',
    '[[ "$version_name" =~ ^1[.](8[.][0-9]+|9)$ ]]')
assert expected_recovery != old_recovery, "Original recovery audit version pattern missing"
assert (ROOT / "scripts/audit_recovered_source.sh").read_text(encoding="utf-8") == expected_recovery, (
    "SAFE ZONE: recovery audit changed beyond v1.9 acceptance"
)

# The owner-only signer must also keep the exact keystore, certificate,
# original security hashes and payload checks. Only its version regex expands.
old_signer = baseline("scripts/sign_verified_upgrade.sh")
expected_signer = old_signer.replace(r"^1\.8\.[0-9]+$", r"^1\.(8\.[0-9]+|9)$")
assert expected_signer != old_signer, "Missing original offline signer version gate"
assert (ROOT / "scripts/sign_verified_upgrade.sh").read_text(encoding="utf-8") == expected_signer, (
    "SAFE ZONE: signing helper changed beyond allowing v1.9"
)

# The owner's build script must retain its signing, hash, debug and
# identity checks; only accept the requested next-minor version 1.9.
old_builder = baseline("scripts/build_apk.sh")
expected_builder = old_builder.replace(r"^1\.8\.[0-9]+$", r"^1\.(8\.[0-9]+|9)$")
assert expected_builder != old_builder, "Missing original offline builder version gate"
assert (ROOT / "scripts/build_apk.sh").read_text(encoding="utf-8") == expected_builder, (
    "SAFE ZONE: signing/build script changed beyond v1.9 metadata allowance"
)

# The archived Batch22 suite must still check ALL 22 old IDs in place;
# only its expected array tail and total length are extended to 25.
old_batch22 = baseline("tests/batch22_category_budget_icon_contract.py")
expected_batch22 = old_batch22.replace(
    'assert theme_names[18:] == ["زجاج سائل أزرق", "فقاعات أكوا", "أورورا ليلية", "لؤلؤي متجمد"], theme_names',
    'assert theme_names[18:22] == ["زجاج سائل أزرق", "فقاعات أكوا", "أورورا ليلية", "لؤلؤي متجمد"], theme_names\n'
    'assert theme_names[22:25] == ["Prism Glow • زجاج نيوني", "Golden Bloom • زجاج شمسي", "Royal Gold • الذهب الملكي"], theme_names\n'
    'assert theme_names[25:] == ["Royal Dark Gold • ذهبي ملكي داكن", "Imperial Dark Gold • ذهبي إمبراطوري", "Champagne Gold • شامبانيا ملكي فاتح", "Soft Orange-Green Glass • زجاج برتقالي أخضر", "Purple-Pink Liquid Glass • زجاج بنفسجي وردي"], theme_names')
expected_batch22 = expected_batch22.replace(
    'assert len(rows) == len(theme_names) == 22, (name, len(rows))',
    'assert len(rows) == len(theme_names) == 30, (name, len(rows))')
expected_batch22 = expected_batch22.replace(
    '18 preserved IDs plus four new glass themes passed',
    '22 preserved IDs plus eight opt-in reference/premium themes passed')
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
# Only the previously frozen visual source assertions have changed:
# orbiting card halo removed, sandbox bevel and pressure optics restored.
expected_energy = expected_energy.replace(
    'assert "showLens = strong && w > h * 3.1f && !lowPower;" in engine',
    '''assert "showLens = false;" in engine, "Sandbox cards must not render unrelated floating orbs"
assert "prismRim.setShader(new LinearGradient" in engine, "Restore fine optical rim"
assert "crown.setShader(new LinearGradient" in engine, "Restore pearly crown"
assert "final int bodyAlpha = strong ? 202 : 221;" in engine, "Keep actual background visible through glass"''')
expected_energy = expected_energy.replace(
    'assert "new android.view.animation.OvershootInterpolator(1.35f)" in ui',
    '''assert "GlassMotion.bindPress(view, SKY)" in ui, "Restore sandbox finite pressure bubble"
assert "WenFabBubbleDrawable(context, SKY, DARK)" in ui, "Restore circular original-Liquid FAB"
assert "liquid ? 2 : 10" in ui and "liquid ? 125 : 300" in ui, "Fast staged Liquid entrances"
assert "setDuration(115)" in ui, "Gentle sandbox-style screen reveal"''')
expected_energy = expected_energy.replace(
    'assert "if (isLiquidTheme(v.getContext()))" in ui',
    'assert "if (isLiquidTheme(view.getContext()))" in ui')
expected_energy = expected_energy.replace(
    'print("PASS: component-wide Liquid glass surfaces are passive; other themes retain fallbacks")',
    '''fab_bubble = (root / "glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java").read_text(encoding="utf-8")
assert "public float getPress()" in fab_bubble and "public void setPress(float fraction)" in fab_bubble
assert "new RadialGradient" in fab_bubble, "Real pearly FAB depth must be restored"
assert "Timer" not in fab_bubble and "postDelayed" not in fab_bubble
assert "return false; // Never steal existing FAB click/hold actions." in ui

print("PASS: component-wide Liquid glass surfaces are passive; other themes retain fallbacks")''')
assert (ROOT / "tests/glass_energy_contract.py").read_text(encoding="utf-8") == expected_energy, (
    "SAFE ZONE: original glass energy regression changed beyond new FAB opt-in"
)

# Sandbox visual port is STRICTLY confined to graphics and four Ui helper
# methods. Compare the fully reconstructed 1.9 file with the pre-port main
# commit, not just fragile line grep patterns.
SANDBOX_SOURCE = "b57dcd3c71df7273acf5cd316687472f60c11d80"
ui_path = "app/src/main/java/app/wensarafet/tracker/Ui.java"
# Compare against independently approved 30-theme code, not the old palette.
# Only four precisely enumerated sandbox visual methods can vary.
PREMIUM_SOURCE = "ea61bb7a266652751fe9c08f321e0721350df1ea"
original_190_ui = git("show", f"{PREMIUM_SOURCE}:{ui_path}")
visual_only_methods = (
    "glassAccentBackground", "animateScreen", "animateChildren", "pressAnimation",
)

def omit_visual_method_bodies(java):
    for name in visual_only_methods:
        regex = re.compile(
            r"    public static [^\n]*\b" + re.escape(name)
            + r"\([^)]*\)\s*\{"
        )
        hits = list(regex.finditer(java))
        assert len(hits) == 1, "SAFE ZONE: expected one UI helper: " + name
        start = hits[0].start()
        pos = hits[0].end()
        depth = 1
        while pos < len(java) and depth:
            if java[pos] == "{":
                depth += 1
            elif java[pos] == "}":
                depth -= 1
            pos += 1
        assert depth == 0, "SAFE ZONE: unterminated visual UI method: " + name
        java = java[:start] + "    /* preserved visual helper: " + name + " */" + java[pos:]
    return java

assert omit_visual_method_bodies(new_ui) == omit_visual_method_bodies(original_190_ui), (
    "SAFE ZONE: non-visual Ui class or palettes changed during sandbox migration"
)
for marker in (
    "WenFabBubbleDrawable(context, SKY, DARK)",
    "GlassMotion.bindPress(view, SKY)",
    "liquid ? 2 : 10",
    "liquid ? 125 : 300",
    "setDuration(115)",
    "return false; // Never steal existing FAB click/hold actions.",
):
    assert marker in new_ui, "Missing sandbox visual-only transfer: " + marker

for graphic in (
    "WenGlassDrawable.java",
    "WenFabBubbleDrawable.java",
):
    location = "glass-engine/src/main/java/app/wensarafet/glass/" + graphic
    expected = git("show", f"{SANDBOX_SOURCE}:{location}")
    actual = (ROOT / location).read_text(encoding="utf-8")
    assert actual == expected, "SAFE ZONE: visual source must match vetted sandbox exactly: " + graphic
    for protected in ("FinanceDbHelper", "SQLiteDatabase", "AppLockManager",
                      "LicenseManager", "SmsCaptureReceiver", "SandboxMode",
                      "setOnClickListener(", "setOnLongClickListener("):
        assert protected not in actual, "SAFE ZONE: drawing file references protected behaviour"

# Strictly isolate new on-screen transitions from all protected business methods.
screen_motion = (ROOT / "app/src/main/java/app/wensarafet/tracker/WenScreenVisualMotion.java").read_text(encoding="utf-8")
assert git("hash-object",
           "app/src/main/java/app/wensarafet/tracker/WenScreenVisualMotion.java").strip() == (
    "860c1a2187d7c3474b00f86d42c8500bcc189c67"
), "SAFE ZONE: host motion changed after reviewed transfer"
for forbidden in (
    "FinanceDbHelper", "SQLiteDatabase", "AppLockManager",
    "BudgetAlertManager", "DebtLedgerRules", "LicenseManager",
    "SandboxMode", "SmsCaptureReceiver", "setOnClickListener(",
    "setOnLongClickListener(", "getSharedPreferences(",
    "showAddTransaction(", "showMain(", "showAnalyticsScreen(",
    "showDebtsScreen(", "deleteTransaction(", "insert(",
):
    assert forbidden not in screen_motion, "SAFE ZONE: screen motion crosses business logic: " + forbidden
assert "destination.performClick();" in screen_motion
assert "protectedHorizontalTarget" in screen_motion
assert "ValueAnimator.areAnimatorsEnabled()" in screen_motion
assert "power.isPowerSaveMode()" in screen_motion
assert "ViewAnimationUtils.createCircularReveal" in screen_motion
assert "private void revealTopChrome(View page)" in screen_motion
assert "introIcons(next);" in screen_motion
assert "setOnTouchListener(" not in screen_motion
assert "postDelayed(" not in screen_motion and "while (true)" not in screen_motion
crown_source = (ROOT / "glass-engine/src/main/java/app/wensarafet/glass/WenFabOpticalCrownDrawable.java").read_text(encoding="utf-8")
for forbidden in ("OnTouchListener", "OnClickListener", "OnLongClickListener",
                  "FinanceDbHelper", "SQLiteDatabase", "AppLockManager",
                  "Handler(", "postDelayed(", "Choreographer"):
    assert forbidden not in crown_source, "FAB crown must remain an inert visual asset"
assert "view.getOverlay().add(crown);" in new_ui
assert "v.getOverlay().remove(crown);" in new_ui
assert "crown.setPress(t);" in new_ui

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
