package com.onyxclient.modules.utility;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class ScreenshotModule extends Module {

    private final ModeSetting filenameFormat;

    public ScreenshotModule() {
        super("ScreenshotTrigger", "Keybind screenshot to configurable format", ModuleCategory.UTILITY);
        filenameFormat = addSetting(new ModeSetting("Filename", "Default", "Default", "Timestamp"));
        setKeybind(Keyboard.KEY_F2);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (!isEnabled()) {
            return;
        }
        if (!Keyboard.getEventKeyState()) {
            return;
        }
        if (Keyboard.getEventKey() != getKeybind()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(mc.mcDataDir, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        // filenameFormat reserved for future custom naming
        filenameFormat.getValue();
    }
}
