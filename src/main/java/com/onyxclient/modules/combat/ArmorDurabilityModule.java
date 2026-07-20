package com.onyxclient.modules.combat;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public class ArmorDurabilityModule extends HudModule {

    private final BooleanSetting showNumeric;
    private final ModeSetting iconStyle;

    public ArmorDurabilityModule() {
        super("ArmorDurability", "Own armor points and durability", true);
        showNumeric = addSetting(new BooleanSetting("Show Numeric", true));
        iconStyle = addSetting(new ModeSetting("Icon Style", "Compact", "Compact", "Detailed"));
        setHudSize(80, 40);
        setHudPosition(2, 180);
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
        int maxW = 40;
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.thePlayer.inventory.armorInventory[i];
            if (stack == null) {
                continue;
            }
            String name = stack.getDisplayName();
            if ("Compact".equals(iconStyle.getValue()) && name.length() > 10) {
                name = name.substring(0, 10);
            }
            int color = Colors.TEXT_PRIMARY;
            if (stack.isItemStackDamageable()) {
                float pct = 1.0F - (float) stack.getItemDamage() / (float) stack.getMaxDamage();
                if (pct < 0.25F) {
                    color = Colors.DANGER;
                } else if (pct < 0.5F) {
                    color = 0xFFFFAA00;
                }
            }
            String line = name;
            if (showNumeric.getValue() && stack.isItemStackDamageable()) {
                int left = stack.getMaxDamage() - stack.getItemDamage();
                line = name + " " + left;
            }
            mc.fontRendererObj.drawStringWithShadow(line, hudX, y, color);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(line) + 4);
            y += 10;
        }
        int armor = mc.thePlayer.getTotalArmorValue();
        String armorLine = "Armor: " + armor;
        mc.fontRendererObj.drawStringWithShadow(armorLine, hudX, y, Colors.ACCENT_BRIGHT);
        maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(armorLine) + 4);
        setHudSize(maxW, y - hudY + 12);
    }
}
