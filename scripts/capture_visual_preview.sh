#!/usr/bin/env bash
# DEBUG-ONLY owner visual captures, no signer, credentials or real accounts.
set -euo pipefail
gradle :app:assembleDebug --no-daemon --console=plain
adb install -r app/build/outputs/apk/debug/app-debug.apk
mkdir -p build/visual-screenshots
for theme in 25 26 27 28 29; do
    adb shell am force-stop app.wensarafet.tracker.recovered.debug || true
    adb shell am start -W -n \
       app.wensarafet.tracker.recovered.debug/app.wensarafet.tracker.VisualPreviewActivity \
       --ei theme "$theme"
    sleep 3
    adb shell dumpsys activity activities | grep -q 'VisualPreviewActivity' || {
        echo "ERROR: preview Activity did not become visible for theme $theme" >&2
        adb logcat -d -t 250 | tail -n 100 >&2
        exit 1
    }
    adb exec-out screencap -p > "build/visual-screenshots/theme-${theme}-android15.png"
    test -s "build/visual-screenshots/theme-${theme}-android15.png"
    echo "Captured owner theme $theme"
done
ls -lh build/visual-screenshots/*.png
