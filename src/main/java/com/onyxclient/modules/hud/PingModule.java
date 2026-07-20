package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;

public class PingModule extends HudModule {

    public PingModule() {
        super("Ping", "Current server ping");
        setHudSize(60, 12);
        hudY = 96;
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getCurrentServerData() == null) {
            return;
        }
        String text = "Ping: " + mc.getCurrentServerData().pingToServer + "ms";
        mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.TEXT_PRIMARY);
        setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
    }
}
