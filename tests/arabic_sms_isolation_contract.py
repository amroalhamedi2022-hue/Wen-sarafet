#!/usr/bin/env python3
"""Experimental Arabic parser is isolated from all live capture/write paths."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
base = root/"app/src/main/java/app/wensarafet/tracker"
pilot = (base/"ArabicSmsParserPilot.java").read_text(encoding="utf-8")
for component in ("CaptureEngine.java","SmsCaptureReceiver.java",
                  "FinanceNotificationListener.java","MainActivity.java",
                  "FinanceDbHelper.java","TransactionParser.java"):
    content=(base/component).read_text(encoding="utf-8")
    assert "ArabicSmsParserPilot" not in content, (
        "pilot leaked into production capture path", component)
for forbidden in ("insertCapture(", "importCapture(", "addTransaction(", "addDebtTransaction(",
                  "SQLiteDatabase", "ContentValues", "SharedPreferences",
                  "ACTION_RESOLVE", "BroadcastReceiver", "NotificationManager"):
    assert forbidden not in pilot, ("pilot has side effects", forbidden)
assert "final boolean requiresReview = true" in pilot
assert "TransactionParser.isVerificationMessage(raw)" in pilot
assert "Money.parseMinor(" in pilot
print("ArabicSmsIsolationContract PASS: read-only candidate, no production integration or financial writes")
