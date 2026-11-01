#!/usr/bin/env bash
set -euo pipefail
adb wait-for-device
adb install -r glass-lab/showcase/build/outputs/apk/debug/showcase-debug.apk
adb shell wm dismiss-keyguard || true
mkdir -p glass-lab/screenshot-results
adb logcat -c || true

capture() {
  local theme="$1"
  local page="$2"
  adb shell am force-stop app.wensarafet.glasslab || true
  adb shell am start -W -n app.wensarafet.glasslab/.MainActivity \
    --ei preview_theme "$theme" --ei preview_page "$page"
  sleep 2
  if ! adb shell pidof app.wensarafet.glasslab >/dev/null 2>&1; then
    adb logcat -d -v time -s AndroidRuntime:E > glass-lab/screenshot-results/android-runtime.log || true
    echo "APP IS NOT RUNNING: theme=$theme page=$page"
    cat glass-lab/screenshot-results/android-runtime.log
    return 1
  fi
  adb shell screencap -p /sdcard/glass-screenshot.png
  adb pull /sdcard/glass-screenshot.png "glass-lab/screenshot-results/theme-${theme}-page-${page}.png"
}

for page in 0 1 2 3 4; do capture 0 "$page"; done
for theme in 1 2 3; do capture "$theme" 0; done
adb logcat -d -v time -s AndroidRuntime:E > glass-lab/screenshot-results/android-runtime.log || true
adb shell dumpsys gfxinfo app.wensarafet.glasslab > glass-lab/screenshot-results/emulator-frame-info.txt || true
