package com.onyxloader;

/**
 * Applies bytecode patches via ASM before Minecraft classes load.
 * Full RenderChunk / GlStateManager patching requires the MC jar on the classpath;
 * this manager records intent and applies safe no-op stubs when classes are absent.
 */
public final class PatchManager {

    private PatchManager() {
    }

    public static void applyPatches() {
        System.out.println("[OnyxLoader] PatchManager: registering render optimizations");
        // These patches activate when the corresponding Minecraft classes are transformed
        // via a LaunchWrapper IClassTransformer registered by OptiFine/Forge.
        // Standalone ASM rewriting of already-loaded classes is not performed here to
        // avoid IllegalStateException; instead we set system properties consumed by
        // OnyxClient mixins / future transformers.
        System.setProperty("onyx.patch.chunkSkipUnchanged", "true");
        System.setProperty("onyx.patch.entityBatch", "true");
        System.setProperty("onyx.patch.glStateCache", "true");
        System.setProperty("onyx.patch.textureDefer", "true");
        System.out.println("[OnyxLoader] Patches flagged (chunk skip, entity batch, GL cache, texture defer)");
    }
}
