package com.onyxclient;

import com.onyxclient.core.EventBus;
import com.onyxclient.core.ModuleManager;
import com.onyxclient.core.config.ConfigManager;
import com.onyxclient.gui.MainMenu;
import com.onyxclient.network.CapeChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = OnyxClient.MODID, name = OnyxClient.NAME, version = OnyxClient.VERSION, clientSideOnly = true)
public class OnyxClient {

    public static final String MODID = "onyxclient";
    public static final String NAME = "Onyx Client";
    public static final String VERSION = "1.0";

    @Mod.Instance(MODID)
    public static OnyxClient instance;

    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private EventBus eventBus;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configManager = new ConfigManager();
        eventBus = new EventBus();
        moduleManager = new ModuleManager();
        CapeChannel.register();
        com.onyxclient.commands.OnyxCommand.register();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        configManager.load();
        moduleManager.init();
        moduleManager.applyConfig(configManager.getConfig());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(moduleManager);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        configManager.save();
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiMainMenu && !(event.gui instanceof MainMenu)) {
            event.gui = new MainMenu();
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!Keyboard.getEventKeyState()) {
            return;
        }
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_RSHIFT) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null) {
                mc.displayGuiScreen(new com.onyxclient.gui.ModMenu());
            } else if (mc.currentScreen instanceof com.onyxclient.gui.ModMenu) {
                mc.displayGuiScreen(null);
            }
            return;
        }
        moduleManager.onKeyPress(key);
    }

    public static ModuleManager getModuleManager() {
        return instance.moduleManager;
    }

    public static ConfigManager getConfigManager() {
        return instance.configManager;
    }

    public static EventBus getEventBus() {
        return instance.eventBus;
    }
}
