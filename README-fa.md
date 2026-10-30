# انتقال پیامک (SmsForwarder)

پیامک‌های دریافتی را طبق قالب و فیلتر دلخواه به شماره‌ی دیگری می‌فرستد.
Kotlin · Material 3 · minSdk 24 · targetSdk 35 · بدون مجوز اینترنت.

## ساخت

**Android Studio:** پوشه را باز کنید و صبر کنید Gradle همگام شود. برای تست: `Run`.

**GitHub Actions:** از تب Actions، گردش‌کار «Build APK» را اجرا کنید؛ فایل‌های APK و AAB در بخش Artifacts قرار می‌گیرند.

## امضا و انتشار در کافه‌بازار

بازار فایل **امضاشده با کلید انتشار** می‌خواهد. کلید را فقط یک بار بسازید و حتماً پشتیبان بگیرید؛ با گم‌شدنش دیگر نمی‌توانید بروزرسانی بدهید.

1. ساخت کلید (روی کامپیوتر خودتان):
   ```
   keytool -genkeypair -v -keystore release.jks -alias smsforwarder -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **روش محلی:** فایل `keystore.properties` را کنار `build.gradle.kts` ریشه بسازید (در `.gitignore` هست):
   ```
   storeFile=/مسیر/کامل/release.jks
   storePassword=...
   keyAlias=smsforwarder
   keyPassword=...
   ```
   سپس `gradle assembleRelease` → خروجی: `app/build/outputs/apk/release/app-release.apk`
3. **روش GitHub:** در Settings ← Secrets ← Actions چهار مقدار بسازید:
   `KEYSTORE_BASE64` (خروجی `base64 -w0 release.jks`)، `KEYSTORE_PASSWORD`، `KEY_ALIAS`، `KEY_PASSWORD`.
4. در پنل توسعه‌دهندگان بازار برنامه را بسازید و APK (یا AAB)، آیکون ۵۱۲، اسکرین‌شات‌ها و متن‌های `store/listing-fa.md` را وارد کنید.
5. فایل `docs/privacy-policy.html` را جایی با آدرس اینترنتی بگذارید (مثلاً GitHub Pages) و لینکش را در فرم بازار بدهید. ایمیل پشتیبانی را در آن جایگزین کنید.

### قبل از انتشار حتماً چک کنید
- READ_SMS و READ_CONTACTS اختیاری‌اند؛ بازار/گوگل‌پلی ممکن است برای دسترسی پیامک فرم توضیح بخواهد (متن آماده در `store/listing-fa.md`).
- `applicationId` (در `app/build.gradle.kts`) باید روی بازار یکتا باشد؛ اگر `ir.smsforward` گرفته شده بود، تغییرش دهید.
- برای هر بروزرسانی `versionCode` را زیاد کنید.
- روی یک گوشی واقعی (ترجیحاً دو سیم‌کارته) تست کنید: روشن‌کردن، گرفتن دسترسی، ارسال آزمایشی، دریافت پیامک واقعی با صفحه‌ی خاموش.
- قوانین فعلی بازار درباره‌ی برنامه‌هایی که دسترسی پیامک می‌خواهند را در مستندات توسعه‌دهندگان بازار بخوانید و توضیح دسترسی‌ها را (`store/listing-fa.md`) در فرم وارد کنید.

## نکات کاربری
- در برخی گوشی‌ها (شیائومی، سامسونگ، هوآوی…) باید برنامه را از بهینه‌سازی باتری مستثنی کنید؛ برنامه خودش راهنمایی می‌کند.
- ارسال پیامک هزینه دارد. متن فارسی در هر بخش حدود ۷۰ نویسه جا می‌شود؛ پیش‌نمایش قالب تعداد بخش‌ها را نشان می‌دهد.

## ساختار
```
app/src/main/java/ir/smsforward/
  MainActivity.kt   صفحه‌ی اصلی (ذخیره‌ی خودکار، دسترسی در لحظه‌ی نیاز)
  LogActivity.kt    گزارش ارسال‌ها
  SmsReceiver.kt    دریافت پیامک و اعمال فیلترها
  SentReceiver.kt   ثبت نتیجه‌ی واقعی ارسال
  Forwarder.kt      اعتبارسنجی شماره، تشخیص سیم، ارسال
  Sources.kt        خواندن مخاطبین و فرستنده‌های صندوق پیامک (اختیاری، فقط در حافظه)
  SuggestAdapter.kt پیشنهاد خودکار هنگام تایپ (شماره‌ی مقصد و لیست فرستنده‌ها)
  PickerDialog.kt   پنجره‌ی انتخاب با جستجو، تب مخاطبین/پیامک‌ها و ورود دستی
  Template.kt       رندر قالب ({sender} {body} {sim} {time} {date})
  Prefs.kt          تنظیمات و گزارش
```

## مجوز فونت
فونت وزیرمتن با مجوز OFL؛ متن مجوز در `docs/Vazirmatn-OFL.txt`.
