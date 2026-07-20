package com.onyxclient.modules.utility;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoGGModule extends Module {

    private final Setting<String> message;
    private long lastSent;

    public AutoGGModule() {
        super("AutoGG", "Send gg at game end", ModuleCategory.UTILITY, true);
        // Reuse BooleanSetting pattern with a string via ModeSetting isn't ideal;
        // store custom message as ModeSetting single or use a simple field + Boolean.
        message = addSetting(new com.onyxclient.modules.settings.ModeSetting("Message", "gg", "gg", "gf", "Good game"));
        addSetting(new BooleanSetting("Enabled Message", true));
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled()) {
            return;
        }
        String msg = event.message.getUnformattedText().toLowerCase();
        boolean endGame = msg.contains("victory")
                || msg.contains("game over")
                || msg.contains("winner")
                || msg.contains("1st killer")
                || msg.contains("won the game")
                || msg.contains("has won")
                || msg.contains("reward summary");
        if (!endGame) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSent < 5000) {
            return;
        }
        lastSent = now;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(message.getValue());
        }
    }
}
