#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
[[ -n "$ANDROID_SDK_ROOT" ]] || { echo "Set ANDROID_SDK_ROOT or ANDROID_HOME" >&2; exit 1; }
BUILD_TOOLS="$ANDROID_SDK_ROOT/build-tools/35.0.0"
MAIN_APK="${1:-$PROJECT_DIR/build/apk/Wen-Sarafet-1.8.0.apk}"
GENERATOR_APK="$PROJECT_DIR/build/license-generator/Wen-Sarafet-License-Generator-1.0.0-PRIVATE.apk"
PRIVATE_PREFIX="${LICENSE_PRIVATE_KEY_PREFIX:-OWNER_PRIVATE_KEY_PREFIX_NOT_SET}"
EXPECTED_CERT="6b8268e5b647b12a2aa055d7a22f7b8ea888e40fb863e74409938aad4ec789c9"

[[ -f "$MAIN_APK" && -f "$GENERATOR_APK" ]]
"$BUILD_TOOLS/apksigner" verify "$MAIN_APK"
"$BUILD_TOOLS/apksigner" verify "$GENERATOR_APK"

main_badging="$($BUILD_TOOLS/aapt2 dump badging "$MAIN_APK")"
generator_badging="$($BUILD_TOOLS/aapt2 dump badging "$GENERATOR_APK")"
# Compare against the source metadata, not an obsolete hardcoded release number.
source_code="$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]+([0-9]+).*$/\1/p' "$PROJECT_DIR/app/build.gradle" | head -1)"
source_name="$(sed -nE 's/^[[:space:]]*versionName[[:space:]]+"([^"]+)".*$/\1/p' "$PROJECT_DIR/app/build.gradle" | head -1)"
[[ "$source_code" =~ ^[0-9]+$ && -n "$source_name" ]]
grep -Fq "package: name='app.wensarafet.tracker' versionCode='$source_code' versionName='$source_name'" <<<"$main_badging"
grep -q "package: name='app.wensarafet.admin'" <<<"$generator_badging"

main_permissions="$(grep '^uses-permission:' <<<"$main_badging" || true)"
# INTERNET is approved only for the optional BOP indicative USD/ILS quote.
grep -Fq "android.permission.INTERNET" <<<"$main_permissions"
! grep -Eq 'CALL_PHONE|BIND_ACCESSIBILITY_SERVICE|QUERY_ALL_PACKAGES' <<<"$main_permissions"
! grep -q '^uses-permission:' <<<"$generator_badging"

main_manifest="$($BUILD_TOOLS/aapt2 dump xmltree --file AndroidManifest.xml "$MAIN_APK")"
generator_manifest="$($BUILD_TOOLS/aapt2 dump xmltree --file AndroidManifest.xml "$GENERATOR_APK")"
grep -q 'allowBackup.*=true' <<<"$main_manifest"
# Android's omitted release debuggable attribute also means false. Never allow true.
grep -q 'debuggable false' "$PROJECT_DIR/app/build.gradle"
! grep -Eq 'debuggable.*=true' <<<"$main_manifest"
grep -q 'usesCleartextTraffic.*=false' <<<"$main_manifest"
grep -q 'allowBackup.*=false' <<<"$generator_manifest"
grep -q 'debuggable.*=false' <<<"$generator_manifest"
grep -q 'usesCleartextTraffic.*=false' <<<"$generator_manifest"

actual_cert="$($BUILD_TOOLS/apksigner verify --print-certs "$MAIN_APK" | sed -n 's/.*SHA-256 digest: //p' | head -1)"
[[ "$actual_cert" == "$EXPECTED_CERT" ]]

! unzip -p "$MAIN_APK" classes.dex | strings | grep -Fq "$PRIVATE_PREFIX"
unzip -p "$GENERATOR_APK" classes.dex | strings | grep -Fq "$PRIVATE_PREFIX"
! unzip -l "$MAIN_APK" | grep -Fq 'app/wensarafet/admin'

[[ -s "$PROJECT_DIR/build/apk/mapping.txt" ]]
[[ -s "$PROJECT_DIR/build/license-generator/mapping.txt" ]]
grep -q 'app.wensarafet.tracker.AppIntegrity -> ' "$PROJECT_DIR/build/apk/mapping.txt"
! grep -q 'app.wensarafet.tracker.AppIntegrity -> app.wensarafet.tracker.AppIntegrity:' "$PROJECT_DIR/build/apk/mapping.txt"

grep -q 'license_state.xml' "$PROJECT_DIR/app/src/main/res/xml/backup_rules.xml"
grep -q 'license_state.xml' "$PROJECT_DIR/app/src/main/res/xml/data_extraction_rules.xml"
db_version="$(sed -nE 's/^[[:space:]]*private static final int DB_VERSION[[:space:]]*=[[:space:]]*([0-9]+);.*$/\1/p' "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java" | head -1)"
[[ "$db_version" =~ ^[0-9]+$ && "$db_version" -ge 13 ]]
grep -q 'UNIQUE(transaction_id,debt_id,kind)' "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java"
grep -q 'CONFIGURATION_CHANGED' "$PROJECT_DIR/app/src/main/AndroidManifest.xml"
[[ -f "$PROJECT_DIR/app/src/main/res/drawable-night/widget_balance_two_background.xml" ]]
[[ -f "$PROJECT_DIR/app/src/main/res/drawable-night/widget_balance_three_background.xml" ]]
grep -q 'notification_preferences' "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/BackupFileManager.java"
grep -q 'transfer_names' "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/BackupFileManager.java"

echo "ReleaseAudit: signatures, packages, permissions, debt migration, widgets, key separation, R8, and backup exclusions passed"
