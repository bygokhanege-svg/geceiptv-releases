/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.pairing

/**
 * Mobile Web Companion served directly from the TV.
 * Includes a Virtual Touch Remote, Channel Manager with "Play on TV" casting, and IPTV/M3U source installer.
 */
object ManagerPage {

    fun page(token: String): String = """
<!DOCTYPE html>
<html lang="tr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover, user-scalable=no">
<meta name="robots" content="noindex, nofollow">
<title>GECE İPTV Player • Akıllı Mobil Kumanda & Panel</title>
<style>
  :root {
    --bg: #090C15;
    --surface: #121726;
    --surface2: #1A2238;
    --surface3: #242E4C;
    --line: rgba(255, 255, 255, 0.10);
    --text: #F8FAFC;
    --muted: #94A3B8;
    --accent: #3B82F6;
    --accent-glow: rgba(59, 130, 246, 0.4);
    --gold: #F59E0B;
    --err: #EF4444;
    --ok: #10B981;
  }
  * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
  body {
    margin: 0; background: var(--bg); color: var(--text);
    font: 16px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    user-select: none; -webkit-user-select: none;
  }
  .wrap { max-width: 600px; margin: 0 auto; padding: 12px 14px 70px; }

  /* Header */
  header {
    display: flex; align-items: center; justify-content: space-between;
    padding: 10px 0 16px; border-bottom: 1px solid var(--line); margin-bottom: 14px;
  }
  .brand { display: flex; align-items: center; gap: 8px; font-weight: 800; font-size: 1.15rem; }
  .badge-live {
    display: inline-flex; align-items: center; gap: 6px; font-size: 0.75rem; font-weight: 700;
    color: var(--ok); background: rgba(16, 185, 129, 0.12); padding: 4px 10px; border-radius: 999px;
    border: 1px solid rgba(16, 185, 129, 0.25);
  }
  .badge-live::before {
    content: ''; width: 7px; height: 7px; border-radius: 50%; background: var(--ok);
    box-shadow: 0 0 8px var(--ok);
  }

  /* Navigation Tabs */
  .tabs {
    display: flex; background: var(--surface); padding: 4px; border-radius: 14px;
    border: 1px solid var(--line); margin-bottom: 18px; gap: 4px;
  }
  .tab-btn {
    flex: 1; padding: 10px 4px; font-size: 0.85rem; font-weight: 700; text-align: center;
    border-radius: 10px; border: none; background: transparent; color: var(--muted);
    cursor: pointer; transition: all 0.2s ease;
  }
  .tab-btn.active {
    background: var(--accent); color: #fff; box-shadow: 0 4px 12px var(--accent-glow);
  }

  .view-tab { display: none; }
  .view-tab.active { display: block; }

  /* ==================== REMOTE CONTROL STYLES ==================== */
  .remote-wrap {
    display: flex; flex-direction: column; align-items: center; gap: 18px;
    padding: 10px 0;
  }

  /* Quick action bar */
  .action-row {
    display: flex; justify-content: space-between; width: 100%; max-width: 380px; gap: 10px;
  }
  .btn-action {
    flex: 1; height: 50px; border-radius: 14px; border: 1px solid var(--line);
    background: var(--surface); color: var(--text); font-size: 1.1rem; font-weight: 600;
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    cursor: pointer; gap: 2px;
  }
  .btn-action span { font-size: 0.65rem; color: var(--muted); }
  .btn-action:active { transform: scale(0.92); background: var(--surface2); }

  /* Search & Keyboard bar */
  .remote-search {
    width: 100%; max-width: 380px; display: flex; gap: 8px;
  }
  .search-input {
    flex: 1; padding: 12px 14px; font-size: 15px; border-radius: 12px;
    background: var(--surface); color: var(--text); border: 1px solid var(--line);
  }
  .search-input:focus { outline: none; border-color: var(--accent); }
  .btn-send {
    padding: 12px 16px; border-radius: 12px; border: none; background: var(--accent);
    color: #fff; font-weight: 700; font-size: 14px; cursor: pointer;
  }
  .btn-send:active { transform: scale(0.94); }

  /* Circular D-PAD */
  .dpad {
    width: 250px; height: 250px; border-radius: 50%;
    background: radial-gradient(circle at 50% 50%, var(--surface2) 0%, var(--surface) 100%);
    border: 2px solid var(--line); position: relative;
    box-shadow: 0 16px 36px rgba(0, 0, 0, 0.6);
  }
  .dpad-btn {
    position: absolute; border: none; background: transparent; color: var(--text);
    font-size: 1.4rem; cursor: pointer; display: flex; align-items: center; justify-content: center;
    transition: background 0.15s ease;
  }
  .dpad-btn:active { color: var(--accent); text-shadow: 0 0 10px var(--accent-glow); }
  .dpad-up { top: 6px; left: 50%; transform: translateX(-50%); width: 90px; height: 60px; }
  .dpad-down { bottom: 6px; left: 50%; transform: translateX(-50%); width: 90px; height: 60px; }
  .dpad-left { left: 6px; top: 50%; transform: translateY(-50%); width: 60px; height: 90px; }
  .dpad-right { right: 6px; top: 50%; transform: translateY(-50%); width: 60px; height: 90px; }
  .dpad-ok {
    top: 50%; left: 50%; transform: translate(-50%, -50%);
    width: 86px; height: 86px; border-radius: 50%;
    background: linear-gradient(135deg, var(--surface3), var(--surface2));
    border: 1px solid var(--line); font-size: 1.15rem; font-weight: 800; color: #fff;
    box-shadow: 0 6px 14px rgba(0, 0, 0, 0.4);
  }
  .dpad-ok:active { transform: translate(-50%, -50%) scale(0.92); background: var(--accent); }

  /* Rocker switches (Vol & Ch) */
  .rocker-grid {
    display: flex; justify-content: space-between; width: 100%; max-width: 380px; gap: 24px;
  }
  .rocker-col {
    flex: 1; display: flex; flex-direction: column; background: var(--surface);
    border: 1px solid var(--line); border-radius: 18px; overflow: hidden;
  }
  .rocker-btn {
    flex: 1; height: 56px; border: none; background: transparent; color: var(--text);
    font-size: 1.2rem; font-weight: 800; cursor: pointer; display: flex;
    flex-direction: column; align-items: center; justify-content: center;
  }
  .rocker-btn:active { background: var(--surface2); color: var(--accent); }
  .rocker-label {
    font-size: 0.7rem; font-weight: 700; text-align: center; color: var(--muted);
    padding: 4px 0; background: rgba(0, 0, 0, 0.2); letter-spacing: 0.5px;
  }

  /* Media controls row */
  .media-row {
    display: flex; justify-content: space-around; width: 100%; max-width: 380px; gap: 12px;
  }
  .media-btn {
    width: 60px; height: 48px; border-radius: 14px; border: 1px solid var(--line);
    background: var(--surface); color: var(--text); font-size: 1.1rem; cursor: pointer;
    display: flex; align-items: center; justify-content: center;
  }
  .media-btn.play { flex: 1.2; background: var(--surface2); border-color: var(--accent); color: var(--accent); }
  .media-btn:active { transform: scale(0.92); }

  /* Number pad */
  .numpad-toggle {
    font-size: 0.85rem; color: var(--accent); background: none; border: none;
    font-weight: 700; cursor: pointer; text-decoration: underline; margin-top: 4px;
  }
  .numpad-grid {
    display: none; grid-template-columns: repeat(3, 1fr); gap: 10px; width: 100%; max-width: 320px;
  }
  .numpad-grid.open { display: grid; }
  .num-btn {
    height: 48px; border-radius: 12px; border: 1px solid var(--line);
    background: var(--surface); color: var(--text); font-size: 1.15rem; font-weight: 700;
    cursor: pointer; display: flex; align-items: center; justify-content: center;
  }
  .num-btn:active { background: var(--accent); color: #fff; }

  /* ==================== CHANNELS & CAST STYLES ==================== */
  .search { width: 100%; padding: 13px 14px; font-size: 16px; margin-bottom: 12px;
            background: var(--surface); color: var(--text); border: 1px solid var(--line); border-radius: 12px; }
  .search:focus { outline: none; border-color: var(--accent); }
  .chips { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
  .chip { padding: 8px 13px; font-size: 0.85rem; font-weight: 600; border-radius: 999px; cursor: pointer;
          background: var(--surface); border: 1px solid var(--line); color: var(--muted); }
  .chip.on { background: var(--accent); border-color: var(--accent); color: #fff; }
  .list { display: flex; flex-direction: column; gap: 8px; }
  .row { display: flex; align-items: center; gap: 10px; padding: 10px 12px;
         background: var(--surface); border: 1px solid var(--line); border-radius: 14px; }
  .row.hidden { opacity: 0.5; }
  .logo { width: 42px; height: 42px; border-radius: 8px; background: #000; object-fit: contain; flex: none; }
  .rowmeta { flex: 1; min-width: 0; }
  .name { font-weight: 700; font-size: 0.95rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
  .sub { color: var(--muted); font-size: 0.75rem; margin-top: 2px; }
  .btn-play-tv {
    padding: 8px 12px; border-radius: 9px; border: 1px solid var(--accent);
    background: rgba(59, 130, 246, 0.15); color: var(--accent); font-size: 0.8rem;
    font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 4px;
  }
  .btn-play-tv:active { background: var(--accent); color: #fff; transform: scale(0.94); }
  .ctrls { display: flex; align-items: center; gap: 4px; flex: none; }
  .icon-btn { width: 34px; height: 34px; border-radius: 8px; border: 1px solid var(--line);
              background: var(--surface2); color: var(--muted); font-size: 15px; cursor: pointer;
              display: flex; align-items: center; justify-content: center; }
  .icon-btn.star.on { color: var(--gold); border-color: var(--gold); }
  .icon-btn.eye.on { color: var(--accent); border-color: var(--accent); }

  /* ==================== ADD SOURCE / M3U FORM ==================== */
  .card-box {
    background: var(--surface); border: 1px solid var(--line); border-radius: 16px;
    padding: 18px; margin-bottom: 16px;
  }
  .card-title { font-size: 1.1rem; font-weight: 700; margin-bottom: 6px; }
  .card-hint { font-size: 0.85rem; color: var(--muted); margin-bottom: 16px; }
  .field { display: block; margin-bottom: 14px; }
  .field span { display: block; font-size: 0.8rem; font-weight: 600; color: var(--muted); margin-bottom: 6px; text-transform: uppercase; }
  .field input, .field select {
    width: 100%; padding: 12px 14px; font-size: 15px; border-radius: 10px;
    background: var(--bg); color: var(--text); border: 1px solid var(--line);
  }
  .field input:focus, .field select:focus { outline: none; border-color: var(--accent); }
  .field[hidden] { display: none; }
  .btn-submit {
    width: 100%; padding: 14px; border-radius: 12px; border: none; font-size: 15px;
    font-weight: 700; background: linear-gradient(135deg, #3B82F6, #2563EB); color: #fff;
    cursor: pointer; box-shadow: 0 4px 15px var(--accent-glow); margin-top: 6px;
  }
  .btn-submit:active { transform: scale(0.97); }

  /* Toast Notification */
  #status {
    position: fixed; left: 50%; bottom: 20px; transform: translateX(-50%) translateY(120%);
    max-width: 90%; padding: 12px 20px; border-radius: 12px; font-size: 0.9rem; font-weight: 600;
    background: var(--surface2); border: 1px solid var(--line); color: var(--text);
    transition: transform 0.25s ease; z-index: 100; box-shadow: 0 10px 30px rgba(0,0,0,0.6);
  }
  #status.show { transform: translateX(-50%) translateY(0); }
  #status.ok { border-color: var(--ok); color: var(--ok); }
  #status.err { border-color: var(--err); color: #ffbaba; }
</style>
</head>
<body>
  <div class="wrap">
    <header>
      <div class="brand">
        <span>GECE İPTV Player</span>
      </div>
      <div class="badge-live">TV Bağlı</div>
    </header>

    <!-- Navigation Tabs -->
    <div class="tabs">
      <button class="tab-btn active" onclick="switchTab('remote')">🎮 Kumanda</button>
      <button class="tab-btn" onclick="switchTab('channels')">📺 Kanallar</button>
      <button class="tab-btn" onclick="switchTab('source')">➕ IPTV Ekle</button>
      <button class="tab-btn" onclick="switchTab('rec')">💾 Kayıt</button>
    </div>

    <!-- TAB 1: VIRTUAL REMOTE -->
    <div id="tab-remote" class="view-tab active">
      <div class="remote-wrap">

        <!-- Quick actions -->
        <div class="action-row">
          <button class="btn-action" onclick="remote('mute')">🔇<span>MUTE</span></button>
          <button class="btn-action" onclick="remote('home')">🏠<span>ANA SAYFA</span></button>
          <button class="btn-action" onclick="remote('back')">↩️<span>GERİ</span></button>
        </div>

        <!-- TV Search Bar -->
        <div class="remote-search">
          <input type="text" id="tvSearchInput" class="search-input" placeholder="TV'de ara (Film, dizi, kanal)...">
          <button class="btn-send" onclick="sendTvSearch()">Gönder</button>
        </div>

        <!-- D-PAD Circular Controller -->
        <div class="dpad">
          <button class="dpad-btn dpad-up" onclick="remote('up')">▲</button>
          <button class="dpad-btn dpad-down" onclick="remote('down')">▼</button>
          <button class="dpad-btn dpad-left" onclick="remote('left')">◀</button>
          <button class="dpad-btn dpad-right" onclick="remote('right')">▶</button>
          <button class="dpad-btn dpad-ok" onclick="remote('ok')">OK</button>
        </div>

        <!-- Rockers (Volume & Channels) -->
        <div class="rocker-grid">
          <div class="rocker-col">
            <button class="rocker-btn" onclick="remote('vol_up')">＋</button>
            <div class="rocker-label">SES</div>
            <button class="rocker-btn" onclick="remote('vol_down')">－</button>
          </div>
          <div class="rocker-col">
            <button class="rocker-btn" onclick="remote('ch_up')">▲</button>
            <div class="rocker-label">KANAL</div>
            <button class="rocker-btn" onclick="remote('ch_down')">▼</button>
          </div>
        </div>

        <!-- Playback Media Controls -->
        <div class="media-row">
          <button class="media-btn play" onclick="remote('play_pause')">⏯ Oynat / Duraklat</button>
        </div>

        <!-- Number Pad Toggle -->
        <button class="numpad-toggle" onclick="toggleNumpad()">Sayısal Tuş Takımını Göster</button>
        <div id="numpad" class="numpad-grid">
          <button class="num-btn" onclick="remoteNum(1)">1</button>
          <button class="num-btn" onclick="remoteNum(2)">2</button>
          <button class="num-btn" onclick="remoteNum(3)">3</button>
          <button class="num-btn" onclick="remoteNum(4)">4</button>
          <button class="num-btn" onclick="remoteNum(5)">5</button>
          <button class="num-btn" onclick="remoteNum(6)">6</button>
          <button class="num-btn" onclick="remoteNum(7)">7</button>
          <button class="num-btn" onclick="remoteNum(8)">8</button>
          <button class="num-btn" onclick="remoteNum(9)">9</button>
          <div></div>
          <button class="num-btn" onclick="remoteNum(0)">0</button>
          <div></div>
        </div>

      </div>
    </div>

    <!-- TAB 2: CHANNELS & INSTANT PLAY -->
    <div id="tab-channels" class="view-tab">
      <input type="search" id="q" class="search" placeholder="Kanal adı ara..." autocomplete="off">
      <div id="chips" class="chips"></div>
      <div id="channels" class="list"></div>
    </div>

    <!-- TAB 3: ADD IPTV / M3U SOURCE -->
    <div id="tab-source" class="view-tab">
      <div class="card-box">
        <div class="card-title">Yeni IPTV / M3U Kaynağı Ekle</div>
        <div class="card-hint">Bağlantınızı veya hesap bilgilerinizi telefondan kolayca yapıştırıp TV'ye aktarın.</div>

        <label class="field">
          <span>Sağlayıcı Türü</span>
          <select id="provKind">
            <option value="xtream" selected>Xtream Codes Girişi</option>
            <option value="m3u">M3U Çalma Listesi URL</option>
            <option value="stalker">Stalker Portal</option>
          </select>
        </label>

        <label class="field">
          <span>Sağlayıcı Adı (İsteğe Bağlı)</span>
          <input id="provName" type="text" placeholder="Ör: IPTV Sağlayıcım">
        </label>

        <label class="field">
          <span id="provUrlLabel">Sunucu Adresi</span>
          <input id="provUrl" type="url" placeholder="http://sunucu.com:8080" autocomplete="off">
        </label>

        <label class="field" id="provUserField">
          <span>Kullanıcı Adı</span>
          <input id="provUser" type="text" autocomplete="off">
        </label>

        <label class="field" id="provPassField">
          <span>Şifre</span>
          <input id="provPass" type="password">
        </label>

        <label class="field" id="provMacField" hidden>
          <span>MAC Adresi</span>
          <input id="provMac" type="text" placeholder="00:1A:79:XX:XX:XX">
        </label>

        <button id="provSave" class="btn-submit">TV'ye Aktar ve Kanalları Yükle</button>
      </div>
    </div>

    <!-- TAB 4: RECORDING / SMB NAS -->
    <div id="tab-rec" class="view-tab">
      <div class="card-box">
        <div class="card-title">Kayıt Yeri (SMB / NAS)</div>
        <div class="card-hint">Yayınları ev ağınızdaki paylaşılan klasöre (NAS) kaydetmek için yapılandırın.</div>

        <label class="field">
          <span>Sunucu / IP</span>
          <input id="recHost" type="text" placeholder="192.168.1.100">
        </label>
        <label class="field">
          <span>Paylaşım Adı (Share)</span>
          <input id="recShare" type="text" placeholder="recordings">
        </label>
        <label class="field">
          <span>Klasör (İsteğe bağlı)</span>
          <input id="recFolder" type="text" placeholder="opentv">
        </label>
        <label class="field">
          <span>Kullanıcı Adı</span>
          <input id="recUser" type="text">
        </label>
        <label class="field">
          <span>Şifre</span>
          <input id="recPass" type="password">
        </label>

        <button id="recSave" class="btn-submit">Kayıt Ayarlarını Kaydet</button>
      </div>
    </div>

    <div id="status"></div>
  </div>

  <script>
    var token = "${token.replace("\"", "")}";
    var UNREACH = 'TV ile bağlantı kurulamadı. Aynı Wi-Fi ağına bağlı olduğunuzdan emin olun.';
    var channels = [];
    var activeCategory = null;

    function vibrate() {
      if (navigator.vibrate) navigator.vibrate(25);
    }

    function switchTab(name) {
      vibrate();
      document.querySelectorAll('.tab-btn').forEach(function(b){ b.classList.remove('active'); });
      document.querySelectorAll('.view-tab').forEach(function(v){ v.classList.remove('active'); });
      var target = document.getElementById('tab-' + name);
      if (target) target.classList.add('active');
      event.target.classList.add('active');
      if (name === 'channels' && channels.length === 0) loadMeta();
    }

    function toggleNumpad() {
      vibrate();
      var el = document.getElementById('numpad');
      el.classList.toggle('open');
    }

    function setStatus(msg, kind) {
      var s = document.getElementById('status');
      s.textContent = msg; s.className = 'show ' + (kind || '');
      clearTimeout(s._t);
      s._t = setTimeout(function(){ s.className = ''; }, 3000);
    }

    async function post(path, body) {
      var r = await fetch(path + '?t=' + encodeURIComponent(token), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    }

    async function getJson(path) {
      var r = await fetch(path + (path.indexOf('?') >= 0 ? '&' : '?') + 't=' + encodeURIComponent(token));
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    }

    // Remote Control API Call
    function remote(cmd) {
      vibrate();
      post('/remote', { cmd: cmd }).catch(function(){ setStatus(UNREACH, 'err'); });
    }

    function remoteNum(digit) {
      vibrate();
      post('/remote', { cmd: 'num', value: String(digit) }).catch(function(){ setStatus(UNREACH, 'err'); });
    }

    function sendTvSearch() {
      var text = document.getElementById('tvSearchInput').value.trim();
      if (!text) return;
      vibrate();
      post('/remote', { cmd: 'search', value: text })
        .then(function(){ setStatus('TV arama ekranına gönderildi ✓', 'ok'); })
        .catch(function(){ setStatus(UNREACH, 'err'); });
    }

    // Play Channel on TV
    function playOnTv(channelId) {
      vibrate();
      setStatus('TV\\'de oynatılıyor... ▶', 'ok');
      post('/play', { channelId: channelId })
        .then(function(){ setStatus('Kanal TV\\'de açıldı ✓', 'ok'); })
        .catch(function(){ setStatus(UNREACH, 'err'); });
    }

    // Channels tab logic
    async function loadMeta() {
      try {
        var meta = await getJson('/meta');
        renderCategories(meta.categories || []);
        loadChannels();
      } catch (e) {
        setStatus(UNREACH, 'err');
      }
    }

    function renderCategories(cats) {
      var chipsEl = document.getElementById('chips');
      chipsEl.innerHTML = '';
      var allChip = document.createElement('div');
      allChip.className = 'chip ' + (activeCategory === null ? 'on' : '');
      allChip.textContent = 'Tümü';
      allChip.onclick = function(){ activeCategory = null; loadChannels(); };
      chipsEl.appendChild(allChip);

      cats.forEach(function(c){
        var chip = document.createElement('div');
        chip.className = 'chip ' + (activeCategory === c.id ? 'on' : '');
        chip.textContent = c.label + ' (' + c.count + ')';
        chip.onclick = function(){ activeCategory = c.id; loadChannels(); };
        chipsEl.appendChild(chip);
      });
    }

    async function loadChannels() {
      try {
        var query = activeCategory ? '?cat=' + encodeURIComponent(activeCategory) : '';
        channels = await getJson('/channels' + query);
        renderChannels();
      } catch (e) {
        setStatus(UNREACH, 'err');
      }
    }

    function renderChannels() {
      var container = document.getElementById('channels');
      container.innerHTML = '';
      var term = document.getElementById('q').value.toLowerCase().trim();

      var filtered = channels.filter(function(c){
        return !term || c.name.toLowerCase().indexOf(term) >= 0;
      });

      if (filtered.length === 0) {
        container.innerHTML = '<div style="text-align:center;padding:30px;color:var(--muted)">Kanal bulunamadı.</div>';
        return;
      }

      filtered.forEach(function(c){
        var row = document.createElement('div');
        row.className = 'row ' + (c.hidden ? 'hidden' : '');

        var logo = document.createElement('img');
        logo.className = 'logo';
        logo.src = c.logo || '';
        logo.onerror = function(){ this.style.display = 'none'; };
        row.appendChild(logo);

        var meta = document.createElement('div');
        meta.className = 'rowmeta';
        var nameSpan = document.createElement('span');
        nameSpan.className = 'name';
        nameSpan.textContent = c.name;
        meta.appendChild(nameSpan);
        row.appendChild(meta);

        // Play on TV Button
        var playBtn = document.createElement('button');
        playBtn.className = 'btn-play-tv';
        playBtn.innerHTML = '▶ TV';
        playBtn.onclick = function(){ playOnTv(c.id); };
        row.appendChild(playBtn);

        var ctrls = document.createElement('div');
        ctrls.className = 'ctrls';

        // Favorite Button
        var favBtn = document.createElement('button');
        favBtn.className = 'icon-btn star ' + (c.favourite ? 'on' : '');
        favBtn.innerHTML = '★';
        favBtn.onclick = function(){ toggleFav(c); };
        ctrls.appendChild(favBtn);

        // Hide/Show Button
        var eyeBtn = document.createElement('button');
        eyeBtn.className = 'icon-btn eye ' + (!c.hidden ? 'on' : '');
        eyeBtn.innerHTML = c.hidden ? '🚫' : '👁';
        eyeBtn.onclick = function(){ toggleHide(c); };
        ctrls.appendChild(eyeBtn);

        row.appendChild(ctrls);
        container.appendChild(row);
      });
    }

    function toggleFav(c) {
      vibrate();
      c.favourite = !c.favourite;
      renderChannels();
      post('/channel', { id: c.id, favourite: c.favourite }).catch(function(){ setStatus(UNREACH, 'err'); });
    }

    function toggleHide(c) {
      vibrate();
      c.hidden = !c.hidden;
      renderChannels();
      post('/channel', { id: c.id, hidden: c.hidden }).catch(function(){ setStatus(UNREACH, 'err'); });
    }

    document.getElementById('q').addEventListener('input', renderChannels);

    // Source Provider Form
    function syncProvFields() {
      var k = document.getElementById('provKind').value;
      document.getElementById('provUserField').hidden = (k !== 'xtream');
      document.getElementById('provPassField').hidden = (k !== 'xtream');
      document.getElementById('provMacField').hidden = (k !== 'stalker');
      document.getElementById('provUrlLabel').textContent = (k === 'm3u') ? 'M3U Çalma Listesi URL' : (k === 'stalker' ? 'Portal URL' : 'Sunucu Adresi');
      document.getElementById('provUrl').placeholder = (k === 'm3u') ? 'http://sunucu.com/playlist.m3u' : (k === 'stalker' ? 'http://portal.com:80/c' : 'http://sunucu.com:8080');
    }
    document.getElementById('provKind').addEventListener('change', syncProvFields);

    document.getElementById('provSave').onclick = function() {
      var kind = document.getElementById('provKind').value;
      var name = document.getElementById('provName').value.trim();
      var url = document.getElementById('provUrl').value.trim();
      var user = document.getElementById('provUser').value.trim();
      var pass = document.getElementById('provPass').value;
      var mac = document.getElementById('provMac').value.trim();

      if (!url) { setStatus('Lütfen URL adresini girin.', 'err'); return; }
      if (kind === 'xtream' && (!user || !pass)) { setStatus('Kullanıcı adı ve şifre gereklidir.', 'err'); return; }
      if (kind === 'stalker' && !mac) { setStatus('MAC adresi gereklidir.', 'err'); return; }

      setStatus('TV\\'ye aktarılıyor ve kanallar yükleniyor…', 'ok');
      post('/source', { kind: kind, name: name, url: url, username: user, password: pass, mac: mac })
        .then(function(r){
          if (r && r.ok) {
            setStatus('Başarılı! ' + (r.channels || 0) + ' kanal yüklendi. ✓', 'ok');
            document.getElementById('provUrl').value = '';
            document.getElementById('provUser').value = '';
            document.getElementById('provPass').value = '';
            loadMeta();
          } else {
            setStatus(r.error || 'Yükleme başarısız', 'err');
          }
        }).catch(function(){ setStatus(UNREACH, 'err'); });
    };

    // Recording form
    document.getElementById('recSave').onclick = function() {
      var host = document.getElementById('recHost').value.trim();
      var share = document.getElementById('recShare').value.trim();
      if (!host || !share) { setStatus('Sunucu ve paylaşım adı gereklidir.', 'err'); return; }
      post('/recording', {
        host: host, share: share,
        folder: document.getElementById('recFolder').value.trim(),
        user: document.getElementById('recUser').value.trim(),
        password: document.getElementById('recPass').value
      }).then(function(){ setStatus('Kayıt ayarları güncellendi ✓', 'ok'); })
        .catch(function(){ setStatus(UNREACH, 'err'); });
    };
  </script>
</body>
</html>
""".trimIndent()
}
