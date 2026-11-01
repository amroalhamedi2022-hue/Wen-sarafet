#!/usr/bin/env python3
"""Owner release must fail closed without original private signing credentials."""
import os
import subprocess
import tempfile
from pathlib import Path

project = Path(__file__).resolve().parents[1]
script = project / "scripts/sign_verified_upgrade.sh"
source = script.read_text(encoding="utf-8")
for required in (
    'WEN_SARAFET_KEYSTORE',
    'WEN_SARAFET_STORE_PASSWORD',
    'WEN_SARAFET_KEY_PASSWORD',
    '-storepass:env WEN_SARAFET_STORE_PASSWORD',
    '--ks-pass env:WEN_SARAFET_STORE_PASSWORD',
    '--key-pass env:WEN_SARAFET_KEY_PASSWORD',
    'sha256sum -c tests/protected_security_sha256.txt',
    'bash scripts/test_core.sh',
    'scripts/verify_release_payload.py',
    'scripts/verify_upgrade_apk.sh',
    '--v2-signing-enabled true',
    '--v3-signing-enabled true',
    'BLOCKED: keystore is not the original signer',
    'BLOCKED: package or version differs from checked source',
):
    assert required in source, required
assert '--ks-pass pass:' not in source
assert 'storepass "$' not in source
assert 'WEN_SARAFET_KEYSTORE:' in source
assert 'case "$keystore" in' in source
assert 'if [[ $# -eq 3 ]]; then' in source

with tempfile.TemporaryDirectory() as folder:
    unsigned = Path(folder) / "unsigned.apk"
    unsigned.write_bytes(b"not-real-apk")
    env = dict(os.environ)
    for key in ("WEN_SARAFET_KEYSTORE", "WEN_SARAFET_STORE_PASSWORD",
                "WEN_SARAFET_KEY_PASSWORD"):
        env.pop(key, None)
    result = subprocess.run(
        ["bash", str(script), str(unsigned)],
        cwd=project, env=env, text=True, capture_output=True,
    )
    assert result.returncode != 0, "No keystore must NEVER produce signed release"
    assert "WEN_SARAFET_KEYSTORE" in result.stderr

print("OwnerSigningContract PASS: local-only private signer, pinned cert, fail-closed release checks")
