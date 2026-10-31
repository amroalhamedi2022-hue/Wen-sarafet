#!/usr/bin/env python3
"""Test strict signed/unsigned payload provenance without any production key."""
import importlib.util
import tempfile
import warnings
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location(
    "verify_release_payload", root / "scripts/verify_release_payload.py"
)
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)

core = {
    "AndroidManifest.xml": b"Android Binary Manifest",
    "classes.dex": b"dex\ncurrent release classes",
    "resources.arsc": b"resource table",
    "res/drawable/logo.png": b"current icon",
}


def write(path, entries, duplicate=False):
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as dest:
        for name, contents in entries.items():
            dest.writestr(name, contents)
        if duplicate:
            with warnings.catch_warnings():
                warnings.simplefilter("ignore", UserWarning)
                dest.writestr("classes.dex", b"evil duplicate dex")


def rejected(a, b, reason):
    try:
        gate.attest(a, b)
    except ValueError:
        return
    raise AssertionError("Accepted mismatched " + reason)


with tempfile.TemporaryDirectory() as directory:
    tmp = Path(directory)
    unsigned = tmp / "unsigned.apk"
    signed = tmp / "signed.apk"
    write(unsigned, core)
    signed_entries = dict(core)
    signed_entries.update({
        "META-INF/MANIFEST.MF": b"signature manifest",
        "META-INF/BUILD.SF": b"v1 signature",
        "META-INF/BUILD.RSA": b"v1 cert",
    })
    write(signed, signed_entries)
    evidence = gate.attest(unsigned, signed)
    assert evidence["status"] == "PASS"
    assert evidence["entry_count"] == len(core)

    changed = dict(signed_entries)
    changed["classes.dex"] = b"dex\nstale prior signed build"
    write(signed, changed)
    rejected(unsigned, signed, "old compiled classes")

    changed = dict(signed_entries)
    changed["res/drawable/logo.png"] = b"old icon"
    write(signed, changed)
    rejected(unsigned, signed, "old resources")

    changed = dict(signed_entries)
    changed.pop("resources.arsc")
    write(signed, changed)
    rejected(unsigned, signed, "deleted resources")

    changed = dict(signed_entries)
    changed["assets/unexpected.dex"] = b"code injected after building"
    write(signed, changed)
    rejected(unsigned, signed, "extra payload entries")

    write(signed, signed_entries, duplicate=True)
    rejected(unsigned, signed, "duplicate ZIP entry")

    changed = dict(signed_entries)
    changed["../unsafe"] = b"path traversal"
    write(signed, changed)
    rejected(unsigned, signed, "path traversal")

    changed = dict(signed_entries)
    changed["META-INF/MALICIOUS.BIN"] = b"unsigned extra file"
    write(signed, changed)
    rejected(unsigned, signed, "unrecognized META-INF files")

print("ReleasePayloadContract PASS: same payload, altered code/assets, deleted/extra/duplicate/traversal paths")
