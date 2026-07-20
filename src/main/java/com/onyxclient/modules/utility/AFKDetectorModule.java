package com.onyxclient.modules.utility;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class AFKDetectorModule extends Module {

    private final NumberSetting thresholdSeconds;
    private long lastInput;
    private boolean afk;
    private int notifyCooldown;

    public AFKDetectorModule() {
        super("AFKDetector", "Detect own AFK state", ModuleCategory.UTILITY);
        thresholdSeconds = addSetting(new NumberSetting("Threshold Seconds", 60, 15, 300, 5));
        lastInput = System.currentTimeMillis();
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        if (Keyboard.getEventKeyState() || Mouse.getEventButtonState()
                || Math.abs(mc.thePlayer.motionX) > 0.01 || Math.abs(mc.thePlayer.motionZ) > 0.01) {
            lastInput = System.currentTimeMillis();
            if (afk) {
                afk = false;
            }
        }
        long idle = System.currentTimeMillis() - lastInput;
        boolean nowAfk = idle > thresholdSeconds.getIntValue() * 1000L;
        if (nowAfk && !afk) {
            afk = true;
            notifyCooldown = 40;
        }
        if (notifyCooldown > 0) {
            notifyCooldown--;
        }
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled() || !afk) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        mc.fontRendererObj.drawStringWithShadow("AFK", 4, 4, 0xFFF87171);
    }

    public boolean isAfk() {
        return afk;
    }
}
