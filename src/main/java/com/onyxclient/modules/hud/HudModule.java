package com.onyxclient.modules.hud;

import com.onyxclient.OnyxClient;
import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.HudFontRenderer;
import com.onyxclient.utils.HudLayoutTokens;
import com.onyxclient.utils.HudTheme;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;

public abstract class HudModule extends Module {

    protected int hudX = 2;
    protected int hudY = 2;
    protected int hudWidth = 80;
    protected int hudHeight = 20;
    private final NumberSetting hudScaleSetting;
    private final BooleanSetting textShadowSetting;
    private final BooleanSetting modernRendererSetting;
    private final BooleanSetting premiumCardSetting;
    private final BooleanSetting reducedMotionSetting;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean useScaledBounds;

    protected HudModule(String name, String description) {
        this(name, description, false);
    }

    protected HudModule(String name, String description, boolean defaultEnabled) {
        super(name, description, ModuleCategory.HUD, defaultEnabled);
        hudScaleSetting = addSetting(new NumberSetting("HUD Scale", 1.0, 0.5, 2.0, 0.1));
        textShadowSetting = addSetting(new BooleanSetting("Text Shadow", true));
        modernRendererSetting = addSetting(new BooleanSetting("Premium Renderer", true));
        premiumCardSetting = addSetting(new BooleanSetting("Premium Card", true));
        reducedMotionSetting = addSetting(new BooleanSetting("Reduced Motion", false));
    }

    public int getHudX() {
        return hudX;
    }

    public int getHudY() {
        return hudY;
    }

    public int getHudWidth() {
        if (!useScaledBounds) {
            return hudWidth;
        }
        return Math.max(1, Math.round(hudWidth * hudScale()));
    }

    public int getHudHeight() {
        if (!useScaledBounds) {
            return hudHeight;
        }
        return Math.max(1, Math.round(hudHeight * hudScale()));
    }

    public void setHudPosition(int x, int y) {
        this.hudX = x;
        this.hudY = y;
    }

    public void setHudSize(int width, int height) {
        this.hudWidth = width;
        this.hudHeight = height;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void startDrag(int mouseX, int mouseY) {
        dragging = true;
        dragOffsetX = mouseX - hudX;
        dragOffsetY = mouseY - hudY;
    }

    public void stopDrag() {
        if (dragging) {
            dragging = false;
            OnyxClient.getConfigManager().saveHudPosition(this);
        }
    }

    public void drag(int mouseX, int mouseY, boolean snapToGrid) {
        if (!dragging) {
            return;
        }
        hudX = mouseX - dragOffsetX;
        hudY = mouseY - dragOffsetY;
        if (snapToGrid) {
            hudX = (hudX / 4) * 4;
            hudY = (hudY / 4) * 4;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        hudX = Math.max(0, Math.min(hudX, sr.getScaledWidth() - getHudWidth()));
        hudY = Math.max(0, Math.min(hudY, sr.getScaledHeight() - getHudHeight()));
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= hudX && mouseX <= hudX + getHudWidth() && mouseY >= hudY && mouseY <= hudY + getHudHeight();
    }

    public float hudScale() {
        return hudScaleSetting.getFloatValue();
    }

    public boolean hudTextShadow() {
        return textShadowSetting.getValue();
    }

    protected boolean usePremiumRenderer() {
        return modernRendererSetting.getValue();
    }

    protected boolean usePremiumCard() {
        return premiumCardSetting.getValue();
    }

    protected boolean reducedMotion() {
        return reducedMotionSetting.getValue();
    }

    protected void setUseScaledBounds(boolean value) {
        this.useScaledBounds = value;
    }

    protected int measureHudText(Minecraft mc, String text) {
        if (usePremiumRenderer()) {
            return HudFontRenderer.regular().width(text);
        }
        return mc.fontRendererObj.getStringWidth(text);
    }

    protected int hudLineHeight(Minecraft mc) {
        if (usePremiumRenderer()) {
            return HudFontRenderer.regular().lineHeight();
        }
        return mc.fontRendererObj.FONT_HEIGHT;
    }

    protected void drawHudText(Minecraft mc, String text, float x, float y, int color) {
        if (usePremiumRenderer()) {
            HudFontRenderer.regular().draw(text, x, y, color, hudTextShadow());
            return;
        }
        if (hudTextShadow()) {
            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        } else {
            mc.fontRendererObj.drawString(text, (int) x, (int) y, color, false);
        }
    }

    protected void drawHudAccentText(Minecraft mc, String text, float x, float y, int color) {
        if (usePremiumRenderer()) {
            HudFontRenderer.accent().draw(text, x, y, color, hudTextShadow());
            return;
        }
        drawHudText(mc, text, x, y, color);
    }

    protected void drawHudCard(int x, int y, int width, int height) {
        RenderUtils.drawSoftShadow(x, y, width, height, HudLayoutTokens.CARD_RADIUS, 4);
        RenderUtils.drawRoundedRect(x, y, width, height, HudLayoutTokens.CARD_RADIUS, HudTheme.CARD_BG);
        RenderUtils.drawRoundedOutline(x, y, width, height, HudLayoutTokens.CARD_RADIUS, 1.0F, HudTheme.CARD_BORDER);
    }

    protected void beginHudScale() {
        if (hudScale() == 1.0F) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(hudX, hudY, 0);
        GlStateManager.scale(hudScale(), hudScale(), 1.0F);
        GlStateManager.translate(-hudX, -hudY, 0);
    }

    protected void endHudScale() {
        if (hudScale() == 1.0F) {
            return;
        }
        GlStateManager.popMatrix();
    }

    /** Force premium renderer + card ON (legacy toggles remain for fallback). */
    protected void enablePremiumDefaults() {
        try {
            modernRendererSetting.setValue(true);
            premiumCardSetting.setValue(true);
        } catch (Exception ignored) {
        }
    }
}
