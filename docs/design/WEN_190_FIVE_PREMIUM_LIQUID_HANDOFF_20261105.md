# وين صرفت — Visual-only premium Liquid Glass handoff

Date: 2026-11-05. Source baseline: main at `283112f4bedaf2e07dd7370e581f84e14959710e`, already containing verified v1.9/code27 Royal Gold and existing Liquid navigation work.

## Mandatory scope
**Do not build, merge, sign, install, or change the existing functionality in this visual design session.**
Leave the protected finance calculations, accounts, debts, transactions, database, backups, authentication, encryption, SMS, permissions, package/signing config, state, navigation destinations, and financial Safe Zone completely unchanged.
Do not use the friend-only Arabic SMS PR #34 or the unlocked sandbox bypass as a base. Never edit/recreate account logos (Bank of Palestine, Jawwal Pay, PalPay).

## Implemented — new selectable IDs only
The original palette entries **0–24 remain in exactly the same order and with the same values**. New append-only choices:

- 25: Royal Dark Gold — dark obsidian / luminous champagne, refined gold edges
- 26: Imperial Dark Gold — graphite / silver-gold / pale bronze
- 27: Champagne Gold / Light Royal Gold — cream, soft ivory, light champagne
- 28: Soft Orange-Green Glass — quiet peach + gentle mint glass
- 29: Purple-Pink Liquid Glass — subtle lavender / fuchsia-pink optical sheen

Only existing visual hooks are used: header, card, balance tile, chart panel, account card, bottom navigation, FAB, and screen backdrop. New `WenPremiumLiquidDrawable` supplies cached multistop shaders, two radial blooms, an elongated soft reflection and two rim strokes. It is passive and low-power aware; drawing adds no idle clock, background capture, permissions, or business handlers.

The four original navigation buttons and callbacks are reused unchanged. A **neutral selected glass lens** replaces the coloured backing for the five new themes, with readable text contrast. Do not replace account icons. No change to FAB click target, size preference, position preference, click handler, or scroll behaviour.

## Changed source boundaries (strict allowlist)
- `glass-engine/src/main/java/app/wensarafet/glass/WenPremiumLiquidDrawable.java`: NEW rendering-only class.
- `app/src/main/java/app/wensarafet/tracker/Ui.java`: append five palette names/rows; dispatch the existing visual factories to new material for indices 25–29; preserve indices 0–24.
- `app/src/main/java/app/wensarafet/tracker/NavigationVisualDecorator.java`: five new glass-plate and text-tint treatments; no callback changes.
- `app/src/androidTest/java/app/wensarafet/tracker/ReferenceThemesInstrumentedTest.java`: visual selection and pixel assertions only.
- `tests/batch22_category_budget_icon_contract.py`: update append-only theme count assertions (22 legacy IDs are frozen).
- `tests/wen_188_visual_safezone_contract.py`: preserve the full historic Safe Zone gate; additionally freeze all first 25 palette indices against main baseline; allowlist only the newly added visual renderer and this handoff.
- This handoff document.

**Untouched:** `MainActivity.java`, `ExpenseChartView.java`, `NavigationLiquidMaterial.java`, `GlassMotion.java`, finance/debt/DB/security/SMS trees, bank icons and assets, Gradle, Manifest, keystore, package, version, release workflows.

## For the other conversation — integration only after independent review
1. Fetch this branch and compare against exact baseline `283112f4`; reject **any unexpected filename** before merge.
2. Run source-only Safe Zone contracts and inspect all 25 previously saved themes. Existing selection persists via the same `appearance/theme` integer key.
3. Only when that conversation is authorised to do so: run its CI/build and visual QA on actual tablet/phone, checking readability in Arabic/RTL, all five previews, glass selected tab with neutral bubble, fab and chart. **This design session requested no APK build.**
4. If checks pass, merge approved visual changes into the chosen v1.9 cumulative app source; do not mix the friend-only PR #34 or sandbox-bypass branches.
5. Building is not a signed update: any installable in-place APK still needs the original private signing certificate, independent tests, owner approval, and a safe backup/clone.

## QA caveat
Code is prepared for integration. No APK was built, no emulator/device visual screenshots were produced, and runtime acceptance must not be claimed before checks actually run. This file is a concise handoff, not evidence of a signed release.
