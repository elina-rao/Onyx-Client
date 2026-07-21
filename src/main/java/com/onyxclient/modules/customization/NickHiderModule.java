package com.onyxclient.modules.customization;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Replaces your real username client-side for streams. Does not change server identity.
 */
public class NickHiderModule extends Module {

    public static NickHiderModule INSTANCE;

    private final BooleanSetting hideInChat;

    public NickHiderModule() {
        super("NickHider", "Hide your username in chat (stream-safe, client-side)", ModuleCategory.CUSTOMIZATION);
        INSTANCE = this;
        hideInChat = addSetting(new BooleanSetting("Hide In Chat", true));
    }

    public String getHiddenName() {
        return "You";
    }

    public String mask(String text) {
        if (!isEnabled() || text == null) {
            return text;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getSession() == null) {
            return text;
        }
        String real = mc.getSession().getUsername();
        if (real == null || real.isEmpty()) {
            return text;
        }
        return text.replace(real, getHiddenName());
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled() || !hideInChat.getValue() || event.message == null) {
            return;
        }
        String masked = mask(event.message.getFormattedText());
        if (masked != null && !masked.equals(event.message.getFormattedText())) {
            event.message = new ChatComponentText(masked);
        }
    }
}
