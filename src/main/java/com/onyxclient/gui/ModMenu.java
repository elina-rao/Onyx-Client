package com.onyxclient.gui;

import com.onyxclient.OnyxClient;
import com.onyxclient.core.config.ConfigManager;
import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.modules.settings.Setting;
import com.onyxclient.utils.AnimationUtils;
import com.onyxclient.utils.ClientUiScale;
import com.onyxclient.utils.Colors;
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
    private static final int HEADER_H = 28;
    private static final int TAB_H = 24;
    private static final int SIDEBAR_W = 86;
    private static final int SETTINGS_W = 160;
    private static final int CARD_RADIUS = 4;
    private static final int NAV_ITEM_H = 18;
    private static final int NAV_GAP = 2;
    private static final int GRID_PAD = 8;
    private static final int GRID_GAP = 6;
    /**
     * Badlion mod-menu panel size measured from screenshot (centered floating HUD).
     * Size only — Onyx colors/chrome stay. ~56.6% × 56.1% of scaled GUI.
     */
    private static final float BLC_PANEL_W = 0.566F;
    private static final float BLC_PANEL_H = 0.561F;
    /** Visible module rows in the Mods grid viewport (BLC shows ~2.5). */
    private static final float BLC_VISIBLE_ROWS = 2.5F;
    /** Prefer 4 columns; cards widen to fill so full names fit. */
    private static final int TILE_W = 48;
    private static final int SCROLLBAR_W = 4;
    private static final int CLIP_INSET = 2;
    private static final int TOGGLE_W = 22;
    private static final int TOGGLE_H = 10;
    private static final int ROW_H = 20;
    private static final int EDIT_HUD_FOOTER = 30;
    private static final int TAB_PAD_X = 8;
    private static final int TAB_GAP = 6;
    private static final int TAB_BTN_H = 15;
    /** Space between Profiles and Search — enough to breathe, not a large void. */
    private static final int TAB_SEARCH_GAP = 20;
    private static final int SEARCH_W = 100;
    private static final int SEARCH_H = 14;
    private static final int SEARCH_RIGHT_PAD = 12;

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

    private enum Focus {
        NONE, SEARCH, PROFILE_NAME, API_KEY, API_ENDPOINT
    }

    private enum ScrollDrag {
        NONE, MODS, SETTINGS_LIST, SETTINGS_NAV
    }

    private TopTab topTab = TopTab.MODS;
    private SettingsPage settingsPage = SettingsPage.GENERAL;
    /** null = All filter */
    private ModuleCategory selectedCategory = null;
    private Module selectedModule;
    private String searchQuery = "";
    private String profileName = "";
    private String apiEditBuffer = "";
    private Focus focus = Focus.NONE;
    private int scrollOffset;
    private int settingsScroll;
    private int settingsNavScroll;
    private NumberSetting draggingSlider;
    private ScrollDrag scrollDrag = ScrollDrag.NONE;
    private int scrollDragTrackY;
    private int scrollDragTrackH;
    private int scrollDragMax;
    /** Tile height for current frame — derived so Mods grid shows {@link #BLC_VISIBLE_ROWS}. */
    private int layoutTileH = 48;
    private String statusMessage = "";
    private int statusTicks;

    public ModMenu() {
        AnimationUtils.startMenuOpen();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
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

        updateDraggingSlider(panelX, panelW, lx);
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

            if (statusTicks > 0 && !statusMessage.isEmpty()) {
                fontRendererObj.drawString(statusMessage, panelX + 10, panelY + panelH - 12, Colors.ACCENT_BRIGHT);
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

    /** Tile height so the Mods viewport shows ~2.5 rows inside the BLC-sized panel. */
    private int tileHeightForClip(int clipH) {
        int view = Math.max(1, clipH - GRID_PAD * 2);
        int h = Math.round((view - 1.5F * GRID_GAP) / BLC_VISIBLE_ROWS);
        return Math.max(36, Math.min(72, h));
    }

    private void applyOpenAnimScale(int x, int y, int w, int h, float progress) {
        float scale = 0.97F + 0.03F * progress;
        GlStateManager.translate(x + w / 2.0F, y + h / 2.0F, 0);
        GlStateManager.scale(scale, scale, 1.0F);
        GlStateManager.translate(-(x + w / 2.0F), -(y + h / 2.0F), 0);
    }

    /**
     * GL scissor is unreliable on Mac Retina / OptiFine — soft-clip in draw paths instead.
     */
    private void enableLayoutScissor(int x, int y, int w, int h) {
        // no-op
    }

    private void drawHeader(int x, int y, int w, int mouseX, int mouseY) {
        drawLogo(x + 8, y + 4, 22);
        int textX = x + 34;
        fontRendererObj.drawString("ONYX", textX, y + 6, Colors.TEXT_PRIMARY);
        fontRendererObj.drawString("CLIENT", textX + fontRendererObj.getStringWidth("ONYX") + 3, y + 6, Colors.ACCENT_BRIGHT);

        int chipX = textX + fontRendererObj.getStringWidth("ONYX CLIENT") + 10;
        int chipW = fontRendererObj.getStringWidth("RANKED") + 8;
        RenderUtils.drawRoundedRect(chipX, y + 5, chipW, 11, 3, Colors.withAlpha(Colors.ACCENT_PRIMARY, 180));
        fontRendererObj.drawString("RANKED", chipX + 4, y + 7, Colors.TEXT_PRIMARY);

        int closeCX = x + w - 16;
        int closeCY = y + 16;
        boolean closeHover = mouseX >= closeCX - 8 && mouseX <= closeCX + 8
                && mouseY >= closeCY - 8 && mouseY <= closeCY + 8;
        RenderUtils.drawCircle(closeCX, closeCY, 7, closeHover ? Colors.withAlpha(Colors.DANGER, 60) : Colors.BG_CARD);
        fontRendererObj.drawString("\u00D7", closeCX - 2, closeCY - 3, closeHover ? Colors.DANGER : Colors.TEXT_MUTED);
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
            int tw = fontRendererObj.getStringWidth(tab.label);
            fontRendererObj.drawString(tab.label, tabX + (tabW - tw) / 2, layout.tabY + 4,
                    active ? Colors.TEXT_PRIMARY : Colors.TEXT_MUTED);
        }

        boolean searchFocused = focus == Focus.SEARCH;
        RenderUtils.drawRoundedRect(layout.searchX, layout.searchY, layout.searchW, layout.searchH, 3,
                searchFocused ? Colors.BG_HOVER : Colors.BG_CARD);
        RenderUtils.drawRoundedOutline(layout.searchX, layout.searchY, layout.searchW, layout.searchH, 3, 1.0F,
                searchFocused ? Colors.ACCENT_PRIMARY : Colors.BORDER_SOFT);
        String text = searchQuery.isEmpty() ? "Search..." : searchQuery;
        fontRendererObj.drawString(trimToWidth(text, layout.searchW - 8), layout.searchX + 4, layout.searchY + 3,
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
            int tw = fontRendererObj.getStringWidth(tabs[i].label) + TAB_PAD_X * 2;
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
        boolean settingsOpen = selectedModule != null;
        int settingsW = settingsOpen ? SETTINGS_W : 0;
        int gridX = panelX + SIDEBAR_W;
        int gridW = panelW - SIDEBAR_W - settingsW - (settingsOpen ? 6 : 0);

        clampModsScroll(gridW, clipH);

        drawModFilters(panelX, contentY, SIDEBAR_W, clipH, mouseX, mouseY);

        RenderUtils.drawVerticalLine(panelX + SIDEBAR_W, contentY + 6, clipH - 12, Colors.DIVIDER);

        drawModuleGrid(gridX, contentY, gridW, clipH, mouseX, mouseY);

        drawScrollbar(gridX, contentY, gridW, clipH, maxModsScroll(gridW, clipH), scrollOffset, ScrollDrag.MODS);

        if (settingsOpen) {
            drawModuleSettings(panelX + panelW - SETTINGS_W - 6, contentY + 6, SETTINGS_W, clipH - 12, mouseX, mouseY);
        }
    }

    private void drawModFilters(int x, int y, int w, int h, int mouseX, int mouseY) {
        RenderUtils.drawRect(x + 1, y, w - 1, h, Colors.withAlpha(Colors.BG_SIDEBAR, 160));
        fontRendererObj.drawString("Filter", x + 10, y + 6, Colors.TEXT_MUTED);

        int itemY = y + 18;
        int padX = 6;
        int itemW = w - padX * 2;

        // All
        drawFilterItem(x, padX, itemW, itemY, "All", selectedCategory == null, mouseX, mouseY, y, h);
        itemY += NAV_ITEM_H + NAV_GAP;

        for (ModuleCategory category : ModuleCategory.values()) {
            if (itemY + NAV_ITEM_H > y + h) {
                break;
            }
            drawFilterItem(x, padX, itemW, itemY, category.getDisplayName(),
                    selectedCategory == category, mouseX, mouseY, y, h);
            itemY += NAV_ITEM_H + NAV_GAP;
        }
    }

    private void drawFilterItem(int x, int padX, int itemW, int itemY, String label, boolean selected,
                                int mouseX, int mouseY, int clipY, int clipH) {
        boolean hovered = mouseX >= x + padX && mouseX <= x + padX + itemW
                && mouseY >= itemY && mouseY <= itemY + NAV_ITEM_H
                && mouseY >= clipY && mouseY <= clipY + clipH;
        if (selected) {
            RenderUtils.drawRoundedRect(x + padX, itemY, itemW, NAV_ITEM_H, 3, Colors.BG_SELECTED);
            RenderUtils.drawRoundedRect(x + padX, itemY + 3, 2, NAV_ITEM_H - 6, 1, Colors.ACCENT_PRIMARY);
        } else if (hovered) {
            RenderUtils.drawRoundedRect(x + padX, itemY, itemW, NAV_ITEM_H, 3, Colors.BG_HOVER);
        }
        fontRendererObj.drawString(label, x + padX + 8, itemY + 5,
                selected ? Colors.TEXT_PRIMARY : (hovered ? Colors.TEXT_SECONDARY : Colors.TEXT_MUTED));
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

                int fill = selected ? Colors.BG_SELECTED : (hovered ? Colors.BG_HOVER : Colors.BG_CARD);
                RenderUtils.drawRoundedRect(cardX, cardY, cardW, cardH, CARD_RADIUS, fill);
                if (enabled) {
                    RenderUtils.drawRoundedOutline(cardX, cardY, cardW, cardH, CARD_RADIUS, 1.0F,
                            Colors.withAlpha(Colors.ACCENT_PRIMARY, 140));
                } else {
                    RenderUtils.drawRoundedOutline(cardX, cardY, cardW, cardH, CARD_RADIUS, 1.0F, Colors.BORDER_SOFT);
                }

                // Full name like Badlion — never ellipsis; shrink-to-fit only if needed
                drawCardTitle(module.getName(), cardX, cardY + 3, cardW);

                // Compact center mark (BLC uses icons; we keep a small accent mark)
                int markCX = cardX + cardW / 2;
                int markCY = cardY + 17;
                int markR = Math.min(6, Math.max(4, cardW / 9));
                RenderUtils.drawCircle(markCX, markCY, markR,
                        enabled ? Colors.withAlpha(Colors.ACCENT_PRIMARY, 100) : Colors.withAlpha(Colors.BG_HOVER, 220));
                RenderUtils.drawCircle(markCX, markCY, Math.max(2, markR / 3),
                        enabled ? Colors.ACCENT_BRIGHT : Colors.TEXT_DISABLED);

                drawToggle(cardX + (cardW - TOGGLE_W) / 2, cardY + cardH - TOGGLE_H - 4, enabled);
            }

            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }
    }

    private void drawModuleSettings(int x, int y, int w, int h, int mouseX, int mouseY) {
        RenderUtils.drawSoftShadow(x, y, w, h, 5, 2);
        RenderUtils.drawRoundedRect(x, y, w, h, 5, Colors.withAlpha(Colors.BG_CARD, 245));
        RenderUtils.drawRoundedOutline(x, y, w, h, 5, 1.0F, Colors.BORDER);

        fontRendererObj.drawString(trimToWidth(selectedModule.getName(), w - 16), x + 8, y + 8, Colors.TEXT_PRIMARY);
        fontRendererObj.drawString("Settings", x + 8, y + 18, Colors.TEXT_MUTED);
        RenderUtils.drawHorizontalLine(x + 6, y + 28, w - 12, Colors.DIVIDER);

        int settingY = y + 34;
        for (Setting<?> setting : selectedModule.getSettings()) {
            if (settingY > y + h - 16) {
                break;
            }
            fontRendererObj.drawString(setting.getName(), x + 8, settingY, Colors.TEXT_SECONDARY);
            if (setting instanceof BooleanSetting) {
                drawToggle(x + w - TOGGLE_W - 10, settingY - 1, ((BooleanSetting) setting).getValue());
            } else if (setting instanceof NumberSetting) {
                drawSlider(x + 8, settingY + 10, w - 16, (NumberSetting) setting);
                settingY += 10;
            } else if (setting instanceof ModeSetting) {
                String val = ((ModeSetting) setting).getValue();
                int vw = fontRendererObj.getStringWidth(val);
                RenderUtils.drawRoundedRect(x + w - vw - 14, settingY - 1, vw + 8, 11, 3, Colors.BG_HOVER);
                fontRendererObj.drawString(val, x + w - vw - 10, settingY, Colors.ACCENT_BRIGHT);
            }
            settingY += 18;
        }
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
        int tw = fontRendererObj.getStringWidth("Edit HUD");
        fontRendererObj.drawString("Edit HUD", panelX + 6 + (btnW - tw) / 2, btnY + 5, Colors.TEXT_PRIMARY);

        RenderUtils.drawVerticalLine(panelX + SIDEBAR_W, contentY + 6, clipH - 12, Colors.DIVIDER);

        int listX = panelX + SIDEBAR_W;
        int listW = panelW - SIDEBAR_W;
        drawSettingsList(listX, contentY, listW, clipH, mouseX, mouseY);
    }

    private void drawSettingsSidebar(int x, int y, int w, int h, int mouseX, int mouseY) {
        RenderUtils.drawRect(x + 1, y, w - 1, h, Colors.withAlpha(Colors.BG_SIDEBAR, 160));
        fontRendererObj.drawString("Overview", x + 10, y + 6 - settingsNavScroll, Colors.TEXT_MUTED);

        SettingsPage[] pages = SettingsPage.values();
        int itemY = y + 18 - settingsNavScroll;
        int padX = 6;
        int itemW = w - padX * 2;
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
                fontRendererObj.drawString(page.label, x + padX + 8, itemY + 5,
                        selected ? Colors.TEXT_PRIMARY : Colors.TEXT_MUTED);
            }
            itemY += NAV_ITEM_H + NAV_GAP;
        }

        int totalNav = 18 + pages.length * (NAV_ITEM_H + NAV_GAP);
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
                rows.add(SettingRow.slider("Unfocused FPS Limit", "unfocusedFpsLimit", 5, 60));
                rows.add(SettingRow.info("Toggle FPS mods in the Mods tab."));
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
                    fontRendererObj.drawString(row.label, x + 12, rowY + 5, Colors.TEXT_MUTED);
                } else if (row.type == SettingRow.Type.INFO) {
                    fontRendererObj.drawString(trimToWidth(row.label, w - 24), x + 12, rowY + 5, Colors.TEXT_SECONDARY);
                } else if (row.type == SettingRow.Type.CONFIG_TOGGLE) {
                    fontRendererObj.drawString(row.label, x + 12, rowY + 5, Colors.TEXT_PRIMARY);
                    drawToggle(x + w - TOGGLE_W - 14, rowY + 3, getConfigBool(row.key));
                } else if (row.type == SettingRow.Type.ACTION) {
                    fontRendererObj.drawString(row.label, x + 12, rowY + 5, Colors.TEXT_PRIMARY);
                    String val = row.value != null ? row.value : "";
                    int vw = fontRendererObj.getStringWidth(val);
                    RenderUtils.drawRoundedRect(x + w - vw - 18, rowY + 2, vw + 10, 12, 3, Colors.BG_HOVER);
                    fontRendererObj.drawString(val, x + w - vw - 13, rowY + 4, Colors.ACCENT_BRIGHT);
                } else if (row.type == SettingRow.Type.SLIDER) {
                    fontRendererObj.drawString(row.label, x + 12, rowY + 1, Colors.TEXT_PRIMARY);
                    int val = getConfigInt(row.key);
                    double pct = (val - row.min) / (double) (row.max - row.min);
                    int sx = x + 12;
                    int sw = w - 36;
                    int sy = rowY + 13;
                    RenderUtils.drawRoundedRect(sx, sy, sw, 3, 1, Colors.BG_HOVER);
                    RenderUtils.drawRoundedRect(sx, sy, Math.max(2, (int) (sw * pct)), 3, 1, Colors.ACCENT_PRIMARY);
                    RenderUtils.drawCircle(sx + (int) (sw * pct), sy + 1, 3, Colors.TEXT_PRIMARY);
                    fontRendererObj.drawString(String.valueOf(val), x + w - 24, rowY + 1, Colors.TEXT_MUTED);
                } else if (row.type == SettingRow.Type.STEPPER) {
                    fontRendererObj.drawString(row.label, x + 12, rowY + 5, Colors.TEXT_PRIMARY);
                    int idx = Math.max(0, Math.min(row.options.length - 1, getConfigInt(row.key)));
                    String val = "< " + row.options[idx] + " >";
                    int vw = fontRendererObj.getStringWidth(val);
                    fontRendererObj.drawString(val, x + w - vw - 14, rowY + 5, Colors.ACCENT_BRIGHT);
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

        fontRendererObj.drawString("Local Profiles", x, y, Colors.TEXT_PRIMARY);
        fontRendererObj.drawString("Save your mods + HUD layout", x, y + 11, Colors.TEXT_MUTED);

        int fieldY = y + 28;
        boolean nameFocused = focus == Focus.PROFILE_NAME;
        RenderUtils.drawRoundedRect(x, fieldY, 170, 16, 3, nameFocused ? Colors.BG_HOVER : Colors.BG_CARD);
        RenderUtils.drawRoundedOutline(x, fieldY, 170, 16, 3, 1.0F,
                nameFocused ? Colors.ACCENT_PRIMARY : Colors.BORDER_SOFT);
        String draft = profileName.isEmpty() ? "Profile name..." : profileName;
        fontRendererObj.drawString(trimToWidth(draft, 158), x + 5, fieldY + 4,
                profileName.isEmpty() ? Colors.TEXT_DISABLED : Colors.TEXT_PRIMARY);

        boolean createHover = mouseX >= x + 178 && mouseX <= x + 240 && mouseY >= fieldY && mouseY <= fieldY + 16;
        RenderUtils.drawRoundedRect(x + 178, fieldY, 62, 16, 3,
                createHover ? Colors.ACCENT_PRIMARY : Colors.BG_HOVER);
        fontRendererObj.drawString("Create", x + 192, fieldY + 4, Colors.TEXT_PRIMARY);

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
            fontRendererObj.drawString(name, x + 6, listY + 5, Colors.TEXT_PRIMARY);
            fontRendererObj.drawString("Load", x + w - 100, listY + 5, Colors.ACCENT_BRIGHT);
            fontRendererObj.drawString("Delete", x + w - 55, listY + 5, Colors.DANGER);
            listY += 22;
        }

        if (profiles.isEmpty()) {
            fontRendererObj.drawString("No profiles yet — create one above.", x, listY, Colors.TEXT_MUTED);
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
            if (selectedCategory != null && module.getCategory() != selectedCategory) {
                continue;
            }
            result.add(module);
        }
        // Enabled / kept mods first (like Badlion), then A–Z within each group
        Collections.sort(result, new Comparator<Module>() {
            @Override
            public int compare(Module a, Module b) {
                if (a.isEnabled() != b.isEnabled()) {
                    return a.isEnabled() ? -1 : 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return result;
    }

    /** Centered full title; scales down uniformly if wider than the card (no ellipsis). */
    private void drawCardTitle(String name, int cardX, int cardY, int cardW) {
        if (name == null || name.isEmpty()) {
            return;
        }
        int maxW = Math.max(8, cardW - 8);
        int nameW = fontRendererObj.getStringWidth(name);
        if (nameW <= maxW) {
            fontRendererObj.drawString(name, cardX + (cardW - nameW) / 2, cardY, Colors.TEXT_PRIMARY);
            return;
        }
        float scale = maxW / (float) nameW;
        if (scale < 0.72F) {
            scale = 0.72F;
        }
        GlStateManager.pushMatrix();
        float cx = cardX + cardW / 2.0F;
        GlStateManager.translate(cx, cardY, 0);
        GlStateManager.scale(scale, scale, 1.0F);
        fontRendererObj.drawString(name, -nameW / 2, 0, Colors.TEXT_PRIMARY);
        GlStateManager.popMatrix();
    }

    private String trimToWidth(String text, int maxPx) {
        if (maxPx <= 0 || text == null) {
            return "";
        }
        if (fontRendererObj.getStringWidth(text) <= maxPx) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisW = fontRendererObj.getStringWidth(ellipsis);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (fontRendererObj.getStringWidth(sb.toString() + text.charAt(i)) + ellipsisW > maxPx) {
                break;
            }
            sb.append(text.charAt(i));
        }
        return sb.append(ellipsis).toString();
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
    }

    private int getConfigInt(String key) {
        ConfigManager.ClientConfig c = OnyxClient.getConfigManager().getConfig();
        if ("unfocusedFpsLimit".equals(key)) return c.unfocusedFpsLimit;
        if ("menuUiScale".equals(key)) return c.menuUiScale;
        return 0;
    }

    private void setConfigInt(String key, int value) {
        ConfigManager.ClientConfig c = OnyxClient.getConfigManager().getConfig();
        if ("unfocusedFpsLimit".equals(key)) c.unfocusedFpsLimit = value;
        else if ("menuUiScale".equals(key)) c.menuUiScale = value;
        else return;
        OnyxClient.getConfigManager().save();
    }

    private void flash(String msg) {
        statusMessage = msg;
        statusTicks = 80;
    }

    private void updateDraggingSlider(int panelX, int panelW, int mouseX) {
        if (draggingSlider == null) return;
        if (Mouse.isButtonDown(0)) {
            int sliderX = panelX + panelW - SETTINGS_W + 4;
            int sliderW = SETTINGS_W - 24;
            double pct = (mouseX - sliderX) / (double) sliderW;
            pct = Math.max(0, Math.min(1, pct));
            double value = draggingSlider.getMin() + pct * (draggingSlider.getMax() - draggingSlider.getMin());
            value = Math.round(value / draggingSlider.getIncrement()) * draggingSlider.getIncrement();
            draggingSlider.setValue(value);
            OnyxClient.getConfigManager().saveModule(selectedModule);
        } else {
            draggingSlider = null;
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

        if (mouseX < panel[0] || mouseX > panel[0] + panel[2]
                || mouseY < contentY || mouseY > contentY + clipH) {
            return;
        }

        int step = layoutTileH + GRID_GAP;
        if (topTab == TopTab.MODS) {
            boolean settingsOpen = selectedModule != null;
            int gridW = panel[2] - SIDEBAR_W - (settingsOpen ? SETTINGS_W + 6 : 0);
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
                int totalNav = 18 + SettingsPage.values().length * (NAV_ITEM_H + NAV_GAP);
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
        if (mouseButton != 0) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        int[] layoutMouse = ClientUiScale.toLayoutMouse(mouseX, mouseY, width, height);
        mouseX = layoutMouse[0];
        mouseY = layoutMouse[1];

        int[] panel = panelBounds();
        int panelX = panel[0];
        int panelY = panel[1];
        int panelW = panel[2];
        int panelH = panel[3];

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

    private void handleModsClick(int panelX, int contentY, int panelW, int clipH, int mouseX, int mouseY) {
        boolean settingsOpen = selectedModule != null;
        int settingsW = settingsOpen ? SETTINGS_W : 0;
        int gridX = panelX + SIDEBAR_W;
        int gridW = panelW - SIDEBAR_W - settingsW - (settingsOpen ? 6 : 0);

        // Scrollbar drag
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
            int itemY = contentY + 18;
            int padX = 6;
            int itemW = SIDEBAR_W - padX * 2;
            // All
            if (mouseX >= panelX + padX && mouseX <= panelX + padX + itemW
                    && mouseY >= itemY && mouseY <= itemY + NAV_ITEM_H) {
                selectedCategory = null;
                selectedModule = null;
                scrollOffset = 0;
                return;
            }
            itemY += NAV_ITEM_H + NAV_GAP;
            for (ModuleCategory category : ModuleCategory.values()) {
                if (itemY + NAV_ITEM_H > contentY + clipH) break;
                if (mouseX >= panelX + padX && mouseX <= panelX + padX + itemW
                        && mouseY >= itemY && mouseY <= itemY + NAV_ITEM_H) {
                    selectedCategory = category;
                    selectedModule = null;
                    scrollOffset = 0;
                    return;
                }
                itemY += NAV_ITEM_H + NAV_GAP;
            }
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
                    int toggleY = cardY + cardH - TOGGLE_H - 4;
                    if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                            && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_H) {
                        module.toggle();
                        OnyxClient.getConfigManager().saveModule(module);
                    } else {
                        selectedModule = module;
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

        if (settingsOpen) {
            handleModuleSettingsClick(panelX + panelW - SETTINGS_W - 6, contentY + 6, SETTINGS_W, clipH - 12, mouseX, mouseY);
        }
    }

    private void handleModuleSettingsClick(int x, int y, int w, int h, int mouseX, int mouseY) {
        if (selectedModule == null) return;
        int settingY = y + 34;
        for (Setting<?> setting : selectedModule.getSettings()) {
            if (settingY > y + h - 16) break;
            if (setting instanceof BooleanSetting) {
                int tx = x + w - TOGGLE_W - 10;
                if (mouseX >= tx && mouseX <= tx + TOGGLE_W && mouseY >= settingY - 1 && mouseY <= settingY + TOGGLE_H) {
                    BooleanSetting bool = (BooleanSetting) setting;
                    bool.setValue(!bool.getValue());
                    OnyxClient.getConfigManager().saveModule(selectedModule);
                }
            } else if (setting instanceof NumberSetting) {
                NumberSetting num = (NumberSetting) setting;
                if (mouseX >= x + 8 && mouseX <= x + w - 8 && mouseY >= settingY + 6 && mouseY <= settingY + 16) {
                    double pct = (mouseX - x - 8) / (double) (w - 16);
                    pct = Math.max(0, Math.min(1, pct));
                    double value = num.getMin() + pct * (num.getMax() - num.getMin());
                    value = Math.round(value / num.getIncrement()) * num.getIncrement();
                    num.setValue(value);
                    OnyxClient.getConfigManager().saveModule(selectedModule);
                    draggingSlider = num;
                }
                settingY += 10;
            } else if (setting instanceof ModeSetting) {
                if (mouseX >= x && mouseX <= x + w && mouseY >= settingY && mouseY <= settingY + 12) {
                    ((ModeSetting) setting).cycle();
                    OnyxClient.getConfigManager().saveModule(selectedModule);
                }
            }
            settingY += 18;
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
            int itemY = contentY + 18 - settingsNavScroll;
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
            if (mouseX >= x + w - 100 && mouseX <= x + w - 70 && mouseY >= listY && mouseY <= listY + 18) {
                if (OnyxClient.getConfigManager().loadProfile(name)) {
                    flash("Loaded: " + name);
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
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            draggingSlider = null;
            scrollDrag = ScrollDrag.NONE;
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
