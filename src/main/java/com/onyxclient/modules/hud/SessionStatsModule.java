package com.onyxclient.modules.hud;

import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.GameContext;
import com.onyxclient.utils.HudLayoutTokens;
import com.onyxclient.utils.HudTheme;
import com.onyxclient.modules.bedwars.HypixelBedwarsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Locale;

public class SessionStatsModule extends HudModule {

    private final BooleanSetting resetPerGame;

    private int kills;
    private int finalKills;
    private int deaths;
    private int beds;

    public SessionStatsModule() {
        super("SessionStats", "Live kills/deaths/beds for session", true);
        resetPerGame = addSetting(new BooleanSetting("Reset Per Game", true));
        setUseScaledBounds(true);
        setHudSize(90, 36);
        setHudPosition(2, 120);
        enablePremiumDefaults();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled()) {
            return;
        }
        String raw = event.message.getUnformattedText();
        String msg = raw.toLowerCase(Locale.US);
        Minecraft mc = Minecraft.getMinecraft();
        String self = mc.thePlayer != null ? mc.thePlayer.getName().toLowerCase(Locale.US) : "";
        if (self.isEmpty()) {
            return;
        }

        if (resetPerGame.getValue() && (msg.contains("the game starts in 1 second")
                || msg.contains("protect your bed")
                || msg.contains("sending you to"))) {
            kills = 0;
            finalKills = 0;
            deaths = 0;
            beds = 0;
        }

        if (msg.contains(self + " was") && (msg.contains("killed by") || msg.contains("fell") || msg.contains("void"))) {
            deaths++;
        }
        // Final kills first so they do not also increment normal kills.
        // Do NOT use startsWith(self) — "<you> was killed by X. FINAL KILL!" is your death.
        boolean isFinalKill = msg.contains("final kill")
                && (msg.contains(" was killed by " + self)
                || msg.contains(" killed by " + self)
                || msg.contains(self + " final killed "));
        if (isFinalKill) {
            finalKills++;
        } else if (msg.contains(" was killed by " + self) || msg.startsWith(self + " killed ")) {
            kills++;
        }
        if (msg.contains("bed was destroyed by " + self) || msg.contains(self + " destroyed")
                || msg.startsWith(self + " broke")) {
            beds++;
        }
        if (resetPerGame.getValue() && GameContext.detect() == GameContext.Mode.LOBBY
                && (msg.contains("you died") || msg.contains("winner"))) {
            // Keep session stats in lobby unless explicit reset phrases above.
        }
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        HypixelBedwarsModule hub = HypixelBedwarsModule.INSTANCE;
        if (hub != null && hub.isEnabled() && hub.isInBedwars() && !hub.showStatsHud()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        String[] lines = {
                "K  " + kills,
                "FK " + finalKills,
                "D  " + deaths,
                "Beds " + beds
        };
        if (!usePremiumRenderer()) {
            renderLegacy(mc, lines);
            return;
        }
        renderPremium(mc, lines);
    }

    private void renderLegacy(Minecraft mc, String[] lines) {
        float s = hudScale();
        GlStateManager.pushMatrix();
        GlStateManager.translate(hudX, hudY, 0);
        GlStateManager.scale(s, s, 1.0F);
        int y = 0;
        int maxW = 40;
        for (String line : lines) {
            drawHudText(mc, line, 0, y, Colors.TEXT_PRIMARY);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(line) + 4);
            y += 10;
        }
        GlStateManager.popMatrix();
        setHudSize((int) (maxW * s), (int) (40 * s));
    }

    private void renderPremium(Minecraft mc, String[] lines) {
        beginHudScale();
        int lineH = hudLineHeight(mc);
        int contentW = 0;
        for (String line : lines) {
            contentW = Math.max(contentW, measureHudText(mc, line));
        }
        int contentH = lines.length * lineH + (lines.length - 1) * HudLayoutTokens.CARD_ROW_GAP;
        int cardW = Math.max(HudLayoutTokens.CARD_MIN_WIDTH, contentW + HudLayoutTokens.CARD_PADDING_X * 2);
        int cardH = contentH + HudLayoutTokens.CARD_PADDING_Y * 2;
        if (usePremiumCard()) {
            drawHudCard(hudX, hudY, cardW, cardH);
        }
        float tx = hudX + HudLayoutTokens.CARD_PADDING_X;
        float ty = hudY + HudLayoutTokens.CARD_PADDING_Y;
        for (int i = 0; i < lines.length; i++) {
            int color = i == 0 ? HudTheme.VALUE_ACCENT : HudTheme.VALUE;
            drawHudText(mc, lines[i], tx, ty + i * (lineH + HudLayoutTokens.CARD_ROW_GAP), color);
        }
        setHudSize(cardW, cardH);
        endHudScale();
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getBeds() {
        return beds;
    }

    public int getFinalKills() {
        return finalKills;
    }
}
