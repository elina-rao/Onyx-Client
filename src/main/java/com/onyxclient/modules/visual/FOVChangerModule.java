package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Applies custom FOV through {@code gameSettings.fovSetting} (no Mixin required).
 */
public class FOVChangerModule extends Module {

    public static FOVChangerModule INSTANCE;
    public final NumberSetting fov = addSetting(new NumberSetting("FOV", 90.0, 30.0, 130.0, 1.0));
    private float previousFov = 70.0F;
    private boolean stored;

    public FOVChangerModule() {
        super("FOV Changer", "Custom field of view", ModuleCategory.VISUAL);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.gameSettings != null) {
            previousFov = mc.gameSettings.fovSetting;
            stored = true;
            applyFov();
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.gameSettings != null && stored) {
            mc.gameSettings.fovSetting = previousFov;
        }
        stored = false;
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (isEnabled()) {
            applyFov();
        }
    }

    private void applyFov() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            return;
        }
        float target = fov.getFloatValue();
        if (Math.abs(mc.gameSettings.fovSetting - target) > 0.01F) {
            mc.gameSettings.fovSetting = target;
        }
    }

    public float getCustomFov() {
        return fov.getFloatValue();
    }
}
