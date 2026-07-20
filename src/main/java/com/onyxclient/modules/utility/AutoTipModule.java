package com.onyxclient.modules.utility;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoTipModule extends Module {

    private final BooleanSetting onInterval;
    private final NumberSetting intervalMinutes;
    private long lastTip;

    public AutoTipModule() {
        super("AutoTip", "Periodically run /tip all", ModuleCategory.UTILITY);
        onInterval = addSetting(new BooleanSetting("Periodic", true));
        intervalMinutes = addSetting(new NumberSetting("Interval Minutes", 15, 5, 60, 1));
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || !onInterval.getValue()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.getCurrentServerData() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long interval = intervalMinutes.getIntValue() * 60L * 1000L;
        if (now - lastTip < interval) {
            return;
        }
        lastTip = now;
        mc.thePlayer.sendChatMessage("/tip all");
    }
}
