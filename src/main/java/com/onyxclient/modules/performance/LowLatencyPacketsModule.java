package com.onyxclient.modules.performance;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import io.netty.channel.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.lang.reflect.Field;

/**
 * Flushes the Netty channel after outbound writes so movement/combat packets
 * leave the socket sooner. Packet contents stay vanilla.
 */
public class LowLatencyPacketsModule extends Module {

    public static LowLatencyPacketsModule INSTANCE;

    private Channel channel;

    public LowLatencyPacketsModule() {
        super(
                "LowLatencyPackets",
                "Flush socket writes sooner (same packet data)",
                ModuleCategory.PERFORMANCE,
                true);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        resolveChannel();
    }

    @SubscribeEvent
    public void onConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        channel = null;
        resolveChannel();
    }

    @SubscribeEvent
    public void onDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        channel = null;
    }

    public void onPacketSent() {
        if (!isEnabled()) {
            return;
        }
        Channel ch = channel;
        if (ch == null || !ch.isOpen()) {
            resolveChannel();
            ch = channel;
        }
        if (ch != null && ch.isActive()) {
            ch.flush();
        }
    }

    private void resolveChannel() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getNetHandler() == null) {
                return;
            }
            NetworkManager nm = mc.getNetHandler().getNetworkManager();
            for (Field field : NetworkManager.class.getDeclaredFields()) {
                if (Channel.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    channel = (Channel) field.get(nm);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
