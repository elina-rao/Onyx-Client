package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/**
 * Hotkeys to jump to shop tabs when Bedwars shop GUI is open. GUI-only clicks.
 */
public class ShopQuickNavModule extends Module {

    private final BooleanSetting enableArmor;
    private final BooleanSetting enableWeapons;
    private final BooleanSetting enableBlocks;
    private final BooleanSetting enableTools;

    public ShopQuickNavModule() {
        super("ShopQuickNav", "Hotkeys for Bedwars shop tabs", ModuleCategory.BEDWARS);
        enableArmor = addSetting(new BooleanSetting("Armor Tab (1)", true));
        enableWeapons = addSetting(new BooleanSetting("Weapons Tab (2)", true));
        enableBlocks = addSetting(new BooleanSetting("Blocks Tab (3)", true));
        enableTools = addSetting(new BooleanSetting("Tools Tab (4)", true));
    }

    @SubscribeEvent
    public void onKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!isEnabled()) {
            return;
        }
        if (!Keyboard.getEventKeyState()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiChest)) {
            return;
        }
        GuiChest chest = (GuiChest) mc.currentScreen;
        int key = Keyboard.getEventKey();
        int slotIndex = -1;
        if (key == Keyboard.KEY_1 && enableArmor.getValue()) {
            slotIndex = 1;
        } else if (key == Keyboard.KEY_2 && enableWeapons.getValue()) {
            slotIndex = 2;
        } else if (key == Keyboard.KEY_3 && enableBlocks.getValue()) {
            slotIndex = 3;
        } else if (key == Keyboard.KEY_4 && enableTools.getValue()) {
            slotIndex = 4;
        }
        if (slotIndex < 0) {
            return;
        }
        // Click category slots in top row of shop (common Hypixel layout)
        tryClickSlot(chest, slotIndex);
    }

    private void tryClickSlot(GuiContainer container, int index) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.playerController == null || container.inventorySlots == null) {
            return;
        }
        if (index >= container.inventorySlots.inventorySlots.size()) {
            return;
        }
        Slot slot = container.inventorySlots.getSlot(index);
        if (slot == null) {
            return;
        }
        mc.playerController.windowClick(
                container.inventorySlots.windowId,
                slot.slotNumber,
                0,
                0,
                mc.thePlayer
        );
    }
}
