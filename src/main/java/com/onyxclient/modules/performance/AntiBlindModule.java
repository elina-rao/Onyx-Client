package com.onyxclient.modules.performance;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AntiBlindModule extends Module {

    public AntiBlindModule() {
        super("AntiBlind", "Remove blindness and darkness effects", ModuleCategory.PERFORMANCE);
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        mc.thePlayer.removePotionEffect(Potion.blindness.id);
        mc.thePlayer.removePotionEffect(Potion.confusion.id);
        for (PotionEffect effect : mc.thePlayer.getActivePotionEffects()) {
            if (effect.getPotionID() == 15 || effect.getPotionID() == 16) {
                mc.thePlayer.removePotionEffect(effect.getPotionID());
            }
        }
    }
}
