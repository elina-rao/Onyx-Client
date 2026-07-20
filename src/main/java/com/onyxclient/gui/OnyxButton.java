package com.onyxclient.gui;

import com.onyxclient.utils.Colors;
import com.onyxclient.utils.OnyxFont;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 * Soft rounded CTA matching Onyx Launcher buttons.
 */
public class OnyxButton extends GuiButton {

    private static final int RADIUS = 12;
    private float hoverAnim;

    public OnyxButton(int id, int x, int y, int width, int height, String label) {
        super(id, x, y, width, height, label);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        hovered = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;

        float target = hovered && enabled ? 1.0F : 0.0F;
        hoverAnim += (target - hoverAnim) * 0.22F;

        int fill = Colors.lerpColor(
                Colors.withAlpha(Colors.BG_CARD, 230),
                Colors.withAlpha(Colors.ACCENT_PRIMARY, 70),
                hoverAnim
        );
        int border = Colors.lerpColor(
                Colors.BORDER,
                Colors.ACCENT_BRIGHT,
                hoverAnim
        );

        // Soft shadow on hover
        if (hoverAnim > 0.05F) {
            int shadowA = (int) (28 * hoverAnim);
            RenderUtils.drawRoundedRect(xPosition, yPosition + 2, width, height, RADIUS,
                    Colors.withAlpha(0x000000, shadowA));
        }

        RenderUtils.drawRoundedRect(xPosition, yPosition, width, height, RADIUS, fill);
        RenderUtils.drawRoundedOutline(xPosition, yPosition, width, height, RADIUS, 1.0F, border);

        // Ensure clean GL before textured text
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        net.minecraft.client.renderer.GlStateManager.enableBlend();

        int textColor = enabled ? Colors.TEXT_PRIMARY : Colors.TEXT_DISABLED;
        float textY = yPosition + (height - OnyxFont.MEDIUM.getHeight()) / 2.0F;
        OnyxFont.MEDIUM.drawCenteredString(displayString, xPosition + width / 2.0F, textY, textColor);
    }
}
