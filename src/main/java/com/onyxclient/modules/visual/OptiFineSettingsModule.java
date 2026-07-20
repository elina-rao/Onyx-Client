package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

public class OptiFineSettingsModule extends Module {

    public static OptiFineSettingsModule INSTANCE;

    public final BooleanSetting dynamicLights = addSetting(new BooleanSetting("Dynamic Lights", true));
    public final BooleanSetting connectedTextures = addSetting(new BooleanSetting("Connected Textures", true));
    public final BooleanSetting customSky = addSetting(new BooleanSetting("Custom Sky", true));
    public final BooleanSetting fastMath = addSetting(new BooleanSetting("Fast Math", true));
    public final NumberSetting zoomLevel = addSetting(new NumberSetting("Zoom Level", 4.0, 2.0, 10.0, 0.5));

    private boolean zooming;
    private float zoomProgress;
    private float originalFov;
    private float targetFov;

    public OptiFineSettingsModule() {
        super("OptiFine Settings", "Zoom and OptiFine-compatible settings", ModuleCategory.OPTIFINE, true);
        INSTANCE = this;
    }

    @Override
    public void onTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        boolean keyDown = Keyboard.isKeyDown(Keyboard.KEY_C);
        if (keyDown && !zooming) {
            zooming = true;
            originalFov = mc.gameSettings.fovSetting;
            targetFov = originalFov / zoomLevel.getFloatValue();
        } else if (!keyDown && zooming) {
            zooming = false;
            targetFov = originalFov;
        }
        if (zooming) {
            zoomProgress = Math.min(1.0F, zoomProgress + 0.15F);
        } else {
            zoomProgress = Math.max(0.0F, zoomProgress - 0.15F);
        }
    }

    public float getZoomFov(float baseFov) {
        if (!isEnabled() || zoomProgress <= 0.0F) {
            return baseFov;
        }
        float zoomed = baseFov / zoomLevel.getFloatValue();
        float eased = easeOut(zoomProgress);
        return baseFov + (zoomed - baseFov) * eased;
    }

    private float easeOut(float t) {
        return 1.0F - (float) Math.pow(1.0F - t, 3.0);
    }

    public boolean isZooming() {
        return zooming && zoomProgress > 0.01F;
    }
}
