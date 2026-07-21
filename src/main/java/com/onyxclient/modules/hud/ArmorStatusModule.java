package com.onyxclient.modules.hud;

import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.modules.settings.ModeSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.ArmorHudRenderer;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.HudLayoutTokens;
import com.onyxclient.utils.HudTheme;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

public class ArmorStatusModule extends HudModule {

    private final NumberSetting scale;
    private final ModeSetting layout;
    private final ModeSetting durabilityMode;
    private final BooleanSetting textShadow;
    private final BooleanSetting vanillaBackground;
    private final BooleanSetting showWhileTyping;
    private final BooleanSetting hideUnbreakableDurability;
    private final BooleanSetting showHelmet;
    private final BooleanSetting showChest;
    private final BooleanSetting showLegs;
    private final BooleanSetting showBoots;
    private final BooleanSetting showMainHand;
    private final BooleanSetting showItemName;
    private final BooleanSetting showItemCount;
    private final BooleanSetting staticDamageColors;
    private final ColorSetting highestColor;
    private final ColorSetting highColor;
    private final ColorSetting mediumColor;
    private final ColorSetting mediumLowColor;
    private final ColorSetting lowColor;
    private final ColorSetting lowestColor;

    public ArmorStatusModule() {
        super("ArmorStatus", "Badlion/Lunar style armor and held-item durability", true);
        scale = addSetting(new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.1));
        layout = addSetting(new ModeSetting("Layout", "Horizontal", "Horizontal", "Vertical"));
        durabilityMode = addSetting(new ModeSetting("Durability", "Percentage", "Percentage", "Numeric", "Max/Current"));
        textShadow = addSetting(new BooleanSetting("Text Shadow", true));
        vanillaBackground = addSetting(new BooleanSetting("Vanilla Background", false));
        showWhileTyping = addSetting(new BooleanSetting("Show While Typing", false));
        hideUnbreakableDurability = addSetting(new BooleanSetting("Hide Unbreakable Durability", false));
        showHelmet = addSetting(new BooleanSetting("Helmet", true));
        showChest = addSetting(new BooleanSetting("Chest", true));
        showLegs = addSetting(new BooleanSetting("Legs", true));
        showBoots = addSetting(new BooleanSetting("Boots", true));
        showMainHand = addSetting(new BooleanSetting("Main Hand", true));
        showItemName = addSetting(new BooleanSetting("Item Name", false));
        showItemCount = addSetting(new BooleanSetting("Item Count", true));
        staticDamageColors = addSetting(new BooleanSetting("Static Damage Colors", false));
        highestColor = addSetting(new ColorSetting("Highest Color", 0xFFFFFFFF));
        highColor = addSetting(new ColorSetting("High Color", 0xFF55FF55));
        mediumColor = addSetting(new ColorSetting("Medium Color", 0xFFFFFF55));
        mediumLowColor = addSetting(new ColorSetting("Medium Low Color", 0xFFFFAA00));
        lowColor = addSetting(new ColorSetting("Low Color", 0xFFFF5555));
        lowestColor = addSetting(new ColorSetting("Lowest Color", 0xFFAA0000));
        setUseScaledBounds(true);
        setHudSize(110, 42);
        hudY = 40;
        enablePremiumDefaults();
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        if (!showWhileTyping.getValue() && mc.currentScreen instanceof GuiChat) {
            return;
        }

        ItemStack[] stacks = new ItemStack[] {
                showHelmet.getValue() ? mc.thePlayer.inventory.armorInventory[3] : null,
                showChest.getValue() ? mc.thePlayer.inventory.armorInventory[2] : null,
                showLegs.getValue() ? mc.thePlayer.inventory.armorInventory[1] : null,
                showBoots.getValue() ? mc.thePlayer.inventory.armorInventory[0] : null,
                showMainHand.getValue() ? mc.thePlayer.getHeldItem() : null
        };
        String[] labels = new String[] { "Helm", "Chest", "Legs", "Boots", "Held" };

        float s = scale.getFloatValue();
        boolean vertical = "Vertical".equals(layout.getValue());
        boolean premium = usePremiumRenderer();

        int slot = 24;
        int visible = 0;
        for (ItemStack stack : stacks) {
            if (stack != null) {
                visible++;
            }
        }
        if (visible == 0) {
            setHudSize(40, 20);
            return;
        }

        int contentW = vertical ? (showItemName.getValue() ? 140 : 48) : visible * slot;
        int contentH = vertical ? visible * slot : 36;
        int pad = premium && usePremiumCard() ? HudLayoutTokens.CARD_PADDING_X : 0;
        int padY = premium && usePremiumCard() ? HudLayoutTokens.CARD_PADDING_Y : 0;
        int cardW = (int) ((contentW + pad * 2) * s);
        int cardH = (int) ((contentH + padY * 2) * s);

        GlStateManager.pushMatrix();
        GlStateManager.translate(hudX, hudY, 0);
        GlStateManager.scale(s, s, 1.0F);

        if (premium && usePremiumCard()) {
            drawHudCard(0, 0, contentW + pad * 2, contentH + padY * 2);
        }

        int drawX = pad;
        int drawY = padY;
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            if (stack == null) {
                continue;
            }

            if (vanillaBackground.getValue() && !(premium && usePremiumCard())) {
                RenderUtils.drawRect(drawX - 1, drawY - 1, 18, 18, Colors.withAlpha(Colors.BG_DEEP, 170));
            }
            mc.getRenderItem().renderItemIntoGUI(stack, drawX, drawY);

            String meta = buildMeta(stack);
            if (meta != null && !meta.isEmpty()) {
                int pct = ArmorHudRenderer.percent(stack);
                int color = staticDamageColors.getValue()
                        ? (premium ? HudTheme.VALUE : Colors.TEXT_PRIMARY)
                        : ArmorHudRenderer.tierColor(
                                pct,
                                highestColor.getValue(),
                                highColor.getValue(),
                                mediumColor.getValue(),
                                mediumLowColor.getValue(),
                                lowColor.getValue(),
                                lowestColor.getValue()
                        );
                drawArmorText(mc, meta, drawX, drawY + 18, color, premium);
            }

            if (showItemName.getValue()) {
                String name = labels[i] + ": " + stack.getDisplayName();
                drawArmorText(mc, name, drawX + 20, drawY + 4, premium ? HudTheme.VALUE : Colors.TEXT_PRIMARY, premium);
            } else if (showItemCount.getValue() && stack.stackSize > 1) {
                drawArmorText(mc, "x" + stack.stackSize, drawX + 11, drawY + 9,
                        premium ? HudTheme.TITLE : Colors.TEXT_MUTED, premium);
            }

            if (vertical) {
                drawY += slot;
            } else {
                drawX += slot;
            }
        }
        GlStateManager.popMatrix();
        setHudSize(Math.max(40, cardW), Math.max(20, cardH));
    }

    private String buildMeta(ItemStack stack) {
        if (!stack.isItemStackDamageable()) {
            return hideUnbreakableDurability.getValue() ? "" : "Unbreakable";
        }
        int remaining = ArmorHudRenderer.remaining(stack);
        int max = stack.getMaxDamage();
        int pct = ArmorHudRenderer.percent(stack);
        String mode = durabilityMode.getValue();
        if ("Numeric".equals(mode)) {
            return String.valueOf(remaining);
        }
        if ("Max/Current".equals(mode)) {
            return remaining + "/" + max;
        }
        return pct + "%";
    }

    private void drawArmorText(Minecraft mc, String text, int x, int y, int color, boolean premium) {
        if (premium) {
            drawHudText(mc, text, x, y, color);
            return;
        }
        if (textShadow.getValue()) {
            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        } else {
            mc.fontRendererObj.drawString(text, x, y, color);
        }
    }
}
