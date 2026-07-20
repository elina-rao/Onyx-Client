const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const { createWriteStream } = require('fs');
const { spawn } = require('child_process');
const { URL } = require('url');
const { app, shell } = require('electron');
const config = require('../config/launcherConfig');
const {
  hasForgeInstall,
  resolveMinecraftRoot,
  defaultMinecraftDir,
  findClientJarCandidates
} = require('../launch/forgeLauncher');
const { resolveJavaPath } = require('../launch/gameLauncher');

const MC_VERSION = '1.8.9';
const FORGE_VERSION = '1.8.9-11.15.1.2318';
const FORGE_VERSION_ID = '1.8.9-Forge11.15.1.2318-1.8.9';
const VERSION_MANIFEST = 'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json';
const FORGE_INSTALLER_URL =
  `https://maven.minecraftforge.net/net/minecraftforge/forge/${FORGE_VERSION}/forge-${FORGE_VERSION}-installer.jar`;

function download(url, dest, onProgress) {
  return new Promise((resolve, reject) => {
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    const file = createWriteStream(dest);
    const get = (targetUrl, redirects = 0) => {
      if (redirects > 5) {
        reject(new Error('Too many redirects'));
        return;
      }
      const p = new URL(targetUrl);
      const l = p.protocol === 'https:' ? https : http;
      l.get(targetUrl, (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          get(res.headers.location, redirects + 1);
          return;
        }
        if (res.statusCode !== 200) {
          reject(new Error(`Download failed (${res.statusCode}): ${targetUrl}`));
          return;
        }
        const total = parseInt(res.headers['content-length'] || '0', 10);
        let received = 0;
        res.on('data', (chunk) => {
          received += chunk.length;
          if (onProgress && total) {
            onProgress(Math.round((received / total) * 100));
          }
        });
        res.pipe(file);
        file.on('finish', () => {
          file.close(() => resolve(dest));
        });
      }).on('error', (err) => {
        try {
          fs.unlinkSync(dest);
        } catch (_) {
          /* ignore */
        }
        reject(err);
      });
    };

    get(url);
  });
}

function fetchJson(url) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith('https') ? https : http;
    lib
      .get(url, (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          fetchJson(res.headers.location).then(resolve).catch(reject);
          return;
        }
        let data = '';
        res.on('data', (c) => {
          data += c;
        });
        res.on('end', () => {
          try {
            resolve(JSON.parse(data));
          } catch (err) {
            reject(err);
          }
        });
      })
      .on('error', reject);
  });
}

function forgeInstallerPath(gameDir) {
  return path.join(gameDir, 'forge', `forge-${FORGE_VERSION}-installer.jar`);
}

function hasClientJar(gameDir, mcRoot) {
  const appPath = app.getAppPath();
  if (findClientJarCandidates(appPath).length) {
    return true;
  }
  const checkMods = (dir) => {
    try {
      if (!fs.existsSync(dir)) return false;
      return fs.readdirSync(dir).some((n) => /^onyxclient.*\.jar$/i.test(n));
    } catch (_) {
      return false;
    }
  };
  return checkMods(path.join(gameDir, 'mods')) || checkMods(path.join(mcRoot, 'mods'));
}

function getLaunchReadiness() {
  const cfg = config.load();
  const gameDir = cfg.gameDir;
  const mcRoot = resolveMinecraftRoot(gameDir);
  const resolvedJava = resolveJavaPath(cfg.javaPath);
  const javaOk = !!(resolvedJava && fs.existsSync(resolvedJava));
  const forgeOk = hasForgeInstall(mcRoot);
  const clientOk = hasClientJar(gameDir, mcRoot);
  return {
    javaOk,
    forgeOk,
    clientOk,
    ready: javaOk && forgeOk && clientOk,
    resolvedJavaPath: resolvedJava,
    minecraftRoot: mcRoot,
    gameDir,
    forgeInstaller: fs.existsSync(forgeInstallerPath(gameDir))
      ? forgeInstallerPath(gameDir)
      : null
  };
}

function isInstalled(gameDir) {
  const mcRoot = resolveMinecraftRoot(gameDir);
  const forge = hasForgeInstall(mcRoot);
  const jar = path.join(mcRoot, 'versions', MC_VERSION, `${MC_VERSION}.jar`);
  return {
    minecraft: fs.existsSync(jar),
    forge,
    ready: forge,
    minecraftRoot: mcRoot,
    clientJar: hasClientJar(gameDir, mcRoot)
  };
}

async function ensureAssetIndex(mcRoot, onProgress, versionJson) {
  const indexPath = path.join(mcRoot, 'assets', 'indexes', '1.8.json');
  if (fs.existsSync(indexPath)) {
    return;
  }
  let indexUrl = versionJson && versionJson.assetIndex && versionJson.assetIndex.url;
  if (!indexUrl) {
    if (onProgress) {
      onProgress({ stage: 'assets', message: 'Fetching asset index…', percent: 70 });
    }
    const manifest = await fetchJson(VERSION_MANIFEST);
    const entry = (manifest.versions || []).find((v) => v.id === MC_VERSION);
    if (!entry) return;
    const vj = await fetchJson(entry.url);
    indexUrl = vj.assetIndex && vj.assetIndex.url;
  }
  if (!indexUrl) return;
  if (onProgress) {
    onProgress({ stage: 'assets', message: 'Downloading asset index 1.8…', percent: 72 });
  }
  await download(indexUrl, indexPath);
}

async function ensureVanillaClient(mcRoot, onProgress) {
  const versionsDir = path.join(mcRoot, 'versions', MC_VERSION);
  const jarPath = path.join(versionsDir, `${MC_VERSION}.jar`);
  if (fs.existsSync(jarPath)) {
    return;
  }

  onProgress({ stage: 'manifest', message: 'Fetching version manifest…', percent: 5 });
  const manifest = await fetchJson(VERSION_MANIFEST);
  const entry = (manifest.versions || []).find((v) => v.id === MC_VERSION);
  if (!entry) {
    throw new Error(`Minecraft ${MC_VERSION} not found in version manifest`);
  }

  onProgress({ stage: 'version', message: 'Downloading version metadata…', percent: 15 });
  const versionJson = await fetchJson(entry.url);
  fs.mkdirSync(versionsDir, { recursive: true });
  fs.writeFileSync(
    path.join(versionsDir, `${MC_VERSION}.json`),
    JSON.stringify(versionJson, null, 2),
    'utf8'
  );

  const client = versionJson.downloads && versionJson.downloads.client;
  if (!client || !client.url) {
    throw new Error('Client download URL missing from version JSON');
  }

  onProgress({ stage: 'client', message: 'Downloading Minecraft 1.8.9…', percent: 25 });
  await download(client.url, jarPath, (p) => {
    onProgress({
      stage: 'client',
      message: `Downloading Minecraft 1.8.9… ${p}%`,
      percent: 25 + Math.round(p * 0.4)
    });
  });

  try {
    await ensureAssetIndex(mcRoot, onProgress, versionJson);
  } catch (err) {
    console.warn('[install] Asset index:', err.message);
  }
}

async function ensureForgeInstallerJar(gameDir, onProgress) {
  const forgeInstaller = forgeInstallerPath(gameDir);
  fs.mkdirSync(path.dirname(forgeInstaller), { recursive: true });
  if (fs.existsSync(forgeInstaller)) {
    return forgeInstaller;
  }
  onProgress({ stage: 'forge', message: 'Downloading Forge installer…', percent: 70 });
  await download(FORGE_INSTALLER_URL, forgeInstaller, (p) => {
    onProgress({
      stage: 'forge',
      message: `Downloading Forge installer… ${p}%`,
      percent: 70 + Math.round(p * 0.15)
    });
  });
  return forgeInstaller;
}

function runForgeInstallerCli(javaPath, installerJar, mcRoot, onProgress) {
  return new Promise((resolve) => {
    onProgress({
      stage: 'forge-install',
      message: 'Running Forge installer (client)…',
      percent: 88
    });
    const args = ['-jar', installerJar, '--installClient', mcRoot];
    const child = spawn(javaPath, args, {
      cwd: path.dirname(installerJar),
      env: { ...process.env },
      stdio: ['ignore', 'pipe', 'pipe']
    });
    let stderr = '';
    child.stderr.on('data', (d) => {
      stderr += d.toString();
      if (stderr.length > 4000) {
        stderr = stderr.slice(-4000);
      }
    });
    const timer = setTimeout(() => {
      try {
        child.kill();
      } catch (_) {
        /* ignore */
      }
      resolve({ ok: false, timedOut: true, error: 'Forge installer timed out' });
    }, 180000);
    child.on('error', (err) => {
      clearTimeout(timer);
      resolve({ ok: false, error: err.message });
    });
    child.on('close', (code) => {
      clearTimeout(timer);
      resolve({
        ok: code === 0,
        code,
        error:
          code === 0
            ? null
            : `Forge installer exited with code ${code}${stderr ? `\n${stderr.trim()}` : ''}`
      });
    });
  });
}

/**
 * Download (if needed) and run the Forge 1.8.9 client installer into the
 * resolved Minecraft root. Falls back to opening the GUI installer.
 */
async function installForge(onProgress) {
  const progress = onProgress || (() => {});
  const cfg = config.load();
  const gameDir = cfg.gameDir;
  const mcRoot = resolveMinecraftRoot(gameDir);
  fs.mkdirSync(gameDir, { recursive: true });
  fs.mkdirSync(mcRoot, { recursive: true });

  if (hasForgeInstall(mcRoot)) {
    progress({ stage: 'ready', message: 'Forge 1.8.9 already installed', percent: 100 });
    return { ok: true, alreadyInstalled: true, minecraftRoot: mcRoot };
  }

  try {
    await ensureVanillaClient(mcRoot, progress);
    const installerJar = await ensureForgeInstallerJar(gameDir, progress);
    const javaPath = resolveJavaPath(cfg.javaPath);
    if (!javaPath || !fs.existsSync(javaPath)) {
      return {
        ok: false,
        error:
          'Java 8 not found — set Java Path in Settings (Browse) or install JDK 8 before installing Forge'
      };
    }

    const cli = await runForgeInstallerCli(javaPath, installerJar, mcRoot, progress);
    if (hasForgeInstall(mcRoot)) {
      progress({ stage: 'ready', message: 'Forge 1.8.9 installed', percent: 100 });
      return { ok: true, minecraftRoot: mcRoot, method: 'cli' };
    }

    progress({
      stage: 'forge-gui',
      message: 'Opening Forge installer — choose Install client…',
      percent: 95
    });
    try {
      spawn(javaPath, ['-jar', installerJar], {
        cwd: path.dirname(installerJar),
        detached: true,
        stdio: 'ignore'
      }).unref();
    } catch (_) {
      await shell.openPath(installerJar);
    }

    return {
      ok: false,
      needsManual: true,
      installerPath: installerJar,
      minecraftRoot: mcRoot,
      error:
        `CLI install did not finish${cli.error ? ` (${cli.error.split('\n')[0]})` : ''}.\n\n` +
        `The Forge installer window should be open — choose Install client, set the folder to:\n${mcRoot}\n\nThen press Refresh readiness or Play.`
    };
  } catch (err) {
    return { ok: false, error: err.message };
  }
}

/**
 * Ensures vanilla client jar + asset index exist. Forge must already be installed
 * (or the user runs Install Forge). When Forge is present this is a no-op success.
 */
async function ensureGameFiles(onProgress) {
  const cfg = config.load();
  const gameDir = cfg.gameDir;
  const mcRoot = resolveMinecraftRoot(gameDir);
  fs.mkdirSync(gameDir, { recursive: true });

  const status = isInstalled(gameDir);
  if (status.forge) {
    try {
      await ensureAssetIndex(mcRoot, onProgress);
    } catch (err) {
      console.warn('[install] Asset index:', err.message);
    }
    onProgress({ stage: 'ready', message: 'Forge 1.8.9 ready', percent: 100 });
    return { ok: true, status, skipped: true, minecraftRoot: mcRoot };
  }

  try {
    await ensureVanillaClient(mcRoot, onProgress);

    let forgeInstaller = null;
    try {
      forgeInstaller = await ensureForgeInstallerJar(gameDir, onProgress);
    } catch (err) {
      onProgress({
        stage: 'forge-warn',
        message: `Forge download skipped: ${err.message}`,
        percent: 90
      });
    }

    fs.writeFileSync(
      path.join(gameDir, 'onyx-install.json'),
      JSON.stringify(
        {
          minecraft: MC_VERSION,
          forge: FORGE_VERSION,
          forgeVersionId: FORGE_VERSION_ID,
          installedAt: new Date().toISOString(),
          forgeInstaller: forgeInstaller && fs.existsSync(forgeInstaller) ? forgeInstaller : null,
          minecraftRoot: mcRoot,
          hint:
            'Use Install Forge in the launcher, or run the Forge installer and press Play again.'
        },
        null,
        2
      ),
      'utf8'
    );

    if (!hasForgeInstall(mcRoot)) {
      return {
        ok: false,
        error:
          `Minecraft files downloaded, but Forge is not installed yet.\n\n` +
          `Use Install Forge 1.8.9 on the Home screen, or run:\n${forgeInstaller || FORGE_INSTALLER_URL}\n` +
          `Target folder: ${mcRoot || defaultMinecraftDir()}`
      };
    }

    onProgress({ stage: 'ready', message: 'Installation complete', percent: 100 });
    return { ok: true, status: isInstalled(gameDir), forgeInstaller, minecraftRoot: mcRoot };
  } catch (err) {
    return { ok: false, error: err.message };
  }
}

module.exports = {
  MC_VERSION,
  FORGE_VERSION,
  FORGE_VERSION_ID,
  isInstalled,
  ensureGameFiles,
  installForge,
  getLaunchReadiness
};
