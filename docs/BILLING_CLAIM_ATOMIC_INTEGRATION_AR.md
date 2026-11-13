# Za Snake — اختبار ربط /claim بالمحفظة (مسودة معطلة افتراضيًا)

## ما تغير
- `backend/main.py` يستورد `atomic_wallet_settlement`، ويعتمد `finalize_consumed_purchase` بدلاً من تحديث حالة الشراء بشكل منفصل.
- لا يحدث أي credit إلا بعد فحص Google Play ProductPurchaseV2 والتحقق من اللاعب والمنتج والكمية، ثم تأكيد consume أو حالة consumed لحجز موجود سابقًا.
- يصبح `/grants` معتمدًا على محفظة `paid_wallets_v1` لا على مجرد حالة المستند في purchases.
- كلتا النقطتين `/claim` و`/grants` ترفض الطلبات افتراضيًا بكود `503 billing_disabled` ما لم يكن متغير الخادم `ATOMIC_BILLING_ENABLED=true`. **لا تفعّل هذا المتغير** قبل استكمال جميع متطلبات الأمان والتسوية.
- تحديث Dockerfile لينسخ وحدات المحفظة الثلاث إلى الحاوية.
- اختبارات preflight مع استبدال اتصال Play وFirestore بـMocks.

## مخاطر ما زالت مفتوحة
1. **لا يوجد تكامل end-to-end مع Firestore Emulator أو Google Play**. لا يُستخدم التصميم بعد لاستهلاك أموال حقيقية.
2. `/session` ما زال يعتمد على رد server auth code من Play Games؛ التحقق من توافق OAuth يحتاج هاتف اختبار.
3. لا يوجد بعد RTDN ولا Voided Purchases API ولا مهمة reconciliation. عدم تعديل المستردات آليًا يترك منحًا غير صالحة.
4. عملات اللعب المجانية ما زالت محلية/سحابية وقابلة لتعديل المستخدم؛ لا ينبغي صرف العملات المدفوعة مع اعتبار تلك الأرصدة موثوقة.
5. معاملة الاستهلاك وFirestore ليست معاملة موزعة: في حالة نجاح consume وفشل Firestore، يجب الاستعادة عند إعادة الطلب والتحقق مجددًا، واختبار هذه الحالات مع فشل الشبكة وسباق الطلبات.
6. Wallet repository يعتمد على مستند واحد وحد 150 حدث؛ يتطلب إعادة تصميم قبل التشغيل الطويل.
7. البوابة Mobile Gateway ما زالت `Private + GATEWAY_ENABLED=false`، وعملية الشراء في Android معطّلة. لا تجعلها عامة أو تفعّل الدفع بسبب نجاح اختبارات الوحدات.

## تشغيل الاختبارات دون نشر
```bash
cd ~/zasnake-gateway-source
git fetch origin feature/billing-claim-atomic-connection
git switch --detach origin/feature/billing-claim-atomic-connection
cd backend
source .venv/bin/activate
python3 -m unittest discover -s tests -v
```

لا تجرِ deploy من هذا الفرع قبل اختبار Firestore Emulator والمشتريات والفشل والتزامن وتوثيق السيناريوهات.
