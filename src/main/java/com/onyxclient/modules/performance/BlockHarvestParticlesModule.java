package com.onyxclient.modules.performance;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;

/**
 * Badlion Graphics setting — toggle block harvest / break particles for QoL and FPS.
 */
public class BlockHarvestParticlesModule extends Module {

    public static BlockHarvestParticlesModule INSTANCE;

    public final BooleanSetting harvestParticles = addSetting(new BooleanSetting("Harvest Particles", true));
    public final BooleanSetting hitParticles = addSetting(new BooleanSetting("Mining Hit Particles", true));

    public BlockHarvestParticlesModule() {
        super("Block Harvest Particles", "Block break and mining hit particles", ModuleCategory.PERFORMANCE, true);
        INSTANCE = this;
    }

    public boolean shouldShowDestroyParticles() {
        return !isEnabled() || harvestParticles.getValue();
    }

    public boolean shouldShowHitParticles() {
        return !isEnabled() || hitParticles.getValue();
    }
}
