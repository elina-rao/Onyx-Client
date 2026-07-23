package com.onyxloader;

/**
 * Process-level FPS / latency hints applied before Minecraft starts.
 * JVM flags themselves are applied by OnyxLauncher; this class handles runtime tweaks.
 */
public final class FPSOptimizer {

    private FPSOptimizer() {
    }

    public static void applyProcessHints() {
        ThreadPriorityManager.install();

        // Prefer performance where the JDK honors these hints
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("java.awt.headless", "false");
        System.setProperty("onyx.fps.pool", "true");
        System.setProperty("onyx.patch.chunkSkipUnchanged", "true");
        System.setProperty("onyx.patch.entityBatch", "true");
        System.setProperty("onyx.patch.glStateCache", "true");

        // Soft GC ergonomics hint for Java 8 (launcher also sets G1 flags)
        if (System.getProperty("onyx.gc.hints", "true").equals("true")) {
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                    String.valueOf(Math.max(2, Runtime.getRuntime().availableProcessors() - 1)));
        }

        System.out.println("[OnyxLoader] FPS optimizer: thread priority + render hints applied");
    }
}
