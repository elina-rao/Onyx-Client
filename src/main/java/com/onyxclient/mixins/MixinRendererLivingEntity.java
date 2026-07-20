package com.onyxclient.mixins;

import com.onyxclient.modules.visual.HitColorModule;
import com.onyxclient.modules.visual.NametagsModule;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(RendererLivingEntity.class)
public class MixinRendererLivingEntity<T extends EntityLivingBase> {

    @ModifyArgs(method = "setBrightness", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;getColorBuffer(FFFF)Ljava/nio/FloatBuffer;"))
    private void onyx$modifyHitColor(Args args) {
        HitColorModule hitColor = HitColorModule.INSTANCE;
        if (hitColor != null && hitColor.isEnabled()) {
            int c = hitColor.color.getValue();
            float r = ((c >> 16) & 0xFF) / 255.0F;
            float g = ((c >> 8) & 0xFF) / 255.0F;
            float b = (c & 0xFF) / 255.0F;
            float a = ((c >> 24) & 0xFF) / 255.0F;
            if (a == 0.0F) a = 0.3F; // Default red opacity is 0.3F
            args.set(0, r);
            args.set(1, g);
            args.set(2, b);
            args.set(3, a);
        }
    }

    @Inject(method = "renderName", at = @At("HEAD"), cancellable = true)
    private void onyx$renderName(T entity, double x, double y, double z, CallbackInfo ci) {
        if (entity instanceof EntityPlayer) {
            NametagsModule nametags = NametagsModule.INSTANCE;
            if (nametags != null && nametags.isEnabled()) {
                ci.cancel();
            }
        }
    }
}
