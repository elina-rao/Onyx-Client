package com.onyxloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class OptiFineInjector {

    private static final String BUNDLED = "/optifine/OptiFine_1.8.9_HD_U_M5.jar";
    private static final String PROPERTIES =
            "ofFastMath=true\n"
                    + "ofSmoothFps=true\n"
                    + "ofDynamicFov=false\n"
                    + "ofFastRender=true\n"
                    + "ofSmoothWorld=true\n"
                    + "ofLazyChunkLoading=true\n";

    private OptiFineInjector() {
    }

    public static File extractBundled(File gameDir) {
        File outDir = new File(gameDir, "onyx-libs");
        outDir.mkdirs();
        File out = new File(outDir, "OptiFine_1.8.9_HD_U_M5.jar");
        if (out.exists() && out.length() > 0) {
            return out;
        }
        InputStream in = OptiFineInjector.class.getResourceAsStream(BUNDLED);
        if (in == null) {
            // Also check alongside loader jar
            File sibling = new File("OptiFine_1.8.9_HD_U_M5.jar");
            if (sibling.exists()) {
                return sibling;
            }
            File inResources = new File("resources/OptiFine_1.8.9_HD_U_M5.jar");
            return inResources.exists() ? inResources : null;
        }
        try {
            OutputStream os = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                os.write(buf, 0, n);
            }
            os.close();
            in.close();
            return out;
        } catch (Exception e) {
            System.err.println("[OnyxLoader] Failed to extract OptiFine: " + e.getMessage());
            return null;
        }
    }

    public static void writeOptiFineProperties(File gameDir) {
        writeOptiFineProperties(gameDir, PROPERTIES);
    }

    public static void writeOptiFineProperties(File gameDir, String content) {
        try {
            java.nio.file.Files.write(new File(gameDir, "optionsof.txt").toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("[OnyxLoader] Could not write optionsof.txt: " + e.getMessage());
        }
    }
}
