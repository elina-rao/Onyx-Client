package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.modules.settings.NumberSetting;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Badlion-style FOV Changer — per-state FOV sliders (default, sprint, speed, slowness, bow, flying, etc.).
 */
public class FOVChangerModule extends Module {

    public static FOVChangerModule INSTANCE;

    public final ModeSetting valueMode = addSetting(new ModeSetting("Value Mode", "Absolute", "Absolute", "Multiplier"));
    public final NumberSetting defaultFov = addSetting(new NumberSetting("Default", 90.0, 30.0, 130.0, 1.0));
    public final NumberSetting sprintFov = addSetting(new NumberSetting("Sprint", 99.0, 30.0, 130.0, 1.0));
    public final NumberSetting speed1Fov = addSetting(new NumberSetting("Speed I", 102.0, 30.0, 130.0, 1.0));
    public final NumberSetting speed2Fov = addSetting(new NumberSetting("Speed II", 108.0, 30.0, 130.0, 1.0));
    public final NumberSetting slownessFov = addSetting(new NumberSetting("Slowness", 78.0, 30.0, 130.0, 1.0));
    public final NumberSetting bowFov = addSetting(new NumberSetting("Bow", 82.0, 30.0, 130.0, 1.0));
    public final NumberSetting flyingFov = addSetting(new NumberSetting("Flying", 95.0, 30.0, 130.0, 1.0));
    public final NumberSetting waterFov = addSetting(new NumberSetting("Water", 85.0, 30.0, 130.0, 1.0));
    public final NumberSetting nightVisionFov = addSetting(new NumberSetting("Night Vision", 110.0, 30.0, 130.0, 1.0));
    public final BooleanSetting ignoreSpeedEffects = addSetting(new BooleanSetting("Static FOV (ignore speed)", false));
    public final BooleanSetting syncVanillaSlider = addSetting(new BooleanSetting("Sync Vanilla FOV Slider", true));

    private float previousFov = 70.0F;
    private boolean stored;

    public FOVChangerModule() {
        super("FOV Changer", "Badlion-style per-state FOV control", ModuleCategory.VISUAL);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.gameSettings != null) {
            previousFov = mc.gameSettings.fovSetting;
            stored = true;
            syncDefaultToVanilla();
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
        if (event.phase != TickEvent.Phase.END || !isEnabled()) {
            return;
        }
        syncDefaultToVanilla();
    }

    private void syncDefaultToVanilla() {
        if (!syncVanillaSlider.getValue()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            return;
        }
        float target = defaultFov.getFloatValue();
        if (Math.abs(mc.gameSettings.fovSetting - target) > 0.01F) {
            mc.gameSettings.fovSetting = target;
        }
    }

    public float computeFov(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            return resolveValue(defaultFov);
        }
        EntityPlayer player = mc.thePlayer;

        if (player.isUsingItem()) {
            ItemStack stack = player.getItemInUse();
            if (stack != null && stack.getItem() == Items.bow) {
                return resolveValue(bowFov);
            }
        }

        if (!ignoreSpeedEffects.getValue()) {
            PotionEffect speed = player.getActivePotionEffect(Potion.moveSpeed);
            if (speed != null) {
                if (speed.getAmplifier() >= 1) {
                    return resolveValue(speed2Fov);
                }
                return resolveValue(speed1Fov);
            }
            PotionEffect slow = player.getActivePotionEffect(Potion.moveSlowdown);
            if (slow != null) {
                return resolveValue(slownessFov);
            }
        }

        if (player.isSprinting()) {
            return resolveValue(sprintFov);
        }

        if (player.capabilities.isFlying) {
            return resolveValue(flyingFov);
        }

        if (player.isInsideOfMaterial(Material.water)) {
            return resolveValue(waterFov);
        }

        if (player.isPotionActive(Potion.nightVision)) {
            return resolveValue(nightVisionFov);
        }

        return resolveValue(defaultFov);
    }

    private float resolveValue(NumberSetting setting) {
        if ("Multiplier".equals(valueMode.getValue())) {
            return defaultFov.getFloatValue() * (setting.getFloatValue() / 100.0F);
        }
        return setting.getFloatValue();
    }

    /** @deprecated use {@link #computeFov(float)} */
    public float getCustomFov() {
        return computeFov(0.0F);
    }
}
