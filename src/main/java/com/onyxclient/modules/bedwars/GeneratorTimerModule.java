package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generator/upgrade timer from scoreboard/actionbar only — no fabricated data.
 */
public class GeneratorTimerModule extends HudModule {

    private static final Pattern TIMER = Pattern.compile("(\\d{1,2}:\\d{2}|\\d+s)");

    private final BooleanSetting warningFlash;
    private int flashTicks;

    public GeneratorTimerModule() {
        super("GeneratorTimer", "Diamond/emerald upgrade timers from scoreboard", true);
        warningFlash = addSetting(new BooleanSetting("Warning Flash", true));
        setHudSize(120, 24);
        setHudPosition(2, 260);
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

        int y = hudY;
        int maxW = 40;
        Collection<Score> scores = board.getSortedScores(objective);
        for (Score score : scores) {
            String name = score.getPlayerName();
            if (name == null || name.startsWith("#")) {
                continue;
            }
            ScorePlayerTeam team = board.getPlayersTeam(name);
            String display = ScorePlayerTeam.formatPlayerName(team, name);
            String plain = display.replaceAll("(?i)§[0-9A-FK-OR]", "");
            String lower = plain.toLowerCase();
            if (!(lower.contains("diamond") || lower.contains("emerald") || lower.contains("generator")
                    || lower.contains("tier") || lower.contains("upgrade"))) {
                continue;
            }
            Matcher m = TIMER.matcher(plain);
            boolean warn = false;
            if (m.find()) {
                String t = m.group(1);
                if (t.endsWith("s")) {
                    try {
                        warn = Integer.parseInt(t.replace("s", "")) <= 10;
                    } catch (NumberFormatException ignored) {
                    }
                } else if (t.startsWith("0:")) {
                    warn = true;
                }
            }
            int color = Colors.TEXT_PRIMARY;
            if (warn && warningFlash.getValue()) {
                flashTicks++;
                if ((flashTicks / 5) % 2 == 0) {
                    color = Colors.DANGER;
                }
            }
            mc.fontRendererObj.drawStringWithShadow(plain, hudX, y, color);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(plain) + 4);
            y += 10;
        }
        if (y == hudY) {
            return;
        }
        setHudSize(maxW, y - hudY);
    }
}
