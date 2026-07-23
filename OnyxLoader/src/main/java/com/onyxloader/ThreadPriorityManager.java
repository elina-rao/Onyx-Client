package com.onyxloader;

/**
 * Elevates critical JVM threads for smoother frame pacing.
 * Best-effort — some platforms ignore priority changes.
 */
public final class ThreadPriorityManager {

    private static volatile boolean installed;

    private ThreadPriorityManager() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        bump(Thread.currentThread(), "bootstrap");
        try {
            ThreadGroup root = Thread.currentThread().getThreadGroup();
            while (root.getParent() != null) {
                root = root.getParent();
            }
            Thread[] threads = new Thread[root.activeCount() + 32];
            int n = root.enumerate(threads, true);
            for (int i = 0; i < n; i++) {
                Thread t = threads[i];
                if (t == null) {
                    continue;
                }
                String name = t.getName();
                if (name == null) {
                    continue;
                }
                String lower = name.toLowerCase();
                if (lower.contains("netty")
                        || lower.contains("render")
                        || lower.contains("client")
                        || lower.contains("server thread")
                        || lower.equals("main")
                        || lower.contains("lwjgl")) {
                    bump(t, name);
                }
            }
        } catch (Throwable ignored) {
        }
        System.out.println("[OnyxLoader] ThreadPriorityManager: elevated main/render/netty candidates");
    }

    /** Call after Minecraft has spawned its threads (from client mixin/tick). */
    public static void boostMinecraftThreads() {
        try {
            Thread[] threads = new Thread[Thread.activeCount() + 64];
            int n = Thread.enumerate(threads);
            for (int i = 0; i < n; i++) {
                Thread t = threads[i];
                if (t == null || t.getName() == null) {
                    continue;
                }
                String lower = t.getName().toLowerCase();
                if (lower.contains("client thread")
                        || lower.contains("netty")
                        || lower.contains("render thread")
                        || lower.equals("main")) {
                    bump(t, t.getName());
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void bump(Thread t, String label) {
        try {
            if (t.getPriority() < Thread.MAX_PRIORITY) {
                t.setPriority(Thread.MAX_PRIORITY);
            }
        } catch (Throwable ignored) {
        }
    }
}
