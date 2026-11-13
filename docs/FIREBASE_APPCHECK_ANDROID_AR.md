# Za Snake — Firebase App Check Android (Staging)

**التسجيل في Firebase Console اكتمل بالفعل:** مشروع `zasnake`، التطبيق `com.zayedart.zasnake`، مزود Play Integrity، رقم المشروع `914975720790`.

## ما أُضيف إلى كود Android
- `firebase-appcheck-playintegrity` للأجهزة التي تثبت إصدار Google Play الموقع (Release).
- `firebase-appcheck-debug` لـDebug فقط؛ لا يتم تضمين هذا المزود في Release.
- تهيئة App Check بعد بوابة العمر للبالغين فقط، مع إبقاء الصغار خارج Billing.
- إرفاق `X-Firebase-AppCheck` بكل اتصال API الخاص بالمشتريات من خدمة Android.
- خاصية Gradle `billingGatewayUrl` بدل الرابط المباشر لخادم Billing الداخلي.
- جميع مسارات checkout تبقى مغلقة افتراضيًا، وفي Release تبقى مغلقة حتى لو استُخدم خيار التفعيل.

## تحذيرات يجب تنفيذها قبل اختبار Billing
1. تطبيق Firebase App Check في Console **Registered**، لكن هذا لا يغني عن إعداد Debug Token من التطبيق المحلي وإضافته يدويًا إلى صفحة App Check → Manage debug tokens. لا تضع Debug Token في GitHub أو Gradle أو الرسائل.
2. خادم `zasnake-billing-staging` خاص بـIAM ولا تصل إليه الهواتف مباشرة.
3. بوابة `billing-gateway/` في مستودع GitHub مجرد مرحلة تجريبية؛ لم تُنشر وتكون `GATEWAY_ENABLED=false` افتراضيًا. لا تُفعّلها أو تجعلها عامة قبل مراجعة الأمان والحصص والتحقق من أنها تتصل بالخادم الخاص فقط.
4. `billingGatewayUrl` يجب أن يشير إلى بوابة منفصلة مفعّل فيها Firebase App Check وليس إلى خادم Billing الخاص.
5. لم يكتمل بعد دفتر عملات مدفوعة authoritative داخل Firestore ولا معالجة استردادات/RTDN. **لا تستخدم أموالًا حقيقية ولا تفعّل الشراء الآن**.
6. لم يُجرَ Build Android في بيئة هذه التعديلات. اختبر البناء بـ`./gradlew :app:assembleDebug :app:testDebugUnitTest` على جهازك وراجع أخطاء Kotlin إن ظهرت.

## بناء آمن، بدون شراء
```powershell
git pull origin main
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --max-workers=2
```
نفّذ هذا فقط بعد مراجعة ودمج الفرع بشكل منفصل، أو اختبر في Worktree مستقل. أي متغير يبدأ `-PbillingIntegrationEnabled=true` يجب ألا يستخدم حتى انتهاء اختبارات البوابة والمحفظة، ولا تُدخل OAuth Client Secret داخل التطبيق أبدًا.

## ملاحظة
App Check يثبت أن الطلبات خرجت من إصدار تطبيق يتوافق مع المزود، لكنه ليس مصادقة لاعب. `/session` لا بد أن يتحقق من Play Games Server Auth Code، وبقية نقاط النهاية تحتاج جلسة لاعب. يجب كذلك وجود rate limiting وaudit logs مُقللة البيانات.
