package com.onyxclient.modules.hud;

import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

public class CooldownTimersModule extends HudModule {

    private final BooleanSetting pearl;
    private final BooleanSetting gapple;
    private final ModeSetting style;

    private final Map<String, Long> usedAt = new HashMap<String, Long>();
    private static final long PEARL_MS = 1000L;

    public CooldownTimersModule() {
        super("CooldownTimers", "Ender pearl / golden apple use timers", true);
        pearl = addSetting(new BooleanSetting("Ender Pearl", true));
        gapple = addSetting(new BooleanSetting("Golden Apple", true));
        style = addSetting(new ModeSetting("Style", "Text", "Text", "Icon"));
        setHudSize(100, 24);
        setHudPosition(2, 300);
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (!isEnabled() || event.entityPlayer == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (event.entityPlayer != mc.thePlayer) {
            return;
        }
        ItemStack stack = event.entityPlayer.getHeldItem();
        if (stack == null) {
            return;
        }
        Item item = stack.getItem();
        if (item == Items.ender_pearl && pearl.getValue()) {
            usedAt.put("pearl", System.currentTimeMillis());
        } else if (item == Items.golden_apple && gapple.getValue()) {
            usedAt.put("gapple", System.currentTimeMillis());
        }
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        long now = System.currentTimeMillis();
        Long p = usedAt.get("pearl");
        if (p != null && now - p > PEARL_MS + 5000) {
            usedAt.remove("pearl");
        }
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.currentTimeMillis();
        int y = hudY;
        int maxW = 40;

        if (pearl.getValue() && usedAt.containsKey("pearl")) {
            long left = Math.max(0, PEARL_MS - (now - usedAt.get("pearl")));
            String prefix = "Icon".equals(style.getValue()) ? "EP " : "Pearl: ";
            String t = prefix + String.format("%.1fs", left / 1000.0);
            int color = left > 0 ? Colors.DANGER : Colors.SUCCESS;
            mc.fontRendererObj.drawStringWithShadow(t, hudX, y, color);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(t) + 4);
            y += 10;
        }
        if (gapple.getValue() && usedAt.containsKey("gapple")) {
            long ago = now - usedAt.get("gapple");
            String prefix = "Icon".equals(style.getValue()) ? "GA " : "Gapple: ";
            String t = prefix + String.format("%.1fs", ago / 1000.0) + " ago";
            mc.fontRendererObj.drawStringWithShadow(t, hudX, y, Colors.TEXT_MUTED);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(t) + 4);
            y += 10;
        }
        if (y == hudY) {
            return;
        }
        setHudSize(maxW, y - hudY);
    }
}
