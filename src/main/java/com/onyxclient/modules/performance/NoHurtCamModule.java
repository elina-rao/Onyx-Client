package com.onyxclient.modules.performance;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;

public class NoHurtCamModule extends Module {

    public static NoHurtCamModule INSTANCE;

    public NoHurtCamModule() {
        super("NoHurtCam", "Disable damage screen shake", ModuleCategory.PERFORMANCE);
        INSTANCE = this;
    }
}
