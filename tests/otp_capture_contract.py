#!/usr/bin/env python3
"""Strict privacy check: no wallet OTP reaches the capture inbox/SQLite.

Java execution cases are in FinanceCoreTest.java. This source-level check
protects the Android-specific SMS/notification routing which cannot be
executed with source-only Java tests.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/app/wensarafet/tracker"
parser = (SRC / "TransactionParser.java").read_text(encoding="utf-8")
capture = (SRC / "CaptureEngine.java").read_text(encoding="utf-8")
sms = (SRC / "SmsCaptureReceiver.java").read_text(encoding="utf-8")
notification = (SRC / "FinanceNotificationListener.java").read_text(encoding="utf-8")
tests = (ROOT / "tests/FinanceCoreTest.java").read_text(encoding="utf-8")
assert 'CODE_PREFIX.matcher(normalized).find()' in parser, "Code: prefix not filtered"
assert 'Pattern.compile("(?i)^code\\\\s*[:：]")' in parser, "Code prefix expression changed"
for fixture in ('"Code:"', '"Code: short"', '"cOdE : A1234567890123"',
                '"CODE： ABC123"'):
    assert fixture in tests, "OTP regression example missing: " + fixture

# Both SMS and provider notifications flow through the central capture
# engine; its first no-code gate MUST run before database allocation.
gate = 'if (clean.isEmpty() || TransactionParser.isVerificationMessage(clean)) return;'
db = 'FinanceDbHelper db = new FinanceDbHelper(context);'
assert gate in capture and capture.index(gate) < capture.index(db), (
    "OTP must be rejected before SQLite is opened"
)
assert "CaptureEngine.handle(context, provider, \"SMS\"" in sms
assert 'CaptureEngine.handle(this, provider, "NOTIFICATION"' in notification

# Android notification construction prefixes the provider title to the body,
# which can disguise an original 'Code:' prefix. Read the original body first.
body = 'String body = !big.isEmpty() ? big : text;'
preflight = 'if (TransactionParser.isVerificationMessage(body)'
title = 'body = title + " — " + body;'
assert body in notification and preflight in notification and title in notification
assert notification.index(body) < notification.index(preflight) < notification.index(title), (
    "Unfiltered Code: notifications can be persisted"
)
assert '|| TransactionParser.isVerificationMessage(title)) return;' in notification

print("PASS: Code: SMS and notifications rejected before capture storage; normal receipts unchanged")
