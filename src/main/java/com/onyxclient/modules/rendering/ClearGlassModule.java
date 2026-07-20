package com.onyxclient.modules.rendering;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;

/**
 * Removes stained-glass tint. DISABLED by default — Hypixel compliance gray zone.
 */
public class ClearGlassModule extends Module {

    public static ClearGlassModule INSTANCE;

    public ClearGlassModule() {
        super("ClearGlass", "Remove stained glass tint (compliance gray zone — default OFF)",
                ModuleCategory.RENDERING, false);
        INSTANCE = this;
    }
}
