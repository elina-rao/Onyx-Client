package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.HudLayoutTokens;
import com.onyxclient.utils.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ResourceOverlayModule extends HudModule {

    private final ModeSetting layout;
    private final BooleanSetting showIron;
    private final BooleanSetting showGold;
    private final BooleanSetting showEmerald;
    private final BooleanSetting showDiamond;
    private final BooleanSetting includeEnderChest;
    private final BooleanSetting textShadow;

    public ResourceOverlayModule() {
        super("ResourceOverlay", "Iron/gold/emerald/diamond counts", true);
        layout = addSetting(new ModeSetting("Layout", "Horizontal", "Horizontal", "Vertical", "Grid"));
        showIron = addSetting(new BooleanSetting("Show Iron", true));
        showGold = addSetting(new BooleanSetting("Show Gold", true));
        showEmerald = addSetting(new BooleanSetting("Show Emerald", true));
        showDiamond = addSetting(new BooleanSetting("Show Diamond", true));
        includeEnderChest = addSetting(new BooleanSetting("Include Ender Chest", true));
        textShadow = addSetting(new BooleanSetting("Text Shadow", true));
        setUseScaledBounds(true);
        setHudSize(120, 14);
        setHudPosition(2, 80);
        tryEnablePremiumDefaults();
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        HypixelBedwarsModule hub = HypixelBedwarsModule.INSTANCE;
        if (hub != null && hub.isEnabled() && hub.isInBedwars() && !hub.showResourceCounter()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        int iron = count(Items.iron_ingot);
        int gold = count(Items.gold_ingot);
        int emerald = count(Items.emerald);
        int diamond = count(Items.diamond);
        if (!usePremiumRenderer()) {
            renderLegacy(mc, iron, gold, emerald, diamond);
            return;
        }
        renderPremium(mc, iron, gold, emerald, diamond);
    }

    private void renderLegacy(Minecraft mc, int iron, int gold, int emerald, int diamond) {
        boolean vertical = "Vertical".equals(layout.getValue()) || "Grid".equals(layout.getValue());
        int x = hudX;
        int y = hudY;
        int maxW = 0;
        int lines = 0;

        if (showIron.getValue()) {
            String t = "Iron: " + iron;
            drawText(mc, t, x, y, 0xFFD0D0D0);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(t));
            if (vertical) {
                y += 10;
                lines++;
            } else {
                x += mc.fontRendererObj.getStringWidth(t) + 8;
            }
        }
        if (showGold.getValue()) {
            String t = "Gold: " + gold;
            drawText(mc, t, x, y, 0xFFFFD700);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(t));
            if (vertical) {
                y += 10;
                lines++;
            } else {
                x += mc.fontRendererObj.getStringWidth(t) + 8;
            }
        }
        if (showEmerald.getValue()) {
            String t = "Em: " + emerald;
            drawText(mc, t, x, y, 0xFF50C878);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(t));
            if (vertical) {
                y += 10;
                lines++;
            } else {
                x += mc.fontRendererObj.getStringWidth(t) + 8;
            }
        }
        if (showDiamond.getValue()) {
            String t = "Dia: " + diamond;
            drawText(mc, t, x, y, 0xFF5B9BD5);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(t));
            if (vertical) {
                y += 10;
                lines++;
            } else {
                x += mc.fontRendererObj.getStringWidth(t) + 8;
            }
        }

        if (vertical) {
            setHudSize(maxW + 4, Math.max(12, lines * 10));
        } else {
            setHudSize(Math.max(40, x - hudX), 12);
        }
    }

    private void renderPremium(Minecraft mc, int iron, int gold, int emerald, int diamond) {
        beginHudScale();
        boolean vertical = "Vertical".equals(layout.getValue()) || "Grid".equals(layout.getValue());
        int lineH = hudLineHeight(mc);
        int rowGap = HudLayoutTokens.CARD_ROW_GAP;

        int x = hudX + HudLayoutTokens.CARD_PADDING_X;
        int y = hudY + HudLayoutTokens.CARD_PADDING_Y;
        int startX = x;
        int maxW = 0;
        int lines = 0;

        if (showIron.getValue()) {
            String t = "Iron: " + iron;
            drawHudText(mc, t, x, y, 0xFFD0D0D0);
            maxW = Math.max(maxW, measureHudText(mc, t));
            if (vertical) {
                y += lineH + rowGap;
                lines++;
            } else {
                x += measureHudText(mc, t) + 10;
            }
        }
        if (showGold.getValue()) {
            String t = "Gold: " + gold;
            drawHudText(mc, t, x, y, 0xFFFFD700);
            maxW = Math.max(maxW, measureHudText(mc, t));
            if (vertical) {
                y += lineH + rowGap;
                lines++;
            } else {
                x += measureHudText(mc, t) + 10;
            }
        }
        if (showEmerald.getValue()) {
            String t = "Em: " + emerald;
            drawHudText(mc, t, x, y, 0xFF50C878);
            maxW = Math.max(maxW, measureHudText(mc, t));
            if (vertical) {
                y += lineH + rowGap;
                lines++;
            } else {
                x += measureHudText(mc, t) + 10;
            }
        }
        if (showDiamond.getValue()) {
            String t = "Dia: " + diamond;
            drawHudText(mc, t, x, y, 0xFF5B9BD5);
            maxW = Math.max(maxW, measureHudText(mc, t));
            if (vertical) {
                y += lineH + rowGap;
                lines++;
            } else {
                x += measureHudText(mc, t) + 10;
            }
        }

        int contentW;
        int contentH;
        if (vertical) {
            contentW = maxW;
            contentH = Math.max(lineH, lines * (lineH + rowGap));
        } else {
            contentW = Math.max(40, x - startX);
            contentH = lineH;
        }
        int cardW = Math.max(HudLayoutTokens.CARD_MIN_WIDTH, contentW + HudLayoutTokens.CARD_PADDING_X * 2);
        int cardH = contentH + HudLayoutTokens.CARD_PADDING_Y * 2;
        if (usePremiumCard()) {
            drawHudCard(hudX, hudY, cardW, cardH);
            // redraw content above card shadow/border
            renderPremiumText(mc, iron, gold, emerald, diamond, vertical, lineH, rowGap);
        }
        setHudSize(cardW, cardH);
        endHudScale();
    }

    private void renderPremiumText(Minecraft mc, int iron, int gold, int emerald, int diamond, boolean vertical, int lineH, int rowGap) {
        int x = hudX + HudLayoutTokens.CARD_PADDING_X;
        int y = hudY + HudLayoutTokens.CARD_PADDING_Y;
        if (showIron.getValue()) {
            String t = "Iron: " + iron;
            drawHudText(mc, t, x, y, 0xFFD0D0D0);
            if (vertical) {
                y += lineH + rowGap;
            } else {
                x += measureHudText(mc, t) + 10;
            }
        }
        if (showGold.getValue()) {
            String t = "Gold: " + gold;
            drawHudText(mc, t, x, y, 0xFFFFD700);
            if (vertical) {
                y += lineH + rowGap;
            } else {
                x += measureHudText(mc, t) + 10;
            }
        }
        if (showEmerald.getValue()) {
            String t = "Em: " + emerald;
            drawHudText(mc, t, x, y, 0xFF50C878);
            if (vertical) {
                y += lineH + rowGap;
            } else {
                x += measureHudText(mc, t) + 10;
            }
        }
        if (showDiamond.getValue()) {
            String t = "Dia: " + diamond;
            drawHudText(mc, t, x, y, 0xFF5B9BD5);
        }
    }

    private int count(Item item) {
        Minecraft mc = Minecraft.getMinecraft();
        int total = 0;
        for (ItemStack stack : mc.thePlayer.inventory.mainInventory) {
            if (stack != null && stack.getItem() == item) {
                total += stack.stackSize;
            }
        }
        if (!includeEnderChest.getValue()) {
            return total;
        }
        // Ender chest contents are not always synced client-side; count when available
        try {
            net.minecraft.inventory.InventoryEnderChest ender = mc.thePlayer.getInventoryEnderChest();
            if (ender != null) {
                for (int i = 0; i < ender.getSizeInventory(); i++) {
                    ItemStack stack = ender.getStackInSlot(i);
                    if (stack != null && stack.getItem() == item) {
                        total += stack.stackSize;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return total;
    }

    private void drawText(Minecraft mc, String text, int x, int y, int color) {
        if (textShadow.getValue()) {
            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        } else {
            mc.fontRendererObj.drawString(text, x, y, color);
        }
    }

    private void tryEnablePremiumDefaults() {
        try {
            for (com.onyxclient.modules.settings.Setting<?> setting : getSettings()) {
                if ("Premium Renderer".equals(setting.getName()) || "Premium Card".equals(setting.getName())) {
                    @SuppressWarnings("unchecked")
                    com.onyxclient.modules.settings.Setting<Boolean> b =
                            (com.onyxclient.modules.settings.Setting<Boolean>) setting;
                    b.setValue(true);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
