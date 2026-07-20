package com.onyxclient.modules.hud;

import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SessionStatsModule extends HudModule {

    private final BooleanSetting resetPerGame;

    private int kills;
    private int deaths;
    private int beds;

    public SessionStatsModule() {
        super("SessionStats", "Live kills/deaths/beds for session", true);
        resetPerGame = addSetting(new BooleanSetting("Reset Per Game", true));
        setHudSize(90, 36);
        setHudPosition(2, 120);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled()) {
            return;
        }
        String msg = event.message.getUnformattedText().toLowerCase();
        Minecraft mc = Minecraft.getMinecraft();
        String self = mc.thePlayer != null ? mc.thePlayer.getName().toLowerCase() : "";

        if (resetPerGame.getValue() && (msg.contains("sending you to") || msg.contains("the game starts in 1 second"))) {
            kills = 0;
            deaths = 0;
            beds = 0;
        }

        if (!self.isEmpty() && msg.contains(self) && (msg.contains("killed by") || msg.contains("was killed"))) {
            if (msg.contains("was killed by") && msg.startsWith(self)) {
                deaths++;
            } else if (msg.contains("killed") && !msg.contains("was killed")) {
                kills++;
            }
        }
        if (msg.contains("bed") && (msg.contains("destroyed") || msg.contains("broken") || msg.contains("final kill"))) {
            if (msg.contains(self) || msg.contains("your")) {
                // beds broken by others mentioning you — skip
            }
            if (msg.contains("destroyed") && msg.contains(self)) {
                beds++;
            }
        }
        if (msg.contains("bed destroyed") || msg.contains("bed was broken")) {
            // Heuristic: if message attributes to player
            if (msg.contains(self)) {
                beds++;
            }
        }
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        String[] lines = {
                "K: " + kills,
                "D: " + deaths,
                "Beds: " + beds
        };
        int y = hudY;
        int maxW = 40;
        for (String line : lines) {
            mc.fontRendererObj.drawStringWithShadow(line, hudX, y, Colors.TEXT_PRIMARY);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(line) + 4);
            y += 10;
        }
        setHudSize(maxW, 30);
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getBeds() {
        return beds;
    }
}
