package com.onyxclient.mixins;

import com.onyxclient.modules.performance.HitFeedbackSyncModule;
import com.onyxclient.modules.performance.LowLatencyPacketsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(method = "addToSendQueue", at = @At("TAIL"))
    private void onyx$flushAfterSend(net.minecraft.network.Packet packet, CallbackInfo ci) {
        LowLatencyPacketsModule mod = LowLatencyPacketsModule.INSTANCE;
        if (mod != null) {
            mod.onPacketSent();
        }
    }

    @Inject(method = "handleEntityStatus", at = @At("TAIL"))
    private void onyx$entityStatus(S19PacketEntityStatus packet, CallbackInfo ci) {
        HitFeedbackSyncModule mod = HitFeedbackSyncModule.INSTANCE;
        if (mod == null || !mod.isEnabled()) {
            return;
        }
        try {
            if (packet.getOpCode() != 2) {
                return;
            }
            World world = Minecraft.getMinecraft().theWorld;
            if (world == null) {
                return;
            }
            Entity entity = packet.getEntity(world);
            mod.onEntityHurtAck(entity);
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "handleAnimation", at = @At("TAIL"))
    private void onyx$animation(S0BPacketAnimation packet, CallbackInfo ci) {
        HitFeedbackSyncModule mod = HitFeedbackSyncModule.INSTANCE;
        if (mod == null || !mod.isEnabled()) {
            return;
        }
        try {
            int type = packet.getAnimationType();
            if (type != 4 && type != 5) {
                return;
            }
            World world = Minecraft.getMinecraft().theWorld;
            if (world == null) {
                return;
            }
            Entity entity = world.getEntityByID(packet.getEntityID());
            mod.onCritAnimationAck(entity);
        } catch (Throwable ignored) {
        }
    }
}
