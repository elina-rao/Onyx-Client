package com.onyxclient.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class RankedStatsClient {

    private static volatile RankedStats lastFetched;

    private RankedStatsClient() {
    }

    public static RankedStats getLastFetched() {
        return lastFetched;
    }

    public static RankedStats fetch(String endpoint, String ign) throws Exception {
        URL url = new URL(endpoint.replaceAll("/$", "") + "/stats/" + ign);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IllegalStateException("HTTP " + code);
        }
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        RankedStats stats = parse(sb.toString());
        lastFetched = stats;
        return stats;
    }

    public static RankedStats parse(String json) {
        RankedStats stats = new RankedStats();
        JsonObject root;
        try {
            JsonElement parsed = new JsonParser().parse(json);
            if (parsed == null || !parsed.isJsonObject()) {
                return stats;
            }
            root = parsed.getAsJsonObject();
        } catch (Exception e) {
            return stats;
        }

        stats.elo = pick(root, "elo", "ELO", "rating");
        stats.rank = pick(root, "rank", "tier", "division");
        stats.wins = pick(root, "wins", "victories");
        stats.losses = pick(root, "losses", "deaths");
        stats.wlr = pick(root, "wlr", "wlrRatio", "winLoss");
        stats.streak = pick(root, "streak", "winStreak");
        if ("—".equals(stats.wlr) && !"—".equals(stats.wins) && !"—".equals(stats.losses)) {
            try {
                double w = Double.parseDouble(stats.wins);
                double l = Double.parseDouble(stats.losses);
                double ratio = l <= 0 ? w : w / l;
                stats.wlr = String.format(java.util.Locale.US, "%.2f", ratio);
            } catch (Exception ignored) {
            }
        }
        return stats;
    }

    private static String pick(JsonObject root, String... keys) {
        for (String key : keys) {
            String value = readString(root, key);
            if (!"—".equals(value)) {
                return value;
            }
        }
        return "—";
    }

    private static String readString(JsonObject root, String key) {
        if (root == null || !root.has(key) || root.get(key).isJsonNull()) {
            return "—";
        }
        JsonElement el = root.get(key);
        try {
            if (el.isJsonPrimitive()) {
                if (el.getAsJsonPrimitive().isNumber()) {
                    double d = el.getAsDouble();
                    if (d == Math.rint(d)) {
                        return String.valueOf((long) d);
                    }
                    return String.valueOf(d);
                }
                String s = el.getAsString();
                if (s != null && !s.trim().isEmpty()) {
                    return s.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "—";
    }

    public static final class RankedStats {
        public String elo = "—";
        public String rank = "—";
        public String wins = "—";
        public String losses = "—";
        public String wlr = "—";
        public String streak = "—";
    }
}
