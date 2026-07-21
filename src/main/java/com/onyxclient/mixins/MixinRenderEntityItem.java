package com.onyxclient.mixins;

import com.onyxclient.modules.rendering.ItemPhysicsModule;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderEntityItem.class)
public class MixinRenderEntityItem {

    @Inject(method = "shouldBob", at = @At("HEAD"), cancellable = true, remap = false)
    private void onyx$shouldBob(CallbackInfoReturnable<Boolean> cir) {
        if (ItemPhysicsModule.INSTANCE != null && ItemPhysicsModule.INSTANCE.isEnabled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "func_177077_a", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V",
            ordinal = 0, shift = At.Shift.AFTER))
    private void onyx$layFlat(EntityItem entity, double x, double y, double z, float partialTicks,
                              IBakedModel model, CallbackInfoReturnable<Integer> cir) {
        if (ItemPhysicsModule.INSTANCE == null || !ItemPhysicsModule.INSTANCE.isEnabled()) {
            return;
        }
        // Flat on ground; vanilla Y-spin after this still applies for motion
        GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(entity.hoverStart * (180.0F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
    }
}
