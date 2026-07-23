const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const { createWriteStream } = require('fs');
const { spawn } = require('child_process');
const { URL } = require('url');
const { app } = require('electron');
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

function libraryAllowedOs(lib) {
  const rules = lib.rules;
  if (!rules || !rules.length) {
    return true;
  }
  const osName =
    process.platform === 'darwin' ? 'osx' : process.platform === 'win32' ? 'windows' : 'linux';
  let allowed = false;
  for (const rule of rules) {
    const actionAllow = rule.action === 'allow';
    if (!rule.os) {
      allowed = actionAllow;
      continue;
    }
    if (rule.os.name === osName) {
      allowed = actionAllow;
    }
  }
  return allowed;
}

function extractZipEntry(zipPath, entryName, destFile) {
  const { execFileSync } = require('child_process');
  const tmp = path.join(path.dirname(destFile), `.extract-${Date.now()}`);
  fs.mkdirSync(tmp, { recursive: true });
  try {
    execFileSync('unzip', ['-o', '-q', zipPath, entryName, '-d', tmp], { stdio: 'ignore' });
    const src = path.join(tmp, entryName);
    if (!fs.existsSync(src)) {
      // try basename only
      const alt = path.join(tmp, path.basename(entryName));
      if (!fs.existsSync(alt)) {
        throw new Error(`Entry not found in installer: ${entryName}`);
      }
      fs.mkdirSync(path.dirname(destFile), { recursive: true });
      fs.copyFileSync(alt, destFile);
    } else {
      fs.mkdirSync(path.dirname(destFile), { recursive: true });
      fs.copyFileSync(src, destFile);
    }
  } finally {
    try {
      fs.rmSync(tmp, { recursive: true, force: true });
    } catch (_) {
      /* ignore */
    }
  }
}

async function downloadLibraryArtifact(mcRoot, lib, onProgress, index, total) {
  if (!libraryAllowedOs(lib)) {
    return;
  }
  const artifact = lib.downloads && lib.downloads.artifact;
  let url = artifact && artifact.url;
  let rel = artifact && artifact.path;
  if (!rel && lib.name) {
    const parts = lib.name.split(':');
    if (parts.length >= 3) {
      const [group, art, ver] = parts;
      rel = `${group.replace(/\./g, '/')}/${art}/${ver}/${art}-${ver}.jar`;
    }
  }
  if (!rel) {
    return;
  }
  const dest = path.join(mcRoot, 'libraries', rel);
  if (fs.existsSync(dest) && fs.statSync(dest).size > 0) {
    return;
  }
  if (!url) {
    url = `https://libraries.minecraft.net/${rel.replace(/\\/g, '/')}`;
    if (lib.name && lib.name.startsWith('net.minecraftforge')) {
      url = `https://maven.minecraftforge.net/${rel.replace(/\\/g, '/')}`;
    }
  }
  if (onProgress) {
    onProgress({
      stage: 'libraries',
      message: `Library ${index}/${total}…`,
      percent: 50 + Math.min(35, Math.round((index / Math.max(1, total)) * 35))
    });
  }
  try {
    await download(url, dest);
  } catch (err) {
    // Forge libs often live on forge maven when Mojang URL 404s
    if (!url.includes('minecraftforge')) {
      const forgeUrl = `https://maven.minecraftforge.net/${rel.replace(/\\/g, '/')}`;
      await download(forgeUrl, dest);
    } else {
      throw err;
    }
  }

  // Natives classifiers
  if (lib.natives && lib.downloads && lib.downloads.classifiers) {
    const osName =
      process.platform === 'darwin' ? 'osx' : process.platform === 'win32' ? 'windows' : 'linux';
    let classifier = lib.natives[osName];
    if (classifier) {
      classifier = classifier.replace(
        '${arch}',
        process.arch === 'x64' || process.arch === 'arm64' ? '64' : '32'
      );
      const info = lib.downloads.classifiers[classifier];
      if (info && info.path) {
        const nDest = path.join(mcRoot, 'libraries', info.path);
        if (!fs.existsSync(nDest) || fs.statSync(nDest).size === 0) {
          const nUrl =
            info.url || `https://libraries.minecraft.net/${info.path.replace(/\\/g, '/')}`;
          await download(nUrl, nDest);
        }
      }
    }
  }
}

/**
 * Silent Forge install: extract version.json + universal jar from the installer,
 * download libraries, never open the Forge GUI.
 */
async function installForgeSilent(javaPath, installerJar, mcRoot, onProgress) {
  const progress = onProgress || (() => {});
  const versionDir = path.join(mcRoot, 'versions', FORGE_VERSION_ID);
  fs.mkdirSync(versionDir, { recursive: true });
  const versionJsonPath = path.join(versionDir, `${FORGE_VERSION_ID}.json`);

  progress({ stage: 'forge-extract', message: 'Extracting Forge profile…', percent: 78 });
  if (!fs.existsSync(versionJsonPath)) {
    extractZipEntry(installerJar, 'version.json', versionJsonPath);
  }

  // Universal / forge jar inside installer (several possible names)
  const forgeJarDest = path.join(versionDir, `${FORGE_VERSION_ID}.jar`);
  const forgeJarAlt = path.join(versionDir, 'forge-1.8.9-11.15.1.2318-1.8.9.jar');
  if (!fs.existsSync(forgeJarDest) && !fs.existsSync(forgeJarAlt)) {
    const candidates = [
      `forge-${FORGE_VERSION}-universal.jar`,
      `maven/net/minecraftforge/forge/${FORGE_VERSION}/forge-${FORGE_VERSION}-universal.jar`,
      `forge-${FORGE_VERSION}-1.8.9-universal.jar`
    ];
    let extracted = false;
    for (const entry of candidates) {
      try {
        extractZipEntry(installerJar, entry, forgeJarDest);
        extracted = true;
        break;
      } catch (_) {
        /* try next */
      }
    }
    if (!extracted) {
      // Last resort: pull from Forge maven
      const mavenJar = path.join(
        mcRoot,
        'libraries',
        'net',
        'minecraftforge',
        'forge',
        `${FORGE_VERSION}-1.8.9`,
        `forge-${FORGE_VERSION}-1.8.9.jar`
      );
      const mavenUrl = `https://maven.minecraftforge.net/net/minecraftforge/forge/${FORGE_VERSION}-${MC_VERSION}/forge-${FORGE_VERSION}-${MC_VERSION}-universal.jar`;
      try {
        await download(mavenUrl, mavenJar);
        fs.copyFileSync(mavenJar, forgeJarDest);
      } catch (err) {
        throw new Error(`Could not extract Forge jar from installer: ${err.message}`);
      }
    }
  }

  const versionJson = JSON.parse(fs.readFileSync(versionJsonPath, 'utf8'));
  const libs = versionJson.libraries || [];
  progress({
    stage: 'libraries',
    message: `Downloading ${libs.length} Forge libraries…`,
    percent: 50
  });
  for (let i = 0; i < libs.length; i++) {
    await downloadLibraryArtifact(mcRoot, libs[i], progress, i + 1, libs.length);
  }

  // Ensure vanilla jar still present
  await ensureVanillaClient(mcRoot, progress);

  if (!hasForgeInstall(mcRoot)) {
    throw new Error('Silent Forge install finished but version files are incomplete');
  }
  progress({ stage: 'ready', message: 'Forge 1.8.9 installed (silent)', percent: 100 });
  return { ok: true, minecraftRoot: mcRoot, method: 'silent' };
}

/**
 * Download (if needed) and install Forge 1.8.9 without a GUI.
 * Tries official CLI first, then silent extract + library download.
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
          'Java 8 not found — the launcher should bundle a JRE. Reinstall Onyx Launcher or set Java Path in Settings.'
      };
    }

    const cli = await runForgeInstallerCli(javaPath, installerJar, mcRoot, progress);
    if (hasForgeInstall(mcRoot)) {
      progress({ stage: 'ready', message: 'Forge 1.8.9 installed', percent: 100 });
      return { ok: true, minecraftRoot: mcRoot, method: 'cli' };
    }

    progress({
      stage: 'forge-silent',
      message: 'CLI incomplete — installing Forge silently…',
      percent: 90
    });
    try {
      return await installForgeSilent(javaPath, installerJar, mcRoot, progress);
    } catch (silentErr) {
      return {
        ok: false,
        installerPath: installerJar,
        minecraftRoot: mcRoot,
        error:
          `Could not install Forge automatically.\n${silentErr.message}\n` +
          (cli.error ? `CLI: ${cli.error.split('\n')[0]}\n` : '') +
          `Installer: ${installerJar}\nTarget: ${mcRoot}`
      };
    }
  } catch (err) {
    return { ok: false, error: err.message };
  }
}

/**
 * Ensures vanilla + Forge are ready for Play. Installs Forge silently when missing.
 */
async function ensureGameFiles(onProgress) {
  const progress = onProgress || (() => {});
  const cfg = config.load();
  const gameDir = cfg.gameDir;
  const mcRoot = resolveMinecraftRoot(gameDir);
  fs.mkdirSync(gameDir, { recursive: true });

  const status = isInstalled(gameDir);
  if (status.forge) {
    try {
      await ensureAssetIndex(mcRoot, progress);
    } catch (err) {
      console.warn('[install] Asset index:', err.message);
    }
    progress({ stage: 'ready', message: 'Forge 1.8.9 ready', percent: 100 });
    return { ok: true, status, skipped: true, minecraftRoot: mcRoot };
  }

  try {
    await ensureVanillaClient(mcRoot, progress);
    const forgeResult = await installForge(progress);
    if (!forgeResult.ok || !hasForgeInstall(mcRoot)) {
      return {
        ok: false,
        error:
          (forgeResult && forgeResult.error) ||
          `Minecraft downloaded, but Forge install failed for:\n${mcRoot}`
      };
    }

    fs.writeFileSync(
      path.join(gameDir, 'onyx-install.json'),
      JSON.stringify(
        {
          minecraft: MC_VERSION,
          forge: FORGE_VERSION,
          forgeVersionId: FORGE_VERSION_ID,
          installedAt: new Date().toISOString(),
          method: forgeResult.method || 'silent',
          minecraftRoot: mcRoot
        },
        null,
        2
      ),
      'utf8'
    );

    progress({ stage: 'ready', message: 'Installation complete', percent: 100 });
    return { ok: true, status: isInstalled(gameDir), minecraftRoot: mcRoot };
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
