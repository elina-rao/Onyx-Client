package com.onyxclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Map-aware Bedwars build-height limits (Badlion Height Overlay pattern).
 * Falls back to the module default when the map is unknown.
 */
public final class BedwarsMapHeights {

    private static final Map<String, Integer> HEIGHTS = new HashMap<String, Integer>();
    private static final Pattern MAP_LINE = Pattern.compile("^map\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);

    private static String cachedMap = "";
    private static int cachedHeight = -1;
    private static String cachedSidebarKey = "";

    static {
        // Common Hypixel Bedwars maps — approximate official build limits.
        put("aquarium", 110);
        put("archway", 108);
        put("ashfire", 112);
        put("boletum", 110);
        put("cascade", 112);
        put("catalyst", 110);
        put("crypt", 100);
        put("da vinci", 112);
        put("davinci", 112);
        put("dragonstorm", 118);
        put("gateway", 110);
        put("glacier", 112);
        put("hollow", 105);
        put("lighthouse", 115);
        put("lotus", 110);
        put("orbit", 112);
        put("pavilion", 110);
        put("playgrove", 108);
        put("rooted", 110);
        put("speedway", 108);
        put("steampunk", 112);
        put("towers", 120);
        put("waterfall", 112);
        put("airshow", 112);
        put("arcade", 110);
        put("invasion", 112);
        put("sky rise", 118);
        put("skyrise", 118);
        put("ironclad", 112);
        put("picnic", 108);
        put("sandcastle", 110);
        put("manhattan", 115);
    }

    private BedwarsMapHeights() {
    }

    private static void put(String name, int height) {
        HEIGHTS.put(name.toLowerCase(Locale.US), height);
    }

    /** Resolve build height from scoreboard map name, or {@code fallback} if unknown. */
    public static int resolve(Minecraft mc, int fallback) {
        String map = detectMapName(mc);
        if (map == null || map.isEmpty()) {
            return fallback;
        }
        String key = map + "|" + fallback;
        if (key.equals(cachedSidebarKey) && cachedHeight > 0) {
            return cachedHeight;
        }
        Integer h = HEIGHTS.get(map);
        if (h == null) {
            h = partialMatch(map);
        }
        int resolved = h != null ? h : fallback;
        cachedMap = map;
        cachedHeight = resolved;
        cachedSidebarKey = key;
        return resolved;
    }

    private static Integer partialMatch(String map) {
        // Prefer longest exact/startsWith key — avoid loose contains false-matches.
        String bestKey = null;
        Integer bestVal = null;
        for (Map.Entry<String, Integer> e : HEIGHTS.entrySet()) {
            String key = e.getKey();
            if (key.length() < 4) {
                continue;
            }
            boolean hit = map.equals(key) || map.startsWith(key + " ");
            if (hit && (bestKey == null || key.length() > bestKey.length())) {
                bestKey = key;
                bestVal = e.getValue();
            }
        }
        return bestVal;
    }

    public static String detectMapName(Minecraft mc) {
        if (mc == null || mc.theWorld == null) {
            return "";
        }
        Scoreboard board = mc.theWorld.getScoreboard();
        ScoreObjective obj = board.getObjectiveInDisplaySlot(1);
        if (obj == null) {
            return cachedMap == null ? "" : cachedMap;
        }
        Collection<Score> scores = board.getSortedScores(obj);
        for (Score score : scores) {
            String name = score.getPlayerName();
            if (name == null || name.startsWith("#")) {
                continue;
            }
            ScorePlayerTeam team = board.getPlayersTeam(name);
            String display = ScorePlayerTeam.formatPlayerName(team, name);
            String plain = strip(display).toLowerCase(Locale.US).trim();
            Matcher m = MAP_LINE.matcher(plain);
            if (m.find()) {
                return m.group(1).trim();
            }
            if (plain.contains("map:")) {
                int idx = plain.indexOf("map:");
                return plain.substring(idx + 4).trim();
            }
        }
        String title = strip(obj.getDisplayName()).toLowerCase(Locale.US);
        if (title.contains("bed war") && title.contains(" - ")) {
            int i = title.lastIndexOf(" - ");
            if (i >= 0 && i + 3 < title.length()) {
                return title.substring(i + 3).trim();
            }
        }
        return "";
    }

    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }
}
