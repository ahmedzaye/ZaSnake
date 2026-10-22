# إعداد Google Play Games Services لنسخة Za Snake

تعمل اللعبة محليًا قبل إكمال هذه الخطوات؛ الإعداد مطلوب فقط للمصادقة السحابية والإنجازات الرسمية.

## 1) تسجيل اللعبة في Play Console

1. أنشئ/اختر مشروع اللعبة في Google Play Console وفعّل Play Games Services.
2. اربط اسم الحزمة `com.zayedart.zasnake` بالمشروع الصحيح.
3. أضف بصمة شهادة SHA-1 المطلوبة لكل مسار اختبار/إصدار حسب تعليمات Play Console.
4. جهز حسابات اختبار واستخدمها للتحقق من تسجيل الدخول وSaved Games قبل النشر.
5. فعّل Saved Games في إعدادات Play Games Services.

## 2) إدخال معرّف المشروع

افتح:

`app/src/main/res/values/strings.xml`

استبدل قيمة `game_services_project_id` من `0` بالمعرّف الرقمي الصحيح لمشروع Play Games Services. هذه قيمة مشروع Google وليست OAuth client ID.

## 3) إعداد الإنجازات

أنشئ الإنجازات المطلوبة في Play Console، ثم استبدل جميع القيم `REPLACE_WITH_PLAY_CONSOLE_ACHIEVEMENT_ID` في موارد `pgs_achievement_*` بالمعرّفات التي يصدرها Play Console. حافظ على مطابقة كل مورد للإنجاز المحلي المقابل في `GameModels.kt` و`PlayGamesBridge.kt`.

الإنجازات المحلية ستظل تعمل كجزء من سجل اللعبة عند غياب الربط، لكن إرسالها إلى Play Games لن يعمل قبل وضع المعرّفات الصحيحة واختبار الحساب.

## 4) اختبار Saved Games

- افتح اللعبة بحساب اختبار على جهاز يدعم Google Play Games.
- تحقق من المصادقة دون شاشة تسجيل دخول إجبارية.
- ابدأ جولة، أوقفها، أغلق التطبيق، ثم افتحه وتحقق من وجود الجولة المحلية.
- اختبر حفظ Snapshot واستعادته بعد حذف بيانات التطبيق على جهاز اختبار فقط.
- اختبر جهازين على الحساب نفسه: تقدم إحصائي على كل جهاز، ثم تحقق من دمج الإحصائيات وعدم تكرار عدادات الجهاز الواحد.
- أنشئ جولتين مختلفتين على الجهازين، ثم تحقق من ظهور نافذة اختيار الجولة فقط عند تعارض الجولة النشطة.
- اختبر انقطاع الإنترنت أثناء الحفظ وعودته، ولا تعتمد الإنتاج قبل التأكد من أن النسخة المحلية المعلقة لم تُفقد.

## ملاحظات

- Play Games authentication وSaved Games وAchievements خدمات منفصلة ضمن التكامل، ويجب اختبار كل منها.
- إعداد Console الحقيقي وبيانات التوقيع وحسابات الاختبار غير متاحة ضمن ملفات هذا المشروع، لذلك لا يمكن تفعيل المزامنة السحابية النهائية تلقائيًا في هذا ZIP.
- وثائق Google الرسمية:
  - https://developer.android.com/games/pgs/android/android-signin
  - https://developer.android.com/games/pgs/android/saved-games
  - https://developers.google.com/android/reference/com/google/android/gms/games/SnapshotsClient
