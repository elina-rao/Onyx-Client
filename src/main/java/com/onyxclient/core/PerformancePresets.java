package com.onyxclient.core;

import com.onyxclient.OnyxClient;

/**
 * One-click performance presets (Lunar/Badlion-style) via {@link PerformanceApplier}.
 */
public final class PerformancePresets {

    public static final String[] NAMES = new String[]{"Balanced", "Max FPS", "Quality"};

    private PerformancePresets() {
    }

    public static void apply(int index) {
        if (index < 0 || index >= NAMES.length) {
            return;
        }
        OnyxClient.getConfigManager().getConfig().perfPreset = index;
        PerformanceApplier.apply(index);
        OnyxClient.getConfigManager().syncAllModulesToConfig();
        OnyxClient.getConfigManager().save();
    }
}
