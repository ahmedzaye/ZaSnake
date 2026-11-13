# Za Snake — إشعارات RTDN وتسوية الاستردادات (مسودة)

## الوضع الحالي
- نجاح 45 اختبارًا، منها 6 ضد Firestore Emulator الحقيقي.
- خادم `zasnake-billing-staging` ما زال خاصًا ويستخدم كود نشر سابقًا.
- بوابة `zasnake-billing-gateway-staging` خاصة، `GATEWAY_ENABLED=false`.
- في كود Billing المستقبلي `ATOMIC_BILLING_ENABLED=false` افتراضيًا.

## ما أضفناه في هذه المسودة
- `backend/play_refund_events.py` يحوّل رسائل Pub/Sub RTDN إلى **تنبيهات تحقق فقط**، ولا يعتبر الإشعار إثبات استرداد.
- التحقق من تطابق حزمة التطبيق، وجود نوع إشعار واحد، حدود الحجم، وتجزئة `purchaseToken` بالـSHA-256 من دون تسجيل قيمة token.
- التحقق المبدئي من سجل Voided Purchases API؛ يرفض حالات `voidedQuantity` الخاصة بالاسترداد الكمي الجزئي بدل تخمين مبلغ العملات.
- `backend/tests/test_play_refund_events.py`: اختبارات للرسائل المتكررة، رفض الحزم الخاطئة، رسائل الاختبار، المدخلات غير الصحيحة، والاستردادات الجزئية غير المدعومة.

## ما لم ينفذ (عائق يمنع تشغيل المال الحقيقي)
1. لم ننشئ Pub/Sub topic/subscription أو endpoint يستقبل الطلبات. يجب التحقق من OIDC للـPub/Sub push و`audience` و`email` و`iss`، أو استخدام مصادقة IAM المناسبة.
2. لم نستدعِ Google Play Voided Purchases API بعد؛ يجب فحص نتائجها الموقّعة بتصريح Google API، وفترة التسوية، والصفحات المتعددة، ومطابقة سجل الشراء.
3. لا يوجد worker يستدعي `revoke_authoritatively_verified_purchase` تلقائيًا. يجب ضمان idempotency وحالات الجهل بشهادة الاسترداد.
4. يحتاج مسار الاسترداد الجزئي إلى تفسير Google Play الصحيح دون افتراض أن شراء الكمية 1 يستحيل استرداد جزء من قيمته.
5. لم نراجع وضع الحماية من الإساءة أو مراجعة صلاحيات الحسابات أو محفظة الصرف من Android.
6. لا تدمج أو تنشر هذا الفرع على أساس اختبارات وحدات فقط.

## اختبار هذا الفرع
```bash
cd ~/zasnake-gateway-source
git fetch origin feature/billing-voided-reconciliation-staging
git switch --detach origin/feature/billing-voided-reconciliation-staging
cd backend
source .venv/bin/activate
unset FIRESTORE_EMULATOR_HOST
python3 -m unittest discover -s tests -v
```
ستظهر اختبارات Firestore الستة بحالة skipped إذا لم يتم تفعيل المحاكي، ولذلك لا تعتبر نتيجة الاختبارات الإجمالية دون فحص skipped إثباتًا لاختبارات Firestore.

## مراجع
- Google Play real-time developer notifications: https://developer.android.com/google/play/billing/rtdn-reference
- Google Play Voided Purchases API: https://developers.google.com/android-publisher/voided-purchases
