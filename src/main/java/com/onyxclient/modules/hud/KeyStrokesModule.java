package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class KeyStrokesModule extends HudModule {

    private boolean wasW, wasA, wasS, wasD;
    private float wAnim, aAnim, sAnim, dAnim, lAnim, rAnim;

    public KeyStrokesModule() {
        super("KeyStrokes", "Animated WASD and click display", true);
        setHudSize(70, 70);
        hudX = 4;
        hudY = 4;
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        updateAnimations();

        drawKey(hudX + 24, hudY, 22, 22, "W", wAnim);
        drawKey(hudX, hudY + 24, 22, 22, "A", aAnim);
        drawKey(hudX + 24, hudY + 24, 22, 22, "S", sAnim);
        drawKey(hudX + 48, hudY + 24, 22, 22, "D", dAnim);
        drawKey(hudX, hudY + 48, 33, 22, "LMB", lAnim);
        drawKey(hudX + 37, hudY + 48, 33, 22, "RMB", rAnim);
    }

    private void updateAnimations() {
        boolean w = Keyboard.isKeyDown(Keyboard.KEY_W);
        boolean a = Keyboard.isKeyDown(Keyboard.KEY_A);
        boolean s = Keyboard.isKeyDown(Keyboard.KEY_S);
        boolean d = Keyboard.isKeyDown(Keyboard.KEY_D);
        boolean l = Mouse.isButtonDown(0);
        boolean r = Mouse.isButtonDown(1);

        wAnim = lerp(wAnim, w ? 1.0F : 0.0F);
        aAnim = lerp(aAnim, a ? 1.0F : 0.0F);
        sAnim = lerp(sAnim, s ? 1.0F : 0.0F);
        dAnim = lerp(dAnim, d ? 1.0F : 0.0F);
        lAnim = lerp(lAnim, l ? 1.0F : 0.0F);
        rAnim = lerp(rAnim, r ? 1.0F : 0.0F);

        wasW = w;
        wasA = a;
        wasS = s;
        wasD = d;
    }

    private float lerp(float current, float target) {
        return current + (target - current) * 0.3F;
    }

    private void drawKey(int x, int y, int w, int h, String label, float pressAnim) {
        int bg = Colors.lerpColor(Colors.BG_CARD, Colors.ACCENT_PRIMARY, pressAnim);
        RenderUtils.drawRoundedRect(x, y, w, h, 4, bg);
        RenderUtils.drawRoundedOutline(x, y, w, h, 4, 1.0F, Colors.ACCENT_PRIMARY);
        int tw = Minecraft.getMinecraft().fontRendererObj.getStringWidth(label);
        Minecraft.getMinecraft().fontRendererObj.drawString(label, x + (w - tw) / 2, y + (h - 8) / 2, Colors.TEXT_PRIMARY);
    }
}
