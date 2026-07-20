package com.onyxclient.modules.performance;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import net.minecraft.client.Minecraft;

public class NoBobbingModule extends Module {

    public static NoBobbingModule INSTANCE;

    public NoBobbingModule() {
        super("NoBobbing", "Remove view bobbing", ModuleCategory.PERFORMANCE);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        Minecraft.getMinecraft().gameSettings.viewBobbing = false;
    }
}
