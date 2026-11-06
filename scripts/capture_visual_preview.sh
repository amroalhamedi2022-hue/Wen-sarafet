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
    adb shell dumpsys activity activities | grep -q 'VisualPreviewActivity' || {
        echo "ERROR: preview Activity did not become visible for theme $theme" >&2
        adb logcat -d -t 250 | tail -n 100 >&2
        exit 1
    }
    # Android's emulator may capture an all-black transitional frame even
    # after am start -W returns OK. Reject it instead of reporting fake parity.
    png="build/visual-screenshots/theme-${theme}-android15.png"
    for attempt in 1 2 3 4; do
        sleep 3
        adb exec-out screencap -p > "$png"
        bytes=$(stat -c '%s' "$png")
        echo "Theme $theme frame $attempt: $bytes bytes"
        if [ "$bytes" -gt 120000 ]; then break; fi
    done
    test "$(stat -c '%s' "$png")" -gt 120000 || {
        echo "ERROR: theme $theme is black/blank after retries" >&2
        adb logcat -d -t 400 | tail -n 150 >&2
        exit 1
    }
    echo "Captured visually nonblank owner theme $theme"
done
ls -lh build/visual-screenshots/*.png
