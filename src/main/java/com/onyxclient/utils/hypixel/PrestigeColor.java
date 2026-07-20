package com.onyxclient.utils.hypixel;

import com.onyxclient.utils.Colors;

public final class PrestigeColor {

    private PrestigeColor() {
    }

    public static int getColor(int level) {
        if (level >= 5000) return 0xFFE040FB;
        if (level >= 4000) return 0xFFAB47BC;
        if (level >= 3000) return 0xFF7E57C2;
        if (level >= 2000) return 0xFF5C6BC0;
        if (level >= 1000) return 0xFF42A5F5;
        if (level >= 500) return 0xFF26C6DA;
        if (level >= 400) return 0xFF66BB6A;
        if (level >= 300) return 0xFF9CCC65;
        if (level >= 200) return 0xFFDCE775;
        if (level >= 100) return 0xFFFFEE58;
        if (level >= 50) return 0xFFFFA726;
        if (level >= 25) return 0xFFEF5350;
        if (level >= 10) return 0xFFB0BEC5;
        return Colors.TEXT_MUTED;
    }

    public static String formatStar(int level) {
        if (level >= 1000) {
            return "[\u2605 " + level + "\u2726]";
        }
        return "[" + level + "\u2726]";
    }
}
