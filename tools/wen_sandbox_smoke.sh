#!/usr/bin/env bash
set -euo pipefail
PACKAGE=app.wensarafet.tracker.sandbox
COMPONENT="$PACKAGE/app.wensarafet.tracker.MainActivity"
APK=app/build/outputs/apk/sandbox/app-sandbox.apk
OUT=app/sandbox-smoke-results
mkdir -p "$OUT"
trap 'adb logcat -d -v time -s WenGlassProbe:I > "$OUT/wen-glass-probe.log" 2>/dev/null || true' EXIT
adb wait-for-device
adb install -r "$APK"
# Pixel Launcher is not under test; its ANR repeatedly blocked screenshots
# on GitHub runners, despite the finance app continuing to function.
# The test starts the app by its explicit component, so the Home launcher is
# unnecessary. Disabling it affects ONLY this disposable emulator instance.
adb shell pm disable-user --user 0 com.google.android.apps.nexuslauncher || true
adb shell am force-stop com.google.android.apps.nexuslauncher || true
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
# Check both material clarity AND optical movement in the same emulator run.
# Do not abort early on tint bleed and conceal whether refraction also works.
nav_ghost_failed=0
if ! python3 tools/check_liquid_glass_ghosting.py "$OUT/home.png"; then
  nav_ghost_failed=1
  echo "INFO: nav tint failed; still gathering independent optical-motion evidence"
fi
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
   # The underlying app tabs remain in uiautomator XML EVEN WHEN a
   # blocking Pixel Launcher ANR is on top. Recover the modal FIRST.
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
     continue
   fi
   if grep -q 'content-desc="تبويب التحليل"' "$out_xml"; then return 0; fi
   sleep 2
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
# Acceptance for contextual top chrome and accessible bubble FAB.
python3 - "$OUT/wen-nav-motion.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter("node"))
assert any(n.get("text")=="الرئيسية" for n in nodes), "Home header must describe this page"
assert any(n.get("content-desc")=="إضافة عملية" for n in nodes), "Bubble FAB missing"
print("PASS: contextual primary header and accessible bubble button")
PY
FAB_POINT="$(python3 - "$OUT/wen-nav-motion.xml" <<'PY'
import sys,re,xml.etree.ElementTree as ET
nodes=[n for n in ET.parse(sys.argv[1]).getroot().iter("node")
       if n.get("content-desc")=="إضافة عملية"]
if len(nodes)!=1: raise SystemExit("FAB accessible node missing/duplicated")
m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",nodes[0].get("bounds",""))
if not m: raise SystemExit("FAB has no usable bounds")
a,b,c,d=map(int,m.groups())
print((a+c)//2,(b+d)//2)
PY
)"
read -r FX FY <<< "$FAB_POINT"
# System Pixel Launcher sometimes presents an ANR over the sandbox in this
# low-CPU emulated environment. A modal can consume an otherwise valid FAB tap.
# Recover *that system modal* then retry the same accessible button; do not
# pretend the finance app opened if the form is absent.
form_opened=0
for attempt in 1 2 3 4; do
  adb shell input tap "$FX" "$FY"
  sleep 2
  for read_attempt in 1 2 3; do
    adb shell uiautomator dump /sdcard/wen-add-transaction.xml >/dev/null || true
    adb pull /sdcard/wen-add-transaction.xml "$OUT/wen-add-transaction.xml" >/dev/null || true
    if grep -q 'text="نوع العملية"' "$OUT/wen-add-transaction.xml" &&
       grep -q 'text="إضافة عملية"' "$OUT/wen-add-transaction.xml"; then
      form_opened=1
      break
    fi
    if grep -q 'Pixel Launcher' "$OUT/wen-add-transaction.xml"; then
      WAIT_POINT="$(python3 - "$OUT/wen-add-transaction.xml" <<'PY'
import re,sys,xml.etree.ElementTree as ET
nodes=[n for n in ET.parse(sys.argv[1]).getroot().iter("node")
       if n.get("text")=="Wait"]
if len(nodes)!=1: raise SystemExit("Launcher ANR without Wait button")
m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",nodes[0].get("bounds",""))
if not m: raise SystemExit("Cannot locate system Wait")
a,b,c,d=map(int,m.groups())
print((a+c)//2,(b+d)//2)
PY
)"
      read -r WX WY <<< "$WAIT_POINT"
      echo "INFO: dismissed unrelated Pixel Launcher ANR, retrying visual check"
      adb shell input tap "$WX" "$WY"
      sleep 2
    else
      sleep 1
    fi
  done
  if [ "$form_opened" -eq 1 ]; then break; fi
done
if [ "$form_opened" -ne 1 ]; then
 echo "FAIL: bubble FAB tap did not open Add Transaction form after launcher recovery"
 exit 1
fi
echo "PASS: glass FAB tap opens Add Transaction form"
if ! adb exec-out screencap -p > "$OUT/fab-add-transaction.png"; then
  echo "WARN: optional FAB screenshot unavailable; testing return and lens behavior"
fi
# Reverse Add Transaction (Android Back) must reach the same Home page.
# Verify completion after the 360 ms circular-close animation settles.
adb shell input keyevent 4
sleep 2
# Use the existing Pixel Launcher ANR recovery for this checkpoint too.
# On slow emulators the system launcher modal may reappear after Back even
# though the finance form opened and closed correctly.
nav_dump "$OUT/wen-after-reverse.xml"
python3 - "$OUT/wen-after-reverse.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter("node"))
desc=[n.get("content-desc") for n in nodes]
if "تبويب الرئيسية" not in desc or "إضافة عملية" not in desc:
    raise SystemExit("FAIL: system Back did not reverse Add Transaction to primary Home")
print("PASS: reverse Add Transaction return preserves Home and accessible FAB")
PY
if ! adb exec-out screencap -p > "$OUT/fab-after-reverse.png"; then
  echo "WARN: optional Back screenshot unavailable; continuing held-lens proof"
fi
# Re-enter home for existing Hold+Drag verification.
adb shell am force-stop "$PACKAGE"
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
# Keep the pointer down long enough for two independent ADB screenshots.
# Some CI emulators delay screencap until after a five-second swipe releases,
# falsely reporting 0 changed pixels even though AGSL_DRAW_ACTIVE was logged.
adb shell input swipe "$TX" "$TY" "$TX" "$TY" 13000 &
hold_pid=$!
sleep 1.20
adb exec-out screencap -p > "$OUT/nav-during-hold.png"
sleep 1.40
adb exec-out screencap -p > "$OUT/nav-during-hold-second.png"
# Ensure both frames were captured while the pointer was still down.
if ! kill -0 "$hold_pid" 2>/dev/null; then
  echo "FAIL: emulator screenshot capture overran held touch; optical QA inconclusive"
  wait "$hold_pid" || true
  exit 1
fi
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
if ! python3 tools/check_nav_lens_pixels.py \
 "$OUT/nav-before-hold.png" "$OUT/nav-during-hold.png" "$TX" "$TY"; then
  echo "INFO: first GPU capture unchanged; checking independent second frame"
  python3 tools/check_nav_lens_pixels.py \
   "$OUT/nav-before-hold.png" "$OUT/nav-during-hold-second.png" "$TX" "$TY"
fi
# The SANDBOX advanced renderer must be real, not a silent static-pill fallback.
adb logcat -d -v time -s WenGlassProbe:I > "$OUT/advanced-glass-probe.log"
if ! grep -q 'MIT_VIEW_GLASS_ATTACHED' "$OUT/advanced-glass-probe.log" ||
   ! grep -q 'MIT_VIEW_GLASS_BACKDROP_BOUND' "$OUT/advanced-glass-probe.log"; then
  echo "FAIL: sandbox native View glass did not attach to actual backdrop"
  exit 1
fi
if ! grep -q 'MIT_VIEW_GLASS_PRESS_ACTIVE' "$OUT/advanced-glass-probe.log"; then
  echo "FAIL: native Liquid Glass pressed refraction never became active"
  exit 1
fi
if ! grep -q 'MIT_FAB_GLASS_ATTACHED' "$OUT/advanced-glass-probe.log" ||
   ! grep -q 'MIT_FAB_BACKDROP_BOUND source=root' "$OUT/advanced-glass-probe.log" ||
   ! grep -q 'MIT_VIEW_GLASS_BACKDROP_BOUND source=scroll' "$OUT/advanced-glass-probe.log"; then
  echo "FAIL: FAB/nav material not sampling the real scroll underlay"
  exit 1
fi
echo "PASS: native glass FAB/nav sample scroll underlay and respond to touch"
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
if [ "${nav_ghost_failed:-0}" -ne 0 ]; then
  echo "FAIL: optical motion tested, but nav-background contamination exceeds visual limit"
  exit 1
fi
echo "PASS: independent finance app launches home/analytics/transactions without activation"
