package com.onyxclient.cape;

import com.onyxclient.OnyxClient;
import com.onyxclient.network.CapeChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public final class CapeRenderer {

    private static final ResourceLocation CAPE_TEXTURE = new ResourceLocation("onyxclient", "textures/cape/cape.png");
    private static int animationFrame;
    private static long lastFrameTime;

    private CapeRenderer() {
    }

    public static boolean shouldRenderCape(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (player == mc.thePlayer) {
            return OnyxClient.getConfigManager().getConfig().capeEnabled;
        }
        return CapeChannel.hasOnyxClient(player.getUniqueID());
    }

    public static void renderCape(AbstractClientPlayer player, ModelBiped model, float partialTicks) {
        updateAnimationFrame();
        Minecraft.getMinecraft().getTextureManager().bindTexture(CAPE_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        float pulse = 0.9F + (animationFrame / 16.0F) * 0.1F;
        GlStateManager.color(pulse, pulse, pulse, 1.0F);

        model.bipedBody.render(0.0625F);
        GlStateManager.popMatrix();
    }

    private static void updateAnimationFrame() {
        long now = System.currentTimeMillis();
        if (now - lastFrameTime > 150L) {
            animationFrame = (animationFrame + 1) % 16;
            lastFrameTime = now;
        }
    }
}
