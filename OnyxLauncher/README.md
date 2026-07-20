# Onyx Launcher

Standalone desktop launcher for **Onyx Client** (Minecraft 1.8.9 Ranked Bedwars). Deep purple / near-black UI, Microsoft or guest login, optimized JVM launch via `OnyxLoader.jar`.

## Requirements

- Node.js 18+
- npm 9+
- Java 8 (or set a custom Java path in Settings; optional bundled JRE under `resources/jre/`)

## Develop

```bash
cd OnyxLauncher
npm install
npm start
```

**macOS reopen / Dock:** Prefer a **real packed app** (correct menu bar / Dock name), not `node_modules/electron/dist/Electron.app` (that shows the default Electron splash).

```bash
npm run pack          # builds dist/mac*/Onyx Launcher.app
npm run open:dist     # opens the packed app
```

Quick-launch wrapper (still useful for `npm start` workflows): **`Onyx Launcher.app`** in this folder, or copy it to **`/Applications/Onyx Launcher.app`**:

```bash
npm run open
# or
open -a "Onyx Launcher"
```

Pin **Onyx Launcher** to the Dock from `dist/.../Onyx Launcher.app` or Applications — not the generic Electron icon.

> If Electron fails with `Cannot read properties of undefined (reading 'whenReady')`, your shell has `ELECTRON_RUN_AS_NODE=1` set (common inside IDE terminals). `npm start` clears it automatically via `scripts/run-electron.js`.

DevTools:

```bash
npm run dev
```

## Build installers / packed app

```bash
npm run pack         # unpacked .app under dist/mac* (best for local Dock use)
npm run open:dist    # open the packed Onyx Launcher.app
npm run build        # current platform installer
npm run build:mac    # .dmg
npm run build:win    # NSIS .exe
```

After `npm run pack`, install into Applications with a **full replace** (do not `cp -R` onto an old wrapper — that leaves a broken hybrid that hangs as “not responding”):

```bash
npm run pack:install
# or: npm run pack && npm run install:app
```

That clears quarantine, ad-hoc signs, and installs to `/Applications/Onyx Launcher.app`.

If Finder still blocks the first open (unsigned local build), right-click the app → **Open**.

Output goes to `dist/` (e.g. `dist/mac-arm64/OnyxLauncher.app`).

## Play button

Play launches **Forge 1.8.9** (`1.8.9-Forge11.15.1.2318-1.8.9`) with the Onyx Client mod on the classpath:

1. Resolves Java 8 (Settings path → bundled JRE → project `.jdks` → `JAVA_HOME` → `java_home -v 1.8`)
2. Finds a Forge install in the game directory or the standard Minecraft folder
3. Copies `OnyxClient-*.jar` into `mods/` from `../build/libs` or `resources/`
4. Builds the Forge classpath + natives and starts `net.minecraft.launchwrapper.Launch`

If Forge is missing, install **Forge 1.8.9-11.15.1.2318** into your Minecraft folder (or set Game Directory in Settings to that folder).

Default game directory (prefers an existing Forge install):

| OS | Preferred path |
|----|------|
| macOS | `~/Library/Application Support/minecraft` (if Forge present) else `…/onyxclient` |
| Windows | `%APPDATA%/.minecraft` (if Forge present) else `%APPDATA%/onyxclient` |
| Linux | `~/.minecraft` (if Forge present) else `~/.onyxclient` |

## Auth

- **Microsoft** — embedded `BrowserView` OAuth against the **Onyx Client** Azure app (`c7f954e5-3103-4f4b-89fd-24f03c746879`); refresh token stored in the OS keychain via `keytar` (never plaintext on disk).
- Redirect URI: `https://login.microsoftonline.com/common/oauth2/nativeclient` (Azure → Authentication → Allow public client flows = Yes).
- **Guest** — offline UUID + username in `launcher.json`; ranked/stats UI disabled.
- Saved Microsoft sessions skip the auth modal and show a “Welcome back” toast.

Override the Azure client id with:

```bash
export ONYX_MS_CLIENT_ID=your-azure-client-id
```

## First-run install

On Play, the launcher downloads Minecraft **1.8.9** client files into the game directory and fetches the Forge `1.8.9-11.15.1.2318` installer when missing.

**Home readiness strip** shows Java / Forge / Client status. Use **Install Forge 1.8.9** to run the installer (`--installClient`); if CLI install cannot finish, the GUI installer opens so you can complete it, then press **Refresh**.

## Auto-update

On home entry, the launcher GETs `{onyxApiEndpoint}/launcher/version`. If unreachable, it skips silently. A newer `launcher` version shows a toast (download via Discord); loader/client jars auto-apply when URLs are present. Expected JSON:

```json
{
  "launcher": "1.0.1",
  "loader": "1.0.1",
  "client": "1.0.1",
  "loaderUrl": "https://…/OnyxLoader.jar",
  "clientUrl": "https://…/OnyxClient-1.8.9-v1.0.jar",
  "changelog": "…"
}
```

## Launcher v1 status

**Onyx Launcher 1.0.1 closes launcher v1.** Further product work should target ranked API / client — not a broad launcher v2 (multi-account, themes, social, etc.).

## Project layout

```
OnyxLauncher/
├── src/main/          # Electron main process
├── src/renderer/      # UI (HTML/CSS/JS)
├── resources/         # Optional OnyxLoader.jar + JRE8
├── assets/            # Icons
└── electron-builder.yml
```

## Config

User config: `{userData}/launcher.json` (RAM, Java path, game dir, guest session, theme).

---

*Onyx Launcher v1.0.1 — Clean. Fast. Onyx. (launcher v1 closed)*
