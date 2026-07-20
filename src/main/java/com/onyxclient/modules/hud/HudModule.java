package com.onyxclient.modules.hud;

import com.onyxclient.OnyxClient;
import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import net.minecraft.client.Minecraft;

public abstract class HudModule extends Module {

    protected int hudX = 2;
    protected int hudY = 2;
    protected int hudWidth = 80;
    protected int hudHeight = 20;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    protected HudModule(String name, String description) {
        this(name, description, false);
    }

    protected HudModule(String name, String description, boolean defaultEnabled) {
        super(name, description, ModuleCategory.HUD, defaultEnabled);
    }

    public int getHudX() {
        return hudX;
    }

    public int getHudY() {
        return hudY;
    }

    public int getHudWidth() {
        return hudWidth;
    }

    public int getHudHeight() {
        return hudHeight;
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
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        hudX = Math.max(0, Math.min(hudX, sr.getScaledWidth() - hudWidth));
        hudY = Math.max(0, Math.min(hudY, sr.getScaledHeight() - hudHeight));
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= hudX && mouseX <= hudX + hudWidth && mouseY >= hudY && mouseY <= hudY + hudHeight;
    }
}
