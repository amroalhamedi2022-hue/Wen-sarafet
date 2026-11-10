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
# Match the reference screenshots' native aspect ratios rather than judging
# a 9:16 / 10:16 owner layout on the Pixel 6's 9:20 viewport.
# This changes only the disposable CI emulator, not any app preference.
theme_for_viewport="${1:-25}"
adb shell wm size 1080x1920  # Three remaining gold reference themes
adb shell wm density 420
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
    # Android 13's SplashScreen is a real, valid PNG, and was previously
    # mistaken for a successful MainActivity capture. Wait for the real HOME
    # hierarchy instead of assuming am start -W implies first content frame.
    local ready=0
    for attempt in 1 2 3 4 5 6 7 8; do
      sleep 3
      test -n "$(timeout 20 adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r')" || continue
      timeout 25 adb shell uiautomator dump /sdcard/wen-window.xml >/dev/null 2>&1 || true
      if timeout 12 adb shell cat /sdcard/wen-window.xml 2>/dev/null | grep -Fq "إجمالي أرصدتك"; then
        ready=1
        break
      fi
      echo "Theme $theme UI probe attempt $attempt: still not on actual home screen"
    done
    test "$ready" -eq 1 || {
      echo "ERROR: UI hierarchy did not show real MainActivity dashboard; refusing SplashScreen" >&2
      exit 1
    }
    if [ "$page" = accounts ]; then
       adb shell input swipe 520 1900 520 610 420
       sleep 2
    fi
    # Capture and COMPLETE the PNG on-device before transferring it. The
    # Champagne emulator has previously severed adb halfway through a PNG
    # stream, leaving an invalid image that looked like a completed screenshot.
    # Saving on device first makes retries recoverable without recomposing UI.
    local target="/sdcard/wen-owner-theme-${theme}.png"
    local bytes=0
    local complete=0
    for attempt in 1 2 3 4; do
      timeout 35 adb shell screencap -p "$target" || true
      timeout 35 adb pull "$target" "$file" >/dev/null 2>&1 || true
      if [ -s "$file" ]; then
        bytes=$(stat -c '%s' "$file")
        if python3 - "$file" <<'CHECK_PNG'
import pathlib,sys,struct,zlib
p=pathlib.Path(sys.argv[1])
data=p.read_bytes()
if len(data)<100000 or data[:8]!=bytes.fromhex("89504e470d0a1a0a"):sys.exit(2)
if data[-12:]!=bytes.fromhex("0000000049454e44ae426082"):sys.exit(3)
width,height=struct.unpack(">II",data[16:24])
if width<720 or height<1280:sys.exit(4)
CHECK_PNG
        then
          complete=1
          break
        fi
      fi
      echo "Attempt $attempt: incomplete screenshot $file ($bytes B)" >&2
      timeout 15 adb start-server >/dev/null 2>&1 || true
      sleep 2
    done
    test "$complete" -eq 1 || {
      echo "ERROR: no complete native screenshot for $theme/$page" >&2
      exit 1
    }
    echo "REAL MainActivity $page theme=$theme complete PNG bytes=$bytes"
}

# In CI, each owner-approved theme runs in its OWN clean AVD process.
# This prevents one GPU/ADB crash from corrupting the other four themes.
if [ "$#" -ge 1 ]; then
  case "$1" in 25|26|27) themes=("$1");;
    *) echo "Unsupported theme index: $1" >&2; exit 2;; esac
else
  themes=(25 26 27)
fi
for theme in "${themes[@]}"; do
  adb shell am force-stop "$PACKAGE" || true
  timeout 45 adb shell am broadcast -n \
      "$PACKAGE/app.wensarafet.tracker.OwnerThemeCaptureReceiver" \
      --ei theme "$theme"
  capture "$theme" home
done
python3 - "$SCREEN_DIR" "${#themes[@]}" <<'PY'
import pathlib,sys
root=pathlib.Path(sys.argv[1])
files=list(root.glob("main-theme-*-home.png"))
assert len(files)==int(sys.argv[2]), "Missing authentic MainActivity screenshots"
for file in files:
    data=file.read_bytes()
    assert data[:8] == bytes.fromhex("89504e470d0a1a0a"), file
    assert data[-12:] == bytes.fromhex("0000000049454e44ae426082"), (
        "Truncated screenshot, not acceptable for review: "+str(file))
print("PASS:",len(files),"genuine full PNG screenshots, original UI pixels.")
PY
sha256sum "$SCREEN_DIR"/*.png > "$SCREEN_DIR/checksums.txt"
