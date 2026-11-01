# Wen Glass UI Engine — visual-only pilot (2026-11-01)

## Design scope
This is a first *code* implementation, not a claim of pixel-perfect match to AI-generated concept boards.
The only altered visual positions are the main header, home balance tiles, spending summary tiles,
and seven-day chart panel. Other themes (indices 0-15) retain their prior drawable behavior.
Liquid Aurora (16) and Liquid Fusion (17) opt into the new glass surface.

## Battery and frame policy
- No background workers, service, animation clock, handler, frame loop, or continuous invalidation.
- Android Canvas shaders and reusable Path/RectF/Paint objects are cached on dimension changes.
- No live blur/refraction; this first renderer is deliberately a passive, precomputed effect.
- Power Save Mode or low-RAM devices draw only the static fill plus outline.
- A call to setAlpha or setColorFilter invalidates only when explicitly changed.
- UI and business state, database, financial Safe Zone, reminders and backup are untouched.
- No new permissions, libraries, signing changes, minSdk changes, or app identifier changes.
- Future true refraction pilot must be measured on actual tablet and phone; do not animate
  offscreen, while app is in background, or while power saver/reduced motion is active.

## Reuse
`app.wensarafet.glass.WenGlassDrawable` has no dependency on Wen Sarafet code.
Move it to an Android library module later, after screenshot and energy benchmarking.
Host apps pass radius and palette: the engine does not own finance logic or settings.

## Measurement and release gates
1. Compare same-device screenshots with the reference concepts; check Arabic text legibility.
2. Record baseline and pilot Perfetto / Android GPU frame metrics during scroll and idle.
3. Check P95/P99 frames, CPU/GPU load and battery drain on actual Xiaomi Pad and phone.
4. Run standard Android compile/Lint and visual contract tests.
5. Keep this Draft PR unmerged until a signed in-place upgrade has been verified.
   CI debug APK has a different package suffix and must NOT replace the installed app.
6. Production upgrade requires the original signing key and verified payload; never expose key.

## UI coverage — phase 2
- The shared navigation bar now uses a frosted static panel in Liquid themes; selected tab uses a
  color-matched static lens surface. Existing tab click/scroll/hide behavior remains untouched.
- Account rows, transaction rows, debt person cards, category rows, chart panels, and key
  settings panels now use the same palette. Buttons reuse the engine's accent surfaces.
- **Performance partition**: scroll-heavy list rows use a native GradientDrawable instead of
  an offscreen blur, animated shader, or per-item refraction; strong glass is for foreground
  navigation and summary cards only.
- Standard 16 themes still use the same rounded-color drawables as before.
- Remaining visual work: design-reference parity, device-tuned typography, icon placement,
  safe-area adjustment, true on-interaction optical refraction if real-device benchmarks allow.

## Reusable Gradle module — phase 3
The renderer is now built as its own Android library module `:glass-engine`.
The original class is removed from the application source directory, so no duplicate
class remains. Apps can consume the library as a Gradle project dependency or build an AAR.
The engine's API uses only Android SDK classes (Context, Drawable, Canvas), no third-party
runtime dependencies, databases, preferences, or finance-app code.
No extra background services, receivers, threads, permissions, or work schedulers.
This module should be independently versioned before it is reused by other applications.

## Four newly selectable visual themes — phase 4
Appended at theme preference IDs 18–21 (never overwrite indices 0–17):
18. زجاج سائل أزرق — sapphire, cobalt, ice-blue.
19. فقاعات أكوا — turquoise, seafoam, aqua.
20. أورورا ليلية — midnight violet; dark style also when the device uses light mode.
21. لؤلؤي متجمد — pearl white, frost-blue, lavender.
Existing preference IDs, financial state, and dark-system rules are preserved.
The image-generation mood boards remain aspirational: these are lightweight,
static palettes until visual screenshot matching is complete.

## Floating navigation and battery fast path — phase 5
The tab bar receives 10dp lateral and 8dp bottom spacing only for Liquid themes;
its click handlers and existing 72dp height are retained. The floating action
button uses the same static lens palette, without modifying its add-transaction action.
No layout change is made to older themes. Power-save/low-RAM path now draws only
a rounded rectangle and outline, entirely avoiding canvas clip/save overhead.
