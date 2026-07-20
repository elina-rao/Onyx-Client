package com.onyxloader;

/**
 * Process-level FPS / latency hints applied before Minecraft starts.
 * JVM flags themselves are applied by OnyxLauncher; this class handles runtime tweaks.
 */
public final class FPSOptimizer {

    private FPSOptimizer() {
    }

    public static void applyProcessHints() {
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        } catch (Exception ignored) {
        }

        // Hint OS to prefer performance (best-effort; may no-op)
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("java.awt.headless", "false");

        // Ensure options defaults if missing (launcher also writes these)
        System.out.println("[OnyxLoader] FPS optimizer: render thread priority elevated");
    }
}
