package com.onyxclient.modules.performance;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;

public class NoFireOverlayModule extends Module {

    public static NoFireOverlayModule INSTANCE;

    public NoFireOverlayModule() {
        super("NoFireOverlay", "Remove fire overlay when burning", ModuleCategory.PERFORMANCE);
        INSTANCE = this;
    }
}
