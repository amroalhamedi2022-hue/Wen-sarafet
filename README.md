# وين صرفت — Wen Sarafet

Arabic-first Android personal finance tracker. Application ID: `app.wensarafet.tracker`.

## Verified source state — 2026-10-30

**Important:** The cumulative development branch is `feature/wen-batch21-reject-duplicate-zip-entries-20261030` (PR #24), currently version `1.8.2`, versionCode `18`. It contains the financial integrity Codex work plus subsequent source, search/UI, backup and financial safety improvements. Do not ship the debug APK or CI's **unsigned** release candidate to the installed 1.8 owner.

The **original release signing private key** is a hard prerequisite for updating the user's installed 1.8.0 while retaining the application identity and AndroidKeyStore-backed activation. A previously signed 1.8.1 APK is **not** proof that it contains the newest code. Compare all unsigned/signed APK ZIP payload entries with `scripts/verify_release_payload.py`, or use the owner-only `scripts/sign_verified_upgrade.sh` which enforces that comparison plus certificate pinning and source checks.

For current financial freeze, debt lineage and cross-currency fixes, see **[FINANCIAL_SAFE_ZONE_AR.md](docs/FINANCIAL_SAFE_ZONE_AR.md)**. Version 1.8.2 is the patch candidate **for updating installed 1.8.1**; it is not the older 1.8.1 APK signed on October 30. Preserve the installed data and existing certificate.

For historical signing, upgrade, data, PIN/biometrics, widgets, notifications and backup acceptance steps, see **[FINAL_1_8_1_RELEASE_HANDOFF_AR.md](docs/FINAL_1_8_1_RELEASE_HANDOFF_AR.md)**. The current release uses the same pinned signer and `scripts/verify_upgrade_apk.sh` now checks both 1.8.0 and 1.8.1 as upgrade sources.

## Verified state — 2026-10-29 (historical baseline)

This public repository **does contain reconstructed application source** under `app/src/main`.
It is a recovered and incrementally improved source tree, **not a proven
byte-for-byte copy of the originally installed 1.8.0 APK**.

The current cumulative draft-PR chain (do not start new work from stale `main`):

1. Previous source fixes through Batch 06: PR #5.
2. GitHub CI safety gates: PR #7.
3. Batch 07 SQLite v13 and atomic debt/split accounting: PR #8.
4. Android API 35 debug compilation CI: PR #9.
5. Batch 08 historical monthly budget cap preservation: PR #10.
6. Batch 09 reduced-motion screen/navigation safety: PR #11.
7. Batch 10 audit policy alignment: PR #12.

The documentation-only handoff is in PR #6 (`docs/HANDOFF_LIVE_AR_2026-10-28.md`);
do not assume that docs-only branch contains newer app code.

### What is verified

- GitHub-hosted Java business-rule and SQLite migration/source-contract tests.
- Protected security source digest checks and Android XML/manifest guards.
- Android debug APK **compilation** with JDK 17, Android SDK 35, Gradle 8.10.2.
- No owner signing secrets or license private keys are tracked in the public tree.

### NOT TESTED / NOT RELEASE-READY

- An Android device exercising transactions, debt splits, FX, PIN/biometrics,
  notifications, widgets, visual themes, animations, and Drive backup.
- A real signed in-place upgrade from the user's installed 1.8.0, including
  signer-certificate identity, DB migration and preservation of personal records.
- Release signing, R8/proguard runtime behavior, and owner-only license generator.
- The precise icon-overlap defect: its original screenshot is still required.

**Do not present a debug APK as an upgrade for the user's installed application.**
The debug variant uses the `.recovered.debug` application ID suffix.

### Local/source checks

```sh
bash scripts/test_core.sh
python3 tests/source_repository_guard.py
CORE_TESTS_ALREADY_RUN=1 bash scripts/audit_recovered_source.sh
```

The Android debug compile and security checks also run through GitHub Actions
on the draft PRs. The release audit requires original signing/generator artifacts
**which must remain outside this public repository**. A passing source-only CI
check is not a production release approval.

### Safe continuation rules

- Continue on the latest validated cumulative feature branch, not `main`.
- Keep each increment in a new branch and Draft PR. Do not auto-merge.
- Prefer GitHub Actions for repeatable tests; use Codex for actual code problems.
- Preserve account identity, balances, local SQLite, backup data, signing
  certificate continuity, PIN/biometrics, ECDSA licensing and key separation.
- Do not publish signing keys, generator private keys, personal financial data,
  personal SMS records or backups.
