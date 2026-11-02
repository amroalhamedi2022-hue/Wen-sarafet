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
# Opt-in animated navigation test. No private account amounts in screenshots.
# The selected tab is found by accessibility description, not fixed device pixels.
adb shell settings put global window_animation_scale 1.0
adb shell settings put global transition_animation_scale 1.0
adb shell settings put global animator_duration_scale 1.0
adb shell am force-stop "$PACKAGE" || true
adb shell am start -W -n "$COMPONENT"
sleep 3
adb shell uiautomator dump /sdcard/wen-nav-motion.xml >/dev/null
adb pull /sdcard/wen-nav-motion.xml "$OUT/wen-nav-motion.xml" >/dev/null
ANALYTICS_POINT="$(python3 - "$OUT/wen-nav-motion.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
nodes=[n for n in root.iter("node") if n.attrib.get("content-desc")=="تبويب التحليل"]
if len(nodes)!=1:
    raise SystemExit("Expected one accessible analytics tab, found "+str(len(nodes)))
b=nodes[0].attrib.get("bounds","")
m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",b)
if m is None: raise SystemExit("Missing tab bounds: "+b)
x1,y1,x2,y2=map(int,m.groups())
if x2<=x1 or y2<=y1: raise SystemExit("Invalid tab dimensions")
print((x1+x2)//2,(y1+y2)//2)
PY
)"
read -r TX TY <<< "$ANALYTICS_POINT"
adb shell input tap "$TX" "$TY"
sleep 2
if ! adb shell pidof "$PACKAGE" >/dev/null 2>&1; then
 echo "FAIL: tapping Liquid analytics tab crashed the sandbox"
 exit 1
fi
adb shell screencap -p /sdcard/wen-sandbox-motion.png
adb pull /sdcard/wen-sandbox-motion.png "$OUT/analytics-after-animated-tab-tap.png"
adb logcat -d -v time -s AndroidRuntime:E > "$OUT/android-runtime.log" || true
adb shell dumpsys gfxinfo "$PACKAGE" > "$OUT/gfxinfo.txt" || true
if grep -E "FATAL EXCEPTION|Process: $PACKAGE" "$OUT/android-runtime.log"; then
 echo "Runtime exception in sandbox"
 exit 1
fi
echo "PASS: independent finance app launches home/analytics/transactions without activation"
