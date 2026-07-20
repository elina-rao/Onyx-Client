const { app, BrowserWindow, ipcMain, shell, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const config = require('./config/launcherConfig');
const sessionStore = require('./auth/sessionStore');
const microsoftAuth = require('./auth/microsoftAuth');
const { launchGame } = require('./launch/gameLauncher');
const { ensureGameFiles, isInstalled, installForge, getLaunchReadiness } = require('./installer/gameInstaller');
const { checkForUpdates, applyUpdates, LOCAL_VERSION } = require('./updater/autoUpdater');

// Prefer "Onyx Launcher" in menus; Dock reopen still needs the packed .app or wrapper
// (opening node_modules Electron.app alone shows the default Electron splash).
if (typeof app.setName === 'function') {
  app.setName('Onyx Launcher');
}
if (process.platform === 'darwin' && app.dock && typeof app.dock.setIcon === 'function') {
  const iconPng = path.join(__dirname, '../../assets/icons/icon.png');
  if (fs.existsSync(iconPng)) {
    try {
      app.dock.setIcon(iconPng);
    } catch (_) {
      /* ignore */
    }
  }
}

let mainWindow = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1100,
    height: 720,
    minWidth: 900,
    minHeight: 600,
    backgroundColor: '#0D0D0D',
    title: 'Onyx Launcher',
    titleBarStyle: process.platform === 'darwin' ? 'hiddenInset' : 'default',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false
    },
    show: false
  });

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
  });

  mainWindow.loadFile(path.join(__dirname, '../renderer/index.html'));

  if (process.argv.includes('--dev')) {
    mainWindow.webContents.openDevTools({ mode: 'detach' });
  }

  mainWindow.on('closed', () => {
    microsoftAuth.cancelMicrosoftLogin(mainWindow);
    mainWindow = null;
  });
}

function send(channel, payload) {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send(channel, payload);
  }
}

function registerIpc() {
  ipcMain.handle('auth:status', async () => {
    const session = await sessionStore.getActiveSession();
    return { session };
  });

  ipcMain.handle('auth:try-refresh', async () => {
    const existing = await sessionStore.getActiveSession();
    if (existing && existing.type === 'guest') {
      return { ok: true, session: existing, welcomeBack: true };
    }
    if (existing && existing.type === 'microsoft') {
      const result = await microsoftAuth.trySilentRefresh();
      if (result.ok) {
        const session = await sessionStore.getActiveSession();
        return { ok: true, session, welcomeBack: true };
      }
      return { ok: false, reason: result.reason || 'expired', sessionExpired: true };
    }
    // No session — try refresh token alone
    const refresh = await sessionStore.getRefreshToken();
    if (refresh) {
      const result = await microsoftAuth.trySilentRefresh();
      if (result.ok) {
        const session = await sessionStore.getActiveSession();
        return { ok: true, session, welcomeBack: true };
      }
      return { ok: false, reason: 'expired', sessionExpired: true };
    }
    return { ok: false, reason: 'none' };
  });

  ipcMain.handle('auth:start-microsoft', async () => {
    if (!mainWindow) {
      return { ok: false, error: 'No window' };
    }
    try {
      const profile = await microsoftAuth.startMicrosoftLogin(mainWindow);
      const session = await sessionStore.getActiveSession();
      config.save({ lastUsername: profile.username, guest: null });
      return { ok: true, session, profile };
    } catch (err) {
      return { ok: false, error: err.message };
    }
  });

  ipcMain.handle('auth:cancel-microsoft', async () => {
    if (mainWindow) {
      microsoftAuth.cancelMicrosoftLogin(mainWindow);
    }
    return { ok: true };
  });

  ipcMain.handle('auth:guest', async (_e, username) => {
    if (!username || !String(username).trim()) {
      return { ok: false, error: 'Username required' };
    }
    const guest = sessionStore.setGuestSession(String(username));
    await sessionStore.clearRefreshToken();
    const session = await sessionStore.getActiveSession();
    return { ok: true, session, guest };
  });

  ipcMain.handle('auth:sign-out', async () => {
    await sessionStore.signOut();
    return { ok: true };
  });

  ipcMain.handle('settings:get', async () => {
    const cfg = config.load();
    const {
      resolveMinecraftRoot,
      hasForgeInstall,
      resolveJavaPath,
      resourcesDir
    } = require('./launch/gameLauncher');
    const mcRoot = resolveMinecraftRoot(cfg.gameDir);
    const resolvedJava = resolveJavaPath(cfg.javaPath);
    const bundledJava = path.join(
      resourcesDir(),
      'jre',
      process.platform === 'win32' ? 'bin/java.exe' : 'bin/java'
    );
    const hasBundledJre = fs.existsSync(bundledJava);
    const javaResolvedExists = !!(resolvedJava && fs.existsSync(resolvedJava));
    return {
      ...cfg,
      maxRamGb: config.detectMaxRamGb(),
      version: LOCAL_VERSION,
      installStatus: isInstalled(cfg.gameDir),
      forgeReady: hasForgeInstall(mcRoot),
      minecraftRoot: mcRoot,
      resolvedJavaPath: resolvedJava,
      hasBundledJre,
      javaResolvedExists,
      readiness: getLaunchReadiness()
    };
  });

  ipcMain.handle('launch:readiness', async () => getLaunchReadiness());

  ipcMain.handle('forge:install', async () => {
    const progress = (p) => send('launch:progress', p);
    try {
      return await installForge(progress);
    } catch (err) {
      return { ok: false, error: err.message };
    }
  });

  ipcMain.handle('settings:set', async (_e, partial) => {
    const next = config.save(partial || {});
    return { ok: true, config: next };
  });

  ipcMain.handle('dialog:pick-java', async () => {
    if (!mainWindow) {
      return { ok: false, cancelled: true };
    }
    const result = await dialog.showOpenDialog(mainWindow, {
      title: 'Select Java binary',
      properties: ['openFile'],
      message: 'Choose the Java 8 executable (bin/java)'
    });
    if (result.canceled || !result.filePaths || !result.filePaths[0]) {
      return { ok: false, cancelled: true };
    }
    return { ok: true, path: result.filePaths[0] };
  });

  ipcMain.handle('dialog:pick-game-dir', async () => {
    if (!mainWindow) {
      return { ok: false, cancelled: true };
    }
    const result = await dialog.showOpenDialog(mainWindow, {
      title: 'Select game directory',
      properties: ['openDirectory', 'createDirectory'],
      message: 'Choose your Minecraft / Onyx game folder'
    });
    if (result.canceled || !result.filePaths || !result.filePaths[0]) {
      return { ok: false, cancelled: true };
    }
    return { ok: true, path: result.filePaths[0] };
  });

  ipcMain.handle('stats:fetch', async () => {
    const session = await sessionStore.getActiveSession();
    if (!session) {
      return { ok: false, reason: 'none' };
    }
    if (session.guest || session.type === 'guest') {
      return { ok: false, reason: 'guest' };
    }
    const cfg = config.load();
    const base = String(cfg.onyxApiEndpoint || 'https://api.onyxrbw.com').replace(/\/$/, '');
    const url = `${base}/stats/${encodeURIComponent(session.uuid)}`;
    try {
      const res = await fetch(url, {
        headers: { Accept: 'application/json' },
        signal: AbortSignal.timeout(8000)
      });
      if (!res.ok) {
        return { ok: false, reason: 'http', status: res.status };
      }
      const data = await res.json();
      return {
        ok: true,
        elo: data.elo ?? data.ELO ?? data.rating ?? null,
        rank: data.rank ?? data.Rank ?? data.tier ?? null,
        wins: data.wins ?? data.Wins ?? data.victories ?? null,
        raw: data
      };
    } catch (err) {
      return { ok: false, reason: 'network', error: err.message };
    }
  });

  ipcMain.handle('launch:play', async () => {
    const progress = (p) => send('launch:progress', p);
    try {
      progress({ stage: 'install', message: 'Verifying game files…', percent: 5 });
      const install = await ensureGameFiles((p) => send('launch:progress', p));
      if (!install.ok) {
        return { ok: false, error: install.error || 'Install failed' };
      }

      const result = await launchGame({}, progress);
      return result;
    } catch (err) {
      return { ok: false, error: err.message };
    }
  });

  ipcMain.handle('updater:check', async () => {
    const info = await checkForUpdates((p) => send('updater:progress', p));
    if (info.ok && info.updates && info.updates.length) {
      const applied = await applyUpdates(info, (p) => send('updater:progress', p));
      return { ...info, applied };
    }
    return info;
  });

  ipcMain.handle('shell:open-external', async (_e, url) => {
    if (typeof url === 'string' && /^https?:\/\//i.test(url)) {
      await shell.openExternal(url);
      return { ok: true };
    }
    return { ok: false };
  });

  ipcMain.handle('app:get-version', async () => LOCAL_VERSION);
}

app.whenReady().then(() => {
  registerIpc();
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
