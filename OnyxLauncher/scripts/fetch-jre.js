#!/usr/bin/env node
/**
 * Fetch a portable Java 8 JRE into OnyxLauncher/resources/jre for packaging.
 * Usage:
 *   node scripts/fetch-jre.js              # host platform/arch
 *   node scripts/fetch-jre.js --platform mac --arch aarch64
 *   node scripts/fetch-jre.js --platform windows --arch x64
 *
 * Uses Eclipse Temurin 8 (Adoptium) JRE archives. Output is gitignored.
 */
const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const { execFileSync } = require('child_process');

const ROOT = path.join(__dirname, '..');
const OUT = path.join(ROOT, 'resources', 'jre');

function parseArgs() {
  const args = process.argv.slice(2);
  let platform = process.platform === 'win32' ? 'windows' : process.platform === 'darwin' ? 'mac' : 'linux';
  let arch =
    process.arch === 'arm64' ? 'aarch64' : process.arch === 'x64' ? 'x64' : process.arch;
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--platform' && args[i + 1]) platform = args[++i];
    if (args[i] === '--arch' && args[i + 1]) arch = args[++i];
  }
  return { platform, arch };
}

function download(url, dest) {
  return new Promise((resolve, reject) => {
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    const file = fs.createWriteStream(dest);
    const get = (u, n = 0) => {
      if (n > 8) return reject(new Error('Too many redirects'));
      const lib = u.startsWith('https') ? https : http;
      lib
        .get(u, (res) => {
          if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
            get(res.headers.location, n + 1);
            return;
          }
          if (res.statusCode !== 200) {
            reject(new Error(`HTTP ${res.statusCode} for ${u}`));
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

function apiJson(url) {
  return new Promise((resolve, reject) => {
    https
      .get(url, { headers: { 'User-Agent': 'OnyxLauncher-fetch-jre' } }, (res) => {
        let data = '';
        res.on('data', (c) => (data += c));
        res.on('end', () => {
          try {
            resolve(JSON.parse(data));
          } catch (e) {
            reject(e);
          }
        });
      })
      .on('error', reject);
  });
}

async function main() {
  const { platform, arch } = parseArgs();
  const api =
    `https://api.adoptium.net/v3/binary/latest/8/ga/${platform}/${arch}/jre/hotspot/normal/eclipse`;
  console.log(`[fetch-jre] Resolving Temurin 8 JRE for ${platform}/${arch}…`);

  // Prefer local Zulu on mac aarch64 if present (offline / faster)
  const localZulu = path.join(
    ROOT,
    '..',
    '.jdks',
    'zulu8.88.0.19-ca-jdk8.0.462-macosx_aarch64'
  );
  if (platform === 'mac' && arch === 'aarch64' && fs.existsSync(path.join(localZulu, 'bin', 'java'))) {
    const srcJre = fs.existsSync(path.join(localZulu, 'jre', 'bin', 'java'))
      ? path.join(localZulu, 'jre')
      : localZulu;
    console.log(`[fetch-jre] Using local Zulu at ${srcJre}`);
    fs.rmSync(OUT, { recursive: true, force: true });
    fs.mkdirSync(OUT, { recursive: true });
    execFileSync('cp', ['-R', `${srcJre}/.`, OUT]);
    console.log(`[fetch-jre] Ready: ${OUT}`);
    return;
  }

  const tmpDir = path.join(ROOT, 'dist', '.jre-tmp');
  fs.rmSync(tmpDir, { recursive: true, force: true });
  fs.mkdirSync(tmpDir, { recursive: true });
  const archive = path.join(
    tmpDir,
    platform === 'windows' ? 'jre.zip' : 'jre.tar.gz'
  );

  // Follow redirect to final binary URL via HEAD-less GET of API endpoint
  await download(api, archive);
  console.log(`[fetch-jre] Downloaded ${archive}`);

  fs.rmSync(OUT, { recursive: true, force: true });
  fs.mkdirSync(OUT, { recursive: true });

  if (platform === 'windows') {
    execFileSync('unzip', ['-q', '-o', archive, '-d', tmpDir]);
  } else {
    execFileSync('tar', ['-xzf', archive, '-C', tmpDir]);
  }

  const entries = fs.readdirSync(tmpDir).filter((n) => !n.endsWith('.zip') && !n.endsWith('.gz'));
  const extracted = entries.map((n) => path.join(tmpDir, n)).find((p) => fs.statSync(p).isDirectory());
  if (!extracted) {
    throw new Error('Could not find extracted JRE directory');
  }
  // Temurin layout is jre-…/ with bin/java at top
  execFileSync('cp', ['-R', `${extracted}/.`, OUT]);
  fs.rmSync(tmpDir, { recursive: true, force: true });

  const javaBin = path.join(OUT, 'bin', platform === 'windows' ? 'java.exe' : 'java');
  if (!fs.existsSync(javaBin)) {
    throw new Error(`JRE java binary missing at ${javaBin}`);
  }
  console.log(`[fetch-jre] Ready: ${javaBin}`);
}

main().catch((err) => {
  console.error('[fetch-jre]', err.message);
  process.exit(1);
});
