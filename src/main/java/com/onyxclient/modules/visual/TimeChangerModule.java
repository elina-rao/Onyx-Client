package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

public class TimeChangerModule extends Module {

    public static TimeChangerModule INSTANCE;
    public final ModeSetting time = addSetting(new ModeSetting("Time", "Day", "Sunrise", "Day", "Dusk", "Night"));

    public TimeChangerModule() {
        super("TimeChanger", "Client-side sky time lock", ModuleCategory.VISUAL);
        INSTANCE = this;
    }

    public long getLockedTime() {
        switch (time.getValue()) {
            case "Sunrise":
                return 23000L;
            case "Dusk":
                return 13000L;
            case "Night":
                return 18000L;
            default:
                return 6000L;
        }
    }

    public static long getDisplayTime() {
        if (INSTANCE != null && INSTANCE.isEnabled()) {
            return INSTANCE.getLockedTime();
        }
        World world = Minecraft.getMinecraft().theWorld;
        return world != null ? world.getWorldTime() : 6000L;
    }
}
