"""Protect the latest owner-approved 2.0 source from network capability regressions; no APK build."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
app=root/"app/src/main"
main=(app/"java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
manifest=(app/"AndroidManifest.xml").read_text(encoding="utf-8")
sms=(app/"java/app/wensarafet/tracker/SmsCaptureReceiver.java").read_text(encoding="utf-8")
nt=(app/"java/app/wensarafet/tracker/FinanceNotificationListener.java").read_text(encoding="utf-8")
db=(app/"java/app/wensarafet/tracker/FinanceDbHelper.java").read_text(encoding="utf-8")
gradle=(root/"app/build.gradle").read_text(encoding="utf-8")
assert 'versionCode 29' in gradle and 'versionName "2.1"' in gradle
assert 'applicationId "app.wensarafet.tracker"' in gradle
assert 'android.permission.INTERNET' not in manifest
assert 'android.permission.ACCESS_NETWORK_STATE' not in manifest
assert 'android:allowBackup="false"' in manifest
assert '.BopWidget' in manifest and 'lower.contains("bop")' in sms
assert 'com.pcnc.bop' in nt
assert 'addAccount(db, "بنك فلسطين", "ILS", 0, 1, 0)' in db
assert 'addAccount(db, "بنك فلسطين", "USD", 0, 1, 1)' in db
assert 'R.drawable.icon_bop_mark' in main
assert 'WenScreenVisualMotion visualMotion' in main
assert 'visualMotion.captureOriginalTabs' in main
assert 'DriveBackupManager' not in main
assert 'BopRateFetcher' not in main
assert 'fx_online_rates' not in main
assert 'new Thread(' not in main.split('private void showFxTransferDialog() {',1)[1].split('private void showTransactionTransferDialog',1)[0]
assert '"MANUAL",System.currentTimeMillis()' in main
assert 'Code: 12345' in (root/"tests/FinanceCoreTest.java").read_text()
assert 'isLocalDocumentUri(uri)' in main
for name in ("BopRateFetcher","DriveBackupManager","DriveBackupPublication"):
    assert not (app/f"java/app/wensarafet/tracker/{name}.java").exists()
for path in (app/"res").rglob("*.xml"):
    ET.parse(path)
print("PASS: 2.0 finance, bank, visual preserved; network & cloud sync absent")
