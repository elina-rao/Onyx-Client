package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class BlockOverlayModule extends HudModule {

    private final NumberSetting fontSize;

    public BlockOverlayModule() {
        super("BlockOverlay", "Held block count while building", true);
        fontSize = addSetting(new NumberSetting("Font Scale", 1.0, 0.8, 2.0, 0.1));
        setHudSize(60, 12);
        setHudPosition(2, 240);
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
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) {
            return;
        }
        int count = 0;
        for (ItemStack stack : mc.thePlayer.inventory.mainInventory) {
            if (stack != null && stack.getItem() == held.getItem()
                    && stack.getMetadata() == held.getMetadata()) {
                count += stack.stackSize;
            }
        }
        String text = "Blocks: " + count;
        float scale = fontSize.getFloatValue();
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.translate(hudX, hudY, 0);
        net.minecraft.client.renderer.GlStateManager.scale(scale, scale, 1F);
        mc.fontRendererObj.drawStringWithShadow(text, 0, 0, Colors.TEXT_PRIMARY);
        net.minecraft.client.renderer.GlStateManager.popMatrix();
        setHudSize((int) (mc.fontRendererObj.getStringWidth(text) * scale) + 4, (int) (12 * scale));
    }
}
