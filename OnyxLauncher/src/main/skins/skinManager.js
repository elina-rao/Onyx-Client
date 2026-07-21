const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const { URL } = require('url');
const { app } = require('electron');
const { v4: uuidv4 } = require('uuid');
const microsoftAuth = require('../auth/microsoftAuth');

const INDEX_NAME = 'index.json';

function skinsDir() {
  return path.join(app.getPath('userData'), 'skins');
}

function indexPath() {
  return path.join(skinsDir(), INDEX_NAME);
}

function ensureLibrary() {
  const dir = skinsDir();
  fs.mkdirSync(dir, { recursive: true });
  if (!fs.existsSync(indexPath())) {
    fs.writeFileSync(
      indexPath(),
      JSON.stringify({ version: 1, activeId: null, model: 'classic', skins: [] }, null, 2),
      'utf8'
    );
  }
  return dir;
}

function readIndex() {
  ensureLibrary();
  try {
    const raw = JSON.parse(fs.readFileSync(indexPath(), 'utf8'));
    if (!raw || !Array.isArray(raw.skins)) {
      return { version: 1, activeId: null, model: 'classic', skins: [] };
    }
    return {
      version: 1,
      activeId: raw.activeId || null,
      model: raw.model === 'slim' ? 'slim' : 'classic',
      skins: raw.skins
    };
  } catch (_) {
    return { version: 1, activeId: null, model: 'classic', skins: [] };
  }
}

function writeIndex(data) {
  const dir = skinsDir();
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(indexPath(), JSON.stringify(data, null, 2), 'utf8');
}

function entryPath(entry) {
  return path.join(skinsDir(), entry.file);
}

function pngDimensions(buf) {
  if (!buf || buf.length < 24) return null;
  if (buf[0] !== 0x89 || buf[1] !== 0x50 || buf[2] !== 0x4e || buf[3] !== 0x47) return null;
  const width = buf.readUInt32BE(16);
  const height = buf.readUInt32BE(20);
  return { width, height };
}

function validateSkinPng(buf) {
  const dim = pngDimensions(buf);
  if (!dim) {
    return { ok: false, error: 'Not a valid PNG' };
  }
  const okSize =
    (dim.width === 64 && dim.height === 64) || (dim.width === 64 && dim.height === 32);
  if (!okSize) {
    return { ok: false, error: 'Skin must be 64×64 or 64×32 PNG' };
  }
  return { ok: true, width: dim.width, height: dim.height };
}

function list() {
  const idx = readIndex();
  return {
    ok: true,
    dir: skinsDir(),
    activeId: idx.activeId,
    model: idx.model,
    skins: idx.skins.map((s) => ({
      id: s.id,
      name: s.name,
      model: s.model || 'classic',
      file: s.file,
      source: s.source || 'upload',
      ign: s.ign || null,
      uuid: s.uuid || null,
      addedAt: s.addedAt || null
    }))
  };
}

function getDataUrl(id) {
  const idx = readIndex();
  const entry = idx.skins.find((s) => s.id === id);
  if (!entry) {
    return { ok: false, error: 'Skin not found' };
  }
  const file = entryPath(entry);
  if (!fs.existsSync(file)) {
    return { ok: false, error: 'Skin file missing' };
  }
  const buf = fs.readFileSync(file);
  return {
    ok: true,
    id,
    model: entry.model || idx.model || 'classic',
    dataUrl: 'data:image/png;base64,' + buf.toString('base64')
  };
}

function addSkinBuffer(buf, meta) {
  const check = validateSkinPng(buf);
  if (!check.ok) {
    return check;
  }
  ensureLibrary();
  const idx = readIndex();
  const id = uuidv4();
  const safeName = String(meta.name || 'skin')
    .replace(/[^a-zA-Z0-9._-]+/g, '_')
    .slice(0, 48) || 'skin';
  const file = `${id}-${safeName}.png`;
  fs.writeFileSync(path.join(skinsDir(), file), buf);
  const entry = {
    id,
    name: meta.name || safeName,
    file,
    model: meta.model === 'slim' ? 'slim' : 'classic',
    source: meta.source || 'upload',
    ign: meta.ign || null,
    uuid: meta.uuid || null,
    addedAt: new Date().toISOString()
  };
  idx.skins.unshift(entry);
  idx.activeId = id;
  if (meta.model === 'slim' || meta.model === 'classic') {
    idx.model = meta.model;
  }
  writeIndex(idx);
  return { ok: true, entry, model: idx.model, activeId: id };
}

function importFromPath(filePath) {
  if (!filePath || !fs.existsSync(filePath)) {
    return { ok: false, error: 'File not found' };
  }
  const buf = fs.readFileSync(filePath);
  const base = path.basename(filePath, path.extname(filePath));
  return addSkinBuffer(buf, { name: base, source: 'upload' });
}

function deleteSkin(id) {
  const idx = readIndex();
  const entry = idx.skins.find((s) => s.id === id);
  if (!entry) {
    return { ok: false, error: 'Skin not found' };
  }
  try {
    const file = entryPath(entry);
    if (fs.existsSync(file)) fs.unlinkSync(file);
  } catch (_) {
    /* ignore */
  }
  idx.skins = idx.skins.filter((s) => s.id !== id);
  if (idx.activeId === id) {
    idx.activeId = idx.skins[0] ? idx.skins[0].id : null;
  }
  writeIndex(idx);
  return { ok: true, activeId: idx.activeId };
}

function setActive(id) {
  const idx = readIndex();
  if (id && !idx.skins.some((s) => s.id === id)) {
    return { ok: false, error: 'Skin not found' };
  }
  idx.activeId = id || null;
  writeIndex(idx);
  return { ok: true, activeId: idx.activeId };
}

function setModel(model) {
  const idx = readIndex();
  idx.model = model === 'slim' ? 'slim' : 'classic';
  if (idx.activeId) {
    const entry = idx.skins.find((s) => s.id === idx.activeId);
    if (entry) entry.model = idx.model;
  }
  writeIndex(idx);
  return { ok: true, model: idx.model };
}

function httpGetBuffer(url, redirects = 0) {
  return new Promise((resolve, reject) => {
    if (redirects > 5) {
      reject(new Error('Too many redirects'));
      return;
    }
    const parsed = new URL(url);
    const lib = parsed.protocol === 'https:' ? https : http;
    const req = lib.get(
      {
        hostname: parsed.hostname,
        path: parsed.pathname + parsed.search,
        headers: { 'User-Agent': 'OnyxLauncher/1.0' }
      },
      (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          const next = new URL(res.headers.location, url).toString();
          res.resume();
          httpGetBuffer(next, redirects + 1).then(resolve, reject);
          return;
        }
        const chunks = [];
        res.on('data', (c) => chunks.push(c));
        res.on('end', () => {
          const buf = Buffer.concat(chunks);
          if (res.statusCode !== 200) {
            reject(new Error(`HTTP ${res.statusCode} from ${url}`));
            return;
          }
          resolve(buf);
        });
      }
    );
    req.on('error', reject);
  });
}

function httpGetJson(url) {
  return httpGetBuffer(url).then((buf) => {
    try {
      return JSON.parse(buf.toString('utf8'));
    } catch (err) {
      throw new Error('Invalid JSON: ' + err.message);
    }
  });
}

async function detectSlimModel(uuidNoDashes) {
  try {
    const profile = await httpGetJson(
      `https://sessionserver.mojang.com/session/minecraft/profile/${uuidNoDashes}`
    );
    const texturesProp = (profile.properties || []).find((p) => p.name === 'textures');
    if (!texturesProp || !texturesProp.value) return 'classic';
    const decoded = JSON.parse(Buffer.from(texturesProp.value, 'base64').toString('utf8'));
    const model = decoded && decoded.textures && decoded.textures.SKIN && decoded.textures.SKIN.metadata
      ? decoded.textures.SKIN.metadata.model
      : null;
    return model === 'slim' ? 'slim' : 'classic';
  } catch (_) {
    return 'classic';
  }
}

async function copyFromUsername(rawIgn) {
  const ign = String(rawIgn || '').trim();
  if (ign.length < 3 || ign.length > 16 || !/^[A-Za-z0-9_]+$/.test(ign)) {
    return { ok: false, error: 'Enter a valid Minecraft username' };
  }
  try {
    profile = await httpGetJson(
      `https://api.mojang.com/users/profiles/minecraft/${encodeURIComponent(ign)}`
    );
  } catch (err) {
    return { ok: false, error: 'Player not found' };
  }
  if (!profile || !profile.id) {
    return { ok: false, error: 'Player not found' };
  }
  const uuid = String(profile.id).replace(/-/g, '');
  const name = profile.name || ign;
  let buf;
  try {
    buf = await httpGetBuffer(`https://mc-heads.net/skin/${uuid}`);
  } catch (_) {
    try {
      buf = await httpGetBuffer(`https://crafatar.com/skins/${uuid}`);
    } catch (err) {
      return { ok: false, error: 'Could not download skin' };
    }
  }
  const model = await detectSlimModel(uuid);
  return addSkinBuffer(buf, {
    name: name,
    source: 'username',
    ign: name,
    uuid,
    model
  });
}

function multipartBody(fields, fileField, fileName, fileBuf) {
  const boundary = '----OnyxSkin' + Date.now();
  const parts = [];
  Object.keys(fields).forEach((key) => {
    parts.push(
      Buffer.from(
        `--${boundary}\r\nContent-Disposition: form-data; name="${key}"\r\n\r\n${fields[key]}\r\n`
      )
    );
  });
  parts.push(
    Buffer.from(
      `--${boundary}\r\nContent-Disposition: form-data; name="${fileField}"; filename="${fileName}"\r\nContent-Type: image/png\r\n\r\n`
    )
  );
  parts.push(fileBuf);
  parts.push(Buffer.from(`\r\n--${boundary}--\r\n`));
  return { boundary, body: Buffer.concat(parts) };
}

function httpsRequestBuffer(url, options, bodyBuf) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const req = https.request(
      {
        hostname: parsed.hostname,
        path: parsed.pathname + parsed.search,
        method: options.method || 'GET',
        headers: options.headers || {}
      },
      (res) => {
        const chunks = [];
        res.on('data', (c) => chunks.push(c));
        res.on('end', () => {
          resolve({ status: res.statusCode, body: Buffer.concat(chunks) });
        });
      }
    );
    req.on('error', reject);
    if (bodyBuf) req.write(bodyBuf);
    req.end();
  });
}

async function applyActiveToAccount() {
  const idx = readIndex();
  if (!idx.activeId) {
    return { ok: false, error: 'Select a skin first' };
  }
  const entry = idx.skins.find((s) => s.id === idx.activeId);
  if (!entry) {
    return { ok: false, error: 'Skin not found' };
  }
  const file = entryPath(entry);
  if (!fs.existsSync(file)) {
    return { ok: false, error: 'Skin file missing' };
  }
  const creds = await microsoftAuth.getLaunchCredentials();
  if (!creds.ok || !creds.accessToken) {
    return {
      ok: false,
      error: creds.reason === 'none' ? 'Sign in with Microsoft to apply skins' : 'Could not refresh Microsoft session'
    };
  }
  const png = fs.readFileSync(file);
  const variant = (entry.model || idx.model) === 'slim' ? 'slim' : 'classic';
  const { boundary, body } = multipartBody({ variant }, 'file', 'skin.png', png);
  const res = await httpsRequestBuffer(
    'https://api.minecraftservices.com/minecraft/profile/skins',
    {
      method: 'POST',
      headers: {
        Authorization: 'Bearer ' + creds.accessToken,
        'Content-Type': 'multipart/form-data; boundary=' + boundary,
        'Content-Length': body.length
      }
    },
    body
  );
  if (res.status < 200 || res.status >= 300) {
    let detail = `HTTP ${res.status}`;
    try {
      const j = JSON.parse(res.body.toString('utf8'));
      if (j.errorMessage) detail = j.errorMessage;
      else if (j.error) detail = j.error;
    } catch (_) {
      /* ignore */
    }
    return { ok: false, error: detail };
  }
  return { ok: true, username: creds.username, variant };
}

async function resetAccountSkin() {
  const creds = await microsoftAuth.getLaunchCredentials();
  if (!creds.ok || !creds.accessToken) {
    return {
      ok: false,
      error: creds.reason === 'none' ? 'Sign in with Microsoft to reset skins' : 'Could not refresh Microsoft session'
    };
  }
  const res = await httpsRequestBuffer(
    'https://api.minecraftservices.com/minecraft/profile/skins/active',
    {
      method: 'DELETE',
      headers: {
        Authorization: 'Bearer ' + creds.accessToken
      }
    },
    null
  );
  if (res.status < 200 || res.status >= 300) {
    let detail = `HTTP ${res.status}`;
    try {
      const j = JSON.parse(res.body.toString('utf8'));
      if (j.errorMessage) detail = j.errorMessage;
    } catch (_) {
      /* ignore */
    }
    return { ok: false, error: detail };
  }
  const idx = readIndex();
  idx.activeId = null;
  writeIndex(idx);
  return { ok: true };
}

module.exports = {
  skinsDir,
  list,
  getDataUrl,
  importFromPath,
  addSkinBuffer,
  deleteSkin,
  setActive,
  setModel,
  copyFromUsername,
  applyActiveToAccount,
  resetAccountSkin,
  validateSkinPng
};
