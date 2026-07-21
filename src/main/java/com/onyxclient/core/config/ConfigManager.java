package com.onyxclient.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.onyxclient.OnyxClient;
import com.onyxclient.modules.Module;
import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.Setting;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File configDir;
    private final File profilesDir;
    private final File configFile;
    private ClientConfig config = new ClientConfig();

    public ConfigManager() {
        File gameDir = net.minecraftforge.fml.common.Loader.instance().getConfigDir().getParentFile();
        configDir = new File(gameDir, "config/onyxclient");
        profilesDir = new File(configDir, "profiles");
        configFile = new File(configDir, "settings.json");
    }

    public ClientConfig getConfig() {
        return config;
    }

    public File getConfigDir() {
        return configDir;
    }

    public void load() {
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        if (!profilesDir.exists()) {
            profilesDir.mkdirs();
        }
        if (!configFile.exists()) {
            config = new ClientConfig();
            save();
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            config = GSON.fromJson(reader, ClientConfig.class);
            if (config == null) {
                config = new ClientConfig();
            }
            if (config.modules == null) {
                config.modules = new HashMap<String, ModuleConfig>();
            }
            if (config.hud == null) {
                config.hud = new HashMap<String, HudPosition>();
            }
            if (config.favoriteModules == null) {
                config.favoriteModules = new ArrayList<String>();
            }
        } catch (Exception e) {
            config = new ClientConfig();
            e.printStackTrace();
        }
    }

    public void save() {
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveModule(Module module) {
        ModuleConfig moduleConfig = config.modules.get(module.getName());
        if (moduleConfig == null) {
            moduleConfig = new ModuleConfig();
            config.modules.put(module.getName(), moduleConfig);
        }
        moduleConfig.enabled = module.isEnabled();
        moduleConfig.keybind = module.getKeybind();
        moduleConfig.settings = new HashMap<String, Object>();
        for (Setting<?> setting : module.getSettings()) {
            moduleConfig.settings.put(setting.getName(), setting.getValue());
        }
        save();
    }

    public void applyModuleConfig(Module module) {
        ModuleConfig moduleConfig = config.modules.get(module.getName());
        if (moduleConfig == null) {
            return;
        }
        module.setKeybind(moduleConfig.keybind);
        if (moduleConfig.settings != null) {
            for (Setting<?> setting : module.getSettings()) {
                Object value = moduleConfig.settings.get(setting.getName());
                if (value != null) {
                    applySettingValue(setting, value);
                }
            }
        }
        if (moduleConfig.enabled) {
            module.setEnabled(true);
        } else {
            module.setEnabled(false);
        }
    }

    /** Snapshot every module into config.modules before saving a profile. */
    public void syncAllModulesToConfig() {
        for (Module module : OnyxClient.getModuleManager().getModules()) {
            ModuleConfig moduleConfig = new ModuleConfig();
            moduleConfig.enabled = module.isEnabled();
            moduleConfig.keybind = module.getKeybind();
            moduleConfig.settings = new HashMap<String, Object>();
            for (Setting<?> setting : module.getSettings()) {
                moduleConfig.settings.put(setting.getName(), setting.getValue());
            }
            config.modules.put(module.getName(), moduleConfig);
            if (module instanceof HudModule) {
                HudModule hud = (HudModule) module;
                HudPosition pos = new HudPosition();
                pos.x = hud.getHudX();
                pos.y = hud.getHudY();
                pos.w = hud.getHudWidth();
                pos.h = hud.getHudHeight();
                config.hud.put(module.getName(), pos);
            }
        }
    }

    public void applyAllModuleConfigs() {
        for (Module module : OnyxClient.getModuleManager().getModules()) {
            applyModuleConfig(module);
            HudPosition pos = getHudPosition(module.getName());
            if (pos != null && module instanceof HudModule) {
                HudModule hud = (HudModule) module;
                hud.setHudPosition(pos.x, pos.y);
                if (pos.w > 0 && pos.h > 0) {
                    hud.setHudSize(pos.w, pos.h);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void applySettingValue(Setting<?> setting, Object value) {
        if (value instanceof Number && setting.getValue() instanceof Double) {
            ((Setting<Double>) setting).setValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean && setting.getValue() instanceof Boolean) {
            ((Setting<Boolean>) setting).setValue((Boolean) value);
        } else if (value instanceof String && setting.getValue() instanceof String) {
            ((Setting<String>) setting).setValue((String) value);
        } else if (value instanceof Number && setting.getValue() instanceof Integer) {
            ((Setting<Integer>) setting).setValue(((Number) value).intValue());
        }
    }

    public void saveHudPosition(HudModule module) {
        HudPosition pos = new HudPosition();
        pos.x = module.getHudX();
        pos.y = module.getHudY();
        pos.w = module.getHudWidth();
        pos.h = module.getHudHeight();
        config.hud.put(module.getName(), pos);
        save();
    }

    public HudPosition getHudPosition(String name) {
        return config.hud.get(name);
    }

    public String getOnyxApiEndpoint() {
        if (config.onyxApiEndpoint == null || config.onyxApiEndpoint.isEmpty()) {
            return "https://api.onyxrbw.com";
        }
        return config.onyxApiEndpoint;
    }

    public void setOnyxApiEndpoint(String endpoint) {
        config.onyxApiEndpoint = endpoint != null ? endpoint : "";
        save();
    }

    public String getHypixelApiKey() {
        return config.hypixelApiKey != null ? config.hypixelApiKey : "";
    }

    public void setHypixelApiKey(String key) {
        config.hypixelApiKey = key != null ? key : "";
        save();
    }

    public List<String> listProfiles() {
        if (!profilesDir.exists()) {
            profilesDir.mkdirs();
            return Collections.emptyList();
        }
        File[] files = profilesDir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<String>();
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(".json")) {
                // Display spaces instead of filename underscores (e.g. Casual_Bedwars → Casual Bedwars).
                names.add(name.substring(0, name.length() - 5).replace('_', ' '));
            }
        }
        Collections.sort(names);
        return names;
    }

    public boolean saveProfile(String name) {
        String safe = sanitizeProfileName(name);
        if (safe.isEmpty()) {
            return false;
        }
        if (!profilesDir.exists()) {
            profilesDir.mkdirs();
        }
        syncAllModulesToConfig();
        File out = new File(profilesDir, safe + ".json");
        try (FileWriter writer = new FileWriter(out)) {
            GSON.toJson(config, writer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean loadProfile(String name) {
        String safe = sanitizeProfileName(name);
        File in = new File(profilesDir, safe + ".json");
        if (!in.exists()) {
            return false;
        }
        try (FileReader reader = new FileReader(in)) {
            ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);
            if (loaded == null) {
                return false;
            }
            if (loaded.modules == null) {
                loaded.modules = new HashMap<String, ModuleConfig>();
            }
            if (loaded.hud == null) {
                loaded.hud = new HashMap<String, HudPosition>();
            }
            // Keep client prefs from current config; swap modules/hud from profile
            config.modules = loaded.modules;
            config.hud = loaded.hud;
            if (loaded.hypixelApiKey != null) {
                config.hypixelApiKey = loaded.hypixelApiKey;
            }
            if (loaded.capeEnabled != config.capeEnabled) {
                config.capeEnabled = loaded.capeEnabled;
            }
            save();
            applyAllModuleConfigs();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteProfile(String name) {
        String safe = sanitizeProfileName(name);
        File in = new File(profilesDir, safe + ".json");
        return in.exists() && in.delete();
    }

    public boolean renameProfile(String oldName, String newName) {
        String from = sanitizeProfileName(oldName);
        String to = sanitizeProfileName(newName);
        if (from.isEmpty() || to.isEmpty() || from.equals(to)) {
            return false;
        }
        File src = new File(profilesDir, from + ".json");
        File dst = new File(profilesDir, to + ".json");
        if (!src.exists() || dst.exists()) {
            return false;
        }
        return src.renameTo(dst);
    }

    private String sanitizeProfileName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().replaceAll("[^a-zA-Z0-9_\\- ]", "").replace(' ', '_');
    }

    public static class ClientConfig {
        public Map<String, ModuleConfig> modules = new HashMap<String, ModuleConfig>();
        public Map<String, HudPosition> hud = new HashMap<String, HudPosition>();
        public String hypixelApiKey = "";
        public boolean capeEnabled = true;
        public String theme = "default";
        public boolean mainMenuMusic = false;
        public String onyxApiEndpoint = "https://api.onyxrbw.com";

        // Settings tab — General
        public boolean smartDisconnect = false;
        public boolean guiDebug = false;

        // Settings tab — Graphics
        public boolean crosshairInF5 = true;
        public boolean centeredPotionInventory = true;
        public boolean dirtScreen = true;
        public boolean weather = true;
        public boolean borderlessFullscreen = false;

        // Settings tab — Performance
        public int unfocusedFpsLimit = 30;
        public int perfPreset = 0; // 0 Balanced, 1 Max FPS, 2 Quality
        /** 0 = unlimited / follow preset */
        public int focusedFpsCap = 0;
        /** 0 = auto (preset), 1-5 = 2/4/6/8/12 chunks */
        public int renderDistanceOverride = 0;
        public boolean retinaDetected = false;
        public boolean retinaTipShown = false;

        // Settings tab — Controls
        public boolean rawMouseInput = false;
        public boolean disableScrollWheel = false;

        // Settings tab — Menus
        public boolean menuBackgroundBlur = true;
        public boolean menuAnimations = true;
        public int menuUiScale = 1; // 0 small, 1 normal, 2 large
        public boolean menuAnimationsFast = false;

        // Settings tab — Ranked
        public boolean rankedTips = true;

        /** Mod names pinned to top of Mods grid (Badlion-style favorites). */
        public List<String> favoriteModules = new ArrayList<String>();
    }

    public static class ModuleConfig {
        public boolean enabled;
        public int keybind = -1;
        public Map<String, Object> settings = new HashMap<String, Object>();
    }

    public static class HudPosition {
        public int x;
        public int y;
        public int w;
        public int h;
    }
}
