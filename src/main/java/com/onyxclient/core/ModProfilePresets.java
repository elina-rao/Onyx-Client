package com.onyxclient.core;

import com.onyxclient.OnyxClient;
import com.onyxclient.core.config.ConfigManager;
import com.onyxclient.modules.Module;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds Casual Bedwars + Ranked Bedwars mod profiles on first launch (does not overwrite existing).
 */
public final class ModProfilePresets {

    private static final String CASUAL = "Casual Bedwars";
    private static final String CASUAL_LEGACY = "Bedwars";
    private static final String RANKED = "Ranked Bedwars";
    private static final String RANKED_LEGACY = "Ranked_BW";

    private ModProfilePresets() {
    }

    public static void ensureDefaults() {
        ConfigManager cm = OnyxClient.getConfigManager();
        List<String> existing = cm.listProfiles();

        // Migrate older preset names to the premium labels (one-time rename).
        migrateLegacyName(cm, existing, CASUAL_LEGACY, CASUAL);
        existing = cm.listProfiles();
        migrateLegacyName(cm, existing, RANKED_LEGACY, RANKED);
        existing = cm.listProfiles();

        boolean needCasual = !containsIgnoreCase(existing, CASUAL);
        boolean needRanked = !containsIgnoreCase(existing, RANKED);
        if (!needCasual && !needRanked) {
            return;
        }
        // Keep current live module state as the restored baseline.
        cm.syncAllModulesToConfig();

        if (needCasual) {
            applyEnabledSet(casualModules(), true);
            cm.saveProfile(CASUAL);
        }
        if (needRanked) {
            applyEnabledSet(rankedModules(), false);
            cm.saveProfile(RANKED);
        }
        cm.applyAllModuleConfigs();
    }

    private static void migrateLegacyName(ConfigManager cm, List<String> existing, String from, String to) {
        if (containsIgnoreCase(existing, from) && !containsIgnoreCase(existing, to)) {
            cm.renameProfile(from, to);
        }
    }

    private static boolean containsIgnoreCase(List<String> names, String target) {
        for (String name : names) {
            if (name == null || target == null) {
                continue;
            }
            if (name.equalsIgnoreCase(target)) {
                return true;
            }
            // Filenames may use underscores for spaces.
            if (name.replace('_', ' ').equalsIgnoreCase(target)
                    || target.replace(' ', '_').equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> casualModules() {
        return new HashSet<String>(Arrays.asList(
                "ArmorStatus", "PotionStatus", "ResourceOverlay", "ToggleSprint", "ToggleSneak",
                "BlockOverlay", "HypixelBedwars", "TNTCountdown", "GeneratorTimer", "TrapAlert",
                "SessionStats", "BedStatus", "Hitboxes", "NoHurtCam", "FPSBoost", "Fullbright",
                "BlockCounter", "BlockInfo", "TeamUpgrades", "BedwarsTeams", "Crosshair",
                "KeyStrokes", "CPS Counter", "CustomFont"
        ));
    }

    private static Set<String> rankedModules() {
        Set<String> set = new HashSet<String>(casualModules());
        set.add("LiveStats");
        set.add("RankElo");
        set.add("PostGameSummary");
        set.add("NickHider");
        // Plan: Keystrokes/CPS optional OFF for competitive ranked preset
        set.remove("KeyStrokes");
        set.remove("CPS Counter");
        return set;
    }

    private static void applyEnabledSet(Set<String> enabled, boolean showKeystrokesCps) {
        for (Module module : OnyxClient.getModuleManager().getModules()) {
            String name = module.getName();
            boolean want = enabled.contains(name);
            if ("KeyStrokes".equals(name) || "CPS Counter".equals(name)) {
                want = showKeystrokesCps;
            }
            if (module.isEnabled() != want) {
                module.setEnabled(want);
            }
        }
        OnyxClient.getConfigManager().syncAllModulesToConfig();
    }
}
