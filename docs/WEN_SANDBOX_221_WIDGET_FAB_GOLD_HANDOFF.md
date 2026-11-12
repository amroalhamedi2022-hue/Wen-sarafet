# WEN SARAFET — SANDBOX 2.2.1 VISUAL PATCH & PORTING RECORD

**Source:** `sandbox/dark-crystal-v22-widget-rebuild-20261111` (v2.2-equivalent app UI).
**Destination:** `sandbox/wen-22-widgets-fab-gold-performance-20261112`.
**Scope:** presentation/performance only; financial DB, transaction math, authentication in production, and navigation callbacks untouched.

## Owner requests and implementation

1. **Quick-action home widgets:** `widget_quick_action.xml` image becomes centerCrop with no image padding, allowing larger artwork in the same fixed tile footprint; the original `widget_choose_contact` click target and text IDs are retained.
2. **Double and triple balance widgets:** images use 16% paint scaling rather than adding height to RemoteViews (avoids text clipping in short launcher cells). Alpha adjusted from .87 to .94 to increase logo clarity. Logos and balance providers remain original.
3. **Glass edge glow:** modestly brighter edge-vector colors in five `widget_crystal_*.xml` drawables. No wallpaper capture, network call, or new assets.
4. **FAB:** `WenFabBubbleDrawable.java` no longer paints the top-left RadialGradient/inner caustic. Exactly one uniform translucent circle + one perimeter stroke; existing press listener and hit target remain.
5. **Gold performance:** `WenGoldGlassSurface.wrap` previously mounted 1 live native `LiquidGlassView` per card: accounts, three stats, and chart. The new version uses each existing cached background `Drawable` and one inexpensive specular edge: **zero live backdrop captures per passive card**. `WenPremiumLiquidDrawable` does not add a fullscreen RuntimeShader sunlight pass on gold themes 25,26,27; the cached gold gradients, caustics, glass edges and main navigation lens remain.
6. **Sandbox version:** `app/build.gradle` debug build is `2.2.1-sandbox`, versionCode 31; separate package `app.wensarafet.tracker.sandbox`.

## Do not accidentally transfer
- Sandbox-only: `app/build.gradle`, debug manifest and activities, debug activation/identity overrides, sample balances.
- Only port presentation files explicitly reviewed; never cherry-pick complete sandbox branch onto production.
- Do not merge or modify finance, database, security, SMS, offline boundaries, navigation or account icon assets.

## Signing and upgrade limitation
Android requires a valid APK signature even for testing. The previous sandbox APK is signed with SHA-256 certificate `5CF424B600A743B9B3C9C0DFD88001DE68BB43009D416E27CFA311C5009F54F4`.
**The original Sandbox debug private key has not been recovered from prior CI; an APK cannot be updated in place using a different key.** A future APK must be signed with that private key to upgrade in place. A new *persistent sandbox-only* key can ensure **future** updates, but cannot match the already installed APK. Never expose or use the owner production signing key for sandbox.

## Evidence gates
- Dedicated source contract `tests/wen_widget_fab_gold_optics_contract.py`.
- Android debug build+Lint.
- Actual RemoteViews screenshot on Android API 33 and 35.
- Emulator UI screenshot(s) for main app FAB/gold after authentication, if possible.
- Final APK package/version/certificate + ZIP hash/size verified before providing a link.
- Release acceptance still requires owner eyeballing live widget and scroll performance.
