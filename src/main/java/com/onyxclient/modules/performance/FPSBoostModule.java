package com.onyxclient.modules.performance;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.NumberSetting;

public class FPSBoostModule extends Module {

    public static FPSBoostModule INSTANCE;

    public final BooleanSetting disableParticles;
    public final BooleanSetting reduceEntityDistance;
    public final BooleanSetting disableClouds;
    public final NumberSetting particleCap;
    public final NumberSetting entityDistance;

    public FPSBoostModule() {
        super("FPSBoost", "Reduce particles and render load", ModuleCategory.PERFORMANCE, true);
        INSTANCE = this;
        disableParticles = addSetting(new BooleanSetting("Disable Particles", true));
        reduceEntityDistance = addSetting(new BooleanSetting("Reduce Entity Distance", true));
        disableClouds = addSetting(new BooleanSetting("Disable Clouds", true));
        particleCap = addSetting(new NumberSetting("Particle Cap", 50.0, 10.0, 200.0, 10.0));
        entityDistance = addSetting(new NumberSetting("Entity Distance", 32.0, 16.0, 64.0, 4.0));
    }

    public boolean shouldCullDistantEntities() {
        return isEnabled() && reduceEntityDistance.getValue();
    }

    public double getReducedEntityDistance() {
        return entityDistance.getValue();
    }

    public boolean shouldDisableClouds() {
        return isEnabled() && disableClouds.getValue();
    }
}
