package com.onyxclient.utils;

import com.onyxclient.OnyxClient;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Badlion-style Menus → UI Scale (Small / Normal / Large).
 * Scales client menus only — not vanilla GUI Scale.
 */
public final class ClientUiScale {

    private ClientUiScale() {
    }

    /** 0 Small → 0.85, 1 Normal → 1.0, 2 Large → 1.15 */
    public static float factor() {
        int v = 1;
        try {
            v = OnyxClient.getConfigManager().getConfig().menuUiScale;
        } catch (Throwable ignored) {
            /* not ready */
        }
        if (v <= 0) {
            return 0.85F;
        }
        if (v >= 2) {
            return 1.15F;
        }
        return 1.0F;
    }

    /** Map screen mouse → layout coords under centered {@link #factor()} scale. */
    public static int[] toLayoutMouse(int mouseX, int mouseY, int screenW, int screenH) {
        float s = factor();
        if (Math.abs(s - 1.0F) < 0.001F) {
            return new int[]{mouseX, mouseY};
        }
        float cx = screenW / 2.0F;
        float cy = screenH / 2.0F;
        return new int[]{
                Math.round(cx + (mouseX - cx) / s),
                Math.round(cy + (mouseY - cy) / s)
        };
    }

    /** Apply centered scale (caller must push/pop matrix). */
    public static void applyCenteredScale(int screenW, int screenH) {
        float s = factor();
        if (Math.abs(s - 1.0F) < 0.001F) {
            return;
        }
        float cx = screenW / 2.0F;
        float cy = screenH / 2.0F;
        GlStateManager.translate(cx, cy, 0.0F);
        GlStateManager.scale(s, s, 1.0F);
        GlStateManager.translate(-cx, -cy, 0.0F);
    }
}
