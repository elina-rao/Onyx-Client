package com.onyxclient.modules.rendering;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class DynamicFPSModule extends Module {

    public static DynamicFPSModule INSTANCE;

    private final NumberSetting backgroundFps;
    private int savedLimit = -1;

    public DynamicFPSModule() {
        super("DynamicFPS", "Cap FPS when window unfocused", ModuleCategory.RENDERING, true);
        INSTANCE = this;
        backgroundFps = addSetting(new NumberSetting("Background FPS", 30, 5, 60, 1));
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        // When truly unfocused (alt-tab), Minecraft sets inGameHasFocus false and currentScreen may be null
        if (!mc.inGameHasFocus && mc.currentScreen == null) {
            if (savedLimit < 0) {
                savedLimit = mc.gameSettings.limitFramerate;
            }
            mc.gameSettings.limitFramerate = backgroundFps.getIntValue();
        } else if (savedLimit >= 0) {
            mc.gameSettings.limitFramerate = savedLimit;
            savedLimit = -1;
        }
    }

    @Override
    public void onDisable() {
        if (savedLimit >= 0) {
            Minecraft.getMinecraft().gameSettings.limitFramerate = savedLimit;
            savedLimit = -1;
        }
    }
}
