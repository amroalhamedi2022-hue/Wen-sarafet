#!/usr/bin/env python3
"""Friend-only Arabic capture contracts: preserve real financial core & signed identity."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"app/src/main/java/app/wensarafet/tracker"
read=lambda name:(src/name).read_text(encoding="utf-8")
parser=read("FriendArabicCapturePolicy.java")
pilot=read("ArabicSmsParserPilot.java")
engine=read("CaptureEngine.java")
receiver=read("SmsCaptureReceiver.java")
chooser=read("FriendCaptureProviderSelector.java")
actions=read("CaptureActionReceiver.java")
ui=read("MainActivity.java")
finance=read("FinanceDbHelper.java")
gradle=(root/"app/build.gradle").read_text(encoding="utf-8")
assert 'applicationId "app.wensarafet.tracker"' in gradle
assert "versionCode 25" in gradle and 'versionName "1.8.9"' in gradle
assert "minifyEnabled true" in gradle and "debuggable false" in gradle
assert 'PACKAGE_NAME = "app.wensarafet.tracker"' in read("AppIntegrity.java")
assert "6B8268E5B647B12A2AA055D7A22F7B8EA888E40FB863E74409938AAD4EC789C9" in read("AppIntegrity.java")
assert "FriendArabicCapturePolicy.parse(provider, clean" in engine
assert "FriendArabicCapturePolicy.isSecurityMessage(clean)" in engine
assert '!"SMS_UNVERIFIED".equals(channel)' in engine
assert '!"OTHER".equals(provider)' in engine
assert "parsed.confidence >= 90" in engine
assert "notifyReview(context, id, parsed, \"SMS_UNVERIFIED\".equals(channel))" in engine
assert "if (!internalTransfer && !unknownSource)" in engine
assert 'channel="SMS_UNVERIFIED"' in receiver
assert 'provider="OTHER"' in receiver
assert "FriendArabicCapturePolicy.isFinancialMessage(raw)" in receiver
assert "FriendArabicCapturePolicy.isSecurityMessage(raw)" in receiver
assert "SMS_UNVERIFIED" in ui and "FriendCaptureProviderSelector.choose(db,id,codes[which])" in ui
assert 'SMS_UNVERIFIED' in actions and "db.importCapture(id,type)" in actions
assert "SELECT 1 FROM capture_inbox" in chooser
assert 'values.put("provider",provider)' in chooser
assert 'getWritableDatabase().update("capture_inbox"' in chooser
for no in ['update("accounts"','insert("transactions"','insertOrThrow("transactions"',
           'delete("debt_movements"','update("debts"','rawQuery("UPDATE accounts"']:
    assert no not in chooser,no
assert "candidate.confidence" not in parser
assert "original.confidence=65" in parser
assert "TransactionParser.parse(provider,raw,ownNames)" in parser
assert "final boolean requiresReview = true" in pilot
# These classes are frozen by protected source and hash tests in the core suite.
assert "FriendArabicCapturePolicy" not in finance
assert "FriendArabicCapturePolicy" not in read("TransactionParser.java")
assert "FriendArabicCapturePolicy" not in read("BackupCrypto.java")
print("FriendArabicSafeCaptureContract PASS: same production package, original signer pin, no core finance changes, explicit SMS approval")
