package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class SpeedModule extends HudModule {

    private double lastX, lastY, lastZ;
    private double speed;

    public SpeedModule() {
        super("Speed", "Movement speed in blocks per second");
        setHudSize(80, 12);
        hudY = 146;
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
        double dx = mc.thePlayer.posX - lastX;
        double dy = mc.thePlayer.posY - lastY;
        double dz = mc.thePlayer.posZ - lastZ;
        speed = MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz) * 20.0;
        lastX = mc.thePlayer.posX;
        lastY = mc.thePlayer.posY;
        lastZ = mc.thePlayer.posZ;

        String text = String.format("Speed: %.2f b/s", speed);
        mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.SUCCESS);
        setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
    }
}
