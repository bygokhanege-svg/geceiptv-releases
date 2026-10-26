# GECE IPTV - Konuşma, İstek ve Geliştirme Geçmişi (Conversation & Decision Log)

> **Önemli Kural (AI Ajanları İçin):** 
> Yeni bir istek veya güncelleme geldiğinde gereksiz token harcamamak için öncelikle bu dosyayı inceleyin. Kullanıcının daha önce sorduğu sorular, aldığı kararlar, mimari tercihler ve versiyon detayları burada kayıtlıdır.

---

## 📋 Kronolojik İstek ve Karar Kayıtları

### 1. TV ve Telefon Eşleştirme (QR Web Portalı)
* **Kullanıcı İstekleri:**
  * TV ile telefon senkronizasyonunun QR menüsü üzerinden nasıl sağlanacağına dair yol haritası.
  * Giriş ekranındaki otomatik yüklenen varsayılan M3U listesinin yasal riskleri önlemek amacıyla iptal edilmesi; kullanıcıların yalnızca kendi listelerini girmesi.
* **Teknik Çözüm ve Kararlar:**
  * Yerel HTTP/WebSocket sunucusu (`ManagerServer.kt`) TV üzerinde başlatıldı.
  * TV ekranında QR kod ve kısa yerel IP adresi oluşturuldu.
  * Web portalı (`web-portal/index.html`) üzerinden telefondan kumanda, kanal gezinme, IPTV ekleme ve TV'ye aktarım sağlandı.
  * Varsayılan M3U kaldırılarak temiz başlangıç sağlandı.

---

### 2. Çoklu Ekran (Multi-View) ve D-Pad Kumanda İyileştirmeleri
* **Kullanıcı İstekleri:**
  * Çoklu kanal izleme ekranında kanal açıldıktan sonra OK basılınca açılan harici menüye kumandadan ulaşılamıyordu. Bu menünün seçili kanal karesinin içinde açılması ve doğrudan kumanda D-Pad ile kontrol edilebilmesi istendi.
  * Kanal değiştir ve tam ekran tuşlarının aktif hale getirilmesi.
* **Teknik Çözüm ve Kararlar:**
  * Çoklu ekran kontrol kutusu odaklanmış aktif slotun içerisine taşındı.
  * Tam ekran, slot kapatma, kanal değiştirme ve ses açma butonları D-pad odaklanabilir hale getirildi.

---

### 3. Video Oynatıcı Kalite Seçenekleri ve Performans
* **Kullanıcı İstekleri:**
  * Oynatıcıda 360p gibi düşük kaliteye çekince değişim olmaması üzerine sunucu tarafı ve lag/donma etkisi soruldu.
  * Sunucudan gelen tekil yayınlarda gereksiz çözünürlük düşürme seçeneklerinin kaldırılması, sadece sunucudan çoklu kalite akışı geliyorsa seçenek sunulması istendi.
* **Teknik Çözüm ve Kararlar:**
  * Yapay çözünürlük düşürme menüsü kaldırıldı.
  * ExoPlayer `TrackSelection` üzerinden sunucunun orijinal HLS/DASH akışındaki gerçek çoklu kalite varyantları listelendi.

---

### 4. İletişim Bilgileri & Lisans Aktivasyonu
* **Kullanıcı İstekleri:**
  * Premium aktivasyon ve tüm ekranlardan telefon numarasının tamamen kaldırılması.
  * İletişimin yalnızca `geceiptv@gmail.com` e-posta adresi üzerinden sağlanması.
* **Teknik Çözüm ve Kararlar:**
  * `DeviceLicenseScreen.kt` ve strings kaynaklarındaki telefon numaraları temizlendi.
  * Tek resmi destek kanalı olarak `geceiptv@gmail.com` entegre edildi.

---

### 5. TV Ekran Koruyucu & Bekleme Modu Engellemesi (v0.12.02)
* **Kullanıcı İstekleri:**
  * TV'de yayın açıkken birkaç dakika sonra televizyonun ekran koruyucuya veya uyku moduna geçmesi sorununun düzeltilmesi.
* **Teknik Çözüm ve Kararlar:**
  * Oynatıcı ve TV ana görünümlerine `FLAG_KEEP_SCREEN_ON` ve Compose `DisposableEffect` yaşam döngüsüyle ekran açık tutma bayrağı eklendi.
  * Versiyon `v0.12.02` olarak GitHub'a yüklendi.

---

### 6. Büyük Güncelleme Paketi (v0.12.03)
* **Kullanıcı İstekleri:**
  1. Sol hareketli menünün kaldırılması, sabit ana ekran yapılması, altına canlı TV, film, dizi ve QR bağlantı butonları konulması.
  2. **Aktif Sağlayıcı İzolasyonu:** Seçili sağlayıcı dışındaki sağlayıcıların içeriklerinin gizlenmesi; yalnızca aktif olanın verilerinin gösterilmesi.
  3. Telefondaki sağlayıcıların TV ile eşleştiğinde otomatik olarak TV'ye senkronize edilmesi.
  4. Canlı TV'de ülke bazlı gruplandırma ve kanal üzerinde OK basılı tutunca gizleme seçeneği.
  5. Gece/Gündüz tema butonunun saat ve tarihin yanına taşınması.
  6. Kumanda ekranına TV kısayol butonları ve tek tıkla TV'ye medya gönderme (Cast).
  7. Dizi/Film yükleme ekranında yüzdelik (%) ve adet bilgisi sunan hızlandırılmış adım adım yükleme mekanizması.
* **Teknik Çözüm ve Kararlar:**
  * `DashboardScreen.kt`, `OpenTvViewModels.kt` ve `SyncWorker.kt` modüler hale getirildi.
  * Versiyon `v0.12.03` (versionCode 24) olarak derlenip GitHub Release'e yüklendi.

---

### 7. Gemini Modelleri Hakkında Bilgilendirme
* **Kullanıcı Sorusu:** Gemini 4 Argon nasıl kullanılır ve ücretli mi?
* **Açıklama:**
  * Google'ın model isimlendirmeleri (Gemini 1.5 Pro, Flash vb.) ve experimental kod adları hakkında bilgi verildi.
  * Google AI Studio / API katmanındaki ücretsiz ve kullandıkça öde (pay-as-you-go) fiyatlandırma prensipleri açıklandı.

---

### 8. 3 Seçenekli Kanal/Grup Gizleme Modali
* **Kullanıcı İstekleri:**
  * Kanal grubunu hem Canlı TV'de hem de Film ve Dizi sayfasında gruba basılı tutunca direkt gizlemek yerine 3 seçenekli ("Aç", "Gizle", "İptal") bir açılır pencere ile sunulması.
* **Teknik Çözüm ve Kararlar:**
  * `VodScreens.kt` içinde `VodActionModal` composable'ı geliştirildi.
  * Kategori çipleri ve grup listelerine `combinedClickable(onLongClick = { ... })` eklendi.

---

### 9. TV Ana Sayfa Buton Hiyerarşisi
* **Kullanıcı İstekleri:**
  * Arama, Ayarlar ve Çoklu TV butonlarının üstteki Canlı TV / Film / Dizi ızgarasının hemen altına taşınması.
* **Teknik Çözüm ve Kararlar:**
  * `DashboardScreen.kt` içinde TV düzeninde buton çubuğu (`Row`) Canlı TV, Film ve Dizi kartlarının hemen altına alındı.

---

### 10. Telefon Arayüzü Kapsamlı Mobil Optimizasyonu & v0.12.04
* **Kullanıcı İstekleri:**
  1. Film ve dizi sayfalarında sol 260dp rayın telefonlarda alanı kaplamasını önleme; pratik ve modern hale getirme.
  2. Film detay sayfasında kapak fotoğrafının yanındaki "TV'ye Gönder" butonunun dikey ezilip uzamasını düzeltme.
  3. Ayarlar ekranlarındaki (Cihaz ve Lisans, Sağlayıcılar, Kanallar, Görüntü ve Oynatma, Telefon Kumandası) metin ve kartların telefona uyarlanması.
  4. Projenin bir üst versiyon olarak GitHub'a yüklenmesi.
* **Teknik Çözüm ve Kararlar:**
  * **Film & Dizi Mobil:** Sol ray gizlendi; üst yatay kaydırılabilir kategori çipleri (`LazyRow`), kategori arama butonu ve tam ekran 3 sütunlu (`GridCells.Fixed(3)`) Netflix tarzı poster ızgarası uygulandı.
  * **Film Detay & Butonlar:** Kapak görseli orantılı boyutlandırıldı (`100.dp x 150.dp`); butonlar kapak altına yatay kaydırılabilir sıraya (`LazyRow`) alındı ve metinlerin tek satır kalması sağlandı (`maxLines = 1`, `TextOverflow.Ellipsis`).
  * **Ayarlar Hub:** Telefonlarda 2 sütunlu kartlar ve 2 satıra sarılabilen tam başlıklar.
  * **Cihaz ve Lisans:** Tek sütunlu, dikey kaydırmalı (`verticalScroll`) kartlar.
  * **Sağlayıcılar:** İsim/URL üstte, eylem butonları altta iki kademeli kart yapısı.
  * **Kanallar:** Üst yatay kategori çipleri ve %100 genişlikte kanal listesi.
  * **Görüntü ve Oynatma:** Donanım kartı ve optimizasyon diyalogunda dikey kaydırma ile tek sütunlu özellik sıralaması.
  * **Telefon Kumandası:** Ortalanmış, tek sütunlu 180dp QR kod ve okunabilir rehber kartları.
  * **Sürüm Yayınlama:** `app/build.gradle.kts` içinde `versionCode = 25`, `versionName = "0.12.04"` yapıldı. Release APK derlendi, commitlendi, `v0.12.04` tag'i ile GitHub Release'e yüklendi.

### 11. Film & Dizi YouTube Tarzı Akıllı Tamponlama (Forward Buffering + LRU Cache) & TV Arka Plan Ses Düzeltmesi (v0.12.05)
* **Kullanıcı İstekleri:**
  1. Dizi ve film izlerken donma/takılmanın önüne geçmek için YouTube mantığında video açıldığında arka planda ileriye doğru indirme yapan akıllı tampon (forward buffering) eklenmesi; ancak bunun TV ve telefonlarda veri şişkinliği ve hafıza dolmasına sebep olmaması.
  2. TV'de uygulamadan çıkıp başka uygulamaya (Netflix, YouTube vb.) geçildiğinde arka planda birkaç dakika çalışmaya ve ses gelmeye devam etmesi sorununun düzeltilmesi.
* **Teknik Çözüm ve Kararlar:**
  * **VOD İleri Tampon:** `PlayerController.kt` içine `isVod` bayrağı eklendi. VOD için `DefaultLoadControl` min 30s, maks 90s (ayarlardan 120s seçilebilir) ve 30s back-buffer ile yapılandırıldı. Video hızlı (1.5s) başlar ve arka planda 1.5 - 2 dakika ilerisini tamponlar.
  * **LRU Disk Önbellek Kalkanı (`VodCacheManager.kt`):** `SimpleCache` ve `LeastRecentlyUsedCacheEvictor` ile `cacheDir/vod_media_cache` altında **250 MB** kesin sınır konuldu. Eski parçalar otomatik silinir; depolama ve RAM asla şişmez.
  * **İlerleme Çubuğu Görseli:** `VodPlayerScreen.kt` içinde YouTube tarzı açık gri tampon çubuğu eklendi, kullanıcı tampon miktarını görsel olarak izleyebilir.
  * **Kullanıcı Ayarı:** `AppSettings.kt` ve `AppSettingsScreen.kt` içine Film/Dizi akıllı tampon seviyesi (Hızlı 30s, Dengeli 90s, Süper Kararlı 120s) eklendi.
  * **Arka Plan Ses Kesme:** `MainActivity.kt` (`onStop`), `PlayerScreen.kt`, `VodPlayerScreen.kt` ve `MultiViewScreen.kt` içine `LifecycleEventObserver` ve merkezi `PlayerController.pauseAll()` eklendi. TV cihazlarında arka planda ses çalınması ve gereksiz PiP denenmesi engellendi.
  * **Sürüm Yayınlama:** `app/build.gradle.kts` içinde `versionCode = 26`, `versionName = "0.12.05"` yapıldı. Release APK derlendi, commitlendi, `v0.12.05` tag'i ile GitHub Release'e yüklendi.

### 12. Mükerrer Eşitleme Koruması, Tek Tıkla Sağlayıcı Silme, Saatlik Hız Testi & Bağlantı Uyarı Kartı (v0.12.06)
* **Kullanıcı İstekleri:**
  1. Telefondan kumanda ile "Sağlayıcıları Eşitle" butonuna basıldığında TV'de aynı sağlayıcının mükerrer (çift/üçlü) eklenmesinin engellenmesi.
  2. TV'de sağlayıcı silmek istendiğinde çıkan ikinci onay penceresinin kaldırılması; "Kaldır" butonuna basınca sağlayıcının doğrudan silinmesi (kumanda ile ikinci onaya ulaşmak zor oluyordu).
  3. TV uygulamasında seçili sağlayıcının hız testinin saat başı otomatik yapılması ve aktif sağlayıcılar kısmında hız sonucunun yazması.
  4. Sağlayıcının bağlantısı koptuğunda (veya şifre/kullanıcı adı hatalı olduğunda) aktif sağlayıcı renginin maviden kırmızıya dönmesi.
  5. Uygulamanın sağ alt köşesinde *"Lütfen sağlayıcı değiştiriniz. Sağlayıcı bağlantınız yok yada kullanıcı adı şifre yanlış"* detaylı bilgilendirme kartının gösterilmesi.
* **Teknik Çözüm ve Kararlar:**
  * **Mükerrer Sağlayıcı Koruması:** `SourceRepository.kt` içine `isSameSource` ve `findMatchingSource` eklendi; `save()` var olan sağlayıcıyı güncellemeye yönlendirildi. `ManagerServer.kt` içine `alreadyExisted` eklendi; `TvCastHelper.kt` ekranda net Türkçe bildirim gösterir. `CatalogRepository.kt` ve `OpenTvApp.kt` açılışta eski mükerrer kopyaları temizler (`cleanupDuplicateSources()`).
  * **Doğrudan Silme:** `ProvidersScreen.kt` içindeki `confirming` durumu kaldırıldı; "Kaldır" butonu doğrudan `viewModel.delete(source)` çalıştırır.
  * **Saatlik Hız & Sağlık Testi:** `OpenTvViewModels.kt` içine `ProviderHealth` durumu, ping, auth doğrulama ve anlık indirme hızı ölçümü eklendi. `combine` ve `collectLatest` ile her 1 saatte bir (veya aktif sağlayıcı değiştiğinde) otomatik test edilir.
  * **Görsel Durum & Kırmızı Renk:** `DashboardScreen.kt` içinde `ProviderAccordionBar` aktif sağlayıcı yanında `⚡ X Mbps • Y ms` rozeti gösterir; bağlantı koptuğunda bar ve rozetler mavi yerine canlı kırmızıya (`#EF4444`) döner.
  * **Sağ Alt Köşe Uyarısı:** `DashboardScreen.kt` içinde ekranın sağ alt köşesine animasyonlu kırmızı uyarı kartı eklendi; kullanıcıya sağlayıcı değiştirme çağrısı ve hata detayı sunulur.
  * **Sürüm Yayınlama:** `app/build.gradle.kts` içinde `versionCode = 27`, `versionName = "0.12.06"` yapıldı. Release APK derlendi, commitlendi, `v0.12.06` tag'i ile GitHub Release'e yüklendi.

### 13. Akıllı VOD Önbellek Kalkanı, SyncWorker Sonsuz Döngü Engellemesi & TV Performans Optimizasyonu (v0.12.07)
* **Kullanıcı İstekleri:**
  * TV ekranının en altında sürekli "Filmler yükleniyor", "Diziler yükleniyor", "Kanallar yükleniyor" mesajlarının döngü halinde tekrar etmesi sorununun tespiti ve akıllı önbellek kalkanı entegre edilerek loop ve kasma/donmaların engellenmesi.
* **Teknik Çözüm ve Kararlar:**
  * **SyncWorker Sonsuz Retry Loop Engellemesi (`SyncWorker.kt`):** EPG feed'lerindeki harici link hataları (`summary.feedsFailed > 0`) fatal hata kapsamından çıkarıldı ve WorkManager'ın `Result.retry()` döngüsü kırılarak `Result.success()` dönmesi sağlandı. Yalnızca tüm sağlayıcılar tamamen çöktüğünde ve `runAttemptCount < 3` ise yeniden deneme izin verildi. `ExistingPeriodicWorkPolicy.UPDATE` ile eski takılmış işler yenilendi.
  * **Repository Katmanında Akıllı VOD Kalkanı (`CatalogRepository.kt`):** `sync()` ve `syncXtreamVod()` metodlarına `forceVod` bayrağı ve `VOD_TTL_MILLIS = 12 saat` kalkanı eklendi. Veritabanında zaten film/dizi varken ve süre dolmamışsa sunucudan 40.000 içerik tekrar indirilmez; CPU ve ağ şişmesi engellenir.
  * **Çoklu Sağlayıcı Zaman Damgası (`AppSettings.kt`):** Her sağlayıcı için ayrı ayrı (`vod_synced_at_$sourceId`) VOD son senkronizasyon zamanı tutuldu.
  * **StatusBus Güvenliği:** `syncXtreamVod` `try-finally` bloğuna alınarak, herhangi bir iptal veya hatada ekranın altında takılı kalan mesajların anında temizlenmesi (`StatusBus.set(null)`) sağlandı.
  * **Sürüm Güncelleme:** `app/build.gradle.kts` içinde `versionCode = 28`, `versionName = "0.12.07"` yapıldı.

### 14. Sağlayıcı Buton Sıralaması, Arama Harf Büyütme/Kapsam Filtresi, EPG Temizliği & Akıllı Kumanda Ergonomisi (v0.12.08)
* **Kullanıcı İstekleri:**
  1. **Sağlayıcı Buton Sıralaması:** Ayarlar > Sağlayıcılar ekranında Hız Testi, Düzenle, Kaldır butonları ile Otomatik, HLS (.m3u8), MPEG-TS (.ts) butonlarının yer değiştirmesi (sık kullanılan format butonları daha kolay erişilebilir olsun).
  2. **Arama Ekranı İyileştirmeleri:** Ekran klavyesine küçük/büyük harf (`a/A`) butonu eklenmesi; arama yapılacak yerin kullanıcı tarafından seçilebilmesi (Tümü, Canlı TV, Filmler, Diziler kapsam filtreleri).
  3. **EPG Rehber Otomatik Temizliği:** Bir sağlayıcı kaldırıldığında TV rehberi (EPG) bölümünde kalan sağlayıcıya ait feed'lerin ve eşleşmelerin otomatik silinmesi, döngü/bug risklerinin önlenmesi.
  4. **Kumanda Otomatik Eşitlemeyi Kaldırma:** Telefonla TV eşleştiğinde otomatik sağlayıcı eşitlemenin kaldırılması; eşitlemenin yalnızca "Sağlayıcıları Eşitle" butonuna basıldığında manuel yapılması (bağlanma anındaki yavaşlığı ve takılmayı önler).
  5. **Yerel Ağda Otomatik Bağlanma:** Bir kez eşleşen telefon ve TV, iki tarafta da uygulama açıksa aynı ağ (Wi-Fi) üzerinden otomatik olarak bağlansın; sürekli QR okutma veya IP girme zorunluluğu kalksın.
  6. **Ergonomik Kumanda Düzeni:** Telefondaki kumandanın joystick yuvarlağı (D-pad) daha aşağıda, sağ el baş parmağın rahatça ulaşabileceği yerde olsun; solunda SES tuşları, sağında KANAL tuşları yer alsın; kumanda daha derli toplu ve pratik hale getirilsin.
* **Teknik Çözüm ve Kararlar:**
  * **Sağlayıcı Format Butonları Önceliği (`ProvidersScreen.kt`):** Hem TV hem mobil arayüzde Otomatik, HLS, MPEG-TS format butonları en başa (üst sıraya), Test, Düzenle ve Kaldır butonları ikinci sıraya taşındı.
  * **Arama Ekranı Klavyesi & Kapsamlar (`SearchScreen.kt`, `OnScreenKeyboard.kt`):**
    * Sanal klavyeye `a/A` Caps/Shift butonu eklendi, harflerin dinamik büyük/küçük harf dönüşümü sağlandı.
    * `SearchScope` (`ALL`, `LIVE`, `MOVIES`, `SERIES`) filtre çipleri eklendi; sonuç sayıları gerçek zamanlı filtrelendi.
  * **EPG Beslemelerini Otomatik Kaldırma (`EpgRepository.kt`, `CatalogRepository.kt`):** `deleteSources()` içine `epgRepository.removeFeedsForSource(id)` eklendi; sağlayıcı silinince ona bağlı EPG XML linkleri ve tablodaki yetim veriler temizlenir.
  * **ManagerServer Kalıcılığı & Port Sabitleme (`ManagerServer.kt`, `AppSettings.kt`):**
    * TV server portu `8765` olarak sabitlendi (meşgulse fallback).
    * Rastgele 5 byte token yerine `settings.getOrCreateManagerToken()` ile SharedPreferences'ta kalıcı token üretildi; TV yeniden başlatılsa bile yetki anahtarı korunur.
    * `GET /ping` endpoint'i token kontrolü önüne alındı; LAN cihazlarının TV'yi ve token geçerliliğini doğrulaması sağlandı. `IDLE_TIMEOUT_MINUTES` 24 saate çıkarıldı.
    * `MainActivity.kt`: TV cihazlarında (`isTelevision`) `managerServer.start()` onCreate'de otomatik başlatılır ve onDestroy'da durdurulur.
  * **Otomatik Eşitlemenin Kaldırılması (`RemoteScreen.kt`):** `parseAndConnect()` içindeki otomatik `TvCastHelper.syncSourcesToTv(...)` çağrısı kaldırıldı. Senkronizasyon sadece kumandadaki "Sağlayıcıları Eşitle" çipine basıldığında çalışır.
  * **Yerel Ağ Otomatik Keşif & Canlı Kalma (`RemoteScreen.kt`):**
    * `pingTv(targetUrl, token)` ve `discoverTvOnLan(token)` (subnet 1..254 paralel tarama) eklendi.
    * `LaunchedEffect`: TV IP'si DHCP ile değişse bile saniyeler içinde otomatik keşfedip bağlanır; 15 saniyelik kalp atışıyla TV'nin açık/kapalı durumu canlı takip edilir.
  * **Sağ Başparmak Odaklı Ergonomik Kumanda Tasarımı (`RemoteScreen.kt`):**
    * **Üst Bar:** Kompakt TV kısayolları (`Canlı TV`, `Film`, `Dizi`, `Anasayfa`, `Sağlayıcıları Eşitle`).
    * **Hızlı Menü Barı:** `Geri`, `Oynat/Duraklat`, `Sessiz`, `Rehber`, `Sayılar (123)`.
    * **Ana Kontrol Bloğu (Thumb Zone):**
      * **Solunda:** `SES` Rocker Kapsülü (Ses Aç, SES etiketi, Ses Kıs) - 62.dp genişlik.
      * **Ortasında:** `Joystick Yuvarlağı` (208.dp çapında D-Pad çemberi; Yukarı, Aşağı, Sol, Sağ ve ortada 74.dp `OK` butonu - sağ başparmağın doğal basma hizasında).
      * **Sağında:** `KANAL` Rocker Kapsülü (Kanal İleri, KANAL etiketi, Kanal Geri) - 62.dp genişlik.
    * **Genişletilebilir Numaratör:** İhtiyaç halinde `Sayılar` butonuna basıldığında altta açılan 0-9 tuş takımı.

### 15. Sıfır Gecikmeli Sanal Fare (Touchpad) Modu & Beliren TV Arama Klavyesi (v0.12.08)
* **Kullanıcı İstekleri:**
  1. Kumanda ekranına bir "Fare" butonu eklenmesi; tıklandığında telefonun TV için kasmadan, donmadan akıcı bir dokunmatik fare (touchpad) olarak kullanılabilmesi.
  2. Fare TV'deki arama butonuna götürülüp tıklandığında (veya TV'de arama ekranı aktif olduğunda) telefondaki mouse alanının hemen altında bir klavye belirmesi; kullanıcının TV'de harf harf seçmek yerine telefondan hızlıca yazıp aratabilmesi ve bu işlemin kasmaya/donmaya yol açmaması.
* **Teknik Çözüm ve Kararlar:**
  * **Sıfır Gecikmeli UDP İletişim Protokolü (`VirtualMouseServer.kt`, Port 8766):**
    * Parmak kaydırmaları saniyede 60-120 olay üretir. HTTP istekleri ağ kuyruğunu tıkayıp TV'de donmaya sebep olacağı için 9 baytlık UDP datagram paketi (`[type: Byte, dx: Float, dy: Float]`) geliştirildi.
    * `Channel<Pair<Float, Float>>(capacity = Channel.CONFLATED)` boru hattı kullanılarak ağ gecikmesi ve bellek yükü 0 ms seviyesine indirildi.
  * **Donanım Hızlandırmalı GPU İmleci (`VirtualMouseOverlay.kt`):**
    * Standart Compose recomposition yerine `graphicsLayer { translationX = pointerX; translationY = pointerY }` ile GPU katmanında 60/120 FPS akıcı imleç hareketi sağlandı; ekrandaki diğer hiçbir bileşen gereksiz yere yeniden çizilmez.
    * Keskin ok imleci, gölge kenarlığı ve tıklama anında yayılan nabız animasyonu (`isClicking`) eklendi. 12 saniye hareketsizlik sonrası otomatik gizlenme uygulandı.
  * **Ekrana Dokunma Enjeksiyonu (`window.decorView.dispatchTouchEvent`):**
    * Fare tıklandığında imlecin altındaki koordinata sentetik `ACTION_DOWN` ve `ACTION_UP` dokunma olayları gönderilir; TV arayüzündeki butonlar, kartlar ve arama çubuğu doğrudan tetiklenir.
  * **Beliren Klavye & Arama Entegrasyonu (`RemoteScreen.kt`, `MainActivity.kt`):**
    * TV'de arama ekranına girildiğinde (`Routes.search`) `NavController.OnDestinationChangedListener` tetiklenir ve `ManagerServer.isSearchActive = true` bayrağı `/ping` yanıtında telefona iletilir.
    * Fare modundayken Touchpad ve Sol/Sağ tık butonlarının hemen altında yer alan klavye kartı turkuaz ışıkla (`TV ARAMA EKRANI AKTİF — Buradan Hızlıca Yazın`) parlar; `focusRequester` ve `keyboardController` ile telefon klavyesi otomatik olarak açılır.
    * Kullanıcı telefondan metni yazıp "TV'de Ara & Gönder" veya klavyeden Ara/Enter tuşuna bastığında sorgu 0 ms gecikmeyle doğrudan TV'ye aktarılır.
  * **Kumanda & Fare Geçiş Ergonomisi (`RemoteScreen.kt`):**
    * Hızlı menü barına `Fare` butonu (açık/kapalı ışıklı durum göstergesiyle) entegre edildi.
    * Fare moduna geçildiğinde üstte `[ 🎮 Kumandaya Dön ]` butonu, ortada 210.dp hassas Touchpad yüzeyi, altında geniş `SOL TIK (SEÇ)` ve `SAĞ TIK (GERİ)` butonları ve hemen altında Hızlı TV Arama Klavyesi yer alır.

### 16. Sanal Fare (Touchpad) & TV Klavyesi Entegrasyonu, Tam Çoklu Dil (TR & EN) Desteği & v0.12.09
* **Kullanıcı İstekleri:**
  1. Kumanda ekranına bir fare (touchpad) butonu eklenmesi; telefon ekranının kasmadan, donmadan TV'de fare gibi kullanılabilmesi.
  2. TV arama ekranına girildiğinde telefondaki farenin altında anında klavye belirmesi ve TV'ye doğrudan hızlı arama metni gönderilebilmesi.
  3. Uygulamanın tamamının detaylı dil analizinden geçirilmesi; sonradan eklenen ve sert kodlanmış (hardcoded) tüm buton, etiket, açıklama, diyalog, tost ve panellerin eksiksiz İngilizce ve Türkçe kaynaklara (`strings.xml`) bağlanarak çoklu dil desteğinin tamamlanması.
  4. Kod doğrulamasının yapılması, sürümün güncellenmesi ve GitHub'a release olarak yüklenmesi.
* **Teknik Çözüm ve Kararlar:**
  * **Sanal Fare & UDP Protokolü (`VirtualMouseServer.kt`, `VirtualMouseOverlay.kt`, `RemoteScreen.kt`):** Port 8766 üzerinde sıfır gecikmeli UDP paketleri, GPU donanım hızlandırmalı imleç overlay'i ve `dispatchTouchEvent` sentetik dokunma tetikleyicisi.
  * **Beliren Akıllı Klavye:** TV'de arama ekranı açıldığında telefon klavyesinin otomatik belirmesi ve doğrudan TV arama motoruna iletilmesi.
  * **Eksiksiz Dil Yerelleştirmesi (`values/strings.xml`, `values-tr/strings.xml`):**
    * Kumanda & Fare (`remote_*`): Touchpad modu, D-pad, ses/kanal düğmeleri, arama kartı, QR/IP diyalogları.
    * Sağlayıcı Hız Testi & Teşhis (`provider_*`): Ping, hız (Mbps), hesap bitiş/oluşturulma tarihleri, yayın formatları.
    * Web Yönetici & Eşleşme (`web_manager_*`): Canlı bağlantı rozetleri, rehber adımları, güvenlik notları.
    * VOD Oynatıcı, Altyazı & Akıllı Senkron (`vod_*`): Harici oynatıcı (VLC/MX), sonraki bölüm kartları, OpenSubtitles/TheSubtitleDB çoklu arama, milisaniye hassasiyetli akıllı senkron HUD'ı, sıralama seçenekleri.
    * Kategori ve Kanal Eylemleri (`category_action_*`, `channel_action_*`): Aç, gizle, iptal, toast bildirimleri.
  * **Sürüm Yayınlama:** `app/build.gradle.kts` içinde `versionCode = 30`, `versionName = "0.12.09"` yapıldı.

### 17. Arama İyileştirmeleri & Ayrı Arama Fonksiyonları
* **Kullanıcı İstekleri:**
  1. Arama ekranında çıkan (q) yazısının kaldırılması.
  2. Film, dizi ve kanal aramalarının ayrı ayrı yapılabilmesi veya filtrelenebilmesi; arama fonksiyonunun eksiksiz çalıştığının doğrulanması.
* **Teknik Çözüm ve Kararlar:**
  * `SearchScreen.kt`: Arama çubuğundaki hatalı `(q)` etiketi ve metinleri kaldırıldı; Türkçe ve İngilizce düzgün placeholder yerleştirildi.
  * Arama kategorileri Live TV, Movies ve Series olarak filtreleme sekmeleriyle ayrıldı.
  * `CatalogRepository.kt`: Türkçe harf duyarlılığı için `expandQueryVariants` (ı/i, ğ/g, ü/u, ş/s, ö/o, ç/c) varyant üretimi eklendi.

---

### 18. Sağlayıcı Kaydında VOD (Film, Dizi, Vitrin) Senkronizasyonunun Onarılması
* **Kullanıcı İstekleri:**
  * Sağlayıcı kaydedildiğinde canlı TV kanalları yükleniyor ancak filmler, diziler ve vitrin yüklenmiyor; başka oynatıcılarda bunların listelendiği, VOD kısmındaki sorunun düzeltilmesi istendi.
* **Kök Nedenler ve Teknik Çözümler:**
  1. **Arka Plan VOD Tetiklemesi Eksikliği (`SourcesViewModel.kt`):**
     * Sağlayıcı eklendiğinde `saveAndSync` yalnızca canlı kanalları ve EPG rehberini çekiyordu; arka plan VOD senkronizasyonu başlatılmıyordu.
     * `onDone(true)` hemen sonrasında arka planda `catalogRepository.syncVod(saved, now, force = true)` çağrısı eklendi.
  2. **Xtream Sunucu Bağlantı Limiti Çakışması (`CatalogRepository.kt`):**
     * `syncXtreamVod` metodu 4 ayrı API isteğini (`movieCategories`, `movies`, `seriesCategories`, `series`) `async(Dispatchers.IO)` ile aynı anda gönderiyordu. `max_connections = 1` limitine veya rate-limit'e sahip Xtream sunucuları eşzamanlı istekleri reddediyor (403/429) ve filmler boş dönüyordu.
     * İstekler sıralı (sequential) hale getirildi: `movieCategories` -> `movies` (parçalı Room ekleme ve `StatusBus` yüzdesi) -> `seriesCategories` -> `series`.
  3. **M3U VOD Desteği (`M3uParser.kt`, `CatalogRepository.kt`):**
     * `M3uParser` içine VOD akış algılama (`.mp4`, `.mkv`, `/movie/`, VOD grup başlıkları) eklendi ve `movies` listesi ayrıştırıldı.
     * `syncM3u` metoduna bu filmleri ve kategorilerini Room veritabanına ekleme desteği entegre edildi.
  4. **Önbellek & Zaman Damgası Karışıklığı (`AppSettings.kt`):**
     * `getVodSyncedAtMillis(sourceId)` yeni eklenen kaynaklarda (timestamp = 0 iken) global `vodSyncedAtMillis` değerine düşüyordu; bu da yeni kaynağın zaten senkronize edildiği yanılsamasına yol açıyordu. Kaynak bazlı salt anahtar kontrolüne dönüştürüldü.
  5. **`VodViewModel` Kilitlenmesi ve Kaynak Başına Kontrol:**
     * `@Volatile private var vodRequested = false` tek seferlik kilit bayrağı kaldırılarak `vodSyncJob` ile yönetildi.
     * `syncVodIfStale` içinde genel veritabanı yerine her aktif kaynak tek tek kontrol edildi; `movieCount(id) + seriesCount(id) == 0` veya zaman aşımı olan kaynaklar otomatik olarak senkronize edildi.
  6. **Vitrin (DashboardScreen) Tetiklemesi:**
     * `DashboardScreen.kt` içerisine `LaunchedEffect(hasSources)` eklenerek kaynaklar aktif olduğunda `ensureVodLoaded()` ve `loadHomeFeeds()` otomatik olarak çalıştırıldı.

### 19. Otomatik Açılış Sürüm Kontrolü, Güncelleme Notları Türkçe Karakter Onarımı & v0.12.10
* **Kullanıcı İstekleri:**
  1. Uygulama açılınca otomatik sürüm kontrolü yapılması; yeni sürüm çıktığında kullanıcıya bilgi verilmesi ve kullanıcı isterse indirebilmesi.
  2. Sürüm güncelleme notlarındaki Türkçe karakter bozulmalarının (mojibake) çözülmesi.
  3. Projenin derlenip (build alınıp) GitHub'a release olarak yüklenmesi.
* **Teknik Çözüm ve Kararlar:**
  * **Açılışta Sürüm Kontrolü (`UpdateScreen.kt`, `UpdateViewModel`):**
    * 6 saatlik engelleyici kısıtlama (`CHECK_INTERVAL_MS`) yerine uygulama açıldığında UI yerleşiminin hemen ardından (1.2 saniye gecikmeyle arka planda) `checkOnLaunch()` çalıştırıldı.
    * Yeni sürüm varsa ekranda D-pad odaklanabilir, TV ve telefon uyumlu `UpdateGate` modal penceresi ("Şimdi Güncelle" / "Daha Sonra") belirir. Kullanıcı onayladığında yüzdelik ilerleme çubuğuyla indirilir ve sistem paket yükleyicisine aktarılır.
  * **Güncelleme Notları Türkçe Karakter Düzeltmesi (`UpdateChecker.kt`):**
    * GitHub API'den gelen veya PowerShell yükleme sürecinde ISO-8859-1 / Windows-1254 dönüşümüne uğrayan çift kodlanmış mojibake karakterleri (`Ä±` -> `ı`, `Ä°` -> `İ`, `ÅŸ` -> `ş`, `Ã§` -> `ç`, `Ã¶` -> `ö`, `Ã¼` -> `ü`, `ÄŸ` -> `ğ`, `Ã‡` -> `Ç`, `Ã–` -> `Ö`, `Ãœ` -> `Ü`, `Äž` -> `Ğ`, `Åž` -> `Ş` ve bozuk emoji dizilimleri) `repairTurkishMojibake` ve `fixMojibake` algoritmalarıyla tamamen onarıldı.
  * **Birim Testleri (`UpdateCheckerTest.kt`):** Sürüm karşılaştırma ve Türkçe karakter düzeltme testleri başarıyla doğrulandı.
  * **Sürüm Güncelleme & Release:** `app/build.gradle.kts` içinde `versionCode = 31`, `versionName = "0.12.10"` olarak güncellendi, `GECEiptv-release.apk` derlendi ve GitHub Release `v0.12.10` olarak yüklendi.

### 20. Çoklu Ekran (Multi-View) Kanal Ekleme Çökmesinin Çözümü (Duplicate Key & Aktif Sağlayıcı İzolasyonu)
* **Kullanıcı Bildirimi:**
  * Çoklu ekran (Multi-View) seçeneğinde boş slota "+ Kanal Ekle" butonuna basıldığında veya kanal seçerken televizyondan/telefondan uygulamanın aniden kapanması/çökmesi.
* **Kök Nedenler:**
  1. **Jetpack Compose Mükerrer Anahtar (`IllegalArgumentException`):** `InSlotChannelPicker` içindeki `LazyRow` kategori çipleri `items(categories, key = { it.id })` kullanıyordu. `Category` entity'sinde `id` yalnızca `sourceId` ile birlikte benzersizdir (`["sourceId", "id", "kind"]`). Birden fazla sağlayıcı veya standart ID'li ("1", "2") kategoriler olduğunda Compose anında `IllegalArgumentException: Key was already used` fırlatarak uygulamayı kapatıyordu.
  2. **Aktif Sağlayıcı İzolasyonu Eksikliği & Bellek Şişmesi:** `MultiViewScreen` içinde `observeChannels()` ve `observeCategories()` parametresiz çağrılarak tüm sağlayıcıların on binlerce kanalı filtrelenmeden tek seferde RAM'e yükleniyordu; TV'lerde bellek tükenmesi (OOM) ve ölçüm kilitlenmesi yaşanıyordu.
  3. **Yakalanamayan İstisnalar (Unhandled Exceptions):** `tuneSlot` ve `onSelectChannelForSlot` çağrılarında coroutine scope içinde hata yakalama (`runCatching`) eksikti.
* **Teknik Çözüm ve Kararlar (`MultiViewScreen.kt`):**
  * **Benzersiz Anahtar Garantisi:** Kategori çipleri `itemsIndexed(categories, key = { index, cat -> "${cat.sourceId}_${cat.id}_$index" })` ve kanal satırları `itemsIndexed(filteredChannels, key = { index, ch -> "${ch.sourceId}_${ch.id}_$index" })` yapılarak Compose duplicate key çökmesi %100 engellendi.
  * **Aktif Sağlayıcı İzolasyonu:** `settings.activeSourceId` dinlenerek sadece seçili sağlayıcının kanalları (`observeChannels(effectiveSourceId)`) ve kategorileri çekildi; mükerrer kategoriler `distinctBy { it.id }` ile temizlendi.
  * **Düşük RAM Kalkanı:** "Tüm Kanallar" seçili iken binlerce kanalın Compose listesine yığılmasını engellemek için ilk yükleme `list.take(200)` ile sınırlandırıldı; spesifik kategori seçildiğinde ise o kategorinin kanalları gösterildi.
  * **Güvenli Hata Yakalama:** `tuneSlot` ve `onSelectChannelForSlot` çağrıları `runCatching` kalkanına alındı; çift `controllers.release()` çağrısı temizlendi.

### 21. Sağlayıcılar Arası Hızlı Geçiş (ProviderQuickChipsBar - Tek Tıkla Değişim)
* **Kullanıcı İstekleri:**
  * Ana sayfadaki sağlayıcılar arasında geçişin çok adımlı/yavaş akordiyon menü yerine hızlı, düzgün ve anlık olması.
* **Teknik Çözüm ve Kararlar (`DashboardScreen.kt`):**
  * Eski `ProviderAccordionBar` ve açma/kapatma durumu (`providerMenuExpanded`) kaldırıldı.
  * Yerine her an görünür, tek tıkla/tek dokunuşla seçilebilen `ProviderQuickChipsBar` eklendi.
  * Aktif sağlayıcı yeşil kenarlık ve `✔ AKTİF` rozeti ile belirtildi; D-pad kumanda odaklaması ve `➕ Yeni Ekle` butonu entegre edildi.

---

### 22. Ana Dizin Gridleri Modernizasyonu ve Türkçe/İngilizce Çift Dil Desteği
* **Kullanıcı İstekleri:**
  1. Ana sayfadaki Canlı TV, Filmler, Diziler gibi ana dizinlerin gridlerinin daha şık ve modern bir şekilde yerleştirilmesi.
  2. Ana sayfayı İngilizce olarak kullananlar için İngilizce desteği eklenmesi (diğer dillere çeviri gerekmiyor).
* **Teknik Çözüm ve Kararlar:**
  * **Modern Ana Dizin Hero Grid (`DashboardMediaHubGrid`):**
    * Canlı TV, Filmler ve Diziler 82dp yükseklikte, camgöbeği/mor/indigo derin gradyanlı, buzlu cam ikon kutulu (`46dp`) ve canlı/durum rozetli (`● CANLI`, `🎬 SİNEMA`, `🍿 SEZONLAR`) 3 geniş kahraman kart haline getirildi.
  * **Hızlı Araçlar ve İşlem Çubuğu (`DashboardQuickToolsRow`):**
    * Çoklu Ekran (Multi-View), Arama (Search), QR Bağlantı (Remote & Web) ve Ayarlar (Settings) 52dp yükseklikte 4 şık ve kompakt araca dönüştürüldü.
  * **Mobil Ana Dizinler (`PhoneDashboardContent`):**
    * 2x2 grid yapısında Canlı TV, Filmler, Diziler ve Çoklu Ekran modern 76dp kartlarla şıklaştırıldı.
  * **İngilizce ve Türkçe Dil Desteği (`res/values/strings.xml`, `res/values-tr/strings.xml`):**
    * Ana sayfadaki tüm sabit Türkçe metinler (başlıklar, istatistikler, durum etiketleri, sağlayıcı barı, içerik sütunları ve hata pencereleri) `dashboard_*` string kaynaklarına taşındı.
    * Cihaz/uygulama dili İngilizce olduğunda tüm ana sayfa İngilizce, Türkçe olduğunda Türkçe olarak dinamik ve yerel ayarlara duyarlı (`Locale.getDefault()`) hale getirildi.

### 23. Donanım Multimedya Tünelleme (Hardware A/V Tunneling) & Otomatik Kurtarma
* **Kullanıcı İstekleri:**
  * Donanım tünelleme (multimedia tunneling) mantığının ne olduğunun açıklanması ve uygulamaya değer katacak şekilde entegre edilmesi.
* **Teknik Çözüm ve Kararlar:**
  * **ExoPlayer / Media3 Tünelleme Entegrasyonu ([PlayerController.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/player/PlayerController.kt)):**
    * `DefaultTrackSelector.parameters` üzerinde `setTunnelingEnabled(isTunnelingActive)` yapılandırıldı.
    * Ön izleme (`preview = true`) ve Çoklu Ekran (Multi-View) modlarında donanım çakışmalarını önlemek için tünelleme daima pasif tutuldu.
  * **Akıllı Otomatik Hata Kurtarma (Fail-Safe Fallback):**
    * Tünelleme açıkken bir kanalın video/ses formatı TV donanımında kod çözücü hatası (`onPlayerError`) verirse motor anında `defaultTrackSelector.parameters = setTunnelingEnabled(false)` yaparak tünellemeyi kapatıp yayını 250ms içinde standart boru hattıyla yeniden başlatır. Böylece kullanıcı asla siyah ekranda veya çökmede kalmaz.
  * **Kullanıcı Kontrolü ve Ayarlar ([AppSettings.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/core/AppSettings.kt), [AppSettingsScreen.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/ui/settings/AppSettingsScreen.kt)):**
    * Ayarlar > Oynatma bölümüne `Donanım Tünelleme (A/V Tunneling)` açma/kapatma anahtarı eklendi.
    * `DeviceOptimizationManager`'da `ULTRA` profili için tünelleme otomatik önerilirken, `LOW_END` ve `BALANCED` için güvenlik gereği varsayılan kapalı bırakıldı; profiller kartında `⚡ A/V Tunneling` rozeti eklendi.
    * Türkçe ve İngilizce açıklamalar (`strings.xml`, `strings.xml (tr)`) eklendi.

### 24. Canlı Yayın / Video Özellikleri (Nerd Stats) ve Sağ Üst Dijital Saat
* **Kullanıcı İstekleri:**
  1. Video izlerken videonun özelliklerini (bağlantı hızı, kaç MB dosya/akış, ne kadarla okunuyor, ebatları, codec vb.) gösteren detaylı bir analiz paneli butonu eklenmesi.
  2. Video/dizi/TV izlerken ekranın sağ üst köşesinde güncel saatin gösterilmesi ve kullanıcının bunu açıp kapatabilmesi.
* **Teknik Çözüm ve Kararlar:**
  * **Oynatıcı İstatistik Motoru ([PlayerController.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/player/PlayerController.kt)):**
    * `DefaultBandwidthMeter.getSingletonInstance(context)` ExoPlayer ve OkHttp veri kaynağına `TransferListener` olarak bağlandı; anlık indirme/okuma hızı (`bitrateEstimate`) ölçümlendi.
    * `StreamStatsInfo` modeli ve `getStreamStats()` fonksiyonu ile çözünürlük, en-boy etiketi (1080p, 4K vb.), FPS, video bitrate, video codec (HEVC, AVC vb.), ses codec (Dolby AC-3, E-AC-3 vb.), ses kanalları (Stereo, 5.1 Surround), örnekleme hızı (48 kHz), tampon süresi/yüzdesi, tünelleme durumu ve VOD içeriklerinde tahmini dosya boyutu (MB/GB) anlık hesaplandı.
  * **İstatistik ve Analiz Paneli ([StreamStatsOverlay.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/ui/player/StreamStatsOverlay.kt)):**
    * `StreamStatsDialog` bileşeni oluşturuldu; modern koyu camgöbeği/lacivert kartlar halinde canlı olarak saniyede bir güncellenen istatistikler ve doğrudan panel içinden saati açma/kapatma anahtarı sunuldu.
  * **Sağ Üst Köşe Saati (`PlayerClockBadge`):**
    * `Alignment.TopEnd` konumunda şık, yarı saydam buzlu cam arka planlı, `HH:mm` formatında her saniye güncellenen dijital saat hapı eklendi.
    * Tıklandığında istatistik panelini açar; kanal numarası tuşlandığında çakışmaz.
  * **Canlı TV ve VOD Entegrasyonu ([PlayerScreen.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/ui/player/PlayerScreen.kt), [VodPlayerScreen.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/ui/vod/VodPlayerScreen.kt)):**
    * Alt kontrol çubuğuna `BarChip` / `VodChip` ile `Yayın Özellikleri` / `Stats & Info` butonu (`Icons.Filled.Analytics`) eklendi.
    * `BackHandler` entegrasyonu sağlandı (Geri tuşu önce açık paneli kapatır).
  * **Ayarlar ve Dil Desteği ([AppSettings.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/core/AppSettings.kt), [AppSettingsScreen.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/ui/settings/AppSettingsScreen.kt)):**
    * Ayarlar > Oynatma menüsüne `Oynatıcıda Güncel Saat` / `Show Current Time in Player` toggle'ı eklendi (varsayılan: açık).

### 25. Favoriler Bölümü (Dashboard Hızlı Araçlar) & İzlemeye Devam Et Yönetimi
* **Kullanıcı İstekleri:**
  1. Görev 1: Ana sayfada Arama ve QR Bağlantısı arasına "Favoriler" bölümü eklenmesi; favorilere alınan dizi ve filmlerin kategorik olarak o menüde listelenmesi.
  2. Görev 2: İzlemeye devam et gridinde kullanıcının tekil içerikleri silebilmesi veya listenin tamamını temizleyebilmesi.
* **Teknik Çözüm ve Kararlar:**
  * **Favoriler Bölümü ve Kategorik Görünüm ([FavoritesOverlay.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/ui/favorites/FavoritesOverlay.kt)):**
    * `DashboardQuickToolsRow` içine Arama ile QR arasına altın sarısı yıldız simgeli (`Color(0xFFF59E0B)`) `Favoriler` hızlı işlem kartı eklendi.
    * Tıklandığında açılan `FavoritesDialog` overlay'i oluşturuldu.
    * `Tümü (All)`, `Filmler (Movies)` ve `Diziler (Series)` üst kategorik filtreleme sekmeleri (`FavTabButton`) sunuldu.
    * "Tümü" seçildiğinde Filmler ve Diziler kategorik olarak iki ayrı yatay karusel (`LazyRow`) halinde sunuldu.
    * "Filmler" ve "Diziler" sekmelerinde TV için 5 sütunlu (`GridCells.Fixed(5)`), telefon için 3 sütunlu (`GridCells.Fixed(3)`) dikey ızgara yapısı kuruldu.
    * Kartlar üzerinde afiş, başlık, çıkış yılı, IMDb/kullanıcı puan rozeti (`⭐ 8.4`) ve tek tıkla favoriden çıkarma butonu yerleştirildi; tıklandığında ilgili detay ekranına yönlendirir.
    * Boş durumda şık yıldız ikonlu bilgilendirme penceresi eklendi.
    * D-pad kumanda odaklanması (`focusRequester`, `border`, `scale` animasyonu) ve `BackHandler` desteklendi.
  * **Veri Katmanı Desteği ([Daos.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/data/db/Daos.kt), [CatalogRepository.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/data/repo/CatalogRepository.kt), [OpenTvViewModels.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/ui/OpenTvViewModels.kt)):**
    * `SeriesDao` içine `@Query("SELECT * FROM series WHERE favourite = 1 ...") fun observeFavourites` eklendi.
    * `PlaybackPositionDao` içine `@Query("DELETE FROM playback_positions WHERE profileId = :profileId") suspend fun clearForProfile` eklendi.
    * `VodViewModel` üzerinde `favouriteMovies`, `favouriteSeries` StateFlow'ları ile `removeContinueWatching(mediaKey)` ve `clearContinueWatching()` fonksiyonları tanımlandı.
  * **İzlemeye Devam Et Yönetimi ([DashboardScreen.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/ui/DashboardScreen.kt), [VodScreens.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/ui/vod/VodScreens.kt)):**
    * Hem TV (`ContinueWatchingColumn`) hem mobil (`PhoneDashboardContent`) arayüzlerinde başlık satırına kırmızı vurgulu **"Tümünü Temizle"** (`Icons.Outlined.DeleteSweep`) butonu eklendi.
    * Birincil devam kartına "▶ Devam Et" butonunun yanına **"Kaldır"** (`Icons.Outlined.DeleteOutline`) butonu yerleştirildi.
    * İkincil liste kartlarına ve `VodScreens`'deki `ResumeCard` bileşenlerine tek tıkla silme (`Icons.Outlined.Close`) düğmesi eklendi.
    * Yanlışlıkla silinmeleri önlemek için şık `ContinueWatchingClearConfirmDialog` ve `ContinueWatchingRemoveConfirmDialog` onay pencereleri entegre edildi.
  * **Çift Dil Desteği (`strings.xml`, `strings.xml (tr)`):**
    * Favoriler ve izlemeyi temizleme/silme seçenekleri İngilizce ve Türkçe olarak tam lokalize edildi.
  * **Video Oynatıcı Dijital Saati İyileştirmesi ([StreamStatsOverlay.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/ui/player/StreamStatsOverlay.kt)):**
    * Kullanıcı isteği doğrultusunda video oynatıcının sağ üst köşesindeki saat sadeleştirildi:
    * Arka plan kutusu, kenarlık/çerçeve (border), yeşil canlılık noktası ve takvim/saat ikonu tamamen kaldırıldı.
    * Yalnızca saf dijital saat (`HH:mm`) metni bırakıldı.
    * Opasitesi %30 oranında düşürüldü (`Color.White.copy(alpha = 0.65f)`).
    * Parlak/açık renkli video sahnelerinde okunurluğu kaybetmemek adına hafif, şık bir metin gölgesi (`Shadow(Color.Black.copy(alpha = 0.8f))`) uygulandı.
    * Tıklanma özelliği (istatistik/analiz panelini açma) minimalist haliyle korunmaya devam etti.

### 26. Ayarlar Menüsünün Sadeleştirilmesi & "DİĞER AYARLAR" Alt Menüsü
* **Kullanıcı İstekleri:**
  * Ayarlar menüsünün sadeleştirilmesi:
  * Ana menüde yalnızca: Cihaz ve Lisans, Sağlayıcılar, Kanallar, Telefon Kumandası, Görüntü ve Oynatma, Altyazılar, DİĞER AYARLAR ve Hakkında menülerinin kalması.
  * Geriye kalan tüm ayarların (Eklentiler, Yayın Akışı/Rehber, Dil Seçimi, Uyku Zamanlayıcı, Kayıtlar/DVR, Ebeveyn Denetimi, Veri/Eşitleme, Profiller, Hızlı Arama) "DİĞER AYARLAR" adında yeni bir alt menü başlığı altında toplanması.
* **Teknik Çözüm ve Kararlar:**
  * **Ayarlar Merkezi Mimarisi ([SettingsHubScreen.kt](file:///c:/Users/EGE/Desktop/Tvapk/app/src/main/java/app/opentv/ui/settings/SettingsHubScreen.kt)):**
    * Ana menü 8 şık, dengeli karta indirgendi (`mainEntries`):
      1. Cihaz ve Lisans (`WorkspacePremium`)
      2. Sağlayıcılar (`PlaylistPlay`)
      3. Kanallar (`LiveTv`)
      4. Telefon Kumandası & Web (`QrCode2`)
      5. Görüntü ve Oynatma (`DisplaySettings`)
      6. Altyazılar (`Subtitles`)
      7. DİĞER AYARLAR (`Tune`, Emerald Green vurgusu, tıklandığında `showingOtherSettings = true`)
      8. Hakkında (`Info`)
    * "DİĞER AYARLAR" menüsü 9 karttan oluştu (`otherEntries`):
      1. Eklentiler / Stremio Addons (`Extension`)
      2. Yayın Akışı / EPG Rehberi (`ViewTimeline`)
      3. Dil Seçimi (`Translate`)
      4. Uyku Zamanlayıcı (`Bedtime`)
      5. Kayıtlar / DVR (`VideoLibrary`)
      6. Ebeveyn Denetimi (`Security`)
      7. Veri & Eşitleme / Cloud Sync (`CloudSync`)
      8. Profiller (`AccountCircle`)
      9. Hızlı Arama (`Search`)
    * **Simetrik Grid Yerleşimi:**
      * Ana menü TV'de 4 sütun (`GridCells.Fixed(4)`): 8 kart tam 2 satırda (4x2) simetrik olarak yerleşir, TV ekranında boşluk veya asimetri oluşturmaz.
      * "Diğer Ayarlar" menüsünde TV'de 5 sütun (`GridCells.Fixed(5)`), tabletlerde 3 sütun (`GridCells.Fixed(3)`), telefonlarda 2 sütun (`GridCells.Fixed(2)`) düzeni korunur.
    * **Kumanda (D-pad) ve Geri Tuşu Ergonomisi:**
      * `BackHandler(enabled = showingOtherSettings) { showingOtherSettings = false }` entegre edildi; TV kumandası veya telefondaki Geri tuşuna basıldığında doğrudan ana ayarlar menüsüne dönülür.
      * Üst başlık çubuğunda dinamik değişim sağlandı: Diğer Ayarlar açıkken başlık "DİĞER AYARLAR", açıklama "Eklentiler, rehber, dil, uyku modu, eşitleme ve fazlası" olur; sağ tarafta `[ ← Geri ]` butonu yer alır.
      * Sol üstteki ikon kutusuna tıklanarak da ana menüye dönülebilir.
      * `rememberSaveable { mutableStateOf(false) }` ile alt sayfalardan (örn. Uyku Zamanlayıcı) geri dönüldüğünde kullanıcının Diğer Ayarlar menüsündeki konumu korunur.
  * **Çift Dil Desteği (`strings.xml`, `strings.xml (tr)`):**
    * `settings_other_title` ("Diğer Ayarlar" / "Other Settings") ve `settings_other_subtitle` kaynakları eklendi.

### 27. Video Oynatıcı 5 Büyük Geliştirme (Netflix Butonlar, Hız, En-Boy Oranı, Dizi Önizleme Drawer, Harici Oynatıcı Seçici)
* **Tarih:** 25 Ekim 2026
* **Kullanıcı İsteği:**
  1. *Görev 1:* İleri / geri sarma butonları (-15s, +15s) ile oynat/duraklat (play/pause) butonu yuvarlak, Netflix tarzında ikonlar olacak; yazısız, sadece ikon ve buton.
  2. *Görev 2:* YouTube tarzı video hızlı ve yavaş oynatma butonu.
  3. *Görev 3:* 4:3, 16:9, tam ekran (zoom), ekrana yay (stretch) ve sayfaya sığdır butonları; aktif çalışır şekilde.
  4. *Görev 4:* Dizi bölümünde dizinin tüm bölümlerini önizleme butonu; tıklandığında ekranın sağ tarafında düşük opasite ile dizinin diğer sezon ve bölümlerini listeleyen ufak yan menü.
  5. *Görev 5:* Cihazda (TV veya telefonda) yüklü harici oynatıcıları otomatik tespit edip kullanıcının istediği oynatıcıda izlemesini sağlayan akıllı seçici.
* **Uygulanan Çözüm & Mimari:**
  * **Görev 1 - Netflix Tarzı Yuvarlak Medya Butonları (`RoundMediaButton`):**
    * Geri Sar (-15s), Oynat/Duraklat (Play/Pause) ve İleri Sar (+15s) butonları `CircleShape` yuvarlak tasarımda yazısız olarak yeniden oluşturuldu.
    * Netflix estetiği: Odaklandığında büyüyen (`scale(1.15f)`), odaklandığında primary renkle parlayan, odak dışındayken yarı saydam buzlu cam arka planlı (`Color.White.copy(alpha = 0.14f)` ve ortadaki büyük oynat butonu için `0.28f`).
    * TV kumandası D-pad odağı (`focusRequester = barFocus`) doğrudan ortadaki dairesel Oynat/Duraklat butonuna atanır.
  * **Görev 2 - YouTube Tarzı Oynatma Hızı Kontrolü (`playbackSpeed`):**
    * `PlayerController.kt` sınıfına `fun setPlaybackSpeed(speed: Float) { player.setPlaybackSpeed(speed) }` fonksiyonu eklendi.
    * Oynatıcı kontrol çubuğuna `Icons.Filled.Speed` ikonu ve güncel hız etiketli ("Oynatma Hızı" veya örn. "1.25x") çip yerleştirildi.
    * `TrackPanel` içine YouTube standartlarında hız seçenekleri eklendi: `0.5x`, `0.75x`, `1.0x (Standart)`, `1.25x`, `1.5x`, `1.75x`, `2.0x`.
  * **Görev 3 - Gerçek Zamanlı En-Boy Oranı & Sığdırma (`AspectRatioMode`):**
    * `AspectRatioMode` enum'u tanımlandı:
      1. `FIT` (Sayfaya Sığdır / Orijinal): `RESIZE_MODE_FIT`
      2. `RATIO_16_9` (16:9 Geniş Ekran): `Modifier.aspectRatio(16f / 9f)` + `RESIZE_MODE_FILL`
      3. `RATIO_4_3` (4:3 Klasik TV): `Modifier.aspectRatio(4f / 3f)` + `RESIZE_MODE_FILL`
      4. `ZOOM` (Full Ekran / Yakınlaştır): `RESIZE_MODE_ZOOM`
      5. `STRETCH` (Ekrana Yay / Uzat): `RESIZE_MODE_FILL`
    * AndroidView / PlayerView kapsayıcısı dinamik `aspectModifier` ile sarıldı; `update = { it.resizeMode = exoResizeMode }` ile video çerçevesi anında seçilen orana göre yeniden şekillenir.
  * **Görev 4 - Dizi Sezon & Bölüm Önizleme Yan Menüsü (`SeriesEpisodesDrawer`):**
    * Ekranın sağ tarafına yaslanan (`Alignment.CenterEnd`), yumuşak geçişli (`slideInHorizontally` + `fadeIn`) ve düşük opasiteli yarı saydam koyu cam arka planlı (`Color(0xF00A0E18)`) yan menü geliştirildi.
    * Üst kısımda Sezon filtreleme çipleri (`LazyRow` - Sezon 1, Sezon 2...), alt kısımda bölüm listesi (`LazyColumn`).
    * Her bölüm kartında: Bölüm önizleme görseli (still), oynat ikonu, bölüm numarası, bölüm başlığı, kısa konusu ve o anda izlenmekte olan bölüm için yeşil "• Şu an Oynatılıyor" etiketi.
    * Kumanda veya dokunmatik seçimle tıklandığında anında o bölüme geçilir ve menü kapanır. `BackHandler` kumanda Geri tuşunu menüyü kapatacak şekilde yakalar.
  * **Görev 5 - Otomatik Harici Oynatıcı Tespiti & Seçim Diyaloğu (`detectExternalPlayers` & `ExternalPlayerChooserDialog`):**
    * `detectExternalPlayers(context)` fonksiyonu ile Android `PackageManager` üzerinden:
      1. `video/*` MIME türünü açabilen tüm yüklü uygulamalar taranır.
      2. Popüler TV/mobil oynatıcı paketleri (VLC, MX Player, MX Player Pro, Just Player, Kodi, mpv-android, Nova Video Player, KMPlayer, XPlayer, BSPlayer) otomatik olarak sorgulanır.
    * Yüklü uygulamaların gerçek simgeleri (`Bitmap` ve `ImageBitmap`) ve uygulama adları çekilerek şık bir diyalog penceresinde listelenir.
    * Kullanıcı istediği harici oynatıcıya tıkladığında doğrudan o uygulamaya stream URL'si ve başlığı gönderilerek başlatılır.
    * Alt kısımda ayrıca sistem genel seçicisini açan "Sistem Seçicisi ile Aç" (`Intent.createChooser`) seçeneği korunmuştur.
  * **Doğrulama:**
    * `.\gradlew compileDebugKotlin` ve `.\gradlew assembleRelease` başarıyla tamamlandı; `app\build\outputs\apk\release\GECEiptv-release.apk` sorunsuz üretildi.

### 28. Favoriler Kumanda Gezinmesi Düzeltmesi, Tüm İzlemeye Devam Et Menüsü & Video Oynatıcı Kontrol Çubuğu Düzeni
* **Tarih:** 25 Ekim 2026
* **Kullanıcı İsteği:**
  1. *Görev 1:* Favoriler menüsüne tıklandığında kumanda ile gezinme zorluğu/geçememe sorununun giderilmesi (D-pad gezinme ve odaklanma düzeltmesi).
  2. *Görev 2:* Anasayfadaki "İzlemeye Devam Et" kısmında toplam içerik sayısına (10-15 içerik vb.) tıklandığında tüm izlemeye devam et listesini gösteren tam menü/diyalog açılması ve buradan tekil silme / tümünü temizleme işlemlerinin yapılabilmesi.
  3. *Görev 3:* Video oynatıcı alt menü düzeni:
     - İleri - geri ve play/pause olduğu yerde kalsın.
     - Ardından biraz boşluk bırakılsın.
     - Sırasıyla: Altyazı, Ses, Oynatma Hızı, Sığdır, Harici Oynatıcı, Info ("Yayın Özellikleri" yazısı "Info" olarak değiştirilsin), Tüm Bölümler (dizi ise), Sonraki Bölüm.
     - Yazı karakterleri ve butonlar tek bir sayfaya sığacak şekilde kompakt boyutlandırılsın.
* **Uygulanan Çözüm & Mimari:**
  * **Görev 1 - Favoriler Kumanda (D-pad) Odaklanma Düzeltmesi (`FavoritesOverlay.kt`):**
    - `FavoritesDialog` açıldığında ilk sekme olan "TÜMÜ" butonuna otomatik odaklanması için `firstTabFocus = remember { FocusRequester() }` ve `LaunchedEffect(Unit) { delay(120); firstTabFocus.requestFocus() }` eklendi.
    - İç konteynerdeki odak tuzağı oluşturan boş `Modifier.clickable(onClick = {})` kaldırıldı.
    - Dış karartma tıklandığında kapatma işlemi televizyon dışındaki cihazlara (`!isTelevision`) sınırlandırılarak kumanda odağının dış çerçeveye kaçması engellendi.
    - Kapatma `IconButton` ve sekme butonları (`FavTabButton`) görsel odak kenarlığı (`border`), odak arka planı ve `.focusable()` ile donatıldı.
    - Film ve dizi kartlarındaki favoriden çıkarma yıldız butonları dokunmatik odak etkileşimiyle izole edildi; D-pad odağı kartın bütününe ve ardından eylemlerine doğru yönlendirildi.
  * **Görev 2 - Tüm İzlemeye Devam Et Yöneticisi & Tekil Silme (`AllContinueWatchingDialog`):**
    - `DashboardScreen.kt` içerisine `showAllContinueWatching` state'i ve `AllContinueWatchingDialog` composable'ı eklendi.
    - Hem TV arayüzündeki `ContinueWatchingColumn` başlığındaki "X İÇERİK ▶" rozeti hem de mobil arayüzdeki rozet ve kart altı "+ X İçerik Daha" butonları tıklanabilir ve odaklanabilir hale getirildi.
    - TV'de 4 sütunlu (`GridCells.Fixed(4)`), mobilde 2 sütunlu (`GridCells.Fixed(2)`) şık, modern cam efektli grid görünümü sağlandı.
    - Her içerik kartında: Önizleme görseli (poster), izleme ilerleme çubuğu (`LinearProgressIndicator`), yüzde rozeti (örn. `%65`), içerik başlığı, "▶ Devam Et" butonu ve "🗑️ Sil" butonu yer alır.
    - Kumanda ile gezinirken ilk karta `firstItemFocus` ile odaklanılır; kart üzerindeki "Devam Et" ve "Sil" butonlarına kumanda sağ/sol tuşlarıyla doğrudan geçilebilir.
    - "Sil" butonuna tıklandığında `ContinueWatchingRemoveConfirmDialog` açılır ve onaylandığında Room veritabanından silinerek liste anlık ve reaktif olarak güncellenir.
    - Sağ üst kısımda ayrıca tüm listeyi tek seferde silmeyi sağlayan "Tümünü Temizle" butonu mevcuttur.
  * **Görev 3 - Video Oynatıcı Buton Sıralaması & Kompakt Tasarım (`VodPlayerScreen.kt`):**
    - Alt kontrol çubuğu satırı (`Row`) tam olarak istenen sırada yeniden düzenlendi:
      1. Geri Sar (-15s) - Dairesel Netflix butonu (`size = 38.dp, iconSize = 20.dp`)
      2. Oynat / Duraklat (Play/Pause) - Dairesel büyük buton (`size = 46.dp, iconSize = 26.dp`)
      3. İleri Sar (+15s) - Dairesel Netflix butonu (`size = 38.dp, iconSize = 20.dp`)
      4. `Spacer(Modifier.width(10.dp))` (İstenen boşluk)
      5. Altyazı (`ClosedCaption`, `player_subtitles`)
      6. Ses (`Audiotrack`, `player_audio`)
      7. Oynatma Hızı (`Speed`, `player_speed` / `${speed}x`)
      8. Sığdır (`AspectRatio`, `aspectMode.shortLabel`)
      9. Harici Oynatıcı (`OpenInNew`, `vod_external_player`)
      10. Info (`Analytics`, label = "Info", `player_stats`) -> "Yayın Özellikleri" yazısı "Info" olarak güncellendi.
      11. Tüm Bölümler (`VideoLibrary`, `player_episodes`, dizi içeriğiyse)
      12. Sonraki Bölüm (`SkipNext`, `vod_next_episode_tag`, dizi ve sonraki bölüm varsa)
    - `VodChip` butonlarının padding'i `horizontal = 7.dp, vertical = 5.dp`, yazı boyutu `11.sp`, ikon boyutu `14.dp`, çip arası boşluk `6.dp` olarak optimize edildi; böylece TV ekranında ve telefon yatay modunda kaydırmaya ihtiyaç duymadan tüm butonlar tek satıra tam sığar hale getirildi.
  * **Doğrulama:**
    - `.\gradlew compileDebugKotlin` çalıştırıldı ve `BUILD SUCCESSFUL in 56s` sonucuyla 0 hata ile doğrulandı.

---

### 23. Versiyon 0.12.16 - Arama Kelime Arası '+' Düzeltmesi & Menülerde Sanal Fare (Mouse/Touchpad) Katmanlama
* **Kullanıcı İstekleri:**
  * **İstek 1:** Arama yaparken iki kelime yazınca araya `+` işareti atıyor, buda aranan videoyu bulmakta zorluk yaratıyor. Kaç kelime yazılırsa yazılsın aralarda `+` işareti olmasın.
  * **İstek 2:** Favoriler kısmında ve izlemeye devam et menülerinde kumandadan geçiş oluyor menüye ama telefondaki uygulama içerisindeki kumanda menüsünden mouse ve touchpad alt menüde kalıyor. O fixlensin.
* **Teknik Çözüm ve Kararlar:**
  * **Arama '+' Sorunu Çözümü:**
    - `MainActivity.kt`: `Routes.search` fonksiyonunda `URLEncoder.encode` (boşlukları `+` yapan eski yöntem) yerine `android.net.Uri.encode(cleanQuery)` entegre edildi.
    - `Routes.SEARCH` composable rotasında `URLDecoder.decode` ve `.replace("+", " ")` sanitize işlemi eklendi; rotaya nasıl girilirse girilsin boşlukların korunması garantiye alındı.
    - `RemoteControlSignals.RemoteCommand.SearchText` dinleyicisinde gelen metin temizlendi (`.replace("+", " ")`).
    - `SearchScreen.kt`: `cleanInitialQuery`, metin kutusu `onValueChange`, TV ekran klavyesi `onKey` ve `onSpace` ile ViewModel'e aktarılan `LaunchedEffect(query)` arındırıldı.
    - `OpenTvViewModels.kt`: `setSearchQuery` ve `setVodSearchQuery` içerisine `.replace("+", " ")` eklenerek Room veritabanı sorgularının her zaman saf boşluklu metin araması yapması sağlandı.
    - `RemoteScreen.kt`: Telefondan gönderilen arama komutlarındaki girdiler arındırıldı.
  * **Sanal Fare (Mouse/Touchpad) Katmanlama Çözümü:**
    - Kök Neden: Android'de `androidx.compose.ui.window.Dialog` kullanımı bağımsız yeni bir Android OS Window (`android.app.Dialog`) oluşturur. Bu nedenle Activity Window'undaki `VirtualMouseOverlay` (fare imleci) Dialog penceresinin altında kalıyor ve Activity'ye iletilen `dispatchTouchEvent` tıklamaları Dialog'a ulaşamıyordu.
    - Çözüm: `FavoritesOverlay.kt` (`FavoritesDialog`), `DashboardScreen.kt` (`AllContinueWatchingDialog`, `ContinueWatchingClearConfirmDialog`, `ContinueWatchingRemoveConfirmDialog`) ve `LegalDisclaimerDialog.kt` pencereleri `Dialog` sarmalayıcısından çıkarılarak modern hiyerarşik bindirme (`Box(Modifier.fillMaxSize().zIndex(100f))` ve `BackHandler`) mimarisine geçirildi.
    - `DashboardScreen.kt` kök seviyesinde `Box(Modifier.fillMaxSize())` ile sarmalandı.
    - `VirtualMouseOverlay.kt` bileşenine `zIndex(99999f)` atanarak imlecin her zaman ekrandaki en üst katmanda çizilmesi ve tıklamaların doğrudan altındaki kartlara/butonlara iletilmesi sağlandı.
  * **Doğrulama:**
    - `.\gradlew compileDebugKotlin` başarıyla derlendi.

---

### 24. Kod Güvenliği (Private Repo) ve Otomatik Güncelleme Ayrımı (geceiptv-releases)
* **Kullanıcı İstekleri:**
  * GitHub üzerindeki kaynak kodların gizlenmesi istendi.
  * Kodlar gizlenirken cihazlardaki otomatik güncelleme sisteminin kesintisiz çalışması istendi.
  * Telefonda `0.12.15` sürümü varken "Güncelle" dendiğinde "Sürümünüz güncel" uyarısı vermesinin sebebi ve cihazların elle güncellenip güncellenmeyeceği soruldu.
* **Teknik Neden & Çözüm:**
  * **Neden "Sürümünüz güncel" dedi?**
    - `0.12.15` sürümü `UpdateChecker.kt` içinde kaynak kodu barındıran `bygokhanege-svg/geceiptv` reposunu sorguluyordu. Kodlar gizlenip ana repo **Private** yapılınca, şifresiz sorgu yapan eski uygulama GitHub API'sinden 404 (bulunamadı) hatası aldı ve bu durumu "yeni sürüm yok" kabul etti.
  * **Mimari Çözüm (Çift Repo Yapısı):**
    - **1. Kaynak Kod Deposu (ÖZEL / PRIVATE):** `https://github.com/bygokhanege-svg/geceiptv.git` (Tüm kodlar, modeller ve geliştirmeler gizlidir, kimse göremez).
    - **2. Güncelleme Dağıtım Deposu (AÇIK / PUBLIC):** `https://github.com/bygokhanege-svg/geceiptv-releases.git` (İçinde kesinlikle kaynak kod yoktur; sadece derlenmiş `.apk` dosyasını ve sürüm notlarını barındırır).
  * **Uygulama İçi Yönlendirme:**
    - `UpdateChecker.kt` dosyası güncellenerek `REPO_SLUG = "bygokhanege-svg/geceiptv-releases"` yapıldı.
    - `0.12.16` APK'sı bu adresle yeniden derlendi (`assembleRelease`) ve `geceiptv-releases` deposuna yüklendi.
  * **Geçiş Stratejisi (Köprü Güncellemesi - Dışarıdan Yükleme İhtiyacını Sıfırlama):**
    - Kullanıcının cihazlara manuel APK yüklemek zorunda kalmaması için `bygokhanege-svg/geceiptv` reposu geçici olarak tekrar **PUBLIC** yapıldı.
    - `0.12.16` sürümü (içinde yeni `geceiptv-releases` yönlendirmesi bulunan yeni derlenmiş APK ile) `bygokhanege-svg/geceiptv` release bölümüne yüklendi.
    - Böylece cihazlardaki `0.12.15` sürümü doğrudan uygulama içinden "Güncelle" dediğinde `0.12.16`yı görecek ve **otomatik olarak** güncellenecektir.
  * **Geçiş Tamamlandı:**
    - Cihazlar başarıyla `0.12.16` sürümüne güncellendi.
    - Ana kaynak kod deposu (`bygokhanege-svg/geceiptv`) kalıcı olarak tekrar **PRIVATE** (gizli) yapıldı.

---

### 25. Versiyon 0.12.17 - Marka & Uygulama İsmi Yenilemesi: "GECE İPTV Player"
* **Kullanıcı İstekleri:**
  * Uygulamanın adı resmi olarak **GECE İPTV Player** yapılması istendi.
  * Uygulama içerisindeki tüm menüler, açıklamalar, sözleşmeler, bildirimler, TV kumanda ekranı, kısa yollar ve "gece ip tv" / "GECE IPTV" yazan her yerin güncellenmesi istendi.
  * Bu değişikliklerin `0.12.17` sürümü olarak derlenip yeni dağıtım deposundan (`geceiptv-releases`) yayınlanması istendi.
* **Teknik Çözüm ve Kararlar:**
  * **Uygulama İsmi ve String Kaynakları:**
    - Tüm 31 dildeki `values*/strings.xml` dosyalarında `<string name="app_name">` değeri `GECE İPTV Player` olarak güncellendi.
    - `values-tr/strings.xml` ve `values/strings.xml` içindeki kayıt bildirimleri, USB kayıt hedefleri, pil optimizasyonu açıklamaları, Stremio eklenti notları, senkronizasyon metinleri, yasal sorumluluk reddi maddeleri, TV kumanda başlıkları ve çıkış diyalogları `GECE İPTV Player` ile yenilendi.
    - Türkçe için eksik olan `exit_title` ("GECE İPTV Player'dan Çıkılsın mı?"), `exit_body`, `exit_confirm` ve `exit_cancel` dizgileri eklendi.
  * **Kod İçi Ekranlar & Arayüzler:**
    - `UpdateScreen.kt`, `SettingsHubScreen.kt`, `RecordingSettingsScreen.kt`, `AdminPanelScreen.kt`, `AboutScreen.kt`, `SubtitleSettingsScreen.kt` güncellendi.
    - `PhonePairingScreen.kt` ve `AddSourceScreen.kt` onboarding logoları ve başlıkları güncellendi.
    - `MainScreen.kt` ve `DashboardScreen.kt` (üst başlık, profil kartı, aksiyon çubuğu) güncellendi.
    - `RecordingService.kt`, `PairingPage.kt`, `ManagerServer.kt`, `ManagerPage.kt` ve `web-portal/index.html` yeni isimle senkronize edildi.
  * **Versiyon ve Dağıtım:**
    - `app/build.gradle.kts`: `versionCode = 38`, `versionName = "0.12.17"`.
    - `.\gradlew compileDebugKotlin`: Başarılı (0 hata).
    - `.\gradlew assembleRelease`: Başarılı.
    - APK, `bygokhanege-svg/geceiptv-releases` deposuna `v0.12.17` etiketiyle yüklendi. Cihazlardaki `0.12.16` sürümü "Güncelle" dendiğinde doğrudan bu depodan otomatik güncelleme alacaktır.
* **Doğrulama:**
  - `https://api.github.com/repos/bygokhanege-svg/geceiptv-releases/releases/latest` üzerinden anonim sorgulama yapıldı; `v0.12.17` sürümünün herkese açık olarak başarıyla dağıtıldığı teyit edildi.

---

### 26. Telefon ve Tablet Ana Sayfası - 3 Renkli Modern Mobil Deneyim Revizyonu
* **Kullanıcı İstekleri:**
  * Ana sayfa kısmının telefon ve tablet için **sadece 3 renkten oluşan**, göz yormayan, zıt renkli (yüksek kontrastlı) metin ve butonlara sahip modern bir renk düzeniyle yenilenmesi istendi.
  * Telefon ve tablet için gerçek bir mobil yayın deneyimi yaşatacak şekilde arayüzün modernize edilmesi istendi.
* **Tasarım Sistemi & 3 Renkli Renk Paleti:**
  * **1. Zemin (Canvas):** `MobileCanvasBg = Color(0xFF090A0F)` (Derin obsidyen gece siyahı - AMOLED dostu, gözü yormayan saf koyu temel).
  * **2. Kart & Yüzey (Surface):** `MobileCardSurface = Color(0xFF131722)` (Grafit kart yüzeyi), `MobileCardBorder = Color(0xFF22283A)` (Ultra ince mikro çerçeve), `MobileCardElevated = Color(0xFF1A1F2E)` (İkincil yüzey).
  * **3. Vurgu (Accent):** `MobileAccent = Color(0xFFF59E0B)` (Asil amber altın vurgu) ve `MobileAccentSubtle = Color(0x26F59E0B)` (%15 saydam rozet zemini).
  * **Zıt Kontrast (High Contrast) Kuralları:**
    * Koyu Kartlar/Zemin üzerinde: `MobileTextPrimary = Color(0xFFFFFFFF)` (Bembeyaz kristal netliğinde başlıklar) ve `MobileTextSecondary = Color(0xFF94A3B8)` (Gümüş/arduvaz okunaklı alt metinler).
    * Altın Vurgulu Butonlar ve Aktif Çiplerde: `MobileOnAccent = Color(0xFF000000)` (**Simsiyah zıt metin/ikon**). Örneğin: Altın renkli *"▶ Şimdi Devam Et"* butonunun üzerindeki metin simsiyah olup anında okunabilir hale getirildi.
* **Teknik Çözüm ve Mobil Modernizasyon:**
  * **`PhoneDashboardContent` (Yenilenmiş Akıcı Akış):**
    - Durum çubuğu boşluğu (`statusBarsPadding()`) ve alt gezinme boşluğu (`navigationBarsPadding()`) eklendi.
    - Eski çift logo ve mükerrer başlık karmaşası kaldırıldı; yerine tek satırda entegre kompakt `PhoneHeaderBar` getirildi (Logo, GECE İPTV Player başlığı, aktif profil/abonelik etiketi ve Arama, Çoklu TV, Ayarlar dokunmatik hızlı işlem ikonları).
  * **Oynatma Listesi / Kaynak Çipler (`PhoneProviderQuickChipsBar`):**
    - Yatay kaydırılabilir, tek dokunuşla kaynak değiştiren modern çipler. Seçili çip altın zemin üzerine simsiyah zıt metinle parlar.
  * **4 Ana Yayın Merkezi Kartları (`PhoneMainNavigationCards`):**
    - Canlı TV, Filmler, Diziler, Çoklu TV kartları:
      - **Telefonda:** Ergonomik 2x2 grid.
      - **Tablette (`screenWidthDp >= 600 && !isTv`):** Yan yana 4 sütunlu tek satır halinde geniş ekran alanını kusursuz değerlendiren düzen.
    - Kartlarda gömülü altın ikonlar, beyaz başlıklar, gri alt açıklamalar ve aktif kanal/içerik sayaç rozetleri.
  * **"İzlemeye Devam Et" Hero Kartı (`PhoneContinueWatchingHero`):**
    - Kullanıcının yarıda bıraktığı son içeriği öne çıkaran, dinamik afişli, ince altın ilerleme çubuklu ve yüksek kontrastlı "▶ Şimdi Devam Et" butonlu özel kart.
  * **Yatay İçerik Şeritleri & Afiş Kartları (`PhonePosterCard`):**
    - Trend Filmler, Popüler Diziler ve Son Eklenen Kanallar için 105x152dp (telefonda) / 120x175dp (tablette) yumuşak köşeli afiş kartları.
  * **Hızlı Erişim Alt Araç Çubuğu (`PhoneQuickToolsBar`):**
    - Favoriler, Arama, Çoklu TV, Uzaktan Kumanda ve Ayarlar tek dokunuşla başparmak erişimine sunuldu.
  * **TV Deneyimi Korundu:**
    - `isTelevision == true` olan tüm TV arayüzü, D-pad kumanda gezinmesi, 5 sütunlu gridler ve odak çerçeveleri sıfır değişiklikle korundu.
* **Doğrulama:**
  - `.\gradlew compileDebugKotlin`: Başarılı (0 hata).
  - `.\gradlew assembleRelease`: Başarılı. Yeni APK boyutu: 13.45 MB (`GECEiptv-release.apk`).

---

### 27. Versiyon 0.12.18 - Güncelleme Dağıtımı & Yayınlama
* **Kullanıcı İstekleri:**
  * Yapılan 3 renkli modern mobil/tablet ana sayfa revizyonlarının ve marka yenilemesinin tüm kullanıcılara otomatik güncelleme bildirimi olarak düşmesi için yeni bir sürüm oluşturulup GitHub'a yüklenmesi istendi.
* **Uygulanan Değişiklikler ve Dağıtım:**
  * **Versiyon Artırımı:** `app/build.gradle.kts` içerisinde `versionCode = 39` ve `versionName = "0.12.18"` olarak yükseltildi. (Böylece `0.12.17` ve altı tüm cihazlar uygulamayı açtıklarında veya 'Güncelle' dediklerinde yeni sürümü anında görecektir).
  * **Release Derlemesi:** `.\gradlew assembleRelease` çalıştırıldı ve `GECEiptv-release.apk` (13.45 MB) üretildi.
  * **GitHub Dağıtımı:**
    - `bygokhanege-svg/geceiptv-releases` genel (public) dağıtım deposunda `v0.12.18` sürümü oluşturuldu ve APK varlığı yüklendi.
    - Ana kaynak kod deposunda (`bygokhanege-svg/geceiptv`) `v0.12.18` etiketi oluşturulup `git push origin v0.12.18` ile pushlandı.
  * **Doğrulama:** GitHub Releases API üzerinden `https://api.github.com/repos/bygokhanege-svg/geceiptv-releases/releases/latest` sorgulandı; `tag_name: "v0.12.18"` ve `GECEiptv-release.apk` indirme bağlantısının aktif olduğu doğrulandı.

---

### 28. Versiyon 0.12.19 - Uygulamanın Tamamında 3-Renkli Kurumsal Palet & Yüksek Kontrast Revizyonu
* **Kullanıcı İstekleri:**
  * Ayarlar ve ayarların içindeki menüler, Canlı TV, Filmler, Diziler ve Kumanda menülerinin tamamının belirlenen 3-renkli renk skalasına (`GecePalette`) göre renklendirilmesi istendi.
* **Tasarım Kuralları & 3-Renkli Renk Sistemi:**
  * **1. Zemin (Canvas):** `GecePalette.CanvasBg = Color(0xFF090A0F)` (Derin Obsidyen Gece Siyahı).
  * **2. Kart & Yüzey (Surface):** `GecePalette.CardSurface = Color(0xFF131722)` (Grafit kart yüzeyi), `GecePalette.CardBorder = Color(0xFF22283A)` (Ultra ince mikro çerçeve), `GecePalette.CardElevated = Color(0xFF1A1F2E)` (Kabarık yüzey).
  * **3. Vurgu (Accent):** `GecePalette.Accent = Color(0xFFF59E0B)` (Asil Kehribar Altın) ve `GecePalette.AccentSubtle = Color(0x26F59E0B)` (%15 saydam rozet zemini).
  * **Zıt Kontrast (High Contrast) Kuralları:**
    * Koyu Kartlar/Zemin üzerinde: `GecePalette.TextPrimary = Color(0xFFFFFFFF)` (Saf Kar Beyazı başlıklar) ve `GecePalette.TextSecondary = Color(0xFF94A3B8)` (Arduvaz gümüşü alt metinler).
    * Altın Vurgulu Butonlar ve Odaklı Çiplerde: `GecePalette.OnAccent = Color(0xFF000000)` (**Simsiyah zıt metin/ikon**).
* **Uygulanan Değişiklikler:**
  1. **Genel Tema Sistemi (`Theme.kt`):**
     - Global `object GecePalette` eklendi.
     - Varsayılan tema olan `AppSettings.AppColorTheme.OBSIDIAN`, doğrudan `GecePalette` token'larına bağlandı.
  2. **Ayarlar ve Tüm Alt Menüler:**
     - **`SettingsHubScreen.kt`:** 17 adet ayar kartındaki dağınık gökkuşağı renkleri kaldırılarak kehribar altın (`#F59E0B`), grafit kartlar ve altın ambiyans ışıması ile modernize edildi.
     - **`AppSettingsScreen.kt`:** Donanım tarayıcı çipleri, AV tünelleme ve önerilen profil rozetleri `GecePalette` altın/siyah zıt renk skalasına uyarlandı.
     - **`ProvidersScreen.kt`:** Hız testi, ping/gecikme, bant genişliği ve sunucu metrik kartları paletle birebir eşitlendi.
     - **`AdminPanelScreen.kt`:** Gizli konsol, güvenlik denetimleri ve lisans yönetim kartları palete dahil edildi.
     - **`WebManagerScreen.kt` & `DeviceLicenseScreen.kt`:** Durum pilleri, onay ve lisans kartları grafit-altın uyumuyla güncellendi.
     - **`RecordingSettingsScreen.kt`:** Arka plan kayıt izin ve durum kartları palete uyarlandı.
  3. **Canlı TV ve Yayın Menüleri:**
     - **`ChannelManagerScreen.kt`:** Kategori görünürlük rozetleri, "Tümünü Göster" kumanda butonları ve odak çerçeveleri altın/siyah zıt renk kontrastıyla yenilendi.
     - **`StreamStatsOverlay.kt`:** Hız, FPS, donanım tünelleme, ses formatı ve tampon ilerleme çubuğu kehribar altın temasına kavuşturuldu.
     - **`MultiViewScreen.kt`:** Çoklu kanal izleme kılavuzları, odak çerçeveleri, ses geçiş rozetleri ve kanal arama menüsü tamamen 3-renk temasına taşındı.
     - **`GuidePreview.kt` & `FavoritesOverlay.kt`:** PIP video çerçevesi ve kategori çipleri paletle eşitlendi.
  4. **Filmler & Diziler (VOD):**
     - **`VodScreens.kt` & `VodDetailScreens.kt`:** MaterialTheme colorScheme üzerinden otomatik olarak `GecePalette` ile senkronize edildi.
     - **`VodPlayerScreen.kt`:** Dizi bölüm kartları, bölüm listesi yan çekmecesi ve harici oynatıcı seçim pencereleri grafit kart ve kehribar altın odak çerçeveleriyle güncellendi.
  5. **Sanal Kumanda ve Fare Menüleri:**
     - **`RemoteScreen.kt`:** Mavi tema yerine zemin derin siyah (`#090A0F`), TV durum kartı altın gradyan, D-Pad ve Ses/Kanal kaydırakları grafit yüzey, orta "OK" butonu ve dokunmatik fare alanı asil kehribar altın ve zıt simsiyah metin ile baştan aşağı yenilendi.
     - **`VirtualMouseOverlay.kt`:** Tıklama dalgası ve nişangah altın vurguyla senkronize edildi.
* **Versiyon ve Dağıtım:**
  - `app/build.gradle.kts`: `versionCode = 40`, `versionName = "0.12.19"`.
  - `.\gradlew compileDebugKotlin`: Başarılı (0 hata).
  - `.\gradlew assembleRelease`: Başarılı (`GECEiptv-release.apk` - 13.45 MB).
  - `bygokhanege-svg/geceiptv`: `main` dalına commit atıldı ve `v0.12.19` etiketi pushlandı.
  - `bygokhanege-svg/geceiptv-releases`: `v0.12.19` sürümü oluşturulup APK yüklendi.
* **Doğrulama:**
  - `https://api.github.com/repos/bygokhanege-svg/geceiptv-releases/releases/latest` üzerinden `v0.12.19` ve APK varlığı doğrulandı.

---

### 29. Televizyon (TV) Arayüzü & Kabuk Bileşenleri 3-Renkli Palet ve Zıt Kontrast Yenilemesi (v0.12.20)
* **Kullanıcı Talebi:** "televizyon içinde renkleri bu şekilde yapalım." (Mobil ve alt ekranlarda uygulanan 3-renkli kurumsal renk paleti ve yüksek kontrastlı okunabilirlik sisteminin televizyon arayüzüne, kumanda odak sistemine ve kabuk bileşenlerine uygulanması).
* **Uygulanan Değişiklikler:**
  1. **Televizyon Dashboard & Kabuk (`DashboardScreen.kt`):**
     - **TV Üst Katman Zemin Radyal Gradyanı:** Eski koyu mavi-mor tonlar yerine `GecePalette.CardElevated` ve `GecePalette.CanvasBg` (`#090A0F`) radyal gradyanına bağlandı.
     - **TV Üst Başlık (Header Row):** Logo kutusu `GecePalette.CardSurface`, altın mikro çerçeve `GecePalette.Accent.copy(0.45f)`, başlık saf beyaz (`#FFFFFF`), alt metin arduvaz gümüşü (`#94A3B8`).
     - **Aktif Durum & Lisans Kartı:** D-pad odaklandığında `GecePalette.AccentSubtle`, altın çerçeve, durum göstergesi parıltılı kehribar altın (`#F59E0B`).
     - **Saat & Tema Değiştirici:** Grafit kart yüzeyleri, TV kumanda odaklandığında altın ışıma ve zıt ikon renklendirmesi.
     - **İstatistik Kartları (`StatCard`):** Sayılar beyaz, etiketler gümüş, durum noktaları asil kehribar altın.
     - **Sunucu Hızlı Seçim Çipleri (`ProviderQuickChipsBar`):** Eski camgöbeği/yeşil tonlar kaldırıldı; grafit zemin, altın çerçeve ve aktif seçimde kehribar altın rozet ile senkronize edildi.
     - **Medya Odak Kartları (`DashboardMediaHeroCard` & `DashboardMediaHubGrid`):**
       - Canlı TV, Filmler ve Diziler dev odak kartlarındaki neon gradyanlar temizlendi.
       - D-pad ile kartın üzerine gelindiğinde `2.5.dp` `GecePalette.Accent` altın çerçeve, altın simge arka planı ve simge rengi **Simsiyah (Jet Black `#000000`)** zıt kontrast kuralına kavuşturuldu.
       - Kart içi durum pilleri ("Yayın Akışı", "Yeni Eklenenler", "Popüler Diziler") altın zemin üzerine siyah metinle kumanda mesafesinden anında okunur kılındı.
     - **Hızlı Araçlar (`DashboardQuickToolsRow` & `QuickToolCard`):** 5 hızlı araç kartı (Çoklu TV, Arama, Favoriler, QR Giriş, Ayarlar) `GecePalette.Accent` vurgusuna taşındı. Odaklandığında simge zemini dolgun altın, simge ise simsiyah zıt renk oldu.
     - **İzlemeye Devam Et (`AllContinueWatchingDialog` & `AllContinueWatchingCard`):** Diyalog konteyneri, kapatma butonu ve kartlar 3-renk temasına taşındı.
  2. **Televizyon Yan Gezinme Çubuğu & Üst Bar (`MainScreen.kt`):**
     - **TV Üst Çubuk:** Zemin `GecePalette.CanvasBg`, sınır `GecePalette.CardBorder`, logo kutusu `GecePalette.CardSurface`, durum noktası kehribar altın.
     - **TV Yan Rayı (`NavigationRail`):** Genişleyen çekmece ve sabit sol ray `GecePalette.CanvasBg` ile `GecePalette.CardSurface` yatay gradyanına bağlandı.
     - **Kumanda Odak Kontrastı (`RailItem`):** D-pad ile yan menü elemanlarına gelindiğinde buton zemini kehribar altına (`#F59E0B`) dönerken, ikon, metin ve gösterge çizgisi **Simsiyah (`#000000`)** zıt renge bürünerek TV kumanda mesafesinden kusursuz okunabilirlik sağlandı. Seçili elemanlarda koyu zemin üzerinde altın vurgu korundu.
     - **Ray Tema Butonu (`ThemeToggleRailItem`):** Eski çivit mavisi (`#6366F1`) kaldırılarak `GecePalette.Accent` ile eşitlendi; odaklandığında zemin altın, ikon ve anahtar simsiyah yapıldı.
* **Versiyon ve Dağıtım:**
  - `app/build.gradle.kts`: `versionCode = 41`, `versionName = "0.12.20"`.
  - `.\gradlew compileDebugKotlin`: Başarılı (0 hata).
  - `.\gradlew assembleRelease`: Başarılı (`GECEiptv-release.apk` - 13.45 MB).
  - `bygokhanege-svg/geceiptv`: `main` dalına commit atıldı ve `v0.12.20` etiketi pushlandı.
  - `bygokhanege-svg/geceiptv-releases`: `v0.12.20` sürümü oluşturulup APK yüklendi.
* **Doğrulama:**
  - `https://api.github.com/repos/bygokhanege-svg/geceiptv-releases/releases/latest` üzerinden `v0.12.20` ve `GECEiptv-release.apk` asset'inin aktif olduğu doğrulandı.

---

### 30. Televizyon, Tablet ve Telefon İçin Lüks Sinematik Açılış (Splash) Ekranı (v0.12.21)
* **Kullanıcı Talebi:** "açılışa şık bir splash ekran olsun hem televizyon hemde telefon ve tablet için."
* **Uygulanan Değişiklikler:**
  1. **Sinematik ve Lüks Splash Ekranı (`SplashScreen.kt`):**
     - `app/src/main/java/app/opentv/ui/SplashScreen.kt` oluşturuldu.
     - **3-Renkli Kurumsal Palet & Ortam Işığı:**
       - Zemin: Derin Obsidyen Siyah (`#090A0F`).
       - Logonun arkasında nefes alan, genişleyen ve nabız gibi atan kehribar altın radyal ortam ışıması (`radialGradient` ve `glowPulse`/`haloScale` sonsuz animasyonları).
     - **Lüks Amblem & Logo Kutusu:**
       - TV'de `116.dp` (24dp köşe), Tablette `100.dp`, Mobilde `84.dp` boyutunda çok katmanlı altın gradyan çerçeveli (`#FFDF70` -> `#F59E0B` -> `#B45309`) ve grafit zeminli logo konteyneri.
       - Dış kısımda genişleyen ve nabız gibi atan yarı saydam altın hale halkası.
       - Yaylı giriş ölçeklenmesi (spring scale 0.75 -> 1.0) ve alfa geçişi.
     - **Marka & Rozet Tipografisi:**
       - "GECE" (Asil Kehribar Altın), "İPTV" (Kar Beyazı Kristal) ve "ULTRA" (Zıt Siyah/Altın dolgulu mikro rozet).
       - Alt başlık: "PREMIUM MEDYA & YAYIN MERKEZİ" (Arduvaz gümüşü, geniş harf aralığı `2.5.sp`).
     - **Cihaz Sürümü Tanımlama Rozeti (Pill):**
       - TV: `[ 📺 ANDROİD TV • 4K ULTRA HD ]`
       - Tablet: `[ 📱 TABLET EDİSYONU • ÇOKLU EKRAN ]`
       - Telefon: `[ 📱 MOBİL EDİSYON • ULTRA HIZLI ]`
     - **İlerleme ve Sistem Durum Geri Bildirimi:**
       - İnce ve zarif altın gradyan ilerleme çubuğu (Progress bar).
       - Dinamik aşamalı sistem durum bildirimleri:
         - "Donanım hızlandırma ve ses motoru başlatılıyor…"
         - "Kanal listeleri ve medya veritabanı yükleniyor…"
         - "Arayüz hazırlandı, hoş geldiniz…"
     - **Erken Atlama & Kumanda Desteği:**
       - TV kumandasından "OK / Enter" tuşuyla veya ekrana dokunarak veritabanı yüklendiğinde anında açılışı tamamlama desteği.
  2. **Ana Uygulama Entegrasyonu & Akıcı Geçiş (`MainActivity.kt`):**
     - Uygulama ilk açıldığında `sourcesUi.loaded` henüz `false` iken eski basit çember gösterge (`CircularProgressIndicator`) kaldırıldı.
     - `SplashScreen`, `AnimatedVisibility` ile üst katmana yerleştirildi.
     - Arka planda veritabanı yüklenir yüklenmez `NavHost` sessizce ve donma/takılma olmadan oluşturuldu.
     - Splash süresi dolduğunda veya kullanıcı erken geçiş yaptığında 600ms süren sinematik bir fade-out (`FastOutSlowInEasing`) ile doğrudan ana ekrana akıcı geçiş sağlandı.
* **Versiyon ve Dağıtım:**
  - `app/build.gradle.kts`: `versionCode = 42`, `versionName = "0.12.21"`.
  - `.\gradlew compileDebugKotlin`: Başarılı (0 hata).
  - `.\gradlew assembleRelease`: Başarılı (`GECEiptv-release.apk` - 13.45 MB).
  - `bygokhanege-svg/geceiptv`: `main` dalına commit atıldı ve `v0.12.21` etiketi pushlandı.
  - `bygokhanege-svg/geceiptv-releases`: `v0.12.21` sürümü oluşturulup APK yüklendi.
* **Doğrulama:**
  - `https://api.github.com/repos/bygokhanege-svg/geceiptv-releases/releases/latest` üzerinden `v0.12.21` ve `GECEiptv-release.apk` asset'inin aktif olduğu doğrulandı.

---

### 31. Profesyonel & Glassmorphism Sağlayıcı Ekleme Menüsü (v0.12.22)
* **Kullanıcı Talebi:** "sağlayıcı ekleme sayfası çok sade kaldı. daha profesonel bir menü haline getirelim o kısmı. gerekirse glassmoprhsym olarak şık yapalım. tv tablet ve telefon için aynı menü olsun"
* **Uygulanan Değişiklikler:**
  1. **Lüks Glassmorphism & Sinematik Atmosfer (`AddSourceScreen.kt`):**
     - **Arka Plan:** Derin obsidyen gece siyahı (`#090A0F`) üzerinde kehribar altın radyal ışımalar.
     - **Ana Gövde Cam Kartı (Frosted Glass Card):** Dumanlı yarı saydam cam zemin (`Brush.verticalGradient(listOf(Color(0xE6141926), Color(0xD90E121D)))`), ince altın/gümüş ışık kırılmalı çerçeve (`1.dp` gradient border) ve 24dp kavisli köşeler.
     - **Giriş Alanları (Glass Inputs):** Yarı saydam koyu cam kutular (`Color(0x800E121D)`), odaklandığında kehribar altın çerçeve ve altın etiketler.
  2. **Birleşik Menü Standardı (TV, Tablet ve Telefon Eşitliği):**
     - TV cihazlarının ilk açılışta zorunlu QR ekranına yönlendirilmesi kaldırıldı; tüm cihazlar doğrudan aynı zengin ve profesyonel cam menüyle karşılanır.
     - Dileyen kullanıcılar için menü içerisine entegre edilmiş şık bir **"Telefondan Kolay Kurulum (QR Kod)"** kartı eklendi.
  3. **Segmentli Protokol Seçim Kartları (Xtream, M3U, Stalker):**
     - Basit çipler yerine 3 adet bağımsız lüks cam kart:
       - **Xtream Codes API:** Sunucu, kullanıcı adı ve şifre ile tam arşiv (*Önerilen • En Hızlı* rozetli).
       - **M3U Çalma Listesi:** M3U / M3U8 veya dosya bağlantısı desteği.
       - **Stalker Portal:** MAG cihazı ve MAC adresi yetkilendirmesi desteği.
     - Seçili kartta kehribar altın dolgu, altın çerçeve ve kumanda odak göstergeleri.
  4. **Kullanılabilirlik & Akıllı Yardımcılar:**
     - **Şifre Göster/Gizle Butonu:** Kumanda ve mobilde şifre yazarken hataları önleyen göz ikonu eklendi.
     - **Akıllı URL Analizcisi:** Tam bir Xtream linki yapıştırıldığında sunucu, kullanıcı adı ve şifreyi otomatik ayrıştırıp ilgili kutulara yerleştirme mantığı korundu ve güçlendirildi.
     - **Gelişmiş MAG Kimlikleri:** Serial (`sn`), Device ID 1 ve 2 alanları açılır kapanır zarif cam kart içine alındı.
     - **Gelişmiş Ayarlar:** Özel EPG URL ve User-Agent alanları cam akordiyon içerisine toplandı.
  5. **Yüksek Kontrastlı Butonlar & Durum Geri Bildirimi:**
     - Test başarılı (zümrüt yeşili) ve hata (yakut kırmızısı) yarı saydam cam bilgi kartları.
     - **"Bağlantıyı Test Et"**: Şık yarı saydam çerçeveli cam buton.
     - **"Kaydet ve Yayınları Başlat"**: Dolgun kehribar altın gradyan zemin ve **Simsiyah (Jet Black `#000000`)** yüksek kontrastlı yazı.
* **Versiyon ve Dağıtım:**
  - `app/build.gradle.kts`: `versionCode = 43`, `versionName = "0.12.22"`.
  - `.\gradlew compileDebugKotlin`: Başarılı (0 hata).
  - `.\gradlew assembleRelease`: Başarılı (`GECEiptv-release.apk` - 13.44 MB).
  - `bygokhanege-svg/geceiptv`: `main` dalına commit atıldı ve `v0.12.22` etiketi pushlandı.
  - `bygokhanege-svg/geceiptv-releases`: `v0.12.22` sürümü oluşturulup APK yüklendi.
* **Doğrulama:**
  - `https://api.github.com/repos/bygokhanege-svg/geceiptv-releases/releases/latest` üzerinden `v0.12.22` ve `GECEiptv-release.apk` asset'inin aktif olduğu doğrulandı.

---

### 32. Sağlayıcılar Ekranı Yayın Formatı Butonları Mobil Düzen Optimizasyonu (v0.12.23)
* **Kullanıcı Talebi:** "telefon için sağlayıcılar bölümündeki otomatik , hls ve mpeg-ts butonları telefonda orantısız duruyor. butonları ufaltarak yazıları da kısaltarak düzen sağlayalım."
* **Uygulanan Değişiklikler:**
  1. **Yayın Formatı Butonlarının Kompaktlaştırılması & Kısaltılması (`ProvidersScreen.kt`):**
     - Uzun ve taşmaya neden olan metinler sadeleştirildi:
       - *"Otomatik (Önerilen)"* ➔ **"Otomatik"**
       - *"HLS (.m3u8)"* ➔ **"HLS"**
       - *"MPEG-TS (.ts)"* ➔ **"MPEG-TS"**
     - Hantal Material 3 butonları yerine `34.dp` yükseklikte, `8.dp` kavisli köşeli modern ve kompakt segment tasarımı uygulandı.
     - Telefondaki `StreamFormatSelector` içinde her üç buton da eşit genişliğe (`Modifier.weight(1f)`) sahip olacak şekilde ayarlandı. Böylece butonlar kart genişliğini 3 eşit parçaya simetrik ve orantılı olarak bölerek taşma veya ezilmeyi tamamen engelledi.
     - **3-Renkli Kurumsal Palet & Yüksek Kontrast:**
       - Seçili format: Asil kehribar altın (`#F59E0B`) zemin üzerinde **Simsiyah (Jet Black `#000000`)** kalın metin.
       - Seçili olmayan formatlar: Koyu grafit kart yüzeyi (`#1A1F2E`) ve gümüş metin.
       - TV kumandası D-pad gezinmesinde altın odak çerçeveleri korundu.
* **Versiyon ve Dağıtım:**
  - `app/build.gradle.kts`: `versionCode = 44`, `versionName = "0.12.23"`.
  - `.\gradlew compileDebugKotlin`: Başarılı (0 hata).
  - `.\gradlew assembleRelease`: Başarılı (`GECEiptv-release.apk` - 13.44 MB).
  - `bygokhanege-svg/geceiptv`: `main` dalına commit atıldı ve `v0.12.23` etiketi pushlandı.
  - `bygokhanege-svg/geceiptv-releases`: `v0.12.23` sürümü oluşturulup APK yüklendi.
* **Doğrulama:**
  - `https://api.github.com/repos/bygokhanege-svg/geceiptv-releases/releases/latest` üzerinden `v0.12.23` ve `GECEiptv-release.apk` asset'inin aktif olduğu doğrulandı.

### 33. Filmler ve Diziler Menüsüne Sinematik Vitrin (Featured Hero Carousel) Entegrasyonu (v0.12.24)
* **Kullanıcı Talebi:** "film veya dizi menüsüne tıklandığında üst kısımda büyük kapak fotoğrafları ile otomatik ve manuel slider şeklinde afiş fotoğrafı filmin kaç puan aldığı , filmin yada dizinin özeti gibi seçenekler gelsin istiyorum onun altında menümüz olduğu gibi devam edebilir. bu çok kasmaya donmaya sebep olur mu ?"
* **Mimari & Performans Analizi:**
  - Sadece en seçkin 5-6 içerik kullanılır.
  - Ekstra ağ isteği yapılmaz (zaten çekilmiş olan kütüphanedeki `recommended` ve `recentlyAdded` listesinden filtrelenir).
  - Coil bellek ve disk önbelleklemesi sayesinde görsel dönüşleri RAM'den milisaniyede yapılır.
  - Jetpack Compose Pager yapısıyla sadece ekrandaki slayt GPU'da işlenir. Kullanıcı listeyi aşağı kaydırdığında vitrin ekran dışına çıktığı için arka planı meşgul etmez (sıfır donma/kasma garantisi).
* **Uygulanan Değişiklikler:**
  1. **`VodHeroCarousel.kt` Bileşeni Geliştirildi:**
     - `HorizontalPager` ve `rememberPagerState` tabanlı akıcı sinematik slider.
     - 6 saniyede bir otomatik geçiş (`LaunchedEffect` + `delay(6000)` + `animateScrollToPage(tween(650))`).
     - Kullanıcı dokunduğunda veya kumandayla odaklandığında otomatik geçişi akıllıca duraklatma (`isDragged`, `isUserInteracting`).
     - **Görsel Tasarım:**
       - Yüksek çözünürlüklü yatay backdrop (arka plan) görseli (`AsyncImage`).
       - Çok katmanlı koyu degrade (gradient scrim) gölgelendirme (alt ve soldan şık siyah erime).
       - 🌟 *"ÖNE ÇIKAN FİLM"* / *"ÖNE ÇIKAN DİZİ"* kehribar altın rozet.
       - ⭐ IMDb/TMDB Puan Rozeti (`⭐ 8.5`).
       - 📅 Çıkış Yılı ve Tür etiketleri.
       - 📝 2-3 satırlık akıcı ve merak uyandıran konu özeti (plot).
       - 🎮 Butonlar: **"Hemen İzle" / "Bölümleri Aç"** (Altın sarısı) ve **"Detay"** (Yarı şeffaf cam efektli).
       - 🔘 Alt kısımda modern, aktif olduğunda genişleyen hap biçiminde dot indicators ve büyük ekranlar için manuel ok butonları.
  2. **`VodScreens.kt` Entegrasyonu:**
     - `MoviesScreen`: Hem telefon hem TV modundaki `LazyColumn`'ın en tepesine `heroItems` vitrini yerleştirildi. Altında `ContinueWatchingRow`, `MoviePosterRow` ve kategori menüleri kesintisiz devam eder.
     - `SeriesScreen`: Hem telefon hem TV modundaki `LazyColumn`'ın en tepesine `heroItems` vitrini yerleştirildi.
* **Versiyon ve Dağıtım:**
  - `app/build.gradle.kts`: `versionCode = 45`, `versionName = "0.12.24"`.
  - `.\gradlew compileReleaseKotlin`: Başarılı (0 hata).
  - `.\gradlew assembleRelease`: Başarılı (`GECEiptv-release.apk` - 13.47 MB).
  - `bygokhanege-svg/geceiptv`: `main` dalına commit atıldı ve `v0.12.24` etiketi pushlandı.
  - `bygokhanege-svg/geceiptv-releases`: `v0.12.24` sürümü oluşturulup APK yüklendi.
* **Doğrulama:**
  - `https://github.com/bygokhanege-svg/geceiptv-releases/releases/download/v0.12.24/GECEiptv-release.apk` aktif hale getirildi.

### 21. Canlı TV Player, VOD Hero, Kumanda Fare ve Anti-Freeze İyileştirmeleri
* **Kullanıcı İstekleri:**
  * Destek kanallarına Telegram adresinin (`@geceiptv`) eklenmesi.
  * Canlı yayın oynatıcı alt kontrol çubuğuna ISS bant daraltmalarını önleyici DoH (DNS-over-HTTPS) "Anti-Freeze" butonu eklenmesi.
  * Oynatıcı kontrollerinin tek satıra sığacak şekilde modernleştirilmesi: Play/Pause/İleri/Geri yuvarlak butonlar, kırmızı yuvarlak kayıt ikonu, "CC" altyazı butonu ve "PİP" mini oynatıcı etiketi.
  * Film ve dizi vitrinindeki (Hero Carousel) banner genişliğinin tam ekran (`fillMaxWidth()`) yapılması ve kategori menüsünün bannerın altından başlatılması.
  * Dizi oynatıcısında bölümler açıldığında D-pad odaklanmasının doğrudan açık olan bölüme konumlandırılması.
  * Sanal kumanda fare imlecinde tıklama halkasının okun tam ucunda gösterilmesi.
* **Teknik Çözüm:**
  * `PlayerScreen.kt`, `VodScreens.kt`, `VodPlayerScreen.kt`, `VirtualMouseOverlay.kt`, `AntiFreezeDns.kt` güncellendi ve derleme doğrulandı.

---

### 22. Mobil/Tablet Anasayfa Çoklu Ekran ve 5'li Hızlı Araçlar Düzenlemesi
* **Kullanıcı İstekleri:**
  * Telefon ve tablet anasayfasında sağ üst köşedeki Ayarlar butonunun yanındaki Çoklu Ekran butonunun kaldırılması.
  * Sayfanın altındaki Favoriler, Arama, Çoklu Ekran, Kumanda QR ve Ayarlar'dan oluşan 5 menülük hızlı işlem çubuğunun (`PhoneQuickToolsBar`), 4'lü medya gridi (Diziler/Çoklu Ekran) ile "İzlemeye Devam Et" bölümünün arasına taşınması.
* **Teknik Çözüm ve Kararlar:**
  * `MainScreen.kt` ve `DashboardScreen.kt` içindeki telefon üst çubuğundan (`PhoneTopBar`, `PhoneHeaderBar`) Çoklu TV butonu kaldırıldı.
  * `PhoneQuickToolsBar` bileşeni `PhoneMainNavigationCards` (Canlı TV, Filmler, Diziler, Çoklu TV) hemen altına, `continueWatching` ("İzlemeye Devam Et") bölümünün hemen üstüne yerleştirildi; alt kısımdaki eski konumu temizlendi.
### 23. Netflix Tarzı Profil Yönetimi, PIN Kilidi ve Altyazı Dil Sıralaması
* **Kullanıcı İstekleri:**
  * Profiller bölümünün Netflix tarzında yeniden tasarlanması.
  * Klavye düzeninin TV (kumanda D-pad), telefon ve tablete (sanal dokunmatik klavye) göre uyarlanması.
  * Kullanıcının kendi profiline isteğe bağlı 4 haneli PIN şifresi koyabilmesi; şifreli profile girişte PIN doğrulama ekranı açılması.
  * Kullanıcının kendi menülerini profil bazında düzenleyebilmesi (Canlı TV, Filmler, Diziler, Çoklu Ekran açma/kapatma).
  * İnternetten altyazı aratıldığında Türkçe altyazıların en üstte, İngilizce altyazıların onun altında yer alması, diğer dillerin sonraya sıralanması.
* **Teknik Çözüm ve Kararlar:**
  * `Models.kt` & `OpenTvDatabase.kt`: Room veritabanı sürümü 11'den 12'ye yükseltildi (`MIGRATION_11_12`). `Profile` tablosuna `avatarId`, `pin`, `isKids`, `liveEnabled`, `moviesEnabled`, `seriesEnabled`, `multiViewEnabled` sütunları eklendi.
  * `ProfileAvatars.kt`: 8 tematik Netflix tarzı degrade renkli ve ikonlu avatar koleksiyonu oluşturuldu.
  * `ProfilesScreen.kt`: Sinematik "Kim İzliyor?" Netflix arayüzü, "+ Profil Ekle" kartı, "Profilleri Yönet" kalem modu, 4 daireli PIN kilit ekranı (`ProfilePinPromptDialog`) ve profil düzenleme ekranı (`ProfileEditScreen`) geliştirildi. TV'de D-pad `OnScreenKeyboard`, telefon/tablette yerel dokunmatik klavye sağlandı.
  * `MainScreen.kt` & `DashboardScreen.kt`: Seçili profilin menü tercihleri (`liveEnabled`, `moviesEnabled`, vb.) dinamik olarak ana ekrana ve navigasyon barlarına yansıtıldı.
  * `OnlineSubtitleService.kt` & `VodPlayerScreen.kt`: `isTurkish` ve `isEnglish` filtreleri genişletilerek altyazı sonuçlarında Türkçe (🇹🇷) 1. sıraya, İngilizce (🇬🇧) 2. sıraya, diğer diller alfabetik olarak sonraki sıralara yerleştirildi.
  * `.\gradlew compileDebugKotlin` ile derleme hatasız doğrulandı (`BUILD SUCCESSFUL in 58s`). Kullanıcı talimatı gereği release build ve git push bekletilmektedir.

---

## 📌 Hızlı Referans & Mimari Kurallar
1. **Ekran Genişliği Sınırı (Mobil Breakpoint):**
   * `LocalConfiguration.current.screenWidthDp < 600` mobil/telefon cihazları temsil eder.
   * Tablet: `screenWidthDp >= 600 && !isTelevision`.
   * Televizyon: `isTelevision == true`. D-pad gezinmesi ve TV rayları aktiftir.
2. **GitHub Bilgileri:**
   * Kaynak Kod Reposu (Private): `https://github.com/bygokhanege-svg/geceiptv.git`
   * Güncelleme / Dağıtım Reposu (Public): `https://github.com/bygokhanege-svg/geceiptv-releases.git`
   * Release APK İndirme Bağlantısı: `https://github.com/bygokhanege-svg/geceiptv-releases/releases/download/v0.12.24/GECEiptv-release.apk`
   * Release APK Adı: `GECEiptv-release.apk`
   * Destek E-postası: `geceiptv@gmail.com`
3. **Derleme Komutları:**
   * Kod Doğrulama: `.\gradlew compileDebugKotlin` veya `.\gradlew compileReleaseKotlin`
   * Release APK Üretimi: `.\gradlew assembleRelease`
   * Çıktı Yolu: `app\build\outputs\apk\release\GECEiptv-release.apk`



