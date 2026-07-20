package com.onyxclient.modules.movement;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Toggle-vs-hold sprint. Does not modify movement speed — only sprint key state.
 */
public class ToggleSprintModule extends Module {

    public static ToggleSprintModule INSTANCE;

    private final BooleanSetting toggleMode;
    private boolean toggledOn;
    private boolean sprintKeyWasDown;

    public ToggleSprintModule() {
        super("ToggleSprint", "Toggle sprint instead of hold", ModuleCategory.MOVEMENT, true);
        INSTANCE = this;
        toggleMode = addSetting(new BooleanSetting("Toggle Mode", true));
    }

    @Override
    public void onDisable() {
        toggledOn = false;
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
        KeyBinding sprint = mc.gameSettings.keyBindSprint;
        boolean down = Keyboard.isKeyDown(sprint.getKeyCode());
        if (down && !sprintKeyWasDown) {
            toggledOn = !toggledOn;
        }
        sprintKeyWasDown = down;
        if (toggledOn && mc.thePlayer.moveForward > 0 && !mc.thePlayer.isSneaking()) {
            mc.thePlayer.setSprinting(true);
        }
    }

    public boolean isToggledOn() {
        return toggledOn && toggleMode.getValue();
    }
}
