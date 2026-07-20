package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class PotionStatusModule extends HudModule {

    public PotionStatusModule() {
        super("PotionStatus", "Active potion effects and timers", true);
        setHudSize(120, 40);
        hudY = 20;
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
        int y = hudY;
        int maxWidth = 80;
        for (PotionEffect effect : mc.thePlayer.getActivePotionEffects()) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            String name = I18n.format(potion.getName());
            int seconds = effect.getDuration() / 20;
            String text = name + " " + seconds + "s";
            mc.fontRendererObj.drawStringWithShadow(text, hudX, y, Colors.TEXT_PRIMARY);
            maxWidth = Math.max(maxWidth, mc.fontRendererObj.getStringWidth(text));
            y += 10;
        }
        setHudSize(maxWidth + 4, Math.max(12, y - hudY));
    }
}
