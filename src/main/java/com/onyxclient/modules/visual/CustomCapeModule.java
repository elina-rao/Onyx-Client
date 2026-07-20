package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;

public class CustomCapeModule extends Module {

    public static CustomCapeModule INSTANCE;

    public CustomCapeModule() {
        super("Custom Cape", "Onyx Client cape for Onyx users", ModuleCategory.VISUAL, true);
        INSTANCE = this;
    }
}
