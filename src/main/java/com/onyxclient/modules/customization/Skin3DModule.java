package com.onyxclient.modules.customization;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.NumberSetting;

public class Skin3DModule extends Module {

    public static Skin3DModule INSTANCE;

    private final NumberSetting depth;

    public Skin3DModule() {
        super("3DSkins", "Slight 3D extrusion on skin layers", ModuleCategory.CUSTOMIZATION);
        INSTANCE = this;
        depth = addSetting(new NumberSetting("Extrusion Depth", 0.5, 0.1, 1.5, 0.1));
    }

    public float getDepth() {
        return depth.getFloatValue();
    }
}
