const fs = require('fs');
const path = require('path');
const os = require('os');
const { app } = require('electron');
const { buildJvmFlags } = require('./jvmFlags');
const config = require('../config/launcherConfig');
const sessionStore = require('../auth/sessionStore');
const microsoftAuth = require('../auth/microsoftAuth');
const {
  launchForge,
  resolveMinecraftRoot,
  hasForgeInstall,
  defaultMinecraftDir,
  findClientJarCandidates,
  ensureClientMod
} = require('./forgeLauncher');

function resourcesDir() {
  if (app.isPackaged) {
    return path.join(process.resourcesPath, 'resources');
  }
  return path.join(app.getAppPath(), 'resources');
}

function resolveLoaderJar(gameDir) {
  const candidates = [
    path.join(gameDir, 'OnyxLoader.jar'),
    path.join(gameDir, 'loader', 'OnyxLoader.jar'),
    path.join(resourcesDir(), 'OnyxLoader.jar')
  ];
  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }
  return null;
}

function resolveJavaPath(configured) {
  if (configured && fs.existsSync(configured)) {
    return configured;
  }

  const home = os.homedir();
  const candidates = [];

  // Bundled JRE
  candidates.push(
    path.join(
      resourcesDir(),
      'jre',
      process.platform === 'win32' ? 'bin/java.exe' : 'bin/java'
    )
  );

  // Project / sibling Zulu JDK 8 (dev)
  const appPath = app.getAppPath();
  candidates.push(
    path.join(
      appPath,
      '..',
      '.jdks',
      'zulu8.88.0.19-ca-jdk8.0.462-macosx_aarch64',
      'bin',
      'java'
    )
  );
  candidates.push(
    path.join(
      home,
      'Documents',
      'Onyx Client Beta',
      '.jdks',
      'zulu8.88.0.19-ca-jdk8.0.462-macosx_aarch64',
      'bin',
      'java'
    )
  );

  if (process.env.JAVA_HOME) {
    candidates.push(
      path.join(
        process.env.JAVA_HOME,
        'bin',
        process.platform === 'win32' ? 'java.exe' : 'java'
      )
    );
  }

  // macOS java_home for 1.8
  if (process.platform === 'darwin') {
    try {
      const { execFileSync } = require('child_process');
      const jhome = execFileSync('/usr/libexec/java_home', ['-v', '1.8'], {
        encoding: 'utf8'
      }).trim();
      if (jhome) {
        candidates.push(path.join(jhome, 'bin', 'java'));
      }
    } catch (_) {
      /* no Java 8 via java_home */
    }
  }

  for (const candidate of candidates) {
    if (candidate && fs.existsSync(candidate)) {
      return candidate;
    }
  }

  return process.platform === 'win32' ? 'java.exe' : 'java';
}

function readPerfPreset(gameDir) {
  try {
    const settingsPath = path.join(gameDir, 'config', 'onyxclient', 'settings.json');
    if (fs.existsSync(settingsPath)) {
      const data = JSON.parse(fs.readFileSync(settingsPath, 'utf8'));
      if (typeof data.perfPreset === 'number') {
        return data.perfPreset;
      }
    }
  } catch (_) {
    /* use default */
  }
  return 0;
}

const OPTIFINE_MAX =
  'ofFastRender=true\nofFastMath=true\nofLazyChunkLoading=true\nofChunkUpdates=1\n'
  + 'ofDynamicLights=0\nofConnectedTextures=0\nofCustomSky=false\nofSmoothFps=false\n'
  + 'ofSmoothWorld=false\nofTerrainLoD=true\nofDynamicFov=false\n';

const OPTIFINE_BALANCED =
  'ofFastRender=true\nofFastMath=true\nofLazyChunkLoading=true\nofChunkUpdates=3\n'
  + 'ofDynamicLights=1\nofConnectedTextures=2\nofCustomSky=true\nofSmoothFps=false\n'
  + 'ofSmoothWorld=false\nofTerrainLoD=true\nofDynamicFov=false\n';

const OPTIFINE_QUALITY =
  'ofFastRender=false\nofFastMath=false\nofLazyChunkLoading=false\nofChunkUpdates=5\n'
  + 'ofDynamicLights=1\nofConnectedTextures=2\nofCustomSky=true\nofSmoothFps=true\n'
  + 'ofSmoothWorld=true\nofTerrainLoD=false\nofDynamicFov=false\n';

function ensureOptionsOfTxt(gameDir, preset) {
  const optionsOfPath = path.join(gameDir, 'optionsof.txt');
  try {
    const content = preset === 1 ? OPTIFINE_MAX : preset === 2 ? OPTIFINE_QUALITY : OPTIFINE_BALANCED;
    fs.writeFileSync(optionsOfPath, content, 'utf8');
  } catch (err) {
    console.error('[launch] Failed to write optionsof.txt:', err.message);
  }
}

function ensureOptionsTxt(gameDir) {
  const optionsPath = path.join(gameDir, 'options.txt');
  const preset = readPerfPreset(gameDir);
  const maxFps = '0';
  const renderDistance = preset === 1 ? '4' : preset === 2 ? '12' : '6';
  const maxPreset = preset === 1;
  try {
    fs.mkdirSync(gameDir, { recursive: true });
    let text = '';
    if (fs.existsSync(optionsPath)) {
      text = fs.readFileSync(optionsPath, 'utf8');
    }
    const ensure = (key, value) => {
      const re = new RegExp(`^${key}:.*$`, 'm');
      if (re.test(text)) {
        text = text.replace(re, `${key}:${value}`);
      } else {
        text += `\n${key}:${value}`;
      }
    };
    ensure('enableVsync', 'false');
    ensure('maxFps', maxFps);
    ensure('renderDistance', renderDistance);
    if (maxPreset) {
      ensure('fancyGraphics', 'false');
      ensure('ao', '0');
      ensure('entityShadows', 'false');
      ensure('particles', '0');
    }
    fs.writeFileSync(optionsPath, text.trim() + '\n', 'utf8');
    ensureOptionsOfTxt(gameDir, preset);
  } catch (err) {
    console.error('[launch] Failed to write options.txt:', err.message);
  }
}

async function resolveLaunchSession() {
  const session = await sessionStore.getActiveSession();
  if (!session) {
    return {
      ok: false,
      error: 'Not signed in. Open the launcher and sign in (Microsoft or Guest) first.'
    };
  }

  if (session.guest || session.type === 'guest') {
    return {
      ok: true,
      session: {
        username: session.username,
        uuid: session.uuid,
        accessToken: '0',
        userType: 'legacy',
        guest: true
      }
    };
  }

  // Microsoft — refresh to get a live Minecraft access token
  try {
    const creds = await microsoftAuth.getLaunchCredentials();
    if (creds && creds.ok) {
      return {
        ok: true,
        session: {
          username: creds.username,
          uuid: creds.uuid,
          accessToken: creds.accessToken,
          userType: 'msa',
          guest: false
        }
      };
    }
  } catch (err) {
    console.warn('[launch] MS token refresh failed, using offline fallback:', err.message);
  }

  // Fallback offline-style launch with saved profile name (singleplayer still works)
  return {
    ok: true,
    session: {
      username: session.username,
      uuid: session.uuid || '00000000000000000000000000000000',
      accessToken: '0',
      userType: 'msa',
      guest: false
    }
  };
}

/**
 * @param {object} opts
 * @param {(payload: object) => void} onProgress
 */
async function launchGame(opts, onProgress) {
  const cfg = config.load();
  const gameDir = (opts && opts.gameDir) || cfg.gameDir;
  const ramGb = (opts && opts.ramGb) || cfg.ramGb || 4;
  const javaPath = resolveJavaPath((opts && opts.javaPath) || cfg.javaPath);
  const appPath = app.getAppPath();

  onProgress({ stage: 'session', message: 'Preparing account…', percent: 5 });
  const auth = await resolveLaunchSession();
  if (!auth.ok) {
    return auth;
  }

  onProgress({ stage: 'java', message: `Using Java: ${javaPath}`, percent: 10 });

  // Sync newest client jar into gameDir/mods from build or resources (size/sha256 checked)
  const clientCandidates = findClientJarCandidates(appPath);
  if (clientCandidates.length > 0) {
    const synced = ensureClientMod(gameDir, clientCandidates[0]);
    if (!synced) {
      console.error(
        `[OnyxLauncher] Pre-launch jar sync failed for ${clientCandidates[0]} — Forge launch will retry`
      );
    }
  }

  ensureOptionsTxt(gameDir);

  const mcRoot = resolveMinecraftRoot(gameDir);
  if (hasForgeInstall(mcRoot)) {
    onProgress({ stage: 'forge', message: 'Launching Forge 1.8.9…', percent: 20 });
    return launchForge(
      {
        javaPath,
        gameDir,
        minecraftRoot: mcRoot,
        ramGb,
        session: auth.session,
        appPath,
        serverIp: cfg.serverIp || config.DEFAULTS.serverIp
      },
      onProgress
    );
  }

  // Fallback: OnyxLoader bootstrap (only works if it can find a full classpath)
  onProgress({ stage: 'checking', message: 'Forge missing — trying OnyxLoader…', percent: 25 });
  const loaderJar = resolveLoaderJar(gameDir);
  if (!loaderJar) {
    return {
      ok: false,
      error:
        `Forge 1.8.9 not found.\n\nInstall Forge 1.8.9-11.15.1.2318 into:\n${defaultMinecraftDir()}\n\nor set Game Directory in Settings to your Minecraft folder that already has Forge.`
    };
  }

  const flags = buildJvmFlags(ramGb);
  const args = [...flags, '-jar', loaderJar, '--gameDir', gameDir];
  const serverIp = (cfg.serverIp || config.DEFAULTS.serverIp || '').trim();
  if (serverIp) {
    const host = serverIp.includes(':') ? serverIp.split(':')[0] : serverIp;
    const port = serverIp.includes(':') ? serverIp.split(':')[1] : '25565';
    args.push('--server', host, '--port', String(port || '25565'));
  }

  return new Promise((resolve) => {
    const { spawn } = require('child_process');
    let child;
    try {
      child = spawn(javaPath, args, {
        cwd: gameDir,
        env: { ...process.env },
        detached: true,
        stdio: 'ignore'
      });
    } catch (err) {
      resolve({
        ok: false,
        error: `Failed to start Java (${javaPath}): ${err.message}`
      });
      return;
    }
    child.on('error', (err) => {
      resolve({
        ok: false,
        error: `Java process error: ${err.message}`
      });
    });
    child.unref();
    onProgress({ stage: 'launched', message: 'Game launched', percent: 100 });
    resolve({ ok: true, pid: child.pid, loaderJar, javaPath, args });
  });
}

module.exports = {
  launchGame,
  resolveLoaderJar,
  resolveJavaPath,
  resourcesDir,
  defaultMinecraftDir,
  hasForgeInstall,
  resolveMinecraftRoot
};
