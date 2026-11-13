#!/usr/bin/env python3
"""Scoped release guard for the owner's three approved v2.2.6 fixes."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BASE = "6eb1ebaa001678747e776dea3ed72cb809354abb"


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True)


def present(path):
    return (ROOT / path).read_text(encoding="utf-8")


def at(path):
    return git("show", f"{BASE}:{path}")


allowed = {
    ".github/workflows/unsigned-release-candidate.yml",
    "app/build.gradle",
    "app/src/main/java/app/wensarafet/tracker/BalanceReclassificationRules.java",
    "app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java",
    "app/src/main/java/app/wensarafet/tracker/MainActivity.java",
    "app/src/main/java/app/wensarafet/tracker/QuickActionWidgetProvider.java",
    "app/src/main/res/layout/widget_quick_action.xml",
    "scripts/test_core.sh",
    "tests/BalanceReclassificationRulesTest.java",
    "tests/debt_cashflow_integrity_contract.py",
    "tests/financial_safe_zone_git_blobs.txt",
    "tests/original_180_upgrade_contract.py",
    "tests/transaction_detail_scroll_contract.py",
    "tests/upgrade_gate_contract.py",
    "tests/wen_200_strict_offline_contract.py",
    "tests/wen_226_three_fix_contract.py",
    "tests/widget_contact_picker_contract.py",
}
changed = set(git("diff", "--name-only", BASE).splitlines())
assert changed <= allowed, "Unexpected file changed: " + ", ".join(sorted(changed - allowed))

for path in (
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/app/wensarafet/tracker/FinanceRules.java",
    "app/src/main/java/app/wensarafet/tracker/DebtLedgerRules.java",
    "app/src/main/java/app/wensarafet/tracker/NonNegativeBalanceRules.java",
    "app/src/main/java/app/wensarafet/tracker/AppIntegrity.java",
    "app/src/main/java/app/wensarafet/tracker/LicenseManager.java",
    "app/src/main/java/app/wensarafet/tracker/AppLockManager.java",
    "app/src/main/java/app/wensarafet/tracker/SecurePinStore.java",
    "app/src/main/java/app/wensarafet/tracker/SmsCaptureReceiver.java",
    "app/src/main/java/app/wensarafet/tracker/NavigationLiquidMaterial.java",
    "app/src/main/java/app/wensarafet/tracker/NavigationVisualDecorator.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenNavigationLensDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenNavigationTrack.java",
):
    assert present(path) == at(path), "Protected source changed: " + path

gradle = at("app/build.gradle").replace(
    '        versionCode 31\n        versionName "2.2.5"',
    '        versionCode 32\n        versionName "2.2.6"',
)
assert present("app/build.gradle") == gradle, "Gradle changed beyond the release version"

finance = present("app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java")
reclassify = finance.split("public void reclassifyTransactionAsDebt(", 1)[1].split(
    "public void updateDebtTransaction(", 1)[0]
assert "BalanceReclassificationRules.netDelta(type, oldImpact, newImpact)" in reclassify
assert "if (netBalanceDelta != 0L) adjustBalance" in reclassify
assert "reverseBalanceEffect(" not in reclassify
assert "applyBalanceEffect(" not in reclassify

main = present("app/src/main/java/app/wensarafet/tracker/MainActivity.java")
details = main.split("private void showTransactionDetails(", 1)[1].split(
    "private void showTransactionEditMenu(", 1)[0]
editor = main.split("private void showEditDebtTransactionDialog(", 1)[1].split(
    "private void showCategoryPicker(", 1)[0]
assert ".setView(scroll)" in details and "scroll.addView(content" in details
assert ".setView(scroll)" in editor and "scroll.addView(form)" in editor

layout = present("app/src/main/res/layout/widget_quick_action.xml")
provider = present("app/src/main/java/app/wensarafet/tracker/QuickActionWidgetProvider.java")
picker = present("app/src/main/java/app/wensarafet/tracker/QuickActionActivity.java")
assert "widget_choose_contact" not in layout
assert "widget_choose_contact" not in provider
assert "Intent.ACTION_PICK" in picker

manifest = present("app/src/main/AndroidManifest.xml")
assert "android.permission.INTERNET" not in manifest
print("PASS: v2.2.6 contains only the three approved fixes and preserves protected behavior")
