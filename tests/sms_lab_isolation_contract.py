#!/usr/bin/env python3
"""No bridge from Arabic SMS Lab into the real finance app (static isolation test)."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
lab=root/"sms-lab"
assert 'include(":app", ":license-generator", ":glass-engine", ":sms-lab")' in (root/"settings.gradle").read_text()
gradle=(lab/"build.gradle").read_text()
manifest=(lab/"src/main/AndroidManifest.xml").read_text()
source={p.name:p.read_text() for p in (lab/"src/main/java/app/wensarafet/smslab").glob("*.java")}
assert 'applicationId "app.wensarafet.smslab"' in gradle
assert 'applicationId "app.wensarafet.tracker"' in (root/"app/build.gradle").read_text()
assert ':app' not in gradle or 'implementation project(":app")' not in gradle
assert "stagePilotSources" in gradle
assert "ArabicSmsParserPilot.java" in gradle
assert "TransactionParser.java" in gradle
assert "FinanceDbHelper.java" not in gradle
for forbidden in ("android.permission.INTERNET","android.permission.READ_SMS",
                  "android.permission.READ_CONTACTS","android:sharedUserId"):
    assert forbidden not in manifest, forbidden
assert "android:allowBackup=\"false\"" in manifest
assert 'android.permission.RECEIVE_SMS' in manifest
assert 'Sms.Intents.SMS_RECEIVED_ACTION' in source["LabSmsReceiver.java"]
assert 'LabStore.smsEnabled(context)' in source["LabSmsReceiver.java"]
assert 'LabStore.notificationsEnabled(this)' in source["LabNotificationListener.java"]
assert 'com.pcnc.bop' in source["LabNotificationListener.java"]
assert 'LabStore.consider' in source["LabSmsReceiver.java"]
assert 'LabStore.consider' in source["LabNotificationListener.java"]
assert 'isVerificationMessage(raw)' in source["LabStore.java"]
assert 'result.verification' in source["LabStore.java"]
for banned in ("FinanceDbHelper","getWritableDatabase","importCapture(",
               "insertCapture(", "addDebtTransaction(", "setAccountBalance(",
               "HttpURLConnection","URL(", "READ_SMS", "startService("):
    for p,s in source.items():
        assert banned not in s,(banned,p)
assert 'android:label="وين صرفت — مختبر العربية"' in manifest
assert "sms_enabled\",false" in source["LabStore.java"]
assert "notifications_enabled\",false" in source["LabStore.java"]
assert 'event.put("fingerprint",hash)' in source["LabStore.java"]
assert 'event.put("raw_text"' not in source["LabStore.java"]
assert 'event.put("sender"' not in source["LabStore.java"]
assert 'MAX_EVENTS=70' in source["LabStore.java"]
assert "دون حفظ" in source["LabActivity.java"]
print("IsolatedSmsLabContract PASS: distinct package, no finance writes, no Internet or SMS history, opt-in, redacted results")
