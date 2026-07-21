package com.onyxclient.mixins;

import com.onyxclient.modules.bedwars.HypixelBedwarsModule;
import net.minecraft.block.BlockBed;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBed.class)
public class MixinBlockBed {

    @Inject(method = "colorMultiplier", at = @At("HEAD"), cancellable = true)
    private void onyx$colorBeds(IBlockAccess worldIn, BlockPos pos, int renderPass, CallbackInfoReturnable<Integer> cir) {
        HypixelBedwarsModule mod = HypixelBedwarsModule.INSTANCE;
        if (mod == null || !mod.isEnabled() || !mod.coloredBeds.getValue() || !mod.isInBedwars()) {
            return;
        }
        int[] palette = {
                0xFFF65C5C,
                0xFF5CA9FF,
                0xFF7BFF7B,
                0xFFFFFF6A,
                0xFFFFA65C,
                0xFFD58BFF,
                0xFF5CE7E7,
                0xFFFFFFFF
        };
        int idx = Math.abs((pos.getX() * 31 + pos.getZ() * 17) % palette.length);
        cir.setReturnValue(palette[idx]);
    }
}
