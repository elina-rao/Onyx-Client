package com.onyxclient.utils;

/**
 * Shared color choices for premium HUD cards.
 */
public final class HudTheme {

    public static final int CARD_BG = Colors.withAlpha(Colors.BG_CARD, 205);
    public static final int CARD_BORDER = Colors.withAlpha(Colors.BORDER, 235);
    public static final int CARD_SHADOW = Colors.withAlpha(0x000000, 80);
    public static final int TITLE = Colors.TEXT_MUTED;
    public static final int VALUE = Colors.TEXT_PRIMARY;
    public static final int VALUE_ACCENT = Colors.ACCENT_BRIGHT;

    private HudTheme() {
    }
}
