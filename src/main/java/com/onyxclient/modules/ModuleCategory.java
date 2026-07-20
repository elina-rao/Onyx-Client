package com.onyxclient.modules;

public enum ModuleCategory {
    VISUAL("Visual"),
    PERFORMANCE("Performance"),
    HUD("HUD"),
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    BEDWARS("Bedwars"),
    RENDERING("Rendering"),
    CUSTOMIZATION("Custom."),
    STATS("Stats"),
    UTILITY("Utility"),
    OPTIFINE("OptiFine");

    private final String displayName;

    ModuleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
