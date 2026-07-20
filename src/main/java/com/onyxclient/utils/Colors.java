package com.onyxclient.utils;

public final class Colors {

    /** Near-black charcoal panel base */
    public static final int BG_DEEP = 0xFF0B0B10;
    /** Elevated surface for cards */
    public static final int BG_CARD = 0xFF16161F;
    /** Sidebar / recessed surface */
    public static final int BG_SIDEBAR = 0xFF0F0F16;
    /** Slightly lifted hover fill */
    public static final int BG_HOVER = 0xFF1E1E2A;
    /** Selected nav / accent wash */
    public static final int BG_SELECTED = 0xFF221533;

    public static final int ACCENT_PRIMARY = 0xFF7B2FBE;
    public static final int ACCENT_BRIGHT = 0xFFA855F7;
    public static final int ACCENT_GLOW = 0xFFC084FC;
    public static final int ACCENT_SOFT = 0x337B2FBE;

    public static final int TEXT_PRIMARY = 0xFFF4F4F8;
    public static final int TEXT_SECONDARY = 0xFFC4C4D4;
    public static final int TEXT_MUTED = 0xFF8B8BA0;
    public static final int TEXT_DISABLED = 0xFF4A4A5C;

    public static final int SUCCESS = 0xFF34D399;
    public static final int DANGER = 0xFFF87171;
    public static final int BORDER = 0xFF2A2A38;
    public static final int BORDER_SOFT = 0xFF1C1C28;
    public static final int DIVIDER = 0xFF22222E;

    private Colors() {
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int lerpColor(int from, int to, float t) {
        int a1 = (from >> 24) & 0xFF;
        int r1 = (from >> 16) & 0xFF;
        int g1 = (from >> 8) & 0xFF;
        int b1 = from & 0xFF;
        int a2 = (to >> 24) & 0xFF;
        int r2 = (to >> 16) & 0xFF;
        int g2 = (to >> 8) & 0xFF;
        int b2 = to & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
