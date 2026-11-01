#!/usr/bin/env python3
"""Sandbox package and SHA-256 security invariants; protects original app."""
from pathlib import Path
import hashlib
import xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[1]
J=R/"app/src/main/java/app/wensarafet/tracker"
G=(R/"app/build.gradle").read_text(encoding="utf-8")
M=(J/"MainActivity.java").read_text(encoding="utf-8")
S=(J/"SandboxMode.java").read_text(encoding="utf-8")
overlay=(R/"app/src/sandbox/AndroidManifest.xml").read_text(encoding="utf-8")
ET.fromstring(overlay)
assert 'applicationId "app.wensarafet.tracker"' in G
assert 'applicationIdSuffix ".recovered.debug"' in G
assert 'applicationIdSuffix ".sandbox"' in G
assert 'initWith debug' in G and 'debuggable true' in G
assert 'versionNameSuffix "-sandbox"' in G
assert 'ID.equals(c.getPackageName())' in S
assert 'ApplicationInfo.FLAG_DEBUGGABLE' in S
assert 'return isSandbox(c) || LicenseManager.isActivated(c)' in S
assert 'SandboxMode.isSandbox(this) || AppIntegrity.isTrusted(this)' in M
assert M.count('SandboxMode.canRun(this)') >= 4
assert 'if (SandboxMode.isSandbox(this))' in M
assert '🧪 نسخة اختبار مستقلة' in M
assert 'android.view.WindowInsets.Type.systemBars()' in M
assert 'screenHost.setOnApplyWindowInsetsListener' in M
assert 'android:allowBackup="false"' in overlay
assert '${applicationId}.notification-sounds' in overlay
P=(J/"NotificationSoundProvider.java").read_text(encoding="utf-8")
N=(J/"NotificationPreferences.java").read_text(encoding="utf-8")
assert 'isOurSound(getContext(),uri)' in P
assert 'uri(context,type)' in N
for name in ["LicenseManager", "AppIntegrity", "AppLockManager", "BackupCrypto",
             "SecurePinStore", "LicenseProtocol", "InstallationIdentity"]:
    file=J/(name+".java")
    actual=hashlib.sha256(file.read_bytes()).hexdigest()
    matching=[x.split()[0] for x in (R/"tests/protected_security_sha256.txt").read_text().splitlines()
              if x.endswith(name+".java")]
    assert matching and matching[0]==actual, name+" security source touched"
for name in ["QuickActionActivity", "FinanceNotificationListener",
             "SmsCaptureReceiver", "ReminderReceiver"]:
    assert 'SandboxMode.canRun(' in (J/(name+".java")).read_text(),name
assert 'new FinanceDbHelper(this)' in M
assert 'BackupCrypto' in (J/"BackupFileManager.java").read_text()
print("PASS: independent full-app sandbox with unchanged financial core and production security files")
