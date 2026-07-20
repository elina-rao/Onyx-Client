package com.onyxclient.modules.movement;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;

public class CompassModule extends HudModule {

    private final ModeSetting style;

    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public CompassModule() {
        super("Compass", "Facing direction / compass heading", true);
        style = addSetting(new ModeSetting("Style", "Text", "Text", "Graphical"));
        setHudSize(40, 12);
        setHudPosition(2, 220);
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
        float yaw = mc.thePlayer.rotationYaw;
        yaw = (yaw % 360 + 360) % 360;
        int index = (int) Math.floor((yaw + 22.5) / 45.0) % 8;
        String dir = DIRS[index];
        String text;
        if ("Graphical".equals(style.getValue())) {
            text = "[" + dir + "] " + (int) yaw + "°";
        } else {
            text = dir + " " + (int) yaw + "°";
        }
        mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.TEXT_PRIMARY);
        setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
    }
}
