package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BlockCounterModule extends HudModule {

    private final BooleanSetting resetPerGame;
    private int placed;

    public BlockCounterModule() {
        super("BlockCounter", "Counts blocks placed this game/session", false);
        resetPerGame = addSetting(new BooleanSetting("Reset Per Game", true));
        setHudSize(90, 12);
        setHudPosition(2, 232);
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (!isEnabled() || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || event.entityPlayer != mc.thePlayer) {
            return;
        }
        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemBlock) {
            placed++;
        }
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null && resetPerGame.getValue() && mc.theWorld.getScoreboard() != null
                && mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(1) != null) {
            String title = mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(1).getDisplayName();
            if (title != null && title.toLowerCase().contains("lobby")) {
                placed = 0;
            }
        }
        String text = "Blocks Placed: " + placed;
        mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.TEXT_PRIMARY);
        setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
    }
}
