package com.onyxloader;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public final class ClassInjector {

    private ClassInjector() {
    }

    public static void injectUrls(List<URL> urls) throws Exception {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        if (cl instanceof URLClassLoader) {
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            for (URL url : urls) {
                addURL.invoke(cl, url);
            }
            return;
        }
        // Java 9+ fallback — not expected for Java 8 launcher, but keep a child loader note
        System.out.println("[OnyxLoader] System classloader is not URLClassLoader; using thread context loader.");
        URL[] arr = urls.toArray(new URL[0]);
        URLClassLoader child = new URLClassLoader(arr, cl);
        Thread.currentThread().setContextClassLoader(child);
    }

    public static void ensureOnClasspath(File jar) throws Exception {
        if (jar == null || !jar.exists()) {
            return;
        }
        java.util.ArrayList<URL> list = new java.util.ArrayList<URL>();
        list.add(jar.toURI().toURL());
        injectUrls(list);
    }
}
