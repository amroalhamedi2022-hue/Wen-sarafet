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
