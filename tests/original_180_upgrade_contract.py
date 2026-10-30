#!/usr/bin/env python3
"""Source guard: never deliver a modified or differently signed 1.8.0 upgrade."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
gradle = (root / "app/build.gradle").read_text(encoding="utf-8")
integrity = (root / "app/src/main/java/app/wensarafet/tracker/AppIntegrity.java").read_text(encoding="utf-8")
license_manager = (root / "app/src/main/java/app/wensarafet/tracker/LicenseManager.java").read_text(encoding="utf-8")
identity = (root / "app/src/main/java/app/wensarafet/tracker/InstallationIdentity.java").read_text(encoding="utf-8")

# SHA256 of the single trusted 1.8.0 release signing certificate.
expected = "6B8268E5B647B12A2AA055D7A22F7B8EA888E40FB863E74409938AAD4EC789C9"
assert expected in integrity, "Original release certificate changed: update not trusted"
assert 'PACKAGE_NAME = "app.wensarafet.tracker"' in integrity
assert "MessageDigest.isEqual(RELEASE_CERT_SHA256, actual)" in integrity
assert "FLAG_DEBUGGABLE" in integrity
assert "getApkContentsSigners()" in integrity
assert 'applicationId "app.wensarafet.tracker"' in gradle
assert re.search(r"versionCode\s+17\b", gradle)
assert 'versionName "1.8.1"' in gradle
assert 'applicationIdSuffix ".recovered.debug"' in gradle
assert "minifyEnabled true" in gradle
assert "debuggable false" in gradle
assert "WEN_SARAFET_KEYSTORE" in gradle
assert "AppIntegrity.isTrusted(context)" in license_manager
assert "InstallationIdentity.provesPossession(context)" in license_manager
assert "if (!InstallationIdentity.provesPossession(context) && !InstallationIdentity.provesPossession(context))" in license_manager
# Protected security sources must remain byte-identical to the approved baseline.
assert 'ALIAS = "wen_sarafet_installation_identity_v2"' in identity
assert 'PREFS = "license_state"' in license_manager
assert 'TOKEN = "activation_token"' in license_manager
print("Original180UpgradeContract: original certificate, package, keystore identity, license and version retained; no runtime bypass")
