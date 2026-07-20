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
        super("EntityCulling", "Skip rendering off-screen entities", ModuleCategory.PERFORMANCE);
        INSTANCE = this;
    }

    public boolean shouldRender(Entity entity) {
        if (!isEnabled()) {
            return true;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderViewEntity() == null) {
            return true;
        }
        if (mc.getRenderViewEntity().getDistanceToEntity(entity) > distance.getValue()) {
            return false;
        }
        frustum.setPosition(
                mc.getRenderManager().viewerPosX,
                mc.getRenderManager().viewerPosY,
                mc.getRenderManager().viewerPosZ
        );
        return frustum.isBoundingBoxInFrustum(entity.getEntityBoundingBox());
    }
}
