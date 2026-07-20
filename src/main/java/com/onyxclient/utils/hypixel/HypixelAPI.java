package com.onyxclient.utils.hypixel;

import net.minecraft.client.Minecraft;

public final class HypixelAPI {

    private static final String HYPIXEL_HOST = "hypixel.net";

    private HypixelAPI() {
    }

    public static boolean isOnHypixel() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getCurrentServerData() == null) {
            return false;
        }
        String ip = mc.getCurrentServerData().serverIP.toLowerCase();
        return ip.contains(HYPIXEL_HOST);
    }
}
