# Za Snake — Atomic Paid Wallet Settlement (WIP)

هذه مسودة معزولة، **ليست مرتبطة بخدمة Billing المنشورة**. لا يتم تعديل `backend/main.py` ولا تشغيل الشراء.

## ما أُضيف
- `backend/atomic_wallet_settlement.py`: finalize داخل معاملة Firestore واحدة لتغيير `purchases/{tokenDigest}` من `reserved` إلى `consumed` وإضافة مبلغ مشتريات Google Play المؤكد إلى `paid_wallets_v1/{hash(playerId)}`.
- التحقق من Player ID وProduct ID و obfuscated account hash، مع منع تكرار credit عبر purchase token digest.
- إجراء revoke محكوم لمستهلك داخلي بعد التحقق من Google Play، دون أي endpoint عام.
- `current_net_grants`: ملخص منح غير ملغاة للاستخدام المستقبلي بعد المصادقة.
- اختبارات preflight مستقلة في `backend/tests/test_atomic_wallet_settlement.py`.

## أهم المتطلبات المتبقية
1. **لا تستخدم finalize إلا بعد تأكيد الاستهلاك** من Google Play، بما في ذلك حالات فقدان رد الشبكة؛ لا يكفي أن العميل يقول `PURCHASED`.
2. **يجب دمج هذا المنطق مع `backend/main.py` بتصميم حالات واضحة** قبل أي نشر. حاليًا `/claim` لا يستدعيه.
3. يجب اختبار المعاملات الحقيقية/المتزامنة في Firestore Emulator، بما فيها retries والسباقات والحجز المنقطع والاسترداد أثناء المعالجة.
4. معالجة استرداد شراء قبل الائتمان ما زالت بحاجة إلى تسوية آمنة، إلى جانب partial refunds وRTDN وVoided Purchases API وreconciliation.
5. التصميم الحالي `paid_wallets_v1` ذو مستند واحد محدود الأحداث، ولن يصلح دون توسعة لسجل طويل الأمد.
6. الرصيد الحالي في Android يمزج عملات مجانية محلية/سحابية؛ لا تعتبره مصدرًا موثوقًا لصرف العملات المدفوعة. يلزم فصل أو توحيد الاقتصاد بشكل server-authoritative وسجل مشتريات العناصر.
7. بوابة `zasnake-billing-gateway-staging` ما زالت Private + Disabled؛ لا تفعّلها أو تفعّل checkout.
8. اختبار المستخدم السابق `27 tests / OK` كان قبل هذه المسودة، ويغطي وحدات فقط، وليس Firestore integration أو Google Play.

## اختبار هذه المسودة في Cloud Shell
```bash
cd ~/zasnake-gateway-source
git fetch origin feature/billing-atomic-claim-wallet
git switch --detach origin/feature/billing-atomic-claim-wallet
cd backend
source .venv/bin/activate
python3 -m unittest discover -s tests -v
```

**لا تعمل deploy من هذه الملفات بعد.** تحقق أولًا من نتائج الاختبارات، ثم صمّم اختبارات واقعية للمعاملات والتسوية واسترداد الأموال.
