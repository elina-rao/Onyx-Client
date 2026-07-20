package com.onyxclient.modules.utility;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;

/**
 * Protective — blocks x-ray style resource packs. Stub detection via pack name heuristics.
 */
public class AntiXrayModule extends Module {

    public static AntiXrayModule INSTANCE;

    public AntiXrayModule() {
        super("AntiXray", "Block x-ray resource pack transparency", ModuleCategory.UTILITY, true);
        INSTANCE = this;
    }
}
