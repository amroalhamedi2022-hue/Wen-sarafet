# Wen Sarafet 2.0 — Strict offline privacy overlay

**Owner instruction (2026-11-06):** Latest full-featured release including all pending design additions must be made strictly offline. **Do not build, sign or install any APK** while the visual themes are being developed elsewhere.

## Correct baseline
- Theme work baseline: `design/wen-visual-parity-owner-review-20261106` at `f84e94bb030dca638a8606e6bed5169a1a6524eb`.
- Independent source overlay: `feature/wen-200-offline-security-overlay-20261106`.
- Latest confirmed version: **2.0**, versionCode **28**, original `applicationId app.wensarafet.tracker` (NOT the main branch v1.9).
- Visual-only release base `release/wen-2.0-premium-original-signer-20261105` is the ancestor of the active theme review branch.

## What changed
- Removed app's INTERNET permission, network fetcher, online FX rate refresh and connected Google Drive backup code. Android auto backup disabled. Local encrypted backup/restore allowed only via local system document providers.
- Bank of Palestine SMS and notification capture, native account logos/widgets, bank USSD shortcut and account seeding all preserved.
- Jawwal Pay/PalPay, original accounts, debts, budgets, analysis, split payment rules, notifications, license, animations and 30 premium theme indices retained.
- The entire `FinanceDbHelper.java` and original protected financial Safe Zone files are byte-for-byte unchanged.
- Exchange-rate entry remains manual, the existing FX arithmetic and local manual rate storage are preserved. Historical BOP rate provenance in old transactions is retained.
- Added OTP filtering for wallet SMS beginning `Code:`.

## How to integrate safely after visual approval
1. Do NOT overwrite `design/wen-visual-parity-owner-review-20261106` or force-push any shared branch. Let that other conversation finish its design work first.
2. Check the **newest** theme branch head again before integrating: it may have advanced beyond `f84e94bb030dca638a8606e6bed5169a1a6524eb`.
3. Rebase/merge/cherry-pick the security-only changes onto the approved theme HEAD, reviewing `MainActivity.java` carefully for simultaneous layout edits. Do not copy a v1.9 MainActivity or replace the whole file.
4. Visual-only tests with pinned MainActivity SHA and manifest content are expected to need **explicit reviewer rebaseline** for this owner-approved network-removal change. Keep all actual visual functional safeguards; do not just switch those tests off.
5. At release time, run a compiled APK merged-manifest permission check, full financial regression suite, original signing certificate/upgrade verification, and preserve data and package identity.

## Verification scope
- No APK build, no signing, no installation, no merge to main or theme branch.
- Source-only GitHub Actions is allowed and covers Java core financial checks and audit scripts; APK-specific gates are staged for the eventual release.
- No user data was touched, migrated, erased or shared by these source changes.

Network offline does NOT disable telephony network use through the separately launched phone dialer, nor does it stop unrelated phone apps from networking.
