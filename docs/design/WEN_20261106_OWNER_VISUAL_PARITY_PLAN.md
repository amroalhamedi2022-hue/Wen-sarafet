# Wen Sarafet — Owner-approved visual parity implementation plan
Date: 2026-11-06. Scope: **visual-only, no finance modifications, no merge/signing without owner's screenshot approval**.

## Inputs and authoritative comparison
- Five reference screenshots from owner: Royal Dark Gold, Imperial Dark Gold, Champagne Gold, Soft Orange-Green, Purple-Pink.
- Five real screenshots from application show flat/smudged gradients, pale pastel colours, insufficient caustics/gloss, unreadable white-on-gold amounts, navigation double pill/late swipe selection, rectangular FAB press shine, overflowing content.
- Latest reviewed source baseline: `release/wen-2.0-premium-original-signer-20261105@caf2f6ee`, ahead of main by 35 commits. **Do not reset to old 1.8.8/1.9 branches.**
- Reference screenshots are VISUAL benchmarks; financial figures, dates, and bank logos are not to be copied from images.

## Non-negotiable safety
1. No edits to ledger, balance, transfers, split amount math, debts, security, SMS, SQLite, backups, activation, signing, package ID or bank/Jawwal Pay/PalPay icon resources.
2. Four existing navigation callbacks and original FAB action must remain byte-identical. No duplicate callbacks, synthetic financial events or navigation changes.
3. Preserve persisted theme indices 0–29; visual catalogue hides obsolete presets rather than renumbering or deleting preference keys. Indices 22–24 (the redundant post-Frozen-Pearl legacy additions) omitted by default; indices 25–29 are the five approved new themes.
4. Work only here until screenshot approval. No signed APK, main merge, or end-user installation.

## Implementation sequence
### A. Theme catalogue
- Short English display names for ALL visible themes, stable internal IDs and palettes.
- Hide/delete action from theme settings (visual-only SharedPreferences), cannot delete the selected theme; restore hidden themes action. Older add-ons 22–24 hidden by default.
- Verify selected theme preserved across process restart/rotation; no forced change to installed users' prior saved theme preference.

### B. Liquid optics / reference parity
- Replace near-opaque, low-contrast 1-stop gradients of premium cards with coherent multi-stop metallic/high-frequency specular and transparent glass layers. Premium card materials must retain legible text.
- Rebuild dark gold as 2 distinctive light scenes; Champagne champagne/ivory metallic with DARK ink; Orange-Green saturated luminous green-peach layered caustic; Pink-Violet high-chroma fuchsia/violet with translucent depth.
- Distinct background patterns, specular rims, depth and glass highlights; optical refraction uses GPU AGSL only where supported and safe software fallback, no idle ticker/per-row texture allocation.
- Preserve original account icons and numerical data source; avoid masking or colour-correcting logos.

### C. Navigation
- One physically selected pill, one real 4-tab row. Selected position driven from ACTIVE selected tab for click AND horizontal screen swipe, tracked across previous/new view hierarchy.
- Prevent old screen's nav bar from remaining in overlay during transition; right-to-left tab order and gesture source exclusions remain unchanged.
- Avoid overlay recolouring pill, duplicate text, and nav overlap; guard 4 unique destinations and 1 click per gesture.

### D. FAB
- Clip foreground press highlight to true circle; prevent square hit/ripple outline; finite pressure morph with smooth tap, long hold and release.
- Keep existing onClick, touch hitbox, on-scroll reveal and FAB position. No long-hold action addition without approval.
- Add detached/reduced-motion clean-up and Android 13 / 15 tests.

### E. Layout/readability and runtime screenshot acceptance
- Fix white-on-gold stats and header; maintain Arabic/right-to-left correct layout, bank icon geometry and long account names; nav bottom inset and FAB overlap.
- Screenshot **real emulator-rendered application UI** for all five themes (Android API33 and 35) using a purpose-made instrumented synthetic visual fixture, no personal data/DB, plus runtime screenshots of full actual app after safe synthetic login where feasible.
- Save PNGs as downloadable GitHub Actions artifacts, create a contact sheet with reference-vs-runtime alignment, inspect structural/pixel differences manually and with perceptual metrics; rerun until approved.
- Performance: 60fps target where viable, no excessive loops; reduced-motion/battery saver fallback.

## Release gate
Visual source guard, core unit/finance guards, clean compile and Android API 33+35 tests all pass, **plus** owner approves actual emulator PNGs for all five themes. Only afterwards merge to main, inspect original signer certificate SHA-256 fingerprint, sign one normal final APK with original private keystore, verify in-place upgrade with preserved data. Never upload key or password to GitHub.

## Verified implementation log — 2026-11-06
- PR #45 on `design/wen-visual-parity-owner-review-20261106` targets only the 2.0 original-signer release candidate; NEVER main/signing.
- Added short English preset labels, reversible hide/restore, retired presets 22–24, preserved actively selected older preset, preserved five approved 25–29. Current settings preview uses production glass shader.
- Reworked `WenPremiumLiquidDrawable` to use cached radial caustics, metallic header/backdrop wave reflections, translucent material palettes, and separate dark/gold/champagne/emerald/violet scenes. First Android 15 debug-only fixture visual pass revealed excessively bright backgrounds, unwanted card ribbons and one blank Imperial frame; second-pass corrections reduce ambient bloom, keep caustics on background/header, and enforce nonblank retry.
- Corrected Champagne/peach text readability and green/violet dark-scene text palette.
- Circular FAB touch foreground prevents square glows; new finite `GlassMotion.slideLens` parent invalidation keeps optical chrome updated during screen swipe or tab tap. Actual gesture interaction still needs Android instrumentation.
- **Gates checked**: latest screenshot workflow source Safe Zone gate passed; prior (pre-second-pass) Android build and Lint passed; screenshots of debug-only materials generated and inspected (NOT real authenticated MainActivity and NOT owner approved).
- **Pending**: second-pass screenshot inspection, swipe-motion instrumentation, genuine MainActivity screenshots with synthetic app state, visual refinements and owner's screenshot approval. No signed APK or main merge.
- Screenshot workflow (Android 15): `https://github.com/amroalhamedi2022-hue/Wen-sarafet/actions/workflows/wen-visual-screenshots.yml` — stores 5 PNGs, no user installation.
- Do not replace this log with a generic progress claim; check the latest run/job conclusions and artifacts.

## Latest emulator evidence — 2026-11-06
- Android 15 screenshot workflow **SUCCESS** at `https://github.com/amroalhamedi2022-hue/Wen-sarafet/actions/runs/42186651170`; artifact `wen-owner-visual-preview-android15` includes 5 nonblank 1080x2400 PNGs (themes 25–29). The screenshot script rejects black transition captures.
- Source-only financial Safe Zone gate also passed on latest visual code commit `c33504b`.
- These images are **debug-only VisualPreviewActivity**, using real production material classes and synthetic amounts. They are NOT a real authenticated MainActivity screenshot, NOT signer release QA, and **NOT visually accepted**.
- Pixel inspection: dark gold materials still too brown and insufficiently glossy versus reference; Orange-Green is olive rather than vivid amber/forest gradients; Pink-Violet glass still lacks rich blue/pink translucency; navigation capsule feels flatter than accepted reference. Further visual code and real app screenshot automation are required before owner approval.
- No production build signed, no main merge, no user-device install.

## Owner's 2026-11-06 visual refinement request — implementation audit
- Recovered actual archived 1.8.8 Sandbox emulator screenshot artifact from run `41411629334`; the reference uses a restrained double-rim translucent blue capsule, native circular optical FAB, real original account badges.
- Ported the Sandbox contextual header titles/subtitles, pageHeader tag and press effect, and true rounded FAB outline to the current 2.0 candidate WITHOUT changing the original click callbacks or account/finance logic. The five premiums now render their FAB using original `WenFabBubbleDrawable`, with circular clipped foreground and press/release optics.
- Added `WenThemeArtDirection.java`: five independent cached radial/specular compositions rather than one recoloured shader. Removed synthetic ribbon/wavy background strokes from `WenPremiumLiquidDrawable`.
- Confirmed original asset blob SHAs and pinned them in `tests/wen_188_visual_safezone_contract.py`: BOP `d71006e0`, Jawwal `0516bd77`, PalPay `d09932ad`. The original PNGs and accountIcon mapping have not been edited.
- Source-only finance/safety gate, original package/no-signing guard and Android debug build/lint have passed in review branch (runs `42194920267`, `42194920164`, `42194920228`). Do NOT confuse these with a visually approved final release.
- Owner requires ACTUAL MainActivity screenshot evidence rather than `VisualPreviewActivity`. Two separate screenshot techniques were implemented:
  1. `RealMainScreenCaptureTest` uses Android PixelCopy on the genuine Activity window in an isolated debug test instance, with fake data and original seeded bank accounts. GitHub emulator has experienced ADB disconnection/runner GPU failures; not yet approved.
  2. `.github/workflows/wen-main-physical-screens.yml` boots a disposable debug build of REAL MainActivity with test-only activation allowances in the CI checkout, NEVER committed into the production code. The build is now performed BEFORE emulator boot with KVM, SwiftShader, x86-only native exclusions and Pixel Launcher ANR workaround, matching verified 1.8.8 Sandbox test setup. Expected 20 framebuffer PNGs: 5 themes x home, account icons, transactions and analytics; attempts without 20 valid images fail.
- **Still blocked**: actual new MainActivity PNG validation and user visual approval; no claims of perfect visual parity, no signed APK, no merge to main, no user-device install.
- Branch: `design/wen-visual-parity-owner-review-20261106`. Review: PR #45.

## Work progress
- [x] Compared source and received 5 reference + 5 runtime screenshots.
- [x] Verified latest 2.0 release candidate baseline and existing 30 theme indices.
- [x] Created dedicated owner-review branch.
- [ ] Theme management and names functional code + passing tests.
- [ ] FAB circular press + nav syncing code + passing tests.
- [ ] Visual matching for each theme.
- [ ] Actual emulator screenshot artifacts and visual inspection.
- [ ] Owner visual approval.
- [ ] Final original-key signed release (approval gated).
