(function () {
  const shell = document.getElementById('app-shell');
  const toastEl = document.getElementById('toast');
  const footerVersion = document.getElementById('footer-version');
  const footerAccount = document.getElementById('footer-account');
  const accountName = document.getElementById('account-name');
  const accountType = document.getElementById('account-type');
  const accountAvatar = document.getElementById('account-avatar');
  const statsPreview = document.getElementById('stats-preview');
  const btnPlay = document.getElementById('btn-play');
  const launchProgress = document.getElementById('launch-progress');
  const progressFill = document.getElementById('progress-fill');
  const progressLabel = document.getElementById('progress-label');
  const launchError = document.getElementById('launch-error');
  const ramSlider = document.getElementById('ram-slider');
  const ramValue = document.getElementById('ram-value');
  const javaPath = document.getElementById('java-path');
  const gameDir = document.getElementById('game-dir');
  const themeSelect = document.getElementById('theme-select');
  const apiEndpoint = document.getElementById('api-endpoint');
  const discordInviteInput = document.getElementById('discord-invite');
  const serverIpInput = document.getElementById('server-ip');
  const javaResolvedHint = document.getElementById('java-resolved-hint');
  const btnSettingsMicrosoft = document.getElementById('btn-settings-microsoft');
  const readyJava = document.getElementById('ready-java');
  const readyForge = document.getElementById('ready-forge');
  const readyClient = document.getElementById('ready-client');
  const btnInstallForge = document.getElementById('btn-install-forge');
  const btnRefreshReady = document.getElementById('btn-refresh-ready');
  const apiStatusChip = document.getElementById('api-status-chip');

  let currentSession = null;
  let settings = null;
  let launching = false;
  let installingForge = false;
  let skinsState = { skins: [], activeId: null, model: 'classic' };
  let skinBusy = false;

  function showToast(message, ms) {
    ms = ms || 2000;
    toastEl.textContent = message;
    toastEl.classList.remove('hidden');
    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => {
      toastEl.classList.add('hidden');
    }, ms);
  }

  function setView(name) {
    document.querySelectorAll('.nav-item').forEach((el) => {
      el.classList.toggle('active', el.dataset.view === name);
    });
    document.getElementById('view-home').classList.toggle('hidden', name !== 'home');
    const skinsView = document.getElementById('view-skins');
    if (skinsView) skinsView.classList.toggle('hidden', name !== 'skins');
    document.getElementById('view-settings').classList.toggle('hidden', name !== 'settings');
    if (name === 'skins') {
      refreshSkinsUi();
    }
  }

  function statsPlaceholders(footerNote) {
    return (
      '<div class="stats-figures">' +
      '<div><div class="muted">ELO</div><div class="stat-value">—</div></div>' +
      '<div><div class="muted">Rank</div><div class="stat-value">—</div></div>' +
      '<div><div class="muted">Wins</div><div class="stat-value">—</div></div>' +
      '</div>' +
      (footerNote ? '<p class="muted stats-footnote">' + escapeHtml(footerNote) + '</p>' : '')
    );
  }

  function renderGuestStats() {
    statsPreview.innerHTML =
      '<p class="stats-pitch">Sign in with Microsoft to sync ranked ELO, rank, and wins.</p>' +
      '<button id="btn-stats-microsoft" class="btn btn-primary compact" type="button">Continue with Microsoft</button>';
    const btn = document.getElementById('btn-stats-microsoft');
    if (btn) {
      btn.addEventListener('click', startMicrosoftFromApp);
    }
  }

  function renderMsStatsLoading() {
    statsPreview.innerHTML =
      '<p class="muted">Loading ranked stats…</p>' + statsPlaceholders('');
  }

  function renderMsStatsLive(data) {
    const elo = data.elo != null ? String(data.elo) : '—';
    const rank = data.rank != null ? String(data.rank) : '—';
    const wins = data.wins != null ? String(data.wins) : '—';
    statsPreview.innerHTML =
      '<div class="stats-figures">' +
      '<div><div class="muted">ELO</div><div class="stat-value">' +
      escapeHtml(elo) +
      '</div></div>' +
      '<div><div class="muted">Rank</div><div class="stat-value">' +
      escapeHtml(rank) +
      '</div></div>' +
      '<div><div class="muted">Wins</div><div class="stat-value">' +
      escapeHtml(wins) +
      '</div></div>' +
      '</div>';
  }

  async function refreshRankedStats() {
    if (!currentSession || currentSession.guest) {
      renderGuestStats();
      return;
    }
    renderMsStatsLoading();
    try {
      const result = await window.onyx.fetchStats();
      if (result && result.ok) {
        renderMsStatsLive(result);
      } else {
        statsPreview.innerHTML = statsPlaceholders('Ranked stats syncing soon');
      }
    } catch (_) {
      statsPreview.innerHTML = statsPlaceholders('Ranked stats syncing soon');
    }
  }

  async function startMicrosoftFromApp() {
    showToast('Opening Microsoft sign-in…');
    const result = await window.onyx.startMicrosoft();
    if (result.ok && result.session) {
      await enterApp(result.session, false);
    } else if (result.error) {
      showToast(result.error, 3500);
    }
  }

  function applySession(session) {
    currentSession = session;
    if (!session) {
      shell.classList.add('hidden');
      footerAccount.textContent = 'Not signed in';
      btnSettingsMicrosoft.classList.add('hidden');
      return;
    }

    shell.classList.remove('hidden');
    footerAccount.textContent = session.username;
    accountName.textContent = session.username;
    accountType.textContent = session.guest ? 'Guest account' : 'Microsoft account';
    btnSettingsMicrosoft.classList.toggle('hidden', !session.guest);

    if (session.avatarUrl) {
      accountAvatar.src = session.avatarUrl;
      accountAvatar.classList.remove('hidden');
    } else {
      accountAvatar.classList.add('hidden');
    }

    refreshRankedStats();
  }

  function updateJavaHint(cfg) {
    if (!javaResolvedHint) return;
    const resolved = cfg.resolvedJavaPath || '';
    const custom = (cfg.javaPath || '').trim();
    const hasBundled = !!cfg.hasBundledJre;
    const exists = cfg.javaResolvedExists !== false;

    if (custom) {
      javaResolvedHint.textContent = exists
        ? 'Using: ' + resolved
        : 'Path not found — fix with Browse or clear for auto-detect';
      javaResolvedHint.classList.toggle('warn', !exists);
      return;
    }

    if (!exists) {
      javaResolvedHint.textContent =
        'Java 8 not found — Browse to a JDK 8 binary or install Java 8';
      javaResolvedHint.classList.add('warn');
      return;
    }

    let note = 'Auto-detect → ' + resolved;
    if (hasBundled) {
      note += ' (bundled JRE available)';
    }
    javaResolvedHint.textContent = note;
    javaResolvedHint.classList.remove('warn');
  }

  function fillSettings(cfg) {
    settings = cfg;
    const maxRam = cfg.maxRamGb || 8;
    ramSlider.max = String(maxRam);
    ramSlider.min = '2';
    ramSlider.value = String(Math.min(Math.max(cfg.ramGb || 4, 2), maxRam));
    ramValue.textContent = ramSlider.value + ' GB';
    javaPath.value = cfg.javaPath || '';
    javaPath.placeholder = cfg.hasBundledJre
      ? 'Leave empty to auto-detect (bundled JRE available)'
      : 'Leave empty to auto-detect';
    gameDir.value = cfg.gameDir || '';
    if (apiEndpoint) {
      apiEndpoint.value = cfg.onyxApiEndpoint || 'https://api.onyxrbw.com';
    }
    if (discordInviteInput) {
      discordInviteInput.value = cfg.discordInvite || 'https://discord.gg/onyxrbw';
    }
    if (serverIpInput) {
      serverIpInput.value = cfg.serverIp || 'eu.onyxrbw.com';
    }
    if (themeSelect) {
      themeSelect.value = cfg.theme || 'onyx-dark';
    }
    footerVersion.textContent = 'v' + (cfg.version || '1.0.2');
    updateJavaHint(cfg);
  }

  function setReadyChip(el, label, ok) {
    if (!el) return;
    el.textContent = label + (ok ? ' OK' : ' missing');
    el.classList.toggle('ok', !!ok);
    el.classList.toggle('bad', !ok);
  }

  async function refreshReadiness() {
    try {
      const r = await window.onyx.getReadiness();
      setReadyChip(readyJava, 'Java', r.javaOk);
      setReadyChip(readyForge, 'Forge', r.forgeOk);
      setReadyChip(readyClient, 'Client', r.clientOk);
      if (btnInstallForge) {
        btnInstallForge.classList.toggle('hidden', !!r.forgeOk);
      }
      return r;
    } catch (_) {
      setReadyChip(readyJava, 'Java', false);
      setReadyChip(readyForge, 'Forge', false);
      setReadyChip(readyClient, 'Client', false);
      return null;
    }
  }

  async function refreshApiStatus() {
    if (!apiStatusChip) return;
    const base = ((settings && settings.onyxApiEndpoint) || 'https://api.onyxrbw.com').replace(
      /\/$/,
      ''
    );
    apiStatusChip.textContent = 'API…';
    apiStatusChip.classList.remove('ok', 'bad');
    try {
      const ctrl = new AbortController();
      const t = setTimeout(() => ctrl.abort(), 5000);
      const res = await fetch(base + '/launcher/version', {
        signal: ctrl.signal,
        headers: { Accept: 'application/json' }
      });
      clearTimeout(t);
      if (res.ok) {
        apiStatusChip.textContent = 'API online';
        apiStatusChip.classList.add('ok');
      } else {
        apiStatusChip.textContent = 'API ' + res.status;
        apiStatusChip.classList.add('bad');
      }
    } catch (_) {
      apiStatusChip.textContent = 'API offline';
      apiStatusChip.classList.add('bad');
    }
  }

  function drawSkinPreview(canvas, img, scale) {
    if (!canvas || !img) return;
    const ctx = canvas.getContext('2d');
    const s = scale || Math.floor(canvas.width / 8);
    ctx.imageSmoothingEnabled = false;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#0a0612';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    // Front body (64×64 UV): head 8x8 at 8,8; body 8x12 at 20,20; arms/legs
    const ox = Math.floor((canvas.width - 16 * s) / 2);
    const oy = Math.floor((canvas.height - 32 * s) / 2);
    // head
    ctx.drawImage(img, 8, 8, 8, 8, ox + 4 * s, oy, 8 * s, 8 * s);
    // hat
    ctx.drawImage(img, 40, 8, 8, 8, ox + 4 * s, oy, 8 * s, 8 * s);
    // body
    ctx.drawImage(img, 20, 20, 8, 12, ox + 4 * s, oy + 8 * s, 8 * s, 12 * s);
    // right arm / left arm (classic)
    ctx.drawImage(img, 44, 20, 4, 12, ox, oy + 8 * s, 4 * s, 12 * s);
    ctx.drawImage(img, 36, 52, 4, 12, ox + 12 * s, oy + 8 * s, 4 * s, 12 * s);
    // right leg / left leg
    ctx.drawImage(img, 4, 20, 4, 12, ox + 4 * s, oy + 20 * s, 4 * s, 12 * s);
    ctx.drawImage(img, 20, 52, 4, 12, ox + 8 * s, oy + 20 * s, 4 * s, 12 * s);
  }

  function drawSkinHead(canvas, img) {
    if (!canvas || !img) return;
    const ctx = canvas.getContext('2d');
    ctx.imageSmoothingEnabled = false;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.drawImage(img, 8, 8, 8, 8, 0, 0, canvas.width, canvas.height);
    ctx.drawImage(img, 40, 8, 8, 8, 0, 0, canvas.width, canvas.height);
  }

  function loadImage(dataUrl) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = () => reject(new Error('Failed to load skin image'));
      img.src = dataUrl;
    });
  }

  function setSkinStatus(msg, isError) {
    const el = document.getElementById('skin-status');
    if (!el) return;
    el.textContent = msg || '';
    el.classList.toggle('warn', !!isError);
  }

  function updateSkinMsGate() {
    const applyBtn = document.getElementById('btn-skin-apply');
    const resetBtn = document.getElementById('btn-skin-reset');
    const isMs = currentSession && !currentSession.guest && currentSession.type !== 'guest';
    if (applyBtn) applyBtn.disabled = !isMs || skinBusy || !skinsState.activeId;
    if (resetBtn) resetBtn.disabled = !isMs || skinBusy;
    if (!isMs) {
      setSkinStatus('Sign in with Microsoft to apply skins to your account. Upload and copy still work.');
    }
  }

  async function refreshSkinsUi() {
    const grid = document.getElementById('skins-grid');
    const empty = document.getElementById('skins-empty');
    const nameEl = document.getElementById('skin-selected-name');
    const preview = document.getElementById('skin-preview');
    if (!grid) return;

    const list = await window.onyx.skinList();
    if (!list.ok) {
      setSkinStatus(list.error || 'Could not load skins', true);
      return;
    }
    skinsState = {
      skins: list.skins || [],
      activeId: list.activeId,
      model: list.model === 'slim' ? 'slim' : 'classic'
    };

    const btnClassic = document.getElementById('btn-model-classic');
    const btnSlim = document.getElementById('btn-model-slim');
    if (btnClassic) btnClassic.classList.toggle('active', skinsState.model === 'classic');
    if (btnSlim) btnSlim.classList.toggle('active', skinsState.model === 'slim');

    grid.innerHTML = '';
    if (!skinsState.skins.length) {
      if (empty) empty.classList.remove('hidden');
      if (nameEl) nameEl.textContent = 'No skin selected';
      if (preview) {
        const ctx = preview.getContext('2d');
        ctx.fillStyle = '#0a0612';
        ctx.fillRect(0, 0, preview.width, preview.height);
      }
      updateSkinMsGate();
      return;
    }
    if (empty) empty.classList.add('hidden');

    for (const skin of skinsState.skins) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'skin-tile' + (skin.id === skinsState.activeId ? ' active' : '');
      btn.dataset.id = skin.id;
      const c = document.createElement('canvas');
      c.width = 48;
      c.height = 48;
      const label = document.createElement('span');
      label.className = 'skin-tile-name';
      label.textContent = skin.name;
      btn.appendChild(c);
      btn.appendChild(label);
      btn.addEventListener('click', () => selectSkin(skin.id));
      grid.appendChild(btn);

      window.onyx.skinGetData(skin.id).then(async (data) => {
        if (!data.ok) return;
        try {
          const img = await loadImage(data.dataUrl);
          drawSkinHead(c, img);
        } catch (_) {
          /* ignore */
        }
      });
    }

    if (skinsState.activeId) {
      await renderSelectedSkin(skinsState.activeId);
    } else if (nameEl) {
      nameEl.textContent = 'No skin selected';
    }
    updateSkinMsGate();
  }

  async function selectSkin(id) {
    const result = await window.onyx.skinSetActive(id);
    if (!result.ok) {
      setSkinStatus(result.error || 'Could not select skin', true);
      return;
    }
    skinsState.activeId = id;
    await refreshSkinsUi();
  }

  async function renderSelectedSkin(id) {
    const nameEl = document.getElementById('skin-selected-name');
    const preview = document.getElementById('skin-preview');
    const skin = skinsState.skins.find((s) => s.id === id);
    if (nameEl) nameEl.textContent = skin ? skin.name : 'No skin selected';
    if (!preview || !id) return;
    const data = await window.onyx.skinGetData(id);
    if (!data.ok) {
      setSkinStatus(data.error || 'Preview failed', true);
      return;
    }
    try {
      const img = await loadImage(data.dataUrl);
      drawSkinPreview(preview, img, 6);
      if (currentSession && !currentSession.guest) setSkinStatus('');
    } catch (err) {
      setSkinStatus(err.message || 'Preview failed', true);
    }
  }

  async function withSkinBusy(fn) {
    if (skinBusy) return;
    skinBusy = true;
    updateSkinMsGate();
    try {
      await fn();
    } finally {
      skinBusy = false;
      updateSkinMsGate();
    }
  }

  function wireSkinsUi() {
    const btnImport = document.getElementById('btn-skin-import');
    const btnCopy = document.getElementById('btn-skin-copy');
    const btnApply = document.getElementById('btn-skin-apply');
    const btnReset = document.getElementById('btn-skin-reset');
    const btnDelete = document.getElementById('btn-skin-delete');
    const btnClassic = document.getElementById('btn-model-classic');
    const btnSlim = document.getElementById('btn-model-slim');
    const ignInput = document.getElementById('skin-copy-ign');

    if (btnImport) {
      btnImport.addEventListener('click', () =>
        withSkinBusy(async () => {
          const result = await window.onyx.skinImport();
          if (result.cancelled) return;
          if (!result.ok) {
            setSkinStatus(result.error || 'Import failed', true);
            showToast(result.error || 'Import failed', 3000);
            return;
          }
          showToast('Skin imported');
          await refreshSkinsUi();
        })
      );
    }

    if (btnCopy) {
      btnCopy.addEventListener('click', () =>
        withSkinBusy(async () => {
          const ign = ignInput ? ignInput.value.trim() : '';
          setSkinStatus('Copying skin…');
          const result = await window.onyx.skinCopyUsername(ign);
          if (!result.ok) {
            setSkinStatus(result.error || 'Copy failed', true);
            showToast(result.error || 'Copy failed', 3000);
            return;
          }
          showToast('Copied skin from ' + (result.entry && result.entry.name ? result.entry.name : ign));
          if (ignInput) ignInput.value = '';
          await refreshSkinsUi();
        })
      );
    }

    if (ignInput) {
      ignInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && btnCopy) btnCopy.click();
      });
    }

    if (btnApply) {
      btnApply.addEventListener('click', () =>
        withSkinBusy(async () => {
          setSkinStatus('Applying to Minecraft account…');
          const result = await window.onyx.skinApply();
          if (!result.ok) {
            setSkinStatus(result.error || 'Apply failed', true);
            showToast(result.error || 'Apply failed', 3500);
            return;
          }
          setSkinStatus('Applied — others will see it after skin caches refresh.');
          showToast('Skin applied to account');
        })
      );
    }

    if (btnReset) {
      btnReset.addEventListener('click', () =>
        withSkinBusy(async () => {
          setSkinStatus('Resetting account skin…');
          const result = await window.onyx.skinReset();
          if (!result.ok) {
            setSkinStatus(result.error || 'Reset failed', true);
            showToast(result.error || 'Reset failed', 3500);
            return;
          }
          showToast('Account skin reset');
          await refreshSkinsUi();
        })
      );
    }

    if (btnDelete) {
      btnDelete.addEventListener('click', () =>
        withSkinBusy(async () => {
          if (!skinsState.activeId) {
            setSkinStatus('Select a skin to delete', true);
            return;
          }
          const result = await window.onyx.skinDelete(skinsState.activeId);
          if (!result.ok) {
            setSkinStatus(result.error || 'Delete failed', true);
            return;
          }
          showToast('Skin deleted');
          await refreshSkinsUi();
        })
      );
    }

    async function setModel(model) {
      const result = await window.onyx.skinSetModel(model);
      if (result.ok) {
        skinsState.model = result.model;
        if (btnClassic) btnClassic.classList.toggle('active', result.model === 'classic');
        if (btnSlim) btnSlim.classList.toggle('active', result.model === 'slim');
      }
    }
    if (btnClassic) btnClassic.addEventListener('click', () => setModel('classic'));
    if (btnSlim) btnSlim.addEventListener('click', () => setModel('slim'));
  }

  async function enterApp(session, welcomeBack) {
    applySession(session);
    window.OnyxAuthModal.hide();
    const cfg = await window.onyx.getSettings();
    fillSettings(cfg);
    setView('home');
    refreshReadiness();
    refreshApiStatus();
    updateSkinMsGate();
    if (welcomeBack) {
      showToast('Welcome back, ' + session.username);
    }
    window.onyx
      .checkUpdates()
      .then((info) => {
        if (!info) return;
        if (info.ok && info.launcherUpdate) {
          const ver = info.launcherUpdate.version || info.launcherUpdate;
          if (info.launcherUpdate.url) {
            showToast('Launcher v' + ver + ' ready — updating…', 4000);
            window.onyx.installLauncherUpdate().then((res) => {
              if (!res || !res.ok) {
                showToast(
                  (res && res.error) || 'Could not install launcher update',
                  5000
                );
              } else {
                showToast('Restarting to finish update…', 3000);
              }
            });
          } else {
            showToast(
              'Launcher update available (v' + ver + ') — publish launcherUrl on the API to auto-install',
              5000
            );
          }
        }
        if (info.ok && info.applied && info.applied.applied && info.applied.applied.length) {
          showToast('Updated client components', 3000);
          if (info.changelog) {
            const list = document.getElementById('news-list');
            const li = document.createElement('li');
            li.innerHTML =
              '<span class="news-tag">Update</span> ' + escapeHtml(info.changelog);
            list.prepend(li);
          }
        }
      })
      .catch(() => {});
  }

  function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }

  function formatLaunchError(raw) {
    const msg = String(raw || 'Launch failed');
    const lower = msg.toLowerCase();
    let headline = null;

    if (
      lower.includes('java 8 not found') ||
      lower.includes('failed to start java') ||
      lower.includes('java process error') ||
      (lower.includes('java') &&
        (lower.includes('enoent') || lower.includes('not found') || lower.includes('spawn')))
    ) {
      headline =
        'Java 8 not found — set Java Path in Settings (Browse) or install JDK 8';
    } else if (lower.includes('forge')) {
      headline =
        'Forge 1.8.9 not found in your game folder — install Forge or set Game Directory in Settings';
    } else if (lower.includes('onyxclient jar') || lower.includes('onyx client jar')) {
      headline =
        'OnyxClient jar missing — build the client or place the jar in mods';
    }

    if (!headline) {
      return msg;
    }
    if (msg.length > 20 && msg !== headline) {
      return headline + '\n\n' + msg;
    }
    return headline;
  }

  async function boot() {
    const version = await window.onyx.getVersion();
    footerVersion.textContent = 'v' + version;

    const refresh = await window.onyx.tryRefresh();
    if (refresh.ok && refresh.session) {
      await enterApp(refresh.session, !!refresh.welcomeBack);
      return;
    }

    if (refresh.sessionExpired) {
      window.OnyxAuthModal.show({ sessionExpired: true });
    } else {
      window.OnyxAuthModal.show();
    }
  }

  document.querySelectorAll('.nav-item').forEach((btn) => {
    btn.addEventListener('click', () => setView(btn.dataset.view));
  });

  wireSkinsUi();

  ramSlider.addEventListener('input', () => {
    ramValue.textContent = ramSlider.value + ' GB';
  });

  document.getElementById('btn-browse-java').addEventListener('click', async () => {
    const result = await window.onyx.pickJava();
    if (result.ok && result.path) {
      javaPath.value = result.path;
    }
  });

  document.getElementById('btn-browse-game-dir').addEventListener('click', async () => {
    const result = await window.onyx.pickGameDir();
    if (result.ok && result.path) {
      gameDir.value = result.path;
    }
  });

  document.getElementById('btn-save-settings').addEventListener('click', async () => {
    const result = await window.onyx.setSettings({
      ramGb: Number(ramSlider.value),
      javaPath: javaPath.value.trim(),
      gameDir: gameDir.value.trim(),
      onyxApiEndpoint: apiEndpoint ? apiEndpoint.value.trim() : undefined,
      discordInvite: discordInviteInput ? discordInviteInput.value.trim() : undefined,
      serverIp: serverIpInput ? serverIpInput.value.trim() : undefined,
      theme: themeSelect ? themeSelect.value : 'onyx-dark'
    });
    if (result.ok) {
      const refreshed = await window.onyx.getSettings();
      fillSettings(refreshed);
      refreshReadiness();
      refreshApiStatus();
      showToast('Settings saved');
    }
  });

  const btnCopyServerIp = document.getElementById('btn-copy-server-ip');
  if (btnCopyServerIp && serverIpInput) {
    btnCopyServerIp.addEventListener('click', async () => {
      const ip = serverIpInput.value.trim() || 'eu.onyxrbw.com';
      try {
        await navigator.clipboard.writeText(ip);
        showToast('Copied ' + ip);
      } catch (_) {
        showToast('Could not copy — select and copy manually', 3000);
      }
    });
  }

  const btnTestApi = document.getElementById('btn-test-api');
  if (btnTestApi) {
    btnTestApi.addEventListener('click', async () => {
      const base = (
        (apiEndpoint && apiEndpoint.value.trim()) ||
        (settings && settings.onyxApiEndpoint) ||
        'https://api.onyxrbw.com'
      ).replace(/\/$/, '');
      btnTestApi.disabled = true;
      try {
        const ctrl = new AbortController();
        const t = setTimeout(() => ctrl.abort(), 5000);
        const res = await fetch(base + '/rbw/api', {
          signal: ctrl.signal,
          headers: { Accept: 'application/json' }
        });
        clearTimeout(t);
        // Any HTTP response means the host is reachable (401 without AUTH_KEY is fine)
        showToast('API reachable (' + res.status + ')', 3000);
        if (apiStatusChip) {
          apiStatusChip.textContent = res.ok ? 'API online' : 'API ' + res.status;
          apiStatusChip.classList.toggle('ok', res.ok || res.status === 401);
          apiStatusChip.classList.toggle('bad', !(res.ok || res.status === 401));
        }
      } catch (_) {
        showToast('API offline — check URL or start the bot', 3500);
        if (apiStatusChip) {
          apiStatusChip.textContent = 'API offline';
          apiStatusChip.classList.remove('ok');
          apiStatusChip.classList.add('bad');
        }
      } finally {
        btnTestApi.disabled = false;
      }
    });
  }
  btnSettingsMicrosoft.addEventListener('click', startMicrosoftFromApp);

  document.getElementById('btn-sign-out').addEventListener('click', async () => {
    await window.onyx.signOut();
    applySession(null);
    window.OnyxAuthModal.show();
  });

  function openDiscord() {
    const url = (settings && settings.discordInvite) || 'https://discord.gg/onyxrbw';
    window.onyx.openExternal(url);
  }

  document.getElementById('btn-discord').addEventListener('click', openDiscord);
  document.getElementById('btn-discord-home').addEventListener('click', openDiscord);
  document.getElementById('btn-discord-footer').addEventListener('click', openDiscord);
  const seasonDiscord = document.getElementById('btn-discord-season');
  if (seasonDiscord) {
    seasonDiscord.addEventListener('click', openDiscord);
  }

  if (btnRefreshReady) {
    btnRefreshReady.addEventListener('click', async () => {
      await refreshReadiness();
      showToast('Readiness refreshed');
    });
  }

  if (btnInstallForge) {
    btnInstallForge.addEventListener('click', async () => {
      if (installingForge || launching) return;
      installingForge = true;
      btnInstallForge.disabled = true;
      launchError.classList.add('hidden');
      launchProgress.classList.remove('hidden');
      progressFill.style.width = '0%';
      progressLabel.textContent = 'Installing Forge…';
      try {
        const result = await window.onyx.installForge();
        await refreshReadiness();
        if (result.ok) {
          progressLabel.textContent = result.alreadyInstalled
            ? 'Forge already installed'
            : 'Forge installed';
          showToast('Forge 1.8.9 ready');
        } else {
          launchError.textContent = formatLaunchError(
            result.error || 'Forge install did not complete'
          );
          launchError.classList.remove('hidden');
          progressLabel.textContent = result.needsManual
            ? 'Finish install in the Forge window'
            : 'Forge install failed';
          if (result.needsManual) {
            showToast('Complete Forge install, then Refresh', 4000);
          }
        }
      } catch (err) {
        launchError.textContent = formatLaunchError(err.message || 'Forge install failed');
        launchError.classList.remove('hidden');
      } finally {
        installingForge = false;
        btnInstallForge.disabled = false;
      }
    });
  }

  window.onyx.onLaunchProgress((data) => {
    launchProgress.classList.remove('hidden');
    progressFill.style.width = (data.percent || 0) + '%';
    progressLabel.textContent = data.message || 'Working…';
  });

  btnPlay.addEventListener('click', async () => {
    if (launching) return;
    launching = true;
    btnPlay.disabled = true;
    launchError.classList.add('hidden');
    launchProgress.classList.remove('hidden');
    progressFill.style.width = '0%';
    progressLabel.textContent = 'Starting…';

    try {
      const result = await window.onyx.play();
      if (!result.ok) {
        launchError.textContent = formatLaunchError(result.error || 'Launch failed');
        launchError.classList.remove('hidden');
        progressLabel.textContent = 'Launch failed';
      } else {
        progressLabel.textContent = 'Game launched';
        showToast('Minecraft is starting…');
      }
      await refreshReadiness();
    } catch (err) {
      launchError.textContent = formatLaunchError(err.message || 'Launch failed');
      launchError.classList.remove('hidden');
      await refreshReadiness();
    } finally {
      launching = false;
      btnPlay.disabled = false;
    }
  });

  window.addEventListener('onyx:authenticated', async (e) => {
    await enterApp(e.detail, false);
  });

  boot().catch((err) => {
    console.error(err);
    window.OnyxAuthModal.show();
  });
})();
