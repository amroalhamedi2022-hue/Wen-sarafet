#!/usr/bin/env python3
"""Source-only GitHub Actions safety gates; no Android SDK or signing secrets needed."""
from __future__ import annotations

import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"


def verify() -> None:
    failures = []
    xml_files = sorted((ROOT / "app/src/main/res").rglob("*.xml"))
    xml_files.append(ROOT / "app/src/main/AndroidManifest.xml")
    if len(xml_files) < 2:
        failures.append("Android resource XML files are missing")
    for path in xml_files:
        try:
            ET.parse(path)
        except (ET.ParseError, OSError) as exc:
            failures.append(f"Invalid XML: {path.relative_to(ROOT)}: {exc}")

    manifest = ROOT / "app/src/main/AndroidManifest.xml"
    if manifest.is_file():
        try:
            tree = ET.parse(manifest).getroot()
            if tree.attrib.get("package") != "app.wensarafet.tracker":
                failures.append("The application package changed unexpectedly")
            permissions = {
                node.attrib.get(ANDROID + "name", "")
                for node in tree.findall("uses-permission")
            }
            forbidden = {
                "android.permission.CALL_PHONE",
                "android.permission.QUERY_ALL_PACKAGES",
                "android.permission.READ_SMS",
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
            }
            if permissions & forbidden:
                failures.append(f"Unexpected privileged permissions: {sorted(permissions & forbidden)}")
            # SmsCaptureReceiver is optional on Wi-Fi-only tablets (e.g. Xiaomi Pad).
            telephony = [
                feature for feature in tree.findall("uses-feature")
                if feature.attrib.get(ANDROID + "name") == "android.hardware.telephony"
            ]
            if not telephony or not any(
                feature.attrib.get(ANDROID + "required") == "false" for feature in telephony
            ):
                failures.append("Telephony must be explicitly optional for tablets")
            app = tree.find("application")
            if app is None:
                failures.append("Missing Android application element")
            else:
                if app.attrib.get(ANDROID + "debuggable") == "true":
                    failures.append("Release manifest must not force debuggable=true")
                if app.attrib.get(ANDROID + "usesCleartextTraffic") == "true":
                    failures.append("Cleartext HTTP must remain disabled")
                for service in app.findall("service"):
                    if service.attrib.get(ANDROID + "permission") == "android.permission.BIND_ACCESSIBILITY_SERVICE":
                        failures.append("An accessibility service was added")
            print(f"Declared manifest permissions: {', '.join(sorted(permissions))}")
            # INTERNET is intentional for the opt-in, best-effort BOP indicative rate feature.
        except (ET.ParseError, OSError) as exc:
            failures.append(f"Manifest could not be checked: {exc}")

    gradle = ROOT / "app/build.gradle"
    if not gradle.is_file() or not re.search(
        r'applicationId\s+["\']app\.wensarafet\.tracker["\']',
        gradle.read_text(encoding="utf-8") if gradle.is_file() else "",
    ):
        failures.append("Android applicationId is missing or changed")
    if gradle.is_file() and not re.search(
        r'buildTypes\s*\{[\s\S]*?release\s*\{[\s\S]*?\bdebuggable\s+false\b',
        gradle.read_text(encoding="utf-8"),
    ):
        failures.append("Release build must explicitly set debuggable false")

    helper = ROOT / "app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java"
    helper_text = helper.read_text(encoding="utf-8") if helper.is_file() else ""
    db_match = re.search(r'\bDB_VERSION\s*=\s*(\d+)\s*;', helper_text)
    if not db_match or int(db_match.group(1)) < 12:
        failures.append("Database version is unexpectedly below the v12 checkpoint")

    try:
        tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode().split("\0")
        blocked = []
        for rel in filter(None, tracked):
            lower = rel.lower()
            parts = lower.split("/")
            if any(s in parts for s in ("private_owner_only", "owner-secrets")) or lower.endswith(
                (".jks", ".keystore", ".p12", ".pfx", ".pem", ".pk8")
            ):
                blocked.append(rel)
        if blocked:
            failures.append("Sensitive-looking material tracked in public Git: " + ", ".join(blocked))
    except (OSError, subprocess.CalledProcessError) as exc:
        failures.append(f"Cannot audit Git-tracked paths: {exc}")

    if failures:
        for issue in failures:
            print("FAIL:", issue, file=sys.stderr)
        raise SystemExit(1)
    print(f"PASS: {len(xml_files)} XML files, manifest safety, package, DB schema floor, tracked-path guard")
    print("Note: this is a source guard, not a secret scanner, Android build, or device test.")


if __name__ == "__main__":
    verify()
