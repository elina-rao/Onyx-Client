package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.ColorSetting;

public class HitColorModule extends Module {

    public static HitColorModule INSTANCE;
    public final ColorSetting color = addSetting(new ColorSetting("Hit Color", 0xFF7B2FBE));

    public HitColorModule() {
        super("HitColor", "Custom entity hit flash color", ModuleCategory.VISUAL);
        INSTANCE = this;
    }
}
