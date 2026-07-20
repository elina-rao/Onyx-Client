const { BrowserView } = require('electron');
const https = require('https');
const http = require('http');
const { URL } = require('url');
const sessionStore = require('./sessionStore');

/**
 * Microsoft / Xbox Live / Minecraft OAuth via embedded BrowserView.
 *
 * Uses the Onyx Client Azure app registration by default.
 * Override with ONYX_MS_CLIENT_ID if needed.
 */
const CLIENT_ID =
  process.env.ONYX_MS_CLIENT_ID || 'c7f954e5-3103-4f4b-89fd-24f03c746879';
const REDIRECT_URI = 'https://login.microsoftonline.com/common/oauth2/nativeclient';
const SCOPES = 'XboxLive.signin offline_access';
const AUTHORIZE_URL =
  'https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize';
const TOKEN_URL = 'https://login.microsoftonline.com/consumers/oauth2/v2.0/token';

let activeView = null;

function requestJson(url, options = {}, body = null) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const lib = parsed.protocol === 'https:' ? https : http;
    const req = lib.request(
      {
        hostname: parsed.hostname,
        path: parsed.pathname + parsed.search,
        method: options.method || 'GET',
        headers: options.headers || {}
      },
      (res) => {
        let data = '';
        res.on('data', (chunk) => {
          data += chunk;
        });
        res.on('end', () => {
          try {
            resolve({ status: res.statusCode, body: data ? JSON.parse(data) : {} });
          } catch (err) {
            reject(new Error(`Invalid JSON from ${url}: ${err.message}`));
          }
        });
      }
    );
    req.on('error', reject);
    if (body) {
      req.write(body);
    }
    req.end();
  });
}

function postForm(url, form) {
  const body = new URLSearchParams(form).toString();
  return requestJson(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': Buffer.byteLength(body)
    }
  }, body);
}

function postJson(url, payload, headers = {}) {
  const body = JSON.stringify(payload);
  return requestJson(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(body),
      ...headers
    }
  }, body);
}

async function exchangeCodeForTokens(code) {
  const res = await postForm(TOKEN_URL, {
    client_id: CLIENT_ID,
    code,
    grant_type: 'authorization_code',
    redirect_uri: REDIRECT_URI,
    scope: SCOPES
  });
  if (res.status >= 400 || !res.body.access_token) {
    throw new Error(res.body.error_description || res.body.error || 'Token exchange failed');
  }
  return res.body;
}

async function refreshAccessToken(refreshToken) {
  const res = await postForm(TOKEN_URL, {
    client_id: CLIENT_ID,
    refresh_token: refreshToken,
    grant_type: 'refresh_token',
    redirect_uri: REDIRECT_URI,
    scope: SCOPES
  });
  if (res.status >= 400 || !res.body.access_token) {
    throw new Error(res.body.error_description || res.body.error || 'Token refresh failed');
  }
  return res.body;
}

async function authenticateXbox(msAccessToken) {
  const res = await postJson('https://user.auth.xboxlive.com/user/authenticate', {
    Properties: {
      AuthMethod: 'RPS',
      SiteName: 'user.auth.xboxlive.com',
      RpsTicket: `d=${msAccessToken}`
    },
    RelyingParty: 'http://auth.xboxlive.com',
    TokenType: 'JWT'
  });
  if (!res.body.Token) {
    throw new Error('Xbox Live authentication failed');
  }
  const userHash = res.body.DisplayClaims.xui[0].uhs;
  return { token: res.body.Token, userHash };
}

async function getXstsToken(xboxToken) {
  const res = await postJson('https://xsts.auth.xboxlive.com/xsts/authorize', {
    Properties: {
      SandboxId: 'RETAIL',
      UserTokens: [xboxToken]
    },
    RelyingParty: 'rp://api.minecraftservices.com/',
    TokenType: 'JWT'
  });
  if (!res.body.Token) {
    throw new Error('XSTS authorization failed');
  }
  const userHash = res.body.DisplayClaims.xui[0].uhs;
  return { token: res.body.Token, userHash };
}

async function loginMinecraft(userHash, xstsToken) {
  const res = await postJson('https://api.minecraftservices.com/authentication/login_with_xbox', {
    identityToken: `XBL3.0 x=${userHash};${xstsToken}`
  });
  if (!res.body.access_token) {
    throw new Error('Minecraft login failed');
  }
  return res.body;
}

async function fetchMinecraftProfile(mcAccessToken) {
  const res = await requestJson('https://api.minecraftservices.com/minecraft/profile', {
    headers: { Authorization: `Bearer ${mcAccessToken}` }
  });
  if (!res.body.id || !res.body.name) {
    throw new Error('Failed to fetch Minecraft profile (do you own the game?)');
  }
  return {
    uuid: res.body.id,
    username: res.body.name,
    name: res.body.name,
    avatarUrl: `https://mc-heads.net/avatar/${res.body.id}/64`
  };
}

async function completeAuthWithMsTokens(tokenResponse) {
  if (tokenResponse.refresh_token) {
    await sessionStore.setRefreshToken(tokenResponse.refresh_token);
  }
  const xbox = await authenticateXbox(tokenResponse.access_token);
  const xsts = await getXstsToken(xbox.token);
  const mc = await loginMinecraft(xsts.userHash, xsts.token);
  const profile = await fetchMinecraftProfile(mc.access_token);
  await sessionStore.setMicrosoftProfile(profile);
  sessionStore.clearGuestSession();
  return profile;
}

function destroyAuthView(parentWindow) {
  if (activeView && parentWindow && !parentWindow.isDestroyed()) {
    try {
      parentWindow.removeBrowserView(activeView);
    } catch (_) {
      /* ignore */
    }
  }
  if (activeView) {
    try {
      activeView.webContents.destroy();
    } catch (_) {
      /* ignore */
    }
  }
  activeView = null;
}

function isOAuthRedirect(parsed) {
  const host = parsed.hostname || '';
  const path = parsed.pathname || '';
  // Modern Azure native client redirect
  if (
    host === 'login.microsoftonline.com' &&
    path.includes('/oauth2/nativeclient')
  ) {
    return true;
  }
  // Legacy Live SDK redirect (kept for older sessions / fallbacks)
  if (host === 'login.live.com' && path.includes('oauth20_desktop.srf')) {
    return true;
  }
  return false;
}

/**
 * Opens an embedded BrowserView for Microsoft OAuth.
 * Resolves with Minecraft profile on success.
 */
function startMicrosoftLogin(parentWindow) {
  return new Promise((resolve, reject) => {
    destroyAuthView(parentWindow);

    const authUrl =
      AUTHORIZE_URL +
      '?' +
      new URLSearchParams({
        client_id: CLIENT_ID,
        response_type: 'code',
        redirect_uri: REDIRECT_URI,
        response_mode: 'query',
        scope: SCOPES,
        prompt: 'select_account'
      }).toString();

    const view = new BrowserView({
      webPreferences: {
        nodeIntegration: false,
        contextIsolation: true,
        partition: 'persist:onyx-ms-auth'
      }
    });
    activeView = view;

    const bounds = parentWindow.getBounds();
    const width = Math.min(480, bounds.width - 80);
    const height = Math.min(640, bounds.height - 80);
    const x = Math.floor((bounds.width - width) / 2);
    const y = Math.floor((bounds.height - height) / 2);

    parentWindow.setBrowserView(view);
    view.setBounds({ x, y, width, height });
    view.setAutoResize({ width: false, height: false });

    let settled = false;

    const finish = async (code) => {
      if (settled) return;
      settled = true;
      destroyAuthView(parentWindow);
      try {
        const tokens = await exchangeCodeForTokens(code);
        const profile = await completeAuthWithMsTokens(tokens);
        resolve(profile);
      } catch (err) {
        reject(err);
      }
    };

    const fail = (err) => {
      if (settled) return;
      settled = true;
      destroyAuthView(parentWindow);
      reject(err);
    };

    const onNavigate = (url) => {
      try {
        const parsed = new URL(url);
        if (!isOAuthRedirect(parsed)) {
          return;
        }
        const params = new URLSearchParams(parsed.search || '');
        // Some native redirects put the code in the hash fragment
        if (parsed.hash && parsed.hash.length > 1) {
          const hash = parsed.hash.replace(/^#/, '');
          const hashParams = new URLSearchParams(hash);
          hashParams.forEach((value, key) => {
            if (!params.has(key)) {
              params.set(key, value);
            }
          });
        }
        const code = params.get('code');
        const error = params.get('error');
        if (error) {
          fail(new Error(params.get('error_description') || error));
          return;
        }
        if (code) {
          finish(code);
        }
      } catch (_) {
        /* ignore parse errors */
      }
    };

    view.webContents.on('will-redirect', (event, url) => onNavigate(url));
    view.webContents.on('will-navigate', (event, url) => onNavigate(url));
    view.webContents.on('did-fail-load', (_e, code, desc) => {
      if (code === -3) return; // aborted
      fail(new Error(`Auth page failed to load: ${desc}`));
    });

    view.webContents.loadURL(authUrl).catch((err) => fail(err));
  });
}

function cancelMicrosoftLogin(parentWindow) {
  destroyAuthView(parentWindow);
}

async function trySilentRefresh() {
  const refresh = await sessionStore.getRefreshToken();
  if (!refresh) {
    return { ok: false, reason: 'none' };
  }
  try {
    const tokens = await refreshAccessToken(refresh);
    const profile = await completeAuthWithMsTokens(tokens);
    return { ok: true, profile };
  } catch (err) {
    console.error('[auth] Silent refresh failed:', err.message);
    await sessionStore.clearRefreshToken();
    return { ok: false, reason: 'expired', error: err.message };
  }
}

/**
 * Refresh Microsoft → Xbox → Minecraft and return launch credentials
 * including a live accessToken for multiplayer.
 */
async function getLaunchCredentials() {
  const refresh = await sessionStore.getRefreshToken();
  if (!refresh) {
    return { ok: false, reason: 'none' };
  }
  const tokens = await refreshAccessToken(refresh);
  if (tokens.refresh_token) {
    await sessionStore.setRefreshToken(tokens.refresh_token);
  }
  const xbox = await authenticateXbox(tokens.access_token);
  const xsts = await getXstsToken(xbox.token);
  const mc = await loginMinecraft(xsts.userHash, xsts.token);
  const profile = await fetchMinecraftProfile(mc.access_token);
  await sessionStore.setMicrosoftProfile(profile);
  return {
    ok: true,
    username: profile.username,
    uuid: profile.uuid,
    accessToken: mc.access_token
  };
}

module.exports = {
  startMicrosoftLogin,
  cancelMicrosoftLogin,
  trySilentRefresh,
  getLaunchCredentials,
  CLIENT_ID,
  REDIRECT_URI
};
