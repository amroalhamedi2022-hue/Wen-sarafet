#!/usr/bin/env bash
set -euo pipefail
adb wait-for-device
adb install -r glass-lab/showcase/build/outputs/apk/debug/showcase-debug.apk
adb shell wm dismiss-keyguard || true
mkdir -p glass-lab/screenshot-results
adb logcat -c || true
adb shell am force-stop app.wensarafet.glasslab || true
adb shell am start -W -n app.wensarafet.glasslab/.MainActivity --ei preview_theme 0 --ei preview_page 0 || true
sleep 4
adb logcat -d -v time -s AndroidRuntime:E ActivityTaskManager:E > glass-lab/screenshot-results/android-runtime.log || true
adb shell dumpsys activity activities > glass-lab/screenshot-results/activity-state.txt || true
adb shell screencap -p /sdcard/glass-screenshot.png || true
adb pull /sdcard/glass-screenshot.png glass-lab/screenshot-results/theme-0-page-0.png || true
if ! adb shell pidof app.wensarafet.glasslab >/dev/null 2>&1; then
  echo "Lab activity exited/crashed. See android-runtime.log; do not claim screenshot parity."
  tail -120 glass-lab/screenshot-results/android-runtime.log
  exit 1
fi
