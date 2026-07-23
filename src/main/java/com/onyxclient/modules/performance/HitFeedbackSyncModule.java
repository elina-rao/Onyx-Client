package com.onyxclient.modules.performance;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/**
 * Plays local hit feedback as soon as the server acknowledges damage/animation.
 * Does not change hitboxes or invent hits — FX/sound only on server packets.
 */
public class HitFeedbackSyncModule extends Module {

    public static HitFeedbackSyncModule INSTANCE;

    public HitFeedbackSyncModule() {
        super(
                "HitFeedbackSync",
                "Sync hit particles/sounds to server acknowledgements",
                ModuleCategory.PERFORMANCE,
                true);
        INSTANCE = this;
    }

    @SubscribeEvent
    public void onConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        // no-op — ready for packet hooks
    }

    /**
     * Called from mixin when S19PacketEntityStatus opcode 2 (hurt) arrives.
     */
    public void onEntityHurtAck(Entity entity) {
        if (!isEnabled() || entity == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        // Only enhance feedback for nearby players (combat feel), never alter damage
        if (!(entity instanceof EntityPlayer) && entity != mc.thePlayer) {
            return;
        }
        try {
            mc.theWorld.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.CRIT,
                    entity.posX,
                    entity.posY + entity.height * 0.5,
                    entity.posZ,
                    0.0,
                    0.0,
                    0.0);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Called when S0BPacketAnimation type 4/5 (crit) is received for an entity.
     */
    public void onCritAnimationAck(Entity entity) {
        if (!isEnabled() || entity == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getSoundHandler() == null) {
            return;
        }
        try {
            mc.getSoundHandler().playSound(
                    PositionedSoundRecord.create(
                            new ResourceLocation("game.player.hurt"),
                            1.0F));
        } catch (Throwable ignored) {
            // Sound id may differ; particle path above still applies
        }
    }
}
