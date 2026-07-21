package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import com.onyxclient.utils.HudFontRenderer;
import com.onyxclient.utils.HudTheme;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class KeyStrokesModule extends HudModule {

    private float wAnim, aAnim, sAnim, dAnim, lAnim, rAnim;

    public KeyStrokesModule() {
        super("KeyStrokes", "Animated WASD and click display", true);
        setUseScaledBounds(true);
        setHudSize(74, 74);
        hudX = 4;
        hudY = 4;
        enablePremiumDefaults();
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        updateAnimations();
        beginHudScale();
        drawKey(hudX + 24, hudY, 22, 22, "W", wAnim);
        drawKey(hudX, hudY + 24, 22, 22, "A", aAnim);
        drawKey(hudX + 24, hudY + 24, 22, 22, "S", sAnim);
        drawKey(hudX + 48, hudY + 24, 22, 22, "D", dAnim);
        drawKey(hudX, hudY + 48, 34, 22, "LMB", lAnim);
        drawKey(hudX + 36, hudY + 48, 34, 22, "RMB", rAnim);
        setHudSize(74, 74);
        endHudScale();
    }

    private void updateAnimations() {
        boolean w = Keyboard.isKeyDown(Keyboard.KEY_W);
        boolean a = Keyboard.isKeyDown(Keyboard.KEY_A);
        boolean s = Keyboard.isKeyDown(Keyboard.KEY_S);
        boolean d = Keyboard.isKeyDown(Keyboard.KEY_D);
        boolean l = Mouse.isButtonDown(0);
        boolean r = Mouse.isButtonDown(1);

        float factor = reducedMotion() ? 1.0F : 0.3F;
        wAnim = lerp(wAnim, w ? 1.0F : 0.0F, factor);
        aAnim = lerp(aAnim, a ? 1.0F : 0.0F, factor);
        sAnim = lerp(sAnim, s ? 1.0F : 0.0F, factor);
        dAnim = lerp(dAnim, d ? 1.0F : 0.0F, factor);
        lAnim = lerp(lAnim, l ? 1.0F : 0.0F, factor);
        rAnim = lerp(rAnim, r ? 1.0F : 0.0F, factor);
    }

    private float lerp(float current, float target, float factor) {
        return current + (target - current) * factor;
    }

    private void drawKey(int x, int y, int w, int h, String label, float pressAnim) {
        int bg = Colors.lerpColor(Colors.withAlpha(Colors.BG_CARD, 210), Colors.withAlpha(Colors.ACCENT_PRIMARY, 120), pressAnim);
        int border = Colors.lerpColor(HudTheme.CARD_BORDER, Colors.ACCENT_BRIGHT, pressAnim);
        int radius = usePremiumCard() ? 6 : 4;
        RenderUtils.drawRoundedRect(x, y, w, h, radius, bg);
        RenderUtils.drawRoundedOutline(x, y, w, h, radius, 1.0F, border);

        Minecraft mc = Minecraft.getMinecraft();
        int textColor = Colors.lerpColor(HudTheme.VALUE, Colors.TEXT_PRIMARY, pressAnim);
        if (usePremiumRenderer()) {
            int tw = HudFontRenderer.regular().width(label);
            int th = HudFontRenderer.regular().lineHeight();
            HudFontRenderer.regular().draw(label, x + (w - tw) / 2.0F, y + (h - th) / 2.0F, textColor, false);
        } else {
            int tw = mc.fontRendererObj.getStringWidth(label);
            mc.fontRendererObj.drawString(label, x + (w - tw) / 2, y + (h - 8) / 2, textColor);
        }
    }
}
