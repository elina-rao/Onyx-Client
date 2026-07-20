package com.onyxclient.modules.performance;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.NumberSetting;

public class FPSBoostModule extends Module {

    public static FPSBoostModule INSTANCE;
    public final BooleanSetting disableParticles = addSetting(new BooleanSetting("Disable Particles", true));
    public final BooleanSetting reduceEntityDistance = addSetting(new BooleanSetting("Reduce Entity Distance", true));
    public final NumberSetting particleCap = addSetting(new NumberSetting("Particle Cap", 50.0, 10.0, 200.0, 10.0));

    public FPSBoostModule() {
        super("FPSBoost", "Reduce particles and render load", ModuleCategory.PERFORMANCE);
        INSTANCE = this;
    }
}
