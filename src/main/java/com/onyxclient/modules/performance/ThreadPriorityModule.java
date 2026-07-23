package com.onyxclient.modules.performance;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Elevates Minecraft client + Netty thread priorities for steadier pacing.
 */
public class ThreadPriorityModule extends Module {

    public static ThreadPriorityModule INSTANCE;

    private int ticks;

    public ThreadPriorityModule() {
        super("ThreadPriority", "Prefer CPU scheduling for game and network threads", ModuleCategory.PERFORMANCE, true);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        boost();
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        ticks++;
        if (ticks % 100 == 0) {
            boost();
        }
    }

    private void boost() {
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        } catch (Throwable ignored) {
        }
        try {
            Thread[] threads = new Thread[Thread.activeCount() + 64];
            int n = Thread.enumerate(threads);
            for (int i = 0; i < n; i++) {
                Thread t = threads[i];
                if (t == null || t.getName() == null) {
                    continue;
                }
                String lower = t.getName().toLowerCase();
                if (lower.contains("client")
                        || lower.contains("netty")
                        || lower.contains("render")
                        || lower.contains("lwjgl")
                        || lower.equals("main")) {
                    try {
                        t.setPriority(Thread.MAX_PRIORITY);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
