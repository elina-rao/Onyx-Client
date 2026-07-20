package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ComboCounterModule extends HudModule {

    private int combo;
    private long lastHitTime;

    public ComboCounterModule() {
        super("Combo Counter", "PvP hit combo tracker");
        setHudSize(70, 12);
        hudX = 4;
        hudY = 130;
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (event.entityPlayer != mc.thePlayer) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastHitTime > 2000L) {
            combo = 0;
        }
        combo++;
        lastHitTime = now;
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (System.currentTimeMillis() - lastHitTime > 2000L) {
            combo = 0;
        }
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled() || combo <= 1) {
            return;
        }
        String text = combo + " Combo";
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.ACCENT_BRIGHT);
        setHudSize(Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) + 4, 12);
    }
}
