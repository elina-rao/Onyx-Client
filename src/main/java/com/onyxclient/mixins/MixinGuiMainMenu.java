package com.onyxclient.mixins;

import com.onyxclient.gui.MainMenu;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private void onyx$redirectMainMenu(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if ((Object) this instanceof MainMenu) {
            return;
        }
    }
}
