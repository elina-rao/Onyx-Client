package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public class ArmorStatusModule extends HudModule {

    public ArmorStatusModule() {
        super("ArmorStatus", "Display armor durability");
        setHudSize(80, 20);
        hudY = 40;
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
        int x = hudX;
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.thePlayer.inventory.armorInventory[i];
            if (stack != null) {
                mc.getRenderItem().renderItemIntoGUI(stack, x, hudY);
                if (stack.isItemStackDamageable()) {
                    int max = stack.getMaxDamage();
                    int dmg = max - stack.getItemDamage();
                    int pct = (int) ((dmg / (float) max) * 100);
                    mc.fontRendererObj.drawString(pct + "%", x, hudY + 18, Colors.TEXT_MUTED);
                }
                x += 20;
            }
        }
        setHudSize(Math.max(80, x - hudX), 36);
    }
}
