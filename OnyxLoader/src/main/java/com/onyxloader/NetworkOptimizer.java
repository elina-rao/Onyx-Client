package com.onyxloader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;

/**
 * TCP_NODELAY / buffer hints. Packet payloads stay vanilla — we only reduce buffering.
 */
public final class NetworkOptimizer {

    private static boolean installed;

    private NetworkOptimizer() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        System.setProperty("onyx.tcpNoDelay", "true");
        System.setProperty("onyx.net.flush", "true");
        System.out.println("[OnyxLoader] Network optimizer installed (TCP_NODELAY + flush preferred)");
    }

    public static void tuneSocket(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.setTcpNoDelay(true);
            socket.setSendBufferSize(65535);
            socket.setReceiveBufferSize(65535);
            socket.setTrafficClass(0x10); // IPTOS_LOWDELAY when supported
        } catch (Exception ignored) {
        }
    }

    /**
     * Reflectively apply TCP_NODELAY on a Netty Channel if present.
     */
    public static void tuneNettyChannel(Object channel) {
        if (channel == null) {
            return;
        }
        try {
            Method config = channel.getClass().getMethod("config");
            Object cfg = config.invoke(channel);
            Class<?> optionClass = Class.forName("io.netty.channel.ChannelOption");
            Field tcpNoDelay = optionClass.getField("TCP_NODELAY");
            Object option = tcpNoDelay.get(null);
            Method setOption = cfg.getClass().getMethod("setOption", optionClass, Object.class);
            setOption.invoke(cfg, option, Boolean.TRUE);
        } catch (Throwable ignored) {
        }
    }

    /** Best-effort channel flush after a write (same packet bytes). */
    public static void flushChannel(Object channel) {
        if (channel == null || !"true".equals(System.getProperty("onyx.net.flush", "true"))) {
            return;
        }
        try {
            Method flush = channel.getClass().getMethod("flush");
            flush.invoke(channel);
        } catch (Throwable ignored) {
        }
    }
}
