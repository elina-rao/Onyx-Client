package com.onyxclient.mixins;

import com.onyxclient.modules.performance.FPSBoostModule;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EffectRenderer.class)
public class MixinEffectRenderer {

    private static int onyxParticleCount;

    @Inject(method = "addEffect", at = @At("HEAD"), cancellable = true)
    private void onyx$addEffect(EntityFX effect, CallbackInfo ci) {
        FPSBoostModule fps = FPSBoostModule.INSTANCE;
        if (fps == null || !fps.isEnabled()) {
            return;
        }
        if (fps.disableParticles.getValue()) {
            ci.cancel();
            return;
        }
        if (onyxParticleCount >= fps.particleCap.getIntValue()) {
            ci.cancel();
            return;
        }
        onyxParticleCount++;
    }

    @Inject(method = "updateEffects", at = @At("HEAD"))
    private void onyx$resetParticleCount(CallbackInfo ci) {
        onyxParticleCount = 0;
    }
}
