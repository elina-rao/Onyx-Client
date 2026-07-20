package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Styled nametags. Cancels vanilla nameplates via Forge event (mixins are not loaded).
 */
public class NametagsModule extends Module {

    public static NametagsModule INSTANCE;

    public final BooleanSetting healthBar = addSetting(new BooleanSetting("Health Bar", true));
    public final BooleanSetting showSelf = addSetting(new BooleanSetting("Show Self (F5)", true));

    public NametagsModule() {
        super("Nametags", "Styled nametags with health bars", ModuleCategory.VISUAL);
        INSTANCE = this;
    }

    @SubscribeEvent
    public void onRenderNameplate(RenderLivingEvent.Specials.Pre event) {
        if (!isEnabled()) {
            return;
        }
        if (event.entity instanceof EntityPlayer) {
            event.setCanceled(true);
        }
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

        for (Object obj : mc.theWorld.playerEntities) {
            EntityPlayer player = (EntityPlayer) obj;
            if (player.isInvisible()) {
                continue;
            }
            if (player == mc.thePlayer) {
                if (!showSelf.getValue() || mc.gameSettings.thirdPersonView == 0) {
                    continue;
                }
            }
            if (mc.thePlayer.getDistanceToEntity(player) > 64.0F) {
                continue;
            }
            renderNametag(mc, player, partialTicks);
        }
    }

    private void renderNametag(Minecraft mc, EntityPlayer player, float partialTicks) {
        RenderManager rm = mc.getRenderManager();
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - rm.viewerPosX;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - rm.viewerPosY
                + player.height + 0.4;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - rm.viewerPosZ;

        FontRenderer fr = mc.fontRendererObj;
        String name = player.getDisplayName().getFormattedText();
        int nameW = fr.getStringWidth(name);

        float scale = 0.02666667F;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int pad = 4;
        int boxW = nameW + pad * 2;
        int boxH = healthBar.getValue() ? 16 : 10;
        int boxX = -boxW / 2;
        int boxY = -boxH;

        drawQuad(boxX, boxY, boxW, boxH, Colors.withAlpha(Colors.BG_CARD, 160));
        fr.drawString(name, -nameW / 2, boxY + 1, Colors.TEXT_PRIMARY);

        if (healthBar.getValue()) {
            float health = MathHelper.clamp_float(player.getHealth() / player.getMaxHealth(), 0.0F, 1.0F);
            int barW = Math.max(boxW - 4, 20);
            int barX = -barW / 2;
            int barY = boxY + boxH - 5;
            drawQuad(barX, barY, barW, 3, Colors.withAlpha(0x000000, 160));
            drawQuad(barX, barY, Math.max(1, (int) (barW * health)), 3, Colors.ACCENT_PRIMARY);
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void drawQuad(int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        GlStateManager.disableTexture2D();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x, y + h, 0).color(r, g, b, a).endVertex();
        wr.pos(x + w, y + h, 0).color(r, g, b, a).endVertex();
        wr.pos(x + w, y, 0).color(r, g, b, a).endVertex();
        wr.pos(x, y, 0).color(r, g, b, a).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
    }
}
