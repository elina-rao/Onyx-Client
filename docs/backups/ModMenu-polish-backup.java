package com.onyxclient.gui;

import com.onyxclient.OnyxClient;
import com.onyxclient.core.config.ConfigManager;
import com.onyxclient.mixins.MixinFontRenderer;
import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.modules.settings.Setting;
import com.onyxclient.utils.AnimationUtils;
import com.onyxclient.utils.ClientUiScale;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.OnyxFont;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Compact BLC-style client HUD — Mods / Settings / Profiles, Onyx look & colors.
 */
public class ModMenu extends GuiScreen {

    private static final ResourceLocation LOGO = new ResourceLocation("onyxclient", "textures/gui/logo.png");

    private static final int PANEL_RADIUS = 8;
    private static final int HEADER_H = 26;
    private static final int TAB_H = 22;
    private static final int SIDEBAR_W = 88;
    private static final int POPUP_W = 300;
    private static final int POPUP_HEADER_H = 38;
    private static final int POPUP_BODY_H = 160;
    private static final int POPUP_FOOTER_PAD = 8;
    private static final int CARD_RADIUS = 4;
    private static final int NAV_ITEM_H = 16;
    private static final int NAV_GAP = 2;
    private static final int FILTER_HEADER_H = 14;
    private static final int GRID_PAD = 6;
    private static final int GRID_GAP = 4;
    /**
     * Badlion-like floating panel; sized for Outfit at near-vanilla density.
     * Size only — Onyx colors/chrome stay. ~62% × 56.1% of scaled GUI.
     */
    private static final float BLC_PANEL_W = 0.62F;
    private static final float BLC_PANEL_H = 0.561F;
    /** Visible module rows in the Mods grid viewport (old pixel menu showed ~3). */
    private static final float BLC_VISIBLE_ROWS = 3.0F;
    /** Prefer ~3 columns like the old mod menu. */
    private static final int TILE_W = 56;
    private static final int SCROLLBAR_W = 4;
    private static final int CLIP_INSET = 2;
    private static final int TOGGLE_W = 20;
    private static final int TOGGLE_H = 9;
    private static final int ROW_H = 18;
    private static final int EDIT_HUD_FOOTER = 28;
    private static final int TAB_PAD_X = 8;
    private static final int TAB_GAP = 6;
    private static final int TAB_BTN_H = 15;
    /** Space between Profiles and Search — enough to breathe, not a large void. */
    private static final int TAB_SEARCH_GAP = 16;
    private static final int SEARCH_W = 110;
    private static final int SEARCH_H = 13;
    private static final int SEARCH_RIGHT_PAD = 10;
    /** Left gutter on mod cards reserved for the favorite star. */
    private static final int CARD_FAV_GUTTER = 10;

    private enum TopTab {
        MODS("Mods"),
        SETTINGS("Settings"),
        PROFILES("Profiles");
        final String label;
        TopTab(String label) { this.label = label; }
    }

    private enum SettingsPage {
        GENERAL("General"),
        GRAPHICS("Graphics"),
        PERFORMANCE("Performance"),
        CONTROLS("Controls"),
        MENUS("Menus"),
        COSMETICS("Cosmetics"),
        RANKED("Ranked");
        final String label;
        SettingsPage(String label) { this.label = label; }
    }

    private enum FilterBucket {
        ALL("All"),
        FAVS("Favs"),
        VISUAL("Visual"),
        HUD("HUD"),
        BEDWARS("Bedwars"),
        COMBAT("Combat");

        final String label;

        FilterBucket(String label) {
            this.label = label;
        }

        boolean matches(ModuleCategory category) {
            if (this == ALL || this == FAVS) {
                return true;
            }
            switch (this) {
                case VISUAL:
                    return category == ModuleCategory.VISUAL
                            || category == ModuleCategory.RENDERING
                            || category == ModuleCategory.CUSTOMIZATION
                            || category == ModuleCategory.PERFORMANCE
                            || category == ModuleCategory.OPTIFINE;
                case HUD:
                    return category == ModuleCategory.HUD
                            || category == ModuleCategory.STATS;
                case BEDWARS:
                    return category == ModuleCategory.BEDWARS;
                case COMBAT:
                    return category == ModuleCategory.COMBAT
                            || category == ModuleCategory.MOVEMENT
                            || category == ModuleCategory.UTILITY;
                default:
                    return false;
            }
        }
    }

    private enum Focus {
        NONE, SEARCH, PROFILE_NAME, API_KEY, API_ENDPOINT
    }

    private enum ScrollDrag {
        NONE, MODS, SETTINGS_LIST, SETTINGS_NAV, MOD_POPUP
    }

    private TopTab topTab = TopTab.MODS;
    private SettingsPage settingsPage = SettingsPage.GENERAL;
    private FilterBucket filterBucket = FilterBucket.ALL;
    private Module selectedModule;
    private Module hoveredModule;
    private int moduleSettingsScroll;
    private String searchQuery = "";
    private String profileName = "";
    private String apiEditBuffer = "";
    private Focus focus = Focus.NONE;
    private int scrollOffset;
    private int settingsScroll;
    private int settingsNavScroll;
    private NumberSetting draggingSlider;
    private ColorSetting draggingColor;
    private int draggingColorChannel; // 0=R 1=G 2=B 3=A
    private ScrollDrag scrollDrag = ScrollDrag.NONE;
    private int scrollDragTrackY;
    private int scrollDragTrackH;
    private int scrollDragMax;
    private int popupDragX;
    private int popupDragY;
    private boolean draggingPopup;
    private int popupDragStartMouseX;
    private int popupDragStartMouseY;
    private int popupDragBaseX;
    private int popupDragBaseY;
    private boolean popupScrollDragging;
    private int popupScrollStartMouseY;
    private int popupScrollStartOffset;
    /** Tile height for current frame — derived so Mods grid shows {@link #BLC_VISIBLE_ROWS}. */
    private int layoutTileH = 48;
    private String statusMessage = "";
    private int statusTicks;

    public ModMenu() {
        AnimationUtils.startMenuOpen();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Never let a leaked scissor from a prior frame clip the whole client UI.
        RenderUtils.clearScissor();

        float progress = AnimationUtils.getMenuOpenProgress();
        int alpha = AnimationUtils.getScaledAlpha(220, progress);
        ConfigManager.ClientConfig cfg = OnyxClient.getConfigManager().getConfig();

        int[] layoutMouse = ClientUiScale.toLayoutMouse(mouseX, mouseY, width, height);
        int lx = layoutMouse[0];
        int ly = layoutMouse[1];

        int dim = cfg.menuBackgroundBlur ? 120 : 60;
        RenderUtils.drawRect(0, 0, width, height, Colors.withAlpha(0x000000, (int) (dim * progress)));
        if (cfg.menuBackgroundBlur) {
            RenderUtils.drawGradientRect(0, 0, width, height / 2,
                    Colors.withAlpha(0x1A0A2E, (int) (40 * progress)),
                    Colors.withAlpha(0x000000, 0));
        }

        int[] panel = panelBounds();
        int panelX = panel[0];
        int panelY = panel[1];
        int panelW = panel[2];
        int panelH = panel[3];

        if (selectedModule != null && topTab != TopTab.MODS) {
            selectedModule = null;
        }

        updateDraggingSlider(panelX, panelY, panelW, panelH, lx);
        updateScrollDrag(lx, ly);

        GlStateManager.pushMatrix();
        try {
            ClientUiScale.applyCenteredScale(width, height);
            if (cfg.menuAnimations) {
                applyOpenAnimScale(panelX, panelY, panelW, panelH, progress);
            }

            RenderUtils.drawSoftShadow(panelX, panelY, panelW, panelH, PANEL_RADIUS, 3);
            RenderUtils.drawRoundedRect(panelX, panelY, panelW, panelH, PANEL_RADIUS,
                    Colors.withAlpha(Colors.BG_DEEP, alpha));
            RenderUtils.drawRoundedOutline(panelX, panelY, panelW, panelH, PANEL_RADIUS, 1.0F,
                    Colors.withAlpha(Colors.BORDER, 160));

            drawHeader(panelX, panelY, panelW, lx, ly);
            drawTabs(panelX, panelY + HEADER_H, panelW, lx, ly);

            int contentY = panelY + HEADER_H + TAB_H;
            int contentH = panelH - HEADER_H - TAB_H;
            int clipH = Math.max(0, contentH - PANEL_RADIUS);
            layoutTileH = tileHeightForClip(clipH);

            if (topTab == TopTab.MODS) {
                drawModsTab(panelX, contentY, panelW, clipH, lx, ly);
            } else if (topTab == TopTab.SETTINGS) {
                drawSettingsTab(panelX, contentY, panelW, clipH, lx, ly);
            } else {
                drawProfilesTab(panelX, contentY, panelW, clipH, lx, ly);
            }

            if (selectedModule != null && topTab == TopTab.MODS) {
                drawModuleSettingsPopup(panelX, panelY, panelW, panelH, lx, ly);
            }

            if (hoveredModule != null && selectedModule == null && topTab == TopTab.MODS) {
                drawModTooltip(lx, ly, hoveredModule);
            }

            if (statusTicks > 0 && !statusMessage.isEmpty()) {
                OnyxFont.UI.drawString(statusMessage, panelX + 10, panelY + panelH - 12, Colors.ACCENT_BRIGHT);
            }
        } finally {
            RenderUtils.clearScissor();
            GlStateManager.popMatrix();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        RenderUtils.clearScissor();
        super.onGuiClosed();
    }

    @Override
    public void updateScreen() {
        if (statusTicks > 0) {
            statusTicks--;
        }
    }

    private int[] panelBounds() {
        // 1:1 Badlion floating-menu footprint (size only)
        int panelW = Math.round(width * BLC_PANEL_W);
        int panelH = Math.round(height * BLC_PANEL_H);
        panelW = Math.max(280, Math.min(panelW, width - 8));
        panelH = Math.max(200, Math.min(panelH, height - 8));
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;
        return new int[]{panelX, panelY, panelW, panelH};
    }

    /** Tile height so the Mods viewport shows ~3 rows inside the BLC-sized panel. */
    private int tileHeightForClip(int clipH) {
        int view = Math.max(1, clipH - GRID_PAD * 2);
        int h = Math.round((view - 2.0F * GRID_GAP) / BLC_VISIBLE_ROWS);
        return Math.max(32, Math.min(56, h));
    }

    private void applyOpenAnimScale(int x, int y, int w, int h, float progress) {
        float scale = 0.97F + 0.03F * progress;
        GlStateManager.translate(x + w / 2.0F, y + h / 2.0F, 0);
        GlStateManager.scale(scale, scale, 1.0F);
        GlStateManager.translate(-(x + w / 2.0F), -(y + h / 2.0F), 0);
    }

    /**
     * GL scissor is unreliable on Mac Retina / OptiFine — soft-clip in draw paths instead.
     * Kept as a no-op so call sites stay readable if we restore scissor later.
     */
    private void enableLayoutScissor(int x, int y, int w, int h) {
        // no-op
    }

    private void drawHeader(int x, int y, int w, int mouseX, int mouseY) {
        drawLogo(x + 8, y + 4, 22);
        int textX = x + 34;
        OnyxFont.MEDIUM.drawString("ONYX", textX, y + 4, Colors.TEXT_PRIMARY);
        OnyxFont.MEDIUM.drawString("CLIENT", textX + OnyxFont.MEDIUM.getStringWidth("ONYX") + 3, y + 4, Colors.ACCENT_BRIGHT);

        int chipX = textX + OnyxFont.MEDIUM.getStringWidth("ONYX CLIENT") + 10;
        int chipW = OnyxFont.UI.getStringWidth("RANKED") + 8;
        RenderUtils.drawRoundedRect(chipX, y + 5, chipW, 11, 3, Colors.withAlpha(Colors.ACCENT_PRIMARY, 180));
        OnyxFont.UI.drawString("RANKED", chipX + 4, y + 7, Colors.TEXT_PRIMARY);

        int closeCX = x + w - 16;
        int closeCY = y + 16;
        boolean closeHover = mouseX >= closeCX - 8 && mouseX <= closeCX + 8
                && mouseY >= closeCY - 8 && mouseY <= closeCY + 8;
        RenderUtils.drawCircle(closeCX, closeCY, 7, closeHover ? Colors.withAlpha(Colors.DANGER, 60) : Colors.BG_CARD);
        OnyxFont.UI.drawString("\u00D7", closeCX - 2, closeCY - 3, closeHover ? Colors.DANGER : Colors.TEXT_MUTED);
    }

    private void drawLogo(int x, int y, int size) {
        try {
            mc.getTextureManager().bindTexture(LOGO);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
            GlStateManager.disableBlend();
        } catch (Exception e) {
            RenderUtils.drawRoundedRect(x, y, size, size, 3, Colors.ACCENT_PRIMARY);
        }
    }

    private void drawTabs(int x, int y, int w, int mouseX, int mouseY) {
        TabRowLayout layout = layoutTabRow(x, y, w);
        TopTab[] tabs = TopTab.values();
        for (int i = 0; i < tabs.length; i++) {
            TopTab tab = tabs[i];
            int tabX = layout.tabX[i];
            int tabW = layout.tabW[i];
            boolean active = tab == topTab;
            boolean hover = mouseX >= tabX && mouseX <= tabX + tabW
                    && mouseY >= layout.tabY && mouseY <= layout.tabY + TAB_BTN_H;
            int fill = active ? Colors.ACCENT_PRIMARY : (hover ? Colors.BG_HOVER : Colors.BG_CARD);
            RenderUtils.drawRoundedRect(tabX, layout.tabY, tabW, TAB_BTN_H, 3, fill);
            int tw = OnyxFont.UI.getStringWidth(tab.label);
            OnyxFont.UI.drawString(tab.label, tabX + (tabW - tw) / 2, layout.tabY + 4,
                    active ? Colors.TEXT_PRIMARY : Colors.TEXT_MUTED);
        }

        boolean searchFocused = focus == Focus.SEARCH;
        RenderUtils.drawRoundedRect(layout.searchX, layout.searchY, layout.searchW, layout.searchH, 3,
                searchFocused ? Colors.BG_HOVER : Colors.BG_CARD);
        RenderUtils.drawRoundedOutline(layout.searchX, layout.searchY, layout.searchW, layout.searchH, 3, 1.0F,
                searchFocused ? Colors.ACCENT_PRIMARY : Colors.BORDER_SOFT);
        String text = searchQuery.isEmpty() ? "Search..." : searchQuery;
        OnyxFont.UI.drawString(trimToWidth(text, layout.searchW - 8), layout.searchX + 4, layout.searchY + 3,
                searchQuery.isEmpty() ? Colors.TEXT_DISABLED : Colors.TEXT_PRIMARY);

        RenderUtils.drawHorizontalLine(x + 8, y + TAB_H - 1, w - 16, Colors.DIVIDER);
    }

    /**
     * Shared Mods / Settings / Profiles + Search geometry (draw + click).
     * Search sits just after Profiles with a modest gap (not jammed, not a wide void).
     */
    private TabRowLayout layoutTabRow(int panelX, int tabRowY, int panelW) {
        TabRowLayout layout = new TabRowLayout();
        TopTab[] tabs = TopTab.values();
        layout.tabX = new int[tabs.length];
        layout.tabW = new int[tabs.length];
        layout.tabY = tabRowY + (TAB_H - TAB_BTN_H) / 2;
        layout.searchH = SEARCH_H;
        layout.searchY = tabRowY + (TAB_H - SEARCH_H) / 2;
        layout.searchW = SEARCH_W;

        int cursor = panelX + 10;
        for (int i = 0; i < tabs.length; i++) {
            int tw = OnyxFont.UI.getStringWidth(tabs[i].label) + TAB_PAD_X * 2;
            layout.tabX[i] = cursor;
            layout.tabW[i] = tw;
            cursor += tw + TAB_GAP;
        }

        int lastRight = layout.tabX[tabs.length - 1] + layout.tabW[tabs.length - 1];
        int rightEdge = panelX + panelW - SEARCH_RIGHT_PAD;
        // Follow tabs with a modest gap (avoids the large empty stretch of far-right search)
        layout.searchX = lastRight + TAB_SEARCH_GAP;
        if (layout.searchX + layout.searchW > rightEdge) {
            layout.searchX = Math.max(lastRight + TAB_SEARCH_GAP, rightEdge - layout.searchW);
            layout.searchW = Math.max(64, rightEdge - layout.searchX);
        }
        return layout;
    }

    private static final class TabRowLayout {
        int[] tabX;
        int[] tabW;
        int tabY;
        int searchX;
        int searchY;
        int searchW;
        int searchH;
    }

    // ——— Mods ———

    private void drawModsTab(int panelX, int contentY, int panelW, int clipH, int mouseX, int mouseY) {
        int gridX = panelX + SIDEBAR_W;
        int gridW = panelW - SIDEBAR_W;

        clampModsScroll(gridW, clipH);
        hoveredModule = null;

        drawModFilters(panelX, contentY, SIDEBAR_W, clipH, mouseX, mouseY);

        RenderUtils.drawVerticalLine(panelX + SIDEBAR_W, contentY + 6, clipH - 12, Colors.DIVIDER);

        drawModuleGrid(gridX, contentY, gridW, clipH, mouseX, mouseY);

        drawScrollbar(gridX, contentY, gridW, clipH, maxModsScroll(gridW, clipH), scrollOffset, ScrollDrag.MODS);
    }

    private void drawModFilters(int x, int y, int w, int h, int mouseX, int mouseY) {
        RenderUtils.drawRect(x + 1, y, w - 1, h, Colors.withAlpha(Colors.BG_SIDEBAR, 160));
        OnyxFont.UI.drawString("Filter", x + 10, y + 3, Colors.TEXT_MUTED);

        int listTop = y + FILTER_HEADER_H;
        int listH = Math.max(0, h - FILTER_HEADER_H);
        int padX = 6;
        int itemW = w - padX * 2;
        int itemY = listTop;
        int textPadY = Math.max(0, (NAV_ITEM_H - OnyxFont.UI.getHeight()) / 2);

        for (FilterBucket bucket : FilterBucket.values()) {
            if (itemY + NAV_ITEM_H > listTop + listH) {
                break;
            }
            drawFilterItem(x, padX, itemW, itemY, bucket, filterBucket == bucket,
                    mouseX, mouseY, listTop, listH, textPadY);
            itemY += NAV_ITEM_H + NAV_GAP;
        }
    }

    private void drawFilterItem(int x, int padX, int itemW, int itemY, FilterBucket bucket, boolean selected,
                                int mouseX, int mouseY, int clipY, int clipH, int textPadY) {
        boolean hovered = mouseX >= x + padX && mouseX <= x + padX + itemW
                && mouseY >= itemY && mouseY <= itemY + NAV_ITEM_H
                && mouseY >= clipY && mouseY <= clipY + clipH;
        if (selected) {
            RenderUtils.drawRoundedRect(x + padX, itemY, itemW, NAV_ITEM_H, 3, Colors.BG_SELECTED);
            RenderUtils.drawRoundedRect(x + padX, itemY + 3, 2, NAV_ITEM_H - 6, 1, Colors.ACCENT_PRIMARY);
        } else if (hovered) {
            RenderUtils.drawRoundedRect(x + padX, itemY, itemW, NAV_ITEM_H, 3, Colors.BG_HOVER);
        }
        int color = selected ? Colors.TEXT_PRIMARY : (hovered ? Colors.TEXT_SECONDARY : Colors.TEXT_MUTED);
        int textX = x + padX + 8;
        int textY = itemY + textPadY;
        if (bucket == FilterBucket.FAVS) {
            int starW = drawVanillaStar(textX, textY, color, true);
            OnyxFont.UI.drawString("Favs", textX + starW + 2, textY, color);
        } else {
            String shown = trimToWidth(bucket.label, Math.max(8, itemW - 12));
            OnyxFont.UI.drawString(shown, textX, textY, color);
        }
    }

    /** ★/☆ via vanilla atlas — Outfit has no star glyphs; bypasses Custom Font mixin. */
    private int drawVanillaStar(final float x, final float y, final int color, boolean filled) {
        final String glyph = filled ? "\u2605" : "\u2606";
        final int[] width = new int[]{8};
        MixinFontRenderer.withVanillaGlyphs(new Runnable() {
            @Override
            public void run() {
                width[0] = mc.fontRendererObj.getStringWidth(glyph);
                mc.fontRendererObj.drawString(glyph, (int) x, (int) y, color, false);
            }
        });
        return width[0];
    }

    private int columnCount(int innerW) {
        int cols = Math.max(1, (innerW + GRID_GAP) / (TILE_W + GRID_GAP));
        return Math.min(4, cols);
    }

    private int cardWidth(int innerW, int cols) {
        return (innerW - GRID_GAP * (cols - 1)) / cols;
    }

    private int cardHeight() {
        return layoutTileH;
    }

    private int gridInnerW(int gridW) {
        return Math.max(0, gridW - GRID_PAD * 2 - SCROLLBAR_W - 6);
    }

    private int contentHeight(int count, int cols) {
        if (count <= 0 || cols <= 0) {
            return 0;
        }
        int rows = (count + cols - 1) / cols;
        return rows * layoutTileH + Math.max(0, rows - 1) * GRID_GAP;
    }

    private int maxModsScroll(int gridW, int clipH) {
        List<Module> modules = modulesVisible();
        int innerW = gridInnerW(gridW);
        int cols = columnCount(innerW);
        int total = contentHeight(modules.size(), cols);
        int view = Math.max(0, clipH - GRID_PAD * 2);
        return Math.max(0, total - view);
    }

    private void clampModsScroll(int gridW, int clipH) {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxModsScroll(gridW, clipH)));
    }

    private void drawModuleGrid(int x, int y, int w, int h, int mouseX, int mouseY) {
        List<Module> modules = modulesVisible();
        int innerW = gridInnerW(w);
        int cols = columnCount(innerW);
        int cardW = cardWidth(innerW, cols);
        int cardH = cardHeight();
        boolean inClip = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        List<String> favorites = favoriteNames();

        int col = 0;
        int row = 0;
        for (Module module : modules) {
            int cardX = x + GRID_PAD + col * (cardW + GRID_GAP);
            int cardY = y + GRID_PAD + row * (cardH + GRID_GAP) - scrollOffset;

            if (cardY + cardH > y && cardY < y + h) {
                boolean hovered = inClip && mouseX >= cardX && mouseX <= cardX + cardW
                        && mouseY >= Math.max(cardY, y) && mouseY <= Math.min(cardY + cardH, y + h);
                boolean selected = module == selectedModule;
                boolean enabled = module.isEnabled();
                boolean fav = favorites.contains(module.getName());

                if (hovered) {
                    hoveredModule = module;
                }

                int fill = selected ? Colors.BG_SELECTED : (hovered ? Colors.BG_HOVER : Colors.BG_CARD);
                RenderUtils.drawRoundedRect(cardX, cardY, cardW, cardH, CARD_RADIUS, fill);
                if (enabled) {
                    RenderUtils.drawRoundedOutline(cardX, cardY, cardW, cardH, CARD_RADIUS, 1.0F,
                            Colors.withAlpha(Colors.ACCENT_PRIMARY, 140));
                } else {
                    RenderUtils.drawRoundedOutline(cardX, cardY, cardW, cardH, CARD_RADIUS, 1.0F, Colors.BORDER_SOFT);
                }

                if (fav) {
                    drawVanillaStar(cardX + 2, cardY + 1, Colors.ACCENT_BRIGHT, true);
                } else if (hovered) {
                    drawVanillaStar(cardX + 2, cardY + 1, Colors.TEXT_MUTED, false);
                }

                drawCardTitle(module.getName(), cardX, cardY + 1, cardW);

                int markCX = cardX + cardW / 2;
                int markCY = cardY + OnyxFont.UI.getHeight() + 3;
                int markR = Math.min(5, Math.max(3, cardW / 10));
                RenderUtils.drawCircle(markCX, markCY, markR,
                        enabled ? Colors.withAlpha(Colors.ACCENT_PRIMARY, 100) : Colors.withAlpha(Colors.BG_HOVER, 220));
                RenderUtils.drawCircle(markCX, markCY, Math.max(2, markR / 3),
                        enabled ? Colors.ACCENT_BRIGHT : Colors.TEXT_DISABLED);

                drawToggle(cardX + (cardW - TOGGLE_W) / 2, cardY + cardH - TOGGLE_H - 3, enabled);
            }

            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }
    }

    private void drawModTooltip(int mouseX, int mouseY, Module module) {
        String desc = module.getDescription();
        if (desc == null || desc.isEmpty()) {
            return;
        }
        int pad = 4;
        int tw = Math.min(OnyxFont.UI.getStringWidth(desc), 160);
        String line = trimToWidth(desc, 160);
        int boxW = tw + pad * 2;
        int boxH = 12 + pad * 2;
        int bx = mouseX + 10;
        int by = mouseY + 10;
        RenderUtils.drawRoundedRect(bx, by, boxW, boxH, 3, Colors.withAlpha(Colors.BG_DEEP, 240));
        RenderUtils.drawRoundedOutline(bx, by, boxW, boxH, 3, 1.0F, Colors.BORDER);
        OnyxFont.UI.drawString(line, bx + pad, by + pad + 1, Colors.TEXT_SECONDARY);
    }

    private int[] modulePopupBounds(int panelX, int panelY, int panelW, int panelH) {
        int w = Math.min(POPUP_W, panelW - 24);
        int h = POPUP_HEADER_H + POPUP_BODY_H + POPUP_FOOTER_PAD;
        h = Math.min(h, panelH - 20);
        int baseX = panelX + (panelW - w) / 2;
        int baseY = panelY + (panelH - h) / 2;
        clampPopupDrag(panelX, panelY, panelW, panelH, w, h);
        int x = baseX + popupDragX;
        int y = baseY + popupDragY;
        return new int[]{x, y, w, h};
    }

    private int popupBodyHeight(int popupH) {
        return Math.max(0, popupH - POPUP_HEADER_H - POPUP_FOOTER_PAD);
    }

    private int popupMaxScroll(int popupH) {
        return Math.max(0, moduleSettingsContentHeight() - popupBodyHeight(popupH));
    }

    private void clampPopupDrag(int panelX, int panelY, int panelW, int panelH, int popupW, int popupH) {
        int baseX = panelX + (panelW - popupW) / 2;
        int baseY = panelY + (panelH - popupH) / 2;
        int minDx = panelX + 4 - baseX;
        int maxDx = panelX + panelW - 4 - popupW - baseX;
        int minDy = panelY + 4 - baseY;
        int maxDy = panelY + panelH - 4 - popupH - baseY;
        popupDragX = Math.max(minDx, Math.min(maxDx, popupDragX));
        popupDragY = Math.max(minDy, Math.min(maxDy, popupDragY));
    }

    private void resetPopupDragState() {
        popupDragX = 0;
        popupDragY = 0;
        draggingPopup = false;
        popupScrollDragging = false;
    }

    private int moduleSettingsContentHeight() {
        if (selectedModule == null) {
            return 20;
        }
        List<Setting<?>> settings = selectedModule.getSettings();
        if (settings.isEmpty()) {
            return 20;
        }
        int h = 0;
        for (Setting<?> setting : settings) {
            h += 18;
            if (setting instanceof NumberSetting) {
                h += 10;
            } else if (setting instanceof ColorSetting) {
                h += 36; // R/G/B/A mini sliders
            }
        }
        return h;
    }

    private void drawModuleSettingsPopup(int panelX, int panelY, int panelW, int panelH, int mouseX, int mouseY) {
        RenderUtils.drawRect(panelX, panelY, panelW, panelH, Colors.withAlpha(0x000000, 140));

        int[] b = modulePopupBounds(panelX, panelY, panelW, panelH);
        int x = b[0];
        int y = b[1];
        int w = b[2];
        int h = b[3];

        RenderUtils.drawSoftShadow(x, y, w, h, 6, 3);
        RenderUtils.drawRoundedRect(x, y, w, h, 6, Colors.withAlpha(Colors.BG_CARD, 250));
        RenderUtils.drawRoundedOutline(x, y, w, h, 6, 1.0F, Colors.BORDER);

        OnyxFont.UI.drawString(trimToWidth(selectedModule.getName(), w - 70), x + 10, y + 8, Colors.TEXT_PRIMARY);
        OnyxFont.UI.drawString(trimToWidth(selectedModule.getDescription(), w - 20), x + 10, y + 18, Colors.TEXT_MUTED);

        drawToggle(x + w - TOGGLE_W - 28, y + 10, selectedModule.isEnabled());
        OnyxFont.UI.drawString("X", x + w - 14, y + 8, Colors.TEXT_MUTED);
        RenderUtils.drawHorizontalLine(x + 8, y + 32, w - 16, Colors.DIVIDER);

        int bodyY = y + POPUP_HEADER_H;
        int bodyH = popupBodyHeight(h);
        int maxScroll = popupMaxScroll(h);
        moduleSettingsScroll = Math.max(0, Math.min(moduleSettingsScroll, maxScroll));

        List<Setting<?>> settings = selectedModule.getSettings();
        if (settings.isEmpty()) {
            OnyxFont.UI.drawString("No settings for this mod.", x + 12, bodyY + 8, Colors.TEXT_MUTED);
        } else {
            int settingY = bodyY - moduleSettingsScroll;
            for (Setting<?> setting : settings) {
                if (settingY + 18 >= bodyY && settingY <= bodyY + bodyH) {
                    OnyxFont.UI.drawString(setting.getName(), x + 12, settingY, Colors.TEXT_SECONDARY);
                    if (setting instanceof BooleanSetting) {
                        drawToggle(x + w - TOGGLE_W - 14, settingY - 1, ((BooleanSetting) setting).getValue());
                    } else if (setting instanceof NumberSetting) {
                        NumberSetting num = (NumberSetting) setting;
                        String val = formatNumber(num);
                        int vw = OnyxFont.UI.getStringWidth(val);
                        OnyxFont.UI.drawString(val, x + w - vw - 14, settingY, Colors.ACCENT_BRIGHT);
                        drawSlider(x + 12, settingY + 10, w - 28, num);
                        settingY += 10;
                    } else if (setting instanceof ColorSetting) {
                        ColorSetting color = (ColorSetting) setting;
                        drawColorSwatch(x + w - 22, settingY - 1, 12, 12, color.getValue());
                        drawColorChannelSliders(x + 12, settingY + 12, w - 28, color);
                        settingY += 36;
                    } else if (setting instanceof ModeSetting) {
                        String val = ((ModeSetting) setting).getValue();
                        int vw = OnyxFont.UI.getStringWidth(val);
                        RenderUtils.drawRoundedRect(x + w - vw - 18, settingY - 1, vw + 8, 11, 3, Colors.BG_HOVER);
                        OnyxFont.UI.drawString(val, x + w - vw - 14, settingY, Colors.ACCENT_BRIGHT);
                    }
                } else if (setting instanceof NumberSetting) {
                    settingY += 10;
                } else if (setting instanceof ColorSetting) {
                    settingY += 36;
                }
                settingY += 18;
            }
        }

        if (maxScroll > 0) {
            drawScrollbar(x, bodyY - GRID_PAD, w, bodyH + GRID_PAD * 2, maxScroll, moduleSettingsScroll, ScrollDrag.MOD_POPUP);
        }
    }

    private List<String> favoriteNames() {
        List<String> fav = OnyxClient.getConfigManager().getConfig().favoriteModules;
        if (fav == null) {
            fav = new ArrayList<String>();
            OnyxClient.getConfigManager().getConfig().favoriteModules = fav;
        }
        return fav;
    }

    private void toggleFavorite(Module module) {
        List<String> fav = favoriteNames();
        if (fav.contains(module.getName())) {
            fav.remove(module.getName());
            flash("Unfavorited " + module.getName());
        } else {
            fav.add(module.getName());
            flash("Favorited " + module.getName());
        }
        OnyxClient.getConfigManager().save();
    }

    // ——— Settings ———

    private void drawSettingsTab(int panelX, int contentY, int panelW, int clipH, int mouseX, int mouseY) {
        int navH = clipH - EDIT_HUD_FOOTER;
        drawSettingsSidebar(panelX, contentY, SIDEBAR_W, navH, mouseX, mouseY);

        // Edit HUD footer (pinned)
        int btnY = contentY + clipH - 24;
        int btnW = SIDEBAR_W - 12;
        boolean btnHover = mouseX >= panelX + 6 && mouseX <= panelX + 6 + btnW
                && mouseY >= btnY && mouseY <= btnY + 18;
        RenderUtils.drawRoundedRect(panelX + 6, btnY, btnW, 18, 4, btnHover ? Colors.BG_HOVER : Colors.BG_CARD);
        RenderUtils.drawRoundedOutline(panelX + 6, btnY, btnW, 18, 4, 1.0F, Colors.BORDER_SOFT);
        int tw = OnyxFont.UI.getStringWidth("Edit HUD");
        OnyxFont.UI.drawString("Edit HUD", panelX + 6 + (btnW - tw) / 2, btnY + 5, Colors.TEXT_PRIMARY);

        RenderUtils.drawVerticalLine(panelX + SIDEBAR_W, contentY + 6, clipH - 12, Colors.DIVIDER);

        int listX = panelX + SIDEBAR_W;
        int listW = panelW - SIDEBAR_W;
        drawSettingsList(listX, contentY, listW, clipH, mouseX, mouseY);
    }

    private void drawSettingsSidebar(int x, int y, int w, int h, int mouseX, int mouseY) {
        RenderUtils.drawRect(x + 1, y, w - 1, h, Colors.withAlpha(Colors.BG_SIDEBAR, 160));
        OnyxFont.UI.drawString("Overview", x + 10, y + 3 - settingsNavScroll, Colors.TEXT_MUTED);

        SettingsPage[] pages = SettingsPage.values();
        int itemY = y + FILTER_HEADER_H - settingsNavScroll;
        int padX = 6;
        int itemW = w - padX * 2;
        int textPadY = Math.max(0, (NAV_ITEM_H - OnyxFont.UI.getHeight()) / 2);
        for (SettingsPage page : pages) {
            if (itemY + NAV_ITEM_H >= y && itemY <= y + h) {
                boolean selected = page == settingsPage;
                boolean hovered = mouseX >= x + padX && mouseX <= x + padX + itemW
                        && mouseY >= itemY && mouseY <= itemY + NAV_ITEM_H
                        && mouseY >= y && mouseY <= y + h;
                if (selected) {
                    RenderUtils.drawRoundedRect(x + padX, itemY, itemW, NAV_ITEM_H, 3,
                            Colors.withAlpha(Colors.ACCENT_PRIMARY, 200));
                } else if (hovered) {
                    RenderUtils.drawRoundedRect(x + padX, itemY, itemW, NAV_ITEM_H, 3, Colors.BG_HOVER);
                }
                OnyxFont.UI.drawString(page.label, x + padX + 8, itemY + textPadY,
                        selected ? Colors.TEXT_PRIMARY : Colors.TEXT_MUTED);
            }
            itemY += NAV_ITEM_H + NAV_GAP;
        }

        int totalNav = FILTER_HEADER_H + pages.length * (NAV_ITEM_H + NAV_GAP);
        int maxNav = Math.max(0, totalNav - h);
        settingsNavScroll = Math.max(0, Math.min(settingsNavScroll, maxNav));
    }

    private List<SettingRow> settingsRows() {
        List<SettingRow> rows = new ArrayList<SettingRow>();
        ConfigManager.ClientConfig cfg = OnyxClient.getConfigManager().getConfig();
        String q = searchQuery.toLowerCase();

        switch (settingsPage) {
            case GENERAL:
                rows.add(SettingRow.header("General"));
                rows.add(SettingRow.action("Hypixel API Key", maskKey(cfg.hypixelApiKey), "hypixel"));
                rows.add(SettingRow.action("Onyx API Endpoint", shortUrl(cfg.onyxApiEndpoint), "onyx"));
                rows.add(SettingRow.configToggle("Smart Disconnect", "smartDisconnect"));
                rows.add(SettingRow.configToggle("GUI Debug", "guiDebug"));
                break;
            case GRAPHICS:
                rows.add(SettingRow.header("Graphics"));
                rows.add(SettingRow.configToggle("Crosshair in F5", "crosshairInF5"));
                rows.add(SettingRow.configToggle("Centered Potion Inventory", "centeredPotionInventory"));
                rows.add(SettingRow.configToggle("Dirt Screen", "dirtScreen"));
                rows.add(SettingRow.configToggle("Weather", "weather"));
                rows.add(SettingRow.configToggle("Borderless Fullscreen", "borderlessFullscreen"));
                break;
            case PERFORMANCE:
                rows.add(SettingRow.header("Performance"));
                rows.add(SettingRow.stepper("Performance Preset", "perfPreset",
                        com.onyxclient.core.PerformancePresets.NAMES));
                rows.add(SettingRow.stepper("Render Distance", "renderDistanceOverride",
                        new String[]{"Auto", "2", "4", "6", "8", "12"}));
                rows.add(SettingRow.slider("Focused FPS Cap", "focusedFpsCap", 0, 500));
                rows.add(SettingRow.slider("Unfocused FPS Limit", "unfocusedFpsLimit", 5, 60));
                rows.add(SettingRow.info(com.onyxclient.core.PerformanceApplier.getDiagnosticLine()));
                rows.add(SettingRow.info("Mac: 400-800 SP target. Windows: match Badlion (~3000+)."));
                rows.add(SettingRow.info("Ping is network — presets cut render load only."));
                break;
            case CONTROLS:
                rows.add(SettingRow.header("Controls"));
                rows.add(SettingRow.info("Mod Menu Keybind: R Shift"));
                rows.add(SettingRow.configToggle("Raw Mouse Input", "rawMouseInput"));
                rows.add(SettingRow.configToggle("Disable Scroll Wheel", "disableScrollWheel"));
                break;
            case MENUS:
                rows.add(SettingRow.header("Menus"));
                rows.add(SettingRow.configToggle("Menu Background Blur", "menuBackgroundBlur"));
                rows.add(SettingRow.configToggle("Menu Animations", "menuAnimations"));
                rows.add(SettingRow.stepper("UI Scale", "menuUiScale", new String[]{"Small", "Normal", "Large"}));
                break;
            case COSMETICS:
                rows.add(SettingRow.header("Cosmetics"));
                rows.add(SettingRow.configToggle("Cape Enabled", "capeEnabled"));
                rows.add(SettingRow.action("Skin Changer", "Open", "skins"));
                rows.add(SettingRow.info("Cape / 3D skin modules are in Mods."));
                break;
            case RANKED:
                rows.add(SettingRow.header("Ranked Bedwars"));
                rows.add(SettingRow.configToggle("Ranked Tips", "rankedTips"));
                rows.add(SettingRow.info("Elo / Live Stats modules are in Mods."));
                break;
            default:
                break;
        }

        if (!q.isEmpty()) {
            List<SettingRow> filtered = new ArrayList<SettingRow>();
            for (SettingRow row : rows) {
                if (row.type == SettingRow.Type.HEADER || row.label.toLowerCase().contains(q)) {
                    filtered.add(row);
                }
            }
            return filtered;
        }
        return rows;
    }

    private String maskKey(String key) {
        if (key == null || key.isEmpty()) {
            return "Not set — click";
        }
        if (key.length() <= 4) {
            return "****";
        }
        return "****" + key.substring(key.length() - 4);
    }

    private String shortUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "default";
        }
        return trimToWidth(url, 100);
    }

    private void drawSettingsList(int x, int y, int w, int h, int mouseX, int mouseY) {
        List<SettingRow> rows = settingsRows();
        int total = 0;
        for (SettingRow row : rows) {
            total += ROW_H + (row.type == SettingRow.Type.SLIDER ? 6 : 0);
        }
        int maxScroll = Math.max(0, total - (h - 12));
        settingsScroll = Math.max(0, Math.min(settingsScroll, maxScroll));

        int rowY = y + 10 - settingsScroll;
        for (SettingRow row : rows) {
            int rh = ROW_H + (row.type == SettingRow.Type.SLIDER ? 6 : 0);
            if (rowY + rh >= y && rowY <= y + h) {
                if (row.type == SettingRow.Type.HEADER) {
                    OnyxFont.UI.drawString(row.label, x + 12, rowY + 5, Colors.TEXT_MUTED);
                } else if (row.type == SettingRow.Type.INFO) {
                    OnyxFont.UI.drawString(trimToWidth(row.label, w - 24), x + 12, rowY + 5, Colors.TEXT_SECONDARY);
                } else if (row.type == SettingRow.Type.CONFIG_TOGGLE) {
                    OnyxFont.UI.drawString(row.label, x + 12, rowY + 5, Colors.TEXT_PRIMARY);
                    drawToggle(x + w - TOGGLE_W - 14, rowY + 3, getConfigBool(row.key));
                } else if (row.type == SettingRow.Type.ACTION) {
                    OnyxFont.UI.drawString(row.label, x + 12, rowY + 5, Colors.TEXT_PRIMARY);
                    String val = row.value != null ? row.value : "";
                    int vw = OnyxFont.UI.getStringWidth(val);
                    RenderUtils.drawRoundedRect(x + w - vw - 18, rowY + 2, vw + 10, 12, 3, Colors.BG_HOVER);
                    OnyxFont.UI.drawString(val, x + w - vw - 13, rowY + 4, Colors.ACCENT_BRIGHT);
                } else if (row.type == SettingRow.Type.SLIDER) {
                    OnyxFont.UI.drawString(row.label, x + 12, rowY + 1, Colors.TEXT_PRIMARY);
                    int val = getConfigInt(row.key);
                    double pct = (val - row.min) / (double) (row.max - row.min);
                    int sx = x + 12;
                    int sw = w - 36;
                    int sy = rowY + 13;
                    RenderUtils.drawRoundedRect(sx, sy, sw, 3, 1, Colors.BG_HOVER);
                    RenderUtils.drawRoundedRect(sx, sy, Math.max(2, (int) (sw * pct)), 3, 1, Colors.ACCENT_PRIMARY);
                    RenderUtils.drawCircle(sx + (int) (sw * pct), sy + 1, 3, Colors.TEXT_PRIMARY);
                    OnyxFont.UI.drawString(String.valueOf(val), x + w - 24, rowY + 1, Colors.TEXT_MUTED);
                } else if (row.type == SettingRow.Type.STEPPER) {
                    OnyxFont.UI.drawString(row.label, x + 12, rowY + 5, Colors.TEXT_PRIMARY);
                    int idx = Math.max(0, Math.min(row.options.length - 1, getConfigInt(row.key)));
                    String val = "< " + row.options[idx] + " >";
                    int vw = OnyxFont.UI.getStringWidth(val);
                    OnyxFont.UI.drawString(val, x + w - vw - 14, rowY + 5, Colors.ACCENT_BRIGHT);
                }
            }
            rowY += rh;
        }

        drawScrollbar(x, y, w, h, maxScroll, settingsScroll, ScrollDrag.SETTINGS_LIST);
    }

    // ——— Profiles ———

    private void drawProfilesTab(int panelX, int contentY, int panelW, int clipH, int mouseX, int mouseY) {
        int x = panelX + 14;
        int y = contentY + 10;
        int w = panelW - 28;

        OnyxFont.UI.drawString("Local Profiles", x, y, Colors.TEXT_PRIMARY);
        OnyxFont.UI.drawString("Save / load mods + HUD — Rename uses the name field", x, y + 11, Colors.TEXT_MUTED);

        int fieldY = y + 28;
        boolean nameFocused = focus == Focus.PROFILE_NAME;
        RenderUtils.drawRoundedRect(x, fieldY, 170, 16, 3, nameFocused ? Colors.BG_HOVER : Colors.BG_CARD);
        RenderUtils.drawRoundedOutline(x, fieldY, 170, 16, 3, 1.0F,
                nameFocused ? Colors.ACCENT_PRIMARY : Colors.BORDER_SOFT);
        String draft = profileName.isEmpty() ? "Profile name..." : profileName;
        OnyxFont.UI.drawString(trimToWidth(draft, 158), x + 5, fieldY + 4,
                profileName.isEmpty() ? Colors.TEXT_DISABLED : Colors.TEXT_PRIMARY);

        boolean createHover = mouseX >= x + 178 && mouseX <= x + 240 && mouseY >= fieldY && mouseY <= fieldY + 16;
        RenderUtils.drawRoundedRect(x + 178, fieldY, 62, 16, 3,
                createHover ? Colors.ACCENT_PRIMARY : Colors.BG_HOVER);
        OnyxFont.UI.drawString("Create", x + 192, fieldY + 4, Colors.TEXT_PRIMARY);

        List<String> profiles = OnyxClient.getConfigManager().listProfiles();
        String q = searchQuery.toLowerCase();
        int listY = fieldY + 24;
        for (String name : profiles) {
            if (!q.isEmpty() && !name.toLowerCase().contains(q)) {
                continue;
            }
            if (listY + 20 > contentY + clipH) {
                break;
            }
            RenderUtils.drawRoundedRect(x, listY, w - 6, 18, 3, Colors.BG_CARD);
            OnyxFont.UI.drawString(name, x + 6, listY + 5, Colors.TEXT_PRIMARY);
            OnyxFont.UI.drawString("Load", x + w - 145, listY + 5, Colors.ACCENT_BRIGHT);
            OnyxFont.UI.drawString("Rename", x + w - 105, listY + 5, Colors.TEXT_SECONDARY);
            OnyxFont.UI.drawString("Delete", x + w - 55, listY + 5, Colors.DANGER);
            listY += 22;
        }

        if (profiles.isEmpty()) {
            OnyxFont.UI.drawString("No profiles yet — create one above.", x, listY, Colors.TEXT_MUTED);
        }
    }

    // ——— Widgets ———

    private void drawToggle(int x, int y, boolean enabled) {
        int trackColor = enabled ? Colors.ACCENT_PRIMARY : Colors.withAlpha(Colors.TEXT_DISABLED, 220);
        RenderUtils.drawRoundedRect(x, y, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2, trackColor);
        int knobX = enabled ? x + TOGGLE_W - 7 : x + 7;
        RenderUtils.drawCircle(knobX, y + TOGGLE_H / 2, 3, Colors.TEXT_PRIMARY);
    }

    private void drawSlider(int x, int y, int w, NumberSetting setting) {
        double pct = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        int fillW = Math.max(0, (int) (w * pct));
        RenderUtils.drawRoundedRect(x, y, w, 3, 1, Colors.BG_HOVER);
        if (fillW > 0) {
            RenderUtils.drawRoundedRect(x, y, fillW, 3, 1, Colors.ACCENT_PRIMARY);
        }
        RenderUtils.drawCircle(x + fillW, y + 1, 4, Colors.TEXT_PRIMARY);
    }

    private void drawColorSwatch(int x, int y, int w, int h, int color) {
        RenderUtils.drawRoundedRect(x, y, w, h, 2, color | 0xFF000000);
        RenderUtils.drawRoundedOutline(x, y, w, h, 2, 1.0F, Colors.BORDER);
    }

    private void drawColorChannelSliders(int x, int y, int w, ColorSetting color) {
        drawChannelRow(x, y, w, "R", color.getRed(), 0xFFFF5555);
        drawChannelRow(x, y + 9, w, "G", color.getGreen(), 0xFF55FF55);
        drawChannelRow(x, y + 18, w, "B", color.getBlue(), 0xFF5555FF);
        drawChannelRow(x, y + 27, w, "A", color.getAlpha(), 0xFFAAAAAA);
    }

    private void drawChannelRow(int x, int y, int w, String label, int value, int fill) {
        OnyxFont.UI.drawString(label, x, y, Colors.TEXT_MUTED);
        int trackX = x + 10;
        int trackW = w - 34;
        double pct = value / 255.0;
        int fillW = Math.max(0, (int) (trackW * pct));
        RenderUtils.drawRoundedRect(trackX, y + 2, trackW, 3, 1, Colors.BG_HOVER);
        if (fillW > 0) {
            RenderUtils.drawRoundedRect(trackX, y + 2, fillW, 3, 1, fill);
        }
        RenderUtils.drawCircle(trackX + fillW, y + 3, 3, Colors.TEXT_PRIMARY);
        String num = String.valueOf(value);
        OnyxFont.UI.drawString(num, x + w - OnyxFont.UI.getStringWidth(num), y, Colors.TEXT_SECONDARY);
    }

    private String formatNumber(NumberSetting setting) {
        double v = setting.getValue();
        if (Math.abs(v - Math.rint(v)) < 0.0001) {
            return String.valueOf((int) Math.rint(v));
        }
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    private void drawScrollbar(int gridX, int contentY, int gridW, int clipH, int max, int offset, ScrollDrag kind) {
        if (max <= 0) {
            return;
        }
        int trackX = gridX + gridW - SCROLLBAR_W - 5;
        int trackY = contentY + GRID_PAD;
        int trackH = Math.max(12, clipH - GRID_PAD * 2);
        RenderUtils.drawRoundedRect(trackX, trackY, SCROLLBAR_W, trackH, 1, Colors.BORDER_SOFT);
        int view = Math.max(1, clipH - GRID_PAD * 2);
        int total = view + max;
        int thumbH = Math.max(14, (int) (trackH * (view / (float) total)));
        int thumbY = trackY + (int) ((trackH - thumbH) * (offset / (float) max));
        RenderUtils.drawRoundedRect(trackX, thumbY, SCROLLBAR_W, thumbH, 1, Colors.ACCENT_PRIMARY);
    }

    private int[] scrollbarGeom(int gridX, int contentY, int gridW, int clipH, int max, int offset) {
        int trackX = gridX + gridW - SCROLLBAR_W - 5;
        int trackY = contentY + GRID_PAD;
        int trackH = Math.max(12, clipH - GRID_PAD * 2);
        if (max <= 0) {
            return new int[]{trackX, trackY, SCROLLBAR_W, trackH, trackY, 0};
        }
        int view = Math.max(1, clipH - GRID_PAD * 2);
        int total = view + max;
        int thumbH = Math.max(14, (int) (trackH * (view / (float) total)));
        int thumbY = trackY + (int) ((trackH - thumbH) * (offset / (float) max));
        return new int[]{trackX, trackY, SCROLLBAR_W, trackH, thumbY, thumbH};
    }

    private List<Module> modulesVisible() {
        List<Module> result = new ArrayList<Module>();
        for (Module module : OnyxClient.getModuleManager().getModules()) {
            if (!searchQuery.isEmpty() && !module.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                continue;
            }
            if (filterBucket == FilterBucket.FAVS) {
                if (!favoriteNames().contains(module.getName())) {
                    continue;
                }
            } else if (filterBucket != FilterBucket.ALL && !filterBucket.matches(module.getCategory())) {
                continue;
            }
            result.add(module);
        }
        // Favorites first, then enabled, then A–Z
        final List<String> favorites = favoriteNames();
        Collections.sort(result, new Comparator<Module>() {
            @Override
            public int compare(Module a, Module b) {
                boolean fa = favorites.contains(a.getName());
                boolean fb = favorites.contains(b.getName());
                if (fa != fb) {
                    return fa ? -1 : 1;
                }
                if (a.isEnabled() != b.isEnabled()) {
                    return a.isEnabled() ? -1 : 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return result;
    }

    /**
     * Left-aligned title after fav gutter; scales to fit (never center+scissor-chop).
     * Offsets draw by OnyxFont PAD bleed so the first letter is not clipped.
     */
    private void drawCardTitle(String name, int cardX, int cardY, int cardW) {
        if (name == null || name.isEmpty()) {
            return;
        }
        int padBleed = 2; // OnyxFont PAD/SCALE expands quad left of logical x
        int titleX = cardX + CARD_FAV_GUTTER + padBleed;
        int maxW = Math.max(8, cardW - CARD_FAV_GUTTER - 4 - padBleed);
        int nameW = OnyxFont.UI.getStringWidth(name);
        if (nameW <= maxW) {
            OnyxFont.UI.drawString(name, titleX, cardY, Colors.TEXT_PRIMARY);
            return;
        }
        float scale = maxW / (float) nameW;
        if (scale < 0.55F) {
            String clipped = trimToWidth(name, maxW);
            OnyxFont.UI.drawString(clipped, titleX, cardY, Colors.TEXT_PRIMARY);
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(titleX, cardY, 0);
        GlStateManager.scale(scale, scale, 1.0F);
        OnyxFont.UI.drawString(name, 0, 0, Colors.TEXT_PRIMARY);
        GlStateManager.popMatrix();
    }

    private String trimToWidth(String text, int maxPx) {
        return OnyxFont.UI.trimToWidth(text, maxPx);
    }

    private boolean getConfigBool(String key) {
        ConfigManager.ClientConfig c = OnyxClient.getConfigManager().getConfig();
        switch (key) {
            case "smartDisconnect": return c.smartDisconnect;
            case "guiDebug": return c.guiDebug;
            case "crosshairInF5": return c.crosshairInF5;
            case "centeredPotionInventory": return c.centeredPotionInventory;
            case "dirtScreen": return c.dirtScreen;
            case "weather": return c.weather;
            case "borderlessFullscreen": return c.borderlessFullscreen;
            case "rawMouseInput": return c.rawMouseInput;
            case "disableScrollWheel": return c.disableScrollWheel;
            case "menuBackgroundBlur": return c.menuBackgroundBlur;
            case "menuAnimations": return c.menuAnimations;
            case "capeEnabled": return c.capeEnabled;
            case "rankedTips": return c.rankedTips;
            default: return false;
        }
    }

    private void toggleConfigBool(String key) {
        ConfigManager.ClientConfig c = OnyxClient.getConfigManager().getConfig();
        switch (key) {
            case "smartDisconnect": c.smartDisconnect = !c.smartDisconnect; break;
            case "guiDebug": c.guiDebug = !c.guiDebug; break;
            case "crosshairInF5": c.crosshairInF5 = !c.crosshairInF5; break;
            case "centeredPotionInventory": c.centeredPotionInventory = !c.centeredPotionInventory; break;
            case "dirtScreen": c.dirtScreen = !c.dirtScreen; break;
            case "weather": c.weather = !c.weather; break;
            case "borderlessFullscreen": c.borderlessFullscreen = !c.borderlessFullscreen; break;
            case "rawMouseInput": c.rawMouseInput = !c.rawMouseInput; break;
            case "disableScrollWheel": c.disableScrollWheel = !c.disableScrollWheel; break;
            case "menuBackgroundBlur": c.menuBackgroundBlur = !c.menuBackgroundBlur; break;
            case "menuAnimations": c.menuAnimations = !c.menuAnimations; break;
            case "capeEnabled": c.capeEnabled = !c.capeEnabled; break;
            case "rankedTips": c.rankedTips = !c.rankedTips; break;
            default: return;
        }
        OnyxClient.getConfigManager().save();
        com.onyxclient.core.ClientSettingsHooks.onConfigToggle(key);
    }

    private int getConfigInt(String key) {
        ConfigManager.ClientConfig c = OnyxClient.getConfigManager().getConfig();
        if ("unfocusedFpsLimit".equals(key)) return c.unfocusedFpsLimit;
        if ("menuUiScale".equals(key)) return c.menuUiScale;
        if ("perfPreset".equals(key)) return c.perfPreset;
        if ("renderDistanceOverride".equals(key)) return c.renderDistanceOverride;
        if ("focusedFpsCap".equals(key)) return c.focusedFpsCap;
        return 0;
    }

    private void setConfigInt(String key, int value) {
        ConfigManager.ClientConfig c = OnyxClient.getConfigManager().getConfig();
        if ("unfocusedFpsLimit".equals(key)) {
            c.unfocusedFpsLimit = value;
        } else if ("menuUiScale".equals(key)) {
            c.menuUiScale = value;
        } else if ("perfPreset".equals(key)) {
            c.perfPreset = value % com.onyxclient.core.PerformancePresets.NAMES.length;
            OnyxClient.getConfigManager().save();
            com.onyxclient.core.PerformancePresets.apply(c.perfPreset);
            flash(com.onyxclient.core.PerformanceApplier.getPresetSummary(c.perfPreset));
            return;
        } else if ("renderDistanceOverride".equals(key)) {
            c.renderDistanceOverride = value % 6;
            OnyxClient.getConfigManager().save();
            com.onyxclient.core.PerformancePresets.apply(c.perfPreset);
            flash("Render distance: " + new String[]{"Auto", "2", "4", "6", "8", "12"}[c.renderDistanceOverride]);
            return;
        } else if ("focusedFpsCap".equals(key)) {
            c.focusedFpsCap = Math.max(0, Math.min(500, value));
            OnyxClient.getConfigManager().save();
            com.onyxclient.core.PerformancePresets.apply(c.perfPreset);
            flash(c.focusedFpsCap == 0 ? "Focused FPS: unlimited" : "Focused FPS cap: " + c.focusedFpsCap);
            return;
        } else {
            return;
        }
        OnyxClient.getConfigManager().save();
    }

    private void flash(String msg) {
        statusMessage = msg;
        statusTicks = 80;
    }

    private void updateDraggingSlider(int panelX, int panelY, int panelW, int panelH, int mouseX) {
        if (draggingColor != null && selectedModule != null) {
            if (Mouse.isButtonDown(0)) {
                int[] b = modulePopupBounds(panelX, panelY, panelW, panelH);
                applyColorChannel(draggingColor, draggingColorChannel, b[0] + 12, b[2] - 28, mouseX);
                saveSelectedModuleSettings();
            } else {
                draggingColor = null;
            }
            return;
        }
        if (draggingSlider == null || selectedModule == null) {
            return;
        }
        if (Mouse.isButtonDown(0)) {
            int[] b = modulePopupBounds(panelX, panelY, panelW, panelH);
            int sliderX = b[0] + 12;
            int sliderW = b[2] - 28;
            double pct = (mouseX - sliderX) / (double) sliderW;
            pct = Math.max(0, Math.min(1, pct));
            double value = draggingSlider.getMin() + pct * (draggingSlider.getMax() - draggingSlider.getMin());
            value = Math.round(value / draggingSlider.getIncrement()) * draggingSlider.getIncrement();
            draggingSlider.setValue(value);
            if (selectedModule != null) {
                saveSelectedModuleSettings();
            }
        } else {
            draggingSlider = null;
        }
    }

    private int hitColorChannel(int x, int y, int w, int mouseX, int mouseY) {
        for (int i = 0; i < 4; i++) {
            int rowY = y + i * 9;
            if (mouseX >= x && mouseX <= x + w && mouseY >= rowY && mouseY <= rowY + 8) {
                return i;
            }
        }
        return -1;
    }

    private void applyColorChannel(ColorSetting color, int channel, int x, int w, int mouseX) {
        int trackX = x + 10;
        int trackW = Math.max(1, w - 34);
        double pct = (mouseX - trackX) / (double) trackW;
        pct = Math.max(0, Math.min(1, pct));
        int value = (int) Math.round(pct * 255);
        if (channel == 0) {
            color.setRed(value);
        } else if (channel == 1) {
            color.setGreen(value);
        } else if (channel == 2) {
            color.setBlue(value);
        } else {
            color.setAlpha(value);
        }
    }

    private void updateScrollDrag(int mouseX, int mouseY) {
        if (scrollDrag == ScrollDrag.NONE || !Mouse.isButtonDown(0)) {
            if (!Mouse.isButtonDown(0)) {
                scrollDrag = ScrollDrag.NONE;
            }
            return;
        }
        int rel = mouseY - scrollDragTrackY;
        float pct = scrollDragTrackH <= 0 ? 0 : rel / (float) scrollDragTrackH;
        pct = Math.max(0, Math.min(1, pct));
        int offset = (int) (pct * scrollDragMax);
        if (scrollDrag == ScrollDrag.MODS) {
            scrollOffset = offset;
        } else if (scrollDrag == ScrollDrag.SETTINGS_LIST) {
            settingsScroll = offset;
        } else if (scrollDrag == ScrollDrag.SETTINGS_NAV) {
            settingsNavScroll = offset;
        } else if (scrollDrag == ScrollDrag.MOD_POPUP) {
            moduleSettingsScroll = offset;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        if (OnyxClient.getConfigManager().getConfig().disableScrollWheel) {
            return;
        }

        int[] panel = panelBounds();
        int contentY = panel[1] + HEADER_H + TAB_H;
        int clipH = Math.max(0, panel[3] - HEADER_H - TAB_H - PANEL_RADIUS);
        layoutTileH = tileHeightForClip(clipH);
        int rawX = Mouse.getEventX() * width / mc.displayWidth;
        int rawY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int[] layoutMouse = ClientUiScale.toLayoutMouse(rawX, rawY, width, height);
        int mouseX = layoutMouse[0];
        int mouseY = layoutMouse[1];

        if (topTab == TopTab.MODS && selectedModule != null) {
            int[] popup = modulePopupBounds(panel[0], panel[1], panel[2], panel[3]);
            if (mouseX >= popup[0] && mouseX <= popup[0] + popup[2]
                    && mouseY >= popup[1] && mouseY <= popup[1] + popup[3]) {
                int maxScroll = popupMaxScroll(popup[3]);
                moduleSettingsScroll = Math.max(0, Math.min(maxScroll,
                        moduleSettingsScroll + (wheel > 0 ? -22 : 22)));
                return;
            }
        }

        if (mouseX < panel[0] || mouseX > panel[0] + panel[2]
                || mouseY < contentY || mouseY > contentY + clipH) {
            return;
        }

        int step = layoutTileH + GRID_GAP;
        if (topTab == TopTab.MODS) {
            if (selectedModule != null) {
                return;
            }
            int gridW = panel[2] - SIDEBAR_W;
            if (mouseX < panel[0] + SIDEBAR_W) {
                return;
            }
            if (wheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - step);
            } else {
                scrollOffset = Math.min(maxModsScroll(gridW, clipH), scrollOffset + step);
            }
        } else if (topTab == TopTab.SETTINGS) {
            if (mouseX < panel[0] + SIDEBAR_W) {
                int navH = clipH - EDIT_HUD_FOOTER;
                int totalNav = FILTER_HEADER_H + SettingsPage.values().length * (NAV_ITEM_H + NAV_GAP);
                int maxNav = Math.max(0, totalNav - navH);
                settingsNavScroll = Math.max(0, Math.min(maxNav,
                        settingsNavScroll + (wheel > 0 ? -NAV_ITEM_H : NAV_ITEM_H)));
            } else {
                List<SettingRow> rows = settingsRows();
                int total = rows.size() * ROW_H;
                int maxScroll = Math.max(0, total - (clipH - 12));
                settingsScroll = Math.max(0, Math.min(maxScroll,
                        settingsScroll + (wheel > 0 ? -ROW_H : ROW_H)));
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int[] layoutMouse = ClientUiScale.toLayoutMouse(mouseX, mouseY, width, height);
        mouseX = layoutMouse[0];
        mouseY = layoutMouse[1];

        int[] panel = panelBounds();
        int panelX = panel[0];
        int panelY = panel[1];
        int panelW = panel[2];
        int panelH = panel[3];

        // Right-click favorite on mod cards
        if (mouseButton == 1 && topTab == TopTab.MODS && selectedModule == null) {
            handleModsRightClick(panelX, panelY + HEADER_H + TAB_H, panelW,
                    Math.max(0, panelH - HEADER_H - TAB_H - PANEL_RADIUS), mouseX, mouseY);
            return;
        }

        if (mouseButton != 0) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        // Popup captures clicks first
        if (selectedModule != null && topTab == TopTab.MODS) {
            if (handleModuleSettingsPopupClick(panelX, panelY, panelW, panelH, mouseX, mouseY)) {
                return;
            }
        }

        int closeCX = panelX + panelW - 16;
        int closeCY = panelY + 16;
        if (mouseX >= closeCX - 8 && mouseX <= closeCX + 8 && mouseY >= closeCY - 8 && mouseY <= closeCY + 8) {
            mc.displayGuiScreen(null);
            return;
        }

        // Search focus + tabs (shared layout with drawTabs)
        TabRowLayout tabRow = layoutTabRow(panelX, panelY + HEADER_H, panelW);
        if (mouseX >= tabRow.searchX && mouseX <= tabRow.searchX + tabRow.searchW
                && mouseY >= tabRow.searchY && mouseY <= tabRow.searchY + tabRow.searchH) {
            focus = Focus.SEARCH;
            return;
        }

        TopTab[] tabs = TopTab.values();
        for (int i = 0; i < tabs.length; i++) {
            if (mouseX >= tabRow.tabX[i] && mouseX <= tabRow.tabX[i] + tabRow.tabW[i]
                    && mouseY >= tabRow.tabY && mouseY <= tabRow.tabY + TAB_BTN_H) {
                topTab = tabs[i];
                selectedModule = null;
                scrollOffset = 0;
                settingsScroll = 0;
                moduleSettingsScroll = 0;
                resetPopupDragState();
                searchQuery = "";
                focus = Focus.NONE;
                return;
            }
        }

        int contentY = panelY + HEADER_H + TAB_H;
        int clipH = Math.max(0, panelH - HEADER_H - TAB_H - PANEL_RADIUS);
        layoutTileH = tileHeightForClip(clipH);

        if (topTab == TopTab.MODS) {
            handleModsClick(panelX, contentY, panelW, clipH, mouseX, mouseY);
        } else if (topTab == TopTab.SETTINGS) {
            handleSettingsClick(panelX, contentY, panelW, clipH, mouseX, mouseY);
        } else {
            handleProfilesClick(panelX, contentY, panelW, clipH, mouseX, mouseY);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void handleModsRightClick(int panelX, int contentY, int panelW, int clipH, int mouseX, int mouseY) {
        int gridX = panelX + SIDEBAR_W;
        int gridW = panelW - SIDEBAR_W;
        if (mouseX < gridX || mouseX > gridX + gridW || mouseY < contentY || mouseY > contentY + clipH) {
            return;
        }
        List<Module> modules = modulesVisible();
        int innerW = gridInnerW(gridW);
        int cols = columnCount(innerW);
        int cardW = cardWidth(innerW, cols);
        int cardH = cardHeight();
        int col = 0;
        int row = 0;
        for (Module module : modules) {
            int cardX = gridX + GRID_PAD + col * (cardW + GRID_GAP);
            int cardY = contentY + GRID_PAD + row * (cardH + GRID_GAP) - scrollOffset;
            if (cardY + cardH > contentY && cardY < contentY + clipH
                    && mouseX >= cardX && mouseX <= cardX + cardW
                    && mouseY >= cardY && mouseY <= cardY + cardH) {
                toggleFavorite(module);
                return;
            }
            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }
    }

    private void saveSelectedModuleSettings() {
        OnyxClient.getConfigManager().saveModule(selectedModule);
        if ("OptiFine Settings".equals(selectedModule.getName())) {
            com.onyxclient.core.PerformanceApplier.applyOptiFineModuleToDisk();
        }
    }

    /** @return true if click consumed */
    private boolean handleModuleSettingsPopupClick(int panelX, int panelY, int panelW, int panelH,
                                                   int mouseX, int mouseY) {
        int[] b = modulePopupBounds(panelX, panelY, panelW, panelH);
        int x = b[0];
        int y = b[1];
        int w = b[2];
        int h = b[3];

        // Click outside popup (dim) closes
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
            selectedModule = null;
            moduleSettingsScroll = 0;
            draggingSlider = null;
            resetPopupDragState();
            return true;
        }

        // X close
        if (mouseX >= x + w - 18 && mouseX <= x + w - 4 && mouseY >= y + 4 && mouseY <= y + 18) {
            selectedModule = null;
            moduleSettingsScroll = 0;
            resetPopupDragState();
            return true;
        }

        // Module enable toggle in header
        int tx = x + w - TOGGLE_W - 28;
        if (mouseX >= tx && mouseX <= tx + TOGGLE_W && mouseY >= y + 10 && mouseY <= y + 10 + TOGGLE_H) {
            selectedModule.toggle();
            OnyxClient.getConfigManager().saveModule(selectedModule);
            return true;
        }

        int bodyY = y + POPUP_HEADER_H;
        int bodyH = popupBodyHeight(h);
        int maxScroll = popupMaxScroll(h);

        // Scrollbar thumb drag
        int[] sb = scrollbarGeom(x, bodyY - GRID_PAD, w, bodyH + GRID_PAD * 2, maxScroll, moduleSettingsScroll);
        if (maxScroll > 0 && mouseX >= sb[0] && mouseX <= sb[0] + sb[2]
                && mouseY >= sb[1] && mouseY <= sb[1] + sb[3]) {
            scrollDrag = ScrollDrag.MOD_POPUP;
            scrollDragTrackY = sb[1];
            scrollDragTrackH = sb[3];
            scrollDragMax = maxScroll;
            float pct = (mouseY - sb[1]) / (float) sb[3];
            moduleSettingsScroll = (int) (Math.max(0, Math.min(1, pct)) * maxScroll);
            return true;
        }

        int settingY = bodyY - moduleSettingsScroll;
        for (Setting<?> setting : selectedModule.getSettings()) {
            if (setting instanceof BooleanSetting) {
                int bx = x + w - TOGGLE_W - 14;
                if (mouseX >= bx && mouseX <= bx + TOGGLE_W
                        && mouseY >= settingY - 1 && mouseY <= settingY + TOGGLE_H
                        && mouseY >= bodyY && mouseY <= bodyY + bodyH) {
                    BooleanSetting bool = (BooleanSetting) setting;
                    bool.setValue(!bool.getValue());
                    saveSelectedModuleSettings();
                    return true;
                }
            } else if (setting instanceof NumberSetting) {
                if (mouseX >= x + 12 && mouseX <= x + w - 16
                        && mouseY >= settingY + 6 && mouseY <= settingY + 16
                        && mouseY >= bodyY && mouseY <= bodyY + bodyH) {
                    NumberSetting num = (NumberSetting) setting;
                    double pct = (mouseX - x - 12) / (double) (w - 28);
                    pct = Math.max(0, Math.min(1, pct));
                    double value = num.getMin() + pct * (num.getMax() - num.getMin());
                    value = Math.round(value / num.getIncrement()) * num.getIncrement();
                    num.setValue(value);
                    saveSelectedModuleSettings();
                    draggingSlider = num;
                    return true;
                }
                settingY += 10;
            } else if (setting instanceof ColorSetting) {
                ColorSetting color = (ColorSetting) setting;
                int channel = hitColorChannel(x + 12, settingY + 12, w - 28, mouseX, mouseY);
                if (channel >= 0 && mouseY >= bodyY && mouseY <= bodyY + bodyH) {
                    applyColorChannel(color, channel, x + 12, w - 28, mouseX);
                    saveSelectedModuleSettings();
                    draggingColor = color;
                    draggingColorChannel = channel;
                    return true;
                }
                settingY += 36;
            } else if (setting instanceof ModeSetting) {
                if (mouseX >= x && mouseX <= x + w && mouseY >= settingY && mouseY <= settingY + 12
                        && mouseY >= bodyY && mouseY <= bodyY + bodyH) {
                    ((ModeSetting) setting).cycle();
                    saveSelectedModuleSettings();
                    return true;
                }
            }
            settingY += 18;
        }

        // Title-bar drag (exclude X and toggle)
        if (mouseY >= y && mouseY < y + POPUP_HEADER_H
                && mouseX >= x + 8 && mouseX < tx - 4) {
            draggingPopup = true;
            popupDragStartMouseX = mouseX;
            popupDragStartMouseY = mouseY;
            popupDragBaseX = popupDragX;
            popupDragBaseY = popupDragY;
            return true;
        }

        // Body click-drag scroll
        if (mouseY >= bodyY && mouseY <= bodyY + bodyH) {
            popupScrollDragging = true;
            popupScrollStartMouseY = mouseY;
            popupScrollStartOffset = moduleSettingsScroll;
            return true;
        }

        return true;
    }

    private void handleModsClick(int panelX, int contentY, int panelW, int clipH, int mouseX, int mouseY) {
        int gridX = panelX + SIDEBAR_W;
        int gridW = panelW - SIDEBAR_W;

        int max = maxModsScroll(gridW, clipH);
        int[] sb = scrollbarGeom(gridX, contentY, gridW, clipH, max, scrollOffset);
        if (max > 0 && mouseX >= sb[0] && mouseX <= sb[0] + sb[2]
                && mouseY >= sb[1] && mouseY <= sb[1] + sb[3]) {
            scrollDrag = ScrollDrag.MODS;
            scrollDragTrackY = sb[1];
            scrollDragTrackH = sb[3];
            scrollDragMax = max;
            float pct = (mouseY - sb[1]) / (float) sb[3];
            scrollOffset = (int) (Math.max(0, Math.min(1, pct)) * max);
            return;
        }

        if (mouseX >= panelX && mouseX <= panelX + SIDEBAR_W && mouseY >= contentY && mouseY <= contentY + clipH) {
            int listTop = contentY + FILTER_HEADER_H;
            int listH = Math.max(0, clipH - FILTER_HEADER_H);
            int itemY = listTop;
            int padX = 6;
            int itemW = SIDEBAR_W - padX * 2;
            for (FilterBucket bucket : FilterBucket.values()) {
                if (itemY + NAV_ITEM_H > listTop + listH) {
                    break;
                }
                if (mouseX >= panelX + padX && mouseX <= panelX + padX + itemW
                        && mouseY >= itemY && mouseY <= itemY + NAV_ITEM_H) {
                    filterBucket = bucket;
                    selectedModule = null;
                    scrollOffset = 0;
                    resetPopupDragState();
                    return;
                }
                itemY += NAV_ITEM_H + NAV_GAP;
            }
            return;
        }

        if (mouseX >= gridX && mouseX <= gridX + gridW && mouseY >= contentY && mouseY <= contentY + clipH) {
            List<Module> modules = modulesVisible();
            int innerW = gridInnerW(gridW);
            int cols = columnCount(innerW);
            int cardW = cardWidth(innerW, cols);
            int cardH = cardHeight();
            int col = 0;
            int row = 0;
            for (Module module : modules) {
                int cardX = gridX + GRID_PAD + col * (cardW + GRID_GAP);
                int cardY = contentY + GRID_PAD + row * (cardH + GRID_GAP) - scrollOffset;
                if (cardY + cardH > contentY && cardY < contentY + clipH
                        && mouseX >= cardX && mouseX <= cardX + cardW
                        && mouseY >= cardY && mouseY <= cardY + cardH) {
                    int toggleX = cardX + (cardW - TOGGLE_W) / 2;
                    int toggleY = cardY + cardH - TOGGLE_H - 3;
                    // Star hit target (top-left) — Badlion-style favorites
                    if (mouseX >= cardX && mouseX <= cardX + 14
                            && mouseY >= cardY && mouseY <= cardY + 12) {
                        toggleFavorite(module);
                        return;
                    }
                    if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                            && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_H) {
                        module.toggle();
                        OnyxClient.getConfigManager().saveModule(module);
                    } else {
                        selectedModule = module;
                        moduleSettingsScroll = 0;
                        resetPopupDragState();
                    }
                    return;
                }
                col++;
                if (col >= cols) {
                    col = 0;
                    row++;
                }
            }
        }
    }

    private void handleSettingsClick(int panelX, int contentY, int panelW, int clipH, int mouseX, int mouseY) {
        // Edit HUD FIRST (footer) — critical so category hits don't steal the click
        int btnY = contentY + clipH - 24;
        int btnW = SIDEBAR_W - 12;
        if (mouseX >= panelX + 6 && mouseX <= panelX + 6 + btnW && mouseY >= btnY && mouseY <= btnY + 18) {
            mc.displayGuiScreen(new HudEditor(this));
            return;
        }

        int navH = clipH - EDIT_HUD_FOOTER;
        if (mouseX >= panelX && mouseX <= panelX + SIDEBAR_W && mouseY >= contentY && mouseY <= contentY + navH) {
            int itemY = contentY + FILTER_HEADER_H - settingsNavScroll;
            int padX = 6;
            int itemW = SIDEBAR_W - padX * 2;
            for (SettingsPage page : SettingsPage.values()) {
                if (mouseY >= itemY && mouseY <= itemY + NAV_ITEM_H
                        && mouseX >= panelX + padX && mouseX <= panelX + padX + itemW
                        && mouseY >= contentY && mouseY <= contentY + navH) {
                    settingsPage = page;
                    settingsScroll = 0;
                    return;
                }
                itemY += NAV_ITEM_H + NAV_GAP;
            }
        }

        int listX = panelX + SIDEBAR_W;
        int listW = panelW - SIDEBAR_W;

        // Settings list scrollbar
        List<SettingRow> rows = settingsRows();
        int total = rows.size() * ROW_H;
        int maxScroll = Math.max(0, total - (clipH - 12));
        int[] sb = scrollbarGeom(listX, contentY, listW, clipH, maxScroll, settingsScroll);
        if (maxScroll > 0 && mouseX >= sb[0] && mouseX <= sb[0] + sb[2]
                && mouseY >= sb[1] && mouseY <= sb[1] + sb[3]) {
            scrollDrag = ScrollDrag.SETTINGS_LIST;
            scrollDragTrackY = sb[1];
            scrollDragTrackH = sb[3];
            scrollDragMax = maxScroll;
            float pct = (mouseY - sb[1]) / (float) sb[3];
            settingsScroll = (int) (Math.max(0, Math.min(1, pct)) * maxScroll);
            return;
        }

        int rowY = contentY + 10 - settingsScroll;
        for (SettingRow row : rows) {
            int rh = ROW_H + (row.type == SettingRow.Type.SLIDER ? 6 : 0);
            if (mouseY >= Math.max(rowY, contentY) && mouseY <= Math.min(rowY + rh, contentY + clipH)
                    && mouseX >= listX && mouseX <= listX + listW) {
                if (row.type == SettingRow.Type.CONFIG_TOGGLE) {
                    toggleConfigBool(row.key);
                    return;
                }
                if (row.type == SettingRow.Type.ACTION) {
                    if ("hypixel".equals(row.key)) {
                        focus = Focus.API_KEY;
                        apiEditBuffer = OnyxClient.getConfigManager().getHypixelApiKey();
                        flash("Type API key, Enter to save");
                    } else if ("onyx".equals(row.key)) {
                        focus = Focus.API_ENDPOINT;
                        apiEditBuffer = OnyxClient.getConfigManager().getOnyxApiEndpoint();
                        flash("Type endpoint, Enter to save");
                    } else if ("skins".equals(row.key)) {
                        mc.displayGuiScreen(new SkinChangerGui(this));
                    }
                    return;
                }
                if (row.type == SettingRow.Type.STEPPER) {
                    int idx = getConfigInt(row.key);
                    setConfigInt(row.key, (idx + 1) % row.options.length);
                    return;
                }
                if (row.type == SettingRow.Type.SLIDER) {
                    int sx = listX + 12;
                    int sw = listW - 36;
                    double pct = (mouseX - sx) / (double) sw;
                    pct = Math.max(0, Math.min(1, pct));
                    setConfigInt(row.key, row.min + (int) Math.round(pct * (row.max - row.min)));
                    return;
                }
            }
            rowY += rh;
        }
    }

    private void handleProfilesClick(int panelX, int contentY, int panelW, int clipH, int mouseX, int mouseY) {
        int x = panelX + 14;
        int y = contentY + 10;
        int w = panelW - 28;
        int fieldY = y + 28;

        if (mouseX >= x && mouseX <= x + 170 && mouseY >= fieldY && mouseY <= fieldY + 16) {
            focus = Focus.PROFILE_NAME;
            return;
        }
        if (mouseX >= x + 178 && mouseX <= x + 240 && mouseY >= fieldY && mouseY <= fieldY + 16) {
            if (OnyxClient.getConfigManager().saveProfile(profileName)) {
                flash("Saved: " + profileName);
                profileName = "";
            } else {
                flash("Could not save profile");
            }
            return;
        }

        List<String> profiles = OnyxClient.getConfigManager().listProfiles();
        String q = searchQuery.toLowerCase();
        int listY = fieldY + 24;
        for (String name : profiles) {
            if (!q.isEmpty() && !name.toLowerCase().contains(q)) continue;
            if (listY + 20 > contentY + clipH) break;
            if (mouseX >= x + w - 145 && mouseX <= x + w - 110 && mouseY >= listY && mouseY <= listY + 18) {
                if (OnyxClient.getConfigManager().loadProfile(name)) {
                    flash("Loaded: " + name);
                }
                return;
            }
            if (mouseX >= x + w - 105 && mouseX <= x + w - 60 && mouseY >= listY && mouseY <= listY + 18) {
                if (profileName.isEmpty()) {
                    flash("Type a new name first");
                    focus = Focus.PROFILE_NAME;
                } else if (OnyxClient.getConfigManager().renameProfile(name, profileName)) {
                    flash("Renamed to " + profileName);
                    profileName = "";
                } else {
                    flash("Rename failed");
                }
                return;
            }
            if (mouseX >= x + w - 55 && mouseX <= x + w - 10 && mouseY >= listY && mouseY <= listY + 18) {
                if (OnyxClient.getConfigManager().deleteProfile(name)) {
                    flash("Deleted: " + name);
                }
                return;
            }
            listY += 22;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (selectedModule != null) {
                selectedModule = null;
                moduleSettingsScroll = 0;
                draggingSlider = null;
                draggingColor = null;
                resetPopupDragState();
                return;
            }
            if (focus != Focus.NONE) {
                focus = Focus.NONE;
                return;
            }
            mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_RSHIFT && focus == Focus.NONE) {
            mc.displayGuiScreen(null);
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if (focus == Focus.API_KEY) {
                OnyxClient.getConfigManager().setHypixelApiKey(apiEditBuffer);
                flash("Hypixel API key saved");
                focus = Focus.NONE;
                apiEditBuffer = "";
                return;
            }
            if (focus == Focus.API_ENDPOINT) {
                OnyxClient.getConfigManager().setOnyxApiEndpoint(apiEditBuffer);
                flash("Onyx API endpoint saved");
                focus = Focus.NONE;
                apiEditBuffer = "";
                return;
            }
        }

        StringBuilder target = null;
        if (focus == Focus.SEARCH) {
            if (keyCode == Keyboard.KEY_BACK && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return;
            }
            if (isTypeChar(typedChar)) {
                searchQuery += typedChar;
            }
            return;
        }
        if (focus == Focus.PROFILE_NAME) {
            if (keyCode == Keyboard.KEY_BACK && !profileName.isEmpty()) {
                profileName = profileName.substring(0, profileName.length() - 1);
                return;
            }
            if (isTypeChar(typedChar)) {
                profileName += typedChar;
            }
            return;
        }
        if (focus == Focus.API_KEY || focus == Focus.API_ENDPOINT) {
            if (keyCode == Keyboard.KEY_BACK && !apiEditBuffer.isEmpty()) {
                apiEditBuffer = apiEditBuffer.substring(0, apiEditBuffer.length() - 1);
                return;
            }
            if (isTypeChar(typedChar) || typedChar == ':' || typedChar == '/' || typedChar == '?' || typedChar == '.') {
                apiEditBuffer += typedChar;
            }
            return;
        }

        // Default: type into search if no focus
        if (keyCode == Keyboard.KEY_BACK && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
        } else if (isTypeChar(typedChar)) {
            focus = Focus.SEARCH;
            searchQuery += typedChar;
        }
    }

    private boolean isTypeChar(char c) {
        return Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_';
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (clickedMouseButton == 0 && selectedModule != null && topTab == TopTab.MODS) {
            int[] layoutMouse = ClientUiScale.toLayoutMouse(mouseX, mouseY, width, height);
            mouseX = layoutMouse[0];
            mouseY = layoutMouse[1];
            int[] panel = panelBounds();

            if (draggingPopup) {
                popupDragX = popupDragBaseX + (mouseX - popupDragStartMouseX);
                popupDragY = popupDragBaseY + (mouseY - popupDragStartMouseY);
                int w = Math.min(POPUP_W, panel[2] - 24);
                int h = Math.min(POPUP_HEADER_H + POPUP_BODY_H + POPUP_FOOTER_PAD, panel[3] - 20);
                clampPopupDrag(panel[0], panel[1], panel[2], panel[3], w, h);
            } else if (popupScrollDragging) {
                int[] popup = modulePopupBounds(panel[0], panel[1], panel[2], panel[3]);
                int maxScroll = popupMaxScroll(popup[3]);
                moduleSettingsScroll = Math.max(0, Math.min(maxScroll,
                        popupScrollStartOffset - (mouseY - popupScrollStartMouseY)));
            }
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            draggingSlider = null;
            draggingColor = null;
            scrollDrag = ScrollDrag.NONE;
            draggingPopup = false;
            popupScrollDragging = false;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static final class SettingRow {
        enum Type { HEADER, INFO, CONFIG_TOGGLE, ACTION, SLIDER, STEPPER }
        final Type type;
        final String label;
        final String key;
        final String value;
        final int min;
        final int max;
        final String[] options;

        private SettingRow(Type type, String label, String key, String value, int min, int max, String[] options) {
            this.type = type;
            this.label = label;
            this.key = key;
            this.value = value;
            this.min = min;
            this.max = max;
            this.options = options;
        }

        static SettingRow header(String label) {
            return new SettingRow(Type.HEADER, label, null, null, 0, 0, null);
        }
        static SettingRow info(String label) {
            return new SettingRow(Type.INFO, label, null, null, 0, 0, null);
        }
        static SettingRow configToggle(String label, String key) {
            return new SettingRow(Type.CONFIG_TOGGLE, label, key, null, 0, 0, null);
        }
        static SettingRow action(String label, String value, String key) {
            return new SettingRow(Type.ACTION, label, key, value, 0, 0, null);
        }
        static SettingRow slider(String label, String key, int min, int max) {
            return new SettingRow(Type.SLIDER, label, key, null, min, max, null);
        }
        static SettingRow stepper(String label, String key, String[] options) {
            return new SettingRow(Type.STEPPER, label, key, null, 0, 0, options);
        }
    }
}
