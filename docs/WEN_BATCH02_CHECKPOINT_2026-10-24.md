# Wen Sarafet — Batch 02 (partial integration checkpoint)
2026-10-24

The recovered v1.8.0 Android source is **not yet fully imported into this public GitHub repository**. The current branch contains independently testable pure Java rules; it is NOT buildable as an Android app.

Verified in local source archive:
- two new theme definitions with the original 16 preserved;
- monthly cap arithmetic, previous-month month-keyed snapshot and greeting (no fake savings balance);
- income versus received aggregation;
- USD-to-ILS indicative Bank of Palestine rate fetch with explicit user acceptance, no silent rate override;
- category archiving and category emoji picker;
- database audit triggers on transactions/categories/splits;
- nav icon gaps and reduced-motion support;
- original security classes unchanged in byte-for-byte comparison.

Limitations:
- history displays edits, but unsafe broad Undo is not enabled;
- mixed debt/repayment splits remain pending;
- no full Android SDK build/installation/signed update has been verified;
- source is recovered, not proven bit-identical with original 1.8.0.

The authoritative changed Android source was packaged locally; do not merge until complete source is sanitized/imported and baseline/migration/device tests pass.
