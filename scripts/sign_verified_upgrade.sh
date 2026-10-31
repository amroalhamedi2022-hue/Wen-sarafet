#!/usr/bin/env bash
# LOCAL OWNER ONLY. Never place private signing keys in public CI or this repository.
set -euo pipefail
umask 077

if [[ $# -lt 1 || $# -gt 3 ]]; then
  echo "Usage: $0 CI_UNSIGNED_ALIGNED.apk [OUTPUT_SIGNED.apk] [ORIGINAL_SIGNED_1.8.0.apk]" >&2
  exit 2
fi
project="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
unsigned="$(realpath "$1")"
gradle="$project/app/build.gradle"
name="$(sed -nE 's/^[[:space:]]*versionName[[:space:]]+"([^"]+)".*$/\1/p' "$gradle" | head -1)"
code="$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]+([0-9]+).*$/\1/p' "$gradle" | head -1)"
[[ "$name" =~ ^1\.8\.[0-9]+$ && "$code" =~ ^[0-9]+$ && "$code" -gt 16 ]] || {
  echo "BLOCKED: unexpected source version" >&2; exit 1;
}
output="${2:-$project/build/owner-release/Wen-Sarafet-${name}-SIGNED.apk}"
[[ -s "$unsigned" && ! -e "$output" ]] || {
  echo "BLOCKED: unsigned APK missing or output already exists; never overwrite" >&2; exit 1;
}
: "${WEN_SARAFET_KEYSTORE:?Set ORIGINAL keystore path locally}"
: "${WEN_SARAFET_STORE_PASSWORD:?Set original keystore password locally}"
WEN_SARAFET_KEY_ALIAS="${WEN_SARAFET_KEY_ALIAS:-wen-sarafet-test}"
WEN_SARAFET_KEY_PASSWORD="${WEN_SARAFET_KEY_PASSWORD:-$WEN_SARAFET_STORE_PASSWORD}"
export WEN_SARAFET_STORE_PASSWORD WEN_SARAFET_KEY_PASSWORD
keystore="$(realpath "$WEN_SARAFET_KEYSTORE")"
[[ -s "$keystore" ]] || { echo "BLOCKED: keystore missing" >&2; exit 1; }
case "$keystore" in
  "$project"/*) echo "BLOCKED: private keystore must stay OUTSIDE repository" >&2; exit 1 ;;
esac
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
[[ -n "$sdk" ]] || { echo "Set Android SDK path" >&2; exit 2; }
signer="$sdk/build-tools/35.0.0/apksigner"
aapt="$sdk/build-tools/35.0.0/aapt"
[[ -x "$signer" && -x "$aapt" ]] || { echo "Android build-tools 35.0.0 required" >&2; exit 1; }
expected="6b8268e5b647b12a2aa055d7a22f7b8ea888e40fb863e74409938aad4ec789c9"
keyinfo="$(keytool -list -v -keystore "$keystore" \
  -storepass:env WEN_SARAFET_STORE_PASSWORD -alias "$WEN_SARAFET_KEY_ALIAS")" || {
  echo "BLOCKED: original private key cannot be opened" >&2; exit 1;
}
cert="$(sed -nE 's/^[[:space:]]*SHA256:[[:space:]]*([0-9A-Fa-f:]+).*$/\1/p' <<< "$keyinfo" | head -1 | tr -d ':' | tr '[:upper:]' '[:lower:]')"
[[ "$cert" == "$expected" ]] || { echo "BLOCKED: keystore is not the original signer" >&2; exit 1; }
(cd "$project" && sha256sum -c tests/protected_security_sha256.txt >/dev/null)
(cd "$project" && bash scripts/test_core.sh >/dev/null)
if "$signer" verify "$unsigned" >/dev/null 2>&1; then
  echo "BLOCKED: input must be unsigned, NOT an old signed APK" >&2; exit 1;
fi
badging="$("$aapt" dump badging "$unsigned" | sed -n '1p')"
[[ "$badging" == *"name='app.wensarafet.tracker'"* &&
   "$badging" == *"versionCode='$code'"* &&
   "$badging" == *"versionName='$name'"* ]] || {
  echo "BLOCKED: package or version differs from checked source" >&2; exit 1;
}
mkdir -p "$(dirname "$output")"
signed_ok=0
trap 'if [[ "$signed_ok" != 1 ]]; then rm -f "$output"; fi' EXIT
"$signer" sign --ks "$keystore" --ks-key-alias "$WEN_SARAFET_KEY_ALIAS" \
  --ks-pass env:WEN_SARAFET_STORE_PASSWORD \
  --key-pass env:WEN_SARAFET_KEY_PASSWORD \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  --out "$output" "$unsigned"
details="$("$signer" verify --verbose --print-certs "$output")" || {
  echo "BLOCKED: APK signature verification failed" >&2; exit 1;
}
actual="$(sed -nE 's/^Signer #1 certificate SHA-256 digest: ([0-9a-fA-F]+).*$/\1/p' <<< "$details" | tr '[:upper:]' '[:lower:]')"
count="$(grep -Ec '^Signer #[0-9]+ certificate SHA-256 digest:' <<< "$details" || true)"
[[ "$count" -eq 1 && "$actual" == "$expected" ]] || {
  echo "BLOCKED: signer mismatch" >&2; exit 1;
}
grep -Eq '^Verified using v(2|3) scheme .*true$' <<< "$details" || {
  echo "BLOCKED: APK v2/v3 signature not verified" >&2; exit 1;
}
python3 "$project/scripts/verify_release_payload.py" "$unsigned" "$output"
if [[ $# -eq 3 ]]; then
  bash "$project/scripts/verify_upgrade_apk.sh" "$3" "$output"
fi
signed_ok=1
echo "VERIFIED SIGNED RELEASE: $output"
echo "Device acceptance still required: in-place upgrade, data, activation, PIN, widgets and backup."
