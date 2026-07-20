package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.NumberSetting;

public class ClearWaterModule extends Module {

    public static ClearWaterModule INSTANCE;
    public final NumberSetting transparency = addSetting(new NumberSetting("Transparency", 60.0, 0.0, 100.0, 5.0));

    public ClearWaterModule() {
        super("ClearWater", "More transparent water textures", ModuleCategory.VISUAL);
        INSTANCE = this;
    }
}
