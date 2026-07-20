package com.onyxclient.modules.combat;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MovingObjectPosition;

/**
 * Shows 1.8 attack cooldown / hit-delay as a bar or percentage.
 * Display-only — does not alter attack timing.
 */
public class HitDelayModule extends HudModule {

    private final ColorSetting barColor;
    private final BooleanSetting showPercentage;
    private final ModeSetting style;

    public HitDelayModule() {
        super("HitDelayCooldown", "Show attack cooldown as a fill bar", true);
        barColor = addSetting(new ColorSetting("Bar Color", Colors.ACCENT_PRIMARY));
        showPercentage = addSetting(new BooleanSetting("Show Percentage", true));
        style = addSetting(new ModeSetting("Style", "Linear", "Linear", "Circular"));
        setHudSize(60, 8);
        setHudPosition(2, 160);
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }

        // 1.8.9 has no visible cooldown attribute; approximate via left-click timing
        // using the player's swing progress as a proxy for attack readiness.
        float cooldown = 1.0F - mc.thePlayer.getSwingProgress(partialTicks);
        if (mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && mc.thePlayer.isSwingInProgress) {
            cooldown = Math.min(cooldown, 0.85F);
        }

        int color = barColor.getValue();
        if ("Circular".equals(style.getValue())) {
            drawCircular(cooldown, color);
        } else {
            int w = 60;
            int h = 6;
            RenderUtils.drawRect(hudX, hudY, w, h, Colors.withAlpha(0x000000, 140));
            RenderUtils.drawRect(hudX, hudY, (int) (w * cooldown), h, color);
            setHudSize(w, showPercentage.getValue() ? 18 : h);
            if (showPercentage.getValue()) {
                String text = (int) (cooldown * 100) + "%";
                mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY + 8, Colors.TEXT_PRIMARY);
            }
        }
    }

    private void drawCircular(float cooldown, int color) {
        Minecraft mc = Minecraft.getMinecraft();
        int size = 16;
        RenderUtils.drawRect(hudX, hudY, size, size, Colors.withAlpha(0x000000, 120));
        RenderUtils.drawRect(hudX + 2, hudY + 2, (int) ((size - 4) * cooldown), size - 4, color);
        setHudSize(size, showPercentage.getValue() ? 28 : size);
        if (showPercentage.getValue()) {
            mc.fontRendererObj.drawStringWithShadow((int) (cooldown * 100) + "%", hudX, hudY + size + 2, Colors.TEXT_PRIMARY);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
    }
}
