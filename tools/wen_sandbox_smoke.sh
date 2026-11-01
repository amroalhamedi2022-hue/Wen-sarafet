#!/usr/bin/env bash
set -euo pipefail
PACKAGE=app.wensarafet.tracker.sandbox
COMPONENT="$PACKAGE/app.wensarafet.tracker.MainActivity"
APK=app/build/outputs/apk/sandbox/app-sandbox.apk
OUT=app/sandbox-smoke-results
mkdir -p "$OUT"
adb wait-for-device
adb install -r "$APK"
adb shell wm dismiss-keyguard || true
adb logcat -c || true
capture() {
 local name="$1"
 shift
 adb shell am force-stop "$PACKAGE" || true
 adb shell am start -W -n "$COMPONENT" "$@"
 sleep 5
 if ! adb shell pidof "$PACKAGE" >/dev/null 2>&1; then
   echo "FAIL: full sandbox app crashed at $name"
   adb logcat -d -v time -s AndroidRuntime:E > "$OUT/android-runtime.log" || true
   tail -100 "$OUT/android-runtime.log"
   exit 1
 fi
 adb shell screencap -p /sdcard/wen-sandbox-smoke.png
 adb pull /sdcard/wen-sandbox-smoke.png "$OUT/$name.png"
 adb shell dumpsys activity activities > "$OUT/activities-$name.txt" || true
}
capture home
capture analytics --ez open_analytics true
capture transactions --ez open_transactions true
adb logcat -d -v time -s AndroidRuntime:E > "$OUT/android-runtime.log" || true
adb shell dumpsys gfxinfo "$PACKAGE" > "$OUT/gfxinfo.txt" || true
if grep -E "FATAL EXCEPTION|Process: $PACKAGE" "$OUT/android-runtime.log"; then
 echo "Runtime exception in sandbox"
 exit 1
fi
echo "PASS: independent finance app launches home/analytics/transactions without activation"
