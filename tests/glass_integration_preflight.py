#!/usr/bin/env python3
"""Preflight-only guard for future design integration; never merges/re-signs anything."""
from pathlib import Path
import re

root=Path(__file__).resolve().parents[1]
doc=(root/"docs/design/WEN_GLASS_SAFE_INTEGRATION_PLAN.md").read_text(encoding="utf-8")
assert "NO MERGE" in doc
for name in ["SandboxMode.java","app/src/sandbox/","FinanceDbHelper","LicenseManager",
             "AppIntegrity","AppLockManager","BackupCrypto","original private signing identity",
             "exact original tab callbacks"]:
    assert name in doc, f"Integration denial/acceptance missing: {name}"
motion=(root/"glass-engine/src/main/java/app/wensarafet/glass/GlassMotion.java").read_text(encoding="utf-8")
assert "app.wensarafet.tracker" not in motion
for banned in ["FinanceDbHelper", "LicenseManager", "SQLiteOpenHelper", "SmsManager", "INTERNET"]:
    assert banned not in motion
main=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
assert "SandboxMode.canRun(this)" in main, "Trial remains isolated"
assert "boolean fastLiquid = SandboxMode.isSandbox(this) && (Ui.isLiquidTheme(this) || paletteSwitch);" in main
assert "boolean paletteSwitch = pendingPaletteSwitch;" in main
assert "if (!SandboxMode.isSandbox(this) || previousDark != Ui.DARK)" in main, "Non-sandbox/dark fallback kept"
assert "GlassMotion.bindNavigationTab(" in main
for callback in ["showMain(false)", "showMain(true)", "showDebtsScreen(FinanceRules.DEBT_RECEIVABLE)",
                 "showAnalyticsScreen()"]:
    assert callback in main
print("PASS: documentation-only safe integration plan, no production merge")
