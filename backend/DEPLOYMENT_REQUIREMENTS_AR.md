# Cloud Run + Firebase/Firestore: متطلبات نشر خدمة المشتريات

**لا تنشر هذا الخادم للإنتاج قبل مراجعة منطق الأمان واختبار دفعة تجريبية كاملة.** الملف `main.py` الموجود في GitHub هو نسخة أولية؛ ملف Dockerfile المضاف هنا أداة للتجهيز وليس شهادة أمان.

1. اختر المشروع `za-snake` في Google Cloud المرتبط بـ Firebase، واربط حساب فوترة صالحًا إذا طلبت Google ذلك.
2. فعّل Cloud Run وArtifact Registry وFirestore Native mode وSecret Manager وGoogle Play Android Publisher API ضمن مشروعك.
3. أعد اعتماد Play Games / OAuth مع حزمة `com.zayedart.zasnake` ومع SHA-1 من Play Console > App integrity. وأنشئ **Game server OAuth Client (Web)**؛ خزّن client ID/secret في إعدادات Cloud Run مع Secret Manager (ولا تضع السر في Android).
4. أنشئ حساب خدمة خاصًا بالخادم (least privilege). امنحه صلاحيات Firestore المناسبة فقط، واربط حساب الخدمة في **Play Console > Users and permissions / API access** بما يسمح بقراءة حالة مشتريات هذا التطبيق ومعالجتها.
5. اختبر `/session` للحصول على جلسة للاعب بعد الحصول على **serverAuthCode** باستخدام Play Games SDK، ثم اختبر `/claim` بحساب License Tester. لا تقبل طلب `/claim` بدون Bearer Session Token صالح.
6. راجع معالجة سباقات الطلبات ومعاملات Firestore، أخطاء الشبكة، OAuth exchange، سجل المعاملات، رد الأموال وعمليات Void Purchase وRTDN، وإعادة المحاولة وإزالة الأسرار من السجلات؛ يجب أن تكون عملية منح العملات فريدة لكل purchaseToken عبر الأجهزة.
7. اربط Android بالخادم عبر HTTPS، وأرسل purchaseToken إلى الخادم فقط عند `PURCHASED`، ثم اسمح بمنح العملات مرة واحدة فقط بعد نجاح تحقق الخادم. يجب أن تتم عملية acknowledgement/consume وفق ضمانات Google. تحقّق من استعادة المشتريات عند استئناف التطبيق.
8. لا تضع خدمة شراء حيّة على الإنترنت أو تجعل زر الدفع قابلًا للضغط قبل نجاح اختبارات الشراء والرفض والتعليق والاسترداد وتغيير الجهاز وفشل الشبكة.

**ملاحظة:** وجود ملف `app/google-services.json` لا يوفر Secret Manager أو Cloud Run، ولا يُعطي صلاحيات Google Play Developer API تلقائيًا.
