package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reformats scoreboard bed-alive/broken data. Legal — same info as sidebar.
 */
public class BedStatusModule extends HudModule {

    private final ModeSetting iconStyle;

    public BedStatusModule() {
        super("BedStatus", "Bed alive/broken status from scoreboard", true);
        iconStyle = addSetting(new ModeSetting("Icon Style", "Text", "Text", "Compact"));
        setHudSize(100, 40);
        setHudPosition(2, 100);
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return;
        }
        Scoreboard board = mc.theWorld.getScoreboard();
        ScoreObjective objective = board.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return;
        }

        Collection<Score> scores = board.getSortedScores(objective);
        List<String> lines = new ArrayList<String>();
        for (Score score : scores) {
            String name = score.getPlayerName();
            if (name == null || name.startsWith("#")) {
                continue;
            }
            ScorePlayerTeam team = board.getPlayersTeam(name);
            String display = ScorePlayerTeam.formatPlayerName(team, name);
            String plain = display.replaceAll("(?i)§[0-9A-FK-OR]", "");
            String lower = plain.toLowerCase();
            if (lower.contains("bed") || plain.contains("✔") || plain.contains("✘")
                    || plain.contains("✓") || plain.contains("✗") || plain.contains("❤")) {
                if ("Compact".equals(iconStyle.getValue())) {
                    lines.add(plain.length() > 18 ? plain.substring(0, 18) : plain);
                } else {
                    lines.add(plain);
                }
            }
        }

        if (lines.isEmpty()) {
            String title = "Beds";
            mc.fontRendererObj.drawStringWithShadow(title, hudX, hudY, Colors.TEXT_MUTED);
            setHudSize(mc.fontRendererObj.getStringWidth(title) + 4, 12);
            return;
        }

        int y = hudY;
        int maxW = 40;
        for (String line : lines) {
            mc.fontRendererObj.drawStringWithShadow(line, hudX, y, Colors.TEXT_PRIMARY);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(line) + 4);
            y += 10;
        }
        setHudSize(maxW, y - hudY);
    }
}
