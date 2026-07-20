# Onyx Client

A custom **Minecraft 1.8.9 Forge** PvP client mod — clean, Hypixel-safe, and visually distinct. Deep purple (`#7B2FBE`) and near-black (`#0D0D0D`) theme.

## Features

- **Module system** — 30+ toggle-able visual, performance, and HUD modules
- **Mod Menu** — Right Shift to open; search, categories, settings panels
- **Main Menu overhaul** — Animated purple particle background
- **HUD Editor** — `/hc hud` for drag-and-drop HUD layout
- **1.7 Animations** — Per-animation toggles with offset sliders
- **OptiFine-compatible zoom** — Hold `C` for smooth FOV zoom
- **Custom Cape** — Client-side Onyx cape via channel handshake
- **Bedwars Stars** — Hypixel API display (requires your own API key)

## Requirements

- Java 8
- Minecraft 1.8.9
- Forge `11.15.1.2318`

## Build

```bash
./gradlew setupDecompWorkspace
./gradlew build
```

Requires **Java 8** and Gradle wrapper (`./gradlew` — uses Gradle 4.10.3 for ForgeGradle 2.1).

Output: `build/libs/OnyxClient-1.8.9-v1.0.jar`

## Loader

```bash
cd OnyxLoader
./gradlew build copyToLauncher
```

See [OnyxLoader/README.md](OnyxLoader/README.md).

## Launcher

The standalone **Onyx Launcher** lives in [`OnyxLauncher/`](OnyxLauncher/):

```bash
cd OnyxLauncher
npm install
npm start
```

See [OnyxLauncher/README.md](OnyxLauncher/README.md) for auth, packaging, and Play / OnyxLoader setup.

## Commands

| Command | Description |
|---------|-------------|
| `/hc` | Show help |
| `/hc reload` | Reload config |
| `/hc apikey <key>` | Set Hypixel API key |
| `/hc reset` | Reset config |
| `/hc hud` | Open HUD editor |

## Keybinds

| Key | Action |
|-----|--------|
| Right Shift | Open Mod Menu |
| C | Zoom (OptiFine Settings module) |

## Config

Settings persist to `config/onyxclient/settings.json`.

## Hypixel Safety

All features are **client-side visual only**. No cheats, automation, or packet modification.

---

*Onyx Client v1.0 — Clean. Fast. Onyx.*
