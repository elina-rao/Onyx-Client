package com.onyxclient.modules.utility;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoReconnectModule extends Module {

    private final NumberSetting delaySeconds;
    private final NumberSetting maxRetries;

    private ServerData lastServer;
    private int ticksUntilReconnect = -1;
    private int attempts;

    public AutoReconnectModule() {
        super("AutoReconnect", "Reconnect to last server after disconnect", ModuleCategory.UTILITY);
        delaySeconds = addSetting(new NumberSetting("Delay Seconds", 5, 1, 30, 1));
        maxRetries = addSetting(new NumberSetting("Max Retries", 3, 1, 10, 1));
    }

    @SubscribeEvent
    public void onGui(GuiOpenEvent event) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getCurrentServerData() != null) {
            lastServer = mc.getCurrentServerData();
        }
        if (event.gui instanceof GuiDisconnected && lastServer != null) {
            if (attempts < maxRetries.getIntValue()) {
                ticksUntilReconnect = delaySeconds.getIntValue() * 20;
            }
        }
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || ticksUntilReconnect < 0) {
            return;
        }
        ticksUntilReconnect--;
        if (ticksUntilReconnect == 0 && lastServer != null) {
            attempts++;
            Minecraft mc = Minecraft.getMinecraft();
            mc.displayGuiScreen(new GuiConnecting(mc.currentScreen, mc, lastServer));
            ticksUntilReconnect = -1;
        }
    }

    @Override
    public void onDisable() {
        ticksUntilReconnect = -1;
        attempts = 0;
    }
}
