package com.onyxclient.modules.stats;

import com.onyxclient.OnyxClient;
import com.onyxclient.core.RankedStatsClient;
import com.onyxclient.modules.Module;
import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.GameContext;
import com.onyxclient.utils.HudLayoutTokens;
import com.onyxclient.utils.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LiveStatsModule extends HudModule {

    private final NumberSetting refreshSeconds;
    private final BooleanSetting lobbyOnly;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile RankedStatsClient.RankedStats cached = new RankedStatsClient.RankedStats();
    private volatile String status = "—";
    private long lastFetch;

    public LiveStatsModule() {
        super("LiveStats", "Live stats from Onyx RBW API", false);
        refreshSeconds = addSetting(new NumberSetting("Refresh Seconds", 30, 10, 120, 5));
        lobbyOnly = addSetting(new BooleanSetting("Lobby Only", false));
        setUseScaledBounds(true);
        setHudSize(100, 24);
        setHudPosition(2, 320);
        tryEnablePremiumDefaults();
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastFetch < refreshSeconds.getIntValue() * 1000L) {
            return;
        }
        lastFetch = now;
        Minecraft mc = Minecraft.getMinecraft();
        GameContext.Mode mode = GameContext.detect();
        if (lobbyOnly.getValue() && mode != GameContext.Mode.LOBBY && mode != GameContext.Mode.UNKNOWN) {
            return;
        }
        final String endpoint = OnyxClient.getConfigManager().getOnyxApiEndpoint();
        final String name = mc.thePlayer != null ? mc.thePlayer.getName() : null;
        if (name == null || endpoint == null || endpoint.isEmpty()) {
            status = "No API";
            return;
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RankedStatsClient.RankedStats stats = RankedStatsClient.fetch(endpoint, name);
                    cached = stats;
                    status = stats.elo + " ELO";
                    pushRankElo(stats);
                } catch (Exception e) {
                    status = "Offline";
                }
            }
        });
    }

    private void pushRankElo(RankedStatsClient.RankedStats stats) {
        try {
            Module mod = OnyxClient.getModuleManager().getModule("RankElo");
            if (mod instanceof RankEloModule) {
                ((RankEloModule) mod).setCached(stats);
            }
        } catch (Exception ignored) {
            /* RankElo may be unregistered */
        }
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GameContext.Mode mode = GameContext.detect();
        boolean full = mode == GameContext.Mode.RANKED || mode == GameContext.Mode.BEDWARS;
        String text = buildDisplayText(full);

        if (!usePremiumRenderer()) {
            mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.ACCENT_GLOW);
            setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
            return;
        }

        beginHudScale();
        int lineH = hudLineHeight(mc);
        RankedStatsClient.RankedStats stats = cached;
        boolean twoLines = full && hasMatchFields(stats);

        if (twoLines) {
            String l1 = compactLine(stats);
            String l2 = matchLine(stats);
            int contentW = Math.max(measureHudText(mc, l1), measureHudText(mc, l2));
            int contentH = lineH * 2 + HudLayoutTokens.CARD_ROW_GAP;
            int cardW = Math.max(HudLayoutTokens.CARD_MIN_WIDTH, contentW + HudLayoutTokens.CARD_PADDING_X * 2);
            int cardH = contentH + HudLayoutTokens.CARD_PADDING_Y * 2;
            if (usePremiumCard()) {
                drawHudCard(hudX, hudY, cardW, cardH);
            }
            float tx = hudX + HudLayoutTokens.CARD_PADDING_X;
            float ty = hudY + HudLayoutTokens.CARD_PADDING_Y;
            drawHudAccentText(mc, l1, tx, ty, HudTheme.VALUE_ACCENT);
            drawHudText(mc, l2, tx, ty + lineH + HudLayoutTokens.CARD_ROW_GAP, HudTheme.VALUE);
            setHudSize(cardW, cardH);
        } else {
            int contentW = measureHudText(mc, text);
            int cardW = Math.max(HudLayoutTokens.CARD_MIN_WIDTH, contentW + HudLayoutTokens.CARD_PADDING_X * 2);
            int cardH = lineH + HudLayoutTokens.CARD_PADDING_Y * 2;
            if (usePremiumCard()) {
                drawHudCard(hudX, hudY, cardW, cardH);
            }
            drawHudAccentText(mc, text, hudX + HudLayoutTokens.CARD_PADDING_X, hudY + HudLayoutTokens.CARD_PADDING_Y, HudTheme.VALUE_ACCENT);
            setHudSize(cardW, cardH);
        }
        endHudScale();
    }

    private String buildDisplayText(boolean full) {
        if ("No API".equals(status) || "Offline".equals(status)) {
            return "RBW: " + status;
        }
        RankedStatsClient.RankedStats stats = cached;
        if (full) {
            return compactLine(stats) + (hasMatchFields(stats) ? "  " + matchLine(stats) : "");
        }
        // Lobby / unknown: ELO + rank only when available; suppress match noise
        return lobbyLine(stats);
    }

    private static String lobbyLine(RankedStatsClient.RankedStats stats) {
        StringBuilder sb = new StringBuilder("RBW");
        if (present(stats.elo)) {
            sb.append(": ").append(stats.elo).append(" ELO");
        } else if (present(stats.rank)) {
            sb.append(": ").append(stats.rank);
        } else {
            sb.append(": —");
        }
        if (present(stats.rank) && present(stats.elo)) {
            sb.append("  ").append(stats.rank);
        }
        return sb.toString();
    }

    private static String compactLine(RankedStatsClient.RankedStats stats) {
        StringBuilder sb = new StringBuilder();
        if (present(stats.elo)) {
            sb.append(stats.elo).append(" ELO");
        } else {
            sb.append("— ELO");
        }
        if (present(stats.rank)) {
            sb.append("  ").append(stats.rank);
        }
        return sb.toString();
    }

    private static String matchLine(RankedStatsClient.RankedStats stats) {
        return "W " + stats.wins + "  L " + stats.losses + "  WLR " + stats.wlr + "  S " + stats.streak;
    }

    private static boolean hasMatchFields(RankedStatsClient.RankedStats stats) {
        return present(stats.wins) || present(stats.losses) || present(stats.wlr) || present(stats.streak);
    }

    private static boolean present(String value) {
        return value != null && !value.isEmpty() && !"—".equals(value);
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
