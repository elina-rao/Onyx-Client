package com.onyxclient.mixins;

import com.onyxclient.core.PerformanceApplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityRendererDispatcher.class)
public class MixinTileEntityRendererDispatcher {

    private static final double TE_CULL_DISTANCE = 24.0;

    @Inject(method = "renderTileEntity", at = @At("HEAD"), cancellable = true)
    private void onyx$cullTileEntity(TileEntity tileEntity, float partialTicks, int destroyStage, CallbackInfo ci) {
        if (!PerformanceApplier.isTileEntityCullingActive() || tileEntity == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderViewEntity() == null) {
            return;
        }
        BlockPos pos = tileEntity.getPos();
        double dx = pos.getX() + 0.5 - mc.getRenderViewEntity().posX;
        double dy = pos.getY() + 0.5 - mc.getRenderViewEntity().posY;
        double dz = pos.getZ() + 0.5 - mc.getRenderViewEntity().posZ;
        if (dx * dx + dy * dy + dz * dz > TE_CULL_DISTANCE * TE_CULL_DISTANCE) {
            ci.cancel();
        }
    }
}
