# Wen Glass Lab — standalone Android UI engine

This folder is an **independent Gradle project**. No app license, activation, auth,
SQLite, notifications, account data, or access to the main app is required.

## Build
From repository root (with JDK 17 / Android SDK 35 / Gradle 8.10.2):
`gradle -p glass-lab :showcase:assembleDebug`

App id: `app.wensarafet.glasslab` (different from installed Wen Sarafet).
Android 13+ is required. It can coexist with the financial app.

## Architecture
- `engine/`: reusable Java / Android View Drawables and theme tokens; no dependencies.
- `showcase/`: independent design playground using fictitious sample transactions.
- Rendering is passive. No endless loops, background workers, SMS or accessibility.
- Eco mode selects simple native gradients; the standard path caches optical surfaces.
- Hardware/API-specific real refraction remains future work, only if it passes battery tests.

## Guardrails
Screenshots for precise matching must be measured on real tablet/phone before signing
or integration. **Do not merge the earlier design PR into the financial app.**
