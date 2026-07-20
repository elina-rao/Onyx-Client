package com.onyxclient.mixins;

import com.onyxclient.cape.CapeRenderer;
import com.onyxclient.modules.visual.CustomCapeModule;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onyx$addCapeLayer(CallbackInfo ci) {
        RenderPlayer self = (RenderPlayer) (Object) this;
        self.addLayer(new LayerRenderer<AbstractClientPlayer>() {
            @Override
            public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                                        float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
                if (CustomCapeModule.INSTANCE == null || !CustomCapeModule.INSTANCE.isEnabled()) {
                    return;
                }
                if (!CapeRenderer.shouldRenderCape(player)) {
                    return;
                }
                CapeRenderer.renderCape(player, (ModelBiped) self.getMainModel(), partialTicks);
            }

            @Override
            public boolean shouldCombineTextures() {
                return false;
            }
        });
    }
}
