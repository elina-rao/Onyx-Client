package com.onyxclient.mixins;

import com.onyxclient.modules.visual.TimeChangerModule;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class MixinWorld {

    @Inject(method = "getWorldTime", at = @At("HEAD"), cancellable = true)
    private void onyx$getWorldTime(CallbackInfoReturnable<Long> cir) {
        if (TimeChangerModule.INSTANCE != null && TimeChangerModule.INSTANCE.isEnabled()) {
            cir.setReturnValue(TimeChangerModule.INSTANCE.getLockedTime());
        }
    }
}
