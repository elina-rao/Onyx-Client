package com.onyxclient.modules.stats;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;

public class RankEloModule extends HudModule {

    private final BooleanSetting showIcon;
    private String cached = "—";

    public RankEloModule() {
        super("RankElo", "Live rank/ELO display", false);
        showIcon = addSetting(new BooleanSetting("Show Rank Icon", true));
        setHudSize(80, 12);
        setHudPosition(2, 340);
    }

    public void setCached(String value) {
        this.cached = value != null ? value : "—";
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        String text = (showIcon.getValue() ? "◆ " : "") + "ELO " + cached;
        mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.ACCENT_BRIGHT);
        setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
    }
}
