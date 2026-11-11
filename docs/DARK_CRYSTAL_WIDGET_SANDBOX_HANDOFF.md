# Dark Crystal Widgets — safe sandbox handoff

- Reference: **FIRST** user-approved Dark Crystal mockup (not the brighter second).
- Dedicated sandbox branch: `sandbox/dark-crystal-widgets-20261111`
- Widget-only commit: `a926a583d3f2f051aa7db810b9c63e9032a2a64c`
- This document's follow-up commit contains SANDBOX-ONLY identity, debug activation bypass, and non-production demo balances. **DO NOT CHERRY-PICK THAT COMMIT INTO PRODUCTION.**

## How to port into the latest real owner-signed release branch

1. Checkout and verify the **actual current production source** (v2.2 owner APK code or later). Do NOT use older main or downgrade versionCode.
2. Port only `a926a583d3f2f051aa7db810b9c63e9032a2a64c` (or copy its specific widget files listed in the commit), resolving conflicts against the newer code.
3. Ensure no changes reach FinanceDbHelper, financial rules, LicenseManager, AppIntegrity, MainActivity, manifest, Gradle build config, or database schema.
4. Retain original images/logos and original widget click actions and IDs.
5. Verify Android RemoteViews rendering on a real device or emulator, including small 1x1 tiles; verify launcher refresh, size, orientation, night/day and RTL.
6. Test financial Safe Zone and original package identity before release.
7. Sign production APK ONLY with original owner signing key after explicit approval. Never reuse the sandbox debug key.

## Sandbox-only behavior

- Package: `app.wensarafet.tracker.sandbox`, name: `وين صرفت — تجربة`.
- Debug APK is **test-key signed**, since Android cannot install unsigned APKs.
- Sandbox data is isolated from the production package. The preview placeholders are `213.84 ILS`, `40.99 ILS` and `0.00 ILS` if no local sandbox accounts exist.
- Original application remains untouched; sandbox never automatically shares its database.
- Sandbox does not register SMS capture, finance notification listener or boot receiver.
- No claims of real system wallpaper blur: layered semitransparent drawable glass is used.
- If actual device testing reveals clipping or a broken widget update, fix in the **widget-only** commit's new follow-up, not by changing financial core.
