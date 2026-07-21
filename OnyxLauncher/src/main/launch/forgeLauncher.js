const fs = require('fs');
const path = require('path');
const os = require('os');
const crypto = require('crypto');
const { spawn, execFileSync } = require('child_process');
const { buildJvmFlags } = require('./jvmFlags');

const FORGE_VERSION_ID = '1.8.9-Forge11.15.1.2318-1.8.9';
const VANILLA_VERSION = '1.8.9';
const ASSET_INDEX = '1.8';

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

function forgeVersionDir(mcRoot) {
  return path.join(mcRoot, 'versions', FORGE_VERSION_ID);
}

function forgeVersionJsonPath(mcRoot) {
  return path.join(forgeVersionDir(mcRoot), `${FORGE_VERSION_ID}.json`);
}

function hasForgeInstall(mcRoot) {
  if (!mcRoot || !fs.existsSync(mcRoot)) {
    return false;
  }
  const json = forgeVersionJsonPath(mcRoot);
  const vanilla = path.join(mcRoot, 'versions', VANILLA_VERSION, `${VANILLA_VERSION}.jar`);
  const forgeJar =
    path.join(forgeVersionDir(mcRoot), `${FORGE_VERSION_ID}.jar`);
  const forgeAlt = path.join(forgeVersionDir(mcRoot), `forge-1.8.9-11.15.1.2318-1.8.9.jar`);
  return fs.existsSync(json) && fs.existsSync(vanilla) && (fs.existsSync(forgeJar) || fs.existsSync(forgeAlt));
}

/**
 * Prefer a working Forge install: explicit root → standard .minecraft → gameDir.
 */
function resolveMinecraftRoot(preferredGameDir) {
  const candidates = [
    preferredGameDir,
    defaultMinecraftDir()
  ].filter(Boolean);

  for (const candidate of candidates) {
    if (hasForgeInstall(candidate)) {
      return candidate;
    }
  }
  return preferredGameDir || defaultMinecraftDir();
}

function mojangOsName() {
  if (process.platform === 'darwin') return 'osx';
  if (process.platform === 'win32') return 'windows';
  return 'linux';
}

function libraryAllowed(lib) {
  const rules = lib.rules;
  if (!rules || !rules.length) {
    return true;
  }
  let allowed = false;
  for (const rule of rules) {
    const actionAllow = rule.action === 'allow';
    if (!rule.os) {
      allowed = actionAllow;
      continue;
    }
    if (rule.os.name === mojangOsName()) {
      allowed = actionAllow;
    }
  }
  return allowed;
}

function mavenPathFromName(name) {
  const parts = name.split(':');
  if (parts.length < 3) {
    return null;
  }
  const [group, artifact, version] = parts;
  return path.join(
    group.replace(/\./g, '/'),
    artifact,
    version,
    `${artifact}-${version}.jar`
  );
}

function resolveLibraryJar(mcRoot, lib) {
  const artifact = lib.downloads && lib.downloads.artifact;
  if (artifact && artifact.path) {
    const p = path.join(mcRoot, 'libraries', artifact.path);
    if (fs.existsSync(p)) {
      return p;
    }
  }
  if (lib.name) {
    const rel = mavenPathFromName(lib.name);
    if (rel) {
      const p = path.join(mcRoot, 'libraries', rel);
      if (fs.existsSync(p)) {
        return p;
      }
    }
  }
  return null;
}

function findForgeJar(mcRoot) {
  const dir = forgeVersionDir(mcRoot);
  const candidates = [
    path.join(dir, `${FORGE_VERSION_ID}.jar`),
    path.join(dir, 'forge-1.8.9-11.15.1.2318-1.8.9.jar')
  ];
  for (const c of candidates) {
    if (fs.existsSync(c)) {
      return c;
    }
  }
  const libForge = path.join(
    mcRoot,
    'libraries',
    'net',
    'minecraftforge',
    'forge',
    '1.8.9-11.15.1.2318-1.8.9',
    'forge-1.8.9-11.15.1.2318-1.8.9.jar'
  );
  return fs.existsSync(libForge) ? libForge : null;
}

function buildClasspath(mcRoot) {
  const jsonPath = forgeVersionJsonPath(mcRoot);
  if (!fs.existsSync(jsonPath)) {
    throw new Error(`Forge version JSON missing:\n${jsonPath}`);
  }
  const version = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
  const jars = [];
  const seen = new Set();

  const add = (jarPath) => {
    if (!jarPath || !fs.existsSync(jarPath)) {
      return;
    }
    const key = path.resolve(jarPath);
    if (seen.has(key)) {
      return;
    }
    seen.add(key);
    jars.push(key);
  };

  // Forge + vanilla client first (order matters for some class lookups)
  add(findForgeJar(mcRoot));
  add(path.join(mcRoot, 'versions', VANILLA_VERSION, `${VANILLA_VERSION}.jar`));

  for (const lib of version.libraries || []) {
    if (!libraryAllowed(lib)) {
      continue;
    }
    // Native classifier jars are not on the main classpath
    if (lib.natives) {
      continue;
    }
    add(resolveLibraryJar(mcRoot, lib));
  }

  // OptiFine as a jarmod on the classpath (also lives in mods/)
  const modsDir = path.join(mcRoot, 'mods');
  if (fs.existsSync(modsDir)) {
    for (const name of fs.readdirSync(modsDir)) {
      if (/^optifine.*\.jar$/i.test(name)) {
        add(path.join(modsDir, name));
      }
    }
  }

  if (jars.length < 10) {
    throw new Error(
      `Forge classpath looks incomplete (${jars.length} jars). Is Forge 1.8.9 installed in:\n${mcRoot}`
    );
  }

  return {
    jars,
    mainClass: version.mainClass || 'net.minecraft.launchwrapper.Launch',
    minecraftArguments:
      version.minecraftArguments ||
      '--username ${auth_player_name} --version ${version_name} --gameDir ${game_dir} --assetsDir ${assets_root} --assetIndex ${assets_index_name} --uuid ${auth_uuid} --accessToken ${auth_access_token} --userProperties ${user_properties} --userType ${user_type} --tweakClass net.minecraftforge.fml.common.launcher.FMLTweaker'
  };
}

function nativesDir(mcRoot) {
  const candidates = [
    path.join(mcRoot, 'versions', VANILLA_VERSION, `natives-${mojangOsName()}`),
    path.join(mcRoot, 'versions', FORGE_VERSION_ID, 'natives'),
    path.join(mcRoot, 'versions', VANILLA_VERSION, 'natives'),
    path.join(mcRoot, 'natives')
  ];
  for (const c of candidates) {
    if (fs.existsSync(c) && fs.readdirSync(c).length > 0) {
      return c;
    }
  }
  return ensureNativesExtracted(mcRoot);
}

function ensureNativesExtracted(mcRoot) {
  const jsonPath = forgeVersionJsonPath(mcRoot);
  const version = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
  const outDir = path.join(mcRoot, 'versions', VANILLA_VERSION, `natives-${mojangOsName()}`);
  fs.mkdirSync(outDir, { recursive: true });

  const classifierKey = `natives-${mojangOsName()}`;
  for (const lib of version.libraries || []) {
    if (!libraryAllowed(lib) || !lib.natives) {
      continue;
    }
    let classifier = lib.natives[mojangOsName()];
    if (!classifier) {
      continue;
    }
    classifier = classifier.replace('${arch}', process.arch === 'x64' || process.arch === 'arm64' ? '64' : '32');
    const classifiers = (lib.downloads && lib.downloads.classifiers) || {};
    const info = classifiers[classifier];
    let nativeJar = null;
    if (info && info.path) {
      nativeJar = path.join(mcRoot, 'libraries', info.path);
    } else if (lib.name) {
      const parts = lib.name.split(':');
      if (parts.length >= 3) {
        const [group, artifact, ver] = parts;
        nativeJar = path.join(
          mcRoot,
          'libraries',
          group.replace(/\./g, '/'),
          artifact,
          ver,
          `${artifact}-${ver}-${classifier}.jar`
        );
      }
    }
    if (!nativeJar || !fs.existsSync(nativeJar)) {
      continue;
    }
    try {
      execFileSync('unzip', ['-o', '-q', nativeJar, '-d', outDir], { stdio: 'ignore' });
    } catch (err) {
      console.warn('[forge] Failed to extract natives from', nativeJar, err.message);
    }
  }

  // Strip META-INF junk
  const meta = path.join(outDir, 'META-INF');
  if (fs.existsSync(meta)) {
    try {
      fs.rmSync(meta, { recursive: true, force: true });
    } catch (_) {
      /* ignore */
    }
  }

  if (!fs.existsSync(outDir) || fs.readdirSync(outDir).length === 0) {
    throw new Error(
      `Native libraries missing. Expected natives under:\n${outDir}\nReinstall Forge 1.8.9 or run the game once from the Mojang launcher.`
    );
  }
  return outDir;
}

function substituteArgs(template, vars) {
  return template.split(' ').map((token) => {
    return token.replace(/\$\{([^}]+)\}/g, (_, key) => {
      if (Object.prototype.hasOwnProperty.call(vars, key)) {
        return String(vars[key]);
      }
      return '';
    });
  }).filter((t) => t.length > 0);
}

/**
 * SHA-256 hex digest of a file. Returns null on failure.
 */
function sha256File(filePath) {
  try {
    const hash = crypto.createHash('sha256');
    hash.update(fs.readFileSync(filePath));
    return hash.digest('hex');
  } catch (err) {
    console.error(`[OnyxLauncher] Failed to hash ${filePath}: ${err.message}`);
    return null;
  }
}

/**
 * True when dest is missing, empty, older, or a different SHA-256 than source.
 */
function needsClientJarSync(source, dest) {
  if (!fs.existsSync(dest)) {
    return true;
  }
  let srcStat;
  let destStat;
  try {
    srcStat = fs.statSync(source);
    destStat = fs.statSync(dest);
  } catch (err) {
    console.error(`[OnyxLauncher] Failed to stat jars for sync: ${err.message}`);
    return true;
  }
  if (!destStat.isFile() || destStat.size <= 0) {
    return true;
  }
  if (srcStat.size !== destStat.size || srcStat.mtimeMs > destStat.mtimeMs) {
    return true;
  }
  const srcHash = sha256File(source);
  const destHash = sha256File(dest);
  if (!srcHash || !destHash || srcHash !== destHash) {
    return true;
  }
  return false;
}

/**
 * Copy newest OnyxClient jar into mods/ with size + sha256 integrity.
 * Logs a clear error and returns null if the copy fails.
 */
function ensureClientMod(mcRoot, clientJarSource) {
  const modsDir = path.join(mcRoot, 'mods');
  fs.mkdirSync(modsDir, { recursive: true });

  if (clientJarSource && fs.existsSync(clientJarSource)) {
    let srcStat;
    try {
      srcStat = fs.statSync(clientJarSource);
    } catch (err) {
      console.error(
        `[OnyxLauncher] Cannot read client jar source ${clientJarSource}: ${err.message}`
      );
      return null;
    }
    if (!srcStat.isFile() || srcStat.size <= 0) {
      console.error(
        `[OnyxLauncher] Client jar source is empty or invalid: ${clientJarSource} (size=${srcStat.size})`
      );
      return null;
    }

    const dest = path.join(modsDir, path.basename(clientJarSource));
    if (!needsClientJarSync(clientJarSource, dest)) {
      console.log(`[OnyxLauncher] Client jar up to date: ${dest}`);
      return dest;
    }

    try {
      // Remove stale differently-named OnyxClient jars so Forge loads one copy
      for (const name of fs.readdirSync(modsDir)) {
        if (/^onyxclient.*\.jar$/i.test(name) && name !== path.basename(clientJarSource)) {
          try {
            fs.unlinkSync(path.join(modsDir, name));
          } catch (unlinkErr) {
            console.error(
              `[OnyxLauncher] Failed to remove stale mod ${name}: ${unlinkErr.message}`
            );
          }
        }
      }
      fs.copyFileSync(clientJarSource, dest);
      const copied = fs.statSync(dest);
      if (!copied.isFile() || copied.size <= 0) {
        console.error(
          `[OnyxLauncher] Client jar copy produced empty file: ${dest}`
        );
        return null;
      }
      if (copied.size !== srcStat.size) {
        console.error(
          `[OnyxLauncher] Client jar size mismatch after copy: expected ${srcStat.size}, got ${copied.size} (${dest})`
        );
        return null;
      }
      const srcHash = sha256File(clientJarSource);
      const destHash = sha256File(dest);
      if (srcHash && destHash && srcHash !== destHash) {
        console.error(
          `[OnyxLauncher] Client jar SHA-256 mismatch after copy:\n  src ${srcHash}\n  dst ${destHash}`
        );
        return null;
      }
      console.log(
        `[OnyxLauncher] Synced client jar → ${dest} (${copied.size} bytes${srcHash ? `, sha256=${srcHash.slice(0, 12)}…` : ''})`
      );
      return dest;
    } catch (err) {
      console.error(
        `[OnyxLauncher] Failed to copy client jar from ${clientJarSource} to ${dest}: ${err.message}`
      );
      return null;
    }
  }

  // Already installed?
  const existing = fs.readdirSync(modsDir).find((n) => /^onyxclient.*\.jar$/i.test(n));
  if (existing) {
    const existingPath = path.join(modsDir, existing);
    try {
      const st = fs.statSync(existingPath);
      if (st.size <= 0) {
        console.error(`[OnyxLauncher] Existing client jar is empty: ${existingPath}`);
        return null;
      }
    } catch (err) {
      console.error(`[OnyxLauncher] Cannot read existing client jar: ${err.message}`);
      return null;
    }
    return existingPath;
  }
  return null;
}

function ensureOptiFine(gameDir, mcRoot) {
  const destDir = path.join(gameDir, 'mods');
  fs.mkdirSync(destDir, { recursive: true });
  const hasLocal = fs.readdirSync(destDir).some((n) => /^optifine.*\.jar$/i.test(n));
  if (hasLocal) {
    return;
  }
  const srcDir = path.join(mcRoot, 'mods');
  if (!fs.existsSync(srcDir)) {
    return;
  }
  const src = fs.readdirSync(srcDir).find((n) => /^optifine.*\.jar$/i.test(n));
  if (src) {
    fs.copyFileSync(path.join(srcDir, src), path.join(destDir, src));
  }
}

/**
 * Collect OnyxClient jars and return existing paths newest-first.
 * Tie-break: prefer ../build/libs over bundled resources so a fresh
 * gradle build is never overwritten by a stale launcher resource.
 */
function findClientJarCandidates(appPath) {
  const seen = new Set();
  const raw = [];
  const add = (p) => {
    const resolved = path.resolve(p);
    if (seen.has(resolved)) {
      return;
    }
    seen.add(resolved);
    raw.push(p);
  };

  // Dev: sibling client build output
  try {
    const libsDir = path.join(appPath, '..', 'build', 'libs');
    if (fs.existsSync(libsDir)) {
      for (const name of fs.readdirSync(libsDir)) {
        if (/^OnyxClient.*\.jar$/i.test(name) && !name.endsWith('-sources.jar')) {
          add(path.join(libsDir, name));
        }
      }
    }
  } catch (_) {
    /* ignore */
  }
  add(path.join(appPath, '..', 'build', 'libs', 'OnyxClient-1.8.9-v1.0.jar'));
  // Bundled with launcher (may lag behind a local build)
  add(path.join(appPath, 'resources', 'OnyxClient-1.8.9-v1.0.jar'));

  const existing = raw.filter((p) => {
    try {
      if (!fs.existsSync(p)) {
        return false;
      }
      const st = fs.statSync(p);
      if (!st.isFile() || st.size <= 0) {
        console.error(`[OnyxLauncher] Skipping empty/invalid client jar candidate: ${p}`);
        return false;
      }
      return true;
    } catch (err) {
      console.error(`[OnyxLauncher] Skipping unreadable client jar candidate ${p}: ${err.message}`);
      return false;
    }
  });
  existing.sort((a, b) => {
    let ma = 0;
    let mb = 0;
    try {
      ma = fs.statSync(a).mtimeMs;
    } catch (_) {
      /* ignore */
    }
    try {
      mb = fs.statSync(b).mtimeMs;
    } catch (_) {
      /* ignore */
    }
    if (mb !== ma) {
      return mb - ma;
    }
    const aLibs = a.replace(/\\/g, '/').includes('/build/libs/') ? 0 : 1;
    const bLibs = b.replace(/\\/g, '/').includes('/build/libs/') ? 0 : 1;
    return aLibs - bLibs;
  });
  return existing;
}

/**
 * @param {object} opts
 * @param {string} opts.javaPath
 * @param {string} opts.gameDir - saves/configs/mods dir passed to --gameDir
 * @param {string} [opts.minecraftRoot] - libraries/versions/assets root
 * @param {number} opts.ramGb
 * @param {object} opts.session - { username, uuid, accessToken, userType }
 * @param {string} [opts.appPath]
 * @param {(p: object) => void} onProgress
 */
async function launchForge(opts, onProgress) {
  const progress = onProgress || (() => {});
  const gameDir = opts.gameDir;
  const mcRoot = resolveMinecraftRoot(opts.minecraftRoot || gameDir);

  progress({ stage: 'checking', message: 'Locating Forge 1.8.9…', percent: 15 });
  if (!hasForgeInstall(mcRoot)) {
    return {
      ok: false,
      error:
        `Forge 1.8.9 not found in:\n${mcRoot}\n\nInstall Forge 1.8.9-11.15.1.2318 (or play once with the Mojang launcher + Forge), then try again.`
    };
  }

  progress({ stage: 'client', message: 'Installing Onyx Client mod…', percent: 30 });
  const appPath = opts.appPath || process.cwd();
  // Newest existing jar first (see findClientJarCandidates)
  const clientSource = findClientJarCandidates(appPath)[0] || null;
  if (clientSource) {
    console.log(`[OnyxLauncher] Using newest client jar source: ${clientSource}`);
  }
  // Prefer copying into the actual --gameDir mods folder
  const installed = ensureClientMod(gameDir, clientSource);
  if (clientSource && !installed) {
    return {
      ok: false,
      error:
        `Failed to sync OnyxClient jar into mods/.\nSource: ${clientSource}\nCheck launcher console for integrity/copy errors.`
    };
  }
  // Also ensure mcRoot mods has it when gameDir !== mcRoot
  if (path.resolve(gameDir) !== path.resolve(mcRoot)) {
    const rootInstalled = ensureClientMod(mcRoot, clientSource || installed);
    if ((clientSource || installed) && !rootInstalled) {
      console.error(
        `[OnyxLauncher] Warning: failed to sync client jar into minecraft root mods (${mcRoot})`
      );
    }
    ensureOptiFine(gameDir, mcRoot);
  }
  if (!installed && !clientSource) {
    // Soft warn — user may already have jar only in one place
    const modsCheck = path.join(gameDir, 'mods');
    const has =
      fs.existsSync(modsCheck) &&
      fs.readdirSync(modsCheck).some((n) => /^onyxclient.*\.jar$/i.test(n));
    if (!has) {
      return {
        ok: false,
        error:
          'OnyxClient jar not found. Build the client (`./gradlew build`) or place OnyxClient-1.8.9-v1.0.jar in the mods folder.'
      };
    }
  }

  progress({ stage: 'classpath', message: 'Building classpath…', percent: 45 });
  const { jars, mainClass, minecraftArguments } = buildClasspath(mcRoot);
  const natives = nativesDir(mcRoot);

  const assetsDir = path.join(mcRoot, 'assets');
  fs.mkdirSync(gameDir, { recursive: true });
  fs.mkdirSync(path.join(gameDir, 'mods'), { recursive: true });

  const session = opts.session || {};
  const username = session.username || 'Player';
  const uuid = (session.uuid || '00000000000000000000000000000000').replace(/-/g, '');
  const accessToken = session.accessToken || '0';
  const userType = session.userType || (session.guest ? 'legacy' : 'msa');

  const gameArgs = substituteArgs(minecraftArguments, {
    auth_player_name: username,
    version_name: FORGE_VERSION_ID,
    game_dir: gameDir,
    assets_root: assetsDir,
    assets_index_name: ASSET_INDEX,
    auth_uuid: uuid,
    auth_access_token: accessToken,
    user_properties: '{}',
    user_type: userType
  });

  const cpSep = process.platform === 'win32' ? ';' : ':';
  const classpath = jars.join(cpSep);
  const jvmFlags = buildJvmFlags(opts.ramGb || 4);
  const javaArgs = [
    ...jvmFlags,
    `-Djava.library.path=${natives}`,
    `-Dorg.lwjgl.librarypath=${natives}`,
    '-cp',
    classpath,
    mainClass,
    ...gameArgs
  ];

  progress({ stage: 'starting', message: 'Starting Minecraft…', percent: 75 });

  const logPath = path.join(gameDir, 'onyx-launcher-latest.log');
  let logStream;
  try {
    logStream = fs.openSync(logPath, 'w');
  } catch (_) {
    logStream = 'ignore';
  }

  return new Promise((resolve) => {
    let child;
    try {
      child = spawn(opts.javaPath, javaArgs, {
        cwd: gameDir,
        env: { ...process.env },
        detached: true,
        stdio: logStream === 'ignore' ? 'ignore' : ['ignore', logStream, logStream]
      });
    } catch (err) {
      resolve({
        ok: false,
        error: `Failed to start Java (${opts.javaPath}): ${err.message}`
      });
      return;
    }

    child.on('error', (err) => {
      resolve({
        ok: false,
        error: `Java process error: ${err.message}`
      });
    });

    // If the process exits within 2s, treat as launch failure and surface log tail
    const earlyTimer = setTimeout(() => {
      child.unref();
      progress({ stage: 'launched', message: 'Game launched', percent: 100 });
      resolve({
        ok: true,
        pid: child.pid,
        javaPath: opts.javaPath,
        mcRoot,
        gameDir,
        logPath,
        clientJar: installed || clientSource
      });
    }, 2000);

    child.once('exit', (code) => {
      clearTimeout(earlyTimer);
      if (code && code !== 0) {
        let tail = '';
        try {
          const text = fs.readFileSync(logPath, 'utf8');
          tail = text.split('\n').slice(-12).join('\n');
        } catch (_) {
          /* ignore */
        }
        resolve({
          ok: false,
          error: `Minecraft exited early (code ${code}).${tail ? `\n\n${tail}` : `\nSee ${logPath}`}`
        });
      }
    });
  });
}

module.exports = {
  FORGE_VERSION_ID,
  VANILLA_VERSION,
  ASSET_INDEX,
  defaultMinecraftDir,
  hasForgeInstall,
  resolveMinecraftRoot,
  launchForge,
  findClientJarCandidates,
  ensureClientMod,
  ensureOptiFine,
  buildClasspath,
  nativesDir
};
