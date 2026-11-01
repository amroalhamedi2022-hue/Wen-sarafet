# وين صرفت — نسخة الاختبار الوظيفية المستقلة

Built from latest functional `fix/wen-contact-picker-xiaomi-20261101` branch,
which already incorporates the 22-theme visual pilot and the contact-picker repair.

## Identity
- Original app stays `app.wensarafet.tracker` and retains signature, license,
  PIN/biometric, backup encryption and financial safe-zone logic unchanged.
- Trial build type uses `app.wensarafet.tracker.sandbox` and the launcher label
  `وين صرفت — تجربة`; it has **its own empty SQLite and preferences**.
- No owner activation/license code nor startup PIN is required for this exact
  sandbox package. A debuggable flag **and** exact package name are required to
  take this isolated path; no bypass exists for production identity.
- Its Android cloud backup is disabled to avoid copying unprotected test data.
- Different sound provider authority permits installation beside the original.

## Functionality
Same account/ledger/debt/budget/categorisation/undo/search/widgets/reminder
and optional notification capture code; only the sandbox startup activation and
app-entry PIN gates are excluded. This is a full finance-app trial, **not**
a static mock. All money/DB code and original protected security sources remain
unchanged. Visual glass renderer and four added themes come from the functional
base's pilot work.

## Encryption safety exception
The current SQLite storage is not SQLCipher-encrypted. The exported backup files
remain password AES-GCM encrypted, and payment USSD PINs remain Android Keystore
protected; purposely removing these protections would expose sensitive data and
break restore compatibility. Do not use any real finance secrets or enable SMS
or notification reading while experimenting with an unprotected sandbox.

## Build
`gradle :app:assembleSandbox` produces `app/build/outputs/apk/sandbox/app-sandbox.apk`.
This is a Debug-signing-key test package, not original owner's signed release.
CI runners may rotate test signing keys; a later update can require uninstalling
**only the sandbox**, losing sandbox records. Never uninstall/replace the original.

## Verification gates
1. Compile, lint, source-safe-zone and security SHA-256 contracts.
2. Emulator launch and smoke check of first-run account/UI.
3. Real tablet/phone tests for financial actions on fictional transactions.
4. Theme screenshot compare, scrolling P95/P99 and actual battery/thermal testing.
100% visual parity is not verified by a successful APK build.
