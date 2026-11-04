# وين صرفت — الخطة الرئيسية لمحرك حركات Liquid Glass

## 4 November 2026 — Batch 2: increased viscosity, navigation ONLY (emulator verified)

**Feature commit:** `8bceb86bc01fcc4b6620ba2ff91283f87c52790f` on `design/wen-full-unlocked-sandbox-20261101` / PR #28 Draft. User requested more viscosity and fluidity **only for navigation**. No FAB, SQLite, finances, security, original package or main branch modifications.

- Sandbox-only nav tracking factor **0.78 → 0.60**; native touch-through tab handlers and accessible destination semantics are unchanged. Fallback and non-sandbox navigation retain 0.78.
- Longer same-tab material release spring **190ms → 265ms**, with lower overshoot `0.24`; disabled animations and battery saver settle without elastic lag. Native drag-strain energy retains a small amount across slow finger movements (`max(newEnergy, oldEnergy * .86)`) and dissolves as the finite morph falls to zero. No idle timers or per-frame unbounded work.
- More visible X stretch (`.088 → .143` per strain), Y compression (`.041 → .065`), neutral refraction (`4.4 → 6.2` strain px in dp-scaled settings), and stronger bevel. All optical tint, zero chromatic aberration, unblurred glyph sampling, over-glyph composite, and touch-through fixes are preserved.
- **Verified CI on code SHA:** `41801375378` sandbox APK + lint PASS; `41801375211` actual Android emulator PASS; `41801375199` finance/security/source tests PASS; `41801375305` debug compile PASS; `41801375187` unsigned original-package candidate PASS.
- Emulator shows legible tab icons at startup, successful FAB click (unchanged), held lens refraction, actual `UP_TARGET tab=3 from=0` and successful Home→Analytics Hold+Drag, active nav strain marker, and no activation blocker on isolated sandbox.
- **Test APK:** `app.wensarafet.tracker.sandbox`, 13,114,737 bytes, SHA-256 `f9f4c60e7fbd655ecd2a1b3b2d230eb8adedb9f07074716e244690af5bb35749`. GitHub Actions artifact `13307308983` from `41801375378`. Debug-key-signed trial APK, **not** an update to the original installed app.
- Remaining visual acceptance: physical tablet and phone testing for subjective gel feel, fluidity balance, 60/120Hz performance, and reduced-motion/manual accessibility. Do not merge to production without user acceptance and original signing/security review.


## 4 November 2026 — Navigation-only Liquid deformation (Batch 1)

**Scope:** User explicitly limited this iteration to the bottom **navigation bar**. No FAB refactor, transaction/finance, database, background notification, authentication, production signing or main-branch change. This continues the already-approved neutral, touch-through optical lens.

- `GlassMotion.NavigationGesture`: finger-follow is now a bounded 0.78 per-touch viscous interpolation; selected-tab release uses a finite damped spring; the material receives a real pointer anchor on DOWN; each fresh touch resets old drag energy (no stale stretched state).
- Sandbox-only `SandboxOpticalGlass.attach(...)`: neutral optical refraction/bevel and capsule X/Y deformation adapt to pull and event delta; the exact same finite press animator smoothly returns strain to zero. Power-saver/disabled animations use no directional stretch. The user-approved no-rainbow, above-label, touch-through behavior is preserved.
- `tests/glass_motion_contract.py` and the real Android emulator smoke enforce source invariants and require `NAV_LIQUID_STRETCH_ACTIVE` while dragging, plus the original real tab navigation to Analytics. Existing FAB behavior and tests remain unchanged.
- Source/finance safety and sandbox APK build+lint passed for product-code commit `491bd5b3821920ee865131e2e6a5922f0038ddb9`: runs `41797059369` and `41797059188`. Its test APK is `app.wensarafet.tracker.sandbox`, size 13,114,737 bytes, SHA-256 `d96b1cd8c9c4efa9737ebeac0fc3b78afac633ce5c3fe3626a5408ad14f74c10`; extracted from artifact `13305628293` (not a production update).
- Real Android emulator run `41797059186` confirmed nav hold, readable labels, live liquid stretch and successful Home→Analytics Hold+Drag; it failed **after** success due to a transient `adb pull` screenshot-transfer interruption. The QA transport was hardened in test-only commits `1981a525c86b855f8e843748fc1fa0691f742d66` and `df71dc904cbffe1d5ef0f541d186e4a9f2134332`, and smoke was retriggered as `41797876568`. Do not equate a passed APK compile with a fully green smoke run until that result is checked.
- Physical tablet/phone parity, motion feel, 60/120 Hz jank, and disabled-motion accessibility still need hands-on verification before merging into the protected production app.



## ✅ 4 نوفمبر 2026 — إصلاح العدسة فوق النص من دون اعتراض اللمس (مُثبت بالمحاكي)

Latest tested functional/UI commit: `f4f5177b459d42e6db53fc2eb91bde2c340e6d55` on `design/wen-full-unlocked-sandbox-20261101`, PR #28 Draft. The original financial application, production signature, protected code, SQLite and business logic were not modified by this visual/gesture batch.

**التعديلات والنتائج المؤكدة:**
- The native material is now composited **above** navigation labels/icons and samples the actual opaque tab strip; the older scroll-content sample exposed page-text ghosts and a dark/black capsule. The native lens FrameLayout now returns `false` from `dispatchTouchEvent`, letting the actual underlying tab TextViews own clicks and Hold+Drag.
- Removed spectrum/pink/orange/blue chromatic rims, RGB-split GPU shaders and native chromatic aberration on both navigation and the FAB (including Canvas fallback). Light reflections are neutral, white/silver, while the lens remains optically active.
- Floating Add (+) material uses circular clipping, lighter blur/frost, with a non-clickable optical surface above the glyph. Its original Add Transaction click and accessibility label remain unchanged.
- Real emulator smoke first failed Hold+Drag until the raised optical View was made touch-through, then **passed** with event evidence: `DOWN tab=0`, `HOVER tab=1,2,3`, `UP_TARGET tab=3 from=0`, and a successful Analytics selection. Added anti-regression contracts and gesture traces.
- CI for this commit: sandbox APK build+lint `41786778149` PASS, emulator held-frame/gesture/FAB smoke `41786778123` PASS, source/finance/security `41786778254` PASS, Android debug compile `41786777977` PASS, unsigned original-package compile `41786778109` PASS.
- Sandbox APK: `app-sandbox.apk`, 13,114,737 bytes, SHA-256 `6de3e44ecf6aa49aa9aa3cdeff6308427f85e8da30128cc26832210fb51b683f`, artifact ID `13300858836`, ARM64/ARM32 native library variants, Android APK signing block present. Actual emulator screenshots, log and gesture dumps: artifact ID `13301597817`. The benchmark screenshots show the neutral lens visible during press, legible navigation, circular FAB, and Analytics after native hold-drag.

**حدود القبول:** Android emulator visual/interaction regression now passes, but actual Xiaomi Pad 5/phone visual acceptance, 60/120Hz jank/battery, TalkBack and multi-theme physical-device verification are not established. Do not call this an original-package signed release, claim perfect Apple parity or merge sandbox protection exceptions into production. This is a separate debug-key-signed sandbox install for fictional data; its signature may change across CI runs.


---


## ✅ 4 نوفمبر 2026 — طبقة زجاج أمام النص + ضغط حقيقي للزر العائم

**آخر Commit للبرمجة والاختبارات المختبرة:** `175deca62f4ff67ab21bc9a9414e09a0b5a90246`. فرع `design/wen-full-unlocked-sandbox-20261101`، PR #28 Draft. **لا دمج في main ولا تعديل لمنطق المال/SQLite.**

### الإنجاز الجديد
- `WenNavigationTrack.GlassGestureBridge.drawForeground(Canvas)` يُرسم الآن **بعد التبويبات والنصوص** في `dispatchDraw`؛ لا يعتمد على ترتيب View غير مضمون. يرسم `OpticalCrown` انعكاسًا طيفيًا متحركًا بحواف عدسة مشغّلة، ويتبع X للعدسة الحقيقية.
- `SandboxOpticalGlass.attachFab` يضيف `frontSurface` غير قابل للضغط باعتباره آخر شقيق فوق TextView +، مع الإبقاء على النص الفعلي هو مالك OnClick وإمكانية الوصول. ينشط عند Hold/Down ويتحرك مع الفيزياء الأصلية.
- تعزيز الانكسار عند التفعيل وتخفيف frosting أثناء الضغط حتى تظهر المادة، مع الحفاظ على وسط مقروء في وضع الراحة، ومن دون تخصيص Path أو Gradient داخل حلقة الرسم أو timers مستمرة.
- أُضيف اختبار FAB حقيقي على المحاكي: يلتقط `fab-before-hold.png` و`fab-during-hold.png` أثناء 13 ثانية لمس، ويفشل إذا لم تُرسم `MIT_NAV_FRONT_ABOVE_GLYPHS` و`MIT_FAB_FRONT_PRESS_DRAWN`. تم إصلاح خطأ مكان الاختبار (كان داخل معالج تعطل لا يُنفّذ في المسار الناجح)، وحمايته باختبار static.
- **نتيجة المحاكي النهائي `41748971745`:** تبويب أثناء Hold `4691/21648` بكسل متغير، FAB أثناء Hold `3025/21648`؛ نقر FAB وفتح عملية، الرجوع، السحب إلى التحليل، وإتاحة أزرار التنقل كلها PASS. تلوث شريط التنقل بالألوان الوردية `0.56%` في منطقة الفحص المحددة، وليس حكمًا بصريًا شاملًا.
- **جميع خمسة GitHub Actions نجحت:** Sandbox APK `41748971744`، source finance/safety `41748971798`، Android compile `41748971779`، unsigned production candidate `41748971981`، emulator optical smoke `41748971745`.
- ملف اختبار منفصل: `WenSarafet_AboveGlyph_20261104.apk`، 13,081,973 bytes، SHA256 `394f5afc3edf105855feac018bf7984cad42921e0f187e3a1219c7b9ec772a4e`. GitHub artifact `13285201698`. يتضمن arm64-v8a وarmeabi-v7a و4 DEX، CRC سليم. لقطات الاختبار GitHub artifact `13284308879`، بما فيها اختبار ضغط الزر.
- لم تُثبَت مطابقة كاملة لـ Apple Liquid Glass / iOS 27؛ مطلوب معايرة على التابلت/الهاتف وفحص frame pacing 60/120Hz، وتحسين البصريات وفق الفيديو المرجعي وتعليق المستخدم. لا تُدمج Sandbox في الإنتاج حتى إزالة أي تجاوزات اختبارية والحصول على قبول المستخدم.

---


## ✅ بناء تجريبي مُختبَر — المحرك الخارجي وإيماءات العدسة (2 نوفمبر 2026)

**الفرع:** `design/wen-full-unlocked-sandbox-20261101`، PR #28 Draft. آخر commit برمجي/اختبارات: `abd2e6c886be37414fd3d0d64aae452b717b3409`. لم يحدث دمج في `main`. هذه النسخة **Sandbox فقط**، وليست تحديثًا للتطبيق المالي المثبت.

### ما تغيّر
- رُبطت مكتبة `QWEA0/Liquid-Glass-Android v2.0.11` (MIT) بواجهة Android Views التجريبية عبر `SandboxOpticalGlass`، مع الاحتفاظ بعمليات الضغط والسحب الأصلية عبر `WenNavigationTrack.GlassGestureBridge` و`GlassMotion`. لا تمس SQLite، قاعدة البيانات، الأرصدة، Safe Zone، أو الحماية.
- التغير البصري أثناء Hold صار حقيقيًا: `MIT_VIEW_GLASS_ATTACHED`, `MIT_VIEW_GLASS_BACKDROP_BOUND`, `MIT_VIEW_GLASS_PRESS_ACTIVE` كلها ظهرت في سجلات المحاكي. تغير `1555/21648` بكسل حول العدسة أثناء الـHold. السحب إلى التحليل نجح بدون تغيير وظائف الوجهات.
- استُخدم `google_apis` x86_64 مع إلغاء ملفات native ARM **في APK المحاكي فقط**. ملف Sandbox المنشور يحتفظ `arm64-v8a` و`armeabi-v7a`. تم تعطيل Pixel Launcher داخل المحاكي لتفادي ANR خارج التطبيق، واستُبعدت لقطات AOSP ATD السوداء من دليل القبول.
- ملف رخصة MIT محفوظ في `docs/licenses/QWEA0_LiquidGlass_MIT.txt`، والمكتبة تضاف فقط إلى `sandboxImplementation`. مسار التجربة لا يُدمج قبل فصل أي تجاوز تجريبي للحماية.
- **نتائج CI جميعها ناجحة على نفس commit:** build Sandbox `41517102305`، smoke emulator screenshots `41517102456`، source/finance `41517102276`، Android compile `41517102436`، unsigned production candidate `41517102263`.
- ملف APK مُختبر: `app.wensarafet.tracker.sandbox`، إصدار `1.8.6-sandbox`، Artifact ID `13175602096`، SHA256 `3f9a30d389d34d4154a5014e1f9078eda376fb895aaa04b8501d85ea9c01b0b9`. حجم 13,065,589 بايت، الأرشيف سليم، يتضمن أربع dex ومكتبات ARM. قابل للتثبيت على جهاز ARM حسب بنية الملف، لكن **لم يُختبر بعد على جهاز المستخدم**.
- صور الفحص `41517102456` محفوظة في GitHub Actions Artifact `13175942014`، منها `nav-before-hold.png` و`nav-during-hold.png`. نتيجة التغير مثبتة، لكنها **ليست دليلًا على تطابق بصري كامل** مع مرجع المستخدم `1000071367.mp4`.

### المتبقي للاعتماد
- اختبار على Xiaomi Pad 5 ثم الهاتف: أداء، ثبات، ضغط/سحب، الفرق البصري مقابل مرجع Apple الفعلي، TalkBack و60/120Hz.
- تحسين عناصر الخامة/الحركة وفق الصور **دون** تكرار تعديلات gradients العشوائية؛ لا تُعلن `pixel-perfect` أو `identical iOS 27` دون مقارنة بصرية مقنعة يوافق عليها المستخدم.
- الإنتاج: المحافظة على `main` دون تغيير واعتماد نسخة Sandbox المستقلة فقط حتى يثبت الاختبار على الأجهزة.

---



## 🔴 مراجعة الفجوة البصرية — 2 نوفمبر 2026 (الحالة الحقيقية، لا ادعاء تطابق)

**ملاحظة المستخدم الأخيرة:** النسخ الأخيرة لا تشبه Liquid Glass في الفيديو المرجعي؛ لا تعتبر نجاح خمسة GitHub Actions أو تحرك آلاف البكسلات دليلاً على قبول التصميم. هذه المراجعة تتقدم على أي وصف سابق للـAPK بأنه "نهائي" أو "مطابق".

### المرجع الذي فُحص
- استُعيد الفيديو الذي أرسله المستخدم، `1000071367.mp4`، وفُحصت إطاراته. يظهر عنصر عدسي شديد العمق، مع انكسار واضح للمحتوى الخلفي وشريط ضوئي طيفي متحرك. الفيديو معنْون "iOS 27 Beta 2/3"؛ لا نفترض أن هذا تسجيل رسمي لواجهة Apple.
- مقارنة `WenSarafet_LiquidGlass_Real_Lens_Comparison_20261102.png` بالفيديو تكشف اختلافًا كبيرًا: الكبسولة في التطبيق باهتة، حولها عدة حلقات بيضاء وزرقاء، والخلفية أسفل الشريط شبه مسطحة. فشل الجودة هنا **بصري** رغم نجاح التفاعل.
- محرك Apple الداخلي مغلق؛ الهدف الواقعي مطابقة بصرية مُقاسة للصورة والفيديو المرجعي مع الحفاظ على سلاسة Android، لا زعم 1:1 بكسل-ببكسل.

### التغيير البرمجي المنفذ حاليًا (لم يحصل على قبول بصري بعد)
- **Commit `e33298eefc6f98bf0ded6b948d74770e2ee17a37`**: عوضًا عن التقاط رموز التبويبات وحدها، يأخذ `WenOpticalNavigationOverlay` لقطة RAM محدودة للمحتوى **خلف** شريط التنقل أثناء DOWN. يُخفى الشريط مؤقتًا فقط أثناء `contentRoot.draw` لمنع حلقة التقاط الذات، ويُستعاد في `finally`. لا ملفات ولا صورة كاملة للشاشة.
- `WenLiquidRuntimeOptics` أعيد بناؤه على **capsule SDF normal**؛ الانكسار أشد عند حافة الكبسولة، مع band ضوئي طيفي حساس لموضع الإصبع وpremultiplied alpha صحيح. يحتفظ بخيار Canvas fallback.
- الاختبارات الآلية: source/finance ✅، Sandbox APK ✅، Android compile ✅، unsigned original candidate ✅، emulator smoke ✅ على `e33298ee`. **Emulator run `41497289173`** أظهر `UNDERLAY_CAPTURE_OK` و`AGSL_DRAW_ACTIVE` ومقارنة 7,369 / 21,648 بكسل خلال الضغط؛ Hold+Drag وFAB Back نجحا. لكن لقطة الشاشة ما زالت بعيدة بصريًا عن المرجع (خلفية فاتحة شبه خالية من تفاصيل تنكسر)، لذلك **ممنوع تقديم الـAPK على أنه متطابق أو جاهز للاعتماد**.

### القرار الهندسي المقترح للمرحلة التالية
- لا مزيد من زيادة العتامة أو الرسم العشوائي لحلقات حول الكبسولة. ينبغي اختبار **محرك معروف مع real-time backdrop provider** بدل ترقيع محركنا اليدوي مرة بعد أخرى.
- مرشح تم فحصه أوليًا: `QWEA0/Liquid-Glass-Android`, tag `v2.0.11`, MIT، معماريته متوافقة مع Android Views، ويحتوي `LiquidGlassView`, `LiquidGlassTabBar`, `LiquidGlassFab`, `GlassLensRenderer`, دعم AGSL SDF واللمسات. الرابط: `https://github.com/QWEA0/Liquid-Glass-Android`. **لم يُدمج ولم تُنقل منه شيفرة بعد**. اختبر التبعيات والثيم/الأداء والتراخيص أولًا في Sandbox فقط.
- قبول بصري قبل تسليم جديد: مقارنة ثابتة تحت نفس الخلفية والمقاس بين (1) idle و(2) pressed و(3) dragging و(4) FAB، مقابل إطارات المرجع، وتقرير واضح للاختلاف في شكل العدسة/شدة الانكسار/اللمعة الطيفية/قراءة النص. مع فحص إطارات وسلاسة وعدم المساس بقاعدة البيانات أو Safe Zone.
- **فرع التجربة فقط:** `design/wen-full-unlocked-sandbox-20261101`, PR #28 Draft؛ ممنوع دمجه في `main` على أنه عمل مكتمل.

---


## ✅ آخر تسليم بصري مُتحقَّق — 2 نوفمبر 2026 (تجربة مستقلة فقط)

**الحالة الحالية:** تحديث Liquid Glass عامل على محاكي Android 35، مع عدسة AGSL GPU حية أثناء Hold+Drag، وواجهة FAB دائرية زجاجية، وبناء Sandbox APK مستقل. **هذا تقريب بصري متقدم على Android، وليس نسخًا مطابِقًا بكسلًا ببكسل لمحرك Apple المغلق**. لا تدمج Sandbox PIN/activation bypass في تطبيق الإنتاج أبدًا.

- **الفرع:** `design/wen-full-unlocked-sandbox-20261101`، **PR #28 Draft**؛ آخر Commit برمجي/اختبارات اجتاز الخمسة workflows معًا: `a480b7c2465e25ba5cb77f84ca5f5ef0ddb065b1`.
- **النطاق المعدل:** `glass-engine/WenLiquidRuntimeOptics.java`، `WenOpticalNavigationOverlay.java`، `WenNavigationLensDrawable.java`، `WenNavigationTrack.java`، `WenFabBubbleDrawable.java`، `WenGlassDrawable.java`، اختبارات الصور والثيم فقط. لم يُعدَّل SQLite أو المنطق المالي أو الحماية الأصلية أو Safe Zone.
- **التنفيذ:** طبقة الشريط ترسم بعد الأيقونات؛ عند اللمس تُلتقط صورة صغيرة **للتبويبات فقط**؛ AGSL RuntimeShader يغيّر إحداثيات أخذ العينات حسب بُعد البكسل عن مركز العدسة ويضيف تشتت RGB صغيرًا عند الحافة، مع تصحيح premultiplied alpha، وFrost لإخفاء النص المكرر. عند غياب تسريع AGSL، يعود إلى BitmapShader/Canvas. لا توقيت دائم في الخمول، لا PixelCopy للشاشة الكاملة، ولا Siri.
- **الانتقالات:** عدسة واحدة مرافقة للإصبع في Hold+Drag؛ زر + دائري ذو outline بيضوي دون ripple مربّع؛ حفظ onClick الأصلي وBack؛ الألوان والحواف خفّفت بعد مراجعة صورة فعليّة للمحاكي.
- **CI المثبت على `a480b7c2`:** البناء Sandbox `41492430166` ✅؛ فحوص المصدر والمالية `41492430157` ✅؛ Android compile `41492430323` ✅؛ مرشح الإنتاج غير الموقّع `41492430529` ✅؛ اختبار المحاكي الفعلي `41492430193` ✅. ملحوظة: نجاح تجميع الإنتاج لا يعني دمج هذه التعديلات إلى `main`.
- **نتيجة فحص العدسة:** مقارنة صور 1080×2400 خلال ضغط مستمر 13 ثانية: `18,414 / 21,648` بكسل تغيّر ضمن منطقة Hold؛ سجل `AGSL_DRAW_ACTIVE` موجود؛ اجتاز فتح إضافة عملية، الرجوع، الضغط المطوّل والسحب لاختيار التحليل، وإقلاع الرئيسية/العمليات/التحليلات بلا تفعيل.
- **APK:** `wen-full-app-unlocked-sandbox-apk`، Artifact ID `13163878691`، GitHub Action `41492430166`، حجم APK المستخرج **7,010,936 بايت**، SHA256 `04e888ea13d355ca81e5d5233da7d0f127926e420b3b1c4c529b349c266a21fe`، الحزمة `app.wensarafet.tracker.sandbox`، الإصدار `1.8.6-sandbox`. اختُبرت صحة ZIP ووجود APK Signing Block وManifest/DEX؛ يجب اختبار تثبيت المستخدم في الجهاز أيضًا.
- **ما بقي:** معاينة فيديو تابلت Xiaomi Pad 5 والجوال تحت Hold وDrag و120Hz؛ مقارنة مرجعية مع `1000071367.mp4`؛ قياسات P95/P99 وحرارة وبطارية؛ اختبار التنقل السريع وTalkBack والـReduced Motion على الأجهزة الحقيقية. هذا لا يساوي إطلاق Production، ولا يدّعي تطابق Apple بنسبة 100%.

---


## ✨ Liquid Glass optical reference pass — 2 November 2026

**User recordings:** `1000071366.mp4` demonstrates the sandbox's square-looking FAB shadow and weak visible lens; `1000071367.mp4` compares visual references labelled “iOS 27 Beta 2/3” with highly chromatic optical highlights. Treat those labels as labels in the video, not as an independently verified Apple software release. The user wants the **optical motion and iridescent lens** but **not Siri or the black Siri orb**.

**Official technical design guidance:** Apple WWDC25 “Meet Liquid Glass” (`https://developer.apple.com/videos/play/wwdc2025/219/`) describes lensing/refraction, context-adaptive highlights, light concentration, interaction-responsive material and preserving legibility; Apple's internal renderer is not open-source, so this Android rendering is an approximation rather than a verified pixel-perfect port.

### Changes in sandbox-only branch PR #28
- `WenNavigationTrack` now hosts a real, passive, non-clickable `WenOpticalNavigationOverlay` View **above** navigation glyphs, invalidated via its own View display list. A legacy foreground fallback remains inert when the View is attached. Keep all original RTL touch/hold-drag/click routes.
- `WenOpticalNavigationOverlay` keeps the bounded tab-only single-touch snapshot and `BitmapShader` magnification, now combining an unobtrusive translucent coating with a chromatic ring, caustic edge and specular glint; no Siri object, duplicate pill, idle ticking, fullscreen capture or PixelCopy. This is a best-effort surface sample, NOT Apple’s proprietary dynamic physical refraction or true full-screen backdrop sampling.
- `WenNavigationLensDrawable` has a restrained chromatic rim even at rest, stronger on hold on the **same** pill shape.
- `WenFabBubbleDrawable` receives an iridescent circular ring. `MainActivity.bindGlassFab` sets a circular `ViewOutlineProvider`, oval clipping, no default rectangular foreground or state-list animator, and a circular shadow to remove the faint square surrounding the bubble, retaining Add Transaction callbacks and circular reveal.
- Code commits: `9351f804`, `f1dd42c1`, `73bcbcb8`, `f458f660`, `439395f5`. Source tests and full-feature sandbox APK `41473410236` succeeded on `439395f5`. Direct Android compilation `41473410256` and finance source tests `41473410125` also succeeded. Emulator visual probe is `41473410194`, outcome must be checked before claiming actual lens parity.
- Sandbox APK SHA256: `692fe70aae0512f84d3e7961f4c82ee0d44870e03cdf20f7aa1e023a968ff6b6` (size 7,004,400 bytes) built from commit `439395f5`. Separate `app.wensarafet.tracker.sandbox`; do NOT uninstall or alter production data.

### Acceptance still pending
Check (a) new exact screenshot pixels under long-hold, (b) tablet + phone visual match with user's `1000071367.mp4`, (c) FAB genuinely circular including shadow/pressed ripple, (d) held lens follows finger with no double bubbles or input failures, (e) smooth fast RTL swipes and scroll, battery and system Reduced Motion, (f) form opens and backs out correctly. Never merge sandbox bypass logic to `main` or claim 1:1 Apple reproduction.

---



## 🧪 فحص ما قبل الدمج — عدسة أعرض وفقاعة الإضافة وأنيميشن الأيقونات وشريط شفاف (2 نوفمبر 2026)

**النتيجة: نسخة Sandbox جديدة قابلة للتجربة، لكن الدمج إلى main لم يُعتمد بعد.**

- الفرع: `design/wen-full-unlocked-sandbox-20261101`، PR #28 Draft. آخر Commit برمجي `e9f91a6506218a1db77e4b62f1b85fbf2ebd008f`، وآخر تصحيح اختبارات `e7828656d2b5ad4701d3b4035ca95b0e78ad67db`.
- العدسة أثناء الضغط: عرض الفقاعة زاد بنحو 23% عند الـHold، والتكبير البصري أوسع، مع الحفاظ على اللمس والسحب. **ما زال اختبار تصوير المحاكي `0/21648` بكسل متغيّر** عند الضغط الطويل رغم سجلات `DOWN` و`SNAPSHOT` و`DRAW_ACTIVE`. لا تعتبر العدسة مثبتة بصريًا إلى أن تُجرَّب على جهاز المستخدم.
- زر إضافة عملية: أُضيف `WenFabBubbleDrawable.java` بشكل زجاجي دائري وتمدد/ارتداد محدودان مع اللمس، وحركة `ViewAnimationUtils.createCircularReveal` تفتح نموذج «إضافة عملية» من مركز الفقاعة. **اختبار Android الفعلي أثبت فتح النموذج**: `PASS: glass FAB tap opens Add Transaction form` من التشغيل `41455692675` مع صورة `fab-add-transaction.png`.
- الافتتاح: أنيميشن للأيقونات السبعة وحدها يبدأ بعد أول `onPreDraw` بواسطة `postOnAnimation`، مرة واحدة لكل Activity، دون تحريك الصفحة أو البطاقات. سجل `ICON_ENTRANCE_STARTED count=7` موجود؛ إحساس الحركة النهائي على الأجهزة الحقيقية بحاجة مراجعة.
- العناوين: الشريط الأساسي يحمل «الرئيسية» / «كل العمليات» / «الديون» / «التحليلات» بدل تكرار «وين صرفت» على كل صفحة؛ النوافذ الفرعية تستعمل عناوينها المعروفة.
- الشفافية: الشريط مثبت **فوق المحتوى نفسه** على التبويبات الأربعة في نسخة Sandbox، وتقل عتامته عند التمرير. أُبقي شريط التنقل السفلي وإجراءات التطبيق دون تغيير.
- تحسين الأداء: بقي عرض 24 عملية في الدفعة الأولى مع «عرض المزيد» دون تغيير أي رصيد أو سجل أو قاعدة بيانات.
- تحقق CI على `e782865`: `41455692674` أمان مالي ومصدر ✅؛ `41455692678` Android compile ✅؛ `41455692634` APK Sandbox ✅؛ `41455692672` unsigned original-package candidate ✅؛ `41455692675` emulator ❌ فقط لفحص العدسة. اختبار العنوان وفتح نموذج الإضافة نجحا داخل هذا التشغيل.
- ملف APK النهائي للتجربة (وليس ترقية التطبيق الأصلي): artifact `13145539784` من تشغيل `41455692634`، مستخرج باسم `WenSarafet_Latest_Glass_Sandbox.apk`، حجمه `7,000,672` بايت، SHA256 `9f42c1296b6414756abf2e763afd97a2a74bb351e0bd10a0f7ae1620ac25baa9`.
- شروط الدمج غير المكتملة: اختبار Hold+Drag بصريًا على التابلت ثم الجوال، حركة الافتتاح عند فتح التطبيق (بدون تكرار أثناء التنقل)، شفافية الشريط أثناء التمرير وسلاسة التبديل، وعدم فقدان بيانات التطبيق الأصلي. بعد موافقة المستخدم والاختبارات الناجحة يمكن دراسة الدمج إلى `main`.

---
## 🔎 إعادة الفحص — عدسة القائمة لم تجتز القبول البصري (2 نوفمبر 2026)

**الحالة: غير جاهزة للدمج.** المستخدم أبلغ بأن العدسة والتقطيع وأنيميشن الافتتاح لا تحقق المطلوب. ممنوع وصفها بأنها نهائية اعتمادًا على نجاح APK.

- آخر HEAD برمجي: `fb2dd3149118cbaaf87d8cc8a194e2829e05b76a` على `design/wen-full-unlocked-sandbox-20261101`، وPR #28 **Draft**.
- اكتشاف مؤكّد: سجلات `WenGlassProbe` أظهرت استقبال `DOWN` والتقاط 964×147 بكسل للعلامات و`DRAW_ACTIVE` بمقدار ~1.75×. لكن المقارنة الصورية أثناء ضغط 5.2 ثانية أظهرت **0/21,648 بكسل تغيّر** داخل العدسة، وحتى مقارنة كامل الشاشة كانت شبه مطابقة.
- فشلت محاولات overlay View منفصل، رسم `WenNavigationTrack.dispatchDraw`، و`setLayerType(SOFTWARE)`. آخر محاولة تستخدم Foreground Drawable أصليًا مع `invalidateSelf`؛ اختبارها `41440131910` **فشل أيضًا** بنفس 0/21,648، فلا تدّعي أن العدسة قد أصلحت.
- إصلاحات حاضرة بالكود: الحفاظ على snapshot أثناء tick صفر للـAnimator؛ انتهاء إطار اللمس قبل بناء صفحة الوجهة؛ عرض 24 بطاقة عمليات في الصفحة الأولى للـSandbox مع `عرض المزيد`؛ أنيميشن افتتاح الأيقونات مرة واحدة، دون تحريك الصفحة.
- آخر فحوص نجاح `fb2dd314`: `41440131890` للأمان/source، `41440131821` للبناء Sandbox APK، `41440131814` للتجميع، `41440131875` للمُرشح غير الموقّع. **نجاح هذه الفحوص لا يثبت سهولة الحركة بصريًا.**
- APK مستقل للفحص فقط: artifact `13139126196` من Action `41440131821`، SHA256 للـAPK المفكوك `26de5c64a0023011428bb39640b8ab7c7b1695cc5c09df01572dd1c46b47f826`.
- المطلوب قبل الدمج: (1) اختبار بكسلات العدسة أثناء Hold أو إثبات خلل تصوير emulator بفحص توقيت SurfaceFlinger/FrameStats ثم التحقق على جهاز حقيقي؛ (2) قياس تقطيع التنقل فعليًا مع سجل إطارات، (3) مشاهدة أنيميشن الأيقونات عند أول فتح فقط وعدم تكراره عند التبويبات. لا تمس كود الحماية/المحاسبة وقاعدة البيانات ولا تدمج `main` قبل القبول من المستخدم.

---
## 🫧 المرحلة قبل الأخيرة — عدسة بصرية حقيقية + سلاسة + أنيميشن افتتاح الأيقونات (2 نوفمبر 2026)

**الفيديو المرجعي:** `1000060816.mp4` (حوالي 11.84 ثانية، شاشة 720×1616، تسجيل قرابة 19.5fps؛ ليس مقياس 120Hz داخل التطبيق).

### الأعمال البرمجية المنجزة (فرع التصميم فقط، PR #28 Draft)
- سبب فشل العدسة السابقة: `WenNavigationLensDrawable` كانت خلف الأيقونات، بينما `focusLensTab` كان يكبّر العنصر نفسه؛ هذا ليس تأثير عدسة على المحتوى.
- أُضيف `glass-engine/src/main/java/app/wensarafet/glass/WenOpticalNavigationOverlay.java`: طبقة أعلى التبويبات، غير تفاعلية ولا تظهر في قارئ الشاشة؛ تأخذ لقطة محدودة من **شريط الأيقونات فقط** عند بدء اللمس، وتطبّق `BitmapShader` و`Matrix` لتكبير البكسلات *داخل* شكل القطرة عند الـHold والسحب. لا تلتقط الشاشة الكاملة، لا خدمات مدفوعة، لا حلقات خلفية أو تخصيصات في كل إطار؛ تحرير اللقطة عند انتهاء اللمس أو فصل العرض.
- أُلغي `focusLensTab` القديم الذي كان يرفع `scaleX/scaleY` للنص والأيقونة نفسها.
- تحسين توقيت انتقال التبويب: `navigatePrimaryTab` يستعمل `postOnAnimation` في نسخة الـSandbox حتى يكمل إطار اللمس/العدسة قبل تنفيذ بناء شاشة الوجهة الثقيل؛ تبقى الاستدعاءات المالية الأصلية وأزرارها نفسها.
- حركة بدء التشغيل: `animateStartupIcons` تتحرك مرة واحدة لكل `Activity` بعد ظهور أول شاشة رئيسية، على أيقونات الشريط والعنوان وزر الإضافة فقط، **ولا تُحرَّك بطاقات الحسابات أو كل الشاشة**.
- اختبار `tests/glass_motion_contract.py` يمنع عودة تكبير الأيقونة المزيّف ويراجع وجود العيّنة والشيدر وعدم الالتقاط الحي.
- آخر Commit برمجي مختبَر: `f54d11974167b5a9af2a56bbef7d309f85a80f14`.

### الفحوص الفعلية
- GitHub Actions: فحص الأمان والمنطق المالي `41431207423` ✅؛ بناء APK sandbox `41431207147` ✅؛ تجميع Android `41431207159` ✅؛ إنتاج مُرشّح الإصدار غير الموقّع `41431207103` ✅؛ محاكي Android `41431207098` ✅.
- ملف Sandbox المبني: Artifact `13134287501`، والـAPK المستخرج حجمه `6,987,000` بايت، SHA256 `1ad7e938b175a84e742b0645923a43ac9c7112330cc0c6dfc15259e2b37124a2`.
- **حد الاختبار الحالي:** لقطتا المحاكي `nav-before-hold.png` و`nav-during-hold.png` متطابقتان بالبكسل في شريط التنقل؛ تشغيل السيناريو الآلي انتهى بنجاح، لكنه لا يثبت بصورة كافية أن **العدسة البصرية حية أثناء استمرار ضغط الإصبع**. يجب فحص فيديو على الجهاز مع Show taps قبل الموافقة على الدمج.
- لم يُعدّل كود الأرصدة أو قاعدة البيانات أو الحماية أو Safe Zone؛ ولم تُدمج التغييرات على `main`.

### شروط الإغلاق والدمج
1. **عدسة:** إبقاء الإصبع 1–2 ثانية على الرئيسية، ثم السحب فوق العمليات/الديون والتحليل؛ تظهر بكسلات مكبّرة **داخل العدسة** وتتحرك مع الإصبع بلا تغير في حجم الأيقونات الأصلية أو رجوع مزعج.
2. **سلاسة:** اختبار 10 انتقالات سريعة يمين/يسار، والتنقل بين التحليل والديون والعودة، وتأكيد عدم ظهور تجمّد محسوس أو واجهة فارغة أو وميض، مع الحفاظ على الأنيميشن الأصلي.
3. **افتتاح:** إغلاق التطبيق تمامًا ثم فتحه، ومشاهدة دخول الأيقونات فقط مرة واحدة، وعدم تكرار دخولها عند تبديل الصفحات.
4. بعد نجاح الثلاثة عمليًا على التابلت ثم الجوال، يُعاد النظر في الدمج إلى `main` بعد موافقة المستخدم؛ في الوقت الحالي يبقى PR #28 **Draft**.

---
## 🎬 تصحيح السحب والسلاسة بعد فيديو Show taps — 2 نوفمبر 2026 (آخر حالة مؤكدة)

**الفيديو المرجعي:** `1000060805.mp4` (23.94 ثانية، 720×1616، تسجيل ~11.97 إطار/ثانية). يظهر إحداثيات الإصبع وتتبّع X/Y، وهي طبقة Android Developer Options وليست رسومات التطبيق. لا يمكن استنتاج معدل إطارات التطبيق الحقيقي من هذا التسجيل وحده.

**فرع التجربة:** `design/wen-full-unlocked-sandbox-20261101`، PR #28 Draft. **أحدث Commit برمجي مصحح:** `25d63a045af9cea852d93c104fe36a7418f4acc1`؛ يجب إعادة فحص HEAD قبل الاستكمال. لم يحصل دمج إلى `main` ولم تتغير قاعدة البيانات أو FinanceRules أو عناصر الحماية.

### التشخيص الدقيق والإصلاحات

| موضع بالفيديو | الملاحظة | إصلاح الكود | التحقق المتبقي |
|---|---|---|---|
| ~4–7 ثوان | تغيرات متقطعة بين الرئيسية/الديون/التحليل أثناء النقر والسحب | تم تصحيح اتجاه RTL: السحب الفعلي **يمين ← التبويب التالي** وفق الترتيب العربي، ويسار ← السابق؛ أصبح اتجاه دخول الشاشة الجديدة موافقًا لحركة اليد | التحقق من الاتجاه في التابلت والعودة |
| ~8–12 و~18–20 ثانية | أجزاء من صفحتين تتداخل في مشهد واحد، مع توقفات مرئية | إزالة رسم صفحتين كاملتين معًا أثناء الانتقال؛ يتم التخلص من القديمة في نفس الدورة وتحريك **الجديدة فقط** بإزاحة 46dp ومدة 190ms؛ تعطيل تدرج Animation البطاقات الأساسية خلال الانتقال وعدم تحريك الهيدر بشكل مستقل عن الصفحة المنزاحة | اختبار فعلي لسلاسة 60Hz/120Hz وPerfetto، دون ضمانها مسبقًا |
| 0–23 ثانية | رؤية عدسة لؤلؤية لكن أثرها ليس شبيهًا بالعدسة المرغوبة تمامًا | في الدفعة السابقة فقاعة morph وتكبير نص/أيقونة التبويب؛ في هذه الدفعة التركيز على كفاءة الإيماءة والانتقال مع الحفاظ على وظيفة Hold، لا ادعاء محاكاة انعكاس بصري كامل | تحسين optical refraction بعد معاينة نسخة الإصلاح |
| أعلى الشاشة طوال الفيديو | خطوط سوداء رأسية وأفقية وX/Y وقيم حمراء | هذه خطوط تعقب المؤشر/Show taps من Android Developer Options؛ **لا تصلحها بإزالة عناصر من واجهة التطبيق** | لا شيء إلا إخفاؤها من الإعدادات بعد انتهاء الاختبار |

**اكتشاف من CI السابق:** محاكي Android انتهى بالفشل في run `41424924622` مع `Pixel Launcher isn't responding` بعد سحب Hold؛ هذا **تعطل بيئة المحاكي** ولا يثبت خلل تطبيق وين صرفت. لا تزعم نجاح Hold بمجرد نجاح بناء APK. اختبارات Safe Zone والمالية المصدرية نجحت على أحدث Commit `25d63a`؛ يلزم التحقق من بقية Actions والـAPK والمحاكي عند استكمال العمل.

### الحواجز

- Sandbox فقط: الحزمة `app.wensarafet.tracker.sandbox`. لا تعديل المنطق المالي أو SQLite أو حماية الإنتاج، ولا دمج بـ`main` دون تفويض مستقل.
- احفظ استمرار تمرير الصفحات عموديًا، ولا تعترض سحب قوائم `HorizontalScrollView`؛ وفي حالة اختبارات البطء لا تغيّر الحسابات للتجميل.
- افحص زر الرجوع، الضغط المطوّل، السحب خارج حدود الشاشة، تكرار السحب سريعًا، الانتقال من أول/آخر تبويب، وعدم إظهار صفحتين متراكبتين.
- استعد للنسخ المتتالية: افحص أحدث HEAD وGitHub Actions، ثم حدّث هذا السجل بدقة مع رابط ملف APK الناجح.

---

## 🆕 تحسينات الفقاعة والتنقل والسحب والهيدر — 2 نوفمبر 2026

**الحالة البرمجية:** فرع `design/wen-full-unlocked-sandbox-20261101` / PR #28 Draft؛ آخر Commit برمجي `13f7e7125d84aa539f3c923d14bad324673ee869`. هذه الدفعة مخصصة للواجهة البصرية في Sandbox فقط؛ لم يُدمج شيء في `main` ولم تتغير قاعدة البيانات أو الحسابات أو Safe Zone.

| طلب المستخدم الجديد | العلاج المنفذ | التحقق |
|---|---|---|
| الخط المستقيم أعلى فقاعة زر الرئيسية | إزالة `topLight` وخط `canvas.drawLine` من `WenNavigationLensDrawable` واستبداله بلمعة بيضوية هادئة داخل عدسة مستديرة | ✅ تعديل المصدر؛ ⏳ التأكد بصريًا على التابلت |
| الـHold لا يشبه العدسة | تكبير فعلي خفيف للأيقونة والنص تحت العدسة (`focusLensTab`)، وتبديل التركيز مع مرور الإصبع، مع المحافظة على الشكل الزجاجي والمحيط الضوئي | ✅ تعديل المصدر؛ ⏳ اختبار ملموس على الجهاز |
| التنقل بين النوافذ بالسحب | إضافة اعتراض سحب أفقي مقيّد بأربع نوافذ رئيسية في Sandbox، بمسافة 70dp للإفلات وعتبة اتجاهية 1.65، وتمرير رأسي أصلي، وحماية عناصر `HorizontalScrollView` من اعتراض الإيماءة | ✅ تعديل المصدر؛ ⏳ اختبار السحب بالحركة على الهاتف والتابلت |
| Sliding movements animation | انتقال أفقي ثنائي للصفحات بعد السحب أو النقر على تبويب، مع الحفاظ على موضع العدسة ومنع الارتداد، ورسوم محدودة المدة | ✅ تعديل المصدر؛ ⏳ قياس التقطيع الفعلي |
| تحسين الـBar العلوي بكل النوافذ | توحيد دخول الـ`mainTopBar` و`pageHeader` بتأثير حركة خفيفة، وإضافة تأثير ضغط لزر الرجوع، وضبط شفافية الهيدر في القائمة الرئيسية والتحليل والديون عند التمرير | ✅ تعديل المصدر؛ ⏳ مطابقة على كل النوافذ |
| أول تشغيل لا يقبل السحب | الاعتماد على `startupResolved && db!=null` بدل `hasStarted` غير الموثوق عند بدء التطبيق أول مرة | ✅ تعديل المصدر؛ ⏳ اختبار Launch جديد |

**قواعد السلامة:** محرك التجميل لا ينفذ قاعدة بيانات ولا يغير `FinanceRules` أو الحماية. swipe على الشاشة الرئيسية فقط ولا يستبدل Gestures الحقول والقوائم الأفقية؛ ينفذ نفس دوال التنقل الأصلية. إذا تعطلت الرسوم على جهاز أو وُفر وضع الطاقة، يبقى التنقل المباشر آمنًا. **لا يعتمد هذا الوصف على اختبار جهاز حقيقي**؛ يلزم فيديو جديد للتأكد من استجابة العدسة وانسيابية الانتقال. لا تنقل SandboxMode أو التوقيع إلى إنتاج.

**ما تبقّى:** التحقق من GitHub Actions أحدث SHA، اختبار Emulator والتنقل ذهابًا وإيابًا، تثبيت APK تجربة محدثة فوق نسخة Sandbox فقط، معاينة الشريط العلوي في كل صفحة فرعية، ومراجعة سلاسة 60/120Hz على الجهاز. ثم تحديث هذا الجدول وفق النتائج، دون إعادة ما اكتمل.

---

## ✅ استكمال الفيديو وملاحظة الـHold الدقيقة — 2 نوفمبر 2026 (أحدث حالة)

**فرع العمل:** `design/wen-full-unlocked-sandbox-20261101` / PR #28 Draft.  
**أحدث HEAD برمجي في هذه الدفعة:** `2af31cf625316ff370b9fcf1fa363ac75b945cbf`.  
**حدود العمل:** تجربة `app.wensarafet.tracker.sandbox` فقط؛ لا تعديل للمنطق المالي أو Safe Zone أو التشفير أو التوقيع ولا دمج في `main`.

### الأخطاء التي ظهرت في الفيديو `1000060777.mp4` (39.1 ثانية، نحو 16.4fps)

| الوقت | ما ظهر / الملاحظة من المستخدم | الإصلاح المنفذ | حالة التحقق |
|---|---|---|---|
| 00:04–00:10 | الضغط المطوّل في الشريط لا ينتج عدسة زجاجية واضحة؛ وعند الإفلات تعود العدسة للخلف ثم تنتقل من جديد | `WenNavigationLensDrawable` أصبح يدعم شكلًا بصريًا دائريًا متحولًا `morph` مع محيط ضوئي، و`GlassMotion.NavigationGesture` يثبّت العدسة عند التبويب المقصود قبل تنفيذ `performClick()`، ويحمل شكلها إلى الشريط الجديد ثم يهدأ دون حركة رجوع | ✅ كود؛ ⏳ اعتماد بصري على التابلت |
| 00:04–00:10 | تحرك الإصبع من مركز الأيقونة إلى حافتها أو خارج الشريط للحظة قد يجعل المؤشر غير متمركز أو يرجع أثناء السحب | حساب `fingerLensX` من الإحداثيات الحقيقية للإصبع مع ضبط حدود الـRTL؛ لا عودة للشريط القديم أثناء `ACTION_MOVE` عند الخروج الرأسي المؤقت؛ الإلغاء النهائي في `UP/CANCEL` | ✅ كود + فحص مصدر؛ ⏳ قياس لمس مباشر |
| 00:12–00:21 | ومضات وثيمات تتطلب `Activity.recreate()`، ومحتوى فاتح مؤقت | الإصلاح السابق يبقي التبديل بين الثيمات الفاتحة داخل Activity Sandbox مع حفظ موضع قائمة الثيمات؛ عند الوضع الليلي أو الإنتاج تبقى إعادة الإنشاء الأصلية | ✅ كود سابق؛ ⏳ اختبار على أجهزة مختلفة |
| 00:25–00:29 | بطاقات التحليل مزدحمة وخط المصروفات الثانوية خافت داخل الأعمدة | تحديث `comparisonCard` في Sandbox Liquid فقط إلى خلفية زجاجية فاتحة وخط داكن، عنوان أكبر، مبلغ قابل لتكييف الحجم وارتفاع 136dp وتباعد أفقي أكبر | ✅ كود؛ ⏳ مقارنة مرئية على شاشة 360dp والتابلت |
| 00:33–00:35 | سواد عند الخروج من التجربة وفتح التطبيق الأصلي الوردي | لا يُنسب إلى عدسة التجربة بلا دليل؛ يحتاج اختبار تشغيل منفصل على الجهاز | 🔍 لم يُعدّ خلل Sandbox مثبتًا |

### التنفيذ الدقيق والملفات

- `glass-engine/.../WenNavigationLensDrawable.java`: `setMorph/getMorph`، شكل متحوّل زجاجي واضح وhalo مرسوم دون تخصيص ذاكرة داخل `draw()` أو حلقة دائمة.
- `glass-engine/.../GlassMotion.java`: تحريك `morph` لمدة محدودة عند `DOWN` وارتداد عند الإلغاء؛ موضع `fingerLensX` تحت الإصبع؛ تجميد الموضع عند خروج مؤقت، وتثبيت الهدف عند `UP`؛ `finishNavigationLensHandoff` يبدأ عدسة الصفحة التالية من موضع الإفلات مباشرة.
- `app/.../MainActivity.java`: استدعاء handoff فقط عند تنقل اللمس، وتحسين بطاقات التحليل ضمن شرط `SandboxMode.isSandbox(this) && Ui.isLiquidTheme(this)`.
- `tests/glass_motion_contract.py`: فحص التشكيل البصري، مركز الإصبع، إلغاء الرجوع أثناء حركة الإصبع، استقرار الانتقال والبطاقات المقروءة.

### شروط القبول قبل الدمج

1. لمس قصير: تنقل مباشر مرة واحدة دون ارتداد أو حفظ/عملية مالية إضافية.
2. Hold لمدة 0.5–2 ثانية: شكل عدسي دائري واضح ويبقى تحت الإصبع.
3. Hold + Drag ذهابًا وإيابًا ثم إفلات فوق تبويب مختلف: اختيار هذا التبويب فقط؛ لا رجوع ولا blink عند إعادة إنشاء الصفحة.
4. الإفلات على التبويب الأصلي أو خارج الشريط، `ACTION_CANCEL`، اللمسات السريعة المتتالية: إعادة حالة طبيعية بلا stuck scale أو تغيير خاطئ.
5. وضع الحركة المعطلة والبطارية المنخفضة، RTL/اللوحي والهاتف وTalkBack: وظائف التنقل مستمرة.
6. الانتقال الرئيسية↔التحليل: قياس `gfxinfo` أو Perfetto فعليًا، لا استخلاص 60/120fps من تسجيل المستخدم البالغ ~16fps.
7. فحص Source-only / Android / Sandbox APK / Emulator Smoke، ثم تثبيت APK مستقل على التابلت ومقارنة فيديو جديد. **لا تدّعِ التطابق التام أو اكتمال M6/M7 قبل الدليل.**

**آخر فحص آلي سابق للدفعة الجديدة:** 5/5 نجاح على `4eca351`، مع نسخة APK ناجحة في الدفعة الجديدة حتى `0f36015`. فحوص `2af31cf` قيد التحقق وقت تحديث هذه الفقرة، ويجب إعادة التحقق من CI عند الاستكمال.

---

## 🆕 آخر استكمال فعلي بعد فيديو المستخدم — 2026-11-02

> هذه هي الحالة الحالية بعد الفيديو، وهي تتقدم على أي HEAD قديم أدناه. العمل فقط على نسخة `app.wensarafet.tracker.sandbox` مع بقاء PR #28 Draft، **دون دمجها أو لمس التطبيق الأصلي**.

**مرجع الفيديو:** `1000060777.mp4`، مدته 39.10 ثانية، مقاس 720×1616، وتسجيله نحو 16.4 إطار/ثانية (غير صالح لقياس 60/120fps). **آخر HEAD برمجي فُحص:** `94594296591d766511d859039131f85e01ebc1e0`، ثم تحديث توثيقي على `4eca3510ecb874f97447220ce1341497e1722e41`. المرجع الحي: [PR #28](https://github.com/amroalhamedi2022-hue/Wen-sarafet/pull/28). افحص أحدث HEAD وActions قبل أي دفعة إضافية.

### نتائج الفيديو وتتبّع العمل

| التوقيت | الملاحظة | النتيجة |
|---|---|---|
| 00:00–00:11 | مؤشر Liquid يتحرك بين التبويبات أثناء الضغط والسحب؛ الكود كان يعيد مؤشر الصفحة الجديدة إلى التبويب القديم حتى بعد الإفلات | ✅ تعديل بداية العدسة إلى التبويب المُفلَت عليه، ⏳ اختبار بصري على الجهاز |
| 00:12–00:17 (~15 ثانية) | شاشة شبه فارغة أثناء اختيار ثيم آخر | ✅ عُولج سبب `recreate()` في تبديل الثيمات الفاتحة داخل Sandbox؛ يلزم اختبار إعادة التصوير |
| 00:19–00:21 | مساحات بيضاء واضحة أثناء انتقال الإعدادات/الرئيسية بعد تغيير الثيم | ✅ تعديل تبديل اللوحة دون إخفاء الشاشة بــalpha أو إعادة إنشاء Activity عند بقاء وضع الإضاءة نفسه |
| 00:25–00:29 | بطاقات التحليل مزدحمة نسبيًا وملاحظات المقارنة صغيرة | ⬜ تحسين بصري متبقٍ يتطلب معايرة الهاتف والتابلت دون تغيير الحسابات |
| 00:33.5–00:34.3 | شاشة سوداء أثناء الانتقال إلى تطبيق آخر ذي شاشة دخول وردية | 🔎 دليل غير كافٍ لاتهام Sandbox؛ افحص تشغيل التطبيق الأصلي على حدة |

### التعديلات الفعلية

- تحديث `MainActivity.java`: تبديل ثيم Sandbox دون Activity restart عند ثبات الوضع الليلي/الفاتح، مع تحديث الخلفية وأشرطة النظام، والاحتفاظ بموضع `HorizontalScrollView`، وعدم إضافة كشف/حفظ عملية مالية.
- إضافة علم `pendingPaletteSwitch` لعرض الشاشة الجديدة مباشرة دون ومضة خلفية، ومنع snap-back للعدسة بعد Hold/Drag بواسطة `lensStart` عند إعادة بناء الشريط؛ تنقل إمكانية الوصول يظل متحركًا طبيعيًا.
- تحديث اختبارات `tests/glass_motion_contract.py` و`tests/glass_integration_preflight.py` وإضافة `tests/sandbox_theme_switch_contract.py`.
- تحقق `source-only finance & safety` بنجاح في [run 41417839865](https://github.com/amroalhamedi2022-hue/Wen-sarafet/actions/runs/41417839865) على كود آخر تعديل؛ وقت حفظ هذه الملاحظة لم تكن كل نتائج APK/emulator قد اكتملت، فلا تُقدَّم على أنها ناجحة قبل فحصها.

### الباقي للقبول النهائي

1. فحص آخر Actions على أحدث HEAD، وتأكيد APK التجربة والحزمة والتوقيع، ثم اختبار التثبيت على التابلت والجوال دون إزالة تطبيق الإنتاج.
2. مقارنة ثيمات Liquid الفاتحة فيديو قبل/بعد ومعايرة تغييرات `Ui.DARK` التي ما زالت تستخدم إعادة الإنشاء الآمنة. اختبار 20 نقرة سريعة، إلغاء اللمس، Hold+Drag، Back، تقليل الحركة وTalkBack.
3. مراجعة كل عنصر من M3–M6، خصوصًا typography والبطاقات والتحليل، ثم قياسات الأداء الحقيقية P95/P99 والطاقة على الأجهزة وفق M7؛ لا تعتبر تسجيل 16fps دليلًا على سلاسة التطبيق.
4. لا دمج للإنتاج ولا مساس بـ FinanceDbHelper/FinanceRules/SQLite/التوقيع/التشفير حتى يتم اختبار الأجهزة والموافقة المستقلة.

---

## ✅ الحالة المعتمدة للاستكمال — 2 نوفمبر 2026 (اقرأ هذا أولًا)

**المهمة:** تطوير ثيمات وحركات Liquid Glass في تطبيق «وين صرفت — تجربة»، مع تجهيز الدمج الآمن مستقبلًا **دون تنفيذ الدمج الآن**. هذه الفقرة هي الأحدث وتنسخ أي حالة قديمة أدناه تصف هذه المراحل بأنها «لم تبدأ».

- **المستودع:** `amroalhamedi2022-hue/Wen-sarafet`
- **الفرع الحالي:** `design/wen-full-unlocked-sandbox-20261101`
- **HEAD البرمجي المؤكد:** `85d287c700576f9684cf56b4dd819ebbfa8d3f94`
- **PR:** [#28](https://github.com/amroalhamedi2022-hue/Wen-sarafet/pull/28)، ما زال **Draft، غير مدموج**، وقاعدته `fix/wen-contact-picker-xiaomi-20261101`.
- **تاريخ التحقق:** 2026-11-02. كل نتائج GitHub Actions الخمس على HEAD أعلاه **Success**: Source-only finance/safety، Android debug compile، Sandbox APK، unsigned release candidate، real-screen emulator smoke.
- **APK التجربة:** بناؤه نجح في [Actions run 41411629168](https://github.com/amroalhamedi2022-hue/Wen-sarafet/actions/runs/41411629168)، الحزمة `app.wensarafet.tracker.sandbox`؛ نسخة تجريبية منفصلة عن الأصل. توقيع CI التجريبي قد يختلف بين الأبنية؛ لا تُزال نسخة الإنتاج ولا بياناتها.
- **اختبار Emulator:** [run 41411629334](https://github.com/amroalhamedi2022-hue/Wen-sarafet/actions/runs/41411629334) نجح؛ أداة UI Automator أجرت سحبًا من تبويب «الرئيسية» إلى «التحليل» مدته 750ms، ثم تحققت من `selected=true` للتبويب الجديد والتقطت screenshot. **هذا إثبات وظيفي للتنقل فقط، وليس شهادة خلو كامل من التقطيع أو وصول الشكل إلى 100%.**
- **مراجعة الفرق:** منذ `91fafb183768` توجد 4 Commits تشمل `GlassMotion.java` و`WenNavigationLensDrawable.java` ومواضع واجهة `MainActivity.java` و`Ui.java` والاختبارات والتوثيق فقط؛ لم تتغير ملفات محاسبة أو SQLite أو حماية في هذه الدفعة.

### المنجز الحالي بصورة دقيقة

1. المحرك الزجاجي موجود داخل `glass-engine`؛ أثر Tap/Hold وعناصر العدسة لا تعمل باستمرار بالخلفية.
2. شريط Liquid السفلي يستخدم عدسة واحدة لؤلؤية مخصصة (`WenNavigationLensDrawable`) خلف أسماء/أيقونات التبويبات، بدل أربع خلفيات منفصلة.
3. أصبح **الضغط المطوّل والسحب والإفلات للتنقل** يعمل في نسخة التجربة: تتبع العدسة الإصبع في RTL/LTR وتنفّذ `performClick()` للتبويب الهدف **مرة واحدة عند الإفلات**، مع إلغاء آمن عند خروج الحركة عموديًا أو `ACTION_CANCEL`.
4. تقليل عبء الشاشة: في وضع Sandbox + Liquid تُزال الشاشة القديمة عند عرض التالية بدل رسم صفحتين كاملتين فوق بعضهما، وتقل مدة حركة الظهور غير المرتبطة بالشريط إلى 115ms، وعناصر القائمة المتدرجة إلى عنصرين بمدة 125ms. **هذا لا يثبت حل أي انتظار من بناء بيانات التحليل أو استعلامات قاعدة البيانات.**
5. توجد خطة دمج بصري آمن مستقلة على GitHub: [`docs/design/WEN_GLASS_SAFE_INTEGRATION_PLAN.md`](https://github.com/amroalhamedi2022-hue/Wen-sarafet/blob/design/wen-full-unlocked-sandbox-20261101/docs/design/WEN_GLASS_SAFE_INTEGRATION_PLAN.md). **ممنوع نقل SandboxMode أو تجاوز التفعيل أو التوقيع أو نسخ MainActivity بالكامل إلى الإنتاج.** الدمج فقط بعد طلب موافقة صريحة.

### المطلوب في المحادثة الجديدة — فيديو المستخدم هو المدخل التالي 🎬

**لم يُرفع الفيديو في هذه المحادثة بعد.** عند استلامه في المحادثة التالية:

1. شاهد الفيديو نفسه بالكامل، واكتب ملاحظات **بتوقيتات محددة** (الثانية/الدقيقة)، مع وصف الحركة المتوقعة والحركة الموجودة، وميّز الأخطاء من التفضيلات الجمالية.
2. راقب **العدسة أثناء Hold ثم سحب الإصبع**: هل تقفز، تتأخر، تنفصل عن الإصبع، تحجب اسم التبويب، أو تختفي عندما تتغير الشاشة؟ جرّب الرجوع والتبديل السريع والتثبيت على نفس التبويب وإلغاء الحركة.
3. افحص **التقطيع** عند الرئيسية ↔ التحليل وبقية النوافذ، وقِس هل مصدره إنشاء الصفحة/عمليات مزامنة أم حركة `alpha/translation`؛ لا تغيّر المنطق المالي للتخفيف، ولا تدّعِ قياسات لم تُجرَ.
4. افحص حجم/لون/حافة/شفافية **الكبسولة الزجاجية والشريط كله** وفق المرجع الذي سيُظهره الفيديو، مع النص العربي RTL وسلامة اللمس.
5. صنّف كل تعديل في قائمة: **مشكلة مرئية → الوقت في الفيديو → الملف/المكون → الحل → اختبار القبول → الحالة**. لا تفترض تفاصيل تعديل لم يُظهرها الفيديو.
6. قبل تعديل الكود افحص **أحدث HEAD وActions وPR #28** لأن محادثة أخرى أو Codex قد يكون عمل عليها. لا تُعد Commits منفّذة ولا تنشئ فرعًا بلا ضرورة.
7. اعمل على نسخة **Sandbox فقط**. حافظ على `FinanceDbHelper`, `FinanceRules`, `DebtLedgerRules`, التحويلات، الأرصدة، التشفير، الإشعارات، الحسابات، التفعيل والتوقيع في نسخة الإنتاج كما هي، واختبر Safe Zone.
8. ابنِ APK منفصلًا متحققًا من الحزمة والتوقيع، اختبر على المحاكي ثم التابلت/الجوال، وقدّم رابط GitHub Actions بديلًا في حال `Library file not found`.
9. بعد كل دفعة حَدّث **هذا الملف بعينه** في GitHub والمكتبة، مع المنجز/المتبقي وCommit/CI الدقيق.

### الحالة المختصرة للمراحل

| المرحلة | آخر حالة مؤكدة | ما يلزم للاعتماد النهائي |
|---|---|---|
| M0 الجرد والمرجع | 🟡 جرد الكود موجود؛ فيديو المستخدم الجديد لم يُفحص | مشاهدة الفيديو وقياس أساس الأداء |
| M1 Motion engine | 🟡 مطبق ومختبر CI | فحوص 60/120Hz وإتاحة الوصول واللمسات السريعة على الجهاز |
| M2 Bottom Liquid Glass | 🟡 عدسة مشتركة، إصلاح طبقاتها، وHold+Drag نجحا على المحاكي | معايرة الشكل والسحب حسب فيديو المستخدم + اختبار الجهاز |
| M3 Tap/Hold | 🟡 مطبق جزئيًا؛ السحب في الشريط يعمل، وتغطية كل عناصر التطبيق ليست نهائية | اختبار إلغاء الحركة والضغط الطويل واللمسات السريعة على كل الشاشة |
| M4 الانتقالات والحوارات | 🟡 تخفيف رسم الشاشات مطبق في Sandbox Liquid فقط | علاج أي تقطيع باقٍ وDialog/Back/regressions |
| M5 بقية الشاشات | ⬜ تدقيق شامل متبقٍ | قائمة تفاعل لكل عنصر حسب الفيديو |
| M6 التطابق 1:1 | ⬜ غير معتمد | مقارنة بصرية فيديو وصور جهاز فعلية؛ لا ادعاء 100% |
| M7 البطارية والأداء | ⬜ قياس جهاز فعلي متبقٍ | Perfetto / gfxinfo / P95 وP99 واستهلاك البطارية |
| M8 التسليم/الدمج | 🟡 APK Sandbox موجود؛ الإنتاج غير مدموج | اختبارات قبول ثم موافقة صريحة على الدمج الآمن لاحقًا |

---


**التاريخ:** 2026-11-02  
**الحالة:** انظر قسم «الحالة المعتمدة للاستكمال» أعلى الملف؛ M1–M4 قيد التطوير وM5–M8 متبقية.  
**نطاق التنفيذ:** نسخة «وين صرفت — تجربة» المستقلة أولًا؛ ممنوع الدمج مع تطبيق الإنتاج إلا بعد الاختبار والموافقة.  
**المستودع:** `amroalhamedi2022-hue/Wen-sarafet`  
**فرع المرجع وقت إنشاء الخطة:** `design/wen-full-unlocked-sandbox-20261101`  
**HEAD البرمجي المؤكد (2026-11-02):** `85d287c700576f9684cf56b4dd819ebbfa8d3f94`  
**Pull Request:** #28 (Draft)  
**ملف المتابعة:** `docs/design/WEN_GLASS_MOTION_MASTER_PLAN_2026-11-02.md` — يُحدَّث هذا الملف في كل دفعة بدل إنشاء خطط متفرقة.

## الهدف غير القابل للتفاوض

إنشاء نظام حركة متناسق بصريًا ومستلهم من **Liquid Glass وحركة واجهات iOS الحديثة** — وليس نسخًا معتمدًا لبرمجيات Apple — لتشعر كل ضغطة وتغيير صفحة وتحديد تبويب وكشف قائمة بأنها مادية، مرنة، زجاجية، وهادئة. **الشريط السفلي الزجاجي العائم وحالات الضغط العادي والمطوّل أولويات رئيسية**. لا نضيف زينة عشوائية ولا نسمح بالتأخير في إدخال المبالغ أو حفظ العمليات.

أولوية المشروع: (1) الاستجابة الصحيحة بدون أي خلل مالي، (2) سلاسة حقيقية، (3) أقصى تطابق مع الهوية المرجعية، (4) أقل تكلفة طاقة، (5) سهولة إعادة استخدام المحرك في تطبيقات Android الأخرى.

## الوضع الأولي قبل تطوير الأنيميشن (أرشيف تاريخي لا يمثّل الوضع الحالي)

- `Ui.pressAnimation(View)` موجود لكنه يعتمد على scale بسيط: ضغط ~90ms ثم عودة 230ms في ثيمات Liquid؛ لا توجد فيزياء بصرية مشتركة، ولا تعامل شامل مع الضغط المطوّل.
- `MainActivity.setContentView(View)` يطبق انتقالًا عامًا بـ alpha وtranslationX؛ كل انتقال يُنشئ واجهة جديدة، ولذلك لا ينتقل التبويب المختار بين المواضع كتكوين زجاجي مستمر.
- `bottomNavigation()` و`navButton()` يعرضان خلفية زجاجية ثابتة للتبويب النشط، لكن **ليس هناك مؤشر زجاجي مشترك متحرك** عبر التبويبات الأربعة.
- جزء من البطاقات يستدعي `Ui.pressAnimation`، وبعض النوافذ والشاشات يستدعي `Ui.animateScreen` أو `Ui.animateChildren`، لكن التغطية والتوقيت غير موحدين.
- هناك طبقة زجاج رسومية مستقلة `:glass-engine`، وأربعة ثيمات إضافية 18–21، وعناصر تنقل عائمة للثيمات الزجاجية.
- يوجد اختبار `tests/motion_accessibility_contract.py` لضمان عدم اختفاء الشاشات عند تعطيل الأنيميشن. يجب الحفاظ عليه وإضافة اختبارات جديدة، لا إزالته.

## مبادئ المحرك

1. **Event-driven**: الحركة تبدأ باستجابة لمسة أو نقرة أو تمرير أو تغيّر حالة واجهة فعلي؛ لا loop ولا shimmer دائم ولا مؤقت GPU عند الخمول.
2. **Transform-first**: تحريك translation/scale وopacity بحذر على طبقات صغيرة؛ لا إعادة حساب قائمة العمليات أو إعادة إنشاء Path/Shader في كل frame.
3. **One animation ownership**: مكوّن واحد مسؤول عن بدء الحركة وإلغائها وإعادة حالة العنصر، لمنع تراكم Animators عند الضغط السريع.
4. **Gesture safe**: لا تُستهلك `MotionEvent` الخاصة بزر الحفظ/التمرير دون داعٍ؛ لا تتحول بداية تمرير إلى نقرة؛ الضغط المطوّل لا يُكرّر عملية مالية.
5. **Accessible**: احترام `ValueAnimator.areAnimatorsEnabled()` وإعدادات تقليل الحركة، وتوفير انتقال فوري دون ترك alpha=0 أو scale غير متزن.
6. **RTL / responsive**: حركة المؤشر والتنقل تتبع مواقع العناصر الفعلية، لا إحداثيات ثابتة؛ تعمل على الهاتف والتابلت وعلى حواف النظام.
7. **Separation**: تأثيرات الرسم والحركة لا تعتمد على SQLite أو FinanceRules أو أنواع الحسابات. التطبيق يرسل أحداث واجهة وحسب.
8. **Battery-aware**: تتوقف كل الحركة فور اكتمال التفاعل أو فقدان الشاشة للتركيز؛ جودة جرافيكس قابلة للتدرج عند توفير الطاقة والحرارة أو الأجهزة الأضعف.

## مواصفات الحركة المستهدفة (قيم بدء للمعايرة لا قيم نهائية)

| العنصر | بداية التفاعل | الحركة عند الاستجابة | الضمان |
|---|---|---|---|
| زر عادي | تحجيم بصري ~0.96 خلال 70–100ms + لمعان مركز عند اللمس | رجوع بنابض مخمّد ~180–260ms | تنفيذ نقرة واحدة فقط |
| ضغط مطوّل / Hold | تزايد عمق العدسة والالتفاف حول الإصبع أثناء الثبات | حلقة لمعان/تمدّد محدود بلا تكرار أبدي؛ عند الإفلات spring back | يستخدم `ViewConfiguration.getLongPressTimeout()` وtouchSlop؛ لا يغيّر معنى long-click الأصلي |
| زر إضافة (+) | ضغط بسيط + ضغط على البريق الزجاجي | نافذة/صفحة تنبثق من موضع الزر إن أمكن | لا يحفظ أي عملية قبل تأكيد المستخدم |
| الشريط السفلي | كبسولة زجاجية عائمة متجانسة مع الثيم | مؤشر عدسي واحد ينزلق أفقيًا بشكل spring بين 4 تبويبات؛ الأيقونة تتفاعل والصفحة تتبدل | لا يظهر شريط مزدوج؛ لا يختل عند النقر السريع |
| Scroll-hide للبار | عند التمرير لأسفل | نزول مرن + تلاشي محدود | عند التمرير لأعلى يعود بسلاسة ودون تغطية المحتوى |
| بطاقات الحسابات والديون | ضغط خفيف مع ضوء موضعي بسيط | صفحة التفاصيل تظهر بـ shared-motion عند الإمكان | الأرقام لا تتبدل بصريًا بشكل مضلل |
| فلاتر/أقسام/شرائح اختيار | pill ينتقل بين الحالات | morph خفيف للحافة واللون والمساحة | تغيير حالة فعلي وليس أنيميشن بلا معنى |
| القوائم والعمليات | الضغط على صف واحد فقط | انتقال محدود للعنصر المختار | لا تُحرّك كل عناصر القائمة أثناء scroll |
| Dialog/Bottom sheet | انفتاح من العنصر المصدر | scale/translate/opacity ناعم، وخلفية معتمة قليلًا | Back وإلغاء اللمس يعملان دائمًا |
| الحقول | Focus ring/underline خفيف | 100–180ms | لا تهتز لوحة المفاتيح ولا يقفز موضع المؤشر |
| نجاح/خطأ | حالة حقيقية بعد العملية | نجاح discreet pulse، وخطأ اهتزاز بصري بسيط غير متكرر | لا يُعرض نجاح قبل حفظ العملية فعليًا |
| الرسم البياني | عند فتح صفحة التحليل | رسم أولي قصير فقط إذا لم يضر القياس | لا تعاد الأنيميشن عند كل Scroll أو تغيير غير متعلق بالبيانات |
| اختيار الثيم | تغيير مقصود | crossfade قصير للألوان أو سطح الزجاج | تُلغى الحركات المتعارضة وتحترم البطارية |

## خطة التنفيذ بالترتيب — مع حالة التقدم

### المرحلة M0 — تصوير الأساس وقياسات المرجع
**الحالة:** انظر الحالة المعتمدة أعلى الملف؛ النص الآتي يشرح مواصفات القبول الأصلية.  فحص أحدث `HEAD` وPR #28 وGitHub Actions؛ جرد كامل لـ`OnClickListener`, `OnLongClickListener`, `onTouch`, `Ui.pressAnimation`, transitions، ونافذة الـFAB؛ تثبيت نسخ Screenshots ولقطات فيديو للحالة الحالية على الهاتف والتابلت أو محاكي مماثل؛ تسجيل إطارات scroll والتنقل قبل التعديل. ممنوع تكرار تغييرات موجودة من محادثات أخرى.

**قبول:** وثيقة mapping واضحة لكل عنصر وتفاعل؛ baseline مراجع، وحالات غياب animations محفوظة.

### المرحلة M1 — المحرك المشترك Motion Foundation
**الحالة:** انظر الحالة المعتمدة أعلى الملف؛ النص الآتي يشرح مواصفات القبول الأصلية.  إنشاء مكوّن مستقل داخل `:glass-engine` أو `:glass-motion` (القرار بعد فحص تبعيات الحزمة)، يشمل `MotionSpec`, `GlassPressController`, `GlassSpring`, `MotionPolicy`, `MotionInspector` اختياري للـsandbox. توحيد durations / easing / cancellation / lifecycle / accessibility. لا استخدام لـPer-frame GPU snapshots ولا continuous timer.

**قبول:** اختبارات وحدة وInstrumentation لحالات DOWN/UP/CANCEL وrapid taps وscroll cancel وreduced-motion؛ العودة إلى scale=1 وalpha=1 في كل مسار.

### المرحلة M2 — الشريط السفلي Liquid Glass (أعلى أولوية)
**الحالة:** انظر الحالة المعتمدة أعلى الملف؛ النص الآتي يشرح مواصفات القبول الأصلية.  تحديث شريط «الرئيسية، العمليات، الديون، التحليل» إلى كبسولة عائمة ذات حدود لؤلؤية وانكسار بصري محدود، **ومؤشر تحديد واحد متحرك**. مطلوب تعديل معمارية `bottomNavigation` و`setContentView` بعناية حتى لا يعاد إنشاء موضع العدسة من الصفر في كل انتقال؛ يمكن فصل overlay مملوك لمضيف الشاشة أو تهيئة shared transition دون تغيير وظائف التبويبات. تتغير الألوان تبعًا للثيم، وتحافظ الإيماءات على hit targets كبيرة ومنطق scroll-hide الحالي.

**قبول:** فيديو 60fps+ يوضح انتقال المؤشر بين كل زوج تبويبات، الضغط المتكرر السريع، إخفاء/إظهار البار، دوران/تغيير حجم الشاشة، ووضع الحركة المعطلة؛ لا ومضات أو قفزات.

### المرحلة M3 — Tap & Hold لجميع الأزرار
**الحالة:** انظر الحالة المعتمدة أعلى الملف؛ النص الآتي يشرح مواصفات القبول الأصلية.  تطبيق تأثير ضغط مادي على الأزرار والبطاقات والأيقونات والعناصر التفاعلية، مع توزيع شدة التأثير بحسب نوع العنصر (لا تَحجيم كبير لصف طويل). للـHold: `ViewConfiguration.getLongPressTimeout()` و`scaledTouchSlop`، متابعة موضع الإصبع للانعكاس اللطيف فقط عند الحاجة، cancel عند سحب الإصبع أو بدء scroll. تحترم `performClick()` و`performLongClick()` وOnClick الأصلية، ولا نضيف أمرًا جديدًا لمجرد الضغط المطول.

**قبول:** سرعة الضغط ×20 على زر لا تولّد ×20 حفظًا إذا كانت هناك حماية من النقر المزدوج؛ عدم فقدان click، أو تكرار التحويل، أو تعليق شكل زر عند release/cancel؛ TalkBack وkeyboard activation تعمل.

### المرحلة M4 — انتقالات الصفحات والحوارات
**الحالة:** انظر الحالة المعتمدة أعلى الملف؛ النص الآتي يشرح مواصفات القبول الأصلية.  تحويل Fade+Slide العام إلى انتقالات متجانسة وفق نوع التنقل (تبويب، تفاصيل، رجوع، Dialog/Sheet، حفظ) مع اتجاه RTL صحيح. المزامنة مع الـNavIndicator، إلغاء الأنيميشن السابق عند سرعة التنقل، واحترام Activity lifecycle وWindowInsets.

**قبول:** العودة/back والتبديل السريع وعدم فقدان محتوى الصفحة وعدم حدوث overlay غير قابل للنقر.

### المرحلة M5 — microinteractions داخل التطبيق كاملًا
**الحالة: لم تبدأ.** تدقيق الرئيسية والحسابات وسجل العمليات والتحويلات والديون والتحليل والتصنيفات والإعدادات وإضافة العمليات والبحث والتبديل بين العملات؛ تحديد مناطق مفقودة وربطها بمكونات الحركة المناسبة. لا تُطبَّق حركة متسلسلة على كل صف عند Scroll؛ استخدام حركة موضعية فقط.

**قبول:** checklist لكل شاشة وكل نوع تحكم: touched / hold / scroll / focus / dismiss / save / error.

### المرحلة M6 — فحص الجودة البصرية 1:1
**الحالة: لم تبدأ.** تصوير نفس الصفحات في نفس حجم الجهاز وحالة البيانات المرجعية؛ comparison overlays للحدود، حجم الكبسولة، موقع العدسة، ظلالها، حواف الزجاج ومقدار الحركة ومجالات اللون. الصور المولدة بالذكاء الاصطناعي مراجع فنية، ولذلك هدف التطابق المرئي العالي جدًا مع واجهات فعلية مقروءة، وليس ادعاء نسبة 100% بكسل-ببكسل على كل الأجهزة.

**قبول:** لا مشكلات قص، لا نص غير مقروء، لا نافذة/شريط يقفز، وتوثيق الاختلافات المتبقية.

### المرحلة M7 — الأداء والبطارية ثم قبول نسخة الاختبار
**الحالة: لم تبدأ.** `Perfetto`, FrameMetrics/JankStats أو gfxinfo حسب المتاح؛ قياس على Xiaomi Pad 5 والهاتف في ثلاث حالات: خمول التطبيق ظاهرًا، تمرير طويل، وتفاعل كثيف. مقارنة الوضع العادي/موفر البطارية وGPU lens اختياري. إجراء idle test ممتد مع ضبط السطوع والاتصال والخدمات. قياس P95/P99 frame time والتقطعات، CPU/GPU/حرارة، واستهلاك الطاقة النسبي؛ تقليل أو إلغاء التأثيرات التي ترفع التكلفة دون فائدة ملحوظة.

**ميزانيات مستهدفة:** 60Hz ≈ 16.7ms للإطار، 120Hz ≈ 8.3ms؛ تُقارن النتائج بخط الأساس بدل ضمان 120fps دون قياس. لا background animation، ولا frame callback دائم. لا تُعتمد نسخة قبل اختبار تراجع وظائف المالية وSafe Zone على بيانات وهمية.

### المرحلة M8 — التسليم والتحديث المستقبلي
**الحالة: لم تبدأ.** تسليم APK مستقل من Sandbox مُتحقَّق منه مع فيديوهات مقارنة، جدول تغييرات، ورابط commit وCI. التحديث على التطبيق المحمي **مرحلة لاحقة بعد موافقة مستقلة**، بنفس هوية التوقيع الأصلية وحفظ البيانات، وليس من هذه المحادثة أثناء تحسين الثيمات.

## ضوابط السلامة الوظيفية

- ممنوع تعديل حساب الرصيد أو العملات أو التحويلات أو الديون أو SQLite أو الإشعارات أو التشفير أو التفعيل الأصلي أثناء تنفيذ خطة الحركة.
- `app.wensarafet.tracker.sandbox` هو الهدف الوحيد للاختبارات، ولا يُستبدل تطبيق الإنتاج.
- لا تشغيل دائم لمؤثرات الضباب/الفقاعات؛ يمكن توليد بريق عند لمسة واحدة، ثم تحريره.
- منع تأخير حدث الحفظ نفسه أو إعادة الضغط غير المقصود؛ أنيميشن نجاح العملية يأتي **بعد** إشارة نجاحها الفعلية.
- إن تعارض اللمس مع `OnTouchListener` موجود، يجب دمج المستمع بآمان بدل الكتابة فوقه.
- ترخيص وأصول التصاميم مفتوحة المصدر تُراجع قبل التوزيع. أدوات iOS تُستخدم كمصدر إلهام بصري، لا نسخ برمجي.

## لوحة متابعة التنفيذ (تُحدَّث في كل دورة)

| المهمة | الحالة | الدليل المطلوب |
|---|---|---|
| M0 جرد + baseline | 🟡 يحتاج فحص فيديو جديد وقياسات جهاز | التقرير/اللقطات |
| M1 Motion engine | 🟡 مُنفذ ومجتاز CI؛ يحتاج قياسات جهاز | commit + tests |
| M2 Liquid bottom bar | 🟡 عدسة وHold+Drag ناجحان على محاكي؛ الشكل يحتاج ضبط | video + screenshots |
| M3 Tap & Hold | 🟡 الشريط يدعم السحب؛ بقية التطبيق قيد التدقيق | automated touch tests |
| M4 Navigation / dialogs | 🟡 تحسين انتقال صفحات Sandbox؛ الحوارات متبقية | regression + video |
| M5 جميع الشاشات | ⬜ لم تبدأ | screen-by-screen checklist |
| M6 التطابق البصري | ⬜ لم تبدأ | visual overlay |
| M7 الأداء والبطارية | ⬜ لم تبدأ | device profiler evidence |
| M8 APK sandbox النهائي | 🟡 APK تجريبي مبني؛ التسليم النهائي لاحقًا | CI + install + release notes |

**قاعدة التحديث:** عند كل دفعة تطوير يجب تعديل هذا الملف ذاته: تاريخ التحديث، HEAD الجديد، ما أُنجز فعليًا، نتائج الاختبارات، وما بقي — دون إعلانات نجاح غير مدعومة، ودون إعادة تنفيذ عمل تمت إضافته من محادثة أو عامل آخر.