package com.onyxclient.modules.rendering;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;

/**
 * Cosmetic item drop physics. Hooked via MixinEntityItem when registered.
 */
public class ItemPhysicsModule extends Module {

    public static ItemPhysicsModule INSTANCE;

    public ItemPhysicsModule() {
        super("ItemPhysics", "Cosmetic physics for dropped items", ModuleCategory.RENDERING);
        INSTANCE = this;
    }
}
