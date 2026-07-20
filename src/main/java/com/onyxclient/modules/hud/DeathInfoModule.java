package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class DeathInfoModule extends HudModule {

    private int deathX;
    private int deathY;
    private int deathZ;
    private boolean hasDeath;

    public DeathInfoModule() {
        super("DeathInfo", "Show own death coordinates on death screen", true);
        setHudSize(120, 12);
        setHudPosition(2, 280);
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        if (mc.thePlayer.getHealth() <= 0 && !hasDeath) {
            deathX = (int) Math.floor(mc.thePlayer.posX);
            deathY = (int) Math.floor(mc.thePlayer.posY);
            deathZ = (int) Math.floor(mc.thePlayer.posZ);
            hasDeath = true;
        }
        if (mc.thePlayer.getHealth() > 0) {
            // keep last death coords until next death
        }
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled() || !hasDeath) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.getHealth() > 0) {
            // Still show briefly? Only on death screen
            if (!(mc.currentScreen instanceof net.minecraft.client.gui.GuiGameOver)) {
                return;
            }
        }
        String text = "Death: " + deathX + ", " + deathY + ", " + deathZ;
        mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.DANGER);
        setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
    }
}
