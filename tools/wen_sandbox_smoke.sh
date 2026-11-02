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
# Hold for 700ms; capture the lens while the pointer is still pressed.
# No extra click/hold handler is invoked by the test: native Android input only.
adb shell screencap -p /sdcard/wen-before-hold.png
adb pull /sdcard/wen-before-hold.png "$OUT/nav-before-hold.png" >/dev/null
adb shell input swipe "$TX" "$TY" "$TX" "$TY" 700 &
hold_pid=$!
sleep 0.43
adb shell screencap -p /sdcard/wen-during-hold.png
adb pull /sdcard/wen-during-hold.png "$OUT/nav-during-hold.png" >/dev/null
wait "$hold_pid"
sleep 2
if ! adb shell pidof "$PACKAGE" >/dev/null 2>&1; then
 echo "FAIL: tapping Liquid analytics tab crashed the sandbox"
 exit 1
fi
adb shell screencap -p /sdcard/wen-sandbox-motion.png
adb pull /sdcard/wen-sandbox-motion.png "$OUT/analytics-after-animated-tab-tap.png"
if cmp -s "$OUT/nav-before-hold.png" "$OUT/nav-during-hold.png"; then
 echo "FAIL: long hold produced an identical screenshot; inspect motion layer"
 exit 1
fi
# Navigate back using accessibility semantics, no hard-coded device coordinates.
adb shell uiautomator dump /sdcard/wen-nav-after.xml >/dev/null
adb pull /sdcard/wen-nav-after.xml "$OUT/wen-nav-after.xml" >/dev/null
HOME_POINT="$(python3 - "$OUT/wen-nav-after.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
nodes=[x for x in ET.parse(sys.argv[1]).getroot().iter("node")
       if x.attrib.get("content-desc")=="تبويب الرئيسية"]
if len(nodes)!=1: raise SystemExit("Home nav tab not accessible")
m=re.fullmatch(r"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]",nodes[0].attrib.get("bounds",""))
if not m: raise SystemExit("Missing home bounds")
a,b,c,d=map(int,m.groups())
print((a+c)//2,(b+d)//2)
PY
)"
read -r HX HY <<< "$HOME_POINT"
adb shell input tap "$HX" "$HY"
sleep 1
if ! adb shell pidof "$PACKAGE" >/dev/null 2>&1; then
 echo "FAIL: navigation back to main screen crashed the sandbox"
 exit 1
fi
adb shell screencap -p /sdcard/wen-after-home.png
adb pull /sdcard/wen-after-home.png "$OUT/home-after-hold-return.png"
adb logcat -d -v time -s AndroidRuntime:E > "$OUT/android-runtime.log" || true
adb shell dumpsys gfxinfo "$PACKAGE" > "$OUT/gfxinfo.txt" || true
if grep -E "FATAL EXCEPTION|Process: $PACKAGE" "$OUT/android-runtime.log"; then
 echo "Runtime exception in sandbox"
 exit 1
fi
echo "PASS: independent finance app launches home/analytics/transactions without activation"
