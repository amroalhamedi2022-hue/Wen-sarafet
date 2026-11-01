#!/usr/bin/env bash
set -euo pipefail
adb wait-for-device
adb install -r glass-lab/showcase/build/outputs/apk/debug/showcase-debug.apk
adb shell wm dismiss-keyguard || true
mkdir -p glass-lab/screenshot-results
# This laboratory displays only synthetic mock data. No access to other apps.
for theme in 0 1 2 3; do
  for page in 0 1 2 3 4; do
    adb shell am force-stop app.wensarafet.glasslab || true
    adb shell am start -n app.wensarafet.glasslab/.MainActivity --ei preview_theme "$theme" --ei preview_page "$page"
    sleep 1
    adb shell screencap -p /sdcard/glass-screenshot.png
    adb pull /sdcard/glass-screenshot.png "glass-lab/screenshot-results/theme-${theme}-page-${page}.png"
  done
done
