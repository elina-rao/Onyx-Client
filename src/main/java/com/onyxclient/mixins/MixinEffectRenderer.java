package com.onyxclient.mixins;

import com.onyxclient.modules.performance.BlockHarvestParticlesModule;
import com.onyxclient.modules.performance.FPSBoostModule;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EffectRenderer.class)
public class MixinEffectRenderer {

    @Shadow
    private List<EntityFX>[][] fxLayers;

    @Inject(method = "addBlockDestroyEffects", at = @At("HEAD"), cancellable = true)
    private void onyx$blockDestroyParticles(BlockPos pos, net.minecraft.block.state.IBlockState state, CallbackInfo ci) {
        BlockHarvestParticlesModule mod = BlockHarvestParticlesModule.INSTANCE;
        if (mod != null && !mod.shouldShowDestroyParticles()) {
            ci.cancel();
        }
    }

    @Inject(method = "addBlockHitEffects", at = @At("HEAD"), cancellable = true)
    private void onyx$blockHitParticles(BlockPos pos, EnumFacing side, CallbackInfo ci) {
        BlockHarvestParticlesModule mod = BlockHarvestParticlesModule.INSTANCE;
        if (mod != null && !mod.shouldShowHitParticles()) {
            ci.cancel();
        }
    }

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
        if (fps.particleCap.getIntValue() <= 0) {
            ci.cancel();
            return;
        }
        if (countLiveParticles() >= fps.particleCap.getIntValue()) {
            ci.cancel();
        }
    }

    private int countLiveParticles() {
        if (fxLayers == null) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < fxLayers.length; i++) {
            if (fxLayers[i] == null) {
                continue;
            }
            for (int j = 0; j < fxLayers[i].length; j++) {
                List<EntityFX> list = fxLayers[i][j];
                if (list != null) {
                    n += list.size();
                }
            }
        }
        return n;
    }
}
