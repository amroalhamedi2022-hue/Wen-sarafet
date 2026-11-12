# WEN SARAFET — SANDBOX 2.2.4 LIVE GLASS + GOLD PERFORMANCE

**Owner complaint (2026-11-13):** Frozen FAB background snapshot looks extremely blurred (5dp), is not live on scroll, and premium gold themes stutter. Preserve all bank/account/financial behavior and every visual style while reducing render cost. Do not touch the phone launcher.

## Changes (exact scope)
1. `glass-engine/.../WenFabBubbleDrawable.java`: new 2-pass glass (uniform 15% opacity light / 17% dark; thin anti-aliased edge). **No screenshots, no internal reflection, no colored image, no heavy Gaussian.** The actual cards/background remain directly visible and animate under it. Edge softness approximates 1dp via antialiasing. This is *not* falsely claimed to be a true GPU backdrop blur.
2. `MainActivity.java`: deleted the frozen 5dp RenderEffect snapshot View. Same original FAB placement, '+' glyph, click target, press animation, show/hide timings, and action; zero extra controller calls on scroll. This is purely a presentation path change.
3. `WenFabBackdropBlur.java`: deleted unused snapshot class entirely; no account pixels are cached.
4. `WenGoldGlassSurface.java`: golden card background composed from original existing cached drawable + same Specular in a `LayerDrawable` rather than creating an extra FrameLayout and View for EACH passive card. Original text layout/visual layers unchanged, click listeners carried on the original content view.
5. `WenPremiumLiquidDrawable.java`: for gold theme indices 25/26/27 and CARD surfaces only, render the exact original gradient/path material to a **bounded 2.2 MB ARGB cache**, reuse one texture on subsequent redraws, instead of running all radial shader and path passes repeatedly. Caches refresh on size, alpha and filter changes. Fullscreen backdrops, headers and navigation lenses are deliberately left as before.
6. `app/build.gradle`: Sandbox versionCode 34 / versionName 2.2.4-sandbox, package `app.wensarafet.tracker.sandbox` and original offline permissions unchanged.
7. `tests/wen_widget_fab_gold_optics_contract.py`: explicit regression constraints for FAB, widgets, protected callbacks and gold cache.
8. `tests/wen_200_strict_offline_contract.py`: corrects obsolete exact version 2.1 assertion to accept increasing versions **without** changing offline, SMS, financial or security safety checks.

## What NOT to merge into production
All sandbox-only debug signing/activation/demo data, version/app ID/manifest changes. The production visual handoff must contain ONLY the 4 presentation files in steps 1,2,4,5 and deletion in step 3. Production golden theme performance should be validated with actual frame times on target device.
## Signing
This is a test-key-signed Sandbox, not owner production. Existing persistent Sandbox keystore in private Library `/وين صرفت/Sandbox Signing/wen-sandbox-test-only.jks`. Verify with apksigner that SHA256 matches previous 2.2.3 certificate. Never put JKS/password in GitHub or an APK/ZIP.
## Visual/Performance QA gates
- build debug; Android Lint; offline and finance source tests.
- screenshot actual Android 13 and 15 widget + main FAB, on both static and scrolled frames.
- gold theme tests exercise main screen (Champagne Light/Imperial Dark/Royal Dark) and compare frame metrics when emulator permits.
- absolute visual parity cannot be claimed without target-device tests, and no Google launcher screenshots should be described as proof of a gold theme.
## Safe portable changes
Portable PR is a new visual-only branch based on PR #57's current visual output, explicitly excluding sandbox config/testing.
