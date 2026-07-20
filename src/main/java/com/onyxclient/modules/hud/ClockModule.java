package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ClockModule extends HudModule {

    private final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

    public ClockModule() {
        super("Clock", "Real-world time display");
        setHudSize(70, 12);
        hudY = 112;
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        String text = format.format(new Date());
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.TEXT_MUTED);
        setHudSize(Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) + 4, 12);
    }
}
