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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Anti-aliased TTF renderer using a per-string texture cache.
 * Avoids shared-atlas packing bugs (missing glyphs / UV bleed).
 */
public final class OnyxFont {

    /** Dense Mod Menu / Custom Font chrome — near vanilla row height; same 2× AA as SMALL. */
    public static final OnyxFont UI = new OnyxFont("Outfit-Regular.ttf", 9);
    public static final OnyxFont SMALL = new OnyxFont("Outfit-Regular.ttf", 16);
    public static final OnyxFont MEDIUM = new OnyxFont("Outfit-Medium.ttf", 20);
    public static final OnyxFont LARGE = new OnyxFont("Outfit-SemiBold.ttf", 32);
    public static final OnyxFont TITLE = new OnyxFont("Outfit-SemiBold.ttf", 40);

    private static final int CACHE_LIMIT = 256;
    private static final int PAD = 4;
    private static final int SCALE = 2; // render at 2x for smoother AA

    private final String fontFile;
    private final float pixelSize;
    private boolean ready;
    private boolean attempted;
    private Font awtFont;
    private int fontHeight;

    private final Map<String, CachedString> cache = new LinkedHashMap<String, CachedString>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedString> eldest) {
            if (size() > CACHE_LIMIT) {
                eldest.getValue().dispose();
                return true;
            }
            return false;
        }
    };

    private OnyxFont(String fontFile, float pixelSize) {
        this.fontFile = fontFile;
        this.pixelSize = pixelSize;
    }

    private void ensureLoaded() {
        if (attempted) {
            return;
        }
        attempted = true;
        try {
            InputStream stream = OnyxFont.class.getResourceAsStream("/assets/onyxclient/fonts/" + fontFile);
            if (stream == null) {
                System.err.println("[OnyxFont] Missing font resource: " + fontFile);
                return;
            }
            Font base = Font.createFont(Font.TRUETYPE_FONT, stream);
            stream.close();
            awtFont = base.deriveFont(pixelSize * SCALE);
            // Measure display height at 1x
            BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = tmp.createGraphics();
            g.setFont(awtFont);
            FontMetrics fm = g.getFontMetrics();
            fontHeight = Math.max(1, Math.round((fm.getAscent() + fm.getDescent()) / (float) SCALE));
            g.dispose();
            ready = true;
        } catch (Exception e) {
            System.err.println("[OnyxFont] Failed to load " + fontFile + ": " + e.getMessage());
            ready = false;
        }
    }

    public boolean isReady() {
        ensureLoaded();
        return ready;
    }

    public int getHeight() {
        ensureLoaded();
        if (!ready) {
            return 9;
        }
        return fontHeight;
    }

    public int getStringWidth(String text) {
        ensureLoaded();
        if (text == null || text.isEmpty()) {
            return 0;
        }
        if (!ready) {
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        }
        CachedString cached = getOrCreate(text);
        return cached != null ? cached.displayWidth : 0;
    }

    public void drawString(String text, float x, float y, int color) {
        ensureLoaded();
        if (text == null || text.isEmpty()) {
            return;
        }
        if (!ready) {
            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            fr.drawString(text, (int) x, (int) y, color, false);
            return;
        }

        CachedString cached = getOrCreate(text);
        if (cached == null) {
            return;
        }

        float a = ((color >> 24) & 255) / 255.0F;
        if (a == 0.0F) {
            a = 1.0F;
        }
        float r = ((color >> 16) & 255) / 255.0F;
        float gCol = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.bindTexture(cached.texId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.color(r, gCol, b, a);

        float x1 = x - PAD / (float) SCALE;
        float y1 = y - PAD / (float) SCALE;
        float x2 = x1 + cached.displayWidth + (PAD * 2) / (float) SCALE;
        float y2 = y1 + cached.displayHeight + (PAD * 2) / (float) SCALE;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x1, y2, 0).tex(0, 1).endVertex();
        wr.pos(x2, y2, 0).tex(1, 1).endVertex();
        wr.pos(x2, y1, 0).tex(1, 0).endVertex();
        wr.pos(x1, y1, 0).tex(0, 0).endVertex();
        tess.draw();

        // Restore color/alpha; keep blend on for translucent UI that follows text
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
    }

    public void drawCenteredString(String text, float x, float y, int color) {
        drawString(text, x - getStringWidth(text) / 2.0F, y, color);
    }

    public void drawStringWithShadow(String text, float x, float y, int color) {
        int shadow = (color & 0xFF000000) | 0x000000;
        drawString(text, x + 1.0F, y + 1.0F, shadow);
        drawString(text, x, y, color);
    }

    /** Trim with ellipsis using this font's metrics (not vanilla widths). */
    public String trimToWidth(String text, int maxPx) {
        if (maxPx <= 0 || text == null) {
            return "";
        }
        if (getStringWidth(text) <= maxPx) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisW = getStringWidth(ellipsis);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (getStringWidth(sb.toString() + text.charAt(i)) + ellipsisW > maxPx) {
                break;
            }
            sb.append(text.charAt(i));
        }
        return sb.append(ellipsis).toString();
    }

    private CachedString getOrCreate(String text) {
        CachedString existing = cache.get(text);
        if (existing != null) {
            return existing;
        }
        try {
            CachedString created = rasterize(text);
            cache.put(text, created);
            return created;
        } catch (Exception e) {
            System.err.println("[OnyxFont] Rasterize failed for \"" + text + "\": " + e.getMessage());
            return null;
        }
    }

    private CachedString rasterize(String text) {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        pg.setFont(awtFont);
        pg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        pg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        pg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = pg.getFontMetrics();
        int textW = Math.max(1, fm.stringWidth(text));
        int textH = Math.max(1, fm.getAscent() + fm.getDescent());
        pg.dispose();

        int imgW = textW + PAD * 2;
        int imgH = textH + PAD * 2;
        BufferedImage image = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setFont(awtFont);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.drawString(text, PAD, PAD + fm.getAscent());
        g.dispose();

        DynamicTexture texture = new DynamicTexture(image);
        CachedString cached = new CachedString();
        cached.texture = texture;
        cached.texId = texture.getGlTextureId();
        cached.displayWidth = Math.max(1, Math.round(textW / (float) SCALE));
        cached.displayHeight = Math.max(1, Math.round(textH / (float) SCALE));
        return cached;
    }

    private static final class CachedString {
        private DynamicTexture texture;
        private int texId;
        private int displayWidth;
        private int displayHeight;

        private void dispose() {
            if (texture != null) {
                try {
                    texture.deleteGlTexture();
                } catch (Exception ignored) {
                    /* ignore */
                }
                texture = null;
            }
        }
    }
}
