# Za Snake Android — Source milestone 0.1.0

هذا هو المشروع الأصلي لـ Za Snake باستخدام Kotlin وJetpack Compose. لا يشغّل ملفات HTML/CSS/JavaScript داخل WebView.

## ما يحتويه المشروع

- واجهات Compose عربية/إنجليزية: القائمة الرئيسية، اختيار الوضع، الإعدادات، اللعب، الإيقاف المؤقت، نهاية الجولة، والإحصائيات/الإنجازات بتبويبين.
- ثلاثة ثيمات مجانية للواجهة ولوحة اللعب: Night وLight وEmerald، مع حفظ الاختيار محليًا في DataStore وسكن ثعبان مستقل عن ثيم الواجهة.
- محرك لعبة Kotlin على شبكة 20×20، وتحديث بخطوة ثابتة ورسم استيفائي.
- Classic وTime Attack وZen، وثلاث درجات صعوبة، وخيار الجدران، ومتجر أشكال ثعبان بينها المجموعة العربية، ومتجر ثيمات.
- عصا تحكم عائمة؛ أسفل اللوحة في Portrait وعلى أحد جانبيها في Landscape، واليمين افتراضيًا.
- حفظ محلي ذري لجولة واحدة قابلة للاستكمال بعد إغلاق التطبيق، وإحصائيات محلية، وإعدادات DataStore.
- رصيد عملات دائم؛ كل تفاحة تمنح 1/2/4 نقاط حسب الصعوبة (سهل/متوسط/صعب)، وجولة Classic تمنح عملة لكل نقطة نهائية عند Game Over فقط، مع سجل مكافآت حسب معرّف الجولة لمنع التكرار عند الدمج السحابي.
- علامة محلية/سحابية للجولة المكتملة لمنع استعادة نفس الجولة المنتهية عند عودة الإنترنت بعد اللعب أوفلاين.
- طبقة تكامل Play Games Services v2 للمصادقة الصامتة، وSaved Games/Snapshots والإنجازات الرسمية.
- اختبارات وحدة أساسية لقواعد محرك اللعبة.
- يتضمن AdMob Rewarded **بالمعرّفات التجريبية فقط**: إكمال واحد بحماية 5 ثوانٍ بعد الاصطدام، ومضاعفة عملات Classic بإعلان منفصل مرة واحدة. راجع `docs/REWARDED_ADS_STAGE_AR.md` قبل الاختبار والنشر. لا Firebase ولا WebView.

## فتح المشروع

1. فك `ZaSnakeAndroid.zip`.
2. افتح مجلد `ZaSnakeAndroid` من Android Studio باستخدام **Open**.
3. استخدم JDK 17 وثبّت Android SDK Platform 36 وأدوات البناء المطلوبة إذا لم تكن موجودة.
4. اسمح لـ Gradle بتنزيل التوزيعة والاعتمادات عند أول Gradle Sync.
5. شغّل التطبيق على جهاز أو محاكي Android API 24 أو أعلى.

يوجد مشغّل `gradlew` و`gradlew.bat` بسيط يقوم بتنزيل Gradle 8.13 عند تشغيله أول مرة. لأن ملف Gradle Wrapper JAR الرسمي غير متوفر في بيئة إنشاء الأرشيف، شغّل من مجلد المشروع أولًا:

- Windows: `gradlew.bat --version`
- macOS/Linux: `./gradlew --version`

بعدها إما تنفّذ البناء من الطرفية باستخدام المشغّل نفسه (`gradlew.bat assembleDebug` أو `./gradlew assembleDebug`)، أو تختار في إعدادات Gradle داخل Android Studio **Local Gradle distribution / Use specified location** وتحدد المجلد الذي نزّله المشغّل. المسار الافتراضي على Windows هو `%USERPROFILE%\.gradle\wrapper\dists\gradle-8.13-bin\zasnake\gradle-8.13`، وعلى macOS/Linux هو `~/.gradle/wrapper/dists/gradle-8.13-bin/zasnake/gradle-8.13`.

## إعداد Google Play Games Services

الحفظ المحلي يعمل دون تسجيل دخول أو إنترنت. تفعيل المزامنة السحابية والإنجازات الرسمية يحتاج إلى إعداد مشروع حقيقي في Google Play Console. القيم الموجودة في `app/src/main/res/values/strings.xml` مجرد عناصر نائبة ولا تفعّل الخدمة:

- `game_services_project_id` يبدأ بالقيمة `0`.
- معرفات الإنجازات تبدأ بـ `REPLACE_WITH_PLAY_CONSOLE_ACHIEVEMENT_ID`.

اتبع `docs/PLAY_GAMES_SETUP_AR.md` قبل اختبار السحابة. لم يتم استيراد بيانات نسخة الويب؛ نسخة Android تبدأ بتقدم جديد كما اتفقنا.

## البناء والتحقق

تم تجميع وتشغيل اختبار دخان مستقل لمحرك اللعبة باستخدام مترجم Kotlin المتاح، واختُبرت قواعد اصطدام Classic، واستمرار Zen، وتجميد ساعة Time Attack، واسترجاع Snapshot. **لكن هذا لا يساوي بناء تطبيق Android كاملًا**: لم يكن Android SDK أو Gradle مثبتًا في بيئة الإنشاء، لذلك لم يتم تشغيل Gradle Sync أو بناء APK أو إجراء اختبار على جهاز. يجب إكمال هذه الخطوات في Android Studio قبل اعتبار المشروع جاهزًا للنشر.

## مواصفات التصميم

- `docs/PROJECT_SPEC_AR.md`: القرارات المتفق عليها وسلوك الشاشات والحفظ.
- `docs/THEMES_AR.md`: نطاق نظام الثيمات الحالي وخطوات اختبار القبول.
- `docs/PLAY_GAMES_SETUP_AR.md`: إعداد مشروع Google Play Games الحقيقي والاختبارات المطلوبة.

## معرّفات المشروع

- Application ID: `com.ahmedzayed.zasnake`
- minSdk: 24
- compileSdk / targetSdk: 36
- Java target: 17


## Windows wrapper note

The Windows bootstrap script avoids unescaped parentheses inside its batch `if` block. If an older extracted copy prints `... was unexpected at this time.`, replace its `gradlew.bat` with the corrected one from this archive, or remove the parentheses from the `echo Downloading Gradle ... (first run only)...` line.


## Kotlin compile fixes

This revision includes null-safety checks for nullable Play Games Snapshot results, removes the invalid top-level Compose `weight` import, restores the game screen layout direction explicitly for Arabic/English, and gives `SettingsPanel` a `ColumnScope` content receiver. Run `gradlew.bat :app:compileDebugKotlin` in Android Studio/PowerShell to verify the Android build in your configured environment.

## Shop preview and UX polish

The shop now renders actual small snake previews for Cyber, Ice, and Fire skins, offers a larger selected preview, confirms purchases before spending coins, shows insufficient-balance hints, and lets players equip already-owned skins from the shop. See `docs/SHOP_PREVIEW_UX_AR.md` for Arabic test steps.

## تحديث منطقة التحكم

أُضيف أسلوبا التحكم Floating Joystick وD-Pad، وضُبطت منطقة التحكم والشريط العلوي في الاتجاهين. راجع `docs/CONTROL_LAYOUT_UPDATE_AR.md` لتفاصيل التعديلات والتحقق.
