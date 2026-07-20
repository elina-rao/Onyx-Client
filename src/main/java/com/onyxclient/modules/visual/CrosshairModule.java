package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.RenderUtils;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CrosshairModule extends Module {

    public static CrosshairModule INSTANCE;

    public final NumberSetting size = addSetting(new NumberSetting("Size", 5.0, 1.0, 20.0, 1.0));
    public final NumberSetting thickness = addSetting(new NumberSetting("Thickness", 1.0, 1.0, 5.0, 1.0));
    public final NumberSetting gap = addSetting(new NumberSetting("Gap", 3.0, 0.0, 15.0, 1.0));
    public final BooleanSetting dot = addSetting(new BooleanSetting("Dot", false));
    public final ColorSetting color = addSetting(new ColorSetting("Color", Colors.TEXT_PRIMARY));

    public CrosshairModule() {
        super("Crosshair", "Custom crosshair overlay", ModuleCategory.VISUAL);
        INSTANCE = this;
    }

    @SubscribeEvent
    public void onRenderCrosshair(RenderGameOverlayEvent.Pre event) {
        if (!isEnabled() || event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            return;
        }
        event.setCanceled(true);
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        int sw = mc.displayWidth / 2;
        int sh = mc.displayHeight / 2;
        int c = color.getValue();
        int s = size.getIntValue();
        int t = thickness.getIntValue();
        int g = gap.getIntValue();

        RenderUtils.drawRect(sw - t / 2, sh - s - g, t, s, c);
        RenderUtils.drawRect(sw - t / 2, sh + g, t, s, c);
        RenderUtils.drawRect(sw - s - g, sh - t / 2, s, t, c);
        RenderUtils.drawRect(sw + g, sh - t / 2, s, t, c);

        if (dot.getValue()) {
            RenderUtils.drawRect(sw - t / 2, sh - t / 2, t, t, c);
        }
    }

    private static final net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
}
