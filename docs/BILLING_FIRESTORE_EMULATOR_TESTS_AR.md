# Za Snake — اختبار معاملات Billing على Firestore Emulator

**الوضع:** فرع مراجعة منفصل. لا تجرِ نشرًا للخادم أو فتح البوابة، ولا توجّه أي اختبار إلى Firestore الحقيقية.

## اختبار التنفيذ الحقيقي للمعاملات
يتضمن الاختبار:
- منح الشراء مرة واحدة حتى مع إعادة المحاولة.
- رفض الشراء الذي ليس له حجز موثق.
- أربعة مطالبات متزامنة لنفس الشراء.
- استرداد شراء تم منحه، وتكرار نفس حدث الاسترداد.
- استرداد يصل بعد الحجز لكن **قبل** تسجيل العملات في المحفظة.
- منع استلام العملات باستخدام هوية لاعب أخرى.

**حارس أمان:** تستدعي الاختبارات محاكيًا محليًا فقط عند ضبط `FIRESTORE_EMULATOR_HOST` إلى `localhost:8085` أو `127.0.0.1:8085`. وإلا فإنها تظهر `skipped` بدل إجراء اتصالات إلى قاعدة البيانات الحقيقية.

## Cloud Shell — نافذة 1
```bash
gcloud emulators firestore start --host-port=127.0.0.1:8085
```
إذا لم يكن أمر المحاكي متاحًا، ثبّت مكون `cloud-firestore-emulator` إن كان gcloud يسمح بذلك، أو استخدم `firebase emulators:start --only firestore --project demo-zasnake-billing` عبر Firebase CLI بعد تهيئته. لا تستعمل Firestore الحقيقية كبديل.

## Cloud Shell — نافذة 2
```bash
cd ~/zasnake-gateway-source
git fetch origin test/billing-firestore-emulator
git switch --detach origin/test/billing-firestore-emulator
cd backend
source .venv/bin/activate
export FIRESTORE_EMULATOR_HOST=127.0.0.1:8085
python3 -m unittest discover -s tests -v
```
**النتيجة المستهدفة:** 45 اختبارًا (39 سابقة + 6 تكاملية)، من دون `skipped` للاختبارات الستة. عدد الاختبارات قد يتغير إذا أضيفت اختبارات أخرى.

بعد الاختبار:
```bash
unset FIRESTORE_EMULATOR_HOST
```
وأوقف المحاكي باستخدام Ctrl+C. لا تشارك رموز جلسات اللاعب أو purchaseToken في النتائج.

## ما لا تثبته هذه الاختبارات
- صحة عملية consume أو serverAuthCode لدى Google Play.
- التزامن بين عملية خارجية في Google Play وFirestore (لا توجد معاملة موزعة بينهما).
- RTDN وVoided Purchases API والرد الجزئي للأموال.
- سلامة صرف العملات في Android، خصوصًا دمج العملات المجانية والحفظ المحلي.
- جاهزية إتاحة Gateway للهواتف؛ تبقى `GATEWAY_ENABLED=false` وخدمة Cloud Run خاصة.

**مهم:** تم إصلاح مسار استرداد موثق يصل قبل تسجيل منح المحفظة في هذا الفرع، لكن يحتاج نتيجة الاختبار الفعلي على محاكي Firestore قبل الدمج.
