package com.onyxclient.modules.hud;

import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ScoreboardModule extends HudModule {

    private final NumberSetting backgroundOpacity;

    public ScoreboardModule() {
        super("Scoreboard", "Custom-styled sidebar scoreboard", true);
        backgroundOpacity = addSetting(new NumberSetting("Background Opacity", 0.55, 0.0, 1.0, 0.05));
        setHudSize(120, 80);
        setHudPosition(200, 2);
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

        List<Score> list = new ArrayList<Score>(board.getSortedScores(objective));
        Collections.reverse(list);
        if (list.size() > 15) {
            list = list.subList(0, 15);
        }

        String title = objective.getDisplayName();
        int maxW = mc.fontRendererObj.getStringWidth(title);
        List<String> lines = new ArrayList<String>();
        for (Score score : list) {
            if (score.getPlayerName() != null && score.getPlayerName().startsWith("#")) {
                continue;
            }
            ScorePlayerTeam team = board.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            lines.add(line);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(line.replaceAll("(?i)§[0-9A-FK-OR]", "")) + 20);
        }

        int h = 14 + lines.size() * 10;
        int alpha = (int) (backgroundOpacity.getValue() * 255);
        RenderUtils.drawRoundedRect(hudX, hudY, maxW + 10, h, 4, Colors.withAlpha(Colors.BG_CARD, alpha));
        mc.fontRendererObj.drawStringWithShadow(title, hudX + 4, hudY + 3, Colors.ACCENT_BRIGHT);
        int y = hudY + 14;
        for (String line : lines) {
            mc.fontRendererObj.drawStringWithShadow(line, hudX + 4, y, Colors.TEXT_PRIMARY);
            y += 10;
        }
        setHudSize(maxW + 10, h);
    }
}
