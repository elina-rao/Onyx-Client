package com.onyxclient.core;

import com.onyxclient.OnyxClient;
import com.onyxclient.core.config.ConfigManager;
import com.onyxclient.modules.Module;
import com.onyxclient.modules.customization.Skin3DModule;
import com.onyxclient.modules.performance.EntityCullingModule;
import com.onyxclient.modules.performance.FPSBoostModule;
import com.onyxclient.modules.performance.NoBobbingModule;
import com.onyxclient.modules.performance.NoFireOverlayModule;
import com.onyxclient.modules.performance.NoHurtCamModule;
import com.onyxclient.modules.performance.TCPNoDelayModule;
import com.onyxclient.modules.rendering.ClearGlassModule;
import com.onyxclient.modules.rendering.CustomFogModule;
import com.onyxclient.modules.rendering.DynamicFPSModule;
import com.onyxclient.modules.rendering.ItemPhysicsModule;
import com.onyxclient.modules.visual.AnimationsModule;
import com.onyxclient.modules.visual.ClearWaterModule;
import com.onyxclient.modules.visual.CustomCapeModule;
import com.onyxclient.modules.visual.HitColorModule;
import com.onyxclient.modules.visual.MotionBlurModule;
import com.onyxclient.modules.visual.NametagsModule;
import com.onyxclient.modules.visual.OptiFineSettingsModule;
import com.onyxclient.modules.visual.TimeChangerModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies Badlion-style performance presets: modules, gameSettings, options.txt, optionsof.txt.
 */
public final class PerformanceApplier {

    private static GameSnapshot qualitySnapshot;

    private PerformanceApplier() {
    }

    public static boolean isMaxFpsPreset() {
        return OnyxClient.getConfigManager().getConfig().perfPreset == 1;
    }

    public static boolean isTileEntityCullingActive() {
        return isMaxFpsPreset();
    }

    public static void apply(int presetIndex) {
        if (presetIndex < 0 || presetIndex >= PerformancePresets.NAMES.length) {
            return;
        }
        if (presetIndex == 2) {
            applyQuality();
        } else if (presetIndex == 1) {
            captureQualitySnapshotIfNeeded();
            applyMaxFps();
        } else {
            captureQualitySnapshotIfNeeded();
            applyBalanced();
        }
        syncOptiFineModuleToggles();
        writeOptionsFiles(presetIndex);
        PlatformPerformance.onClientReady();
    }

    private static void captureQualitySnapshotIfNeeded() {
        if (qualitySnapshot != null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            return;
        }
        GameSettings gs = mc.gameSettings;
        ConfigManager.ClientConfig cfg = OnyxClient.getConfigManager().getConfig();
        qualitySnapshot = new GameSnapshot();
        qualitySnapshot.renderDistance = gs.renderDistanceChunks;
        qualitySnapshot.fancyGraphics = gs.fancyGraphics;
        qualitySnapshot.ambientOcclusion = gs.ambientOcclusion;
        qualitySnapshot.entityShadows = gs.entityShadows;
        qualitySnapshot.particleSetting = gs.particleSetting;
        qualitySnapshot.clouds = gs.clouds;
        qualitySnapshot.limitFramerate = gs.limitFramerate;
        qualitySnapshot.weather = cfg.weather;
        qualitySnapshot.dirtScreen = cfg.dirtScreen;
        qualitySnapshot.menuBackgroundBlur = cfg.menuBackgroundBlur;
        qualitySnapshot.menuAnimations = cfg.menuAnimations;
        qualitySnapshot.capeEnabled = cfg.capeEnabled;
    }

    private static void applyBalanced() {
        ConfigManager.ClientConfig cfg = OnyxClient.getConfigManager().getConfig();
        cfg.weather = true;
        cfg.dirtScreen = true;
        cfg.menuBackgroundBlur = true;
        cfg.menuAnimations = true;

        applyFpsBoost(true, false, true, true, 50.0, 48.0);
        setModule(EntityCullingModule.INSTANCE, true);
        if (EntityCullingModule.INSTANCE != null) {
            EntityCullingModule.INSTANCE.distance.setValue(48.0);
        }
        setModule(TCPNoDelayModule.INSTANCE, true);
        setByName("AntiBlind", false);
        setModule(NoBobbingModule.INSTANCE, false);
        setModule(NoHurtCamModule.INSTANCE, false);
        setModule(NoFireOverlayModule.INSTANCE, false);
        setModule(DynamicFPSModule.INSTANCE, true);
        if (DynamicFPSModule.INSTANCE != null) {
            DynamicFPSModule.INSTANCE.backgroundFps.setValue((double) cfg.unfocusedFpsLimit);
        }
        setModule(CustomFogModule.INSTANCE, false);

        setModule(MotionBlurModule.INSTANCE, false);
        setModule(Skin3DModule.INSTANCE, false);
        setByName("ChestESP", false);
        setModule(ItemPhysicsModule.INSTANCE, false);
        setModule(ClearGlassModule.INSTANCE, false);

        applyGameSettings(resolveRenderDistance(6), 0, false, 0, false, 1, 1);
        writeOptiFineFile(BALANCED_OPTIFINE);
    }

    private static void applyMaxFps() {
        ConfigManager.ClientConfig cfg = OnyxClient.getConfigManager().getConfig();
        cfg.weather = false;
        cfg.dirtScreen = false;
        cfg.menuBackgroundBlur = false;
        cfg.menuAnimations = false;
        cfg.capeEnabled = false;

        applyFpsBoost(true, true, true, true, 0.0, 16.0);
        setModule(EntityCullingModule.INSTANCE, true);
        if (EntityCullingModule.INSTANCE != null) {
            EntityCullingModule.INSTANCE.distance.setValue(20.0);
        }
        setModule(TCPNoDelayModule.INSTANCE, true);
        setByName("AntiBlind", true);
        setModule(NoBobbingModule.INSTANCE, true);
        setModule(NoHurtCamModule.INSTANCE, true);
        setModule(NoFireOverlayModule.INSTANCE, true);
        setModule(DynamicFPSModule.INSTANCE, true);
        if (DynamicFPSModule.INSTANCE != null) {
            DynamicFPSModule.INSTANCE.backgroundFps.setValue((double) cfg.unfocusedFpsLimit);
        }
        setModule(CustomFogModule.INSTANCE, true);
        if (CustomFogModule.INSTANCE != null) {
            CustomFogModule.INSTANCE.disableFog.setValue(true);
            CustomFogModule.INSTANCE.fogDistance.setValue(0.1);
        }

        setModule(MotionBlurModule.INSTANCE, false);
        setModule(NametagsModule.INSTANCE, false);
        setByName("BedwarsStars", false);
        setModule(Skin3DModule.INSTANCE, false);
        setModule(CustomCapeModule.INSTANCE, false);
        setByName("ChestESP", false);
        setModule(AnimationsModule.INSTANCE, false);
        setModule(HitColorModule.INSTANCE, false);
        setModule(TimeChangerModule.INSTANCE, false);
        setModule(ClearWaterModule.INSTANCE, false);
        setModule(ItemPhysicsModule.INSTANCE, false);
        setModule(ClearGlassModule.INSTANCE, false);

        applyGameSettings(resolveRenderDistance(4), 0, false, 0, false, 0, 0);
        writeOptiFineFile(MAX_FPS_OPTIFINE);
    }

    private static void applyQuality() {
        if (qualitySnapshot != null) {
            ConfigManager.ClientConfig cfg = OnyxClient.getConfigManager().getConfig();
            cfg.weather = qualitySnapshot.weather;
            cfg.dirtScreen = qualitySnapshot.dirtScreen;
            cfg.menuBackgroundBlur = qualitySnapshot.menuBackgroundBlur;
            cfg.menuAnimations = qualitySnapshot.menuAnimations;
            cfg.capeEnabled = qualitySnapshot.capeEnabled;
            applyGameSettings(
                    qualitySnapshot.renderDistance,
                    qualitySnapshot.limitFramerate,
                    qualitySnapshot.fancyGraphics,
                    qualitySnapshot.ambientOcclusion,
                    qualitySnapshot.entityShadows,
                    qualitySnapshot.particleSetting,
                    qualitySnapshot.clouds
            );
        } else {
            applyGameSettings(resolveRenderDistance(12), 0, true, 2, true, 2, 2);
        }

        setModule(FPSBoostModule.INSTANCE, false);
        setModule(EntityCullingModule.INSTANCE, false);
        setModule(TCPNoDelayModule.INSTANCE, true);
        setByName("AntiBlind", false);
        setModule(NoBobbingModule.INSTANCE, false);
        setModule(NoHurtCamModule.INSTANCE, false);
        setModule(NoFireOverlayModule.INSTANCE, false);
        setModule(DynamicFPSModule.INSTANCE, true);
        if (DynamicFPSModule.INSTANCE != null) {
            DynamicFPSModule.INSTANCE.backgroundFps.setValue(
                    (double) OnyxClient.getConfigManager().getConfig().unfocusedFpsLimit);
        }
        setModule(CustomFogModule.INSTANCE, false);
        writeOptiFineFile(QUALITY_OPTIFINE);
    }

    public static int resolveRenderDistance(int presetDefault) {
        int override = OnyxClient.getConfigManager().getConfig().renderDistanceOverride;
        if (override <= 0) {
            return presetDefault;
        }
        switch (override) {
            case 1: return 2;
            case 2: return 4;
            case 3: return 6;
            case 4: return 8;
            case 5: return 12;
            default: return presetDefault;
        }
    }

    private static void applyFpsBoost(boolean on, boolean particlesOff, boolean entityDist, boolean clouds,
                                       double particleCap, double entityDistance) {
        setModule(FPSBoostModule.INSTANCE, on);
        if (FPSBoostModule.INSTANCE != null && on) {
            FPSBoostModule.INSTANCE.disableParticles.setValue(particlesOff);
            FPSBoostModule.INSTANCE.reduceEntityDistance.setValue(entityDist);
            FPSBoostModule.INSTANCE.disableClouds.setValue(clouds);
            FPSBoostModule.INSTANCE.particleCap.setValue(particleCap);
            FPSBoostModule.INSTANCE.entityDistance.setValue(entityDistance);
        }
    }

    private static void applyGameSettings(int renderDistance, int fpsLimit, boolean fancy, int ao,
                                          boolean entityShadows, int particles, int clouds) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            return;
        }
        ConfigManager.ClientConfig cfg = OnyxClient.getConfigManager().getConfig();
        GameSettings gs = mc.gameSettings;
        gs.renderDistanceChunks = Math.max(2, Math.min(32, renderDistance));
        int cap = cfg.focusedFpsCap;
        gs.limitFramerate = cap > 0 ? cap : fpsLimit;
        gs.enableVsync = false;
        gs.fancyGraphics = fancy;
        gs.ambientOcclusion = ao;
        gs.entityShadows = entityShadows;
        gs.particleSetting = particles;
        gs.clouds = clouds;
        if (NoBobbingModule.INSTANCE != null && NoBobbingModule.INSTANCE.isEnabled()) {
            gs.viewBobbing = false;
        }
    }

    private static void setModule(Module module, boolean enabled) {
        if (module != null) {
            module.setEnabled(enabled);
        }
    }

    private static void setByName(String name, boolean enabled) {
        Module module = OnyxClient.getModuleManager().getModule(name);
        if (module != null) {
            module.setEnabled(enabled);
        }
    }

    public static void writeOptionsFiles(int presetIndex) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        File gameDir = mc.mcDataDir;
        int renderDist = presetIndex == 1 ? resolveRenderDistance(4) : presetIndex == 2 ? resolveRenderDistance(12) : resolveRenderDistance(6);
        ConfigManager.ClientConfig cfg = OnyxClient.getConfigManager().getConfig();
        int maxFps = cfg.focusedFpsCap > 0 ? cfg.focusedFpsCap : 0;
        writeOptionsTxt(gameDir, maxFps, renderDist, presetIndex == 1);
        String optifine = presetIndex == 1 ? MAX_FPS_OPTIFINE : presetIndex == 2 ? QUALITY_OPTIFINE : BALANCED_OPTIFINE;
        writeOptiFineFile(gameDir, optifine);
    }

    public static void writeOptionsTxt(File gameDir, int maxFps, int renderDistance, boolean maxPreset) {
        try {
            File options = new File(gameDir, "options.txt");
            String text = options.exists() ? new String(Files.readAllBytes(options.toPath()), StandardCharsets.UTF_8) : "";
            text = setOption(text, "enableVsync", "false");
            text = setOption(text, "maxFps", String.valueOf(maxFps));
            text = setOption(text, "renderDistance", String.valueOf(renderDistance));
            text = setOption(text, "fancyGraphics", maxPreset ? "false" : "true");
            text = setOption(text, "ao", maxPreset ? "0" : "2");
            text = setOption(text, "entityShadows", maxPreset ? "false" : "true");
            text = setOption(text, "particles", maxPreset ? "0" : "1");
            Files.write(options.toPath(), (text.trim() + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeOptiFineFile(String content) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        writeOptiFineFile(mc.mcDataDir, content);
    }

    public static void writeOptiFineFile(File gameDir, String content) {
        try {
            Files.write(new File(gameDir, "optionsof.txt").toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String setOption(String text, String key, String value) {
        Matcher m = Pattern.compile("^" + key + ":.*$", Pattern.MULTILINE).matcher(text);
        if (m.find()) {
            return m.replaceAll(key + ":" + value);
        }
        return text + "\n" + key + ":" + value;
    }

    public static void syncOptiFineModuleToggles() {
        OptiFineSettingsModule mod = OptiFineSettingsModule.INSTANCE;
        if (mod == null) {
            return;
        }
        ConfigManager.ClientConfig cfg = OnyxClient.getConfigManager().getConfig();
        boolean quality = cfg.perfPreset == 2;
        mod.fastMath.setValue(!quality);
        mod.dynamicLights.setValue(quality);
        mod.connectedTextures.setValue(quality);
        mod.customSky.setValue(quality);
    }

    public static void applyOptiFineModuleToDisk() {
        OptiFineSettingsModule mod = OptiFineSettingsModule.INSTANCE;
        if (mod != null) {
            String content =
                    "ofFastRender=" + (mod.fastMath.getValue() ? "true" : "false") + "\n"
                            + "ofFastMath=" + (mod.fastMath.getValue() ? "true" : "false") + "\n"
                            + "ofLazyChunkLoading=true\n"
                            + "ofChunkUpdates=" + (mod.fastMath.getValue() ? "1" : "5") + "\n"
                            + "ofDynamicLights=" + (mod.dynamicLights.getValue() ? "1" : "0") + "\n"
                            + "ofConnectedTextures=" + (mod.connectedTextures.getValue() ? "2" : "0") + "\n"
                            + "ofCustomSky=" + (mod.customSky.getValue() ? "true" : "false") + "\n"
                            + "ofSmoothFps=false\n"
                            + "ofSmoothWorld=false\n"
                            + "ofTerrainLoD=true\n"
                            + "ofDynamicFov=false\n";
            writeOptiFineFile(content);
            return;
        }
        ConfigManager.ClientConfig cfg = OnyxClient.getConfigManager().getConfig();
        if (cfg.perfPreset == 1) {
            writeOptiFineFile(MAX_FPS_OPTIFINE);
        } else if (cfg.perfPreset == 2) {
            writeOptiFineFile(QUALITY_OPTIFINE);
        } else {
            writeOptiFineFile(BALANCED_OPTIFINE);
        }
    }

    public static String getPresetSummary(int preset) {
        if (preset == 1) {
            return "Max FPS: unlimited, render " + resolveRenderDistance(4) + ", fast render, particles off";
        }
        if (preset == 2) {
            return "Quality: visuals restored, higher render distance";
        }
        return "Balanced: fast render on, moderate render distance";
    }

    public static String getDiagnosticLine() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            return "Game not loaded";
        }
        boolean fastRender = readOptiFineFlag(mc.mcDataDir, "ofFastRender", false);
        return "Render " + mc.gameSettings.renderDistanceChunks
                + " | FPS cap " + (mc.gameSettings.limitFramerate == 0 ? "off" : mc.gameSettings.limitFramerate)
                + " | FastRender " + (fastRender ? "on" : "off")
                + " | " + mc.displayWidth + "x" + mc.displayHeight;
    }

    private static boolean readOptiFineFlag(File gameDir, String key, boolean def) {
        try {
            File f = new File(gameDir, "optionsof.txt");
            if (!f.exists()) {
                return def;
            }
            String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("^" + key + ":(.*)$", Pattern.MULTILINE).matcher(text);
            if (m.find()) {
                return "true".equalsIgnoreCase(m.group(1).trim());
            }
        } catch (Exception ignored) {
        }
        return def;
    }

    private static final String MAX_FPS_OPTIFINE =
            "ofFastRender=true\n"
                    + "ofFastMath=true\n"
                    + "ofLazyChunkLoading=true\n"
                    + "ofChunkUpdates=1\n"
                    + "ofDynamicLights=0\n"
                    + "ofConnectedTextures=0\n"
                    + "ofCustomSky=false\n"
                    + "ofSmoothFps=false\n"
                    + "ofSmoothWorld=false\n"
                    + "ofTerrainLoD=true\n"
                    + "ofDynamicFov=false\n";

    private static final String BALANCED_OPTIFINE =
            "ofFastRender=true\n"
                    + "ofFastMath=true\n"
                    + "ofLazyChunkLoading=true\n"
                    + "ofChunkUpdates=3\n"
                    + "ofDynamicLights=1\n"
                    + "ofConnectedTextures=2\n"
                    + "ofCustomSky=true\n"
                    + "ofSmoothFps=false\n"
                    + "ofSmoothWorld=false\n"
                    + "ofTerrainLoD=true\n"
                    + "ofDynamicFov=false\n";

    private static final String QUALITY_OPTIFINE =
            "ofFastRender=false\n"
                    + "ofFastMath=false\n"
                    + "ofLazyChunkLoading=false\n"
                    + "ofChunkUpdates=5\n"
                    + "ofDynamicLights=1\n"
                    + "ofConnectedTextures=2\n"
                    + "ofCustomSky=true\n"
                    + "ofSmoothFps=true\n"
                    + "ofSmoothWorld=true\n"
                    + "ofTerrainLoD=false\n"
                    + "ofDynamicFov=false\n";

    private static final class GameSnapshot {
        int renderDistance;
        boolean fancyGraphics;
        int ambientOcclusion;
        boolean entityShadows;
        int particleSetting;
        int clouds;
        int limitFramerate;
        boolean weather;
        boolean dirtScreen;
        boolean menuBackgroundBlur;
        boolean menuAnimations;
        boolean capeEnabled;
    }
}
