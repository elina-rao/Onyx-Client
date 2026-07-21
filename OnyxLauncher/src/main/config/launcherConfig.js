const fs = require('fs');
const path = require('path');
const { app } = require('electron');
const os = require('os');

const DEFAULTS = {
  ramGb: 4,
  javaPath: '',
  gameDir: '',
  theme: 'onyx-dark',
  lastUsername: '',
  guest: null,
  onyxApiEndpoint: 'https://api.onyxrbw.com',
  discordInvite: 'https://discord.gg/onyxrbw',
  serverIp: 'eu.onyxrbw.com',
  termsUrl: 'https://onyxrbw.com/terms'
};

function configPath() {
  return path.join(app.getPath('userData'), 'launcher.json');
}

function defaultMinecraftDir() {
  const home = os.homedir();
  if (process.platform === 'darwin') {
    return path.join(home, 'Library', 'Application Support', 'minecraft');
  }
  if (process.platform === 'win32') {
    return path.join(process.env.APPDATA || path.join(home, 'AppData', 'Roaming'), '.minecraft');
  }
  return path.join(home, '.minecraft');
}

function defaultOnyxDir() {
  const home = os.homedir();
  if (process.platform === 'darwin') {
    return path.join(home, 'Library', 'Application Support', 'onyxclient');
  }
  if (process.platform === 'win32') {
    return path.join(process.env.APPDATA || path.join(home, 'AppData', 'Roaming'), 'onyxclient');
  }
  return path.join(home, '.onyxclient');
}

/**
 * Prefer the standard Minecraft folder when Forge 1.8.9 is already installed
 * there so Play works out of the box with an existing install.
 */
function defaultGameDir() {
  const mc = defaultMinecraftDir();
  const forgeJson = path.join(
    mc,
    'versions',
    '1.8.9-Forge11.15.1.2318-1.8.9',
    '1.8.9-Forge11.15.1.2318-1.8.9.json'
  );
  const vanillaJar = path.join(mc, 'versions', '1.8.9', '1.8.9.jar');
  if (fs.existsSync(forgeJson) && fs.existsSync(vanillaJar)) {
    return mc;
  }
  return defaultOnyxDir();
}

function load() {
  const file = configPath();
  let data = { ...DEFAULTS, gameDir: defaultGameDir() };
  try {
    if (fs.existsSync(file)) {
      const raw = JSON.parse(fs.readFileSync(file, 'utf8'));
      data = { ...data, ...raw };
    }
  } catch (err) {
    console.error('[config] Failed to load launcher.json:', err.message);
  }
  if (!data.gameDir) {
    data.gameDir = defaultGameDir();
  }
  // If saved gameDir has no Forge but the standard Minecraft folder does, prefer Minecraft
  try {
    const forgeJson = path.join(
      data.gameDir,
      'versions',
      '1.8.9-Forge11.15.1.2318-1.8.9',
      '1.8.9-Forge11.15.1.2318-1.8.9.json'
    );
    const mc = defaultMinecraftDir();
    const mcForge = path.join(
      mc,
      'versions',
      '1.8.9-Forge11.15.1.2318-1.8.9',
      '1.8.9-Forge11.15.1.2318-1.8.9.json'
    );
    if (!fs.existsSync(forgeJson) && fs.existsSync(mcForge) && path.resolve(data.gameDir) !== path.resolve(mc)) {
      data.gameDir = mc;
    }
  } catch (_) {
    /* ignore */
  }
  return data;
}

function save(partial) {
  const current = load();
  const next = { ...current, ...partial };
  const file = configPath();
  try {
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, JSON.stringify(next, null, 2), 'utf8');
  } catch (err) {
    console.error('[config] Failed to save launcher.json:', err.message);
  }
  return next;
}

function detectMaxRamGb() {
  const totalGb = Math.floor(os.totalmem() / (1024 * 1024 * 1024));
  return Math.max(2, totalGb - 2);
}

module.exports = {
  DEFAULTS,
  load,
  save,
  configPath,
  defaultGameDir,
  defaultMinecraftDir,
  defaultOnyxDir,
  detectMaxRamGb
};
