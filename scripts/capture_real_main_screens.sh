#!/usr/bin/env bash
# Purely ephemeral Android 15 DEBUG-package instrumentation of REAL MainActivity.
# Never signs, installs, unlocks or touches the owner's existing application.
set -euo pipefail
gradle :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon --console=plain
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
mkdir -p build/owner-real-main-screens
adb shell am instrument -w -e class \
    app.wensarafet.tracker.RealMainScreenCaptureTest \
    app.wensarafet.tracker.recovered.debug.test/androidx.test.runner.AndroidJUnitRunner \
    | tee build/owner-real-main-screens/instrumentation.txt
grep -Eq 'OK \(1 test\)' build/owner-real-main-screens/instrumentation.txt || {
    echo 'Real MainActivity screenshot instrumentation did not pass' >&2
    adb logcat -d -t 500 | tail -n 240 >&2
    exit 1
}
src=/sdcard/Android/data/app.wensarafet.tracker.recovered.debug/files/owner-real-screens
adb pull "$src/." build/owner-real-main-screens/
total=$(find build/owner-real-main-screens -name '*.png' | wc -l)
test "$total" -eq 20 || {
    echo "Expected 20 genuine MainActivity PNGs; captured $total" >&2
    exit 1
}
for file in build/owner-real-main-screens/*.png; do
    test "$(stat -c '%s' "$file")" -gt 100000
done
echo "PASS: 20 full-resolution screenshots of protected MainActivity in isolated debug process."
ls -lh build/owner-real-main-screens/*.png
