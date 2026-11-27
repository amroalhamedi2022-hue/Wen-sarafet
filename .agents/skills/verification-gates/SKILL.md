---
name: verification-gates
description: Use for code changes, CI, test planning and release readiness.
---

# verification-gates

Inventory existing tests and avoid duplicating CI. Run narrow tests first, then relevant regression checks; do not weaken tests to obtain green status. Use bounded timeouts, cancellation-safe diagnostics and exact-SHA evidence. Reuse artifacts where practical. Build only when authorized or necessary for actual runtime verification. Status: IMPLEMENTED, TESTED, FAILED, BLOCKED or NOT RUN, separately. No auto-merge on failed, skipped or cancelled required checks.
