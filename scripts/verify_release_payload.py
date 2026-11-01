#!/usr/bin/env python3
"""Ensure a signed APK carries EXACTLY the compiled unsigned release payload.

APK signing blocks live outside ZIP entries. V1 signing may add META-INF
certificate files. Those signing-only entries are ignored; everything else
(including classes.dex, resources.arsc, AndroidManifest.xml) must match byte
for byte. This blocks submitting an older signed 1.8.1 as the new 1.8.1.
"""
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path

SIGNATURE_ENTRY = re.compile(
    r"^META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))$", re.I
)
CORE = {"AndroidManifest.xml", "classes.dex", "resources.arsc"}


def digest_file(path):
    hasher = hashlib.sha256()
    with Path(path).open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


def payload(path):
    result = {}
    with zipfile.ZipFile(path, "r") as archive:
        for entry in archive.infolist():
            name = entry.filename
            if name in result or name.endswith("/") or (
                name.startswith("/") or "\\" in name or
                any(part in (".", "..", "") for part in name.split("/"))
            ):
                if name.endswith("/") and name == "META-INF/":
                    continue
                raise ValueError("invalid or duplicate ZIP entry: " + name)
            if SIGNATURE_ENTRY.fullmatch(name):
                continue
            if entry.is_dir():
                continue
            hasher = hashlib.sha256()
            with archive.open(entry) as data:
                for chunk in iter(lambda: data.read(1024 * 1024), b""):
                    hasher.update(chunk)
            result[name] = hasher.hexdigest()
    if not CORE.issubset(result):
        raise ValueError("missing mandatory APK payload files: " +
                         ", ".join(sorted(CORE - set(result))))
    if not any(name.startswith("res/") for name in result):
        raise ValueError("missing compiled Android resources")
    return result


def attest(unsigned, signed):
    original = payload(unsigned)
    candidate = payload(signed)
    lost = sorted(set(original) - set(candidate))
    extra = sorted(set(candidate) - set(original))
    altered = sorted(name for name in original.keys() & candidate.keys()
                     if original[name] != candidate[name])
    if lost or extra or altered:
        raise ValueError(
            "SIGNED APK DOES NOT MATCH THE CURRENT UNSIGNED BUILD: " +
            json.dumps({"removed": lost[:20], "added": extra[:20],
                        "different": altered[:20]}, ensure_ascii=False)
        )
    fingerprint = hashlib.sha256()
    for name in sorted(original):
        fingerprint.update(name.encode("utf-8") + b"\0")
        fingerprint.update(original[name].encode("ascii") + b"\n")
    return {
        "status": "PASS",
        "entry_count": len(original),
        "payload_sha256": fingerprint.hexdigest(),
        "unsigned_apk_sha256": digest_file(unsigned),
        "signed_apk_sha256": digest_file(signed),
    }


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("Usage: python3 scripts/verify_release_payload.py UNSIGNED.apk SIGNED.apk")
    try:
        print(json.dumps(attest(sys.argv[1], sys.argv[2]), ensure_ascii=False, indent=2))
    except (OSError, ValueError, zipfile.BadZipFile) as exc:
        print("BLOCKED: " + str(exc), file=sys.stderr)
        sys.exit(1)
