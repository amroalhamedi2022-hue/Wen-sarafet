#!/usr/bin/env bash
# Local pre-install check only. Never uploads APKs or keystore material.
set -euo pipefail
if [[ $# -ne 2 ]]; then
  echo "Usage: $0 ORIGINAL_1.8.0.apk SIGNED_CANDIDATE_1.8.1.apk" >&2
  exit 2
fi
original="$1"
candidate="$2"
[[ -s "$original" && -s "$candidate" ]] || {
  echo "Both locally-owned signed APK files must exist." >&2
  exit 2
}
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
[[ -n "$sdk" ]] || { echo "Set ANDROID_SDK_ROOT" >&2; exit 2; }
build_tools="$sdk/build-tools/35.0.0"
apksigner="$build_tools/apksigner"
aapt="$build_tools/aapt"
[[ -x "$apksigner" && -x "$aapt" ]] || { echo "Android build-tools 35.0.0 required" >&2; exit 2; }

EXPECTED_CERT="6b8268e5b647b12a2aa055d7a22f7b8ea888e40fb863e74409938aad4ec789c9"
expected_package="app.wensarafet.tracker"

cert() {
  local details
  details="$("$apksigner" verify --verbose --print-certs "$1")" || return 1
  local count
  count="$(grep -Ec '^Signer #[0-9]+ certificate SHA-256 digest:' <<< "$details" || true)"
  [[ "$count" -eq 1 ]] || { echo "Expected exactly one APK signer for $1" >&2; return 1; }
  sed -nE 's/^Signer #1 certificate SHA-256 digest: ([[:xdigit:]]{64}).*/\1/p' <<< "$details" | tr '[:upper:]' '[:lower:]'
}
app_badging() { "$aapt" dump badging "$1" | head -n 1; }
field() {
  local key="$1" badging="$2"
  sed -nE "s/^package: .*\\b${key}='([^']+)'.*/\\1/p" <<< "$badging"
}
installed_cert="$(cert "$original")"
new_cert="$(cert "$candidate")"
if [[ "$installed_cert" != "$EXPECTED_CERT" || "$new_cert" != "$EXPECTED_CERT" ]]; then
  echo "BLOCKED: original/candidate APK signing certificate does not match 1.8.0 trusted certificate" >&2
  exit 1
fi
old_badging="$(app_badging "$original")"
new_badging="$(app_badging "$candidate")"
old_package="$(field name "$old_badging")"
new_package="$(field name "$new_badging")"
old_version="$(field versionCode "$old_badging")"
new_version="$(field versionCode "$new_badging")"
if [[ "$old_package" != "$expected_package" || "$new_package" != "$expected_package" ]]; then
  echo "BLOCKED: applicationId mismatch" >&2
  exit 1
fi
if [[ ! "$old_version" =~ ^[0-9]+$ || ! "$new_version" =~ ^[0-9]+$ ||
      "$old_version" -ne 16 || "$new_version" -le "$old_version" ]]; then
  echo "BLOCKED: expected original code 16 and strictly newer candidate versionCode" >&2
  exit 1
fi
if "$aapt" dump xmltree "$candidate" AndroidManifest.xml |
     grep -Eq 'android:debuggable.*(0xffffffff|0x[1-9a-fA-F])'; then
  echo "BLOCKED: candidate is debuggable" >&2
  exit 1
fi
echo "PASS: SAME trusted signing certificate, package and increasing versionCode ($old_version -> $new_version)."
echo "This is a static APK check only. Test the SQLite migration and activation on a backup/test device before installation."
echo "Do not uninstall the working 1.8.0 app or clear its data."
