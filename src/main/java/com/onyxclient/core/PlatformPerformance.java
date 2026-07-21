package com.onyxclient.core;

import com.onyxclient.OnyxClient;
import com.onyxclient.core.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.opengl.Display;

/**
 * macOS-specific performance detection and tips.
 */
public final class PlatformPerformance {

    private static final boolean MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

    private PlatformPerformance() {
    }

    public static boolean isMac() {
        return MAC;
    }

    public static void onClientReady() {
        if (!MAC) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        ConfigManager.ClientConfig cfg = OnyxClient.getConfigManager().getConfig();
        if (cfg.retinaTipShown) {
            return;
        }
        try {
            ScaledResolution sr = new ScaledResolution(mc);
            int scale = sr.getScaleFactor();
            int scaledW = sr.getScaledWidth();
            if (Display.getWidth() > scaledW * scale * 3 / 2) {
                cfg.retinaDetected = true;
                cfg.retinaTipShown = true;
                OnyxClient.getConfigManager().save();
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText(
                            "\u00A75[Onyx] \u00A77Retina display detected — lower Video Settings resolution or use a smaller window for higher FPS on Mac."));
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
