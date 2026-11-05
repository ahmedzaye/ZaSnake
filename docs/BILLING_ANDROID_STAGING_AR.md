# Za Snake — مسودة ربط Android Billing بالخادم

**الحالة: مسودة على فرع مستقل، ليست جاهزة للنشر أو الشراء الحقيقي.**

## ما تم توصيله برمجيًا
- `PlayGamesBridge.requestServerAuthCode(webClientId)`: طلب رمز الخادم باستخدام **Web OAuth Client ID**.
- `CoinPriceCatalog`: أسعار Google Play، وخيار تنفيذ checkout عند تحقق شروط التهيئة، واستعادة المشتريات بعد عودة التطبيق.
- `/session`: إرسال رمز الخادم واستلام جلسة اللاعب و obfuscated account ID.
- `/claim`: إرسال `purchaseToken` فقط للمشتريات بحالة `PURCHASED`؛ عدم منح رصيد محليًا اعتمادًا على callback.
- `/grants`: قراءة المنح من الخادم، وحفظها في الذاكرة فقط، وعدم كتابتها في Saved Games.
- واجهة متجر العملات: زر الشراء يظل غير مفعل حتى تتوفر جلسة آمنة وإعدادات تسمح بالشراء.
- الأطفال والمراهقون: نفس قيود العمر الحالية، Billing للبالغين فقط.

## إعدادات بناء Android
لا تُدخل Web OAuth Client Secret في أي مكان داخل Android.

```powershell
.\gradlew.bat :app:assembleDebug -Pkotlin.compiler.execution.strategy=in-process --max-workers=2
```

ميزة الشراء **معطلة افتراضيًا**. لاحقًا، وعقب تجهيز بوابة API آمنة، يمكن اختبار Debug بهذه الخصائص:

```powershell
.\gradlew.bat :app:assembleDebug -PpgsWebClientId=YOUR_WEB_CLIENT_ID -PbillingIntegrationEnabled=true -Pkotlin.compiler.execution.strategy=in-process --max-workers=2
```

**لا تفعّل الخيار الثاني الآن**: خدمة Cloud Run على المشروع `zasnake` ما زالت خاصة باستخدام IAM، والهواتف العادية غير مصرح لها باستدعائها. تشغيل الخدمة الآن متاح فقط للهوية السحابية.

## عوائق الأمان المتبقية قبل أي دفع حقيقي
1. بوابة إنترنت عامة ذات مصادقة لاعب موثوقة وحماية إساءة الاستخدام/الحصص، منفصلة عن صلاحيات IAM الداخلية، مع رفض جميع الطلبات غير المعتمدة.
2. اختبارات توافق الـ OAuth (PGS Game Server server-side access) من جهاز حقيقي؛ لا يمكن التحقق من صحته عبر `/health`.
3. الخادم الحالي يستهلك الشراء ويسجل `purchases` ولكن لم يطبق RTDN / Voided Purchases بشكل موثوق (الإلغاء، استرداد الأموال، الرفض، إعادة التسوية).
4. **النقطة الأهم:** رصيد اللعبة حاليًا يعتمد على `PlayerStats` وحفظ محلي/سحابي؛ يلزم دفتر محفظة **server-authoritative** للاستهلاك، وقواعد تصحيح رصيد المشتريات الملغاة، والربط بالأجهزة؛ الاحتفاظ بالمنح في الذاكرة فقط لا يكفي لحماية اقتصاد اللعبة من التلاعب.
5. ضبط المنتجات وعدم السماح بتعدد الكميات فوق 1 (أو دعمها بشكل صريح)، وتأكيد منح العملات المتكررة بنفس `purchaseToken` مرة واحدة.
6. الاختبار على License Tester: نجاح، إلغاء، PENDING، عدم اتصال، تكرار callback، تغيير جهاز، إعادة تثبيت، استرداد/voided، انتهاء session.
7. تنفيذ `./gradlew :app:assembleDebug :app:testDebugUnitTest` على جهاز تتوفر فيه Android SDK/Gradle قبل الدمج. لم يتم إجراء بناء Android آلي على هذا الفرع داخل ChatGPT.
8. في حال تعديل `backend/main.py`، انشر نسخة جديدة من Cloud Run؛ التعديلات في GitHub وحدها لا تغيّر الخدمة القائمة.

## في النسخة الحالية
- **الإصدار 3 المنشور لا يتغير**.
- **الخادم Staging لا يتغير**.
- المشتريات الحقيقية **تبقى غير متاحة** حتى تتحقق المتطلبات أعلاه.

## تحديثات المشروع
احترم التعديلات المحلية: افحص `git status` قبل checkout. يمكن استعراض الفرع على GitHub بدل الدمج مباشرة.
