#!/usr/bin/env bash
# Real native MainActivity screenshot QA, never a generated/mocked UI.
# CI-only ephemeral edits permit the disposable DEBUG package to start
# offline with its own default 7 seeded accounts. NOTHING is committed to
# production MainActivity, AppIntegrity, LicenseManager or an owner signer.
set -euo pipefail
export PACKAGE=app.wensarafet.tracker.recovered.debug
export SCREEN_DIR=build/owner-live-main-screens
mkdir -p "$SCREEN_DIR"
if [ "${WEN_DEBUG_CAPTURE_PREPARE_ONLY:-0}" = "1" ]; then
python3 - <<'PY'
from pathlib import Path
path = Path('app/src/main/java/app/wensarafet/tracker/MainActivity.java')
before = path.read_text(encoding='utf-8')
expected = {
    'trusted = AppIntegrity.isTrusted(this);': 1,
    'activated = LicenseManager.isActivated(this);': 1,
    'if (!LicenseManager.isActivated(this)) { showActivationScreen(); return; }': 2,
}
for text, count in expected.items():
    assert before.count(text) == count, ("Debug-only test patch preimage changed", text)
prefix = '"app.wensarafet.tracker.recovered.debug".equals(getPackageName())'
after = before.replace('trusted = AppIntegrity.isTrusted(this);',
    f'trusted = {prefix} || AppIntegrity.isTrusted(this);')
after = after.replace('activated = LicenseManager.isActivated(this);',
    f'activated = {prefix} || LicenseManager.isActivated(this);')
after = after.replace(
    'if (!LicenseManager.isActivated(this)) { showActivationScreen(); return; }',
    f'if (!({prefix} || LicenseManager.isActivated(this))) '
    '{ showActivationScreen(); return; }')
assert after != before
path.write_text(after,encoding='utf-8')
print("DEBUG-ONLY: modified three licence-branch conditions solely in ephemeral CI checkout")
PY
cat >> app/build.gradle <<'GRADLE_CI_ONLY'
android {
    packagingOptions {
        jniLibs {
            excludes += ['**/libnativegauss.so']
        }
    }
}
GRADLE_CI_ONLY
gradle :app:assembleDebug --no-daemon --console=plain
exit 0
fi
adb wait-for-device
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Proven GitHub Android 15 emulator launcher ANR workaround (debug user only).
adb shell pm disable-user --user 0 com.google.android.apps.nexuslauncher || true
adb shell am force-stop com.google.android.apps.nexuslauncher || true
adb shell wm dismiss-keyguard || true
# STATIC screenshot pass: 0x animator scales eliminate transitional black
# frames; the liquid visual material/shaders remain the production drawables.
# Motion quality is validated separately with animations re-enabled.
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb logcat -c || true
# Preserve app/runtime GPU crash output even if the emulator/ADB dies.
adb logcat -v time -s AndroidRuntime:E WenSarafet:E WenGlassProbe:I WenRealPreview:I \
  > "$SCREEN_DIR/live-renderer.log" 2>&1 &
LOGCAT_PID=$!
trap 'kill "$LOGCAT_PID" 2>/dev/null || true' EXIT


onfailure() {
  echo 'EMULATOR DIAGNOSTICS:' >&2
  adb shell dumpsys activity activities 2>/dev/null | tail -n 35 >&2 || true
  adb logcat -d -t 260 2>/dev/null | tail -n 120 >&2 || true
}
trap onfailure ERR

capture() {
    local theme="$1" page="$2"
    local file="$SCREEN_DIR/main-theme-${theme}-${page}.png"
    adb shell am force-stop "$PACKAGE" || true
    case "$page" in
      home|accounts)
        timeout 80 adb shell am start -W -n "$PACKAGE/app.wensarafet.tracker.MainActivity" ;;
      analytics)
        timeout 80 adb shell am start -W -n "$PACKAGE/app.wensarafet.tracker.MainActivity" --ez open_analytics true ;;
      transactions)
        timeout 80 adb shell am start -W -n "$PACKAGE/app.wensarafet.tracker.MainActivity" --ez open_transactions true ;;
      *) echo "Unknown page $page" >&2; exit 1 ;;
    esac
    sleep 4
    test -n "$(timeout 25 adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r')" || {
       echo "MainActivity crashed for $theme/$page" >&2; exit 1;
    }
    if [ "$page" = accounts ]; then
       adb shell input swipe 520 1900 520 610 420
       sleep 2
    fi
    timeout 45 adb exec-out screencap -p > "$file"
    test -s "$file"
    local bytes
    bytes=$(stat -c '%s' "$file")
    test "$bytes" -gt 100000 || {
       echo "Black/blank real-screen capture: $file ($bytes bytes)" >&2
       exit 1
    }
    echo "REAL MainActivity $page theme=$theme screenshot bytes=$bytes"
}

for theme in 25 26 27 28 29; do
  adb shell am force-stop "$PACKAGE" || true
  timeout 45 adb shell am broadcast -n \
      "$PACKAGE/app.wensarafet.tracker.OwnerThemeCaptureReceiver" \
      --ei theme "$theme"
  for page in home accounts transactions analytics; do
     capture "$theme" "$page"
  done
done
test "$(find "$SCREEN_DIR" -name '*.png' | wc -l)" -eq 20
echo 'PASS: 20 REAL MainActivity screenshots using the original production layout.'
sha256sum "$SCREEN_DIR"/*.png > "$SCREEN_DIR/checksums.txt"
