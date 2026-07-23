package com.onyxclient.modules.performance;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

/**
 * LWJGL 2 input polish: keep the cursor grabbed while in-game so look input
 * isn't lost to focus edge cases. Does not invent attacks or alter reach.
 * Does not call Mouse.getDX/getDY (that would steal deltas from vanilla).
 */
public class RawInputModule extends Module {

    public static RawInputModule INSTANCE;

    public RawInputModule() {
        super(
                "RawInput",
                "Keep LWJGL2 mouse grab stable for camera feel",
                ModuleCategory.PERFORMANCE,
                true);
        INSTANCE = this;
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.currentScreen != null || !mc.inGameHasFocus) {
            return;
        }
        try {
            if (Mouse.isCreated() && !Mouse.isGrabbed()) {
                Mouse.setGrabbed(true);
            }
        } catch (Throwable ignored) {
        }
    }
}
