package com.onyxclient.mixins;

import com.onyxclient.modules.performance.FPSBoostModule;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void onyx$disableClouds(float partialTicks, int pass, CallbackInfo ci) {
        if (FPSBoostModule.INSTANCE != null && FPSBoostModule.INSTANCE.shouldDisableClouds()) {
            ci.cancel();
        }
    }
}
