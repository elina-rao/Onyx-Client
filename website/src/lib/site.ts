// Single source of truth for Onyx brand constants.
// Mirrors OnyxLauncher/src/main/config/launcherConfig.js.

export const SITE = {
  name: "Onyx",
  tagline: "Ranked Bedwars",
  serverIp: import.meta.env.VITE_SERVER_IP ?? "eu.onyxrbw.com",
  discordInvite: import.meta.env.VITE_DISCORD_INVITE ?? "https://discord.gg/onyxrbw",
  domain: "onyxrbw.com",
  launcherVersion: "1.0.3",
  /** YouTube or direct video URL — empty hides the Trailer section. */
  trailerUrl: import.meta.env.VITE_TRAILER_URL ?? "",
  youtubeUrl: import.meta.env.VITE_YOUTUBE_URL ?? "",
  tiktokUrl: import.meta.env.VITE_TIKTOK_URL ?? "",
  instagramUrl: import.meta.env.VITE_INSTAGRAM_URL ?? "",
} as const;

// GitHub Releases asset URLs. Empty string => button shows a "building…" state
// instead of a broken link (see /client). Fill these once the release exists.
export const DOWNLOADS = {
  mac: import.meta.env.VITE_DOWNLOAD_MAC ?? "",
  windows: import.meta.env.VITE_DOWNLOAD_WIN ?? "",
} as const;

export type TierName =
  | "Onyx"
  | "Obsidian"
  | "Amethyst"
  | "Diamond"
  | "Gold"
  | "Iron"
  | "Coal"
  | "Wood"
  | "Unranked";

export interface Tier {
  name: TierName;
  min: number;
  max: number | null;
  color: string;
}

// High -> low. Names are the user's ladder; colors are Onyx-brand.
export const TIERS: Tier[] = [
  { name: "Onyx", min: 2150, max: null, color: "#b060ff" },
  { name: "Obsidian", min: 1900, max: 2149, color: "#5a3aa5" },
  { name: "Amethyst", min: 1650, max: 1899, color: "#b060ff" },
  { name: "Diamond", min: 1400, max: 1649, color: "#38d0e0" },
  { name: "Gold", min: 1150, max: 1399, color: "#e8b23a" },
  { name: "Iron", min: 900, max: 1149, color: "#c9c9d6" },
  { name: "Coal", min: 600, max: 899, color: "#3a3a42" },
  { name: "Wood", min: 0, max: 599, color: "#8a5a3b" },
  { name: "Unranked", min: -Infinity, max: -1, color: "#6b6b7b" },
];

export function tierForElo(elo: number | null | undefined): Tier {
  if (elo == null) return TIERS[TIERS.length - 1];
  return TIERS.find((t) => elo >= t.min && (t.max == null || elo <= t.max)) ?? TIERS[TIERS.length - 1];
}

export const NAV_LINKS = [
  { to: "/", label: "Home" },
  { to: "/leaderboard", label: "Leaderboard" },
  { to: "/client", label: "Client" },
  { to: "/rules", label: "Rules" },
  { to: "/store", label: "Store" },
] as const;
