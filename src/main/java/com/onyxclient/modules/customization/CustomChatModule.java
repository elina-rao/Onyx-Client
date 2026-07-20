package com.onyxclient.modules.customization;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.NumberSetting;

public class CustomChatModule extends Module {

    public static CustomChatModule INSTANCE;

    private final BooleanSetting timestamps;
    private final NumberSetting fontScale;

    public CustomChatModule() {
        super("CustomChat", "Chat timestamps and filtering", ModuleCategory.CUSTOMIZATION);
        INSTANCE = this;
        timestamps = addSetting(new BooleanSetting("Timestamps", true));
        fontScale = addSetting(new NumberSetting("Font Scale", 1.0, 0.8, 1.5, 0.05));
    }

    public boolean showTimestamps() {
        return isEnabled() && timestamps.getValue();
    }

    public float getFontScale() {
        return fontScale.getFloatValue();
    }
}
