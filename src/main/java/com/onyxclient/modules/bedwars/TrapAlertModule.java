package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Restyles trap-trigger notifications from chat/title. Cosmetic only.
 */
public class TrapAlertModule extends Module {

    private final ColorSetting textColor;
    private final BooleanSetting playSound;
    private final NumberSetting duration;
    private String alertText;
    private int ticksLeft;

    public TrapAlertModule() {
        super("TrapAlert", "Custom trap trigger alerts", ModuleCategory.BEDWARS, true);
        textColor = addSetting(new ColorSetting("Text Color", Colors.DANGER));
        playSound = addSetting(new BooleanSetting("Sound", true));
        duration = addSetting(new NumberSetting("Duration Ticks", 40, 20, 100, 5));
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled()) {
            return;
        }
        String msg = event.message.getUnformattedText().toLowerCase();
        if (msg.contains("trap") && (msg.contains("trigger") || msg.contains("set off")
                || msg.contains("activated") || msg.contains("tripped"))) {
            alertText = event.message.getUnformattedText();
            ticksLeft = duration.getIntValue();
            if (playSound.getValue()) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer != null) {
                    mc.thePlayer.playSound("random.anvil_land", 0.6F, 1.2F);
                }
            }
        }
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (ticksLeft > 0) {
            ticksLeft--;
            if (ticksLeft == 0) {
                alertText = null;
            }
        }
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled() || alertText == null || ticksLeft <= 0) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int w = mc.fontRendererObj.getStringWidth(alertText) + 16;
        int x = (sr.getScaledWidth() - w) / 2;
        int y = 40;
        RenderUtils.drawRoundedRect(x, y, w, 18, 4, Colors.withAlpha(Colors.BG_CARD, 200));
        mc.fontRendererObj.drawStringWithShadow(alertText, x + 8, y + 5, textColor.getValue());
    }
}
