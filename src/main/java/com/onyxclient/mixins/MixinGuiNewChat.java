package com.onyxclient.mixins;

import com.onyxclient.modules.customization.CustomChatModule;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

@Mixin(GuiNewChat.class)
public class MixinGuiNewChat {

    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm");

    @ModifyVariable(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private IChatComponent onyx$timestampAndStack(IChatComponent component) {
        if (component == null || CustomChatModule.INSTANCE == null || !CustomChatModule.INSTANCE.isEnabled()) {
            return component;
        }
        IChatComponent result = component;
        String stack = CustomChatModule.INSTANCE.consumeStackSuffix(component.getUnformattedText());
        if (stack != null) {
            ChatComponentText stacked = new ChatComponentText("");
            stacked.appendSibling(component);
            stacked.appendSibling(new ChatComponentText(stack));
            result = stacked;
        }
        if (CustomChatModule.INSTANCE.showTimestamps()) {
            String stamp = "§8[" + TIME.format(new Date()) + "] §r";
            ChatComponentText prefixed = new ChatComponentText(stamp);
            prefixed.appendSibling(result);
            return prefixed;
        }
        return result;
    }

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void onyx$scaleChatPre(int updateCounter, CallbackInfo ci) {
        if (CustomChatModule.INSTANCE == null || !CustomChatModule.INSTANCE.isEnabled()) {
            return;
        }
        float scale = CustomChatModule.INSTANCE.getFontScale();
        if (Math.abs(scale - 1.0F) < 0.01F) {
            return;
        }
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.scale(scale, scale, 1.0F);
    }

    @Inject(method = "drawChat", at = @At("RETURN"))
    private void onyx$scaleChatPost(int updateCounter, CallbackInfo ci) {
        if (CustomChatModule.INSTANCE == null || !CustomChatModule.INSTANCE.isEnabled()) {
            return;
        }
        float scale = CustomChatModule.INSTANCE.getFontScale();
        if (Math.abs(scale - 1.0F) < 0.01F) {
            return;
        }
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }
}
