package com.onyxclient.mixins;

import com.onyxclient.modules.rendering.ClearGlassModule;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stained glass uses per-color textures in 1.8.9 (not runtime tint), so swap the baked model
 * to clear glass / glass pane when ClearGlass is enabled (default OFF).
 */
@Mixin(BlockRendererDispatcher.class)
public abstract class MixinBlockRendererDispatcher {

    @Shadow
    public abstract net.minecraft.client.renderer.BlockModelShapes getBlockModelShapes();

    @Inject(method = "getModelFromBlockState", at = @At("HEAD"), cancellable = true)
    private void onyx$clearGlass(IBlockState state, IBlockAccess worldIn, BlockPos pos,
                                 CallbackInfoReturnable<IBakedModel> cir) {
        if (ClearGlassModule.INSTANCE == null || !ClearGlassModule.INSTANCE.isEnabled()) {
            return;
        }
        if (state == null) {
            return;
        }
        Block block = state.getBlock();
        if (block instanceof BlockStainedGlass) {
            cir.setReturnValue(getBlockModelShapes().getModelForState(Blocks.glass.getDefaultState()));
        } else if (block instanceof BlockStainedGlassPane) {
            cir.setReturnValue(getBlockModelShapes().getModelForState(Blocks.glass_pane.getDefaultState()));
        }
    }
}
