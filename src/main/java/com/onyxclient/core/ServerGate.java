package com.onyxclient.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Server allow/deny helpers for modules banned on specific networks (e.g. Hypixel freelook).
 */
public final class ServerGate {

    private ServerGate() {
    }

    public static boolean isSingleplayer() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.isSingleplayer() || mc.getCurrentServerData() == null;
    }

    public static boolean isHypixel() {
        ServerData data = Minecraft.getMinecraft().getCurrentServerData();
        if (data == null || data.serverIP == null) {
            return false;
        }
        String ip = data.serverIP.toLowerCase();
        return ip.contains("hypixel") || ip.endsWith(".hypixel.net") || ip.contains("hypixel.net");
    }

    /** Perspective / Freelook — denied on Hypixel; allowed in SP and other servers. */
    public static boolean isPerspectiveAllowed() {
        if (isSingleplayer()) {
            return true;
        }
        return !isHypixel();
    }
}
