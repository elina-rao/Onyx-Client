package com.onyxclient.core;

import com.onyxclient.OnyxClient;
import com.onyxclient.core.config.ConfigManager;
import com.onyxclient.gui.GuiConfirmDisconnect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/**
 * Applies ClientConfig settings that are toggled in Mod Menu Settings but need runtime hooks.
 */
public final class ClientSettingsHooks {

    public static final ClientSettingsHooks INSTANCE = new ClientSettingsHooks();

    private boolean rankedTipShown;
    private Boolean lastBorderless;
    private Boolean lastRawMouse;
    private int savedFpsLimit = -1;

    private ClientSettingsHooks() {
    }

    public void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    private static ConfigManager.ClientConfig cfg() {
        return OnyxClient.getConfigManager().getConfig();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ConfigManager.ClientConfig c = cfg();

        // Weather off — clear rain/thunder strength each tick
        if (!c.weather && mc.theWorld != null) {
            mc.theWorld.setRainStrength(0.0F);
            mc.theWorld.setThunderStrength(0.0F);
        }

        applyUnfocusedFps(mc, c);
        applyBorderlessIfChanged(c);
        applyRawMouseIfChanged(c);
        maybeShowRankedTip(mc);
    }

    private void maybeShowRankedTip(Minecraft mc) {
        if (!cfg().rankedTips || rankedTipShown) {
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (mc.getCurrentServerData() != null) {
            rankedTipShown = true;
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    "§5[Onyx] §7Tip: Open Mod Menu with §fRight Shift §7— ranked stats sync by IGN."));
        }
    }

    private void applyUnfocusedFps(Minecraft mc, ConfigManager.ClientConfig c) {
        if (com.onyxclient.modules.rendering.DynamicFPSModule.INSTANCE == null
                || !com.onyxclient.modules.rendering.DynamicFPSModule.INSTANCE.isEnabled()) {
            if (savedFpsLimit >= 0) {
                mc.gameSettings.limitFramerate = savedFpsLimit;
                savedFpsLimit = -1;
            }
            return;
        }
        boolean unfocused = !Display.isActive();
        if (unfocused) {
            if (savedFpsLimit < 0) {
                savedFpsLimit = mc.gameSettings.limitFramerate;
            }
            int limit = c.unfocusedFpsLimit;
            if (com.onyxclient.modules.rendering.DynamicFPSModule.INSTANCE != null) {
                limit = com.onyxclient.modules.rendering.DynamicFPSModule.INSTANCE.backgroundFps.getIntValue();
            }
            mc.gameSettings.limitFramerate = Math.max(5, Math.min(60, limit));
        } else if (savedFpsLimit >= 0) {
            int restore = savedFpsLimit;
            savedFpsLimit = -1;
            if (c.focusedFpsCap > 0) {
                mc.gameSettings.limitFramerate = c.focusedFpsCap;
            } else {
                mc.gameSettings.limitFramerate = restore > 0 ? restore : 0;
            }
        } else if (c.focusedFpsCap > 0) {
            mc.gameSettings.limitFramerate = c.focusedFpsCap;
        }
    }

    public void applyBorderlessIfChanged(ConfigManager.ClientConfig c) {
        if (lastBorderless != null && lastBorderless.booleanValue() == c.borderlessFullscreen) {
            return;
        }
        lastBorderless = c.borderlessFullscreen;
        applyBorderless(c.borderlessFullscreen);
    }

    public static void applyBorderless(boolean enabled) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (enabled) {
                if (!Display.isFullscreen()) {
                    DisplayMode desktop = Display.getDesktopDisplayMode();
                    Display.setDisplayMode(desktop);
                    Display.setFullscreen(true);
                    // Best-effort: some platforms ignore undecorated once created
                }
            } else {
                if (Display.isFullscreen()) {
                    Display.setFullscreen(false);
                    int w = Math.max(854, mc.displayWidth);
                    int h = Math.max(480, mc.displayHeight);
                    Display.setDisplayMode(new DisplayMode(w, h));
                }
            }
            Display.update();
            mc.resize(Display.getWidth(), Display.getHeight());
        } catch (Exception ignored) {
        }
    }

    public void applyRawMouseIfChanged(ConfigManager.ClientConfig c) {
        if (lastRawMouse != null && lastRawMouse.booleanValue() == c.rawMouseInput) {
            return;
        }
        lastRawMouse = c.rawMouseInput;
        applyRawMouse(c.rawMouseInput);
    }

    public static void applyRawMouse(boolean enabled) {
        try {
            System.setProperty("onyx.rawMouse", enabled ? "true" : "false");
            // LWJGL2: Mouse.setGrabbed + recenter. Raw input is primarily a Windows win;
            // macOS builds often ignore raw-mode flags.
            if (Mouse.isCreated()) {
                boolean grabbed = Mouse.isGrabbed();
                if (enabled) {
                    // Re-assert grab so Windows raw path re-initializes when supported
                    if (grabbed) {
                        Mouse.setGrabbed(false);
                        Mouse.setGrabbed(true);
                    }
                    Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
                }
            }
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                // Hint for any native hooks; harmless if unused
                System.setProperty("org.lwjgl.input.Mouse.raw", enabled ? "true" : "false");
            }
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            return;
        }
        ConfigManager.ClientConfig c = cfg();
        if (c.crosshairInF5) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.thirdPersonView != 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onGuiAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!cfg().smartDisconnect) {
            return;
        }
        if (!(event.gui instanceof GuiIngameMenu)) {
            return;
        }
        // Vanilla pause "Disconnect" is button id 1
        if (event.button != null && event.button.id == 1) {
            event.setCanceled(true);
            Minecraft mc = Minecraft.getMinecraft();
            mc.displayGuiScreen(new GuiConfirmDisconnect(event.gui));
        }
    }

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        rankedTipShown = false;
    }

    /** Called from ModMenu when a settings toggle that needs immediate apply changes. */
    public static void onConfigToggle(String key) {
        ConfigManager.ClientConfig c = cfg();
        if ("borderlessFullscreen".equals(key)) {
            INSTANCE.lastBorderless = null;
            INSTANCE.applyBorderlessIfChanged(c);
        } else if ("rawMouseInput".equals(key)) {
            INSTANCE.lastRawMouse = null;
            INSTANCE.applyRawMouseIfChanged(c);
        }
    }
}
