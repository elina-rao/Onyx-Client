package com.onyxclient.mixins;

import com.onyxclient.OnyxClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public class MixinGuiScreen {

    /** Dirt options background — used when no world is loaded (menus). */
    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void onyx$skipDirt(int tint, CallbackInfo ci) {
        try {
            if (OnyxClient.getConfigManager() != null
                    && !OnyxClient.getConfigManager().getConfig().dirtScreen) {
                GuiScreen self = (GuiScreen) (Object) this;
                Gui.drawRect(0, 0, self.width, self.height, 0xFF101018);
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }
}
