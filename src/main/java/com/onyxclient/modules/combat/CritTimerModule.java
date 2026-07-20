package com.onyxclient.modules.combat;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Visual/audio cue at jump apex for critical-hit timing. Display-only.
 */
public class CritTimerModule extends Module {

    private final ColorSetting flashColor;
    private final BooleanSetting sound;
    private final NumberSetting sensitivity;
    private int flashTicks;
    private double lastMotionY;

    public CritTimerModule() {
        super("CritTimer", "Cue when at jump apex for crits", ModuleCategory.COMBAT);
        flashColor = addSetting(new ColorSetting("Flash Color", Colors.ACCENT_BRIGHT));
        sound = addSetting(new BooleanSetting("Sound", false));
        sensitivity = addSetting(new NumberSetting("Sensitivity", 0.0, -0.2, 0.2, 0.01));
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        double motionY = mc.thePlayer.motionY;
        double threshold = 0.0 + sensitivity.getValue();
        // Falling after rising — near apex
        if (lastMotionY > threshold && motionY <= threshold && !mc.thePlayer.onGround) {
            flashTicks = 6;
            if (sound.getValue()) {
                mc.thePlayer.playSound("random.orb", 0.4F, 1.6F);
            }
        }
        lastMotionY = motionY;
        if (flashTicks > 0) {
            flashTicks--;
        }
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled() || flashTicks <= 0) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int cx = sr.getScaledWidth() / 2;
        int cy = sr.getScaledHeight() / 2;
        int alpha = Math.min(200, flashTicks * 35);
        int color = Colors.withAlpha(flashColor.getValue(), alpha);
        RenderUtils.drawRect(cx - 12, cy - 12, 24, 2, color);
        RenderUtils.drawRect(cx - 12, cy + 10, 24, 2, color);
        RenderUtils.drawRect(cx - 12, cy - 12, 2, 24, color);
        RenderUtils.drawRect(cx + 10, cy - 12, 2, 24, color);
    }
}
