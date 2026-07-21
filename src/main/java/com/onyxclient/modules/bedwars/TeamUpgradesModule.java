package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.GameContext;
import com.onyxclient.utils.HudLayoutTokens;
import com.onyxclient.utils.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Team upgrade progress — scoreboard parse first, then owned-only gear signals
 * (never invent diamond costs Hypixel does not publish).
 */
public class TeamUpgradesModule extends HudModule {

    private static final Pattern DIAMOND_COST = Pattern.compile(
            "(?:need\\s*)?(\\d+)\\s*(?:diamonds?|♦|◆)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEVEL = Pattern.compile(
            "\\b(?:lvl|level|tier)?\\s*(?:I{1,3}|IV|V|\\d+)\\b", Pattern.CASE_INSENSITIVE);
    private static final String[] UPGRADE_KEYS = {
            "sharpness", "sharpened", "protection", "reinforced",
            "mining fatigue", "it's a trap", "alarm trap", "counter-offensive",
            "heal pool", "dragon buff", "iron forge", "golden forge", "emerald forge",
            "maniac miner", "forge", "haste"
    };

    public TeamUpgradesModule() {
        super("TeamUpgrades", "Team upgrade progress / diamonds needed HUD", true);
        setUseScaledBounds(true);
        setHudSize(120, 48);
        setHudPosition(2, 180);
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
        if (!inBedwarsContext(mc)) {
            return;
        }

        List<String> lines = parseUpgradeLines(mc);
        if (lines.isEmpty()) {
            lines = fallbackFromGear(mc);
        }
        if (usePremiumRenderer()) {
            renderPremium(mc, lines);
            return;
        }

        if (lines.isEmpty()) {
            String fallback = "Upgrades: —";
            mc.fontRendererObj.drawStringWithShadow(fallback, hudX, hudY, Colors.TEXT_MUTED);
            setHudSize(mc.fontRendererObj.getStringWidth(fallback) + 4, 12);
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

    private boolean inBedwarsContext(Minecraft mc) {
        HypixelBedwarsModule hub = HypixelBedwarsModule.INSTANCE;
        if (hub != null && hub.isInBedwars()) {
            return true;
        }
        GameContext.Mode mode = GameContext.detect();
        return mode == GameContext.Mode.BEDWARS || mode == GameContext.Mode.RANKED;
    }

    private List<String> parseUpgradeLines(Minecraft mc) {
        List<String> out = new ArrayList<String>();
        Scoreboard board = mc.theWorld.getScoreboard();
        ScoreObjective objective = board.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return out;
        }
        Collection<Score> scores = board.getSortedScores(objective);
        for (Score score : scores) {
            String name = score.getPlayerName();
            if (name == null || name.startsWith("#")) {
                continue;
            }
            ScorePlayerTeam team = board.getPlayersTeam(name);
            String display = ScorePlayerTeam.formatPlayerName(team, name);
            String plain = stripColor(display);
            String formatted = formatUpgradeLine(plain);
            if (formatted != null) {
                out.add(formatted);
            }
        }
        return out;
    }

    /**
     * Owned-only signals from local gear. Missing upgrades show "—" — never invent shop costs.
     */
    private List<String> fallbackFromGear(Minecraft mc) {
        Map<String, String> lines = new LinkedHashMap<String, String>();
        if (mc.thePlayer == null) {
            return new ArrayList<String>();
        }

        int sharp = 0;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held != null && held.getItem() instanceof ItemSword) {
            sharp = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, held);
        }
        for (ItemStack stack : mc.thePlayer.inventory.mainInventory) {
            if (stack != null && stack.getItem() instanceof ItemSword) {
                sharp = Math.max(sharp, EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack));
            }
        }
        if (sharp > 0) {
            lines.put("Sharpness", "Sharpness  Owned Lvl " + sharp);
        } else {
            lines.put("Sharpness", "Sharpness  —");
        }

        int prot = 0;
        for (ItemStack armor : mc.thePlayer.inventory.armorInventory) {
            if (armor != null && armor.getItem() instanceof ItemArmor) {
                prot = Math.max(prot, EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, armor));
            }
        }
        if (prot > 0) {
            lines.put("Protection", "Protection  Owned Lvl " + prot);
        } else {
            lines.put("Protection", "Protection  —");
        }

        String forge = detectForgeFromScoreboard(mc);
        if (forge != null) {
            lines.put("Forge", forge);
        } else {
            lines.put("Forge", "Forge  —");
        }

        return new ArrayList<String>(lines.values());
    }

    private String detectForgeFromScoreboard(Minecraft mc) {
        Scoreboard board = mc.theWorld.getScoreboard();
        ScoreObjective objective = board.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return null;
        }
        for (Score score : board.getSortedScores(objective)) {
            String name = score.getPlayerName();
            if (name == null || name.startsWith("#")) {
                continue;
            }
            ScorePlayerTeam team = board.getPlayersTeam(name);
            String plain = stripColor(ScorePlayerTeam.formatPlayerName(team, name)).toLowerCase(Locale.US);
            if (plain.contains("emerald") && (plain.contains("gen") || plain.contains("forge") || plain.contains("ii") || plain.contains("iii"))) {
                return "Forge  Emerald+";
            }
            if (plain.contains("gold") && plain.contains("forge")) {
                return "Forge  Golden";
            }
            if (plain.contains("iron forge") || (plain.contains("forge") && plain.contains("iron"))) {
                return "Forge  Iron";
            }
        }
        return null;
    }

    private String formatUpgradeLine(String plain) {
        if (plain == null || plain.isEmpty()) {
            return null;
        }
        String lower = plain.toLowerCase(Locale.ROOT);
        String upgradeName = matchUpgradeName(lower, plain);
        if (upgradeName == null) {
            return null;
        }

        boolean owned = lower.contains("owned") || lower.contains("purchased")
                || plain.contains("✔") || plain.contains("✓");
        boolean missing = plain.contains("✘") || plain.contains("✗") || plain.contains("✕");

        Matcher cost = DIAMOND_COST.matcher(plain);
        String diamonds = null;
        if (cost.find()) {
            diamonds = cost.group(1);
        }

        Matcher lvl = LEVEL.matcher(plain);
        String level = null;
        if (lvl.find()) {
            level = lvl.group().trim();
        }

        // Require a concrete signal — bare keyword hits are usually unrelated sidebar text.
        if (!owned && diamonds == null && level == null && !missing) {
            return null;
        }

        StringBuilder sb = new StringBuilder(upgradeName);
        if (owned && diamonds == null) {
            sb.append("  Owned");
        } else if (diamonds != null) {
            sb.append("  Need ").append(diamonds).append(" diamonds");
        } else if (level != null && !owned) {
            sb.append("  ").append(normalizeLevel(level));
        } else if (missing) {
            sb.append("  —");
        } else if (level != null) {
            sb.append("  ").append(normalizeLevel(level));
        } else {
            sb.append("  ").append(plain.trim());
        }
        return sb.toString();
    }

    private String matchUpgradeName(String lower, String plain) {
        for (String key : UPGRADE_KEYS) {
            if (lower.contains(key)) {
                return prettyUpgradeName(key, plain);
            }
        }
        return null;
    }

    private String prettyUpgradeName(String key, String plain) {
        String lower = plain.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(key);
        if (idx >= 0) {
            int end = Math.min(plain.length(), idx + key.length());
            String slice = plain.substring(idx, end).trim();
            if (!slice.isEmpty()) {
                return capitalizeWords(slice);
            }
        }
        return capitalizeWords(key);
    }

    private String normalizeLevel(String level) {
        String t = level.trim();
        if (t.toLowerCase(Locale.ROOT).startsWith("lvl")
                || t.toLowerCase(Locale.ROOT).startsWith("level")
                || t.toLowerCase(Locale.ROOT).startsWith("tier")) {
            return "Lvl " + t.replaceAll("(?i)^(lvl|level|tier)\\s*", "");
        }
        return "Lvl " + t;
    }

    private String capitalizeWords(String s) {
        String[] parts = s.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                sb.append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }

    private String stripColor(String s) {
        return s == null ? "" : s.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    private void renderPremium(Minecraft mc, List<String> lines) {
        beginHudScale();
        int lineH = hudLineHeight(mc);
        int rowGap = HudLayoutTokens.CARD_ROW_GAP;
        String title = "Upgrades";
        String fallback = "Upgrades: —";

        int maxW = measureHudText(mc, title);
        if (lines.isEmpty()) {
            maxW = Math.max(maxW, measureHudText(mc, fallback));
        } else {
            for (String line : lines) {
                maxW = Math.max(maxW, measureHudText(mc, line));
            }
        }
        int rows = lines.isEmpty() ? 2 : 1 + lines.size();
        int contentH = rows * lineH + (rows - 1) * rowGap + HudLayoutTokens.CARD_TITLE_GAP;
        int cardW = Math.max(HudLayoutTokens.CARD_MIN_WIDTH, maxW + HudLayoutTokens.CARD_PADDING_X * 2);
        int cardH = contentH + HudLayoutTokens.CARD_PADDING_Y * 2;
        if (usePremiumCard()) {
            drawHudCard(hudX, hudY, cardW, cardH);
        }

        float tx = hudX + HudLayoutTokens.CARD_PADDING_X;
        float ty = hudY + HudLayoutTokens.CARD_PADDING_Y;
        drawHudAccentText(mc, title, tx, ty, HudTheme.TITLE);
        ty += lineH + HudLayoutTokens.CARD_TITLE_GAP;
        if (lines.isEmpty()) {
            drawHudText(mc, fallback, tx, ty, HudTheme.VALUE);
        } else {
            for (String line : lines) {
                drawHudText(mc, line, tx, ty, HudTheme.VALUE);
                ty += lineH + rowGap;
            }
        }
        setHudSize(cardW, cardH);
        endHudScale();
    }
}
