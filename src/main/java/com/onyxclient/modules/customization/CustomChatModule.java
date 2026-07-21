package com.onyxclient.modules.customization;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.NumberSetting;

public class CustomChatModule extends Module {

    public static CustomChatModule INSTANCE;

    private final BooleanSetting timestamps;
    private final BooleanSetting stackMessages;
    private final NumberSetting fontScale;

    private String lastStackKey = "";
    private int stackCount;

    public CustomChatModule() {
        super("CustomChat", "Chat timestamps, stacking, and scale", ModuleCategory.CUSTOMIZATION);
        INSTANCE = this;
        timestamps = addSetting(new BooleanSetting("Timestamps", true));
        stackMessages = addSetting(new BooleanSetting("Stack Messages", true));
        fontScale = addSetting(new NumberSetting("Font Scale", 1.0, 0.8, 1.5, 0.05));
    }

    public boolean showTimestamps() {
        return isEnabled() && timestamps.getValue();
    }

    public boolean stackMessages() {
        return isEnabled() && stackMessages.getValue();
    }

    public float getFontScale() {
        return fontScale.getFloatValue();
    }

    /**
     * @return stack suffix to append, or null if this is a fresh line
     */
    public String consumeStackSuffix(String unformatted) {
        if (!stackMessages() || unformatted == null) {
            lastStackKey = "";
            stackCount = 0;
            return null;
        }
        if (unformatted.equals(lastStackKey)) {
            stackCount++;
            return " §8[x" + stackCount + "]";
        }
        lastStackKey = unformatted;
        stackCount = 1;
        return null;
    }
}
