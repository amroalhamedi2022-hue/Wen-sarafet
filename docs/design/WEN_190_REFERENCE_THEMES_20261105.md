# وين صرفت — v1.9 — Prism Glow, Golden Bloom & Royal Gold

Date: 5 November 2026. Base: verified v1.8.10 (code 26) at `af0e69a4bf03fb59ecbf73d24a15eac4917a997d`.

## References from the owner

Reference A: magenta/fuchsia/violet ambient scene, vivid translucent floating capsule navigation, fine luminous edges and a compact control above the bar.

Reference B: diffuse peach/orange floral-like ambient light and a strong circular translucent golden add button, a subtle white border and soft highlight.

These are **stylistic references**, not screenshots blindly embedded as app backgrounds. Text, charts, balances, notifications, and accessibility labels must remain readable, with the existing 4 original Arabic tab destinations.

## Implementation

- **Prism Glow (index 22)**: fuchsia/violet blue ambient screen, bright magenta-purple header, translucent deep-violet capsule navigation, light labels, pink/orchid highlights, glass action button.
- **Golden Bloom (index 23)**: warm peach/sunset/soft-mint ambient backdrop, copper-to-peach header, frosted cream navigation, warm ink labels, circular amber optical FAB with white '+' and clean luminous rim.
- **Royal Gold • الذهب الملكي (index 24)**: premium deep-obsidian atmosphere, five-stop polished champagne-metallic reflections, smoked-glass navigation with fine gold borders and warm legible labels, faceted luminous circular gold FAB. Royal Gold is deliberately dark in system light and dark modes for consistent contrast; all other themes keep their prior mode semantics.
- All three themes are **append-only**. Indices 0–21, all current preference keys, theme order and palettes remain unchanged. Old themes continue with the old drawables.
- Passive shader background only draws when Android redraws or bounds change; no bitmap backdrop snapshots, timers or endless animation. Low-RAM / power-saving fallback.
- Existing native MIT Liquid refraction `NavigationLiquidMaterial` and `GlassMotion` remain **untouched**: hold/drag through real tabs, optical distortions above glyphs, .60 viscosity, 265ms recovery, touch-through lens, and Android API compatibility.
- The original FAB callback `showAddTransaction()`, preferences for size/position, on-scroll hide/show and press animation are unchanged; only its draw surface changes under theme 22, 23 or 24.
- App version is **1.9 (versionCode 27)**. The release package is still `app.wensarafet.tracker` and must be signed privately with the original certificate.

## Hard protection gates

1. `tests/wen_188_visual_safezone_contract.py`: verify original 22 theme names + palettes unchanged, Prism/Bloom frozen at 22–23 and Royal Gold appended at 24, original financial/DB/security source untouched and MainActivity exact baseline after removing approved visual hook.
2. Real Android drawing and preferences tests on API 33/35, including Royal Gold metallic pixels and circular FAB in `ReferenceThemesInstrumentedTest.java`. Existing 10 tests cover SQLite (synthetic debug data), split remainder, and touch navigation.
3. Original Java/SQLite financial source tests, release compile, lint and exact permissions test.
4. Owner real tablet visual approval with each of the three themes: Arabic glyph legibility, RTL nav, drag cancel, single callback, plus control, light/dark, scroll hide/show, screen rotation, reduced motion / battery saver.
5. **No installable update without original private signing key**. Unsigned source artifact or a newly signed certificate would be rejected by Android when upgrading 1.8.8 and could risk app state. Preserve installed app and user data.

## Release acceptance

Prepare unsigned original-package 1.9/code27 APK, verify with aapt/aapt2 and zipalign. Sign it locally with the original owner's keystore only, verify SHA256 signer `6B8268E5B647B12A2AA055D7A22F7B8EA888E40FB863E74409938AAD4EC789C9`, run `scripts/verify_upgrade_apk.sh` against the trusted signed 1.8.8, compare signed/unsigned payloads and install over a **backed-up test clone** before using the owner's installed device. Never upload private secrets to GitHub.


## 1.8.8-sandbox visual parity staging — NO BUILD

**Status:** source-only staging branch `design/wen-190-sandbox-visual-parity-20261105`, based on signed 1.9's `main@283112f4`. Do **not** merge or run Android Gradle / Actions / APK signing until the owner provides additional theme designs and explicitly requests final assembly. The owner's installed v1.9 APK remains unchanged.

### Reference identity and safe comparisons

The Library's `WenSarafet_LiquidNav_Test_20261104.apk` is an actual **1.8.8-sandbox** variant, matching the supplied App Info screenshot and the separate package `app.wensarafet.tracker.sandbox`. SHA-256: `d96b1cd8c9c4efa9737ebeac0fc3b78afac633ce5c3fe3626a5408ad14f74c10`. Original sandbox source review: `design/wen-full-unlocked-sandbox-20261101@b57dcd3c` (its base build metadata predates the 1.8.8-sandbox APK, so source review does not establish pixel-exact binary equivalence).

- **Retained verbatim:** `WenGlassBackdropDrawable`, `WenLiquidRuntimeOptics`, `WenNavigationLensDrawable`, `WenNavigationTrack`, `WenOpticalNavigationOverlay` and design token file match the sandbox Git blob SHAs exactly. The latest `GlassMotion` is a superset of the sandbox engine: it adds tap eligibility to stop accidental navigation on drag-and-return and retains the native lens, viscous hold, physics and release.
- **Previously present in 1.9:** the original 22 theme indices (0–21) and three new selectable themes: Prism Glow 22, Golden Bloom 23, Royal Gold 24. They are not renumbered or recoloured.
- **Now staged in existing drawing code:** copy sandbox `WenGlassDrawable` exactly: more transparent frosted cards, fine pearlescent dispersion rims/crown and no unrelated animated floating orb.
- **Now staged in a new visual-only asset:** sandbox `WenFabBubbleDrawable` exactly, wired through `Ui.glassAccentBackground(context,24)` **only for existing Liquid indices 16–21**, leaving 0–15 and all three 1.9 reference FAB materials unchanged. Finite 155/245ms bubble pressure morph consumes *no* click or long-click events and resets after view detachment.
- **Now staged in Ui-only animation helpers:** the sandbox `GlassMotion.bindPress` optical bubble for all other Liquid pressable views; limited 2-item 125ms content entrance and restrained 115ms screen arrival. Legacy non-Liquid behaviours are preserved word-for-word.
- **View-host integration now staged:** `MainActivity` has one manually reviewed, pinned visual-only hook set for the sandbox's original-tab swipe/circular reveal, FAB-origin position and icon metadata. Its original four nav click targets and `showAddTransaction()` destination remain exact. `QuickActionActivity`, `FinanceDbHelper`, `FinanceRules`, debt ledgers, budget, encryption, PIN, lock, backup, SMS, SQLite schema, application ID, permissions and signing remain untouched relative to verified v1.9 source.

### Strict no-functional-changes proof, no APK build

`tests/wen_188_visual_safezone_contract.py` maintains the original 1.8.8 financial code and callback byte-for-byte baseline. For this staging pass it also pins the complete verified 1.9 `Ui.java` source outside **four** specific appearance-only methods (`glassAccentBackground`, `animateScreen`, `animateChildren`, `pressAnimation`) and requires the two original sandbox graphics classes **byte-for-byte** from trusted sandbox commit `b57dcd3c`. `tests/glass_energy_contract.py` verifies the new pearl rims, lack of idle loops, exact bubble material, finite entrance and non-consuming touch listener. Neither test is disabled.

### Host-level sandbox transfer — staged, with protected callbacks frozen

**Added to the staging branch, not to main:**
- `WenScreenVisualMotion.java` is a separate view-local animation class: FAB-origin **360ms circular reveal**, reverse closing reveal, single 190ms horizontal primary tab transition, first-launch icon choreography and 240ms top-bar reveal. It has no finance/DB/auth import, no persistent timer and no new tab destinations.
- Real horizontal swipe is recognised only on **unlocked primary screens using Liquid themes**. It rejects swipe attempts starting inside `EditText`, `SeekBar`, `HorizontalScrollView`, any horizontally scrollable child, and clickable/long-clickable controls. A valid drag cancels the child once and invokes the original tab `TextView.performClick()`; it never calls a finance or navigation method directly. RTL is left intact.
- `MainActivity.java`: only view-bound routing hooks, press origin, icon tags and the pre-existing original FAB action wrapped with a visual origin tracker. **The complete reviewed MainActivity blob is pinned** as SHA `bf6163c8464c8cb99189e962ee35fdbeb1c5f8ae`. The old version 1.9 remains the baseline for all business callbacks, including the 1.8.8 immutable Safe Zone.
- `WenFabOpticalCrownDrawable.java` draws the sandbox-style pearly, press-reactive *over-glyph optical sheen* over the existing “+” text using `ViewOverlay`. It receives **no touch at all**, creates no child or second button, follows the original FAB automatically during scroll hide/show, and uses no per-frame object allocation. The original sandbox bubble underneath and its bounded finite press morph are preserved. Prism/Bloom/Royal retain their own backgrounds.

**Deliberately not copied wholesale:** `SandboxOpticalGlass.attachFab`'s additional native AGSL/refraction host: it replaces the FAB background and touch listener and requires moving/reparenting the original button. Doing that without device QA or a build would risk changing the add button hit target, scroll hide/show and the Safe Zone. The compatible over-glyph caustic is transferred instead; pixel-perfect native FAB backdrop parity is therefore **not yet verified**. This must not be silently marked as complete.

**Regression checks added (not yet executed on emulators because owner deferred builds):**
- `tests/wen_188_visual_safezone_contract.py`: precise source SHA pin for MainActivity + screen motion and original 1.8.8 financial logic; no skipped guard.
- `tests/wen_190_visual_motion_contract.py`: source-only invariants for FAB-origin reveals, original tab dispatch, protected interactive controls, non-consuming crown and app identity.
- `app/src/androidTest/java/app/wensarafet/tracker/WenVisualMotionInstrumentedTest.java`: upcoming on-device crown pixel+press tests, original FAB click, disabled gesture on protected screens, `EditText` exclusion and single child CANCEL on valid swipe.
- Existing 14/14 per API 33 and 35 tests **belong to the previous 1.9 release**. No new Android runtime tests have been executed on these staged source changes, so no claim of new passes.

### Future combined v2.0 release gate — NOT TODAY

- Owner plans **4 or 5 additional new themes**, append-only at indices 25–28 or 25–29. Preserve exact indices, colours, account logos and icon semantics for all existing 25 themes. Keep the same native Liquid navigation, no financial, debt, SQLite, security, SMS or signing edits.
- When all designs are provided: run the strict source tests, original `scripts/test_core.sh`, Android lint + release compilation, API33/API35 instrumentation including the new host/crown cases and one test-clone in-place update; fix failures without bypassing safety gates.
- Only then set version **2.0**, versionCode **greater than 27** (use 28 unless release history requires a higher code), package `app.wensarafet.tracker`, and produce ONE signed release with the **verified original owner keystore held privately in Google Drive**. Check certificate SHA256 `6B8268E5B647B12A2AA055D7A22F7B8EA888E40FB863E74409938AAD4EC789C9`, v2/v3 signatures and backup-preserving upgrade against the owner's current signed app. Do not commit passwords or keystores to GitHub.
- Do not create a debug APK, unsigned build or production release before the additional theme work. Keep this staging branch separate from `main` until complete.
