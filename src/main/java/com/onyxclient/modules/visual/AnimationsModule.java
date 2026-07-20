package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.NumberSetting;

public class AnimationsModule extends Module {

    public static AnimationsModule INSTANCE;

    public final BooleanSetting swordSwing = addSetting(new BooleanSetting("Sword Swing", true));
    public final BooleanSetting blocking = addSetting(new BooleanSetting("Blocking Position", true));
    public final BooleanSetting rod = addSetting(new BooleanSetting("Rod Hold", true));
    public final BooleanSetting bow = addSetting(new BooleanSetting("Bow Pull", true));
    public final BooleanSetting eating = addSetting(new BooleanSetting("Eating", true));
    public final NumberSetting xOffset = addSetting(new NumberSetting("X Offset", 0.0, -1.0, 1.0, 0.05));
    public final NumberSetting yOffset = addSetting(new NumberSetting("Y Offset", 0.0, -1.0, 1.0, 0.05));
    public final NumberSetting zOffset = addSetting(new NumberSetting("Z Offset", 0.0, -1.0, 1.0, 0.05));

    public AnimationsModule() {
        super("Animations", "1.7-style item and arm animations", ModuleCategory.VISUAL, true);
        INSTANCE = this;
    }

    public boolean isActive() {
        return isEnabled();
    }
}
