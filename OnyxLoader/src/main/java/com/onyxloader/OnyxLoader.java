package com.onyxloader;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * OnyxLoader entry point — applies JVM/network tuning, injects OptiFine + OnyxClient,
 * then launches Minecraft via Forge/vanilla main.
 *
 * Typical launch (from OnyxLauncher):
 *   java [JVM flags] -jar OnyxLoader.jar --gameDir &lt;dir&gt; --version 1.8.9 ...
 */
public final class OnyxLoader {

    public static void main(String[] args) throws Exception {
        System.out.println("[OnyxLoader] Bootstrapping Onyx Client…");

        File gameDir = resolveGameDir(args);
        if (!gameDir.exists()) {
            gameDir.mkdirs();
        }

        FPSOptimizer.applyProcessHints();
        NetworkOptimizer.install();
        ThreadPriorityManager.install();
        PatchManager.applyPatches();
        System.setProperty("onyx.transformer", OnyxClassTransformer.TRANSFORMER_CLASS);

        File optifineJar = OptiFineInjector.extractBundled(gameDir);
        File clientJar = findClientJar(gameDir);

        List<URL> classpath = new ArrayList<URL>();
        if (optifineJar != null && optifineJar.exists()) {
            classpath.add(optifineJar.toURI().toURL());
            System.out.println("[OnyxLoader] OptiFine injected: " + optifineJar.getName());
        } else {
            System.out.println("[OnyxLoader] OptiFine not bundled — place OptiFine_1.8.9_HD_U_M5.jar in resources/");
        }
        if (clientJar != null) {
            classpath.add(clientJar.toURI().toURL());
            System.out.println("[OnyxLoader] OnyxClient injected: " + clientJar.getName());
        }

        ClassInjector.injectUrls(classpath);
        OptiFineInjector.writeOptiFineProperties(gameDir);

        // Prefer Forge client main when available on classpath; else vanilla.
        String[] launchArgs = buildLaunchArgs(args, gameDir);
        try {
            launch("net.minecraft.launchwrapper.Launch", launchArgs);
        } catch (ClassNotFoundException e) {
            try {
                launch("net.minecraft.client.main.Main", launchArgs);
            } catch (ClassNotFoundException e2) {
                System.err.println("[OnyxLoader] Could not find Minecraft main class on classpath.");
                System.err.println("[OnyxLoader] Ensure game files + Forge libraries are installed, then relaunch.");
                System.err.println("[OnyxLoader] Game dir: " + gameDir.getAbsolutePath());
                // Stay alive briefly so launcher progress can show the message via process exit
                System.exit(2);
            }
        }
    }

    private static void launch(String mainClass, String[] args) throws Exception {
        Class<?> clazz = Class.forName(mainClass);
        Method main = clazz.getMethod("main", String[].class);
        System.out.println("[OnyxLoader] Launching " + mainClass);
        main.invoke(null, (Object) args);
    }

    private static File resolveGameDir(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--gameDir".equals(args[i]) || "--game-dir".equals(args[i])) {
                return new File(args[i + 1]);
            }
        }
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return new File(home, "Library/Application Support/onyxclient");
        }
        if (os.contains("win")) {
            String appdata = System.getenv("APPDATA");
            return new File(appdata != null ? appdata : home, "onyxclient");
        }
        return new File(home, ".onyxclient");
    }

    private static File findClientJar(File gameDir) {
        File mods = new File(gameDir, "mods");
        if (mods.isDirectory()) {
            File[] files = mods.listFiles();
            if (files != null) {
                for (File f : files) {
                    String n = f.getName().toLowerCase();
                    if (n.startsWith("onyxclient") && n.endsWith(".jar")) {
                        return f;
                    }
                }
            }
        }
        File bundled = new File(gameDir, "OnyxClient-1.8.9-v1.0.jar");
        return bundled.exists() ? bundled : null;
    }

    private static String[] buildLaunchArgs(String[] args, File gameDir) {
        List<String> out = new ArrayList<String>();
        boolean hasGameDir = false;
        for (String a : args) {
            out.add(a);
            if ("--gameDir".equals(a) || "--game-dir".equals(a)) {
                hasGameDir = true;
            }
        }
        if (!hasGameDir) {
            out.add("--gameDir");
            out.add(gameDir.getAbsolutePath());
        }
        // LaunchWrapper tweak chain placeholders — launcher/installer should supply full args
        if (!contains(out, "--tweakClass")) {
            out.add("--tweakClass");
            out.add("net.minecraftforge.fml.common.launcher.FMLTweaker");
        }
        return out.toArray(new String[0]);
    }

    private static boolean contains(List<String> list, String value) {
        for (String s : list) {
            if (value.equals(s)) {
                return true;
            }
        }
        return false;
    }
}
