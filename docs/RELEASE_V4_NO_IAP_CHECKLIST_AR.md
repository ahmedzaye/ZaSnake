# Za Snake — الإصدار 4 بدون مشتريات بأموال حقيقية

## النطاق
- `versionCode = 4`, `versionName = 1.0.1`؛ الحزمة الأصلية `com.zayedart.zasnake`.
- إخفاء تبويب باقات العملات في **Release فقط**؛ متاجر الأشكال والرؤوس والأجسام والثيمات بعملات اللعب متاحة كالمعتاد.
- لا يُنشأ `CoinPriceCatalog` أو `BillingClient` في Release v4؛ يظل كود تجارب Billing متاحًا لنسخ Debug فقط.
- Manifest إصدار Release يزيل `com.android.vending.BILLING`. CI يفحص **Merged Manifest الفعلي** للتأكد.
- خدمات Cloud Run غير مفعلة للشراء؛ `ATOMIC_BILLING_ENABLED=false`، ولا يُسمح بتفعيلها لإصدار v4.
- لا توجد منتجات مدفوعة يمكن شراؤها في v4؛ العملات المكتسبة من اللعب لا تتطلب Google Play Billing.
- حاليًا تتضمن dependencies مكتبة Google Play Billing في المصدر؛ عدم تقديم الدفع يعتمد على **عدم تهيئة المكتبة في Release وإزالة الإذن واختفاء الواجهة**، ويجب فحص الـAAB قبل النشر.

## فحوصات لا بد من إتمامها قبل رفع ملف AAB
1. نجاح GitHub Actions: Debug Kotlin compilation, unit tests, `assembleDebug`, `bundleRelease` مع R8، والتحقق الآلي من الـMerged Manifest وإذن Billing. لم يتم افتراض النجاح حتى ظهور نتيجة workflow فعلية.
2. على Windows/Android Studio، حدّث `main` بعد اكتمال PR. أنشئ `Generate Signed Bundle / APK → Android App Bundle → release` **بنفس مفتاح upload keystore القديم**. لا ترفع مفتاحًا جديدًا ولا تعرض كلمات السر في السجلات/المحادثة. نسخة CI غير الموقعة ليست صالحة للرفع.
3. راجع تطبيق `versionCode 4` فوق `versionCode 3`، والتأكد من عدم ضياع الحفظ المحلي أو العناصر المفتوحة، خصوصًا عند تحديث النسخة دون مسح بياناتها.
4. اختبر على هاتف حقيقي بعد التثبيت من مسار Google Play Internal testing: أوضاع Classic/Time Attack/Zen، تحكم D-pad والJoystick، كتم الصوت، تغيير اللغة، Settings/Shop، المظهر، الإحياء، مكافآت الإعلانات، مكافآت اليوم والإنجازات.
5. اختبر CHILD (6-12)، TEEN (13-17)، ADULT (+18) على **بيئة منفصلة أو ملف مستخدم اختبار** لا يعرّض التقدم للحذف:
   - CHILD: لا تهيئة SDK الإعلانات/Play Games/Firebase؛ الإحياء ومضاعفة العملات مجانيان.
   - TEEN: لا يبدأ Billing أو Firebase App Check؛ UMP قبل عرض الإعلان، وتصنيف الإعلان G، ومعالجة المراهقين.
   - ADULT: UMP قبل عرض الإعلان، Play Games بعد العمر، Firebase App Check، ولا يظهر تبويب باقات العملات أو دفع نقدي.
   - اختبر تراجع/رفض موافقة UMP، وعدم ظهور إعلان أو مكافأة مضاعفة غير مكتسبة عند عدم توفره.
6. راجع إعداد AdMob المرتبط بحسابك: شبكات إعلانات معتمدة لسياسات Families حيث ينطبق، تصنيف المحتوى، بيانات الخصوصية، UMP المعتمدة، وإعداد أجهزة الاختبار. لا تستخدم إعلانات إنتاج فعلية لتوليد مرات عرض أثناء الاختبارات.
7. Play Games: تسجيل الدخول، جميع الإنجازات المعرّفة، حفظ/استعادة Saved Games على جهازين، تعارض الحفظ، اللعب دون إنترنت؛ تأكد من SHA-1 شهادة App Signing والتفويض للمختبرين.
8. Play Console: راجع `App content` / `Data safety` / `Target audience and content` / Ads declaration / Content rating وPrivacy Policy الحالية بناءً على السلوك الفعلي للخدمات.
9. بعد إنشاء نسخة موقّعة، ارفع إلى **Internal testing فقط أولًا**، اختبر التحديث من Play Console وشاهد Pre-launch report وAndroid vitals. لا تنشر `Production` قبل إغلاق هذه الفحوصات.

## تعليمات محلية في Android Studio (Windows PowerShell)
```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug :app:bundleRelease
```
> المشروع في GitHub يفتقد حاليًا `gradle/wrapper/gradle-wrapper.jar`. إذا لم يعمل `gradlew.bat`، استخدم Gradle 8.13 محليًا أو أعِد الملف الرسمي الموثوق إلى المشروع قبل تشغيل الـwrapper، دون تنزيل JAR من رابط غير موثوق.

**بوابات منع النشر:** أخطاء بناء Release، إذن `BILLING` في الـMerged Manifest، عرض باقات العملات المدفوعة في الإصدار، فشل موافقة إعلانات Children/Families، فشل التحديث دون فقد التقدم، أو فشل التوقيع بنفس مفتاح الرفع.

## عناصر مؤجلة للإصدار الذي يتيح الدفع
- Play API `401 permissionDenied` في Voided Purchases.
- RTDN والمصالحة الفعلية واختبارات شراء/استرداد E2E.
- Gateway آمن بمصادقة وApp Check، صرف عملات مدفوعة مع فصل/توفيق العملات المجانية، مراجعة إساءة الاستخدام.
