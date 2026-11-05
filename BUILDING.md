# بناء المشروع

## Android Studio / Gradle

1. ثبّت Android Studio وAndroid SDK Platform 35 وBuild Tools 35.0.0 وJDK 17.
2. افتح مجلد المشروع وانتظر Gradle Sync.
3. للاختبار المحلي استخدم `assembleDebug`. نسخة debug لا تمر بفحص توقيع الإنتاج، وهذا متوقع.
4. لبناء Release إنتاجي مرّر أسرار التوقيع كمتغيرات بيئة، ولا تكتبها في الملفات:

```bash
export WEN_SARAFET_KEYSTORE=/secure/path/release.jks
export WEN_SARAFET_STORE_PASSWORD='...'
export WEN_SARAFET_KEY_PASSWORD='...'
export WEN_SARAFET_KEY_ALIAS='wen-sarafet-test'
./gradlew :app:assembleRelease
```

يمكن استعمال Gradle مثبت على الجهاز إذا لم يكن wrapper متوفرًا:

```bash
gradle :app:assembleRelease
```

## سكربت البناء اليدوي

السكربت الأصلي اليدوي ما زال متاحًا:

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
export SIGNING_KEYSTORE=/secure/path/release.jks
export SIGNING_STORE_PASSWORD='...'
scripts/build_apk.sh
```

بدون مفتاح التوقيع ينشئ السكربت APK غير موقع للفحص فقط. لن يثبت كتحديث فوق الإصدار الحالي ولن يجتاز `AppIntegrity`.

## مولّد التراخيص

الحزمة العامة تتضمن قالب المصدر فقط، وفيه `REPLACE_WITH_OWNER_PRIVATE_KEY`. استعد المفتاح الحقيقي من حزمة المالك الخاصة في مساحة آمنة فقط، ثم ابنِ المولّد منفصلًا. لا تضع المفتاح الخاص داخل app أو مستودع عام.

## اختبارات المنطق

```bash
scripts/test_core.sh
```
