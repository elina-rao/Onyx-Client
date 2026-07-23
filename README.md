# Onyx Client

Onyx Client is an independent Minecraft Java launcher and companion client for the Onyx Ranked Bedwars community.

It authenticates players with Microsoft, then Xbox Live / XSTS, then Minecraft Services (`login_with_xbox`) so they can launch Minecraft Java Edition (1.8.9).

## Auth (Microsoft / Minecraft Services)

| Item | Value |
|------|--------|
| Azure AD app | Public client (Allow public client flows = Yes) |
| Application (client) ID | `c7f954e5-3103-4f4b-89fd-24f03c746879` |
| Redirect URI | `https://login.microsoftonline.com/common/oauth2/nativeclient` |
| Flow | Microsoft → Xbox Live → XSTS → Minecraft Services (`login_with_xbox`) |
| Game | Minecraft: Java Edition |

## AppID review

Custom Azure AppID pending Mojang approval: [aka.ms/mce-reviewappid](https://aka.ms/mce-reviewappid).

## Downloads

Installer builds (no source): [Releases](https://github.com/elina-rao/Onyx-Client/releases)

## Not affiliated

Not affiliated with Mojang, Microsoft, Hypixel, or Discord.

## Status

In development. Source code will be published after Minecraft Services API approval.
