package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;

/**
 * When enabled, vanilla Minecraft text (chat, inventories, menus) uses Outfit via OnyxFont.
 * Strings with formatting codes (§) keep the vanilla renderer so colors stay correct.
 */
public class CustomFontModule extends Module {

    public static CustomFontModule INSTANCE;

    public CustomFontModule() {
        super("CustomFont", "Anti-aliased Outfit font for Minecraft UI text", ModuleCategory.VISUAL, true);
        INSTANCE = this;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }
}
