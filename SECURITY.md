# حماية وين صرفت؟ 1.6.0

## التصميم المعتمد

1. عند أول تشغيل ينشئ التطبيق زوج مفاتيح EC P-256 باسم ثابت داخل `AndroidKeyStore`.
2. يحاول استخدام StrongBox أولًا، ثم TEE/العتاد الآمن، ثم Android Keystore عند عدم توفر العتاد.
3. يرسل المستخدم رمز `WSR2-…` الذي يحتوي المفتاح العام للتثبيت فقط؛ المفتاح الخاص لا يغادر الجهاز.
4. يحسب مولّد التراخيص الخاص معرّف التثبيت من SHA-256 للمفتاح العام، ثم يوقّع ترخيص `WSL2` بمفتاح ECDSA الخاص بالمشروع.
5. يحتوي التطبيق العام مفتاح التحقق العام فقط. ويتحقق من التوقيع، وتطابق معرّف التثبيت، وصحة الحقول، وحيازة المفتاح الخاص المحلي.
6. يتحقق التطبيق أيضًا من شهادة توقيع APK ومن عدم تشغيل نسخة debuggable. يُعاد التحقق عند التشغيل والعودة للتطبيق، وتُعطّل الـWidgets والالتقاط والتذكيرات عند غياب ترخيص صالح.
7. نسخة Release تمر عبر R8 للتصغير والتمويه. ملف mapping وAPK المولّد الإداري ومفتاح توقيع Android ملفات خاصة.

## إعادة التفعيل

- تحديث APK الموقّع بالمفتاح نفسه يحافظ على مفتاح التثبيت والترخيص.
- إعادة تشغيل الهاتف لا تتطلب اتصالًا ولا تفعيلًا جديدًا.
- حذف التطبيق أو تغيير الهاتف يحذف/يغيّر مفتاح التثبيت؛ يعرض التطبيق رمز طلب جديد ويصدر المالك ترخيصًا جديدًا.
- نسخ البيانات أو ملف APK أو شفرة الترخيص وحدها إلى جهاز آخر لا ينقل المفتاح الخاص، لذلك يُرفض الترخيص.
- النسخ الاحتياطية تستثني التفعيل ورمز قفل التطبيق وPIN المحافظ عمدًا.

## حدود واقعية

الحماية الأوفلاين ترفع كلفة العبث وتمنع المشاركة العادية ونسخ الترخيص، لكنها لا تستطيع ضمان منع مهاجم متخصص يملك الجهاز والوقت من إعادة هندسة وبناء نسخة معدلة. الحماية الأقوى من ذلك تحتاج خادم تراخيص أو Google Play Integrity واتصالًا دوريًا، وهو خارج قرار العمل الكامل دون إنترنت.

## اختبارات الإصدار

يشغّل `scripts/test_core.sh` اختبارات الترخيص الصحيح، التعديل، الشفرة العشوائية، جهاز آخر، إعادة التثبيت، إعادة التشغيل، وتشفير النسخة الاحتياطية. ويشغّل `scripts/audit_release.sh` فحص التوقيع، الحزم، الصلاحيات، فصل المفتاح الخاص، وجود R8، واستثناء التفعيل من النسخ الاحتياطية.

## Current privacy boundary — 2026-11-06 owner-approved strict offline release

- The Android app MUST NOT request `android.permission.INTERNET` or `ACCESS_NETWORK_STATE`. The merged release APK permissions are inspected in GitHub Actions; the source-only guard checks for reintroduced network code.
- The app MUST NOT include any online exchange-rate fetcher, cloud-sync code or legacy bank-specific financial capture, quick actions, widgets or image assets.
- Manual FX conversion, locally encrypted backups, optional SMS/notification capture for the remaining wallets and financial ledger entries still work without network access. The document picker restricts manual backups/restores to local system storage providers.
- Android automatic backup is disabled (`allowBackup=false`) and extraction rules exclude the app's private data from cloud and device-transfer backups.
- Existing user accounts, balances, audit records and transactions MUST NOT be deleted or silently rewritten during an update. Previously existing locally named accounts can remain in the user's own database until the user explicitly renames them.
- Historical remote backup files and old public Git commits or branch snapshots are not erased by a source-level privacy update. Existing installations are NOT protected by the new policy until a verified owner-signed update is installed.
- Any future Android release that requests network access must fail CI. Do not release or sign an APK that fails the merged-permission audit.
