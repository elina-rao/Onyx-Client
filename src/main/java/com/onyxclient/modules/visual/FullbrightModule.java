package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import net.minecraft.client.Minecraft;

public class FullbrightModule extends Module {

    private float previousGamma;

    public FullbrightModule() {
        super("Fullbright", "Maximum brightness without night vision", ModuleCategory.VISUAL);
    }

    @Override
    public void onEnable() {
        previousGamma = Minecraft.getMinecraft().gameSettings.gammaSetting;
        Minecraft.getMinecraft().gameSettings.gammaSetting = 100.0F;
    }

    @Override
    public void onDisable() {
        Minecraft.getMinecraft().gameSettings.gammaSetting = previousGamma;
    }
}
