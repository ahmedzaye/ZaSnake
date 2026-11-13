# Za Snake — Google Play Voided Purchases reconciliation (مسودة)

## الوضع الحالي
نجحت الاختبارات الـ53 الموجودة في الفرع السابق، منها 47 اختبارًا تم تشغيلها و6 اختبارات Firestore Emulator تم تخطيها في ذلك التشغيل (وكانت قد نجحت سابقًا عند تشغيل المحاكي).

## الملفات الجديدة
- `backend/voided_reconciliation.py`: جلب استردادات Google Play الرسمية ضمن نافذة زمنية محدودة ودعم pagination. يفرض `type=0` للمشتريات داخل التطبيق، ويتعامل بتحفظ مع quantity-based partial refunds (يفشل ولا يخمّن).
- مطابقة purchase-token SHA-256 مع مستند `purchases` الموجود والتحقق من playerId وproductId وaccountId ومبلغ العملات.
- `dry_run=True` افتراضيًا: معاينة تعداد سجلات الاسترداد والمشتريات المطابقة، **دون أي تعديل في Firestore**.
- `backend/tests/test_voided_reconciliation.py`: ثمانية اختبارات Mock لا تتصل بـGoogle Play ولا Firestore الفعلية.

## عوائق قبل التشغيل
1. **لا توجد وظيفة مستدعاة تلقائيًا ولا Cloud Run endpoint لهذا الملف**. لم يتم نشر كود جديد.
2. خيار `dry_run=False` موجود للاختبار والتصميم فقط، ولا يجوز تشغيله ضد قاعدة حقيقية حاليًا.
3. الربط مع RTDN Pub/Sub المستوثق، Google API IAM/OAuth، المجدول الدوري، checkpointing والمراقبة غير منفّذ.
4. يلزم ضمان معالجة الاستردادات داخل عمليات آمنة بالتزامن مع `/claim` وإعادة محاولة صفحات أخفقت جزئيًا.
5. API يعيد سجلات استرداد خلال نافذة محددة؛ ينبغي تغطية آخر 30 يومًا بعمليات دورية متداخلة وليس الاعتماد على إشعارات RTDN فقط.
6. التصميم الحالي لا يحل ترحيل الأرصدة المجانية/المدفوعة، ولا يثبت التدفق الكامل لشراء Google Play أو فواتير المستخدمين.
7. `GATEWAY_ENABLED=false` و`ATOMIC_BILLING_ENABLED=false` كما هما؛ لا تفعّل الدفع الآن.

## اختبارات Cloud Shell (من غير محاكي)
```bash
cd ~/zasnake-gateway-source
git fetch origin feature/billing-voided-api-readonly-reconcile
git switch --detach origin/feature/billing-voided-api-readonly-reconcile
cd backend
source .venv/bin/activate
unset FIRESTORE_EMULATOR_HOST
python3 -m unittest discover -s tests -v
```
المتوقع 61 اختبارًا إجمالًا: 55 ناجحًا و6 skipped (Firestore). راجع نتائج كل اختبار، وليس العدد فقط.

## روابط رسمية
- https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.voidedpurchases/list
- https://developer.android.com/google/play/billing/rtdn-reference
