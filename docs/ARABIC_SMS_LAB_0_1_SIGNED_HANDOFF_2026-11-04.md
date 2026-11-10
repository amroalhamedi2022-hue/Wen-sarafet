# Arabic SMS Lab v0.1 — delivery handoff (2026-11-04)

## What is delivered
- Standalone Android application, **not** the Wen Sarafet finance package.
- App package `app.wensarafet.smslab`, versionCode **1**, versionName **0.1**, requires Android 13+.
- New Gradle module `sms-lab`; only pure unchanged Money/FinanceRules/TransactionParser plus read-only ArabicSmsParserPilot are compiled from finance repository sources.
- Receiver `LabSmsReceiver` watches **new incoming financial-looking SMS only after the user opts in**. Does not assume the sender is the WhatsApp sender of the forwarded examples.
- Optional `LabNotificationListener` only inspects three existing supported financial app package names with explicit Android settings permission and a separate Lab opt-in.
- Native UI shows app isolation, permissions, manual paste-and-analyse, recent received **redacted predictions** (maximum 70), refresh and clear.
- No original finance database or finance application code connection; never posts expenses, debt, transfers, or account balances. The original package `app.wensarafet.tracker` and finance release 1.8.8 are unchanged.
- No INTERNET/READ_SMS/READ_CONTACTS. No raw SMS, sender names, sender numbers or OTP stored. Captures store a hash for short-window duplicate filtering (SMS and notifications remain separate channels).
- Original English/current production parser is left unchanged.

## GitHub release evidence for source commit
- App source commit: `d92ac0a979e20fafde07c13745a69ad2b0d790f2`.
- Test adjustment only, same runtime sources: `09f81111b5973f2fe03cde6eb8d7e8f5a91fd2c6`.
- CI runs on latest code:
  - Source-only finance/safety `41795769240`: SUCCESS.
  - Isolated SMS Lab Release + lint `41795769225`: SUCCESS.
  - Original finance package Release build `41795769230`: SUCCESS.
  - Original finance package debug+lint `41795769222`: SUCCESS.
- All existing original finance contract checks and new isolation/parser fixtures passed.
- ZIP payload of privately signed Lab APK matched the CI unsigned release byte-for-byte across all 5 unsigned entries.
- Lab APK signed **locally with an independent TEST signing key**, no finance owner signing key. v3 APK signature verified; SHA-256 certificate `CF584D1FE5EADBF5110E09A4F910165DEBBDAB12F39308E3B046277BE394A483`.
- Independent package means installation beside Wen Sarafet. There has been **no real-device install/SMS permission/delivery test**.

## How to test
1. Download the separate installable `Wen_Sarafet_Arabic_SMS_Lab_0.1_Installable.apk` attachment from this conversation. Do not uninstall Wen Sarafet or replace its APK.
2. On Android 13+, allow installation from the selected source if prompted, open **وين صرفت — مختبر العربية**.
3. Start with “تحليل تجريبي — دون حفظ” by pasting a redacted sample.
4. Tap “السماح باستقبال SMS الجديدة”, grant SMS permission, then ask the tester to send/receive a genuine finance SMS; tap “تحديث القائمة.” Messages received before permission are **not** fetched.
5. To inspect bank app notifications, enable Android notification listener for the Lab and then enable the second toggle in the Lab. On some devices sideloaded apps require allowing restricted settings first.
6. Read parsed amount, balance and direction; verify with the financial provider. If wrong, do not edit real finance balances; send anonymized message template/screenshot.
7. OTP/code texts should not enter captured finance results; verify. If old English SMS have altered extraction in the Lab, treat as a failed test and do not merge.

## Non-negotiable
PR #32 stays **Draft**, no merge into main or functional development without a separate explicit owner approval after field tests.
No production APK update and no data migration.
The Lab is disposable test-only software. To update it in place, the **same Lab signing key** is required; if the key is unavailable later, uninstalling the Lab clears only its isolated trial summaries and **does not affect Wen Sarafet**.
