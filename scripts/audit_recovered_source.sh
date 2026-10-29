#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

bash scripts/test_core.sh

python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
for path in Path('app/src/main/res').rglob('*.xml'):
    ET.parse(path)
ET.parse('app/src/main/AndroidManifest.xml')
print('XML audit: all resources parsed')
PY

grep -q 'versionCode 16' app/build.gradle
grep -q 'versionName "1.8.0"' app/build.gradle
grep -q 'android:layout_width="60dp"' app/src/main/res/layout/widget_balances.xml
grep -q 'android:layout_width="54dp"' app/src/main/res/layout/widget_balances_triple.xml
grep -q 'isVerificationMessage' app/src/main/java/app/wensarafet/tracker/CaptureEngine.java
grep -q 'DriveBackupManager' app/src/main/java/app/wensarafet/tracker/MainActivity.java
grep -q 'addManualDebtPayment' app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java
grep -q 'movementKind' app/src/main/java/app/wensarafet/tracker/DebtLedgerRules.java
grep -q 'finishAfterTransition' app/src/main/java/app/wensarafet/tracker/MainActivity.java
grep -q '@color/widget_quick_title' app/src/main/res/layout/widget_quick_action.xml

! grep -R --exclude='audit_recovered_source.sh' -E 'WenSarafetTest2026|MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg' .
! find . -type f \( -name '*.jks' -o -name '*.keystore' \) | grep -q .
# INTERNET is intentionally limited to the optional BOP USD/ILS indicative quote.
# Keep direct dialing and unrestricted package enumeration disallowed.
grep -Fq 'android.permission.INTERNET' app/src/main/AndroidManifest.xml
! grep -Fq 'android.permission.CALL_PHONE' app/src/main/AndroidManifest.xml
! grep -Fq 'android.permission.QUERY_ALL_PACKAGES' app/src/main/AndroidManifest.xml
python3 tests/source_repository_guard.py

echo 'RecoveredSourceAudit: core tests, XML, 1.8 features, BOP INTERNET permission policy, and secret separation passed'
