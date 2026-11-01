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

## Experimental Android 13 GPU lens
Optional **عدسة GPU** toggle on the demo's top hero enables `WenOpticalDrawable`.
It uses Android AGSL `RuntimeShader` to offset samples from a locally generated
static gradient; this is not real-time refraction of content beneath the card.
No screen readback, no timers, no shader animation, and automatic fallback in battery
saver, low-RAM or unsupported environments. Default is OFF. This experiment remains
strictly separate from the financial app until measured on physical devices.

## Reference-alignment pass
The 1122x1402 concept image contains six device mockups. The lab now prioritizes
screen *content fidelity* over a marketing-style banner: compact RTL header,
paired icon buttons, airy light glass tab bar, small theme/energy controls, and
separate chart frost recipes. Since the concept includes decorative mockup frames,
its overall pixel diff is NOT an Android target. We match the inner screen only.
