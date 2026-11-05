# وين صرفت — v1.9 — Prism Glow & Golden Bloom

Date: 5 November 2026. Base: verified v1.8.10 (code 26) at `af0e69a4bf03fb59ecbf73d24a15eac4917a997d`.

## References from the owner

Reference A: magenta/fuchsia/violet ambient scene, vivid translucent floating capsule navigation, fine luminous edges and a compact control above the bar.

Reference B: diffuse peach/orange floral-like ambient light and a strong circular translucent golden add button, a subtle white border and soft highlight.

These are **stylistic references**, not screenshots blindly embedded as app backgrounds. Text, charts, balances, notifications, and accessibility labels must remain readable, with the existing 4 original Arabic tab destinations.

## Implementation

- **Prism Glow (index 22)**: fuchsia/violet blue ambient screen, bright magenta-purple header, translucent deep-violet capsule navigation, light labels, pink/orchid highlights, glass action button.
- **Golden Bloom (index 23)**: warm peach/sunset/soft-mint ambient backdrop, copper-to-peach header, frosted cream navigation, warm ink labels, circular amber optical FAB with white '+' and clean luminous rim.
- Both themes are **append-only**. Indices 0–21, all current preference keys, theme order and palettes remain unchanged. Old themes continue with the old drawables.
- Passive shader background only draws when Android redraws or bounds change; no bitmap backdrop snapshots, timers or endless animation. Low-RAM / power-saving fallback.
- Existing native MIT Liquid refraction `NavigationLiquidMaterial` and `GlassMotion` remain **untouched**: hold/drag through real tabs, optical distortions above glyphs, .60 viscosity, 265ms recovery, touch-through lens, and Android API compatibility.
- The original FAB callback `showAddTransaction()`, preferences for size/position, on-scroll hide/show and press animation are unchanged; only its draw surface changes under theme 22 or 23.
- App version is **1.9 (versionCode 27)**. The release package is still `app.wensarafet.tracker` and must be signed privately with the original certificate.

## Hard protection gates

1. `tests/wen_188_visual_safezone_contract.py`: verify original 22 theme names + palettes unchanged, original financial/DB/security source untouched and MainActivity exact baseline after removing approved visual hook.
2. Real Android drawing and preferences tests on API 33/35 in `ReferenceThemesInstrumentedTest.java`. Existing 10 tests cover SQLite (synthetic debug data), split remainder, and touch navigation.
3. Original Java/SQLite financial source tests, release compile, lint and exact permissions test.
4. Owner real tablet visual approval with each of the two themes: Arabic glyph legibility, RTL nav, drag cancel, single callback, plus control, light/dark, scroll hide/show, screen rotation, reduced motion / battery saver.
5. **No installable update without original private signing key**. Unsigned source artifact or a newly signed certificate would be rejected by Android when upgrading 1.8.8 and could risk app state. Preserve installed app and user data.

## Release acceptance

Prepare unsigned original-package 1.9/code27 APK, verify with aapt/aapt2 and zipalign. Sign it locally with the original owner's keystore only, verify SHA256 signer `6B8268E5B647B12A2AA055D7A22F7B8EA888E40FB863E74409938AAD4EC789C9`, run `scripts/verify_upgrade_apk.sh` against the trusted signed 1.8.8, compare signed/unsigned payloads and install over a **backed-up test clone** before using the owner's installed device. Never upload private secrets to GitHub.
