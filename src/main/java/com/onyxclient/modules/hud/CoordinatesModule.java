package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;

public class CoordinatesModule extends HudModule {

    public CoordinatesModule() {
        super("Coordinates", "XYZ coordinate display");
        setHudSize(100, 12);
        hudY = 80;
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
        String text = String.format("XYZ: %.1f / %.1f / %.1f", mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.TEXT_MUTED);
        setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
    }
}
