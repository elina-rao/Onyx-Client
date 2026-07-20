package com.onyxclient.modules.stats;

import com.onyxclient.OnyxClient;
import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LiveStatsModule extends HudModule {

    private final NumberSetting refreshSeconds;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile String status = "—";
    private long lastFetch;

    public LiveStatsModule() {
        super("LiveStats", "Live stats from Onyx RBW API", false);
        refreshSeconds = addSetting(new NumberSetting("Refresh Seconds", 30, 10, 120, 5));
        setHudSize(100, 24);
        setHudPosition(2, 320);
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastFetch < refreshSeconds.getIntValue() * 1000L) {
            return;
        }
        lastFetch = now;
        final String endpoint = OnyxClient.getConfigManager().getOnyxApiEndpoint();
        Minecraft mc = Minecraft.getMinecraft();
        final String name = mc.thePlayer != null ? mc.thePlayer.getName() : null;
        if (name == null || endpoint == null || endpoint.isEmpty()) {
            status = "No API";
            return;
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(endpoint.replaceAll("/$", "") + "/stats/" + name);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);
                    conn.setRequestMethod("GET");
                    int code = conn.getResponseCode();
                    if (code != 200) {
                        status = "API " + code;
                        return;
                    }
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    String json = sb.toString();
                    // Minimal parse without shading extra deps
                    status = extract(json, "elo") + " ELO";
                } catch (Exception e) {
                    status = "Offline";
                }
            }
        });
    }

    private String extract(String json, String key) {
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) {
            return "—";
        }
        int colon = json.indexOf(':', i);
        if (colon < 0) {
            return "—";
        }
        int end = colon + 1;
        while (end < json.length() && (json.charAt(end) == ' ' || json.charAt(end) == '"')) {
            end++;
        }
        int stop = end;
        while (stop < json.length() && ",}\"".indexOf(json.charAt(stop)) < 0) {
            stop++;
        }
        return json.substring(end, stop).replace("\"", "").trim();
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        String text = "RBW: " + status;
        mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.ACCENT_GLOW);
        setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
    }
}
