# Wen Sarafet — Batch 01 source checkpoint (2026-10-24)

## Provenance and limitation
- Recovery input: `Wen-Sarafet-1.8.0-RECOVERED-SOURCE.zip`, which is a reconstruction from 1.6.2 and evidence of 1.7/1.8 behavior, **not** a verified byte-identical original source checkout.
- Reference 1.8.0 APK SHA-256: `f5722ccabfbb211f2999a00c1d0f67cbab7a86f57159f43e8aa07cba19f3c6a0`.
- The full modified, sanitized source ZIP is retained in ChatGPT Library: `/Wen-Sarafet/Wen-Sarafet-1.8.0-BATCH01-SAFE-SOURCE.zip`. Do not confuse the code snapshot in Library with an APK or a verified production release.
- Public GitHub branch currently contains isolated rules/tests plus this checkpoint; remaining Android source/resources are **not yet imported to GitHub**. This branch is NOT buildable as an Android app yet.
- Excluded from public snapshot: all `license-generator/`, `signing/`, original private materials and generated build outputs.

## Actual local source changes in Batch 01
1. Added `CategorySuggestionRules`; source-history-based category suggestions at transaction entry, applying only when explicitly selected; 2 ILS transport example requires repeated account-specific evidence. Data remains on device.
2. Added `FinanceDbHelper.suggestCategory` read-only 200-item history query, and suggestion UI in `MainActivity`.
3. Guarded `ReminderReceiver` by fresh count of unclassified expense and pending expense capture; never posts the daily reminder when both counts are zero; no other notification channels changed.
4. Added `FxTransferMath` with exact BigDecimal rounding. Added DB v9 FX fields + atomic manual cross-currency transfer; source-account debited and destination-account credited separately. Added FX delete reversal; legacy transfer behavior unchanged. A separate cross-currency transfer dialog and small “Save rate” action are present in the sanitized source ZIP.
5. Protected 7 source classes (PIN, biometrics, offline license/crypto/integrity etc.) have SHA-256 matching the recovered input snapshot.

## Passed LOCAL checks (not device/Android APK verification)
- FinanceCoreTest; CategorySuggestionRulesTest; FxTransferMathTest; LicenseProtocolTest; BackupCryptoTest.
- DebtMigrationAudit and FxMigrationAudit (SQLite v8→v9, reversal, pending classification query).
- Recovery audit (XML, resource structure, version metadata, declared permissions, secret separation).
- No Android SDK available in the environment. **No APK built and no real-device update test performed**.

## Known unimplemented requirements
- Daily online rate acquisition and “Refresh online” from a trustworthy, dated source with clearly distinguished BOP buy quote vs market reference; rates cannot be fabricated.
- Settings change history/undo, cap congratulations, debt-aware split, income/received analytics, multi-category deletion, motion redesign, icon alignment and picker, two theme additions.
- Review-and-delete FX edit restriction: to protect ledger consistency, FX entries support delete/recreate, but not in-place reclassification yet.
- Full Android build/CI, privacy review, v8→v9 device migration and visual tests still required.

## Security and acceptance blockers
Do not upload signing or licensing private keys, owner-only generator, personal data, or backups to the public repo. Do not claim the APK 1.8.0 installation can be updated until the original signing identity and Android instrumented tests are verified offline.
