package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityTNTPrimed;
import org.lwjgl.opengl.GL11;

/**
 * Fuse countdown above primed TNT entities (Lunar/Badlion essential).
 */
public class TNTCountdownModule extends Module {

    private final BooleanSetting showSeconds;
    private final BooleanSetting throughWalls;
    private final ColorSetting color;
    private final ColorSetting dangerColor;
    private final NumberSetting dangerBelow;
    private final NumberSetting scale;

    public TNTCountdownModule() {
        super("TNTCountdown", "Fuse timer on primed TNT", ModuleCategory.BEDWARS, true);
        showSeconds = addSetting(new BooleanSetting("Show Seconds", true));
        throughWalls = addSetting(new BooleanSetting("Through Walls", true));
        color = addSetting(new ColorSetting("Color", Colors.DANGER));
        dangerColor = addSetting(new ColorSetting("Danger Color", 0xFFFF3333));
        dangerBelow = addSetting(new NumberSetting("Danger Below", 1.0, 0.2, 3.0, 0.1));
        scale = addSetting(new NumberSetting("Scale", 1.0, 0.5, 2.5, 0.1));
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        RenderManager rm = mc.getRenderManager();
        FontRenderer fr = mc.fontRendererObj;
        float userScale = scale.getFloatValue();
        float dangerAt = dangerBelow.getFloatValue();

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityTNTPrimed)) {
                continue;
            }
            EntityTNTPrimed tnt = (EntityTNTPrimed) obj;
            float seconds = Math.max(0.0F, tnt.fuse / 20.0F);
            String text;
            if (showSeconds.getValue()) {
                text = String.format("%.1fs", seconds);
            } else {
                text = String.valueOf((int) Math.ceil(seconds));
            }

            double x = tnt.lastTickPosX + (tnt.posX - tnt.lastTickPosX) * partialTicks - rm.viewerPosX;
            double y = tnt.lastTickPosY + (tnt.posY - tnt.lastTickPosY) * partialTicks - rm.viewerPosY
                    + tnt.height + 0.35D;
            double z = tnt.lastTickPosZ + (tnt.posZ - tnt.lastTickPosZ) * partialTicks - rm.viewerPosZ;

            float baseScale = 0.02666667F * userScale;
            int width = fr.getStringWidth(text);
            int drawColor = seconds <= dangerAt ? dangerColor.getValue() : color.getValue();

            GlStateManager.pushMatrix();
            GlStateManager.translate((float) x, (float) y, (float) z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(-baseScale, -baseScale, baseScale);
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            if (throughWalls.getValue()) {
                GlStateManager.disableDepth();
            }
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            fr.drawString(text, -width / 2, 0, drawColor);

            if (throughWalls.getValue()) {
                GlStateManager.enableDepth();
            }
            GlStateManager.depthMask(true);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }
}
