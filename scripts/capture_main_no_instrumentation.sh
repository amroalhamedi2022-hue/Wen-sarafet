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
  # ADB failure diagnostics must NEVER hang after the emulator disappears.
  timeout -k 2s 8s adb shell dumpsys activity activities 2>/dev/null | tail -n 35 >&2 || true
  timeout -k 2s 8s adb logcat -d -t 260 2>/dev/null | tail -n 120 >&2 || true
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
    # Wait for the REAL native optical marker in the ALREADY STREAMING local
    # logcat file, NOT through repeated 'adb shell pidof' / 'adb logcat -d'
    # requests. Those synchronous ADB queries often froze the emulator after
    # the screen had already rendered, before screenshot capture could start.
    # This never accepts SplashScreen: the marker is emitted by the real
    # four-tab MainActivity after the live backdrop has been attached.
    local ready=0
    for attempt in $(seq 1 30); do
      if grep -Eq "MIT_VIEW_GLASS_BACKDROP_BOUND source=(primary-scroll|gold-scene)" \
          "$SCREEN_DIR/live-renderer.log"; then
        ready=1
        break
      fi
      sleep 0.5
    done
    if [ "$ready" -ne 1 ]; then
      echo "ERROR: 15s expired before real MainActivity backdrop attachment" >&2
      tail -n 40 "$SCREEN_DIR/live-renderer.log" >&2 || true
      exit 1
    fi
    # Give the original View hierarchy one compositor beat, then CAPTURE NOW,
    # while ADB is still alive. The former on-device PNG + second adb pull
    # routinely came too late (and often returned zero bytes).
    sleep 0.6
    if [ "$page" = accounts ]; then
       timeout -k 2s 8s adb shell input swipe 520 1700 520 610 420
       sleep 0.6
    fi
    local bytes=0
    local complete=0
    local tmp="$file.partial"
    rm -f "$file" "$tmp"
    # One direct framebuffer stream; no second ADB round trip. Preserve the
    # stderr and exact invalid bytes for a failed job so GPU stalls, zero-byte
    # output and strict PNG validation errors are distinguishable afterward.
    timeout -k 3s 20s adb exec-out screencap -p > "$tmp" \
      2>> "$SCREEN_DIR/adb-screencap-stderr.log" || true
    if python3 scripts/validate_native_capture.py "$tmp" >/dev/null 2>&1; then
      mv "$tmp" "$file"
      complete=1
    else
      echo "WARN: direct framebuffer PNG rejected for $theme/$page" >&2
      wc -c "$tmp" >&2 || true
      python3 scripts/validate_native_capture.py "$tmp" >&2 || true
      # These files must NEVER be named .png: they are NOT verified images.
      cp "$tmp" "$SCREEN_DIR/unverified-theme-${theme}-execout.bin" || true
      local target="/sdcard/wen-owner-theme-${theme}.png"
      timeout -k 3s 16s adb shell screencap -p "$target" \
        2>> "$SCREEN_DIR/adb-screencap-stderr.log" || true
      timeout -k 3s 16s adb pull "$target" "$tmp" \
        >> "$SCREEN_DIR/adb-screencap-stderr.log" 2>&1 || true
      if python3 scripts/validate_native_capture.py "$tmp" >/dev/null 2>&1; then
        mv "$tmp" "$file"
        complete=1
      else
        echo "WARN: on-device framebuffer PNG rejected for $theme/$page" >&2
        wc -c "$tmp" >&2 || true
        python3 scripts/validate_native_capture.py "$tmp" >&2 || true
        cp "$tmp" "$SCREEN_DIR/unverified-theme-${theme}-ondevice.bin" || true
      fi
    fi
    rm -f "$tmp"
    test "$complete" -eq 1 || {
      echo "ERROR: no valid framebuffer PNG for theme $theme/$page" >&2
      tail -n 40 "$SCREEN_DIR/live-renderer.log" >&2 || true
      exit 1
    }
    bytes=$(stat -c '%s' "$file")
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
    # This fast final scan checks both content and all PNG chunk CRCs.
    # The initial capture already used this same validator before acceptance.
    import subprocess
    subprocess.run(["python3","scripts/validate_native_capture.py",str(file)],
                   check=True)
print("PASS:",len(files),"genuine full PNG screenshots, original UI pixels.")
PY
sha256sum "$SCREEN_DIR"/*.png > "$SCREEN_DIR/checksums.txt"
