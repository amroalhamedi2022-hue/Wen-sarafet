---
name: signed-android-release
description: Use only for explicitly authorized signed APK handoffs or Android release verification.
---

# signed-android-release

Keep existing application ID, signing identity and monotonic versionCode. Never generate replacement keys without explicit approval. Never expose, request in public, or commit secrets. Use existing private signing process and verify APK with apksigner, compare pinned cert and package/version; hash actual output. Test installation/update when available. Preview or unsigned artifacts are not deliverables. Do not claim signed release or publish if blocked.
