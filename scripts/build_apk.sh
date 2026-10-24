#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
[[ -n "$ANDROID_SDK_ROOT" ]] || { echo "Set ANDROID_SDK_ROOT or ANDROID_HOME" >&2; exit 1; }
BUILD_TOOLS="$ANDROID_SDK_ROOT/build-tools/35.0.0"
ANDROID_JAR="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
BUILD_DIR="$PROJECT_DIR/build/apk"
GENERATED_DIR="$BUILD_DIR/generated"
CLASSES_DIR="$BUILD_DIR/classes"
DEX_DIR="$BUILD_DIR/dex"
R8_JAR="$ANDROID_SDK_ROOT/cmdline-tools/latest/lib/r8.jar"
KEY_DIR="$PROJECT_DIR/signing"
KEYSTORE="${SIGNING_KEYSTORE:-$KEY_DIR/wen-sarafet-test.jks}"
STORE_PASS="${SIGNING_STORE_PASSWORD:-}"
EXPECTED_CERT_SHA256="6B8268E5B647B12A2AA055D7A22F7B8EA888E40FB863E74409938AAD4EC789C9"
ECJ_JAR="${ECJ_JAR:-}"

rm -rf "$BUILD_DIR"
mkdir -p "$GENERATED_DIR" "$CLASSES_DIR" "$DEX_DIR" "$KEY_DIR"

"$BUILD_TOOLS/aapt2" compile --dir "$PROJECT_DIR/app/src/main/res" -o "$BUILD_DIR/resources.zip"
"$BUILD_TOOLS/aapt2" link \
  -I "$ANDROID_JAR" \
  --manifest "$PROJECT_DIR/app/src/main/AndroidManifest.xml" \
  --java "$GENERATED_DIR" \
  --min-sdk-version 33 \
  --target-sdk-version 35 \
  --version-code 16 \
  --version-name 1.8.0 \
  -o "$BUILD_DIR/unsigned.apk" \
  "$BUILD_DIR/resources.zip"

mapfile -t JAVA_FILES < <(find "$PROJECT_DIR/app/src/main/java" "$GENERATED_DIR" -name '*.java' -type f | sort)
if [[ -n "$ECJ_JAR" && -f "$ECJ_JAR" ]]; then
  java -jar "$ECJ_JAR" -encoding UTF-8 --release 8 -classpath "$ANDROID_JAR" -d "$CLASSES_DIR" "${JAVA_FILES[@]}"
else
  java com.sun.tools.javac.Main -encoding UTF-8 --release 8 -classpath "$ANDROID_JAR" -d "$CLASSES_DIR" "${JAVA_FILES[@]}"
fi

(cd "$CLASSES_DIR" && zip -q -r "$BUILD_DIR/classes.jar" .)
java -cp "$R8_JAR" com.android.tools.r8.R8 --release --min-api 33 --lib "$ANDROID_JAR" \
  --pg-conf "$PROJECT_DIR/proguard-rules.pro" --pg-map-output "$BUILD_DIR/mapping.txt" \
  --output "$DEX_DIR" "$BUILD_DIR/classes.jar"
(cd "$DEX_DIR" && zip -q -u "$BUILD_DIR/unsigned.apk" classes.dex)
"$BUILD_TOOLS/zipalign" -f 4 "$BUILD_DIR/unsigned.apk" "$BUILD_DIR/aligned.apk"

if [[ ! -f "$KEYSTORE" || -z "$STORE_PASS" ]]; then
  cp "$BUILD_DIR/aligned.apk" "$BUILD_DIR/Wen-Sarafet-1.8.0-unsigned.apk"
  echo "Unsigned build created. It cannot pass production integrity checks or update the installed release." >&2
  echo "$BUILD_DIR/Wen-Sarafet-1.8.0-unsigned.apk"
  exit 0
fi
ACTUAL_CERT_SHA256="$(keytool -list -v -keystore "$KEYSTORE" -storepass "$STORE_PASS" -alias wen-sarafet-test | sed -n 's/.*SHA256: //p' | tr -d ':' | head -1)"
if [[ "$ACTUAL_CERT_SHA256" != "$EXPECTED_CERT_SHA256" ]]; then
  echo "Signing certificate does not match AppIntegrity.java" >&2
  exit 1
fi

"$BUILD_TOOLS/apksigner" sign \
  --ks "$KEYSTORE" \
  --ks-key-alias wen-sarafet-test \
  --ks-pass "pass:$STORE_PASS" \
  --key-pass "pass:$STORE_PASS" \
  --out "$BUILD_DIR/Wen-Sarafet-1.8.0.apk" \
  "$BUILD_DIR/aligned.apk"

"$BUILD_TOOLS/apksigner" verify --verbose "$BUILD_DIR/Wen-Sarafet-1.8.0.apk"
"$BUILD_TOOLS/aapt2" dump badging "$BUILD_DIR/Wen-Sarafet-1.8.0.apk" | head -10
echo "$BUILD_DIR/Wen-Sarafet-1.8.0.apk"
