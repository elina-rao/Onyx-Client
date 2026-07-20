package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ResourceOverlayModule extends HudModule {

    private final ModeSetting layout;
    private final BooleanSetting showIron;
    private final BooleanSetting showGold;
    private final BooleanSetting showEmerald;
    private final BooleanSetting showDiamond;

    public ResourceOverlayModule() {
        super("ResourceOverlay", "Iron/gold/emerald/diamond counts", true);
        layout = addSetting(new ModeSetting("Layout", "Horizontal", "Horizontal", "Vertical", "Grid"));
        showIron = addSetting(new BooleanSetting("Show Iron", true));
        showGold = addSetting(new BooleanSetting("Show Gold", true));
        showEmerald = addSetting(new BooleanSetting("Show Emerald", true));
        showDiamond = addSetting(new BooleanSetting("Show Diamond", true));
        setHudSize(120, 14);
        setHudPosition(2, 80);
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
        int iron = count(Items.iron_ingot);
        int gold = count(Items.gold_ingot);
        int emerald = count(Items.emerald);
        int diamond = count(Items.diamond);

        boolean vertical = "Vertical".equals(layout.getValue()) || "Grid".equals(layout.getValue());
        int x = hudX;
        int y = hudY;
        int maxW = 0;
        int lines = 0;

        if (showIron.getValue()) {
            String t = "Iron: " + iron;
            mc.fontRendererObj.drawStringWithShadow(t, x, y, 0xFFD0D0D0);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(t));
            if (vertical) {
                y += 10;
                lines++;
            } else {
                x += mc.fontRendererObj.getStringWidth(t) + 8;
            }
        }
        if (showGold.getValue()) {
            String t = "Gold: " + gold;
            mc.fontRendererObj.drawStringWithShadow(t, x, y, 0xFFFFD700);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(t));
            if (vertical) {
                y += 10;
                lines++;
            } else {
                x += mc.fontRendererObj.getStringWidth(t) + 8;
            }
        }
        if (showEmerald.getValue()) {
            String t = "Em: " + emerald;
            mc.fontRendererObj.drawStringWithShadow(t, x, y, 0xFF50C878);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(t));
            if (vertical) {
                y += 10;
                lines++;
            } else {
                x += mc.fontRendererObj.getStringWidth(t) + 8;
            }
        }
        if (showDiamond.getValue()) {
            String t = "Dia: " + diamond;
            mc.fontRendererObj.drawStringWithShadow(t, x, y, 0xFF5B9BD5);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(t));
            if (vertical) {
                y += 10;
                lines++;
            } else {
                x += mc.fontRendererObj.getStringWidth(t) + 8;
            }
        }

        if (vertical) {
            setHudSize(maxW + 4, Math.max(12, lines * 10));
        } else {
            setHudSize(Math.max(40, x - hudX), 12);
        }
    }

    private int count(Item item) {
        Minecraft mc = Minecraft.getMinecraft();
        int total = 0;
        for (ItemStack stack : mc.thePlayer.inventory.mainInventory) {
            if (stack != null && stack.getItem() == item) {
                total += stack.stackSize;
            }
        }
        // Ender chest contents are not always synced client-side; count when available
        try {
            net.minecraft.inventory.InventoryEnderChest ender = mc.thePlayer.getInventoryEnderChest();
            if (ender != null) {
                for (int i = 0; i < ender.getSizeInventory(); i++) {
                    ItemStack stack = ender.getStackInSlot(i);
                    if (stack != null && stack.getItem() == item) {
                        total += stack.stackSize;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return total;
    }
}
