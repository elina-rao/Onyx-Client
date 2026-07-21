package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import com.onyxclient.utils.HudLayoutTokens;
import com.onyxclient.utils.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

public class PotionStatusModule extends HudModule {

    public PotionStatusModule() {
        super("PotionStatus", "Active potion effects and timers", true);
        setUseScaledBounds(true);
        setHudSize(120, 40);
        hudY = 20;
        enablePremiumDefaults();
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        List<Line> lines = new ArrayList<Line>();
        for (PotionEffect effect : mc.thePlayer.getActivePotionEffects()) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) {
                continue;
            }
            String name = I18n.format(potion.getName());
            int seconds = effect.getDuration() / 20;
            int amp = effect.getAmplifier();
            String ampSuffix = amp > 0 ? " " + (amp + 1) : "";
            lines.add(new Line(name + ampSuffix + "  " + formatDuration(seconds), seconds));
        }
        if (lines.isEmpty()) {
            setHudSize(48, 12);
            return;
        }
        if (!usePremiumRenderer()) {
            renderLegacy(mc, lines);
            return;
        }
        renderPremium(mc, lines);
    }

    private void renderLegacy(Minecraft mc, List<Line> lines) {
        int y = hudY;
        int maxWidth = 80;
        for (Line line : lines) {
            mc.fontRendererObj.drawStringWithShadow(line.text, hudX, y, colorFor(line.seconds, Colors.TEXT_PRIMARY));
            maxWidth = Math.max(maxWidth, mc.fontRendererObj.getStringWidth(line.text));
            y += 10;
        }
        setHudSize(maxWidth + 4, Math.max(12, y - hudY));
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
            drawHudText(mc, line.text, tx, ty + i * (lineH + HudLayoutTokens.CARD_ROW_GAP),
                    colorFor(line.seconds, HudTheme.VALUE));
        }
        setHudSize(cardW, cardH);
        endHudScale();
    }

    private static int colorFor(int seconds, int base) {
        return seconds <= 10 ? Colors.DANGER : base;
    }

    private static String formatDuration(int seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        int m = seconds / 60;
        int s = seconds % 60;
        if (m > 0) {
            return m + ":" + (s < 10 ? "0" : "") + s;
        }
        return s + "s";
    }

    private static final class Line {
        private final String text;
        private final int seconds;

        private Line(String text, int seconds) {
            this.text = text;
            this.seconds = seconds;
        }
    }
}
