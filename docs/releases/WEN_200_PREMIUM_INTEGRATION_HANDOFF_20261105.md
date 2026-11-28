# Wen Sarafet — cumulative premium visual integration (2026-11-05)

Official starting point: `main@283112f4bedaf2e07dd7370e581f84e14959710e`, v1.9/code27.
Latest visual motion: `design/wen-190-sandbox-visual-parity-20261105@d71d8082b1efe7a1cfc3b9aaf29f5d64e20c4c25`. The branch is not the unlocked bypass.
Five append-only themes: `design/wen-190-five-premium-liquid-visual-20261105@ea61bb7a266652751fe9c08f321e0721350df1ea`.

Two-parent integration commit. Only two files were edited jointly: Ui.java (non-overlapping, line-level changes) and its Safe Zone source test. UI normalization proves every line outside the four pre-approved sandbox visual helper methods matches the exact approved 30-theme source.
Theme IDs 0–24 frozen. IDs 25–29 appended. Screen-motion hooks and the existing FAB/gesture optics retained. Original MainActivity and motion SHA pins remain unchanged. No finance, debt, DB, SMS, auth, licence, backup, account assets, package, signing or permissions were changed.

## Gates required before release
- Full new source suite, release compilation/lint and Android API 33/35 instrumentation must run on the integrated source; older 1.9 results do not prove the integration.
- RTL/contrast, gesture and FAB touch-target, low-power, motion and preference persistence need device QA.
- This merge intentionally retains 1.9/code27 staging metadata. Before actual distribution raise versionCode and update hardened upgrade/version tests, then build a release signed by the original owner certificate.
- Private owner keystore must never be uploaded to GitHub. No signed APK has been produced by this integration commit.
- PR #34 friend-only SMS and original unlocked sandbox bypass are excluded.
