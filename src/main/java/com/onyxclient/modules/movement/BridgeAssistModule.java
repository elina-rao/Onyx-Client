package com.onyxclient.modules.movement;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;

/**
 * Ghost outline of next block placement while bridging. Visual only — no auto-place.
 */
public class BridgeAssistModule extends Module {

    private final ColorSetting outlineColor;
    private final NumberSetting opacity;
    private final BooleanSetting onlyWhenHoldingBlock;

    public BridgeAssistModule() {
        super("BridgeAssist", "Ghost outline for next block placement", ModuleCategory.MOVEMENT);
        outlineColor = addSetting(new ColorSetting("Outline Color", Colors.ACCENT_PRIMARY));
        opacity = addSetting(new NumberSetting("Opacity", 0.35, 0.1, 1.0, 0.05));
        onlyWhenHoldingBlock = addSetting(new BooleanSetting("Only Holding Block", true));
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (onlyWhenHoldingBlock.getValue()) {
            if (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)) {
                return;
            }
        }
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        BlockPos placePos = mop.getBlockPos().offset(mop.sideHit);
        if (!mc.theWorld.isAirBlock(placePos) && !mc.theWorld.getBlockState(placePos).getBlock().isReplaceable(mc.theWorld, placePos)) {
            return;
        }

        double x = placePos.getX() - mc.getRenderManager().viewerPosX;
        double y = placePos.getY() - mc.getRenderManager().viewerPosY;
        double z = placePos.getZ() - mc.getRenderManager().viewerPosZ;
        AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1).expand(0.002, 0.002, 0.002);

        int c = outlineColor.getValue();
        float r = ((c >> 16) & 0xFF) / 255F;
        float g = ((c >> 8) & 0xFF) / 255F;
        float b = (c & 0xFF) / 255F;
        float a = opacity.getFloatValue();

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        drawOutlinedBox(bb);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawOutlinedBox(AxisAlignedBB bb) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        tess.draw();
    }
}
