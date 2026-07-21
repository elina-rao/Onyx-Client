package com.onyxclient.mixins;

import com.onyxclient.modules.visual.CustomFontModule;
import com.onyxclient.utils.OnyxFont;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes plain (no §) FontRenderer draws through Outfit when Custom Font is enabled.
 */
@Mixin(FontRenderer.class)
public class MixinFontRenderer {

    private static final ThreadLocal<Boolean> DRAWING = new ThreadLocal<Boolean>();
    /** When set, skip Outfit so glyphs missing from TTF (e.g. ★) use vanilla atlas. */
    private static final ThreadLocal<Boolean> VANILLA_GLYPH = new ThreadLocal<Boolean>();

    /** Run a draw/width call that must use the real Minecraft font (not Outfit). */
    public static void withVanillaGlyphs(Runnable action) {
        VANILLA_GLYPH.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            VANILLA_GLYPH.remove();
        }
    }

    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
    private void onyx$drawString(String text, float x, float y, int color, boolean dropShadow,
                                 CallbackInfoReturnable<Integer> cir) {
        if (!shouldReplace(text)) {
            return;
        }
        DRAWING.set(Boolean.TRUE);
        try {
            if (dropShadow) {
                int shadow = (color & 0xFCFCFC) >> 2 | (color & 0xFF000000);
                OnyxFont.UI.drawString(text, x + 1.0F, y + 1.0F, shadow);
            }
            OnyxFont.UI.drawString(text, x, y, color);
            cir.setReturnValue(OnyxFont.UI.getStringWidth(text));
        } finally {
            DRAWING.remove();
        }
    }

    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
    private void onyx$getStringWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (!shouldReplace(text)) {
            return;
        }
        cir.setReturnValue(OnyxFont.UI.getStringWidth(text));
    }

    private static boolean shouldReplace(String text) {
        if (!CustomFontModule.isActive()) {
            return false;
        }
        if (Boolean.TRUE.equals(VANILLA_GLYPH.get())) {
            return false;
        }
        if (Boolean.TRUE.equals(DRAWING.get())) {
            return false;
        }
        if (text == null || text.isEmpty()) {
            return false;
        }
        // Keep vanilla path for formatting codes so § colors still work.
        if (text.indexOf('\u00a7') >= 0) {
            return false;
        }
        if (!OnyxFont.UI.isReady()) {
            return false;
        }
        return true;
    }
}
