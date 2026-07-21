package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.GameContext;
import com.onyxclient.utils.HudLayoutTokens;
import com.onyxclient.utils.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedwarsTeamsModule extends HudModule {

    private static final Pattern HEARTS = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*❤");
    private static final Pattern DEAD_MARK = Pattern.compile("(?i)\\b(dead|eliminated|final)\\b|✘|✗");

    private final ModeSetting viewMode;

    public BedwarsTeamsModule() {
        super("BedwarsTeams", "Own team roster / status panel", true);
        viewMode = addSetting(new ModeSetting("View", "Compact", "Compact", "Expanded"));
        setUseScaledBounds(true);
        setHudSize(100, 50);
        setHudPosition(2, 140);
        enablePremiumDefaults();
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.getNetHandler() == null || mc.theWorld == null) {
            return;
        }
        if (!inBedwarsContext(mc)) {
            return;
        }

        ScorePlayerTeam ownTeam = mc.theWorld.getScoreboard().getPlayersTeam(mc.thePlayer.getName());
        String header = ownTeam != null ? "Team " + ownTeam.getColorPrefix() + ownTeam.getRegisteredName() : "Team";
        header = stripColor(header);

        Map<String, String> sidebarHints = collectSidebarHints(mc);
        List<Row> rows = new ArrayList<Row>();
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

            TeammateStatus status = resolveStatus(mc, info, name, sidebarHints);
            String line = formatLine(name, status);
            if ("Compact".equals(viewMode.getValue())) {
                line = ellipsize(mc, line, 120);
            }
            rows.add(new Row(line, status.alive, status.known));
            shown++;
            if ("Compact".equals(viewMode.getValue()) && shown >= 4) {
                break;
            }
        }

        if (usePremiumRenderer()) {
            renderPremium(mc, header, rows);
            return;
        }

        mc.fontRendererObj.drawStringWithShadow(header, hudX, hudY, Colors.ACCENT_BRIGHT);
        int y = hudY + 12;
        int maxW = mc.fontRendererObj.getStringWidth(header) + 4;
        for (Row row : rows) {
            int color = rowColor(row);
            mc.fontRendererObj.drawStringWithShadow(row.text, hudX, y, color);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(row.text) + 4);
            y += 10;
        }
        setHudSize(maxW, Math.max(22, y - hudY));
    }

    private boolean inBedwarsContext(Minecraft mc) {
        HypixelBedwarsModule hub = HypixelBedwarsModule.INSTANCE;
        if (hub != null && hub.isInBedwars()) {
            return true;
        }
        GameContext.Mode mode = GameContext.detect();
        return mode == GameContext.Mode.BEDWARS || mode == GameContext.Mode.RANKED;
    }

    private void renderPremium(Minecraft mc, String header, List<Row> rows) {
        beginHudScale();
        int lineH = hudLineHeight(mc);
        int rowGap = HudLayoutTokens.CARD_ROW_GAP;
        int maxW = measureHudText(mc, header);
        for (Row row : rows) {
            maxW = Math.max(maxW, measureHudText(mc, row.text));
        }
        int totalRows = 1 + Math.max(1, rows.size());
        int contentH = totalRows * lineH + (totalRows - 1) * rowGap + HudLayoutTokens.CARD_TITLE_GAP;
        int cardW = Math.max(HudLayoutTokens.CARD_MIN_WIDTH, maxW + HudLayoutTokens.CARD_PADDING_X * 2);
        int cardH = contentH + HudLayoutTokens.CARD_PADDING_Y * 2;
        if (usePremiumCard()) {
            drawHudCard(hudX, hudY, cardW, cardH);
        }
        float tx = hudX + HudLayoutTokens.CARD_PADDING_X;
        float ty = hudY + HudLayoutTokens.CARD_PADDING_Y;
        drawHudAccentText(mc, header, tx, ty, HudTheme.TITLE);
        ty += lineH + HudLayoutTokens.CARD_TITLE_GAP;
        if (rows.isEmpty()) {
            drawHudText(mc, "No teammates", tx, ty, HudTheme.VALUE);
        } else {
            for (Row row : rows) {
                drawHudText(mc, row.text, tx, ty, rowColor(row));
                ty += lineH + rowGap;
            }
        }
        setHudSize(cardW, cardH);
        endHudScale();
    }

    private static int rowColor(Row row) {
        if (!row.known) {
            return Colors.TEXT_MUTED;
        }
        return row.alive ? Colors.TEXT_PRIMARY : Colors.DANGER;
    }

    private String ellipsize(Minecraft mc, String line, int maxPx) {
        if (mc.fontRendererObj.getStringWidth(line) <= maxPx) {
            return line;
        }
        String ellipsis = "…";
        int budget = maxPx - mc.fontRendererObj.getStringWidth(ellipsis);
        if (budget <= 0) {
            return ellipsis;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (mc.fontRendererObj.getStringWidth(sb.toString() + c) > budget) {
                break;
            }
            sb.append(c);
        }
        return sb.append(ellipsis).toString();
    }

    private TeammateStatus resolveStatus(Minecraft mc, NetworkPlayerInfo info, String name,
                                         Map<String, String> sidebarHints) {
        TeammateStatus status = new TeammateStatus();
        EntityPlayer entity = mc.theWorld.getPlayerEntityByName(name);

        if (entity != null && !entity.isDead) {
            float hp = entity.getHealth();
            if (hp > 0.0F) {
                status.known = true;
                status.alive = true;
                status.hearts = hp / 2.0F;
                return status;
            }
            status.known = true;
            status.alive = false;
            return status;
        }

        if (info.getDisplayName() != null) {
            String raw = stripColor(info.getDisplayName().getUnformattedText());
            Matcher hearts = HEARTS.matcher(raw);
            if (hearts.find()) {
                try {
                    status.hearts = Float.parseFloat(hearts.group(1)) / 2.0F;
                    status.known = true;
                    status.alive = status.hearts > 0.0F;
                    return status;
                } catch (NumberFormatException ignored) {
                }
            }
            if (DEAD_MARK.matcher(raw).find()) {
                status.known = true;
                status.alive = false;
                return status;
            }
        }

        String hint = sidebarHints.get(name.toLowerCase(Locale.ROOT));
        if (hint != null) {
            if (DEAD_MARK.matcher(hint).find()) {
                status.known = true;
                status.alive = false;
                return status;
            }
            Matcher hearts = HEARTS.matcher(hint);
            if (hearts.find()) {
                try {
                    status.hearts = Float.parseFloat(hearts.group(1)) / 2.0F;
                    status.known = true;
                    status.alive = status.hearts > 0.0F;
                    return status;
                } catch (NumberFormatException ignored) {
                }
            }
            if (hint.toLowerCase(Locale.ROOT).contains("alive") || hint.contains("✔") || hint.contains("✓")) {
                status.known = true;
                status.alive = true;
                return status;
            }
        }

        // No positive signal — unknown (do not assume alive)
        status.known = false;
        status.alive = false;
        return status;
    }

    private Map<String, String> collectSidebarHints(Minecraft mc) {
        Map<String, String> map = new HashMap<String, String>();
        Scoreboard board = mc.theWorld.getScoreboard();
        ScoreObjective objective = board.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return map;
        }
        for (Score score : board.getSortedScores(objective)) {
            String playerName = score.getPlayerName();
            if (playerName == null || playerName.startsWith("#")) {
                continue;
            }
            ScorePlayerTeam team = board.getPlayersTeam(playerName);
            String display = stripColor(ScorePlayerTeam.formatPlayerName(team, playerName));
            for (String token : display.split("\\s+")) {
                String clean = token.replaceAll("[^A-Za-z0-9_]", "");
                if (clean.length() >= 3) {
                    map.put(clean.toLowerCase(Locale.ROOT), display);
                }
            }
            map.put(stripColor(playerName).toLowerCase(Locale.ROOT), display);
        }
        return map;
    }

    private String formatLine(String name, TeammateStatus status) {
        StringBuilder sb = new StringBuilder(name);
        if (!status.known) {
            sb.append("  ?");
            return sb.toString();
        }
        if (status.alive) {
            if (status.hearts > 0.0F) {
                if (status.hearts == Math.floor(status.hearts)) {
                    sb.append("  ").append((int) status.hearts).append("❤");
                } else {
                    sb.append("  ").append(String.format(Locale.ROOT, "%.1f", status.hearts)).append("❤");
                }
            }
            if ("Expanded".equals(viewMode.getValue())) {
                sb.append("  Alive");
            }
        } else {
            sb.append("  Dead");
        }
        return sb.toString();
    }

    private String stripColor(String s) {
        return s == null ? "" : s.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    private static final class TeammateStatus {
        boolean known = false;
        boolean alive = false;
        float hearts = -1.0F;
    }

    private static final class Row {
        private final String text;
        private final boolean alive;
        private final boolean known;

        private Row(String text, boolean alive, boolean known) {
            this.text = text;
            this.alive = alive;
            this.known = known;
        }
    }
}
