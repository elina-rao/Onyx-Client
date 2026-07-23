package com.onyxclient.core;

import com.onyxclient.OnyxClient;
import com.onyxclient.core.config.ConfigManager;
import com.onyxclient.modules.Module;
import com.onyxclient.modules.bedwars.*;
import com.onyxclient.modules.combat.*;
import com.onyxclient.modules.customization.*;
import com.onyxclient.modules.hud.*;
import com.onyxclient.modules.movement.*;
import com.onyxclient.modules.performance.*;
import com.onyxclient.modules.rendering.*;
import com.onyxclient.modules.stats.*;
import com.onyxclient.modules.utility.*;
import com.onyxclient.modules.visual.*;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<Module>();

    public void init() {
        // Visual
        register(new AnimationsModule());
        register(new CrosshairModule());
        register(new MotionBlurModule());
        register(new HitColorModule());
        register(new ChestESPModule());
        register(new FullbrightModule());
        register(new FOVChangerModule());
        register(new NametagsModule());
        register(new HitboxesModule());
        register(new ClearWaterModule());
        register(new TimeChangerModule());
        register(new BedwarsStarsModule());
        register(new CustomCapeModule());
        register(new PerspectiveModule());
        register(new CustomFontModule());

        // Performance
        register(new FPSBoostModule());
        register(new AntiBlindModule());
        register(new NoFireOverlayModule());
        register(new NoBobbingModule());
        register(new NoHurtCamModule());
        register(new EntityCullingModule());
        register(new TCPNoDelayModule());
        register(new ThreadPriorityModule());
        register(new LowLatencyPacketsModule());
        register(new HitFeedbackSyncModule());
        register(new RawInputModule());
        register(new BlockHarvestParticlesModule());

        // HUD
        register(new ArmorStatusModule());
        register(new FPSCounterModule());
        register(new PotionStatusModule());
        register(new KeyStrokesModule());
        register(new CPSCounterModule());
        register(new CoordinatesModule());
        register(new PingModule());
        register(new ClockModule());
        register(new ComboCounterModule());
        register(new SpeedModule());
        register(new ScoreboardModule());
        register(new DeathInfoModule());
        register(new CooldownTimersModule());
        register(new SessionStatsModule());
        register(new SaturationModule());

        // Combat
        register(new HitDelayModule());
        register(new CritTimerModule());
        register(new ArmorDurabilityModule());

        // Movement
        register(new SprintResetModule());
        register(new BridgeAssistModule());
        register(new ToggleSprintModule());
        register(new ToggleSneakModule());
        register(new CompassModule());

        // Bedwars
        register(new ResourceOverlayModule());
        register(new HypixelBedwarsModule());
        register(new BedStatusModule());
        register(new BedwarsTeamsModule());
        register(new TeamUpgradesModule());
        register(new TNTCountdownModule());
        register(new ShopQuickNavModule());
        register(new BlockOverlayModule());
        register(new BlockCounterModule());
        register(new BlockInfoModule());
        register(new GeneratorTimerModule());
        register(new TrapAlertModule());

        // Rendering
        register(new DynamicFPSModule());
        register(new CustomFogModule());
        register(new ItemPhysicsModule());
        register(new ClearGlassModule());

        // Customization
        register(new CustomChatModule());
        register(new Skin3DModule());
        register(new NickHiderModule());

        // Stats
        register(new LiveStatsModule());
        register(new RankEloModule());
        register(new PostGameSummaryModule());

        // Utility
        register(new AutoGGModule());
        register(new AutoTipModule());
        register(new AFKDetectorModule());
        register(new AutoReconnectModule());
        register(new ScreenshotModule());
        register(new AntiXrayModule());

        // OptiFine
        register(new OptiFineSettingsModule());
    }

    private void register(Module module) {
        modules.add(module);
        OnyxClient.getEventBus().register(module);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(module);
    }

    public void applyConfig(ConfigManager.ClientConfig config) {
        for (Module module : modules) {
            OnyxClient.getConfigManager().applyModuleConfig(module);
            if (module instanceof HudModule) {
                HudModule hud = (HudModule) module;
                ConfigManager.HudPosition pos = OnyxClient.getConfigManager().getHudPosition(module.getName());
                if (pos != null) {
                    hud.setHudPosition(pos.x, pos.y);
                    if (pos.w > 0 && pos.h > 0) {
                        hud.setHudSize(pos.w, pos.h);
                    }
                }
            }
        }
    }

    public void onKeyPress(int key) {
        for (Module module : modules) {
            if (module.getKeybind() == key) {
                module.toggle();
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick(event);
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        float partialTicks = event.partialTicks;
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onRender2D(partialTicks);
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        float partialTicks = event.partialTicks;
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onRender3D(partialTicks);
            }
        }
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public Module getModule(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public List<Module> getModulesByCategory(com.onyxclient.modules.ModuleCategory category) {
        List<Module> result = new ArrayList<Module>();
        for (Module module : modules) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return result;
    }
}
