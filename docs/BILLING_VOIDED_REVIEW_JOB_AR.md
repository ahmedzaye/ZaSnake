# Za Snake — Cloud Run Job لمعاينة استردادات Google Play (لا تشغيل فعلي بعد)

الحالة: كود تجريبي في فرع مراجعة. **لم يتم إنشاء أو تشغيل Job ولا تغيير خدمة Cloud Run**.

## الأمان
- `backend/reconcile_dry_run.py` لا يعمل إلا بوسيط `--run`، ويستدعي دائمًا `reconcile_window(..., dry_run=True)`.
- Cloud Run Job ليس Endpoint ولا يستقبل طلبات HTTP عامة؛ تشغيل يدوي فقط، دون Scheduler.
- **مطلوب حساب خدمة جديد مخصص للقراءة** بدلاً من استخدام حساب Billing القادر على تعديل Firestore. اسم مقترح: `zasnake-voided-review@zasnake.iam.gserviceaccount.com`.
- يجب منحه أقل صلاحيات Firebase/Firestore للقراءة على مشروع `zasnake`، وربطه من Play Console بالصلاحية الضرورية لقراءة بيانات الطلبات/المشتريات فقط. لا تمنحه `roles/datastore.user` أو Editor أو Secret Manager Accessor أو run.invoker للخادم Billing.
- يجب التأكد من الصلاحيات بواسطة المسؤول في Play Console؛ مجرد وجود حساب IAM لا يعني أنه يملك الوصول للـAndroid Publisher API.
- لا تستخدم ملف مفاتيح JSON ولا OAuth Client Secret. يعتمد تشغيل Job على هوية حساب الخدمة المسندة إليه.
- **لا تشغل Python CLI على Cloud Shell** ببيانات اعتماد المستخدم أو بحساب الخدمة ذي صلاحية الكتابة؛ الاختبار المباشر يجب أن يكون من بيئة حساب الخدمة المخصص بعد المراجعة.

## ما يلزم لاختبار الاتصال بعد المراجعة
1. إنشاء حساب الخدمة الخاص بالمعاينة، ومنحه `roles/datastore.viewer` فقط للقراءة في Firestore (تحقق من الحدود الفعلية لصلاحية الدور وإعدادات قواعد البيانات).
2. إضافته لمستخدمي Play Console مع صلاحيات القراءة الخاصة بتطبيق Za Snake فقط حسب متطلبات Android Publisher API.
3. استخدام `gcloud builds submit backend/ --tag=...` لبناء صورة الحاوية من فرع المراجعة بعد الاختبارات. راجع مكان Artifact Registry والصلاحيات قبل البناء.
4. إنشاء Cloud Run Job باستخدام تلك الصورة وبحساب الخدمة الجديد وبالأمر:
   `python reconcile_dry_run.py --run --hours 24`.
   ضَع حد المهمة `--max-retries=0` و`--tasks=1` وبدون Scheduler. تتاح مهمة المعاينة يدويًا فقط.
5. لا تنفّذ Job قبل مراجعة حدود Google Play API وخطأ عدم وجود فواتير/صلاحية، والتأكد من أنه يطبع *عدادات فقط* وأنه لا يكتب أي شيء إلى Firestore.
6. لو أردت اختبار إذن الوصول أولًا، فاستعمل endpoint read-only موثّق داخل job، مع عدم إفشاء محتوى الطلبات أو نتائجها الحساسة.

## اختبار الكود الآن (لا يستخدم بيانات حقيقية)
```bash
cd ~/zasnake-gateway-source
git fetch origin feature/billing-voided-review-job
git switch --detach origin/feature/billing-voided-review-job
cd backend
source .venv/bin/activate
unset FIRESTORE_EMULATOR_HOST
python3 -m unittest discover -s tests -v
```

## عوائق تمنع الدفع الحقيقي
RTDN المستوثق عبر Pub/Sub، reconcile تلقائي وآمن مع حفظ نقاط الاستئناف، معالجة الاسترداد الجزئي، ترحيل أرصدة Android المحلية، مصادقة App Check/Play Games من الهاتف وإتمام تجارب License Tester، وفحص حدود rate limiting. `GATEWAY_ENABLED=false` و`ATOMIC_BILLING_ENABLED=false` كما هما.
