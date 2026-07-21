package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.HudLayoutTokens;
import com.onyxclient.utils.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generator/upgrade timer from scoreboard — hardened parsing for Hypixel + Ranked formats.
 */
public class GeneratorTimerModule extends HudModule {

    private static final Pattern TIMER = Pattern.compile(
            "(?:(\\d{1,2}):(\\d{2}))|(?:(\\d+)\\s*s(?:ec(?:onds?)?)?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CLEAN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    private final BooleanSetting warningFlash;
    private final NumberSetting warningSeconds;
    private int flashTicks;

    public GeneratorTimerModule() {
        super("GeneratorTimer", "Diamond/emerald upgrade timers from scoreboard", true);
        warningFlash = addSetting(new BooleanSetting("Warning Flash", true));
        warningSeconds = addSetting(new NumberSetting("Warning Seconds", 10, 3, 30, 1));
        setUseScaledBounds(true);
        setHudSize(120, 24);
        setHudPosition(2, 260);
        enablePremiumDefaults();
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

        List<Line> lines = new ArrayList<Line>();
        Collection<Score> scores = board.getSortedScores(objective);
        for (Score score : scores) {
            String name = score.getPlayerName();
            if (name == null || name.startsWith("#")) {
                continue;
            }
            ScorePlayerTeam team = board.getPlayersTeam(name);
            String display = ScorePlayerTeam.formatPlayerName(team, name);
            String plain = CLEAN.matcher(display).replaceAll("").trim();
            if (plain.isEmpty()) {
                continue;
            }
            String lower = plain.toLowerCase(Locale.US);
            if (!isGeneratorLine(lower)) {
                continue;
            }
            int secondsLeft = parseSecondsLeft(plain);
            if (secondsLeft < 0) {
                continue; // label-only rows without a timer are noise
            }
            boolean warn = secondsLeft <= warningSeconds.getIntValue();
            String label = shorten(plain);
            lines.add(new Line(label, warn));
        }
        if (lines.isEmpty()) {
            return;
        }
        flashTicks++;
        if (!usePremiumRenderer()) {
            renderLegacy(mc, lines);
            return;
        }
        renderPremium(mc, lines);
    }

    private static boolean isGeneratorLine(String lower) {
        boolean gem = lower.contains("diamond") || lower.contains("emerald");
        boolean genHint = lower.contains("generator")
                || lower.contains("tier")
                || lower.contains(" ii")
                || lower.contains(" iii")
                || lower.endsWith("ii")
                || lower.endsWith("iii")
                || lower.contains("upgrade")
                || lower.contains("spawn");
        if (gem && genHint) {
            return true;
        }
        return lower.contains("generator") && TIMER.matcher(lower).find();
    }

    private static int parseSecondsLeft(String plain) {
        Matcher m = TIMER.matcher(plain);
        if (!m.find()) {
            return -1;
        }
        try {
            if (m.group(1) != null && m.group(2) != null) {
                int mins = Integer.parseInt(m.group(1));
                int secs = Integer.parseInt(m.group(2));
                return mins * 60 + secs;
            }
            if (m.group(3) != null) {
                return Integer.parseInt(m.group(3));
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }

    private static String shorten(String plain) {
        String s = plain.replaceAll("\\s{2,}", " ").trim();
        if (s.length() > 28) {
            return s.substring(0, 27) + "…";
        }
        return s;
    }

    private void renderLegacy(Minecraft mc, List<Line> lines) {
        int y = hudY;
        int maxW = 40;
        for (Line line : lines) {
            int color = lineColor(line, Colors.TEXT_PRIMARY);
            mc.fontRendererObj.drawStringWithShadow(line.text, hudX, y, color);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(line.text) + 4);
            y += 10;
        }
        setHudSize(maxW, y - hudY);
    }

    private void renderPremium(Minecraft mc, List<Line> lines) {
        beginHudScale();
        int lineH = hudLineHeight(mc);
        int contentW = 0;
        for (Line line : lines) {
            contentW = Math.max(contentW, measureHudText(mc, line.text));
        }
        int contentH = lines.size() * lineH + (lines.size() - 1) * HudLayoutTokens.CARD_ROW_GAP;
        int cardW = Math.max(HudLayoutTokens.CARD_MIN_WIDTH, contentW + HudLayoutTokens.CARD_PADDING_X * 2);
        int cardH = contentH + HudLayoutTokens.CARD_PADDING_Y * 2;
        if (usePremiumCard()) {
            drawHudCard(hudX, hudY, cardW, cardH);
        }
        float tx = hudX + HudLayoutTokens.CARD_PADDING_X;
        float ty = hudY + HudLayoutTokens.CARD_PADDING_Y;
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            int color = lineColor(line, HudTheme.VALUE);
            drawHudText(mc, line.text, tx, ty + i * (lineH + HudLayoutTokens.CARD_ROW_GAP), color);
        }
        setHudSize(cardW, cardH);
        endHudScale();
    }

    private int lineColor(Line line, int base) {
        if (line.warn && warningFlash.getValue()) {
            if ((flashTicks / 5) % 2 == 0) {
                return Colors.DANGER;
            }
            return HudTheme.VALUE_ACCENT;
        }
        if (line.emerald) {
            return 0xFF55FF55;
        }
        if (line.diamond) {
            return 0xFF55FFFF;
        }
        return base;
    }

    private static final class Line {
        private final String text;
        private final boolean warn;
        private final boolean diamond;
        private final boolean emerald;

        private Line(String text, boolean warn) {
            this.text = text;
            this.warn = warn;
            String lower = text.toLowerCase(Locale.US);
            this.diamond = lower.contains("diamond") || lower.contains("dia");
            this.emerald = lower.contains("emerald") || lower.contains("eme");
        }
    }
}
