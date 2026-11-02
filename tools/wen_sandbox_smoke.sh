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
# Android emulator's Pixel Launcher can show a system ANR overlay even when
# the finance app underneath runs normally. Dismiss "Wait" and retry UI dump.
# Never treat Launcher ANR as a finance-app crash.
nav_dump() {
 local out_xml="$1"
 for attempt in 1 2 3 4; do
   adb shell uiautomator dump /sdcard/wen-nav-motion.xml >/dev/null || true
   adb pull /sdcard/wen-nav-motion.xml "$out_xml" >/dev/null || true
   if grep -q 'content-desc="تبويب التحليل"' "$out_xml"; then return 0; fi
   if grep -q "Pixel Launcher" "$out_xml"; then
     WAIT_POINT="$(python3 - "$out_xml" <<'PY'
import re,sys,xml.etree.ElementTree as ET
nodes=[n for n in ET.parse(sys.argv[1]).getroot().iter("node")
       if n.attrib.get("text")=="Wait"]
if len(nodes)!=1: raise SystemExit("System launcher ANR without Wait control")
m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",nodes[0].attrib.get("bounds",""))
if not m: raise SystemExit("Unable to locate system Wait control")
a,b,c,d=map(int,m.groups())
print((a+c)//2,(b+d)//2)
PY
)"
     read -r WX WY <<< "$WAIT_POINT"
     adb shell input tap "$WX" "$WY"
     sleep 3
   else
     sleep 2
   fi
 done
 echo "FAIL: accessibility tab missing after launcher-recovery retries"
 return 1
}
# Opt-in animated navigation test. No private account amounts in screenshots.
# The selected tab is found by accessibility description, not fixed device pixels.
adb shell settings put global window_animation_scale 1.0
adb shell settings put global transition_animation_scale 1.0
adb shell settings put global animator_duration_scale 1.0
adb shell am force-stop "$PACKAGE" || true
adb shell am start -W -n "$COMPONENT"
sleep 3
nav_dump "$OUT/wen-nav-motion.xml"
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
adb shell input swipe "$TX" "$TY" "$TX" "$TY" 1400 &
hold_pid=$!
sleep 0.69
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
# A whole-image cmp used to pass on unrelated pixels. Pixel-check a small
# rectangle centered on the ACTUAL held analytics tab. Fail if no visual lens.
python3 tools/check_nav_lens_pixels.py \
 "$OUT/nav-before-hold.png" "$OUT/nav-during-hold.png" "$TX" "$TY"
# Navigate back using accessibility semantics, no hard-coded device coordinates.
nav_dump "$OUT/wen-nav-after.xml"
HOME_POINT="$(python3 - "$OUT/wen-nav-after.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
nodes=[x for x in ET.parse(sys.argv[1]).getroot().iter("node")
       if x.attrib.get("content-desc")=="تبويب الرئيسية"]
if len(nodes)!=1: raise SystemExit("Home nav tab not accessible")
m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",nodes[0].attrib.get("bounds",""))
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

# Hold and drag across RTL navigation (no fixed phone coordinates).
nav_dump "$OUT/drag-start-home.xml"
DRAG_POINTS="$(python3 - "$OUT/drag-start-home.xml" <<'PY'
import re,sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter("node"))
def center(name):
    items=[n for n in nodes if n.attrib.get("content-desc")=="تبويب "+name]
    if len(items)!=1: raise SystemExit("Tab missing: "+name)
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",items[0].attrib.get("bounds",""))
    if not m: raise SystemExit("Invalid nav bounds")
    a,b,c,d=map(int,m.groups())
    return (a+c)//2,(b+d)//2
print(*center("الرئيسية"),*center("التحليل"))
PY
)"
read -r DX1 DY1 DX2 DY2 <<< "$DRAG_POINTS"
adb shell input swipe "$DX1" "$DY1" "$DX2" "$DY2" 750
sleep 2
adb shell uiautomator dump /sdcard/wen-after-drag.xml >/dev/null
adb pull /sdcard/wen-after-drag.xml "$OUT/wen-after-drag.xml" >/dev/null
python3 - "$OUT/wen-after-drag.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter("node"))
selected=[n for n in nodes if n.attrib.get("content-desc")=="تبويب التحليل"
          and n.attrib.get("selected")=="true"]
if len(selected)!=1: raise SystemExit("FAIL: hold-drag did not select analytics")
print("PASS: native long drag selected analytics")
PY
adb shell screencap -p /sdcard/wen-after-drag.png
adb pull /sdcard/wen-after-drag.png "$OUT/analytics-after-hold-drag.png"

adb logcat -d -v time -s AndroidRuntime:E > "$OUT/android-runtime.log" || true
adb shell dumpsys gfxinfo "$PACKAGE" > "$OUT/gfxinfo.txt" || true
if grep -E "FATAL EXCEPTION|Process: $PACKAGE" "$OUT/android-runtime.log"; then
 echo "Runtime exception in sandbox"
 exit 1
fi
echo "PASS: independent finance app launches home/analytics/transactions without activation"
