# Wen Sarafet — Scoped Repair & Development Plan
Version: 1.0 | 2026-10-23 | Status: PLAN ONLY — no application code modified

## 0. Scope, baseline and release gates
Only the 12 user-requested feature/change groups in this document may be implemented. All other ideas from the prior feature survey are excluded. No broad refactors, dependencies, permissions or behavior changes without direct need and regression proof.

Repository: https://github.com/amroalhamedi2022-hue/Wen-sarafet (public; observed empty, no commits, no branches, 2026-10-23).
**BLOCKER:** Obtain the most recent ORIGINAL buildable source, versioned DB schema, configuration, tests, and UI screenshot. APK alone is inadequate. Do not put owner-private bundles, app signing keystores, device-bound licensing data, key generators/private signing material, personal financial data, Google tokens, or any other secret in this public repository. Only upload a reviewed secret-free source snapshot; keep secrets outside GitHub. Create `.gitignore`, secret scanning, and clean-room sample test data before source import.
Preserve installed-app identity (applicationId), signing continuity, Room schema migrations, all existing user data, existing themes, PIN/biometric lock, offline ECDSA P-256 verification / Keystore binding / anti-tamper / R8, widgets, USSD, debt and account functions, background notifications, offline behavior, backup/restore, and accessibility unless an explicitly requested change requires a narrow modification. No write to security modules. Any security issue discovered is reported separately, without an unapproved modification.

### Release gates
- G0 source import verified (compile + test), baseline checksum/version, tests for current flows including lock/back navigation, existing feature screenshots, sample database backup-and-restore; no secrets committed.
- G1 each change implemented behind isolated PR with tests and screenshots where visual, assessed against baseline.
- G2 full regression + migration tests + no unintended permissions, network access or security/lock behavior changes.
- G3 install **updating** previous version with retained finance data, verify accurate balances and debt ledger; rollback strategy and backup; protected signing and real-device checks.
- G4 approved candidate release only after user review; never claim working APK or passed test without evidence.

## 1. Smart classification rules
Learn on-device from confirmed edits: normalized merchant/payee, account, description and per-user examples. Priority:
(1) user-selected category; (2) reliable merchant+account+description associations; (3) shorter merchant/description/account associations; (4) transaction-amount pattern when strongly supported by repeated confirmed historical examples; (5) no suggestion.
Example: if previous confirmed cash payments for 2 NIS commonly belong to Transportation, show “غالبًا مواصلات” with confidence and one-tap acceptance. Not a forced rule for every amount=2, not a transfer/debt/income, and NEVER auto-post or silently change balances. Arabic-normalization, case/whitespace variations, source specificity, and correction-learning. Local-only, disable/reset learned rules option; test same amount different categories and different accounts.

## 2. Settings > Change history + Undo
Settings entry “سجل التعديلات” with dated change list, type, actor/source, before/after values, origin, and search/filter. Record create/edit/delete/category changes/splits with append-only audit semantics, atomic data+log transactions. User confirmation for Undo, reversible delete (tombstone or equivalent), new audit entry for Undo; never recursively duplicate effects or money movement. Hide sensitive details until normal app unlock; no plaintext logs or external telemetry. Backup and schema migration include audit. Tests: 1 edit, multi-field edit, undo, delete/restore, after app relaunch, debt and transfer correction, repeated Undo.

## 3. Savings within existing monthly spending cap
Monthly cap spending only across expense-eligible transactions in selected currency; exclude transfers between own accounts, income, received, debt principal/settlement, and non-affecting-balance entries as previously defined. While month is underway label difference `remaining under cap`, not definitely “saved”. At month-end display `saved vs cap = max(0, cap - qualifying expense)`; if over cap show overrun without a fake negative saving. Optional light celebratory micro-animation “Congratulations / أحسنت” on eligible milestones/finished under-cap month, no repeated spam. Never create actual balance or separate account automatically. Handle no cap, cap reset, category edits, refunds and month boundaries; derive from canonical transactions rather than cache-only calculations.

## 4. No unnecessary classification notifications
Send reminder **only when outstanding transactions still require classification**. Query pending count from canonical DB immediately before posting notification; when 0, do not send and cancel outdated classification notification; throttle/dedupe on count+pending IDs and respect existing settings / Android permission. Never affect other reminder types. Tests 0 pending, 1 pending, after resolving last, app background/reboot.

## 5. Bank of Palestine USD => ILS internal transfer, online rate/manual rate
Select from BOP USD to BOP ILS in existing “تحويل بين حساباتي”. Bank **buys** USD when the customer sells dollars to obtain shekels; prefer BOP *buy* USD/ILS quote. Display rate source, direction, quote timestamp, tentative status and effective exchange/fees clearly. BOP public website rates are indicative and may be unavailable/JS-rendered; **do not invent a quote or silently substitute a market midrate as the BOP rate**. Test retrieval feasibility; fallback to explicitly labelled reference rate or offline manually entered value, user confirms actual effective rate. Online daily at most one background successful refresh per date when connected, on relevant screen with stale/no rate request input; small buttons **حفظ** and **تحديث أونلاين**. Manual override saved locally and not overwritten within an in-progress transfer. Periodic scheduling (WorkManager with connected network) is best effort, not exact clock guarantee.
Atomic transfer: debit USD, credit ILS according to chosen rate, separate optional fees, store immutable effective rate and timestamp/source for historical records; transfer not expense or income, restore/undo reverses both legs exactly. Validate amount, rounding and account sufficiency consistent with existing rules. No other types of transfer changed.

## 6. Split/edit transaction with debt or repayment
Each split line has category AND transaction nature (expense, debt lent / owed as relevant, debt repayment, other existing supported classification), person/debt relationship when needed, amount and currency. Keep old standard category picker for normal splits. Sum of split values exactly matches original posted amount in minor monetary units; balance effect posted once, debt ledger entries per selected nature and direction. Editing/reversing split recomputes effects atomically; no double spend/income; show totals remaining.

## 7. Analysis: income and received
Extend current analysis UI (preserve charts/filter/currency toggle) with separate Income and Received views/segments and date/account filters as existing. Do not let “مستلم” leak into expenses, double-count incoming inter-account transfers, or misrepresent debt repayment. Keep distinctions of category vs actual ledger effect. Tests for mixed currencies, excluded categories, months with zero entries.

## 8. Delete categories (single and batch)
Inside Add/Edit Category list: Delete action; multi-select + Delete; confirm with affected transaction count. Unused categories can be removed. Used categories require archive/replace in a safe transaction; retain historical labels or explicit user-selected migration without orphaned records, avoid changing historical analytics silently. Protect system-critical category semantics and preserve existing custom categories; test default, custom, category used by split/debt, cancel, undo if permitted.

## 9. Animations
Improve motion throughout app via a shared motion token spec: screen navigation, loading/saving feedback, item add/remove, bottom sheet/dialog, cards, chart transitions and microinteractions. No changes to flows/business behavior. Favor native GPU-friendly transform/alpha, short intentional durations, reduced-motion system support, low battery/performance fallbacks; verify smoothness on Xiaomi Pad 5 and low-end Android, no blocked touches, no extra biometric prompts.

## 10. UI icon spacing regression
Use reference screenshot when provided (not available in this handoff), identify exact offending screen/icon row, enforce consistent paddings/gaps and minimum touch target with RTL, Arabic long titles, different display/font scales, portrait+landscape and accessibility testing. No rearrangement outside the requested visual correction.

## 11. Modern, user-selectable icons
Replace/refresh visual icon set with coherent modern icons, retain semantic meanings. Provide icon selection in settings (theme/global style and/or per-category as supported by existing architecture); ship offline assets only; icon previews; fallback icon for old/missing values; ensure old category IDs/transaction mapping intact. Verify color contrasts and spacing for all preserved old themes.

## 12. Exactly two NEW themes
Append Liquid Aurora and Liquid Fusion as separate theme IDs without editing existing palettes/IDs/storage values. Respect both light/dark support where applicable; keep previous theme selection across upgrade, avoid forcing a new theme; build semantic palette tokens with readable contrast and accessibility/RTL. New animation theme accents only where compatible with existing UI.

## 13. Mandatory request-by-request verification
| ID | Acceptance test (must demonstrate) |
|---|---|
| C1 | Confirmed 2 NIS transport entries cause suggestion; conflicting/other account does not force it; manual corrections learned; never mutate ledger. |
| C2 | Settings history logs add/edit/delete with before/after; Undo restores ledger exactly including transfer/debt and logs reversal. |
| C3 | Cap calculation excludes income/transfers/debt; live remaining and end-month congratulations accurate; no fabricated savings account. |
| C4 | Zero pending means zero classification reminders and stale notification cancelled. |
| C5 | USD->ILS BOP internal transfer uses correctly labelled buy-rate/manual fallback, offline/save/online refresh, accurate two-leg balances and fee/rounding, once-daily best effort. |
| C6 | Split supports debt, repayment and normal categories, amounts reconcile, ledger corrected once. |
| C7 | Analysis separates expense/income/received while preserving existing exclusions and currency modes. |
| C8 | One/many custom categories delete/archive safely without orphaning history; confirm/cancel works. |
| C9 | Motion smooth, respects reduced-motion, preserves touches/focus and app behavior. |
| C10 | No icon overlap on screenshot screen and standard device/scales/RTL. |
| C11 | Icon picker, selection persisted, modern icons, legacy category links intact. |
| C12 | Two new themes visible; old themes byte-for-byte visually/semantically unchanged and persist across upgrade. |
| R1 | PIN/biometric/back-nav protections, offline licensing, Keystore, tamper behavior and all current functions unchanged. |
| R2 | Database upgrade+restore with real-like redacted fixtures, account totals, debt balances, backup import, USSD widgets and notifications validated. |

Double check every item at unit/instrumented level where possible and via real device evidence. Report pass/fail/not-tested honestly; preserve screenshot/video, Actions run, commit SHA.

## 14. PR/change order
A. Baseline import (no security secrets), baseline tests/CI; source backup and diff map; collect target screenshot.
B. Pure logic & persistence: C1 (classification) + C4 (notification), C2 audit/undo, C3 cap savings.
C. Finance flows: C5 transfer/rates, C6 split/debt, C7 income/received, C8 category deletion.
D. UI only: C10 spacing, C11 icon picker, C12 themes, C9 animations as final cross-cutting polish.
E. Full double regression, migrated-install test, protected security verification, candidate package.

Implement one scoped PR for each group or a tightly justified set of isolated commits. Never merge unbuilt/untested code. GitHub Actions should enforce lint/unit test/build/instrumented tests when supported by runners. All development tools should be free/open source where possible.

## 15. Specific user manual test path
1. Make an app backup; record source account balances, debt totals, theme and PIN/biometrics; perform update install (not clear data).
2. Create known 2 NIS transport examples, then a new 2 NIS transaction and verify suggestion/manual correction.
3. Edit/delete an example, open Settings > Change history, Undo and compare amounts.
4. Set monthly cap; verify remaining changes on expense but not internal transfer/received/debt repayment; review end-month message with test date/fixtures.
5. Mark every pending classification as done; verify no classification alert remains and none reappears without new pending.
6. Choose BOP USD -> ILS, inspect buy-rate source/time, manually save rate, refresh with internet, retry offline, confirm USD/ILS balances with exact computation.
7. Split transaction into expense + debt/repayment, verify total and debt ledger, Undo.
8. Analytics: check expense vs income vs received, date filters/currency; category single/bulk deletion with reassignment/archive.
9. Verify all legacy themes unchanged, two new themes, icon selector and overlapping-icons screen, animation with reduced motion.
10. Check app lock cannot be bypassed with Back, biometric/PIN, USSD widgets, database restore, balance sums and offline activation across restart.

**State at plan creation**: repository empty. Neither application implementation nor app runtime verification has occurred. Source import and screenshots must precede any claimed code fix.
