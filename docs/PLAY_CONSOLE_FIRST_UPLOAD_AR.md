# Za Snake — تجهيز أول رفع داخلي على Google Play

## هوية التطبيق المعتمدة
- **Google Play package / applicationId:** `com.zayedart.zasnake`
- **Kotlin namespace:** `com.ahmedzayed.zasnake` (لم يتم تغييره عمدًا؛ يمكن أن يختلف عن applicationId)
- تم تثبيت أسماء `MainActivity` و`ZaSnakeApplication` كاملة داخل AndroidManifest حتى لا تعتمد على تفسير أسماء نسبية.
- معرّف التطبيق الجديد ينشئ تثبيتًا منفصلًا عن النسخة القديمة؛ لا يُنقل تقدم الاختبارات المحلية بينهما تلقائيًا.

## قبل الرفع
1. افتح المشروع من مجلد `ZaSnakeAndroid` في Android Studio وحدّث Gradle Sync.
2. نفّذ في Windows PowerShell:
   ```powershell
   .\gradlew.bat :app:compileDebugKotlin
   .\gradlew.bat :app:testDebugUnitTest
   .\gradlew.bat :app:assembleDebug
   ```
3. راجع merged manifest وتأكد أن تطبيق الحزمة هو `com.zayedart.zasnake` وأن الأنشطة متاحة.
4. لأول ملف رفع موقّع: Android Studio → Build → Generate Signed Bundle / APK → Android App Bundle → Create new upload keystore (إذا لم تكن تملك مفتاح رفع) → release → Finish. احفظ المفتاح وكلمات المرور خارج ملفات المشروع ولا تشاركها داخل المحادثات أو ملفات ZIP.
5. الملف المعتاد بعد توليد نسخة موقعة: `app/build/outputs/bundle/release/app-release.aab`، وقد يختلف الاسم حسب إعداد Android Studio.
6. في Play Console: Za Snake → Testing → Internal testing → Create new release، فعّل Play App Signing عند طلبه، وارفع ملف AAB الموقّع. استخدم قائمة المختبرين لتجربة التثبيت والتحديث عبر المتجر.

## تنبيهات قبل الإنتاج
- معرّف Google Ads الموجود ما زال TEST، وليس معرفًا حقيقيًا للنشر.
- سياسة الخصوصية/Google UMP والامتثال لفئات الأطفال لم تكتمل بعد، ويجب مراجعتها قبل النشر العام.
- `game_services_project_id` الحالي ومعرّفات إنجازات Play Games تحتاج إعداد Console وربط OAuth جديد باسم الحزمة الجديد وشهادة التوقيع المناسبة.
- شاشة Coins تعرض الباقات فقط؛ Google Play Billing والدفع الحقيقي لم يتم تفعليهما بعد.
- لا تستخدم `assembleDebug` للرفع: Google Play يحتاج AAB إصدار موقعًا.

## ما تغير في هذه المرحلة؟
- `app/build.gradle.kts`: applicationId فقط.
- `app/src/main/AndroidManifest.xml`: تحويل اسمَي Application/Activity إلى أسماء مؤهلة بالكامل.
- تحديث معرف الحزمة في الوثائق، وإضافة هذا الدليل.
- لا تغيير في Kotlin package declarations أو منطق اللعبة أو المتجر أو إعدادات المستخدم أو البيانات المخزنة.
