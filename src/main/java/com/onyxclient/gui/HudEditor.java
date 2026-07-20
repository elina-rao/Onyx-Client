package com.onyxclient.gui;

import com.onyxclient.OnyxClient;
import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.utils.ClientUiScale;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;

/**
 * BLC-style fullscreen HUD editor for Forge 1.8.9 (GuiScreen / initGui only).
 * Top chrome respects Menus → UI Scale; HUD element positions stay screen-space.
 */
public class HudEditor extends GuiScreen {

    private final GuiScreen parent;
    private HudModule draggingModule;
    private boolean snapToGrid = true;
    private boolean previewMode;

    public HudEditor() {
        this(null);
    }

    public HudEditor(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        float s = ClientUiScale.factor();
        int by = Math.max(6, Math.round(8 * s));
        int bh = Math.max(16, Math.round(20 * s));
        int bw = Math.max(64, Math.round(80 * s));
        int gap = Math.max(6, Math.round(10 * s));
        int x = width - Math.round(14 * s) - bw;
        buttonList.add(new GuiButton(0, x, by, bw, bh, "Done"));
        x -= bw + gap;
        buttonList.add(new GuiButton(1, x, by, bw, bh, "Reset"));
        int wide = Math.max(72, Math.round(100 * s));
        x -= wide + gap;
        buttonList.add(new GuiButton(2, x, by, wide, bh, snapToGrid ? "Snap: On" : "Snap: Off"));
        x -= wide + gap;
        buttonList.add(new GuiButton(3, x, by, wide, bh, previewMode ? "Edit Mode" : "Preview"));
    }

    private int topBarH() {
        return Math.max(28, Math.round(36 * ClientUiScale.factor()));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float s = ClientUiScale.factor();
        int barH = topBarH();

        // Dim overlay — game stays visible underneath
        RenderUtils.drawRect(0, 0, width, height, Colors.withAlpha(0x000000, 100));

        // Top bar (scaled chrome)
        RenderUtils.drawRect(0, 0, width, barH, Colors.withAlpha(Colors.BG_DEEP, 230));
        GlStateManager.pushMatrix();
        GlStateManager.translate(12, barH / 2.0F - 4 * s, 0);
        GlStateManager.scale(s, s, 1.0F);
        fontRendererObj.drawString("ONYX", 0, 0, Colors.TEXT_PRIMARY);
        fontRendererObj.drawString("HUD Editor", fontRendererObj.getStringWidth("ONYX") + 6, 0, Colors.ACCENT_BRIGHT);
        GlStateManager.popMatrix();

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
                fontRendererObj.drawStringWithShadow(module.getName(), hud.getHudX(), Math.max(0, hud.getHudY() - 10),
                        Colors.TEXT_PRIMARY);
            }
        }

        if (enabledCount == 0) {
            String tip = "Enable HUD modules in Mods first, then drag them here.";
            int tw = fontRendererObj.getStringWidth(tip);
            fontRendererObj.drawString(tip, (width - tw) / 2, height / 2 - 4, Colors.TEXT_MUTED);
        } else if (!previewMode) {
            fontRendererObj.drawString("Drag HUD elements to reposition", 12, height - 16, Colors.TEXT_MUTED);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && !previewMode && mouseY > topBarH()) {
            draggingModule = null;
            List<com.onyxclient.modules.Module> modules = OnyxClient.getModuleManager().getModules();
            for (com.onyxclient.modules.Module module : modules) {
                if (module instanceof HudModule && module.isEnabled()) {
                    HudModule hud = (HudModule) module;
                    if (hud.isMouseOver(mouseX, mouseY)) {
                        draggingModule = hud;
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
        if (draggingModule != null && clickedMouseButton == 0) {
            draggingModule.drag(mouseX, mouseY, snapToGrid);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (draggingModule != null) {
            draggingModule.stopDrag();
            draggingModule = null;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            closeEditor();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            saveAll();
            closeEditor();
        } else if (button.id == 1) {
            int index = 0;
            for (com.onyxclient.modules.Module module : OnyxClient.getModuleManager().getModules()) {
                if (module instanceof HudModule) {
                    HudModule hud = (HudModule) module;
                    hud.setHudPosition(4, 40 + index * 16);
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
