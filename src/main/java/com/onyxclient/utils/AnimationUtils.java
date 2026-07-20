package com.onyxclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public final class AnimationUtils {

    private static float menuOpenProgress = 0.0F;
    private static long menuOpenStart = 0L;
    private static boolean menuOpening = false;

    private AnimationUtils() {
    }

    public static void startMenuOpen() {
        menuOpening = true;
        menuOpenStart = System.currentTimeMillis();
    }

    public static float getMenuOpenProgress() {
        if (!menuOpening) {
            return 1.0F;
        }
        float elapsed = (System.currentTimeMillis() - menuOpenStart) / 120.0F;
        menuOpenProgress = MathHelper.clamp_float(elapsed, 0.0F, 1.0F);
        if (menuOpenProgress >= 1.0F) {
            menuOpening = false;
        }
        return easeOutCubic(menuOpenProgress);
    }

    public static float easeOutCubic(float t) {
        return 1.0F - (float) Math.pow(1.0F - t, 3.0);
    }

    public static float easeInOut(float t) {
        return t < 0.5F ? 2.0F * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 2.0F) / 2.0F;
    }

    public static int getScaledAlpha(int baseAlpha, float progress) {
        return (int) (baseAlpha * progress);
    }
}
