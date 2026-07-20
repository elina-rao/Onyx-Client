const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const { createWriteStream } = require('fs');
const { URL } = require('url');
const config = require('../config/launcherConfig');
const { resourcesDir } = require('../launch/gameLauncher');

const LOCAL_VERSION = '1.0.1';

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

function download(url, dest) {
  return new Promise((resolve, reject) => {
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    const file = createWriteStream(dest);
    const get = (targetUrl, redirects = 0) => {
      if (redirects > 5) {
        reject(new Error('Too many redirects'));
        return;
      }
      https
        .get(targetUrl, (res) => {
          if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
            get(res.headers.location, redirects + 1);
            return;
          }
          if (res.statusCode !== 200) {
            reject(new Error(`Download failed (${res.statusCode})`));
            return;
          }
          res.pipe(file);
          file.on('finish', () => file.close(() => resolve(dest)));
        })
        .on('error', reject);
    };
    get(url);
  });
}

/**
 * Checks remote version endpoint. Fails gracefully if unreachable.
 * Expected payload:
 * {
 *   "launcher": "1.0.0",
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

    return {
      ok: true,
      localVersion: LOCAL_VERSION,
      remote,
      updates,
      changelog: remote.changelog || null,
      launcherUpdate: remote.launcher && remote.launcher !== LOCAL_VERSION
        ? remote.launcher
        : null
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
      // Also copy loader into packaged resources when possible
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

module.exports = {
  LOCAL_VERSION,
  checkForUpdates,
  applyUpdates
};
