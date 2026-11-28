# Wen Sarafet — Big Boss handoff

Snapshot 2026-11-28. Documentation only; README/open PR metadata reviewed, not full code audit or fresh CI/device certification.

## Product and constraints
Arabic Android finance tracker app.wensarafet.tracker. Preserve money logic, DB, backups, data, navigation and signing; offline only per latest owner decisions. Visual-only liquid glass integration; never merge sandbox authentication/finance changes.

## Observed development checkpoint
PR #60 b96c1ca966a508b3af44b1a6de9cd187d5de3d74 titled release 2.2.5 visual integration; #59 50e084088984faf7d430e21272cc38d8f12e4815 portable visual-only; #62 4593ed65ced89f1f9ea0850943553a9ff0fc5bda docs only. #58/#56 sandbox must not ship. Main README 1.8.5 is stale; installed version must be checked.

## Authoritative plans and evidence
docs/FINANCIAL_SAFE_ZONE_AR.md; docs/FINAL_1_8_1_RELEASE_HANDOFF_AR.md historical; scripts/verify_release_payload.py; scripts/sign_verified_upgrade.sh; newest release PR and Actions.

## Execution sequence and acceptance
1. Resolve latest production ancestry incl offline security and all financial fixes; compare visual-only diff. 2. Optimize glass/FAB and golden cards, synchronized transparent lens, nav scroll and animations; real screenshot/diff and frame pacing. 3. Financial regression and backup restore with synthetic fixtures; verify no network/SMS functionality and no DB or security regression. 4. Release with same package and original signer, increment versionCode, verify exact payload and upgrade without data loss.

## Build and test
Follow owner-only sign_verified_upgrade.sh and verify_release_payload.py, signer pin/apksigner and install-upgrade test. Never ship debug, sandbox or unsigned APK. Never regenerate an existing production signer.

## Resume procedure
Read AGENTS.md if present, original plans, current default branch, open PR heads/base/ancestry, exact SHA Actions and artifacts; compare against this snapshot. Choose newest compatible cumulative implementation, never overwrite newer changes. Record completed / remaining / blocked with exact commit and test evidence. Work in substantial nonduplicate batches; build at meaningful milestones, not each change. Before publishing, check package/version/signing continuity, licenses, offline/privacy policy and real-device acceptance where applicable. Do not infer completion from PR title or CI alone.

## Private handoff boundary
Owner-only material (keys, passwords, private data, ROMs, personal documents, original restricted assets) stays outside GitHub in access-restricted owner storage. Consult the private Big Boss Drive register. No secret values or private links are included here. If missing, record blocker; do not generate replacement keys for installed apps.

## Cross-project law
A lot of work to reach our goal as perfect as possible and as masterpiece as possible at best time. Master functionality in a masterpiece app. Evidence over claims; optimize time/cost; no duplicated work.
