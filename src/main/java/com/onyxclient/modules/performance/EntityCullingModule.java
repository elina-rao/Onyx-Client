package com.onyxclient.modules.performance;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;

public class EntityCullingModule extends Module {

    public static EntityCullingModule INSTANCE;
    public final NumberSetting distance = addSetting(new NumberSetting("Cull Distance", 48.0, 16.0, 128.0, 4.0));
    private final Frustum frustum = new Frustum();

    public EntityCullingModule() {
        super("EntityCulling", "Skip rendering off-screen entities", ModuleCategory.PERFORMANCE, true);
        INSTANCE = this;
    }

    public boolean shouldRender(Entity entity) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderViewEntity() == null) {
            return true;
        }
        if (entity == mc.getRenderViewEntity()) {
            return true;
        }

        double maxDist = Double.MAX_VALUE;
        boolean cull = isEnabled();
        if (cull) {
            maxDist = distance.getValue();
        }
        if (FPSBoostModule.INSTANCE != null && FPSBoostModule.INSTANCE.shouldCullDistantEntities()) {
            cull = true;
            maxDist = Math.min(maxDist, FPSBoostModule.INSTANCE.getReducedEntityDistance());
        }
        if (!cull) {
            return true;
        }

        if (mc.getRenderViewEntity().getDistanceToEntity(entity) > maxDist) {
            return false;
        }
        if (!isEnabled()) {
            // FPSBoost distance-only path — skip frustum when EntityCulling module off
            return true;
        }
        frustum.setPosition(
                mc.getRenderManager().viewerPosX,
                mc.getRenderManager().viewerPosY,
                mc.getRenderManager().viewerPosZ
        );
        return frustum.isBoundingBoxInFrustum(entity.getEntityBoundingBox());
    }
}
