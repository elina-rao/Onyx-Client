export interface LeaderboardEntry {
  name: string;
  elo: number;
  wins: number;
  losses: number;
}

/**
 * Mock Season 1 board — swap for API fetch later.
 * Wins / losses intentionally diverge from ELO so Rating / Wins / W/L sorts
 * produce visibly different orders in the UI.
 */
export const MOCK_LEADERBOARD: LeaderboardEntry[] = [
  { name: "VoidPulse", elo: 2280, wins: 380, losses: 140 },
  { name: "AetherBed", elo: 2210, wins: 410, losses: 95 },
  { name: "NightScaffold", elo: 2140, wins: 290, losses: 80 },
  { name: "CrystalRush", elo: 2088, wins: 355, losses: 160 },
  { name: "OnyxRising", elo: 2012, wins: 420, losses: 210 },
  { name: "AmethystAce", elo: 1880, wins: 200, losses: 55 },
  { name: "BridgeKingEU", elo: 1825, wins: 310, losses: 190 },
  { name: "PurplePace", elo: 1760, wins: 265, losses: 95 },
  { name: "DiamondDrop", elo: 1590, wins: 340, losses: 280 },
  { name: "FastPearls", elo: 1520, wins: 175, losses: 60 },
  { name: "GoldRush99", elo: 1380, wins: 250, losses: 220 },
  { name: "IronClutch", elo: 1295, wins: 145, losses: 70 },
  { name: "CoalMiner", elo: 1188, wins: 280, losses: 310 },
  { name: "WoodStarter", elo: 1080, wins: 90, losses: 40 },
  { name: "SkyBridge", elo: 980, wins: 210, losses: 195 },
  { name: "QuietQueue", elo: 920, wins: 60, losses: 25 },
  { name: "LobbyLurker", elo: 860, wins: 190, losses: 240 },
  { name: "FirstBed", elo: 780, wins: 130, losses: 100 },
  { name: "RookieRush", elo: 720, wins: 55, losses: 20 },
  { name: "NewIgnHere", elo: 640, wins: 160, losses: 200 },
  { name: "PracticeBot", elo: 580, wins: 40, losses: 15 },
  { name: "WarmupOnly", elo: 520, wins: 120, losses: 180 },
  { name: "ClickReg", elo: 460, wins: 85, losses: 90 },
  { name: "SeasonOne", elo: 400, wins: 30, losses: 12 },
  { name: "EuQueue", elo: 340, wins: 95, losses: 160 },
  { name: "FreshSpawn", elo: 280, wins: 22, losses: 8 },
  { name: "UnrankedYet", elo: 220, wins: 70, losses: 140 },
  { name: "JustJoined", elo: 160, wins: 14, losses: 6 },
  { name: "HelloOnyx", elo: 100, wins: 45, losses: 110 },
  { name: "WoodChip", elo: 50, wins: 8, losses: 3 },
];
