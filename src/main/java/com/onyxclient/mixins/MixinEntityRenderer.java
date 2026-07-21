package com.onyxclient.mixins;

import com.onyxclient.modules.performance.NoBobbingModule;
import com.onyxclient.modules.performance.NoFireOverlayModule;
import com.onyxclient.modules.performance.NoHurtCamModule;
import com.onyxclient.modules.visual.FOVChangerModule;
import com.onyxclient.modules.visual.MotionBlurModule;
import com.onyxclient.modules.visual.OptiFineSettingsModule;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void onyx$hurtCameraEffect(float partialTicks, CallbackInfo ci) {
        if (NoHurtCamModule.INSTANCE != null && NoHurtCamModule.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "setupViewBobbing", at = @At("HEAD"), cancellable = true)
    private void onyx$setupViewBobbing(float partialTicks, CallbackInfo ci) {
        if (NoBobbingModule.INSTANCE != null && NoBobbingModule.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true)
    private void onyx$renderFireOverlay(float partialTicks, CallbackInfo ci) {
        if (NoFireOverlayModule.INSTANCE != null && NoFireOverlayModule.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true)
    private void onyx$modifyFovReturn(float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        float fov = cir.getReturnValueF();
        if (FOVChangerModule.INSTANCE != null && FOVChangerModule.INSTANCE.isEnabled()) {
            fov = FOVChangerModule.INSTANCE.computeFov(partialTicks);
        }
        if (OptiFineSettingsModule.INSTANCE != null && OptiFineSettingsModule.INSTANCE.isEnabled()) {
            fov = OptiFineSettingsModule.INSTANCE.getZoomFov(fov);
        }
        cir.setReturnValue(fov);
    }

    @Inject(method = "renderWorldPass", at = @At("TAIL"))
    private void onyx$motionBlur(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        MotionBlurModule blur = MotionBlurModule.INSTANCE;
        if (blur != null && blur.isEnabled() && blur.getIntensityFactor() > 0.01F) {
            // Motion blur placeholder — full shader pipeline requires framebuffer extension
        }
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void onyx$setupFog(int startColor, float partialTicks, CallbackInfo ci) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        com.onyxclient.modules.rendering.CustomFogModule fog = com.onyxclient.modules.rendering.CustomFogModule.INSTANCE;
        if (fog != null && fog.isEnabled()) {
            if (fog.shouldDisableFog()) {
                net.minecraft.client.renderer.GlStateManager.disableFog();
                return;
            }
            // Density scaling is best-effort; exact fog params vary by call site
            float mult = fog.getFogMultiplier();
            if (mult != 1.0F) {
                try {
                    net.minecraft.client.renderer.GlStateManager.setFogDensity(
                            Math.max(0.001F, 0.1F * (2.0F - mult)));
                } catch (Throwable ignored) {
                }
            }
        }
        if (mc.thePlayer != null && mc.thePlayer.isInsideOfMaterial(net.minecraft.block.material.Material.water)) {
            com.onyxclient.modules.visual.ClearWaterModule clearWater = com.onyxclient.modules.visual.ClearWaterModule.INSTANCE;
            if (clearWater != null && clearWater.isEnabled()) {
                float transparency = clearWater.transparency.getFloatValue() / 100.0F;
                float density = 0.1F * (1.0F - transparency) + 0.01F * transparency;
                net.minecraft.client.renderer.GlStateManager.setFogDensity(density);
            }
        }
    }

    @Inject(method = "renderRainSnow", at = @At("HEAD"), cancellable = true)
    private void onyx$skipRainSnow(float partialTicks, CallbackInfo ci) {
        try {
            if (com.onyxclient.OnyxClient.getConfigManager() != null
                    && !com.onyxclient.OnyxClient.getConfigManager().getConfig().weather) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "addRainParticles", at = @At("HEAD"), cancellable = true)
    private void onyx$skipRainParticles(CallbackInfo ci) {
        try {
            if (com.onyxclient.OnyxClient.getConfigManager() != null
                    && !com.onyxclient.OnyxClient.getConfigManager().getConfig().weather) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void onyx$perspectivePre(float partialTicks, CallbackInfo ci) {
        com.onyxclient.modules.visual.PerspectiveModule persp = com.onyxclient.modules.visual.PerspectiveModule.INSTANCE;
        if (persp != null && persp.isActive()) {
            persp.applyCameraToViewEntity();
        }
    }

    @Inject(method = "orientCamera", at = @At("RETURN"))
    private void onyx$perspectivePost(float partialTicks, CallbackInfo ci) {
        com.onyxclient.modules.visual.PerspectiveModule persp = com.onyxclient.modules.visual.PerspectiveModule.INSTANCE;
        if (persp != null && persp.isActive()) {
            persp.restoreViewEntity();
        }
    }
}
