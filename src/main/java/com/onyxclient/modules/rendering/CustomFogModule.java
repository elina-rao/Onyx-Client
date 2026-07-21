package com.onyxclient.modules.rendering;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.NumberSetting;

public class CustomFogModule extends Module {

    public static CustomFogModule INSTANCE;

    public final BooleanSetting disableFog;
    public final NumberSetting fogDistance;

    public CustomFogModule() {
        super("CustomFog", "Reduce or disable client fog", ModuleCategory.RENDERING);
        INSTANCE = this;
        disableFog = addSetting(new BooleanSetting("Disable Fog", false));
        fogDistance = addSetting(new NumberSetting("Fog Distance", 0.8, 0.1, 2.0, 0.05));
    }

    public boolean shouldDisableFog() {
        return isEnabled() && disableFog.getValue();
    }

    public float getFogMultiplier() {
        return fogDistance.getFloatValue();
    }
}
