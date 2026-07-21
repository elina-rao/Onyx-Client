package com.onyxclient.gui;

import com.onyxclient.skin.MojangSkinApi;
import com.onyxclient.skin.SkinLibrary;
import com.onyxclient.skin.SkinOverrideManager;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.OnyxFont;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * In-game Skin Changer — shared library with the Onyx Launcher.
 */
public class SkinChangerGui extends GuiScreen {

    private final GuiScreen parent;
    private GuiTextField copyField;
    private GuiButton applyBtn;
    private GuiButton localBtn;
    private GuiButton classicBtn;
    private GuiButton slimBtn;
    private String status = "";
    private boolean statusError;
    private boolean busy;
    private SkinLibrary.Index index;
    private int selectedIndex;
    private int scroll;

    public SkinChangerGui(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        index = SkinLibrary.load();
        selectedIndex = 0;
        if (index.activeId != null) {
            for (int i = 0; i < index.skins.size(); i++) {
                if (index.activeId.equals(index.skins.get(i).id)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        int cx = width / 2;
        int bottom = height - 36;
        copyField = new GuiTextField(1, fontRendererObj, cx - 140, bottom - 44, 160, 18);
        copyField.setMaxStringLength(16);
        buttonList.add(new GuiButton(10, cx + 28, bottom - 46, 70, 20, "Copy"));
        applyBtn = new GuiButton(11, cx - 140, bottom - 22, 110, 20, "Apply to account");
        localBtn = new GuiButton(12, cx - 24, bottom - 22, 90, 20, "Use locally");
        buttonList.add(applyBtn);
        buttonList.add(localBtn);
        buttonList.add(new GuiButton(13, cx + 72, bottom - 22, 68, 20, "Back"));
        classicBtn = new GuiButton(20, cx + 90, 48, 60, 18, "Classic");
        slimBtn = new GuiButton(21, cx + 154, 48, 50, 18, "Slim");
        buttonList.add(classicBtn);
        buttonList.add(slimBtn);
        buttonList.add(new GuiButton(14, cx + 90, 70, 114, 18, "Delete selected"));
        refreshButtons();
        SkinOverrideManager.INSTANCE.applyActiveFromLibrary();
    }

    private void refreshButtons() {
        boolean ms = MojangSkinApi.isMicrosoftSession();
        applyBtn.enabled = ms && !busy && selected() != null;
        localBtn.enabled = !busy && selected() != null;
        boolean slim = "slim".equals(index.model);
        classicBtn.displayString = slim ? "Classic" : "[Classic]";
        slimBtn.displayString = slim ? "[Slim]" : "Slim";
    }

    private SkinLibrary.Entry selected() {
        if (index == null || index.skins.isEmpty()) return null;
        if (selectedIndex < 0 || selectedIndex >= index.skins.size()) return null;
        return index.skins.get(selectedIndex);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (busy) return;
        if (button.id == 13) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == 20) {
            SkinLibrary.setModel("classic");
            index = SkinLibrary.load();
            refreshButtons();
            return;
        }
        if (button.id == 21) {
            SkinLibrary.setModel("slim");
            index = SkinLibrary.load();
            refreshButtons();
            return;
        }
        if (button.id == 14) {
            SkinLibrary.Entry e = selected();
            if (e == null) return;
            SkinLibrary.delete(e.id);
            index = SkinLibrary.load();
            selectedIndex = Math.min(selectedIndex, Math.max(0, index.skins.size() - 1));
            SkinOverrideManager.INSTANCE.applyActiveFromLibrary();
            status = "Deleted";
            statusError = false;
            refreshButtons();
            return;
        }
        if (button.id == 10) {
            final String ign = copyField.getText();
            busy = true;
            status = "Copying…";
            statusError = false;
            refreshButtons();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String err = MojangSkinApi.copyFromUsername(ign);
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            busy = false;
                            index = SkinLibrary.load();
                            selectedIndex = 0;
                            if (err != null) {
                                status = err;
                                statusError = true;
                            } else {
                                status = "Copied " + ign;
                                statusError = false;
                            }
                            refreshButtons();
                        }
                    });
                }
            }, "OnyxSkinCopy").start();
            return;
        }
        if (button.id == 12) {
            SkinLibrary.Entry e = selected();
            if (e == null) return;
            SkinLibrary.setActive(e.id);
            index = SkinLibrary.load();
            if (SkinOverrideManager.INSTANCE.applyEntry(e)) {
                status = "Local override active (F5 to preview)";
                statusError = false;
            } else {
                status = "Could not load skin file";
                statusError = true;
            }
            refreshButtons();
            return;
        }
        if (button.id == 11) {
            final SkinLibrary.Entry e = selected();
            if (e == null) return;
            SkinLibrary.setActive(e.id);
            final File file = e.resolveFile(SkinLibrary.resolveSkinsDir());
            final String variant = e.model;
            busy = true;
            status = "Applying to account…";
            statusError = false;
            refreshButtons();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String err = MojangSkinApi.applySkinFile(file, variant);
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            busy = false;
                            if (err != null) {
                                status = err;
                                statusError = true;
                            } else {
                                SkinOverrideManager.INSTANCE.applyEntry(e);
                                status = "Applied to Minecraft account";
                                statusError = false;
                            }
                            refreshButtons();
                        }
                    });
                }
            }, "OnyxSkinApply").start();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (copyField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        copyField.mouseClicked(mouseX, mouseY, mouseButton);
        int listX = width / 2 - 200;
        int listY = 48;
        int listH = height - 140;
        if (mouseX >= listX && mouseX <= listX + 160 && mouseY >= listY && mouseY <= listY + listH) {
            int row = (mouseY - listY) / 14 + scroll;
            if (row >= 0 && row < index.skins.size()) {
                selectedIndex = row;
                refreshButtons();
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = org.lwjgl.input.Mouse.getEventDWheel();
        if (d != 0) {
            if (d > 0) scroll = Math.max(0, scroll - 1);
            else scroll = Math.min(Math.max(0, index.skins.size() - 8), scroll + 1);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int cx = width / 2;
        OnyxFont.MEDIUM.drawCenteredString("Skin Changer", cx, 18, Colors.ACCENT_PRIMARY);
        OnyxFont.SMALL.drawString("Library (shared with Onyx Launcher)", cx - 200, 36, Colors.TEXT_MUTED);

        int listX = cx - 200;
        int listY = 48;
        int listW = 160;
        int listH = height - 140;
        RenderUtils.drawRoundedRect(listX - 4, listY - 4, listW + 8, listH + 8, 6, 0xE0110820);
        List<SkinLibrary.Entry> skins = index.skins;
        if (skins.isEmpty()) {
            OnyxFont.SMALL.drawString("No skins yet.", listX + 8, listY + 8, Colors.TEXT_MUTED);
            OnyxFont.SMALL.drawString("Copy a username or use", listX + 8, listY + 22, Colors.TEXT_MUTED);
            OnyxFont.SMALL.drawString("the launcher Upload.", listX + 8, listY + 36, Colors.TEXT_MUTED);
        } else {
            int y = listY;
            for (int i = scroll; i < skins.size() && y < listY + listH - 12; i++) {
                SkinLibrary.Entry e = skins.get(i);
                int color = i == selectedIndex ? Colors.ACCENT_BRIGHT : Colors.TEXT_PRIMARY;
                String label = e.name;
                if (e.id != null && e.id.equals(index.activeId)) {
                    label = "> " + label;
                }
                OnyxFont.SMALL.drawString(OnyxFont.SMALL.trimToWidth(label, listW - 12), listX + 6, y, color);
                y += 14;
            }
        }

        SkinLibrary.Entry sel = selected();
        OnyxFont.SMALL.drawString(sel == null ? "No selection" : sel.name, cx + 90, 36, Colors.TEXT_PRIMARY);
        if (!MojangSkinApi.isMicrosoftSession()) {
            OnyxFont.SMALL.drawString("Guest: local only — MS to Apply", cx + 90, 92, Colors.TEXT_MUTED);
        } else {
            OnyxFont.SMALL.drawString("Microsoft session ready", cx + 90, 92, Colors.SUCCESS);
        }

        if (status != null && !status.isEmpty()) {
            OnyxFont.MEDIUM.drawCenteredString(status, cx, height - 58, statusError ? Colors.DANGER : Colors.TEXT_MUTED);
        }

        copyField.drawTextBox();
        GlStateManager.color(1f, 1f, 1f, 1f);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
