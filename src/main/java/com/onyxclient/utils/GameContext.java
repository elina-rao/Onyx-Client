package com.onyxclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.Collection;

public final class GameContext {

    public enum Mode {
        LOBBY,
        RANKED,
        BEDWARS,
        DUELS,
        UNKNOWN
    }

    private GameContext() {
    }

    public static Mode detect() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) {
            return Mode.UNKNOWN;
        }

        String serverIp = serverIpLower(mc);
        boolean rankedServer = looksLikeRankedServer(serverIp);

        Scoreboard board = mc.theWorld.getScoreboard();
        ScoreObjective obj = board.getObjectiveInDisplaySlot(1);
        if (obj == null) {
            return rankedServer ? Mode.RANKED : Mode.LOBBY;
        }

        String title = stripFormatting(obj.getDisplayName()).toLowerCase();
        String sidebar = sidebarTextLower(board, obj);

        // RANKED before generic BEDWARS when ranked keywords match
        if (rankedServer || looksLikeRankedBoard(title, sidebar)) {
            return Mode.RANKED;
        }
        if (title.contains("bed war") || sidebar.contains("bed war")) {
            return Mode.BEDWARS;
        }
        if (title.contains("duel") || sidebar.contains("duel")) {
            return Mode.DUELS;
        }
        return Mode.UNKNOWN;
    }

    private static String serverIpLower(Minecraft mc) {
        ServerData data = mc.getCurrentServerData();
        if (data == null || data.serverIP == null) {
            return "";
        }
        return data.serverIP.toLowerCase();
    }

    private static boolean looksLikeRankedServer(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return ip.contains("ranked") || ip.contains("rbw") || ip.contains("onyxranked");
    }

    private static boolean looksLikeRankedBoard(String title, String sidebar) {
        return containsRankedKeyword(title) || containsRankedKeyword(sidebar);
    }

    private static boolean containsRankedKeyword(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // Whole-word match — bare "elo" false-positives on nicks/chat fragments.
        return text.contains("ranked")
                || text.contains("rbw")
                || text.matches("(?s).*\\belo\\b.*")
                || text.contains("onyx ranked");
    }

    private static String sidebarTextLower(Scoreboard board, ScoreObjective objective) {
        StringBuilder sb = new StringBuilder();
        try {
            Collection<Score> scores = board.getSortedScores(objective);
            for (Score score : scores) {
                String name = score.getPlayerName();
                if (name == null || name.startsWith("#")) {
                    continue;
                }
                ScorePlayerTeam team = board.getPlayersTeam(name);
                String display = ScorePlayerTeam.formatPlayerName(team, name);
                sb.append(stripFormatting(display)).append(' ');
            }
        } catch (Exception ignored) {
            /* scoreboard can be mid-update */
        }
        return sb.toString().toLowerCase();
    }

    private static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }
}
