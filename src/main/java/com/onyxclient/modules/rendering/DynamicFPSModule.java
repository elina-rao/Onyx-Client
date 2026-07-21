package com.onyxclient.modules.rendering;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.NumberSetting;

/**
 * When enabled, {@link com.onyxclient.core.ClientSettingsHooks} caps FPS while unfocused.
 */
public class DynamicFPSModule extends Module {

    public static DynamicFPSModule INSTANCE;

    public final NumberSetting backgroundFps;

    public DynamicFPSModule() {
        super("DynamicFPS", "Cap FPS when window unfocused", ModuleCategory.RENDERING, true);
        INSTANCE = this;
        backgroundFps = addSetting(new NumberSetting("Background FPS", 30, 5, 60, 1));
    }
}
