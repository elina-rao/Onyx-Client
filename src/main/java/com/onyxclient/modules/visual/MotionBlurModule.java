package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.NumberSetting;

public class MotionBlurModule extends Module {

    public static MotionBlurModule INSTANCE;
    public final NumberSetting intensity = addSetting(new NumberSetting("Intensity", 50.0, 0.0, 100.0, 5.0));

    public MotionBlurModule() {
        super("MotionBlur", "Smooth motion blur effect", ModuleCategory.VISUAL);
        INSTANCE = this;
    }

    public float getIntensityFactor() {
        return intensity.getFloatValue() / 100.0F;
    }
}
