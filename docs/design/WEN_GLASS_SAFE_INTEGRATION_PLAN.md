# Wen Glass — integration without functional changes

> **Baseline update — 4 Nov 2026:** The authoritative **signed 1.8.8, visual-only Safe Zone merge plan** is [`WEN_188_VISUAL_ONLY_SAFEZONE_MERGE_PLAN_2026-11-04.md`](WEN_188_VISUAL_ONLY_SAFEZONE_MERGE_PLAN_2026-11-04.md). It pins the exact 1.8.8 code/approved APK identity, rejects the isolated Arabic SMS branch, and prohibits wholesale PR #28 merging. This earlier design note is supporting context only; the 1.8.8 plan takes precedence.
**Status:** proposed architecture & gates only. **NO MERGE / NO CHERRY-PICK INTO ORIGINAL APP.**
**Trial PR:** #28 (`design/wen-full-unlocked-sandbox-20261101`).
**Target candidate:** latest protected branch when user later approves; never assume `main` is current.

## Scope of the eventual merge
1. Import the independent `glass-engine/` Gradle **library** (or an AAR at a pinned, tested commit).
   Keep engine free of business/finance/security classes, storage, permissions, background workers and network calls.
2. Import design tokens / reusable view helpers from `Ui.java` and the **strictly visual**
   navigation methods in `MainActivity.java`. Cherry-pick **specific hunks**, not the giant
   `MainActivity` file wholesale. The protected branch may have newer functionality.
3. Keep original theme IDs and preferences unchanged; append any new palette IDs only.
   Non-Liquid themes get the original visual fallback. Use power-saver/reduced-motion guard.
4. The bottom bar is a view component with the **exact original tab callbacks**:
   `showMain(false)`, `showMain(true)`, `showDebtsScreen(...)`,
   `showAnalyticsScreen()`. The shared lens/press gestures are decorative.
5. Optional screen transitions must not queue or delay a business action; adjust only
   view reveal alpha/translation and stop all animators on detach.
6. Original applicationID, provider authorities, signing config, release version,
   license/auth, PIN/biometric, backup crypto and SQLite migrations remain unchanged.

## Explicit exclusions
- **DO NOT COPY** `SandboxMode.java`, `app/src/sandbox/**`, unlocked startup checks,
  special manifest/back-up flags, the `.sandbox` applicationIdSuffix, debug/development
  signing config, or test launcher label into the protected release.
- Do not copy the sandbox-specific `MainActivity` or `app/build.gradle` wholesale.
- No changes to `FinanceDbHelper`, `FinanceRules`, `DebtLedgerRules`, `Money`,
  `SplitDebtRules`, `CategoryCheckpointRules`, `BackupCrypto`,
  `LicenseManager`, `AppLockManager`, `AppIntegrity`, or other security source.
- Avoid changing XML component authorities, intent filters, accessibility services,
  notification receivers or contact pickers while integrating visual changes.

## Controlled integration procedure — later, only with approval
A. Freeze exact origin and target commits; fetch latest protected target HEAD/Actions.
   Complete functional tests on **target HEAD before any design patch**.
B. Create a private review branch from the latest protected target, not from the
   sandbox branch. Apply only the files listed in a review manifest plus visual
   method hunks. No automatic merge of PR #28.
C. Inspect the diff with a denylist gate. Fail if there are ANY business, DB,
   package identity, encryption, activation, license or signing changes.
D. Run source contracts: `scripts/test_core.sh`, 
   `tests/financial_safe_zone_guard.py`, `tests/wen_full_sandbox_contract.py`
   only in the sandbox (not production), plus security SHA-256 baseline, UI
   and navigation tests. Compare both old and new flows for add/edit/delete,
   accounts, currencies, budgets, debts, undo, history, notifications/contact picker,
   encrypted backups and first-launch / lock screen.
E. Build signed release with the **owner's original private signing identity**, through
   secure CI / local signing only. No signing keys in Git or ChatGPT output.
F. Verify same package/signing certificate and test update in place on a **test clone**
   with sample data first. Only after tablet/phone acceptance consider production.
   Backup original data, verify row counts/balances/ledger history before and after.
G. Compare reference screenshots and real device screenshots (RTL, system insets,
   60/120Hz, screen rotation, high text scale, reduced motion, power saver).
   Android's performance guidance expects ~16ms budget at 60fps and ~8ms at 120fps,
   and `adb shell dumpsys gfxinfo <package>` plus Perfetto can find jank.
   Measure P95/P99 and battery impact; don't claim a numerical speed improvement
   without device measurements.

## Guardrail for the current trial
A small delay may come from synchronous page construction/DB calculations,
which shorter animation alone cannot remove. Instrument frame timing on real
devices and keep financial queries unchanged in this visual-only stream.
**Acceptance is not "build passed".** Needs a real device, visual comparison,
round-trip and long-hold gestures, and finance regression testing.
