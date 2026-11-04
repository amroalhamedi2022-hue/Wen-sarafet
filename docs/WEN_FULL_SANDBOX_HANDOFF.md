# وين صرفت — نسخة الاختبار الوظيفية المستقلة

## 4 November 2026 — Navigation-only Liquid deformation (Batch 1)

**Scope:** User explicitly limited this iteration to the bottom **navigation bar**. No FAB refactor, transaction/finance, database, background notification, authentication, production signing or main-branch change. This continues the already-approved neutral, touch-through optical lens.

- `GlassMotion.NavigationGesture`: finger-follow is now a bounded 0.78 per-touch viscous interpolation; selected-tab release uses a finite damped spring; the material receives a real pointer anchor on DOWN; each fresh touch resets old drag energy (no stale stretched state).
- Sandbox-only `SandboxOpticalGlass.attach(...)`: neutral optical refraction/bevel and capsule X/Y deformation adapt to pull and event delta; the exact same finite press animator smoothly returns strain to zero. Power-saver/disabled animations use no directional stretch. The user-approved no-rainbow, above-label, touch-through behavior is preserved.
- `tests/glass_motion_contract.py` and the real Android emulator smoke enforce source invariants and require `NAV_LIQUID_STRETCH_ACTIVE` while dragging, plus the original real tab navigation to Analytics. Existing FAB behavior and tests remain unchanged.
- Source/finance safety and sandbox APK build+lint passed for product-code commit `491bd5b3821920ee865131e2e6a5922f0038ddb9`: runs `41797059369` and `41797059188`. Its test APK is `app.wensarafet.tracker.sandbox`, size 13,114,737 bytes, SHA-256 `d96b1cd8c9c4efa9737ebeac0fc3b78afac633ce5c3fe3626a5408ad14f74c10`; extracted from artifact `13305628293` (not a production update).
- Real Android emulator run `41797059186` confirmed nav hold, readable labels, live liquid stretch and successful Home→Analytics Hold+Drag; it failed **after** success due to a transient `adb pull` screenshot-transfer interruption. The QA transport was hardened in test-only commits `1981a525c86b855f8e843748fc1fa0691f742d66` and `df71dc904cbffe1d5ef0f541d186e4a9f2134332`, and smoke was retriggered as `41797876568`. Do not equate a passed APK compile with a fully green smoke run until that result is checked.
- Physical tablet/phone parity, motion feel, 60/120 Hz jank, and disabled-motion accessibility still need hands-on verification before merging into the protected production app.


Built from latest functional `fix/wen-contact-picker-xiaomi-20261101` branch,
which already incorporates the 22-theme visual pilot and the contact-picker repair.

## Identity
- Original app stays `app.wensarafet.tracker` and retains signature, license,
  PIN/biometric, backup encryption and financial safe-zone logic unchanged.
- Trial build type uses `app.wensarafet.tracker.sandbox` and the launcher label
  `وين صرفت — تجربة`; it has **its own empty SQLite and preferences**.
- No owner activation/license code nor startup PIN is required for this exact
  sandbox package. A debuggable flag **and** exact package name are required to
  take this isolated path; no bypass exists for production identity.
- Its Android cloud backup is disabled to avoid copying unprotected test data.
- Different sound provider authority permits installation beside the original.

## Functionality
Same account/ledger/debt/budget/categorisation/undo/search/widgets/reminder
and optional notification capture code; only the sandbox startup activation and
app-entry PIN gates are excluded. This is a full finance-app trial, **not**
a static mock. All money/DB code and original protected security sources remain
unchanged. Visual glass renderer and four added themes come from the functional
base's pilot work.

## Encryption safety exception
The current SQLite storage is not SQLCipher-encrypted. The exported backup files
remain password AES-GCM encrypted, and payment USSD PINs remain Android Keystore
protected; purposely removing these protections would expose sensitive data and
break restore compatibility. Do not use any real finance secrets or enable SMS
or notification reading while experimenting with an unprotected sandbox.

## Build
`gradle :app:assembleSandbox` produces `app/build/outputs/apk/sandbox/app-sandbox.apk`.
This is a Debug-signing-key test package, not original owner's signed release.
CI runners may rotate test signing keys; a later update can require uninstalling
**only the sandbox**, losing sandbox records. Never uninstall/replace the original.

## Verified neutral optical-glass correction — 4 November 2026

Latest tested functional/UI commit: `f4f5177b459d42e6db53fc2eb91bde2c340e6d55` on `design/wen-full-unlocked-sandbox-20261101`, PR #28 Draft. The original financial application, production signature, protected code, SQLite and business logic were not modified by this visual/gesture batch.

**Corrections made:**
- The native material is now composited **above** navigation labels/icons and samples the actual opaque tab strip; the older scroll-content sample exposed page-text ghosts and a dark/black capsule. The native lens FrameLayout now returns `false` from `dispatchTouchEvent`, letting the actual underlying tab TextViews own clicks and Hold+Drag.
- Removed spectrum/pink/orange/blue chromatic rims, RGB-split GPU shaders and native chromatic aberration on both navigation and the FAB (including Canvas fallback). Light reflections are neutral, white/silver, while the lens remains optically active.
- Floating Add (+) material uses circular clipping, lighter blur/frost, with a non-clickable optical surface above the glyph. Its original Add Transaction click and accessibility label remain unchanged.
- Real emulator smoke first failed Hold+Drag until the raised optical View was made touch-through, then **passed** with event evidence: `DOWN tab=0`, `HOVER tab=1,2,3`, `UP_TARGET tab=3 from=0`, and a successful Analytics selection. Added anti-regression contracts and gesture traces.
- CI for this commit: sandbox APK build+lint `41786778149` PASS, emulator held-frame/gesture/FAB smoke `41786778123` PASS, source/finance/security `41786778254` PASS, Android debug compile `41786777977` PASS, unsigned original-package compile `41786778109` PASS.
- Sandbox APK: `app-sandbox.apk`, 13,114,737 bytes, SHA-256 `6de3e44ecf6aa49aa9aa3cdeff6308427f85e8da30128cc26832210fb51b683f`, artifact ID `13300858836`, ARM64/ARM32 native library variants, Android APK signing block present. Actual emulator screenshots, log and gesture dumps: artifact ID `13301597817`. The benchmark screenshots show the neutral lens visible during press, legible navigation, circular FAB, and Analytics after native hold-drag.

**Acceptance boundary:** Android emulator visual/interaction regression now passes, but actual Xiaomi Pad 5/phone visual acceptance, 60/120Hz jank/battery, TalkBack and multi-theme physical-device verification are not established. Do not call this an original-package signed release, claim perfect Apple parity or merge sandbox protection exceptions into production. This is a separate debug-key-signed sandbox install for fictional data; its signature may change across CI runs.

## Verification gates
1. Compile, lint, source-safe-zone and security SHA-256 contracts.
2. Emulator launch and smoke check of first-run account/UI.
3. Real tablet/phone tests for financial actions on fictional transactions.
4. Theme screenshot compare, scrolling P95/P99 and actual battery/thermal testing.
100% visual parity is not verified by a successful APK build.
