#!/usr/bin/env python3
"""Exercise release/upgrade safety gates with isolated fake SDK tools; no signing secrets."""
import os
import subprocess
import tempfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
builder = root / "scripts/build_apk.sh"
checker = root / "scripts/verify_upgrade_apk.sh"
source = builder.read_text(encoding="utf-8")
assert '--version-code "$APP_VERSION_CODE"' in source
assert '--version-name "$APP_VERSION_NAME"' in source
assert 'Wen-Sarafet-${APP_VERSION_NAME}-UNSIGNED.apk' in source
assert '--version-code 16' not in source
assert 'Wen-Sarafet-1.8.0.apk' not in source
assert 'EXPECTED_CERT_SHA256=' in source
assert '"$SIGNING_KEY_ALIAS"' in source
section = source.split('GRADLE_CONFIG="$PROJECT_DIR/app/build.gradle"', 1)[1].split('rm -rf "$BUILD_DIR"', 1)[0]
command = ('set -euo pipefail\nPROJECT_DIR="$1"\n'
           'GRADLE_CONFIG="$PROJECT_DIR/app/build.gradle"\n' + section
           + '\nprintf "%s|%s" "$APP_VERSION_CODE" "$APP_VERSION_NAME"\n')
parsed = subprocess.run(["bash", "-c", command, "bash", str(root)],
                        capture_output=True, text=True, check=True)
assert parsed.stdout == "17|1.8.1", parsed

cert = "6b8268e5b647b12a2aa055d7a22f7b8ea888e40fb863e74409938aad4ec789c9"
with tempfile.TemporaryDirectory() as directory:
    home = Path(directory)
    sdk_tools = home / "build-tools/35.0.0"
    sdk_tools.mkdir(parents=True)
    signer = sdk_tools / "apksigner"
    signer.write_text("""#!/bin/sh
for last do :; done
case "$last" in
  *wrong-cert*) digest=ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff ;;
  *) digest=6b8268e5b647b12a2aa055d7a22f7b8ea888e40fb863e74409938aad4ec789c9 ;;
esac
echo "Signer #1 certificate SHA-256 digest: $digest"
""")
    aapt = sdk_tools / "aapt"
    aapt.write_text("""#!/bin/sh
case "$2" in
  badging)
    case "$3" in
      *wrong-package*) pkg=unrelated.app ;;
      *) pkg=app.wensarafet.tracker ;;
    esac
    case "$3" in
      *original*|*same-version*) code=16 ;;
      *) code=17 ;;
    esac
    echo "package: name='$pkg' versionCode='$code' versionName='1.8.1'"
    ;;
  xmltree)
    case "$3" in
      *debuggable*) echo "android:debuggable(0x0101000f)=(type 0x12)0xffffffff" ;;
    esac
    ;;
esac
""")
    for executable in (signer, aapt):
        executable.chmod(0o700)
    original = home / "original.apk"
    original.write_bytes(b"fake original")
    env = os.environ.copy()
    env["ANDROID_SDK_ROOT"] = str(home)
    env.pop("ANDROID_HOME", None)

    def run(name):
        apk = home / name
        apk.write_bytes(b"fake candidate")
        return subprocess.run(["bash", str(checker), str(original), str(apk)],
                              env=env, capture_output=True, text=True)

    good = run("candidate.apk")
    assert good.returncode == 0 and "PASS" in good.stdout, (good.stdout, good.stderr)
    for name in ("wrong-cert.apk", "same-version.apk", "wrong-package.apk", "debuggable.apk"):
        rejected = run(name)
        assert rejected.returncode != 0, (name, rejected.stdout, rejected.stderr)

print("UpgradeGateContract: source version, same certificate, increased version, package and debug safety PASS")
