package com.onyxclient.modules.visual;

import com.onyxclient.OnyxClient;
import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.RenderUtils;
import com.onyxclient.utils.hypixel.BedwarsStarCache;
import com.onyxclient.utils.hypixel.HypixelAPI;
import com.onyxclient.utils.hypixel.PrestigeColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class BedwarsStarsModule extends Module {

    private final BedwarsStarCache cache = new BedwarsStarCache();
    private int tickCounter;

    public BedwarsStarsModule() {
        super("BedwarsStars", "Hypixel Bedwars star display above players", ModuleCategory.VISUAL);
    }

    @Override
    public void onEnable() {
        cache.clearExpired();
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        if (!HypixelAPI.isOnHypixel()) {
            return;
        }
        tickCounter++;
        if (tickCounter % 40 != 0) {
            return;
        }
        String apiKey = OnyxClient.getConfigManager().getConfig().hypixelApiKey;
        if (apiKey == null || apiKey.isEmpty()) {
            return;
        }
        for (Object obj : mc.theWorld.playerEntities) {
            EntityPlayer player = (EntityPlayer) obj;
            if (player == mc.thePlayer) {
                continue;
            }
            cache.fetchIfNeeded(player.getName(), apiKey);
        }
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || !HypixelAPI.isOnHypixel()) {
            return;
        }
        for (Object obj : mc.theWorld.playerEntities) {
            EntityPlayer player = (EntityPlayer) obj;
            if (player == mc.thePlayer) {
                continue;
            }
            Integer stars = cache.getStars(player.getName());
            if (stars == null) {
                continue;
            }
            renderStarTag(mc, player, stars, partialTicks);
        }
    }

    private void renderStarTag(Minecraft mc, EntityPlayer player, int stars, float partialTicks) {
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY + player.height + 0.85;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;

        String text = PrestigeColor.formatStar(stars);
        int color = PrestigeColor.getColor(stars);
        int width = mc.fontRendererObj.getStringWidth(text);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.025F, -0.025F, 0.025F);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();

        RenderUtils.drawRoundedRect(-width / 2 - 3, -10, width + 6, 12, 2, Colors.withAlpha(Colors.BG_DEEP, 160));
        mc.fontRendererObj.drawString(text, -width / 2, -8, color);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
