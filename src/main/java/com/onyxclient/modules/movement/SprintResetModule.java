package com.onyxclient.modules.movement;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Flash/sound when directional sprint resets (W-tap timing aid). Display-only.
 */
public class SprintResetModule extends Module {

    private final ColorSetting flashColor;
    private final BooleanSetting soundEnabled;
    private boolean wasSprinting;
    private boolean wasForward;
    private int flashTicks;

    public SprintResetModule() {
        super("SprintReset", "Cue on directional sprint reset", ModuleCategory.MOVEMENT);
        flashColor = addSetting(new ColorSetting("Flash Color", Colors.SUCCESS));
        soundEnabled = addSetting(new BooleanSetting("Sound", true));
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        boolean sprinting = mc.thePlayer.isSprinting();
        boolean forward = mc.thePlayer.moveForward > 0;
        // Sprint dropped while still trying to move forward = reset opportunity
        if (wasSprinting && !sprinting && forward && wasForward) {
            flashTicks = 5;
            if (soundEnabled.getValue()) {
                mc.thePlayer.playSound("random.click", 0.3F, 1.8F);
            }
        }
        wasSprinting = sprinting;
        wasForward = forward;
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
        int alpha = Math.min(180, flashTicks * 40);
        RenderUtils.drawRect(sr.getScaledWidth() / 2 - 20, sr.getScaledHeight() / 2 + 20, 40, 3,
                Colors.withAlpha(flashColor.getValue(), alpha));
    }
}
