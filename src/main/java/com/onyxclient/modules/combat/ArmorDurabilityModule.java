package com.onyxclient.modules.combat;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public class ArmorDurabilityModule extends HudModule {

    private final BooleanSetting showNumeric;
    private final ModeSetting iconStyle;

    public ArmorDurabilityModule() {
        super("ArmorDurability", "Deprecated: use ArmorStatus (merged feature set)", false);
        showNumeric = addSetting(new BooleanSetting("Show Numeric", true));
        iconStyle = addSetting(new ModeSetting("Icon Style", "Compact", "Compact", "Detailed"));
        setHudSize(80, 40);
        setHudPosition(2, 180);
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        mc.fontRendererObj.drawStringWithShadow("Use ArmorStatus module", hudX, hudY, Colors.TEXT_MUTED);
        setHudSize(110, 12);
    }
}
