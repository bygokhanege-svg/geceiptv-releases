# Changelog

## 0.12.16

- **Arama Kelimeleri Arasındaki '+' İşareti Sorunu Çözüldü:** Telefonda sanal kumandadan, URL yönlendirmelerinden veya ekran klavyesinden kaç kelime yazılırsa yazılsın kelime aralarına `+` işareti girmesi tamamen engellendi (`android.net.Uri.encode` ve otomatik sanitize işlemiyle boşluklar korundu). Veritabanı aramaları artık eksiksiz ve tam eşleşmeyle sonuç bulur.
- **Sanal Fare (Mouse) ve Touchpad Katmanlama (Z-Index & Window) Düzeltmesi:** Favoriler menüsü ve Tüm İzlemeye Devam Et menüleri `Dialog` penceresinden modern hiyerarşik bindirme (in-hierarchy modal overlay) mimarisine taşındı. Böylece telefon uygulamasındaki kumanda sekmesinden fare imleci (mouse) ve touchpad hareketleri menülerin üzerinde kusursuz şekilde görünür, menü kartlarına ve butonlarına doğrudan tıklama ve kaydırma yapılabilir.
- **Yasal Uyarı & Sözleşme Modalı Fare Desteği:** Yasal uyarı modalı da aynı katman mimarisine geçirilerek sanal kumanda faresiyle kabul etme ve tıklama yeteneği kazandırıldı.

## 0.12.15

- **Favoriler Menüsü Kumanda (D-pad) Gezinme Düzeltmesi:** Favoriler menüsü açıldığında kumanda odağının doğrudan "TÜMÜ" sekmesine konumlanması sağlandı; menü içi odak tuzakları temizlendi ve kumanda ile sorunsuz gezinme garantilendi.
- **Tüm İzlemeye Devam Et Menüsü & Tekil Silme:** Ana sayfadaki "İzlemeye Devam Et" başlığındaki içerik sayısına (veya "+ X İçerik Daha" butonuna) tıklandığında tüm devam edilen içerikleri listeleyen modern cam ızgara menü açılır; buradan içerikler tek tek veya topluca silinebilir.
- **Video Oynatıcı Alt Menü Düzeni:** İleri/geri sarma ve oynat/duraklat butonlarının ardından boşluk bırakılarak sırasıyla Altyazı, Ses, Oynatma Hızı, Sığdır, Harici Oynatıcı, Info, Tüm Bölümler ve Sonraki Bölüm butonları konumlandırıldı.
- **Yayın Özellikleri → Info:** Yayın özellikleri butonunun etiketi kullanıcı isteğine göre "Info" olarak sadeleştirildi.
- **Kompakt Buton & Font Optimizasyonu:** Oynatıcı çubuğundaki tüm butonlar ve yazı boyutları TV ekranına ve yatay mobil görünüme tek bir satırda tam sığacak şekilde optimize edildi.

## 0.12.14

- **Netflix Tarzı Yuvarlak Oynatıcı Butonları:** İleri (+15s) ve geri (-15s) sarma ile oynat/duraklat butonları tamamen yuvarlak, yazısız, zarif Netflix tarzı ikonlarla yenilendi. Odaklandığında büyüyen ve parlayan D-pad kumanda uyumlu yapı kuruldu.
- **YouTube Tarzı Video Oynatma Hızı:** Oynatıcı çubuğuna eklenen hız butonu ile video akışı 0.5x, 0.75x, 1.0x (Normal), 1.25x, 1.5x, 1.75x ve 2.0x hızlarında gerçek zamanlı kontrol edilebilir.
- **Dinamik En-Boy Oranı & Sığdırma (Aspect Ratio):** Sayfaya Sığdır (Orijinal), 16:9 Geniş Ekran, 4:3 Klasik TV, Tam Ekran (Zoom) ve Ekrana Yay (Stretch) seçenekleri video ekranını anında ve kesintisiz yeniden boyutlandırır.
- **Dizi Sezon & Bölüm Önizleme Yan Menüsü (Drawer):** Dizi izlerken sağ taraftan açılan düşük opasiteli modern menü ile diğer sezonlara ve bölümlere resimleriyle birlikte anında göz atılabilir, tek tıkla istenen bölüme geçilebilir.
- **Akıllı Harici Oynatıcı Tespiti:** Cihazda (TV veya telefonda) yüklü olan tüm harici oynatıcılar (VLC, MX Player, Just Player, Kodi, mpv vb.) otomatik bulunur ve kendi logolarıyla seçme şansı sunulur.
- **Ana Sayfa Hızlı Araçlar Menüsüne Favoriler Bölümü:** Arama ve QR arasına eklenen altın sarısı yıldız simgeli Favoriler butonu ile favori dizi ve filmler kategorik karusel ve ızgaralarla tek bir yerde toplandı.
- **İzlemeye Devam Et Yönetimi:** Kullanıcının listeden tek tek içerik silebilmesi veya tüm listeyi tek dokunuşla onaylı şekilde temizleyebilmesi sağlandı.
- **Sadeleştirilmiş Dijital Saat:** Oynatıcıdaki sağ üst saat kutusuz, çerçevesiz, saf ve şık bir dijital saate dönüştürüldü.
- **Sadeleştirilmiş Ayarlar:** Ana menü 8 temel başlıkta toplanarak diğer ayarlar "DİĞER AYARLAR" menüsü altına taşındı.

## 0.12.13

- **Donanım Multimedya Tünelleme (Hardware A/V Tunneling):** Destekleyen Android TV'lerde ses ve görüntü doğrudan TV'nin donanım çipine tünellenerek dudak-ses kayması (lip-sync lag) sıfırlandı; 60fps ve 4K spor yayınları için kusursuz akıcılık sağlandı.
- **Akıllı Otomatik Hata Kurtarma:** Tünelleme aktifken bir yayında kod çözücü hatası oluşursa motor anında standart boru hattına dönerek 250ms içinde yayını kesintisiz yeniden başlatır; kullanıcı asla siyah ekranda kalmaz.
- **Canlı Yayın Özellikleri & İstatistik Paneli (Nerd Stats):** Video izlerken alt çubuktaki "Yayın Özellikleri" butonu ile bağlantı hızı (Mbps), video çözünürlüğü ve ebatları, FPS, video/ses codec'leri, tampon sağlık yüzdesi ve tahmini dosya boyutu canlı olarak analiz edilir.
- **Sağ Üst Köşede Canlı Dijital Saat:** Canlı TV, film ve dizi izlerken ekranın sağ üst köşesinde şık, buzlu camlı ve her saniye güncellenen dijital saat hapı eklendi; Ayarlar > Oynatma menüsünden ve istatistik panelinden kolayca açılıp kapatılabilir.
- **Optimizasyon Profilleri Entegrasyonu:** ULTRA donanım profili için donanım tünelleme otomatik aktifleşirken, giriş seviyesi cihazlarda güvenlik amacıyla varsayılan kapalı bırakıldı.

## 0.12.12

- **Ana Dizin Gridleri Modernizasyonu:** Ana sayfadaki Canlı TV, Filmler ve Diziler alanları 82dp yükseklikte, camgöbeği/mor/indigo derin gradyanlı, buzlu cam ikon kutulu ve canlı/durum rozetli (`● CANLI`, `🎬 SİNEMA`, `🍿 SEZONLAR`) geniş ve şık kahraman kartlara dönüştürüldü.
- **Hızlı Araçlar ve İşlem Çubuğu:** Çoklu Ekran (Multi-View), Arama (Search), QR Bağlantı (Remote & Web) ve Ayarlar (Settings) 52dp kompakt ve şık araç paneline yerleştirildi.
- **Sağlayıcılar Arası Tek Tıkla Anında Geçiş (Quick Chips):** Eski çok adımlı akordiyon menü yerine ana sayfada her an görünür yatay kaydırılabilir sağlayıcı çipleri eklendi; kumandadan veya ekrandan tek dokunuşla sıfır gecikmeyle aktif sağlayıcı değiştirilebilir hale getirildi.
- **İngilizce ve Türkçe Tam Dil Desteği:** Ana sayfadaki tüm başlıklar, butonlar, istatistikler ve durum göstergeleri Türkçe ve İngilizce dillerine uyarlandı; cihaz dili veya ayarlar İngilizce olduğunda tüm ana sayfa İngilizce olarak görüntülenir.
- **Mobil Arayüz Geliştirmeleri:** 2x2 grid yapısında Canlı TV, Filmler, Diziler ve Çoklu Ekran modern mobil kartlarla yenilendi.

## 0.12.11

- **Çoklu Ekran (Multi-View) Çökme Onarımı:** Çoklu ekranda boş kutuya "+ Kanal Ekle"ye basıldığında veya kanal seçerken uygulamanın kapanmasına neden olan Jetpack Compose mükerrer anahtar (duplicate key) hatası giderildi.
- **Aktif Sağlayıcı İzolasyonu (Çoklu Ekran):** Çoklu ekran kanal ve kategori seçici artık tüm sağlayıcıların on binlerce kanalını tek seferde çekmek yerine yalnızca seçili aktif sağlayıcının kanallarını listeler.
- **Düşük RAM'li TV'ler İçin Koruma Kalkanı:** "Tüm Kanallar" görünümünde binlerce kanalın Compose listesine yığılmasını önlemek için ilk yükleme 200 kanalla sınırlandırıldı; kategori seçildiğinde akıcı ve hafif liste filtrelemesi sağlandı.
- **Güvenli Akış Bağlantısı:** Kanal oynatıcı bağlantıları ve seçim eylemleri `runCatching` kalkanına alınarak oluşabilecek olası ağ/çözücü istisnalarına karşı tam koruma sağlandı.

## 0.12.04

- **Telefon Arayüzü Film & Dizi Ekranı:** Sol dikey kategori rayı kaldırılarak üst yatay kaydırılabilir kategori çipleri, kategori arama butonu ve tam ekran 3 sütunlu modern afiş ızgarası getirildi.
- **Film Detay & "TV'ye Gönder" Butonu:** Kapak ve buton yerleşimi mobilde düzeltildi; dikey sıkışan TV'ye Gönder butonu tek satırlı yatay kaydırma çubuğuna alındı.
- **Ayarlar Menüleri Mobil Uyumu:** Cihaz ve Lisans, Sağlayıcılar, Kanallar, Görüntü & Oynatma ve Telefon Kumandası menülerindeki tüm kartlar ve yazılar telefon boyutlarına tam oturacak şekilde dikey/kaydırılabilir yapıya kavuşturuldu.
- **TV Ana Sayfa Buton Sıralaması:** Arama, Ayarlar ve Çoklu TV butonları üstteki Canlı TV / Film / Dizi ızgarasının hemen altına alındı.
- **3 Seçenekli Kategori Gizleme:** Canlı TV, Film ve Dizi kategorilerine basılı tutulduğunda doğrudan gizlemek yerine "Aç", "Gizle" ve "İptal" seçeneklerini sunan açılır diyalog eklendi.

## 0.12.03

- **Sabit TV Ana Sayfası:** Sol hareketli menü kaldırıldı, sabit ve şık gezinti butonları eklendi.
- **Aktif Sağlayıcı İzolasyonu:** Yalnızca seçili sağlayıcının içerikleri aktif olur.
- **Otomatik TV Eşitleme:** Telefondaki sağlayıcılar TV ile eşleşince otomatik TV'ye aktarılır.
- **Canlı TV Ülke Filtreleme:** Canlı TV ülke bazlı gruplandırıldı.
- **Gece/Gündüz Teması:** Tema değiştirme butonu saat ve tarihin yanına alındı.
- **Akıllı Telefon Kumandası & Cast:** Web kumandasına TV kısayolları ve TV'ye gönder özelliği eklendi.
- **Hızlandırılmış VOD:** Spesifik adet ve yüzde bildirimli adım adım VOD yükleme mekanizması.

## 0.11.8

- **Edit a saved provider.** Settings → Providers now has an **Edit** button on each provider, so you
  can fix a server address, password, MAC or name in place instead of removing and re-adding it —
  your favourites, hidden channels and per-provider settings are kept.
- **Stalker portals: fewer dropped sessions.** A portal that occasionally rejected a call
  mid-session (channels loading only "sometimes") now gets one automatic re-handshake and retry, so
  an intermittent refusal becomes a silent re-auth rather than an empty channel list or a channel
  that won't tune.
- **Player controls stay clear of the system bar off-TV.** On phones and tablets the bottom control
  bar (subtitles, audio, quality, aspect ratio) could be drawn under the navigation bar and cut off;
  it's now inset above it. No change on Android TV.

## 0.11.7

- **Stalker portals: closer to a real box.** Building on v0.11.6, OpenTV now sends the rest of the
  identity a real MAG set-top box presents at login — the auth token in the session cookie (not just
  the header), a hardware-version hash, a timestamp and the box's API signature — and it probes a
  couple more portal paths. Some Ministra/Stalker panels only return the channel list when they see
  all of this, so lines that connected but showed no channels have a better chance of loading. If a
  portal still comes up empty, please open an issue and say which panel software it runs.

## 0.11.6

- **Stalker portals: full set-top-box identity.** OpenTV now authenticates to Stalker/Ministra
  portals the way a real MAG box does — sending the device serial, `device_id`, a signature and a
  metrics blob derived from your MAC, instead of the MAC on its own. Many portals won't hand over
  the channel list or stream links to a box that only presents a MAC, which is why some lines that
  worked in other apps came up empty in OpenTV. This lets those portals authorise OpenTV. If a
  portal still fails, please open an issue and say which one — it helps a lot.

## 0.11.5

- **Add a provider from your phone or laptop.** The "Manage on phone or laptop" web page now has an
  **Add a provider** section — pick Xtream, M3U or Stalker portal, fill it in with a real keyboard,
  then Test and Add. Channels load straight onto the TV, so you never have to type a server address
  or a MAC on the remote again.
- **Fixed the "camera won't scan it?" web address.** The manage screen was printing that link without
  its access token, so typing it into a browser hit a dead "Not found". It now shows the full working
  URL, and the token is short enough to type.


## 0.11.4

The big DVR release. Recordings behave like Sky Q, you can record straight to a NAS, watch a
recording while it's still taping, and the whole interface now speaks 30 languages.

### Recording & DVR

- **Watch a recording while it's still recording.** Start playing something the moment it begins
  taping — OpenTV reads the growing file straight off disk, so it costs **zero extra connections**
  to your provider. On a single-stream account that's the difference between "wait until it's
  finished" and "watch now". Fast-forward and rewind work within whatever's been taped so far.
- **Record straight to a NAS.** Point recordings at a network share over SMB (Settings →
  Recordings → NAS, or set it up from the phone/laptop web manager) so a cheap box isn't boxed in
  by its own storage — and every OpenTV in the house can reach the same recordings.
- **Single-connection auto-switch.** On a one-stream provider, if a recording is due to start on
  another channel, OpenTV moves the screen onto that recording as it begins — with a **30-second
  warning** first and a *Keep watching* button if you'd rather not. No more "why did my live stream
  just cut out?" (Recordings → Recording behaviour → *Auto-switch when recording starts*.)
- **Clash handling with multi-provider fallback.** Book two overlapping recordings and, if more
  than one of your providers carries that channel, OpenTV records each from a **different provider**
  so neither is cut. On a single provider it flags the clash instead.
- **Recording padding.** Start each recording a minute early and run a few minutes past the listed
  end, so a late kick-off or an overrun isn't clipped. Adjustable per side.
- **Series links show the next episode.** A series link now tells you exactly when it next records.
- **Storage readout.** See how much space recordings use and how much is left.

### Live TV

- **Pause & rewind live TV** *(experimental, opt-in).* Holds the last couple of minutes so you can
  pause and jump back. Off by default — it uses more memory.

### Languages

- **30 languages.** The whole interface is translated into Spanish, French, German, Italian,
  Portuguese, Dutch, Polish, Russian, Turkish, Arabic, Simplified Chinese, Japanese, Korean, Hindi,
  Swedish, Danish, Finnish, Norwegian, Czech, Greek, Romanian, Hungarian, Ukrainian, Indonesian,
  Thai, Vietnamese, Bulgarian, Slovak, Croatian and Persian. Settings → Language (each listed in
  its own name), or leave it on *System* to follow your device.

### Fixes & polish

- Smoother watch-while-recording, especially from a NAS: playback now keeps a buffer behind the
  live edge so a network share doesn't stutter at the write head.
- Back never lands you on a stray live channel or drops you out by accident — coming back from a
  recording returns to your recordings, and Back on the home screen asks before it exits.
- Password fields are masked, with a show/hide toggle.
- Recording stability: NAS write timeouts and reconnect handling, so a quiet network doesn't
  silently freeze a capture.

### Notes

- Everything stays on your device — provider logins never leave the box.
- Free and open source (GPL-3.0): https://github.com/opentvproject/opentv


## 0.10.0

- **Movies & Shows, redesigned.** A Plex-style layout with big artwork, cast & director, "more with
  this cast" rows, and cleaned-up titles (no more "NF -" / "(KR)" junk). Optional: add your own free
  TMDB key in Settings → Metadata and OpenTV fills in any posters, backdrops, cast or synopses your
  provider left blank — the key stays on your device.
- **No more stuck "Loading movies & shows".** The movies/series catalogue is cached, so it loads
  instantly on later launches instead of re-downloading every time — and the live preview no longer
  stutters while it loads.
- **Record to a USB / external drive.** Settings → Recordings → USB, pick a folder on a plugged-in
  drive, and recordings write straight there (no storage permission needed) and play back in-app.
- **Recordings screen overhaul.** Scheduled recordings show *when* they'll record and sit in their
  own "Scheduled" section; failed ones have a **Retry**; each is tagged to a profile; and bookings
  re-arm on every launch so a force-stop or app update can't quietly drop them.

## 0.9.0

- **Manage your channels from a phone or laptop.** A new "Manage on phone or laptop" screen in
  Settings shows a QR code and a link — open it on any device on your wifi and you get a proper web
  page to browse, **rename**, hide, favourite and **reorder** channels with a real keyboard and
  mouse. Changes apply to the TV instantly. Local-only: the TV is the server, nothing touches a
  cloud.
- **Recording keeps going when you leave the app or the box sleeps.** OpenTV can now ask Android to
  exempt it from battery optimisation (Settings → Recording → "Recording in the background"), which
  is what keeps recordings running in standby and when you switch away — swiping OpenTV out of
  recents no longer stops a recording either.
- **Rename channels.** Give a channel your own name; it sticks and survives a guide refresh (from
  the web manager for now).
- **Tidier update notes.** The "update available" prompt shows a clean summary of what's new instead
  of build metadata.

## 0.8.0

- **No more endless "Loading channels".** If a provider fails to load or comes back empty (a wrong
  login, a dead server), the guide now shows a clear error with **Retry** and **Provider setup**
  buttons instead of spinning forever.
- **Set up on a phone without the QR dance.** Installing on a phone now shows the login form
  directly so you can type your details in; the QR code is only offered on a TV, where typing is the
  painful part.
- **Paste into the setup fields.** The URL, username and password fields — and the on-screen
  keyboard — now have a **Paste** button, so a copied Xtream line drops straight in.
- **MPEG-TS or HLS, your choice.** Each Xtream provider now has a **Stream format** toggle. If your
  panel only serves `.ts` and channels wouldn't play, switch to MPEG-TS and they will. Defaults to
  HLS, so nothing changes unless you need it.
- **Parental controls scrolls.** The hidden-categories list is reachable to the bottom now, however
  many categories your provider has.
- **A real Channel Manager.** Settings → Channels is now a browsable, two-pane manager: pick a
  category on the left, see its channels on the right, hide or favourite each one — including
  hidden channels so you can bring them back. With more than one provider, a **source filter** keeps
  them separate instead of merged into one list.

## 0.3.0

- **Profiles.** Add local profiles (just a name — no accounts) from the person icon in the top
  menu, and switch between them. Each profile keeps its own resume points and watched state, so
  your half-watched film doesn't show as watched on someone else's profile. Existing resume points
  become the default "Me" profile's — nothing is lost.
- **Continue Watching.** Movies and Shows now open with a "Continue watching" shelf for the active
  profile — pick up your last film or episode where you left off, with a progress bar on each.
- **Local sync (no servers).** Settings → Sync copies your continue-watching between two OpenTV
  devices over your own wifi: one device shares behind a six-digit code, the other receives. It's
  local-only — nothing leaves the house — the household answer to the cloud sync that died with
  Viewella.

- **Proper player controls.** A single control bar slides up from the bottom with play/pause,
  rewind/forward (where the stream allows it), and pickers for **subtitles, audio track, quality
  and aspect ratio**. It hides after a few seconds and any remote button brings it back —
  nothing is ever left painted permanently over the picture.
- **Captions that actually show.** Subtitles are now chosen from the real tracks in the stream
  (and can be turned off), instead of a blind on/off that often left the renderer enabled but
  nothing on screen. Audio-track switching works the same way for multi-language streams.
- **Page through the guide.** Earlier / Now / Later buttons above the guide move the timeline
  forward and back, so you can see what's on later this evening with a remote.
- **Live preview in the guide.** The highlighted channel now plays, muted, inside the preview
  pane. It uses a single player that stops the instant you go full-screen or leave the guide —
  no second decoder — and it can be turned off in Display & playback for older boxes.
- **Channel manager.** Settings → Channels: search for a channel and hide it from the guide (or
  favourite it). Hidden channels drop out of Live TV and search but stay here, greyed, so you can
  bring them back. Hiding covers every quality variant of a channel at once.
- **Parental controls.** Set a 4-digit PIN and mark categories as hidden — they drop out of the
  guide, All channels and search until you unlock them (and with a PIN set, the parental screen
  itself is locked). In Settings → Parental controls.
- **Unified search with an on-screen keyboard.** Search now has its own screen with a d-pad
  keyboard (no more "type on your phone") and covers **channels, movies and shows** together, with
  live results as you type. Reachable from the top menu.
- **Loading indicators for Movies and Shows.** Both now show a spinner while the catalogue is
  syncing instead of a premature "nothing here".
- **Settings and Search in the top menu.** Both now live top-right, next to Live TV / Movies /
  Shows, instead of being buried behind the guide's icons.
- **A single Settings screen.** One Settings hub — Providers (add / remove / re-test sources), TV
  guide, Display & playback, Parental controls, and About (version, a manual update check,
  licence and links).
- **Dark / light / follow-system.** A new appearance setting. TV still defaults to dark; pick
  Dark or Light to force it on any device.
- **Quality picker only when there's a choice.** No more "Quality: Standard" on single-quality
  channels; the picker appears only when a channel actually has multiple qualities.
- New **Display & playback** settings screen (the sliders icon in the guide).

## 0.2.0

- **Guide detail pane** — highlighting a channel now shows its logo, what's on now with
  start/end times and a live progress bar, a synopsis when the guide carries one, and what's
  on next. Press to watch full-screen.
- **Live progress fill** on the current programme in the guide grid, and a focus highlight on
  the highlighted channel.
- **Fix: never asks for your provider again.** A cold launch could briefly decide "first run"
  before your saved sources had loaded and drop you on the setup screen — it now waits for the
  sources to load before deciding what to show.
- **Fix: no second video decoder on the guide**, which was locking up low-end boxes (a
  Chromecast could freeze). The detail pane is logo + guide info; the one decoder lives
  full-screen.

## 0.1.0

First public release.

- Xtream Codes and M3U/M3U8 sources
- Streaming XMLTV guide parsing with incremental, non-destructive sync
- Live TV playback on Media3/ExoPlayer with IPTV-tuned retry behaviour
- Movies and series catalogue
- Favourites, categories, search
- In-app self-update for sideloaded installs
- Single APK for Android TV, Fire TV, phones and tablets
