package com.onyxclient.network;

import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight client-side cape presence registry.
 * Full FML plugin-channel handshake can be expanded later; for now we track
 * local player and any UUIDs registered via {@link #markOnyx(UUID)}.
 */
public final class CapeChannel {

    public static final String CHANNEL = "OC|Cape";
    private static final Set<UUID> ONYX_PLAYERS = ConcurrentHashMap.newKeySet();

    private CapeChannel() {
    }

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new CapeListener());
    }

    public static void sendHandshake() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            markOnyx(mc.thePlayer.getUniqueID());
        }
    }

    public static void markOnyx(UUID uuid) {
        if (uuid != null) {
            ONYX_PLAYERS.add(uuid);
        }
    }

    public static boolean hasOnyxClient(UUID uuid) {
        return uuid != null && ONYX_PLAYERS.contains(uuid);
    }

    public static void clear() {
        ONYX_PLAYERS.clear();
    }

    public static class CapeListener {
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
            sendHandshake();
        }

        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
            clear();
        }
    }
}
