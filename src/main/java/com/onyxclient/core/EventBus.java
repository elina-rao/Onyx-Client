package com.onyxclient.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class EventBus {

    private final List<ListenerEntry> listeners = new ArrayList<ListenerEntry>();

    public void register(Object target) {
        for (Method method : target.getClass().getDeclaredMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) {
                continue;
            }
            if (method.getParameterTypes().length != 1) {
                continue;
            }
            method.setAccessible(true);
            listeners.add(new ListenerEntry(target, method, method.getParameterTypes()[0], annotation.priority()));
        }
        listeners.sort((a, b) -> Integer.compare(b.priority, a.priority));
    }

    public void unregister(Object target) {
        listeners.removeIf(entry -> entry.target == target);
    }

    public void post(Object event) {
        Class<?> eventType = event.getClass();
        for (ListenerEntry entry : listeners) {
            if (entry.eventType.isAssignableFrom(eventType)) {
                try {
                    entry.method.invoke(entry.target, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static final class ListenerEntry {
        private final Object target;
        private final Method method;
        private final Class<?> eventType;
        private final int priority;

        private ListenerEntry(Object target, Method method, Class<?> eventType, int priority) {
            this.target = target;
            this.method = method;
            this.eventType = eventType;
            this.priority = priority;
        }
    }
}
