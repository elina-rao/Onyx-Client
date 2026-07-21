package com.onyxclient.gui;

import com.onyxclient.OnyxClient;
import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.utils.ClientUiScale;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.OnyxFont;
import com.onyxclient.utils.HudMotion;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;

/**
 * Fullscreen HUD editor — drag, snap, corner resize, reset defaults.
 */
public class HudEditor extends GuiScreen {

    private static final int HANDLE = 6;
    private static final int REVEAL_ZONE = 18;

    private final GuiScreen parent;
    private HudModule draggingModule;
    private HudModule resizingModule;
    private HudModule selectedModule;
    private int resizeCorner; // 0 TL, 1 TR, 2 BL, 3 BR
    private boolean snapToGrid = true;
    private boolean previewMode;
    private float toolbarAnim;
    private boolean toolbarPinned;
    private boolean showCenterGuides;

    public HudEditor() {
        this(null);
    }

    public HudEditor(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        layoutToolbar(0);
    }

    private int topBarH() {
        return Math.max(28, Math.round(36 * ClientUiScale.factor()));
    }

    private int effectiveBarH() {
        return Math.max(0, Math.round(topBarH() * toolbarAnim));
    }

    private void layoutToolbar(int offsetY) {
        float s = ClientUiScale.factor();
        int by = offsetY + Math.max(6, Math.round(8 * s));
        int bh = Math.max(16, Math.round(20 * s));
        int bw = Math.max(64, Math.round(80 * s));
        int gap = Math.max(6, Math.round(10 * s));
        int x = width - Math.round(14 * s) - bw;

        if (buttonList.size() < 4) {
            buttonList.add(new GuiButton(0, x, by, bw, bh, "Done"));
            x -= bw + gap;
            buttonList.add(new GuiButton(1, x, by, bw, bh, "Reset"));
            int wide = Math.max(72, Math.round(100 * s));
            x -= wide + gap;
            buttonList.add(new GuiButton(2, x, by, wide, bh, snapToGrid ? "Snap: On" : "Snap: Off"));
            x -= wide + gap;
            buttonList.add(new GuiButton(3, x, by, wide, bh, previewMode ? "Edit Mode" : "Preview"));
        } else {
            GuiButton done = (GuiButton) buttonList.get(0);
            GuiButton reset = (GuiButton) buttonList.get(1);
            GuiButton snap = (GuiButton) buttonList.get(2);
            GuiButton preview = (GuiButton) buttonList.get(3);

            done.xPosition = x;
            done.yPosition = by;
            done.width = bw;
            done.height = bh;
            x -= bw + gap;

            reset.xPosition = x;
            reset.yPosition = by;
            reset.width = bw;
            reset.height = bh;
            x -= bw + gap;

            int wide = Math.max(72, Math.round(100 * s));
            snap.xPosition = x;
            snap.yPosition = by;
            snap.width = wide;
            snap.height = bh;
            x -= wide + gap;

            preview.xPosition = x;
            preview.yPosition = by;
            preview.width = wide;
            preview.height = bh;
        }

        boolean showButtons = toolbarAnim >= 0.15F;
        for (Object obj : buttonList) {
            GuiButton btn = (GuiButton) obj;
            btn.visible = showButtons;
            btn.enabled = showButtons;
        }
    }

    private void updateToolbarAnim(int mouseX, int mouseY) {
        boolean buttonHovered = false;
        for (Object obj : buttonList) {
            GuiButton btn = (GuiButton) obj;
            if (btn.visible && mouseX >= btn.xPosition && mouseX <= btn.xPosition + btn.width
                    && mouseY >= btn.yPosition && mouseY <= btn.yPosition + btn.height) {
                buttonHovered = true;
                break;
            }
        }

        int barH = topBarH();
        int offsetY = (int) ((1.0F - toolbarAnim) * -barH);
        boolean inBar = mouseY >= offsetY && mouseY <= offsetY + barH && mouseX >= 0 && mouseX <= width;
        boolean dragging = draggingModule != null || resizingModule != null;

        if (previewMode) {
            toolbarPinned = false;
        }

        float target;
        if (toolbarPinned || buttonHovered || inBar || mouseY < REVEAL_ZONE || dragging) {
            target = 1.0F;
        } else {
            target = 0.0F;
        }

        toolbarAnim = HudMotion.approach(toolbarAnim, target, 0.18F);
        if (toolbarAnim < 0.01F) {
            toolbarAnim = 0.0F;
        } else if (toolbarAnim > 0.99F) {
            toolbarAnim = 1.0F;
        }

        if (!toolbarPinned && !buttonHovered && !inBar && mouseY >= REVEAL_ZONE && !dragging) {
            toolbarPinned = false;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateToolbarAnim(mouseX, mouseY);

        float s = ClientUiScale.factor();
        int barH = topBarH();
        int offsetY = (int) ((1.0F - toolbarAnim) * -barH);
        int editTop = effectiveBarH();

        int dimAlpha = toolbarAnim < 0.05F ? 80 : 100;
        RenderUtils.drawRect(0, 0, width, height, Colors.withAlpha(0x000000, dimAlpha));

        if (toolbarAnim < 0.05F) {
            RenderUtils.drawRect(0, 0, width, 4, Colors.withAlpha(Colors.ACCENT_PRIMARY, 100));
            if (toolbarAnim < 0.02F) {
                String hint = "HUD Editor";
                int tw = OnyxFont.SMALL.getStringWidth(hint);
                OnyxFont.SMALL.drawString(hint, (width - tw) / 2, 6, Colors.withAlpha(Colors.TEXT_MUTED, 80));
            }
        }

        layoutToolbar(offsetY);

        if (toolbarAnim > 0.02F) {
            int barAlpha = (int) (230 * toolbarAnim);
            RenderUtils.drawRect(0, offsetY, width, barH, Colors.withAlpha(Colors.BG_DEEP, barAlpha));
            GlStateManager.pushMatrix();
            GlStateManager.translate(12, offsetY + barH / 2.0F - 4 * s, 0);
            GlStateManager.scale(s, s, 1.0F);
            OnyxFont.MEDIUM.drawString("ONYX", 0, 0, Colors.TEXT_PRIMARY);
            OnyxFont.MEDIUM.drawString("HUD Editor", OnyxFont.MEDIUM.getStringWidth("ONYX") + 6, 0, Colors.ACCENT_BRIGHT);
            GlStateManager.popMatrix();
        }

        if (snapToGrid && !previewMode) {
            for (int gx = 0; gx < width; gx += 16) {
                RenderUtils.drawRect(gx, editTop, 1, height - editTop, Colors.withAlpha(Colors.ACCENT_PRIMARY, 18));
            }
            for (int gy = editTop; gy < height; gy += 16) {
                RenderUtils.drawRect(0, gy, width, 1, Colors.withAlpha(Colors.ACCENT_PRIMARY, 18));
            }
        }

        // Lunar-style center magnetic guides while dragging/resizing
        if (showCenterGuides && !previewMode) {
            int cx = width / 2;
            int cy = height / 2;
            RenderUtils.drawRect(cx, editTop, 1, height - editTop, Colors.withAlpha(Colors.ACCENT_BRIGHT, 90));
            RenderUtils.drawRect(0, cy, width, 1, Colors.withAlpha(Colors.ACCENT_BRIGHT, 90));
        }

        int enabledCount = 0;
        List<com.onyxclient.modules.Module> modules = OnyxClient.getModuleManager().getModules();
        for (com.onyxclient.modules.Module module : modules) {
            if (!(module instanceof HudModule) || !module.isEnabled()) {
                continue;
            }
            enabledCount++;
            HudModule hud = (HudModule) module;
            if (!previewMode) {
                RenderUtils.drawDashedRect(hud.getHudX(), hud.getHudY(), hud.getHudWidth(), hud.getHudHeight(),
                        Colors.ACCENT_PRIMARY, 4);
                OnyxFont.SMALL.drawStringWithShadow(module.getName(),
                        hud.getHudX() + 2, hud.getHudY() + 2, Colors.TEXT_PRIMARY);
                drawHandles(hud);
            }
        }

        if (enabledCount == 0) {
            String tip = "Enable HUD modules in Mods first, then drag them here.";
            int tw = OnyxFont.SMALL.getStringWidth(tip);
            OnyxFont.SMALL.drawString(tip, (width - tw) / 2, height / 2 - 4, Colors.TEXT_MUTED);
        } else if (!previewMode && toolbarAnim > 0.5F) {
            OnyxFont.SMALL.drawString("Drag · corners resize · arrows nudge (Shift=10) · Snap · center guides",
                    12, height - 16, Colors.TEXT_MUTED);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawHandles(HudModule hud) {
        int[][] corners = handleCorners(hud);
        for (int i = 0; i < 4; i++) {
            RenderUtils.drawRect(corners[i][0], corners[i][1], HANDLE, HANDLE, Colors.TEXT_PRIMARY);
        }
    }

    private int[][] handleCorners(HudModule hud) {
        int x = hud.getHudX();
        int y = hud.getHudY();
        int w = hud.getHudWidth();
        int h = hud.getHudHeight();
        return new int[][]{
                {x - HANDLE / 2, y - HANDLE / 2},
                {x + w - HANDLE / 2, y - HANDLE / 2},
                {x - HANDLE / 2, y + h - HANDLE / 2},
                {x + w - HANDLE / 2, y + h - HANDLE / 2}
        };
    }

    private int hitHandle(HudModule hud, int mouseX, int mouseY) {
        int[][] corners = handleCorners(hud);
        for (int i = 0; i < 4; i++) {
            if (mouseX >= corners[i][0] && mouseX <= corners[i][0] + HANDLE
                    && mouseY >= corners[i][1] && mouseY <= corners[i][1] + HANDLE) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int barH = topBarH();
        int offsetY = (int) ((1.0F - toolbarAnim) * -barH);
        if (mouseY >= offsetY && mouseY <= offsetY + barH) {
            toolbarPinned = true;
        }

        if (mouseButton == 0 && !previewMode && mouseY > effectiveBarH()) {
            draggingModule = null;
            resizingModule = null;
            List<com.onyxclient.modules.Module> modules = OnyxClient.getModuleManager().getModules();
            for (com.onyxclient.modules.Module module : modules) {
                if (!(module instanceof HudModule) || !module.isEnabled()) {
                    continue;
                }
                HudModule hud = (HudModule) module;
                int handle = hitHandle(hud, mouseX, mouseY);
                if (handle >= 0) {
                    resizingModule = hud;
                    selectedModule = hud;
                    resizeCorner = handle;
                    showCenterGuides = true;
                    return;
                }
            }
            for (com.onyxclient.modules.Module module : modules) {
                if (module instanceof HudModule && module.isEnabled()) {
                    HudModule hud = (HudModule) module;
                    if (hud.isMouseOver(mouseX, mouseY)) {
                        draggingModule = hud;
                        selectedModule = hud;
                        showCenterGuides = true;
                        hud.startDrag(mouseX, mouseY);
                        break;
                    }
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (resizingModule != null && clickedMouseButton == 0) {
            resizeHud(resizingModule, mouseX, mouseY, resizeCorner);
            snapToCenter(resizingModule);
        } else if (draggingModule != null && clickedMouseButton == 0) {
            draggingModule.drag(mouseX, mouseY, snapToGrid);
            snapToCenter(draggingModule);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    /** Soft magnetic snap to screen center (Lunar-style guides). */
    private void snapToCenter(HudModule hud) {
        int cx = width / 2;
        int cy = height / 2;
        int hx = hud.getHudX() + hud.getHudWidth() / 2;
        int hy = hud.getHudY() + hud.getHudHeight() / 2;
        int thresh = 6;
        int nx = hud.getHudX();
        int ny = hud.getHudY();
        boolean near = false;
        if (Math.abs(hx - cx) <= thresh) {
            nx = cx - hud.getHudWidth() / 2;
            near = true;
        }
        if (Math.abs(hy - cy) <= thresh) {
            ny = cy - hud.getHudHeight() / 2;
            near = true;
        }
        showCenterGuides = near || draggingModule != null || resizingModule != null;
        if (near) {
            hud.setHudPosition(nx, ny);
        }
    }

    private void resizeHud(HudModule hud, int mouseX, int mouseY, int corner) {
        int x = hud.getHudX();
        int y = hud.getHudY();
        int r = x + hud.getHudWidth();
        int b = y + hud.getHudHeight();
        if (corner == 0) { // TL
            x = mouseX;
            y = mouseY;
        } else if (corner == 1) { // TR
            r = mouseX;
            y = mouseY;
        } else if (corner == 2) { // BL
            x = mouseX;
            b = mouseY;
        } else { // BR
            r = mouseX;
            b = mouseY;
        }
        if (snapToGrid) {
            x = (x / 4) * 4;
            y = (y / 4) * 4;
            r = (r / 4) * 4;
            b = (b / 4) * 4;
        }
        int w = Math.max(20, r - x);
        int h = Math.max(10, b - y);
        ScaledResolution sr = new ScaledResolution(mc);
        x = Math.max(0, Math.min(x, sr.getScaledWidth() - w));
        y = Math.max(0, Math.min(y, sr.getScaledHeight() - h));
        hud.setHudPosition(x, y);
        hud.setHudSize(w, h);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (draggingModule != null) {
            draggingModule.stopDrag();
            draggingModule = null;
        }
        if (resizingModule != null) {
            OnyxClient.getConfigManager().saveHudPosition(resizingModule);
            resizingModule = null;
        }
        showCenterGuides = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            closeEditor();
            return;
        }
        if (!previewMode && selectedModule != null) {
            int step = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? 10 : 1;
            int x = selectedModule.getHudX();
            int y = selectedModule.getHudY();
            boolean moved = false;
            if (keyCode == Keyboard.KEY_LEFT) {
                x -= step;
                moved = true;
            } else if (keyCode == Keyboard.KEY_RIGHT) {
                x += step;
                moved = true;
            } else if (keyCode == Keyboard.KEY_UP) {
                y -= step;
                moved = true;
            } else if (keyCode == Keyboard.KEY_DOWN) {
                y += step;
                moved = true;
            }
            if (moved) {
                ScaledResolution sr = new ScaledResolution(mc);
                x = Math.max(0, Math.min(x, sr.getScaledWidth() - selectedModule.getHudWidth()));
                y = Math.max(0, Math.min(y, sr.getScaledHeight() - selectedModule.getHudHeight()));
                selectedModule.setHudPosition(x, y);
                OnyxClient.getConfigManager().saveHudPosition(selectedModule);
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        toolbarPinned = true;
        if (button.id == 0) {
            saveAll();
            closeEditor();
        } else if (button.id == 1) {
            int index = 0;
            ScaledResolution sr = new ScaledResolution(mc);
            for (com.onyxclient.modules.Module module : OnyxClient.getModuleManager().getModules()) {
                if (module instanceof HudModule) {
                    HudModule hud = (HudModule) module;
                    int x = 4;
                    int y = 40 + index * 18;
                    if (y + 12 > sr.getScaledHeight()) {
                        x = sr.getScaledWidth() / 2;
                        y = 40 + (index % 8) * 18;
                    }
                    hud.setHudPosition(x, y);
                    index++;
                }
            }
            saveAll();
        } else if (button.id == 2) {
            snapToGrid = !snapToGrid;
            button.displayString = snapToGrid ? "Snap: On" : "Snap: Off";
        } else if (button.id == 3) {
            previewMode = !previewMode;
            button.displayString = previewMode ? "Edit Mode" : "Preview";
            if (previewMode) {
                toolbarPinned = false;
            }
        }
    }

    private void saveAll() {
        for (com.onyxclient.modules.Module module : OnyxClient.getModuleManager().getModules()) {
            if (module instanceof HudModule) {
                OnyxClient.getConfigManager().saveHudPosition((HudModule) module);
            }
        }
    }

    private void closeEditor() {
        saveAll();
        mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
