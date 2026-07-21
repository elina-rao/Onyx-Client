package com.onyxclient.modules.visual;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CrosshairModule extends Module {

    public static CrosshairModule INSTANCE;

    public final NumberSetting size = addSetting(new NumberSetting("Size", 5.0, 1.0, 20.0, 1.0));
    public final NumberSetting thickness = addSetting(new NumberSetting("Thickness", 1.0, 1.0, 5.0, 1.0));
    public final NumberSetting gap = addSetting(new NumberSetting("Gap", 3.0, 0.0, 15.0, 1.0));
    public final BooleanSetting dot = addSetting(new BooleanSetting("Dot", false));
    public final ColorSetting color = addSetting(new ColorSetting("Color", Colors.TEXT_PRIMARY));
    public final BooleanSetting reachColor = addSetting(new BooleanSetting("Reach Color", true));
    public final ColorSetting inReachColor = addSetting(new ColorSetting("In-Reach Color", 0xFFFF5555));
    public final NumberSetting reachDistance = addSetting(new NumberSetting("Reach Distance", 3.0, 2.0, 4.5, 0.1));

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
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth() / 2;
        int sh = sr.getScaledHeight() / 2;
        int c = color.getValue();
        if (reachColor.getValue() && isTargetInReach(mc)) {
            c = inReachColor.getValue();
        }
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

    private boolean isTargetInReach(Minecraft mc) {
        if (mc.thePlayer == null) {
            return false;
        }
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) {
            return false;
        }
        Entity entity = mop.entityHit;
        if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer) {
            return false;
        }
        double reach = reachDistance.getFloatValue();
        if (mop.hitVec != null) {
            double dist = mc.thePlayer.getPositionEyes(1.0F).distanceTo(mop.hitVec);
            return dist <= reach;
        }
        double distSq = mc.thePlayer.getDistanceSqToEntity(entity);
        return distSq <= reach * reach;
    }
}
