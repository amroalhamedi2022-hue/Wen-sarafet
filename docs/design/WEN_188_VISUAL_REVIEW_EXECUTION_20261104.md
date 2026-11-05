# Wen Sarafet 1.8.8 — visual-only review branch (4 November 2026)

## Base and scope
- Review branch: `design/wen-188-visual-only-review-20261104`, created from the exact merged and signed 1.8.8 source commit `a78ce29603daf3f64efb52f15aca17e17c38a8b9`.
- Signed reference APK in the user's Library: `Wen_Sarafet_1.8.8_Debt_Audit_Signed_Update.apk`, original applicationId `app.wensarafet.tracker`, version 1.8.8, code 24. SHA-256 `ab6c39b54922781fe0af858b312df560a8d1bcf717a957ea3ef3c35df7c1781d`.
- All finances (including 1.8.8 debt reclassification, direct debt repayment, SQLite v15 audit and cash handling), security, biometrics, PIN, SMS, notifications, backup and service code are frozen.
- Explicitly excluded: `experiment/arabic-sms-parser-isolated-20261104`, the sandbox app bypass, and wholesale PR #28 merge.

## What is actually implemented
- The existing 1.8.8 `MainActivity.bottomNavigation` is kept intact except for a tiny visual-only hook after the four original `setOnClickListener` callbacks.
- New `NavigationVisualDecorator` rearranges the SAME four TextViews, preserving their OnClick destinations, accessibility, ordering and theme selection. The optical lens is explicitly touch-through.
- Visual-only `glass-engine` renderers from the tested Liquid sandbox supply neutral AGSL refraction, capsule morph, 0.60 viscosity, 265ms recovery, no rainbow, and a runtime fallback.
- `NavigationLiquidMaterial` is a freshly isolated **navigation-only** rendering adapter. Its source is drawn from the tested sandbox navigation implementation but contains **no** sandbox unlock/activation, FAB integration, finance references, or application side effects.
- The existing 1.8.8 themes, user choices and preferences are **retained in place**. There is no need to re-import or rename them.
- The JitPack reference to MIT `com.github.QWEA0:liquidglass:v2.0.11` is pinned for the optical drawing surface; the license is copied to `docs/licenses/`.

## Gates to pass before any merge
1. `tests/wen_188_visual_safezone_contract.py`: diff allowlist for the ENTIRE repository against the exact 1.8.8 source; removes the one new visual hook from `MainActivity` then requires byte-for-byte equality, similarly reverts the exact Gradle visual dependency line and requires byte-for-byte equality.
2. Original `tests/financial_safe_zone_guard.py` and `scripts/test_core.sh` must pass unchanged; no DB or financial blob revisions.
3. Compile and lint original app with the new visual renderer. Verify no new Android permissions, component identifiers, database migration or signing settings.
4. Manual visual testing of each Liquid theme and old theme; tab click, press, RTL Hold+Drag, no selected-icon occlusion, black backgrounds, colored lens edges or system gesture theft; accessibility and reduced-motion behavior.
5. Manual functionality regression tests **on synthetic accounts only**, including 1.8.8 debt/ledger/account/audit tasks. Never test on real balances.
6. Do not sign, install over or merge into the original app until the user visually accepts it, the original private signing key is available in its secure environment, and in-place upgrade over a safe clone is validated.

**Status:** review-only implementation. It is NOT a signed update, does NOT replace 1.8.8, and does not touch the production branch.
