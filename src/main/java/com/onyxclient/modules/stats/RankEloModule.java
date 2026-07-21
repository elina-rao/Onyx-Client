package com.onyxclient.modules.stats;

import com.onyxclient.core.RankedStatsClient;
import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.HudLayoutTokens;
import com.onyxclient.utils.HudMotion;
import com.onyxclient.utils.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class RankEloModule extends HudModule {

    private final BooleanSetting showIcon;
    private final ModeSetting viewMode;
    private RankedStatsClient.RankedStats cached = new RankedStatsClient.RankedStats();
    private float cardReveal;

    public RankEloModule() {
        super("RankElo", "Live rank/ELO display", false);
        showIcon = addSetting(new BooleanSetting("Show Rank Icon", true));
        viewMode = addSetting(new ModeSetting("View", "Compact", "Compact", "Expanded"));
        setUseScaledBounds(true);
        setHudSize(120, 22);
        setHudPosition(2, 340);
        // Keep legacy fallback available, but default to premium for ranked HUD.
        tryEnablePremiumDefaults();
    }

    public void setCached(RankedStatsClient.RankedStats value) {
        if (value != null) {
            this.cached = value;
        }
    }

    public void setCached(String elo) {
        RankedStatsClient.RankedStats stats = new RankedStatsClient.RankedStats();
        stats.elo = elo != null ? elo : "—";
        setCached(stats);
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        float target = usePremiumCard() ? 1.0F : 0.0F;
        float factor = reducedMotion() ? 1.0F : 0.18F;
        cardReveal = HudMotion.approach(cardReveal, target, factor);

        if (usePremiumRenderer()) {
            renderPremium(partialTicks);
        } else {
            renderLegacy(partialTicks);
        }
    }

    private void renderLegacy(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        float s = hudScale();
        GlStateManager.pushMatrix();
        GlStateManager.translate(hudX, hudY, 0);
        GlStateManager.scale(s, s, 1.0F);
        if ("Compact".equals(viewMode.getValue())) {
            String text = (showIcon.getValue() ? "◆ " : "") + "ELO " + cached.elo + "  " + cached.rank;
            drawHudText(mc, text, 0, 0, Colors.ACCENT_BRIGHT);
            setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
            GlStateManager.popMatrix();
            return;
        }
        String l1 = (showIcon.getValue() ? "◆ " : "") + "ELO " + cached.elo + "  " + cached.rank;
        String l2 = "W " + cached.wins + "  L " + cached.losses + "  WLR " + cached.wlr + "  S " + cached.streak;
        drawHudText(mc, l1, 0, 0, Colors.ACCENT_BRIGHT);
        drawHudText(mc, l2, 0, 10, Colors.TEXT_PRIMARY);
        int w = Math.max(mc.fontRendererObj.getStringWidth(l1), mc.fontRendererObj.getStringWidth(l2));
        setHudSize(w + 4, 22);
        GlStateManager.popMatrix();
    }

    private void renderPremium(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        beginHudScale();
        String l1 = (showIcon.getValue() ? "◆ " : "") + "ELO " + cached.elo + "  " + cached.rank;
        String l2 = "W " + cached.wins + "  L " + cached.losses + "  WLR " + cached.wlr + "  S " + cached.streak;
        int lineH = hudLineHeight(mc);

        int contentW = "Compact".equals(viewMode.getValue())
                ? measureHudText(mc, l1)
                : Math.max(measureHudText(mc, l1), measureHudText(mc, l2));
        int rows = "Compact".equals(viewMode.getValue()) ? 1 : 2;
        int contentH = rows * lineH + (rows - 1) * HudLayoutTokens.CARD_ROW_GAP;

        int cardW = Math.max(HudLayoutTokens.CARD_MIN_WIDTH, contentW + HudLayoutTokens.CARD_PADDING_X * 2);
        int cardH = contentH + HudLayoutTokens.CARD_PADDING_Y * 2;
        if (usePremiumCard() || cardReveal > 0.01F) {
            drawHudCard(hudX, hudY, cardW, cardH);
        }

        float tx = hudX + HudLayoutTokens.CARD_PADDING_X;
        float ty = hudY + HudLayoutTokens.CARD_PADDING_Y;
        drawHudAccentText(mc, l1, tx, ty, HudTheme.VALUE_ACCENT);
        if (rows > 1) {
            drawHudText(mc, l2, tx, ty + lineH + HudLayoutTokens.CARD_ROW_GAP, HudTheme.VALUE);
        }
        setHudSize(cardW, cardH);
        endHudScale();
    }

    private void tryEnablePremiumDefaults() {
        // Keep graceful fallback if settings are unavailable for any reason.
        try {
            for (com.onyxclient.modules.settings.Setting<?> setting : getSettings()) {
                if ("Premium Renderer".equals(setting.getName())) {
                    @SuppressWarnings("unchecked")
                    com.onyxclient.modules.settings.Setting<Boolean> b =
                            (com.onyxclient.modules.settings.Setting<Boolean>) setting;
                    b.setValue(true);
                }
                if ("Premium Card".equals(setting.getName())) {
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
