package com.onyxclient.mixins;

import com.onyxclient.modules.visual.AnimationsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Shadow
    protected abstract void transformFirstPersonItem(float equipProgress, float swingProgress);

    @Inject(method = "transformFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void onyx$transformFirstPersonItem(float equipProgress, float swingProgress, CallbackInfo ci) {
        AnimationsModule anim = AnimationsModule.INSTANCE;
        if (anim == null || !anim.isActive()) {
            return;
        }

        // Apply custom X/Y/Z offsets
        float x = 0.56F + anim.xOffset.getFloatValue();
        float y = -0.52F + anim.yOffset.getFloatValue();
        float z = -0.72F + anim.zOffset.getFloatValue();

        GlStateManager.translate(x, y, z);
        GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);

        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);

        if (anim.swordSwing.getValue()) {
            // 1.7 sword swing rotation logic
            GlStateManager.rotate(f1 * -20.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(f * -20.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        } else {
            // Default 1.8 sword swing rotation logic
            GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        }

        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        ci.cancel();
    }

    @Inject(method = "doBlockTransformations", at = @At("HEAD"), cancellable = true)
    private void onyx$doBlockTransformations(CallbackInfo ci) {
        AnimationsModule anim = AnimationsModule.INSTANCE;
        if (anim == null || !anim.isActive() || !anim.blocking.getValue()) {
            return;
        }
        // 1.7 blocking transformations
        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
        ci.cancel();
    }

    @Inject(method = "doBowTransformations", at = @At("HEAD"), cancellable = true)
    private void onyx$doBowTransformations(float partialTicks, CallbackInfo ci) {
        AnimationsModule anim = AnimationsModule.INSTANCE;
        if (anim == null || !anim.isActive() || !anim.bow.getValue()) {
            return;
        }
        GlStateManager.translate(-0.1F, 0.15F, 0.0F);
        GlStateManager.rotate(-10.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(8.0F, 1.0F, 0.0F, 0.0F);
        ci.cancel();
    }

    @Redirect(method = "renderItemInFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V"))
    private void onyx$redirectTransform(ItemRenderer instance, float equipProgress, float swingProgress, float partialTicks) {
        AnimationsModule anim = AnimationsModule.INSTANCE;
        if (anim != null && anim.isEnabled()) {
            if (swingProgress == 0.0F) {
                swingProgress = Minecraft.getMinecraft().thePlayer.getSwingProgress(partialTicks);
            }
        }
        this.transformFirstPersonItem(equipProgress, swingProgress);
    }

    @Redirect(method = "renderItemInFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V"))
    private void onyx$redirectTranslate(float x, float y, float z) {
        AnimationsModule anim = AnimationsModule.INSTANCE;
        if (anim != null && anim.isEnabled() && anim.swordSwing.getValue()) {
            // Skip swing translation (x = f3 * 0.6F, y = -f3 * 0.2F, z = -f2 * 0.2F)
            if (x != 0.0F && Math.abs(y - (-x / 3.0F)) < 0.001F) {
                return;
            }
        }
        GlStateManager.translate(x, y, z);
    }
}
