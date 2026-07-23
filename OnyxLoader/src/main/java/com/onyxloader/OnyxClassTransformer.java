package com.onyxloader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * LaunchWrapper-compatible ASM transformer.
 * Registers via system property for Forge when available; also usable standalone.
 *
 * Hypixel-safe: only strips expensive vanilla debug / cloud / weather hooks when
 * {@code onyx.patch.*} properties are enabled — never touches combat or hitboxes.
 */
public final class OnyxClassTransformer implements Opcodes {

    public static final String TRANSFORMER_CLASS = "com.onyxloader.OnyxClassTransformer";

    private OnyxClassTransformer() {
    }

    /**
     * Transform a class if it matches known render/main targets.
     * Returns original bytes when no change applies.
     */
    public static byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || basicClass.length == 0) {
            return basicClass;
        }
        String target = transformedName != null ? transformedName : name;
        if (target == null) {
            return basicClass;
        }

        try {
            if ("net.minecraft.client.Minecraft".equals(target)
                    || "ave".equals(target)) {
                return injectThreadBoost(basicClass, "runGameLoop");
            }
            if ("net.minecraft.client.renderer.RenderGlobal".equals(target)
                    || "bfr".equals(target)) {
                if ("true".equals(System.getProperty("onyx.patch.skipClouds", "false"))) {
                    return nopMethod(basicClass, "renderCloudsCheck", "a");
                }
            }
        } catch (Throwable t) {
            System.err.println("[OnyxLoader] ASM transform skipped for " + target + ": " + t.getMessage());
        }
        return basicClass;
    }

    private static byte[] injectThreadBoost(byte[] basicClass, String... methodHints) {
        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(basicClass);
        reader.accept(node, 0);
        boolean changed = false;
        for (Object methodObj : node.methods) {
            MethodNode method = (MethodNode) methodObj;
            if (method.name == null) {
                continue;
            }
            boolean match = false;
            for (String hint : methodHints) {
                if (method.name.equals(hint) || method.name.equals("as")) {
                    match = true;
                    break;
                }
            }
            // Also match runTick-like short obfuscated names carefully — only inject once
            if (!match && ("runTick".equals(method.name) || "s".equals(method.name))) {
                match = "()V".equals(method.desc);
            }
            if (!match) {
                continue;
            }
            InsnList preamble = new InsnList();
            preamble.add(new MethodInsnNode(
                    INVOKESTATIC,
                    "com/onyxloader/ThreadPriorityManager",
                    "boostMinecraftThreads",
                    "()V",
                    false));
            method.instructions.insert(preamble);
            changed = true;
            break;
        }
        if (!changed) {
            return basicClass;
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        System.out.println("[OnyxLoader] ASM: injected thread boost into Minecraft loop");
        return writer.toByteArray();
    }

    private static byte[] nopMethod(byte[] basicClass, String... names) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);
        boolean changed = false;
        for (Object methodObj : node.methods) {
            MethodNode method = (MethodNode) methodObj;
            for (String name : names) {
                if (name.equals(method.name) && method.desc != null && method.desc.startsWith("(")) {
                    // Replace body with RETURN for void methods only
                    if ("V".equals(method.desc.substring(method.desc.indexOf(')') + 1))) {
                        method.instructions.clear();
                        if (method.tryCatchBlocks != null) {
                            method.tryCatchBlocks.clear();
                        }
                        method.instructions.add(new InsnNode(RETURN));
                        changed = true;
                    }
                }
            }
        }
        if (!changed) {
            return basicClass;
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        System.out.println("[OnyxLoader] ASM: neutralized optional render method");
        return writer.toByteArray();
    }

    /** LaunchWrapper IClassTransformer-shaped entry (reflective). */
    public byte[] transformInstance(String name, String transformedName, byte[] basicClass) {
        return transform(name, transformedName, basicClass);
    }
}
