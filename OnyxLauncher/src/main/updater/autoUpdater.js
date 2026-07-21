const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const crypto = require('crypto');
const { spawn, execFileSync } = require('child_process');
const { createWriteStream } = require('fs');
const { URL } = require('url');
const { app } = require('electron');
const config = require('../config/launcherConfig');
const { resourcesDir } = require('../launch/gameLauncher');

function readPackageVersion() {
  try {
    const pkgPath = path.join(__dirname, '../../../package.json');
    const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
    if (pkg && typeof pkg.version === 'string' && pkg.version.trim()) {
      return pkg.version.trim();
    }
  } catch (_) {
    /* ignore */
  }
  return '1.0.3';
}

const LOCAL_VERSION = readPackageVersion();

function parseSemver(v) {
  if (!v || typeof v !== 'string') {
    return null;
  }
  const cleaned = v.trim().replace(/^v/i, '');
  const parts = cleaned.split('.').map((p) => parseInt(p, 10));
  if (parts.length < 2 || parts.some((n) => Number.isNaN(n))) {
    return null;
  }
  while (parts.length < 3) {
    parts.push(0);
  }
  return parts.slice(0, 3);
}

/** @returns {boolean} true if remote is strictly newer than local */
function isNewerVersion(remote, local) {
  const r = parseSemver(remote);
  const l = parseSemver(local);
  if (!r || !l) {
    return Boolean(remote && local && remote !== local);
  }
  for (let i = 0; i < 3; i++) {
    if (r[i] > l[i]) {
      return true;
    }
    if (r[i] < l[i]) {
      return false;
    }
  }
  return false;
}

function fetchJson(url, timeoutMs = 8000) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const lib = parsed.protocol === 'https:' ? https : http;
    const req = lib.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        fetchJson(res.headers.location, timeoutMs).then(resolve).catch(reject);
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
    });
    req.on('error', reject);
    req.setTimeout(timeoutMs, () => {
      req.destroy();
      reject(new Error('Update check timed out'));
    });
  });
}

function download(url, dest, onProgress) {
  return new Promise((resolve, reject) => {
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    const file = createWriteStream(dest);
    const get = (targetUrl, redirects = 0) => {
      if (redirects > 5) {
        reject(new Error('Too many redirects'));
        return;
      }
      const parsed = new URL(targetUrl);
      const lib = parsed.protocol === 'https:' ? https : http;
      lib
        .get(targetUrl, (res) => {
          if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
            get(res.headers.location, redirects + 1);
            return;
          }
          if (res.statusCode !== 200) {
            reject(new Error(`Download failed (${res.statusCode})`));
            return;
          }
          const total = parseInt(res.headers['content-length'] || '0', 10);
          let received = 0;
          res.on('data', (chunk) => {
            received += chunk.length;
            if (onProgress && total > 0) {
              onProgress(Math.min(99, Math.round((received / total) * 100)));
            }
          });
          res.pipe(file);
          file.on('finish', () =>
            file.close(() => {
              if (onProgress) {
                onProgress(100);
              }
              resolve(dest);
            })
          );
        })
        .on('error', reject);
    };
    get(url);
  });
}

function sha256File(filePath) {
  const hash = crypto.createHash('sha256');
  hash.update(fs.readFileSync(filePath));
  return hash.digest('hex');
}

function findAppBundle(rootDir) {
  const stack = [rootDir];
  while (stack.length) {
    const dir = stack.pop();
    let entries;
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true });
    } catch (_) {
      continue;
    }
    for (const ent of entries) {
      const full = path.join(dir, ent.name);
      if (ent.isDirectory()) {
        if (ent.name.endsWith('.app')) {
          return full;
        }
        stack.push(full);
      }
    }
  }
  return null;
}

/**
 * Checks remote version endpoint. Fails gracefully if unreachable.
 * Expected payload:
 * {
 *   "launcher": "1.0.3",
 *   "launcherUrl": "https://…/OnyxLauncher-mac.zip",
 *   "launcherSha256": "optional",
 *   "loader": "1.0.0",
 *   "client": "1.0.0",
 *   "loaderUrl": "...",
 *   "clientUrl": "...",
 *   "changelog": "..."
 * }
 */
async function checkForUpdates(onProgress) {
  const cfg = config.load();
  const endpoint = `${cfg.onyxApiEndpoint.replace(/\/$/, '')}/launcher/version`;

  try {
    onProgress && onProgress({ stage: 'checking', message: 'Checking for updates…' });
    const remote = await fetchJson(endpoint);
    const updates = [];

    if (remote.loader && remote.loaderUrl) {
      updates.push({
        kind: 'loader',
        version: remote.loader,
        url: remote.loaderUrl,
        dest: path.join(cfg.gameDir, 'OnyxLoader.jar')
      });
    }
    if (remote.client && remote.clientUrl) {
      updates.push({
        kind: 'client',
        version: remote.client,
        url: remote.clientUrl,
        dest: path.join(cfg.gameDir, 'mods', 'OnyxClient-1.8.9-v1.0.jar')
      });
    }

    let launcherUpdate = null;
    if (remote.launcher && isNewerVersion(remote.launcher, LOCAL_VERSION)) {
      launcherUpdate = {
        version: remote.launcher,
        url: typeof remote.launcherUrl === 'string' ? remote.launcherUrl : null,
        sha256: typeof remote.launcherSha256 === 'string' ? remote.launcherSha256 : null
      };
    }

    return {
      ok: true,
      localVersion: LOCAL_VERSION,
      remote,
      updates,
      changelog: remote.changelog || null,
      launcherUpdate
    };
  } catch (err) {
    console.warn('[updater] Skipping update check:', err.message);
    return {
      ok: false,
      skipped: true,
      error: err.message,
      localVersion: LOCAL_VERSION
    };
  }
}

async function applyUpdates(updateInfo, onProgress) {
  if (!updateInfo || !updateInfo.updates || !updateInfo.updates.length) {
    return { ok: true, applied: [] };
  }

  const applied = [];
  for (let i = 0; i < updateInfo.updates.length; i++) {
    const u = updateInfo.updates[i];
    onProgress &&
      onProgress({
        stage: 'download',
        message: `Downloading ${u.kind} ${u.version}…`,
        percent: Math.round((i / updateInfo.updates.length) * 100)
      });
    try {
      const tmp = `${u.dest}.tmp`;
      await download(u.url, tmp);
      fs.mkdirSync(path.dirname(u.dest), { recursive: true });
      fs.renameSync(tmp, u.dest);
      if (u.kind === 'loader') {
        try {
          const resCopy = path.join(resourcesDir(), 'OnyxLoader.jar');
          fs.mkdirSync(path.dirname(resCopy), { recursive: true });
          fs.copyFileSync(u.dest, resCopy);
        } catch (_) {
          /* ignore */
        }
      }
      applied.push(u);
    } catch (err) {
      return { ok: false, error: err.message, applied };
    }
  }

  onProgress && onProgress({ stage: 'done', message: 'Updates applied', percent: 100 });
  return { ok: true, applied, changelog: updateInfo.changelog };
}

/**
 * Download launcher zip, verify optional sha256, extract, find .app, schedule replace.
 */
async function downloadAndStageLauncherUpdate(launcherUpdate, onProgress) {
  if (!launcherUpdate || !launcherUpdate.url) {
    return { ok: false, error: 'No launcherUrl provided by the update server' };
  }

  const tmpRoot = path.join(app.getPath('temp'), 'onyx-launcher-update');
  fs.rmSync(tmpRoot, { recursive: true, force: true });
  fs.mkdirSync(tmpRoot, { recursive: true });

  const archivePath = path.join(tmpRoot, 'launcher-update.zip');
  onProgress && onProgress({ stage: 'download', message: 'Downloading launcher…', percent: 0 });
  await download(launcherUpdate.url, archivePath, (pct) => {
    onProgress && onProgress({ stage: 'download', message: 'Downloading launcher…', percent: pct });
  });

  if (launcherUpdate.sha256) {
    const actual = sha256File(archivePath);
    if (actual.toLowerCase() !== launcherUpdate.sha256.toLowerCase()) {
      return { ok: false, error: 'Launcher download failed integrity check' };
    }
  }

  const extractDir = path.join(tmpRoot, 'extracted');
  fs.mkdirSync(extractDir, { recursive: true });
  onProgress && onProgress({ stage: 'extract', message: 'Preparing launcher…', percent: 0 });

  try {
    execFileSync('unzip', ['-oq', archivePath, '-d', extractDir], { stdio: 'ignore' });
  } catch (err) {
    return { ok: false, error: 'Could not extract launcher update (expected a .zip)' };
  }

  const stagedApp = findAppBundle(extractDir);
  if (!stagedApp) {
    return { ok: false, error: 'Update archive did not contain an .app bundle' };
  }

  return { ok: true, stagedApp, version: launcherUpdate.version };
}

function applicationsDest() {
  return '/Applications/Onyx Launcher.app';
}

/**
 * Spawn a detached shell script that waits for this process to exit,
 * replaces /Applications/Onyx Launcher.app, then relaunches.
 */
function scheduleLauncherReplaceAndRelaunch(stagedAppPath) {
  const dest = applicationsDest();
  const scriptPath = path.join(app.getPath('temp'), 'onyx-apply-launcher-update.sh');
  const pid = process.pid;
  const script = `#!/bin/bash
set -euo pipefail
DEST=${JSON.stringify(dest)}
SRC=${JSON.stringify(stagedAppPath)}
PID=${pid}
# Wait until the running launcher exits
while kill -0 "$PID" 2>/dev/null; do
  sleep 0.4
done
sleep 0.6
rm -rf "$DEST"
cp -R "$SRC" "$DEST"
xattr -cr "$DEST" 2>/dev/null || true
find "$DEST" -name '._*' -delete 2>/dev/null || true
codesign --force --deep --sign - "$DEST" 2>/dev/null || true
open -a "$DEST"
`;
  fs.writeFileSync(scriptPath, script, { mode: 0o755 });
  const child = spawn('/bin/bash', [scriptPath], {
    detached: true,
    stdio: 'ignore'
  });
  child.unref();
  return { ok: true };
}

module.exports = {
  LOCAL_VERSION,
  isNewerVersion,
  checkForUpdates,
  applyUpdates,
  downloadAndStageLauncherUpdate,
  scheduleLauncherReplaceAndRelaunch,
  applicationsDest
};
