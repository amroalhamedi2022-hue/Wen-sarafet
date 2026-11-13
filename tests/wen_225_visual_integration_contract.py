#!/usr/bin/env python3
"""Production visual merge guard for v2.2.5.

The accepted sandbox work may change presentation only. Finance, persistence,
security, offline policy, navigation-lens code and application identity remain
byte-for-byte equal to the verified v2.1 production base.
"""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BASE = "1d724a9ec6f8d2eaef4043f925ee07407d5c32d6"


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True)


def present(path):
    return (ROOT / path).read_text(encoding="utf-8")


def at(path):
    return git("show", f"{BASE}:{path}")


allowed = {
    "app/build.gradle",
    ".github/workflows/unsigned-release-candidate.yml",
    "app/src/main/java/app/wensarafet/tracker/MainActivity.java",
    "app/src/main/java/app/wensarafet/tracker/QuickActionWidgetProvider.java",
    "app/src/main/java/app/wensarafet/tracker/WenGoldGlassSurface.java",
    "app/src/main/java/app/wensarafet/tracker/WenScreenVisualMotion.java",
    "app/src/main/res/drawable/widget_crystal_balance_three.xml",
    "app/src/main/res/drawable/widget_crystal_balance_two.xml",
    "app/src/main/res/drawable/widget_crystal_quick_bop.xml",
    "app/src/main/res/drawable/widget_crystal_quick_jawwal.xml",
    "app/src/main/res/drawable/widget_crystal_quick_palpay.xml",
    "app/src/main/res/layout/widget_balances.xml",
    "app/src/main/res/layout/widget_balances_triple.xml",
    "app/src/main/res/layout/widget_quick_action.xml",
    "glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenPremiumLiquidDrawable.java",
    "scripts/test_core.sh",
    "scripts/build_apk.sh",
    "scripts/audit_recovered_source.sh",
    "scripts/sign_verified_upgrade.sh",
    "scripts/verify_upgrade_apk.sh",
    "tests/original_180_upgrade_contract.py",
    "tests/glass_energy_contract.py",
    "tests/upgrade_gate_contract.py",
    "tests/wen_200_strict_offline_contract.py",
    "tests/wen_225_visual_integration_contract.py",
}
changed = set(git("diff", "--name-only", BASE).splitlines())
assert changed <= allowed, "Non-visual file changed: " + ", ".join(sorted(changed - allowed))

protected = (
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java",
    "app/src/main/java/app/wensarafet/tracker/FinanceRules.java",
    "app/src/main/java/app/wensarafet/tracker/DebtLedgerRules.java",
    "app/src/main/java/app/wensarafet/tracker/AppIntegrity.java",
    "app/src/main/java/app/wensarafet/tracker/LicenseManager.java",
    "app/src/main/java/app/wensarafet/tracker/AppLockManager.java",
    "app/src/main/java/app/wensarafet/tracker/SecurePinStore.java",
    "app/src/main/java/app/wensarafet/tracker/SmsCaptureReceiver.java",
    "app/src/main/java/app/wensarafet/tracker/NavigationLiquidMaterial.java",
    "app/src/main/java/app/wensarafet/tracker/NavigationVisualDecorator.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenNavigationLensDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenNavigationTrack.java",
)
for path in protected:
    assert present(path) == at(path), "Protected production source changed: " + path

gradle = at("app/build.gradle").replace(
    '        versionCode 29\n        versionName "2.1"',
    '        versionCode 31\n        versionName "2.2.5"',
)
assert present("app/build.gradle") == gradle, "Gradle changed beyond release version increment"

main = present("app/src/main/java/app/wensarafet/tracker/MainActivity.java")
motion = present("app/src/main/java/app/wensarafet/tracker/WenScreenVisualMotion.java")
ui = present("app/src/main/java/app/wensarafet/tracker/Ui.java")
assert main.count("showAddTransaction(); // exact same original action, still immediate") == 1
assert "visualMotion.markFabTap(v, screenHost);" in main
assert "if (Ui.isLiquidTheme(this)) visualMotion.markFabTap" not in main
assert "int fabGravity = Ui.homeFabOnRight(this) ? Gravity.RIGHT : Gravity.LEFT;" in main
assert 'getBoolean("home_fab_right", false)' in ui
assert "public boolean hasFabTransition()" in motion
assert "!Ui.isLiquidTheme(context) && !hasFabTransition()" in motion
assert "WenFabBackdropBlur" not in main
assert not (ROOT / "app/src/main/java/app/wensarafet/tracker/WenFabBackdropBlur.java").exists()

gold = present("app/src/main/java/app/wensarafet/tracker/WenGoldGlassSurface.java")
premium = present("glass-engine/src/main/java/app/wensarafet/glass/WenPremiumLiquidDrawable.java")
assert "new LayerDrawable" in gold
assert "550_000" in premium or "550000" in premium
for theme in ("ROYAL_DARK", "IMPERIAL_DARK", "CHAMPAGNE"):
    assert theme in premium

for layout in ("widget_quick_action.xml", "widget_balances.xml", "widget_balances_triple.xml"):
    source = present("app/src/main/res/layout/" + layout)
    assert "@drawable/widget_crystal_" in source

manifest = present("app/src/main/AndroidManifest.xml")
assert "android.permission.INTERNET" not in manifest
print("PASS: v2.2.5 visual merge preserves production behavior, offline policy and identity")
