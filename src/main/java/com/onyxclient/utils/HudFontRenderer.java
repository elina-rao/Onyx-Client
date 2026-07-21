package com.onyxclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Lightweight bitmap-atlas HUD font renderer for 2D overlays.
 * Uses one atlas per size and renders printable ASCII glyphs.
 */
public final class HudFontRenderer {

    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR = 126;
    private static final int CHAR_COUNT = LAST_CHAR - FIRST_CHAR + 1;
    private static final int ATLAS_W = 1024;
    private static final int ATLAS_H = 512;
    private static final int PAD = 3;

    private static final HudFontRenderer SMALL = new HudFontRenderer("Outfit-Medium.ttf", 16F);
    private static final HudFontRenderer MEDIUM = new HudFontRenderer("Outfit-SemiBold.ttf", 18F);

    private final String fontFile;
    private final float size;

    private boolean attempted;
    private boolean ready;
    private DynamicTexture atlasTexture;
    private int textureId;
    private final Glyph[] glyphs = new Glyph[CHAR_COUNT];
    private int lineHeight = 10;

    private HudFontRenderer(String fontFile, float size) {
        this.fontFile = fontFile;
        this.size = size;
    }

    public static HudFontRenderer regular() {
        return SMALL;
    }

    public static HudFontRenderer accent() {
        return MEDIUM;
    }

    public boolean isReady() {
        ensureAtlas();
        return ready;
    }

    public int lineHeight() {
        ensureAtlas();
        return lineHeight;
    }

    public int width(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        ensureAtlas();
        if (!ready) {
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        }
        float x = 0.0F;
        for (int i = 0; i < text.length(); i++) {
            x += glyphAdvance(text.charAt(i));
        }
        return Math.round(x);
    }

    public void draw(String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        ensureAtlas();
        if (!ready) {
            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            if (shadow) {
                fr.drawStringWithShadow(text, x, y, color);
            } else {
                fr.drawString(text, (int) x, (int) y, color, false);
            }
            return;
        }
        if (shadow) {
            drawInternal(text, x + 0.8F, y + 0.8F, Colors.withAlpha(0x000000, Math.max(90, (color >> 24) & 0xFF)));
        }
        drawInternal(text, x, y, color);
    }

    private void drawInternal(String text, float x, float y, int color) {
        float a = ((color >> 24) & 255) / 255.0F;
        if (a == 0.0F) {
            a = 1.0F;
        }
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlpha();
        GlStateManager.bindTexture(textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.color(r, g, b, a);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        float penX = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Glyph glyph = glyphFor(c);
            if (glyph == null) {
                penX += glyphAdvance(c);
                continue;
            }
            float x1 = penX + glyph.xOffset;
            float y1 = y + glyph.yOffset;
            float x2 = x1 + glyph.w;
            float y2 = y1 + glyph.h;

            wr.pos(x1, y2, 0).tex(glyph.u0, glyph.v1).endVertex();
            wr.pos(x2, y2, 0).tex(glyph.u1, glyph.v1).endVertex();
            wr.pos(x2, y1, 0).tex(glyph.u1, glyph.v0).endVertex();
            wr.pos(x1, y1, 0).tex(glyph.u0, glyph.v0).endVertex();
            penX += glyph.advance;
        }
        tess.draw();

        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private float glyphAdvance(char c) {
        Glyph glyph = glyphFor(c);
        if (glyph != null) {
            return glyph.advance;
        }
        if (c == ' ') {
            return Math.max(4, lineHeight / 2.0F);
        }
        return Math.max(5, lineHeight * 0.55F);
    }

    private Glyph glyphFor(char c) {
        if (c < FIRST_CHAR || c > LAST_CHAR) {
            return null;
        }
        return glyphs[c - FIRST_CHAR];
    }

    private void ensureAtlas() {
        if (attempted) {
            return;
        }
        attempted = true;
        try {
            InputStream stream = HudFontRenderer.class.getResourceAsStream("/assets/onyxclient/fonts/" + fontFile);
            if (stream == null) {
                ready = false;
                return;
            }
            Font base = Font.createFont(Font.TRUETYPE_FONT, stream);
            stream.close();
            Font font = base.deriveFont(size);

            BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D pg = probe.createGraphics();
            pg.setFont(font);
            pg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            pg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            FontMetrics fm = pg.getFontMetrics();
            int ascent = fm.getAscent();
            int descent = fm.getDescent();
            lineHeight = Math.max(8, ascent + descent);
            pg.dispose();

            BufferedImage atlas = new BufferedImage(ATLAS_W, ATLAS_H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = atlas.createGraphics();
            g.setFont(font);
            g.setColor(Color.WHITE);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            FontMetrics atlasFm = g.getFontMetrics();

            int cursorX = PAD;
            int cursorY = PAD;
            int rowH = 0;

            for (int code = FIRST_CHAR; code <= LAST_CHAR; code++) {
                char c = (char) code;
                int gw = Math.max(1, atlasFm.charWidth(c));
                int gh = Math.max(1, lineHeight);
                int boxW = gw + PAD * 2;
                int boxH = gh + PAD * 2;
                if (cursorX + boxW >= ATLAS_W) {
                    cursorX = PAD;
                    cursorY += rowH + PAD;
                    rowH = 0;
                }
                if (cursorY + boxH >= ATLAS_H) {
                    g.dispose();
                    ready = false;
                    return;
                }
                g.drawString(String.valueOf(c), cursorX + PAD, cursorY + PAD + ascent);
                Glyph glyph = new Glyph();
                glyph.u0 = cursorX / (float) ATLAS_W;
                glyph.v0 = cursorY / (float) ATLAS_H;
                glyph.u1 = (cursorX + boxW) / (float) ATLAS_W;
                glyph.v1 = (cursorY + boxH) / (float) ATLAS_H;
                glyph.w = boxW;
                glyph.h = boxH;
                glyph.xOffset = -PAD;
                glyph.yOffset = -PAD;
                glyph.advance = gw + 1.0F;
                glyphs[code - FIRST_CHAR] = glyph;

                cursorX += boxW + PAD;
                rowH = Math.max(rowH, boxH);
            }
            g.dispose();

            atlasTexture = new DynamicTexture(atlas);
            textureId = atlasTexture.getGlTextureId();
            ready = true;
        } catch (Exception ignored) {
            ready = false;
        }
    }

    private static final class Glyph {
        private float u0;
        private float v0;
        private float u1;
        private float v1;
        private int w;
        private int h;
        private float xOffset;
        private float yOffset;
        private float advance;
    }
}
