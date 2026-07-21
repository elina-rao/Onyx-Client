package com.onyxclient.modules.movement;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Toggle-vs-hold sneak. Does not modify hitboxes — only sneak key state.
 */
public class ToggleSneakModule extends Module {

    public static ToggleSneakModule INSTANCE;

    private final BooleanSetting toggleMode;
    private boolean toggledOn;
    private boolean sneakKeyWasDown;

    public ToggleSneakModule() {
        super("ToggleSneak", "Toggle sneak instead of hold", ModuleCategory.MOVEMENT);
        INSTANCE = this;
        toggleMode = addSetting(new BooleanSetting("Toggle Mode", true));
    }

    @Override
    public void onDisable() {
        toggledOn = false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || !toggleMode.getValue()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) {
            return;
        }
        KeyBinding sneak = mc.gameSettings.keyBindSneak;
        boolean down = Keyboard.isKeyDown(sneak.getKeyCode());
        if (down && !sneakKeyWasDown) {
            toggledOn = !toggledOn;
        }
        sneakKeyWasDown = down;
        KeyBinding.setKeyBindState(sneak.getKeyCode(), toggledOn);
    }
}
