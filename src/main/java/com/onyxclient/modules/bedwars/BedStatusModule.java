package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.bedwars.HypixelBedwarsModule;
import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.ModeSetting;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reformats scoreboard bed-alive/broken data. Legal — same info as sidebar.
 */
public class BedStatusModule extends HudModule {

    private static final Pattern TEAM_PREFIX = Pattern.compile("§([0-9a-fk-or])");
    private final ModeSetting iconStyle;

    public BedStatusModule() {
        super("BedStatus", "Bed alive/broken status from scoreboard", true);
        iconStyle = addSetting(new ModeSetting("Icon Style", "Text", "Text", "Compact"));
        setUseScaledBounds(true);
        setHudSize(100, 40);
        setHudPosition(2, 100);
        tryEnablePremiumDefaults();
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
        boolean selfBedBroken = false;
        Minecraft mcInst = mc;
        String self = mcInst.thePlayer != null ? mcInst.thePlayer.getName().toLowerCase() : "";
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
                boolean broken = lower.contains("✘") || lower.contains("✗") || lower.contains("lost")
                        || lower.contains("destroyed");
                String icon = broken ? "✗ " : "✓ ";
                if (broken && !self.isEmpty() && (lower.contains(self) || lower.contains("you")
                        || lower.contains("your"))) {
                    selfBedBroken = true;
                }
                String teamTag = "";
                Matcher m = TEAM_PREFIX.matcher(display.toLowerCase());
                if (m.find()) {
                    teamTag = "[" + m.group(1).toUpperCase() + "] ";
                }
                if ("Compact".equals(iconStyle.getValue())) {
                    String compact = teamTag + icon + plain;
                    lines.add(trimToWidth(mc, compact, 128));
                } else {
                    lines.add(teamTag + icon + plain);
                }
            }
        }
        HypixelBedwarsModule hub = HypixelBedwarsModule.INSTANCE;
        if (hub != null) {
            hub.setBedBrokenHint(selfBedBroken);
        }

        if (usePremiumRenderer()) {
            renderPremium(mc, lines);
            return;
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

    private void renderPremium(Minecraft mc, List<String> lines) {
        beginHudScale();
        int lineH = hudLineHeight(mc);
        int rowGap = HudLayoutTokens.CARD_ROW_GAP;
        String fallback = "Beds";

        int maxW = measureHudText(mc, fallback);
        for (String line : lines) {
            maxW = Math.max(maxW, measureHudText(mc, line));
        }
        int rows = Math.max(1, lines.size());
        int contentH = rows * lineH + (rows - 1) * rowGap;
        int cardW = Math.max(HudLayoutTokens.CARD_MIN_WIDTH, maxW + HudLayoutTokens.CARD_PADDING_X * 2);
        int cardH = contentH + HudLayoutTokens.CARD_PADDING_Y * 2;
        if (usePremiumCard()) {
            drawHudCard(hudX, hudY, cardW, cardH);
        }

        float tx = hudX + HudLayoutTokens.CARD_PADDING_X;
        float ty = hudY + HudLayoutTokens.CARD_PADDING_Y;
        if (lines.isEmpty()) {
            drawHudText(mc, fallback, tx, ty, HudTheme.TITLE);
        } else {
            for (String line : lines) {
                drawHudText(mc, line, tx, ty, HudTheme.VALUE);
                ty += lineH + rowGap;
            }
        }
        setHudSize(cardW, cardH);
        endHudScale();
    }

    private String trimToWidth(Minecraft mc, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (!usePremiumRenderer()) {
            return mc.fontRendererObj.trimStringToWidth(text, maxWidth);
        }
        if (measureHudText(mc, text) <= maxWidth) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String candidate = sb.toString() + text.charAt(i);
            if (measureHudText(mc, candidate + "…") > maxWidth) {
                break;
            }
            sb.append(text.charAt(i));
        }
        return sb.toString() + "…";
    }

    private void tryEnablePremiumDefaults() {
        try {
            for (com.onyxclient.modules.settings.Setting<?> setting : getSettings()) {
                if ("Premium Renderer".equals(setting.getName()) || "Premium Card".equals(setting.getName())) {
                    @SuppressWarnings("unchecked")
                    com.onyxclient.modules.settings.Setting<Boolean> b =
                            (com.onyxclient.modules.settings.Setting<Boolean>) setting;
                    b.setValue(true);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
