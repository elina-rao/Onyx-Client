package com.onyxloader;

/**
 * Applies bytecode patch flags consumed by OnyxClient mixins / OnyxClassTransformer.
 */
public final class PatchManager {

    private PatchManager() {
    }

    public static void applyPatches() {
        System.setProperty("onyx.patch.chunkSkipUnchanged", "true");
        System.setProperty("onyx.patch.entityBatch", "true");
        System.setProperty("onyx.patch.glStateCache", "true");
        System.setProperty("onyx.patch.textureDefer", "true");
        System.setProperty("onyx.patch.hitFxSync", "true");
        System.setProperty("onyx.patch.rawInput", "true");
        System.out.println("[OnyxLoader] PatchManager: render/net/input optimization flags set");
    }
}
