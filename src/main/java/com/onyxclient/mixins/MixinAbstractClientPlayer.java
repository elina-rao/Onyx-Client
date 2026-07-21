package com.onyxclient.mixins;

import com.onyxclient.skin.SkinOverrideManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer {

    @Inject(method = "getLocationSkin", at = @At("HEAD"), cancellable = true)
    private void onyx$overrideSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        if (!SkinOverrideManager.INSTANCE.hasOverride()) {
            return;
        }
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || self != mc.thePlayer) {
            return;
        }
        ResourceLocation loc = SkinOverrideManager.INSTANCE.getOverrideLocation();
        if (loc != null) {
            cir.setReturnValue(loc);
        }
    }

    @Inject(method = "getSkinType", at = @At("HEAD"), cancellable = true)
    private void onyx$overrideSkinType(CallbackInfoReturnable<String> cir) {
        if (!SkinOverrideManager.INSTANCE.hasOverride()) {
            return;
        }
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || self != mc.thePlayer) {
            return;
        }
        cir.setReturnValue(SkinOverrideManager.INSTANCE.getOverrideSkinType());
    }
}
