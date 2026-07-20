const keytar = require('keytar');
const { v4: uuidv4 } = require('uuid');
const config = require('../config/launcherConfig');

const SERVICE = 'OnyxLauncher';
const ACCOUNT_REFRESH = 'microsoft-refresh-token';
const ACCOUNT_PROFILE = 'microsoft-profile';

async function getRefreshToken() {
  try {
    return await keytar.getPassword(SERVICE, ACCOUNT_REFRESH);
  } catch (err) {
    console.error('[session] keytar getRefreshToken failed:', err.message);
    return null;
  }
}

async function setRefreshToken(token) {
  await keytar.setPassword(SERVICE, ACCOUNT_REFRESH, token);
}

async function clearRefreshToken() {
  try {
    await keytar.deletePassword(SERVICE, ACCOUNT_REFRESH);
  } catch (_) {
    /* ignore */
  }
  try {
    await keytar.deletePassword(SERVICE, ACCOUNT_PROFILE);
  } catch (_) {
    /* ignore */
  }
}

async function getMicrosoftProfile() {
  try {
    const raw = await keytar.getPassword(SERVICE, ACCOUNT_PROFILE);
    return raw ? JSON.parse(raw) : null;
  } catch (_) {
    return null;
  }
}

async function setMicrosoftProfile(profile) {
  await keytar.setPassword(SERVICE, ACCOUNT_PROFILE, JSON.stringify(profile));
}

function getGuestSession() {
  const cfg = config.load();
  return cfg.guest || null;
}

function setGuestSession(username) {
  const offlineUuid = uuidv4();
  const guest = {
    username: username.trim().slice(0, 16),
    uuid: offlineUuid,
    guest: true
  };
  config.save({ guest, lastUsername: guest.username });
  return guest;
}

function clearGuestSession() {
  config.save({ guest: null });
}

async function getActiveSession() {
  const profile = await getMicrosoftProfile();
  const refresh = await getRefreshToken();
  if (profile && refresh) {
    return {
      type: 'microsoft',
      username: profile.username || profile.name || 'Player',
      uuid: profile.uuid || null,
      avatarUrl: profile.avatarUrl || null,
      guest: false
    };
  }

  const guest = getGuestSession();
  if (guest) {
    return {
      type: 'guest',
      username: guest.username,
      uuid: guest.uuid,
      avatarUrl: null,
      guest: true
    };
  }

  return null;
}

async function signOut() {
  await clearRefreshToken();
  clearGuestSession();
}

module.exports = {
  SERVICE,
  getRefreshToken,
  setRefreshToken,
  clearRefreshToken,
  getMicrosoftProfile,
  setMicrosoftProfile,
  getGuestSession,
  setGuestSession,
  clearGuestSession,
  getActiveSession,
  signOut
};
