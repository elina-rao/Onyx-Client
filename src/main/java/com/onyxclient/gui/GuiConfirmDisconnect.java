package com.onyxclient.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import com.onyxclient.utils.OnyxFont;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;

import java.io.IOException;

/**
 * Confirm before disconnecting when Smart Disconnect is enabled.
 */
public class GuiConfirmDisconnect extends GuiScreen {

    private final GuiScreen parent;

    public GuiConfirmDisconnect(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 100, height / 2 + 8, 98, 20, "Disconnect"));
        buttonList.add(new GuiButton(1, width / 2 + 2, height / 2 + 8, 98, 20, "Cancel"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == 0) {
            boolean sp = mc.isSingleplayer();
            mc.theWorld.sendQuittingDisconnectingPacket();
            mc.loadWorld((WorldClient) null);
            if (sp) {
                mc.displayGuiScreen(new GuiMainMenu());
            } else {
                mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        OnyxFont.MEDIUM.drawCenteredString("Disconnect from server?", width / 2, height / 2 - 20, 0xFFFFFF);
        OnyxFont.MEDIUM.drawCenteredString("Smart Disconnect is on", width / 2, height / 2 - 8, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
