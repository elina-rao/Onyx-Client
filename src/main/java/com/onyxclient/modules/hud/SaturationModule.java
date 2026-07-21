package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;

/**
 * Displays food saturation (display-only).
 */
public class SaturationModule extends HudModule {

    public SaturationModule() {
        super("Saturation", "Food saturation bar", true);
        setHudSize(70, 10);
        setHudPosition(2, 120);
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
        float sat = mc.thePlayer.getFoodStats().getSaturationLevel();
        float pct = Math.min(1.0F, sat / 20.0F);
        int w = 70;
        int h = 6;
        RenderUtils.drawRect(hudX, hudY, w, h, Colors.withAlpha(0x000000, 140));
        RenderUtils.drawRect(hudX, hudY, (int) (w * pct), h, 0xFFE6A23C);
        String text = String.format("Sat %.1f", sat);
        mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY + 8, Colors.TEXT_PRIMARY);
        setHudSize(w, 18);
    }
}
