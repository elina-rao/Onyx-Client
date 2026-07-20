package com.onyxclient.modules.stats;

import com.onyxclient.OnyxClient;
import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.hud.SessionStatsModule;
import com.onyxclient.modules.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostGameSummaryModule extends Module {

    private final BooleanSetting optOut;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PostGameSummaryModule() {
        super("PostGameSummary", "Sync end-of-game stats to Onyx API", ModuleCategory.STATS);
        optOut = addSetting(new BooleanSetting("Privacy Opt-Out", false));
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled() || optOut.getValue()) {
            return;
        }
        String msg = event.message.getUnformattedText().toLowerCase();
        if (!(msg.contains("reward summary") || msg.contains("won the game") || msg.contains("game over"))) {
            return;
        }
        SessionStatsModule session = null;
        for (Module m : OnyxClient.getModuleManager().getModules()) {
            if (m instanceof SessionStatsModule) {
                session = (SessionStatsModule) m;
                break;
            }
        }
        if (session == null) {
            return;
        }
        final int kills = session.getKills();
        final int deaths = session.getDeaths();
        final int beds = session.getBeds();
        final String name = Minecraft.getMinecraft().thePlayer != null
                ? Minecraft.getMinecraft().thePlayer.getName() : "unknown";
        final String endpoint = OnyxClient.getConfigManager().getOnyxApiEndpoint();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(endpoint.replaceAll("/$", "") + "/summary");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json");
                    String body = "{\"player\":\"" + name + "\",\"kills\":" + kills
                            + ",\"deaths\":" + deaths + ",\"beds\":" + beds + "}";
                    OutputStream os = conn.getOutputStream();
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                    os.close();
                    conn.getResponseCode();
                } catch (Exception ignored) {
                }
            }
        });
    }
}
