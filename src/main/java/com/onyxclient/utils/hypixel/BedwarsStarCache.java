package com.onyxclient.utils.hypixel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BedwarsStarCache {

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<String, CacheEntry>();

    public void fetchIfNeeded(String username, String apiKey) {
        CacheEntry entry = cache.get(username.toLowerCase());
        if (entry != null && !entry.isExpired()) {
            return;
        }
        if (entry != null && entry.pending) {
            return;
        }
        cache.put(username.toLowerCase(), new CacheEntry(null, System.currentTimeMillis(), true));
        EXECUTOR.submit(new FetchTask(username, apiKey));
    }

    public Integer getStars(String username) {
        CacheEntry entry = cache.get(username.toLowerCase());
        return entry != null ? entry.stars : null;
    }

    public void clearExpired() {
        for (Map.Entry<String, CacheEntry> e : cache.entrySet()) {
            if (e.getValue().isExpired()) {
                cache.remove(e.getKey());
            }
        }
    }

    private class FetchTask implements Runnable {
        private final String username;
        private final String apiKey;

        private FetchTask(String username, String apiKey) {
            this.username = username;
            this.apiKey = apiKey;
        }

        @Override
        public void run() {
            try {
                URL url = new URL("https://api.hypixel.net/player?key=" + apiKey + "&name=" + username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JsonObject root = new JsonParser().parse(sb.toString()).getAsJsonObject();
                if (!root.get("success").getAsBoolean()) {
                    cache.remove(username.toLowerCase());
                    return;
                }
                JsonObject player = root.getAsJsonObject("player");
                if (player == null) {
                    cache.remove(username.toLowerCase());
                    return;
                }
                JsonObject achievements = player.getAsJsonObject("achievements");
                int stars = 0;
                if (achievements != null && achievements.has("bedwars_level")) {
                    stars = achievements.get("bedwars_level").getAsInt();
                }
                cache.put(username.toLowerCase(), new CacheEntry(stars, System.currentTimeMillis(), false));
            } catch (Exception e) {
                cache.remove(username.toLowerCase());
            }
        }
    }

    private static class CacheEntry {
        private final Integer stars;
        private final long timestamp;
        private final boolean pending;

        private CacheEntry(Integer stars, long timestamp, boolean pending) {
            this.stars = stars;
            this.timestamp = timestamp;
            this.pending = pending;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
