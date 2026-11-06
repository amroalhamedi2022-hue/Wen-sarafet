"""Prevent regression: Bank of Palestine remains local-only; no cloud networking."""
from pathlib import Path
import xml.etree.ElementTree as ET
r=Path(__file__).resolve().parents[1]
app=r/"app/src/main"
manifest=(app/"AndroidManifest.xml").read_text()
main=(app/"java/app/wensarafet/tracker/MainActivity.java").read_text()
db=(app/"java/app/wensarafet/tracker/FinanceDbHelper.java").read_text()
sms=(app/"java/app/wensarafet/tracker/SmsCaptureReceiver.java").read_text()
notification=(app/"java/app/wensarafet/tracker/FinanceNotificationListener.java").read_text()
fx=main.split('private void showFxTransferDialog() {',1)[1].split('private void showTransactionTransferDialog',1)[0]
assert 'android.permission.INTERNET' not in manifest
assert 'android.permission.ACCESS_NETWORK_STATE' not in manifest
assert 'android:allowBackup="false"' in manifest
assert 'android.permission.RECEIVE_SMS' in manifest
assert '.BopWidget' in manifest
assert '<receiver android:name=".SmsCaptureReceiver"' in manifest
assert 'com.pcnc.bop' in notification
assert 'lower.contains("bop")' in sms
assert '"BOP".equals(provider)' in db
assert 'addAccount(db, "بنك فلسطين", "ILS", 0, 1, 0)' in db
assert 'addAccount(db, "بنك فلسطين", "USD", 0, 1, 1)' in db
assert 'BopRateFetcher' not in main
assert 'BopRateFetcher' not in fx
assert 'DriveBackupManager' not in main
assert 'new Thread(' not in fx
assert '"MANUAL",System.currentTimeMillis()' in fx
assert 'isVerificationMessage(clean)' in (app/"java/app/wensarafet/tracker/CaptureEngine.java").read_text()
assert 'Code: 12345' in (r/"tests/FinanceCoreTest.java").read_text()
assert (app/"drawable-nodpi/icon_bop_mark.png").exists()
assert (app/"drawable-nodpi/icon_bop_widget_soft.png").exists()
assert (app/"xml/widget_bop.xml").exists()
assert 'widget_bop_balance' in (app/"layout/widget_balances.xml").read_text()
for path in app.rglob("*.xml"):
    ET.parse(path)
print("BankOfflineContract PASS: local banking functionality restored, network prohibited, OTP filtered")
