package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;

public class FPSCounterModule extends HudModule {

    public FPSCounterModule() {
        super("FPSCounter", "Display current FPS", true);
        setHudSize(60, 12);
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        String text = mc.debug.split(" ")[0] + " FPS";
        mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.ACCENT_BRIGHT);
        setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
    }
}
