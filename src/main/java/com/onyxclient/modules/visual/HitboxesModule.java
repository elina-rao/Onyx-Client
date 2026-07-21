package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

/**
 * Outlines entity hitboxes for players and projectiles.
 */
public class HitboxesModule extends Module {

    private final BooleanSetting showPlayers;
    private final BooleanSetting showProjectiles;
    private final BooleanSetting showItems;
    private final ColorSetting playerColor;
    private final ColorSetting projectileColor;
    private final ColorSetting itemColor;
    private final NumberSetting lineWidth;

    public HitboxesModule() {
        super("Hitboxes", "Outline entity hitboxes (players and projectiles)", ModuleCategory.VISUAL, true);
        showPlayers = addSetting(new BooleanSetting("Show Players", true));
        showProjectiles = addSetting(new BooleanSetting("Show Projectiles", true));
        showItems = addSetting(new BooleanSetting("Show Items", false));
        playerColor = addSetting(new ColorSetting("Player Color", Colors.ACCENT_BRIGHT));
        projectileColor = addSetting(new ColorSetting("Projectile Color", Colors.DANGER));
        itemColor = addSetting(new ColorSetting("Item Color", 0xFFFFFF55));
        lineWidth = addSetting(new NumberSetting("Line Width", 2.0, 1.0, 5.0, 0.1));
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

        double rx = mc.getRenderManager().viewerPosX;
        double ry = mc.getRenderManager().viewerPosY;
        double rz = mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GL11.glLineWidth(lineWidth.getFloatValue());

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof Entity)) {
                continue;
            }
            Entity entity = (Entity) obj;
            if (entity == mc.thePlayer) {
                continue;
            }
            int color = resolveColor(entity);
            if (color == 0) {
                continue;
            }
            AxisAlignedBB bb = entity.getEntityBoundingBox();
            if (bb == null) {
                continue;
            }
            // Interpolate so boxes track smoothly between ticks
            double ix = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
            double iy = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
            double iz = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
            double dx = ix - entity.posX;
            double dy = iy - entity.posY;
            double dz = iz - entity.posZ;
            AxisAlignedBB shifted = bb.offset(dx, dy, dz)
                    .expand(0.002D, 0.002D, 0.002D)
                    .offset(-rx, -ry, -rz);
            drawOutlinedBox(shifted, color);
        }

        GL11.glLineWidth(1.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /** @return ARGB color, or 0 to skip */
    private int resolveColor(Entity entity) {
        if (entity instanceof EntityPlayer) {
            return showPlayers.getValue() ? playerColor.getValue() : 0;
        }
        if (entity instanceof EntityArrow || entity instanceof EntityFireball || entity instanceof EntityThrowable) {
            return showProjectiles.getValue() ? projectileColor.getValue() : 0;
        }
        if (entity instanceof EntityItem) {
            return showItems.getValue() ? itemColor.getValue() : 0;
        }
        return 0;
    }

    private void drawOutlinedBox(AxisAlignedBB bb, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (a == 0) {
            a = 255;
        }
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        addOutlinedVertices(wr, bb, r, g, b, a);
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
