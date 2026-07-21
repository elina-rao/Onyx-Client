package com.onyxclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

public final class RenderUtils {

    private static final int ARC_SEGMENTS = 32;
    private static final Deque<int[]> SCISSOR_STACK = new ArrayDeque<int[]>();
    /** Stack marker: enableScissor skipped (invalid size); GL state unchanged. */
    private static final int[] SCISSOR_NOOP = new int[0];

    private RenderUtils() {
    }

    /**
     * Clip subsequent draws to a GuiScreen-space rectangle.
     * Converts to OpenGL framebuffer coords using framebuffer / scaled-GUI ratios
     * (Retina / OptiFine safe — not a single getScaleFactor()).
     * Supports nesting via an intersect stack.
     */
    public static void enableScissor(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            // Do not blank the framebuffer with a 0×0 scissor; pair with disableScissor.
            SCISSOR_STACK.push(SCISSOR_NOOP);
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int fbW = mc.displayWidth;
        int fbH = mc.displayHeight;
        Framebuffer fb = mc.getFramebuffer();
        if (fb != null && fb.framebufferWidth > 0 && fb.framebufferHeight > 0) {
            fbW = fb.framebufferWidth;
            fbH = fb.framebufferHeight;
        }
        int scaledW = Math.max(1, sr.getScaledWidth());
        int scaledH = Math.max(1, sr.getScaledHeight());
        float scaleX = fbW / (float) scaledW;
        float scaleY = fbH / (float) scaledH;

        int sx = Math.round(x * scaleX);
        int sw = Math.max(0, Math.round(width * scaleX));
        int sh = Math.max(0, Math.round(height * scaleY));
        int sy = Math.round(fbH - (y + height) * scaleY);

        // Clamp to framebuffer so glScissor never gets negative extents
        if (sx < 0) {
            sw += sx;
            sx = 0;
        }
        if (sy < 0) {
            sh += sy;
            sy = 0;
        }
        if (sx + sw > fbW) {
            sw = fbW - sx;
        }
        if (sy + sh > fbH) {
            sh = fbH - sy;
        }
        sw = Math.max(0, sw);
        sh = Math.max(0, sh);

        if (!SCISSOR_STACK.isEmpty()) {
            int[] parent = SCISSOR_STACK.peek();
            if (parent.length == 4) {
                int px = parent[0];
                int py = parent[1];
                int pw = parent[2];
                int ph = parent[3];
                int x0 = Math.max(sx, px);
                int y0 = Math.max(sy, py);
                int x1 = Math.min(sx + sw, px + pw);
                int y1 = Math.min(sy + sh, py + ph);
                sx = x0;
                sy = y0;
                sw = Math.max(0, x1 - x0);
                sh = Math.max(0, y1 - y0);
            }
        }

        SCISSOR_STACK.push(new int[]{sx, sy, sw, sh});
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);
    }

    public static void disableScissor() {
        if (SCISSOR_STACK.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return;
        }
        SCISSOR_STACK.pop();
        if (SCISSOR_STACK.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return;
        }
        int[] parent = SCISSOR_STACK.peek();
        if (parent.length != 4) {
            // Still inside a noop / skipped level — leave GL as restored by next real parent walk
            restoreTopScissor();
            return;
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(parent[0], parent[1], parent[2], parent[3]);
    }

    private static void restoreTopScissor() {
        int[] real = null;
        for (int[] entry : SCISSOR_STACK) {
            if (entry.length == 4) {
                real = entry;
                break; // head = top of stack
            }
        }
        if (real == null) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(real[0], real[1], real[2], real[3]);
        }
    }

    /** Drop any nested clip state and turn off GL scissor (safe across GUI switches). */
    public static void clearScissor() {
        SCISSOR_STACK.clear();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void drawRect(int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float alpha = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x, y + height, 0.0D).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).endVertex();
        worldrenderer.pos(x, y, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawGradientRect(int x, int y, int width, int height, int colorTop, int colorBottom) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float a1 = (colorTop >> 24 & 255) / 255.0F;
        float r1 = (colorTop >> 16 & 255) / 255.0F;
        float g1 = (colorTop >> 8 & 255) / 255.0F;
        float b1 = (colorTop & 255) / 255.0F;
        float a2 = (colorBottom >> 24 & 255) / 255.0F;
        float r2 = (colorBottom >> 16 & 255) / 255.0F;
        float g2 = (colorBottom >> 8 & 255) / 255.0F;
        float b2 = (colorBottom & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x + width, y, 0.0D).color(r1, g1, b1, a1).endVertex();
        wr.pos(x, y, 0.0D).color(r1, g1, b1, a1).endVertex();
        wr.pos(x, y + height, 0.0D).color(r2, g2, b2, a2).endVertex();
        wr.pos(x + width, y + height, 0.0D).color(r2, g2, b2, a2).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawHorizontalGradient(int x, int y, int width, int height, int colorLeft, int colorRight) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float a1 = (colorLeft >> 24 & 255) / 255.0F;
        float r1 = (colorLeft >> 16 & 255) / 255.0F;
        float g1 = (colorLeft >> 8 & 255) / 255.0F;
        float b1 = (colorLeft & 255) / 255.0F;
        float a2 = (colorRight >> 24 & 255) / 255.0F;
        float r2 = (colorRight >> 16 & 255) / 255.0F;
        float g2 = (colorRight >> 8 & 255) / 255.0F;
        float b2 = (colorRight & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x, y, 0.0D).color(r1, g1, b1, a1).endVertex();
        wr.pos(x, y + height, 0.0D).color(r1, g1, b1, a1).endVertex();
        wr.pos(x + width, y + height, 0.0D).color(r2, g2, b2, a2).endVertex();
        wr.pos(x + width, y, 0.0D).color(r2, g2, b2, a2).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    /**
     * Filled rounded rect — body quads + per-corner fans (matches the outline curve).
     * Minecraft GUI: Y increases downward. Angles: 0=right, 90=down, 180=left, 270=up.
     */
    public static void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (radius <= 0) {
            drawRect(x, y, width, height, color);
            return;
        }
        float r = Math.min(radius, Math.min(width, height) / 2.0F);
        float x0 = x;
        float y0 = y;
        float x1 = x + width;
        float y1 = y + height;

        float alpha = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        // Body (no corners) — float quads so edges meet the arcs cleanly
        drawQuadF(x0 + r, y0, x1 - r, y1);
        drawQuadF(x0, y0 + r, x0 + r, y1 - r);
        drawQuadF(x1 - r, y0 + r, x1, y1 - r);

        // Corner wedges — fan from each corner center (same arcs as the outline)
        int segs = Math.max(12, ARC_SEGMENTS / 2);
        drawCornerFan(x0 + r, y0 + r, r, 180, 270, segs);
        drawCornerFan(x1 - r, y0 + r, r, 270, 360, segs);
        drawCornerFan(x1 - r, y1 - r, r, 0, 90, segs);
        drawCornerFan(x0 + r, y1 - r, r, 90, 180, segs);

        if (cullWasEnabled) {
            GlStateManager.enableCull();
        }
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawQuadF(float x0, float y0, float x1, float y1) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(x0, y1, 0.0D).endVertex();
        wr.pos(x1, y1, 0.0D).endVertex();
        wr.pos(x1, y0, 0.0D).endVertex();
        wr.pos(x0, y0, 0.0D).endVertex();
        tessellator.draw();
    }

    private static void drawCornerFan(float cx, float cy, float r, int startDeg, int endDeg, int segs) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(cx, cy, 0.0D).endVertex();
        for (int i = 0; i <= segs; i++) {
            double angle = Math.toRadians(startDeg + (endDeg - startDeg) * (i / (double) segs));
            wr.pos(cx + Math.cos(angle) * r, cy + Math.sin(angle) * r, 0.0D).endVertex();
        }
        tessellator.draw();
    }

    /** Walk the rounded-rect outline clockwise starting at left side of top-left corner. */
    private static void appendRoundedPerimeter(WorldRenderer wr, float x0, float y0, float x1, float y1, float r) {
        int segs = Math.max(12, ARC_SEGMENTS / 2);
        appendCorner(wr, x0 + r, y0 + r, r, 180, 270, segs);
        appendCorner(wr, x1 - r, y0 + r, r, 270, 360, segs);
        appendCorner(wr, x1 - r, y1 - r, r, 0, 90, segs);
        appendCorner(wr, x0 + r, y1 - r, r, 90, 180, segs);
    }

    private static void appendCorner(WorldRenderer wr, float cx, float cy, float r, int startDeg, int endDeg, int segs) {
        for (int i = 0; i <= segs; i++) {
            double angle = Math.toRadians(startDeg + (endDeg - startDeg) * (i / (double) segs));
            wr.pos(cx + Math.cos(angle) * r, cy + Math.sin(angle) * r, 0.0D).endVertex();
        }
    }

    public static void drawSoftShadow(int x, int y, int width, int height, int radius, int layers) {
        for (int i = layers; i >= 1; i--) {
            int expand = i;
            int alpha = Math.max(6, 14 / i);
            drawRoundedRect(x - expand, y - expand + 2, width + expand * 2, height + expand * 2,
                    radius + expand, Colors.withAlpha(0x000000, alpha));
        }
    }

    public static void drawGlow(int x, int y, int width, int height, int radius, int color, int layers) {
        for (int i = layers; i >= 1; i--) {
            int spread = i * 2;
            int alpha = Math.max(4, ((color >> 24) & 0xFF) / (layers + 2));
            drawRoundedRect(x - spread, y - spread, width + spread * 2, height + spread * 2, radius + spread,
                    Colors.withAlpha(color, alpha));
        }
    }

    /**
     * Rounded outline matching the single-mesh fill perimeter.
     */
    public static void drawRoundedOutline(int x, int y, int width, int height, int radius, float thickness, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (radius <= 0) {
            drawOutline(x, y, width, height, color);
            return;
        }
        float r = Math.min(radius, Math.min(width, height) / 2.0F);
        float x0 = x + 0.5F;
        float y0 = y + 0.5F;
        float x1 = x + width - 0.5F;
        float y1 = y + height - 0.5F;

        float alpha = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
        GL11.glLineWidth(Math.max(1.0F, thickness));
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        appendRoundedPerimeter(wr, x0, y0, x1, y1, Math.max(0.5F, r - 0.5F));
        tessellator.draw();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawHorizontalLine(int x, int y, int width, int color) {
        drawRect(x, y, width, 1, color);
    }

    public static void drawVerticalLine(int x, int y, int height, int color) {
        drawRect(x, y, 1, height, color);
    }

    public static void drawOutline(int x, int y, int width, int height, int color) {
        drawHorizontalLine(x, y, width, color);
        drawHorizontalLine(x, y + height - 1, width, color);
        drawVerticalLine(x, y, height, color);
        drawVerticalLine(x + width - 1, y, height, color);
    }

    public static void drawDashedRect(int x, int y, int width, int height, int color, int dashLength) {
        for (int dx = x; dx < x + width; dx += dashLength * 2) {
            drawHorizontalLine(dx, y, Math.min(dashLength, x + width - dx), color);
            drawHorizontalLine(dx, y + height - 1, Math.min(dashLength, x + width - dx), color);
        }
        for (int dy = y; dy < y + height; dy += dashLength * 2) {
            drawVerticalLine(x, dy, Math.min(dashLength, y + height - dy), color);
            drawVerticalLine(x + width - 1, dy, Math.min(dashLength, y + height - dy), color);
        }
    }

    public static void drawCircle(int centerX, int centerY, int radius, int color) {
        drawCircleF(centerX, centerY, radius, color);
    }

    public static void drawCircleF(float centerX, float centerY, float radius, int color) {
        if (radius <= 0) {
            return;
        }
        float alpha = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(centerX, centerY, 0.0D).endVertex();
        int segs = Math.max(16, ARC_SEGMENTS * 2);
        for (int i = 0; i <= segs; i++) {
            double angle = Math.toRadians(i * 360.0 / segs);
            wr.pos(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius, 0.0D).endVertex();
        }
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /**
     * Small filled heart (Y-down GUI). Size is approximate height in pixels.
     */
    public static void drawHeart(float x, float y, float size, int color) {
        float alpha = (color >> 24 & 255) / 255.0F;
        if (alpha == 0.0F) {
            alpha = 1.0F;
        }
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        float s = size;
        // Two lobes
        float lobeR = s * 0.28F;
        float leftCx = x + s * 0.32F;
        float rightCx = x + s * 0.68F;
        float lobeCy = y + s * 0.32F;
        drawCircleF(leftCx, lobeCy, lobeR, color);
        drawCircleF(rightCx, lobeCy, lobeR, color);

        // Re-apply color (drawCircleF restores GL state)
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        // Lower diamond / V
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION);
        float topY = y + s * 0.38F;
        float tipX = x + s * 0.5F;
        float tipY = y + s * 0.95F;
        float leftX = x + s * 0.08F;
        float rightX = x + s * 0.92F;
        wr.pos(leftX, topY, 0).endVertex();
        wr.pos(tipX, tipY, 0).endVertex();
        wr.pos(rightX, topY, 0).endVertex();
        // Fill the middle between lobes
        wr.pos(leftCx, lobeCy, 0).endVertex();
        wr.pos(rightCx, lobeCy, 0).endVertex();
        wr.pos(tipX, tipY, 0).endVertex();
        tess.draw();

        if (cullWasEnabled) {
            GlStateManager.enableCull();
        }
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Soft edge darkening — stacked translucent frames (launcher-style vignette).
     * @param strength 0..1 typical 0.45–0.7
     */
    public static void drawVignette(int width, int height, float strength) {
        strength = Math.max(0.0F, Math.min(1.0F, strength));
        int layers = 6;
        for (int i = 0; i < layers; i++) {
            float t = (i + 1) / (float) layers;
            int inset = (int) (Math.min(width, height) * 0.04F * t);
            int alpha = (int) (28 * strength * t);
            int color = Colors.withAlpha(0x000000, alpha);
            // top
            drawRect(0, 0, width, inset, color);
            // bottom
            drawRect(0, height - inset, width, inset, color);
            // left
            drawRect(0, inset, inset, height - inset * 2, color);
            // right
            drawRect(width - inset, inset, inset, height - inset * 2, color);
        }
        // Extra bottom/top gradient wash
        drawGradientRect(0, 0, width, height / 5, Colors.withAlpha(0x000000, (int) (90 * strength)), Colors.withAlpha(0x000000, 0));
        drawGradientRect(0, height - height / 4, width, height / 4, Colors.withAlpha(0x000000, 0), Colors.withAlpha(0x000000, (int) (110 * strength)));
    }
}
