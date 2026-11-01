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

## Visual structure pass — inner screen fidelity
The demo now has mock-up-aligned home balances, three dense blue stats, chart,
three accounts and a five-item activity list. Transactions show local-search/filter
visual controls and a long scrolling list. Analytics has currency pills, 2x2 stats,
monthly budget, chart and categories. Debts has action tabs and active/closed rows.
A fifth Settings mock is opened from the header gear. All names/amounts are fake.
No form actually saves anything or accesses the original finance app.

## Deterministic screenshot capture
QA can start the separate Android demo with `--ei preview_theme 0 --ei preview_page 0`.
These optional Intent extras render mock data only. No account or finance app API is accessed.
CI screenshot workflow uses this to capture five pages on Android emulator for inspection.

## Emulator screenshot review (2026-11-01)
The first real emulator screenshot exposed three concrete discrepancies: screenshot
captured controls occupying excessive space; title/header and chart panels too
tall; Android 15 edge-to-edge system bars overlap the page. This pass adds explicit
window inset handling, hides lab controls by default behind the grid icon, uses a
more compact chart and account rows, and gives tab icons a two-line layout.
No background work is added. Automated screenshot review is not a claim of
pixel-perfect match; next compare the newly rendered app shot to the concept crop.

## Second actual emulator-render pass
A successful Android 35 emulator screenshot confirms the fully drawn UI without
activation or crash. Review identified a heavy black inner contour and missing
Arabic weekday names. This pass makes rims pearly and adds weekday labels using
a cached Paint. Automated QA now captures eight synthetic previews (five pages in
blue and home in each alternate palette), and captures emulator gfxinfo as
**developer evidence only**, not a physical battery benchmark.
