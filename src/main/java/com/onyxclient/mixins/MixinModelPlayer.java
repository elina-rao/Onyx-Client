package com.onyxclient.mixins;

import com.onyxclient.modules.customization.Skin3DModule;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Light second-layer extrusion: scale wear boxes slightly using Skin3DModule depth.
 */
@Mixin(ModelPlayer.class)
public class MixinModelPlayer {

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;pushMatrix()V",
            shift = At.Shift.AFTER))
    private void onyx$extrudeWear(Entity entityIn, float p_limbSwing, float p_limbSwingAmount,
                                  float p_ageInTicks, float p_netHeadYaw, float p_headPitch,
                                  float scale, CallbackInfo ci) {
        if (Skin3DModule.INSTANCE == null || !Skin3DModule.INSTANCE.isEnabled()) {
            return;
        }
        float depth = Skin3DModule.INSTANCE.getDepth();
        float s = 1.0F + depth * 0.04F;
        GlStateManager.scale(s, s, s);
    }
}
