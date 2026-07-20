package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

import java.util.Collection;

public class BedwarsTeamsModule extends HudModule {

    private final ModeSetting viewMode;

    public BedwarsTeamsModule() {
        super("BedwarsTeams", "Own team roster / status panel", true);
        viewMode = addSetting(new ModeSetting("View", "Compact", "Compact", "Expanded"));
        setHudSize(100, 50);
        setHudPosition(2, 140);
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.getNetHandler() == null) {
            return;
        }

        ScorePlayerTeam ownTeam = mc.theWorld.getScoreboard().getPlayersTeam(mc.thePlayer.getName());
        String header = ownTeam != null ? "Team " + ownTeam.getColorPrefix() + ownTeam.getRegisteredName() : "Team";
        header = header.replaceAll("(?i)§[0-9A-FK-OR]", "");
        mc.fontRendererObj.drawStringWithShadow(header, hudX, hudY, Colors.ACCENT_BRIGHT);

        int y = hudY + 12;
        int maxW = mc.fontRendererObj.getStringWidth(header) + 4;
        int shown = 0;
        Collection<NetworkPlayerInfo> players = mc.getNetHandler().getPlayerInfoMap();
        for (NetworkPlayerInfo info : players) {
            if (info.getGameProfile() == null) {
                continue;
            }
            String name = info.getGameProfile().getName();
            ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(name);
            if (ownTeam != null && team != ownTeam) {
                continue;
            }
            if (ownTeam == null && !name.equals(mc.thePlayer.getName())) {
                continue;
            }
            String line = name + " " + info.getResponseTime() + "ms";
            if ("Compact".equals(viewMode.getValue()) && line.length() > 22) {
                line = line.substring(0, 22);
            }
            mc.fontRendererObj.drawStringWithShadow(line, hudX, y, Colors.TEXT_PRIMARY);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(line) + 4);
            y += 10;
            shown++;
            if ("Compact".equals(viewMode.getValue()) && shown >= 4) {
                break;
            }
        }
        setHudSize(maxW, Math.max(22, y - hudY));
    }
}
