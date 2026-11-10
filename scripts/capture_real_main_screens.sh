#!/usr/bin/env bash
# Purely ephemeral Android 15 DEBUG-package instrumentation of REAL MainActivity.
# Never signs, installs, unlocks or touches the owner's existing application.
set -euo pipefail
# Debug x86_64 emulator cannot load the optional ARM-native gaussian
# library. The *real production* APK keeps all native libs. This exclusion
# exists only in a disposable CI checkout, never committed to app/build.gradle.
cat >> app/build.gradle <<'CI_DEBUG_ONLY'
android {
    packagingOptions {
        jniLibs {
            excludes += ['**/libnativegauss.so']
        }
    }
}
CI_DEBUG_ONLY
gradle :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon --console=plain
# Every ADB call has a hard deadline. A crashed SwiftShader emulator must
# yield a FAILED diagnostic job rather than tying up an Actions runner.
timeout -k 5s 45s adb install -r app/build/outputs/apk/debug/app-debug.apk
timeout -k 5s 45s adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Known-good 1.8.8-Sandbox emulator recovery: Pixel Launcher has an
# intermittent Android 15 ANR in GitHub runners although finance UI works.
# Only the CI emulator user 0 is affected; no package change.
timeout -k 5s 15s adb shell pm disable-user --user 0 com.google.android.apps.nexuslauncher || true
timeout -k 5s 15s adb shell am force-stop com.google.android.apps.nexuslauncher || true
timeout -k 5s 15s adb shell wm dismiss-keyguard || true
mkdir -p build/owner-real-main-screens
# Retain actual crash/renderer evidence if PixelCopy or the emulator dies.
timeout -k 5s 12s adb logcat -c || true
# Continuous logcat is deliberately bounded and killed before the job exits.
timeout -k 5s 300s adb logcat -v brief -s AndroidRuntime:E WenSarafet:E WenRealPreview:I TestRunner:I \
    > build/owner-real-main-screens/android-renderer.log 2>&1 &
log_pid=$!
cleanup() { kill "$log_pid" 2>/dev/null || true; }
trap cleanup EXIT
set +e
# The previous 600s instrumentation followed by an UNBOUNDED adb logcat
# repeatedly hung for 55m and got marked Cancelled by GitHub. Instead fail
# within 180s with the original instrumentation and renderer logs preserved.
timeout -k 5s 180s adb shell am instrument -w -e class \
    app.wensarafet.tracker.RealMainScreenCaptureTest \
    app.wensarafet.tracker.recovered.debug.test/androidx.test.runner.AndroidJUnitRunner \
    > build/owner-real-main-screens/instrumentation.txt 2>&1
status=$?
set -e
cleanup
cat build/owner-real-main-screens/instrumentation.txt
if [ "$status" -ne 0 ] || ! grep -Fq "OK (1 test)" \
        build/owner-real-main-screens/instrumentation.txt; then
    echo "FAIL: REAL MainActivity instrumentation exit=$status; no screenshot success claimed" >&2
    tail -n 150 build/owner-real-main-screens/android-renderer.log >&2 || true
    # Never let diagnostics hang after the emulator has already died!
    timeout -k 3s 12s adb logcat -d -t 160 2>/dev/null | tail -n 100 >&2 || true
    exit 1
fi
src=/sdcard/Android/data/app.wensarafet.tracker.recovered.debug/files/owner-real-screens
timeout -k 5s 40s adb pull "$src/." build/owner-real-main-screens/
total=$(find build/owner-real-main-screens -name '*.png' | wc -l)
test "$total" -eq 12 || {
    echo "Expected 12 genuine MainActivity PNGs (three gold themes); captured $total" >&2
    exit 1
}
for file in build/owner-real-main-screens/*.png; do
    test "$(stat -c '%s' "$file")" -gt 100000
done
echo "PASS: 12 full-resolution screenshots of protected MainActivity in isolated debug process."
ls -lh build/owner-real-main-screens/*.png
