package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;

public class BlockOverlayModule extends HudModule {

    private static final String MODE_OUTLINE = "Outline";
    private static final String MODE_FILL = "Fill";
    private static final String MODE_FULL = "Full";

    private final NumberSetting fontSize;
    private final BooleanSetting overlayEnabled;
    private final BooleanSetting outlineEnabled;
    private final BooleanSetting fillEnabled;
    private final ColorSetting outlineColor;
    private final ColorSetting fillColor;
    private final NumberSetting lineWidth;
    private final NumberSetting fillOpacity;
    private final ModeSetting overlayMode;

    public BlockOverlayModule() {
        super("BlockOverlay", "Held block count while building", true);
        fontSize = addSetting(new NumberSetting("Font Scale", 1.0, 0.8, 2.0, 0.1));
        overlayEnabled = addSetting(new BooleanSetting("Overlay Enabled", true));
        outlineEnabled = addSetting(new BooleanSetting("Outline", true));
        fillEnabled = addSetting(new BooleanSetting("Fill", true));
        outlineColor = addSetting(new ColorSetting("Outline Color", 0xFFAA5AFF));
        fillColor = addSetting(new ColorSetting("Fill Color", 0xFFAA5AFF));
        lineWidth = addSetting(new NumberSetting("Line Width", 2.0, 1.0, 5.0, 0.1));
        fillOpacity = addSetting(new NumberSetting("Fill Opacity", 0.20, 0.05, 1.0, 0.05));
        overlayMode = addSetting(new ModeSetting("Overlay Mode", MODE_FULL, MODE_OUTLINE, MODE_FILL, MODE_FULL));
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

    @Override
    public void onRender3D(float partialTicks) {
        if (!isEnabled() || !overlayEnabled.getValue()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mc.theWorld == null || mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        BlockPos pos = mop.getBlockPos();
        AxisAlignedBB bb = mc.theWorld.getBlockState(pos).getBlock().getSelectedBoundingBox(mc.theWorld, pos);
        if (bb == null) {
            return;
        }
        double rx = mc.getRenderManager().viewerPosX;
        double ry = mc.getRenderManager().viewerPosY;
        double rz = mc.getRenderManager().viewerPosZ;
        AxisAlignedBB shifted = bb.expand(0.002D, 0.002D, 0.002D).offset(-rx, -ry, -rz);
        boolean drawFill = shouldRenderFill();
        boolean drawOutline = shouldRenderOutline();
        if (!drawFill && !drawOutline) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        if (drawFill) {
            GlStateManager.depthMask(false);
            drawFilledBox(shifted, fillColor.getValue(), fillOpacity.getFloatValue());
            GlStateManager.depthMask(true);
        }
        if (drawOutline) {
            GL11.glLineWidth(lineWidth.getFloatValue());
            drawOutlinedBox(shifted, outlineColor.getValue());
        }
        GL11.glLineWidth(1.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private boolean shouldRenderOutline() {
        if (!outlineEnabled.getValue()) {
            return false;
        }
        String mode = overlayMode.getValue();
        return MODE_OUTLINE.equals(mode) || MODE_FULL.equals(mode);
    }

    private boolean shouldRenderFill() {
        if (!fillEnabled.getValue()) {
            return false;
        }
        String mode = overlayMode.getValue();
        return MODE_FILL.equals(mode) || MODE_FULL.equals(mode);
    }

    private void drawOutlinedBox(AxisAlignedBB bb, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        addOutlinedVertices(wr, bb, r, g, b, a);
        tess.draw();
    }

    /** Full-block translucent fill (all 6 faces). Depth test stays enabled so fill does not draw through occluders. */
    private void drawFilledBox(AxisAlignedBB bb, int color, float opacityScale) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int alpha = Math.max(0, Math.min(255, Math.round(((color >> 24) & 0xFF) * opacityScale)));
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        // Down
        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, alpha).endVertex();
        // Up
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, alpha).endVertex();
        // North
        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, alpha).endVertex();
        // South
        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, alpha).endVertex();
        // West
        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, alpha).endVertex();
        // East
        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, alpha).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, alpha).endVertex();

        tess.draw();
    }

    private void addOutlinedVertices(WorldRenderer wr, AxisAlignedBB bb, int r, int g, int b, int a) {
        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
    }
}
