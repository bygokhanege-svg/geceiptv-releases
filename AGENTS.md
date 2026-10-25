# GECE IPTV - AI Agent Guidelines & Project Memory

## ⚠️ KRİTİK KURAL (MANDATORY FIRST STEP - TOKEN TASARRUFU)
Kullanıcı yeni bir istekte bulunduğunda veya bir güncelleme istediğinde:
1. **Gereksiz token harcamamak ve mükerrer araştırma yapmamak için** öncelikle [CONVERSATION_HISTORY.md](file:///c:/Users/EGE/Desktop/Tvapk/CONVERSATION_HISTORY.md) dosyasını kontrol edin.
2. Kullanıcının sorduğu sorular, aldığı kararlar, tasarım tercihleri (örneğin telefon ve TV arayüz ayrımı, buton yerleşimleri, iletişim bilgileri vb.) bu dosyada detaylıca belgelenmiştir.
3. Kullanıcıya daha önce kararlaştırılmış konuları tekrar sormayın; doğrudan `CONVERSATION_HISTORY.md` referansıyla çalışmaya başlayın.

---

## 📱 Cihaz ve Arayüz Ayrım Kuralları
* **Telefon / Tablet (`screenWidthDp < 600`):**
  * Sol dikey raylar kullanılmaz. Üst yatay kaydırılabilir çipler (`LazyRow`) tercih edilir.
  * Film/Dizi ızgaraları 3 sütunlu (`GridCells.Fixed(3)`) ve `fillMaxWidth()` olmalıdır.
  * Kartlar ve diyaloglar dikey kaydırılabilir (`verticalScroll`) ve tek sütun olmalıdır.
  * Buton metinlerinde dikey ezilmeyi önlemek için `maxLines = 1` ve `TextOverflow.Ellipsis` kullanılır.
* **Televizyon (`screenWidthDp >= 600` veya `isTv`):**
  * D-pad (kumanda) gezinmesi, odak göstergeleri (`focusRequester`, `border`), sabit sol raylar ve 5 sütunlu geniş ızgaralar korunmalıdır.
  * Ekran koruyucuyu engelleme bayrağı (`FLAG_KEEP_SCREEN_ON`) korunmalıdır.

---

## 🛠️ GitHub & Release İş Akışı
* **Repository (Private - Kod):** `https://github.com/bygokhanege-svg/geceiptv.git`
* **Release Repo (Public - Sadece APK):** `https://github.com/bygokhanege-svg/geceiptv-releases.git`
* **Destek İletişim:** `geceiptv@gmail.com` (Telefon numarası asla kullanılmaz).
* **Versiyon Güncelleme:** `app/build.gradle.kts` içinde `versionCode` ve `versionName` artırılır.
* **Release APK:** `.\gradlew assembleRelease` çalıştırılır.
  * Çıktı: `app\build\outputs\apk\release\GECEiptv-release.apk`
* **Release Yükleme:** `git push origin main`, `git tag <tag>`, `git push origin <tag>` ve GitHub Release API üzerinden APK yüklemesi yapılır (Bkz. `CONVERSATION_HISTORY.md`).
