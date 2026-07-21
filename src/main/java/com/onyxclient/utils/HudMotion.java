package com.onyxclient.utils;

/**
 * Small easing helpers for subtle HUD animation.
 */
public final class HudMotion {

    private HudMotion() {
    }

    public static float approach(float current, float target, float factor) {
        float f = clamp01(factor);
        return current + (target - current) * f;
    }

    public static float clamp01(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        if (value > 1.0F) {
            return 1.0F;
        }
        return value;
    }
}
