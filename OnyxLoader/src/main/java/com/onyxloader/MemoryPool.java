package com.onyxloader;

import java.util.ArrayDeque;

/**
 * Tiny reusable object pool to cut allocation churn in hot helper paths.
 * Does not (and cannot) disable the JVM GC — it only reduces pressure.
 */
public final class MemoryPool<T> {

    public interface Factory<T> {
        T create();
    }

    public interface Reset<T> {
        void reset(T value);
    }

    private final ArrayDeque<T> free = new ArrayDeque<T>();
    private final Factory<T> factory;
    private final Reset<T> reset;
    private final int maxSize;
    private int borrowed;
    private int created;

    public MemoryPool(Factory<T> factory, Reset<T> reset, int maxSize) {
        this.factory = factory;
        this.reset = reset;
        this.maxSize = Math.max(1, maxSize);
    }

    public synchronized T borrow() {
        T value = free.pollFirst();
        if (value == null) {
            value = factory.create();
            created++;
        }
        borrowed++;
        return value;
    }

    public synchronized void release(T value) {
        if (value == null) {
            return;
        }
        if (reset != null) {
            reset.reset(value);
        }
        if (free.size() < maxSize) {
            free.addLast(value);
        }
        borrowed = Math.max(0, borrowed - 1);
    }

    public synchronized int pooled() {
        return free.size();
    }

    public synchronized int createdCount() {
        return created;
    }

    public synchronized int borrowedCount() {
        return borrowed;
    }
}
