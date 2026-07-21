package com.onyxclient.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class MojangSkinApi {

    private MojangSkinApi() {
    }

    public static boolean isMicrosoftSession() {
        Session session = Minecraft.getMinecraft().getSession();
        if (session == null) return false;
        String token = session.getToken();
        if (token == null || token.isEmpty() || "0".equals(token) || "FML".equalsIgnoreCase(token)) {
            return false;
        }
        // Offline/guest sessions often use a short or invalid token
        return token.length() > 20;
    }

    public static String applySkinFile(File png, String variant) {
        if (!isMicrosoftSession()) {
            return "Sign in with Microsoft to apply skins";
        }
        if (png == null || !png.isFile()) {
            return "Skin file missing";
        }
        String token = Minecraft.getMinecraft().getSession().getToken();
        String boundary = "----OnyxSkin" + System.currentTimeMillis();
        try {
            byte[] fileBytes = readAll(png);
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            String v = "slim".equals(variant) ? "slim" : "classic";
            writeFormField(body, boundary, "variant", v);
            body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(fileBytes);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.minecraftservices.com/minecraft/profile/skins").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            OutputStream os = conn.getOutputStream();
            os.write(body.toByteArray());
            os.close();
            int code = conn.getResponseCode();
            InputStream err = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String resp = err != null ? new String(readAll(err), StandardCharsets.UTF_8) : "";
            conn.disconnect();
            if (code >= 200 && code < 300) {
                return null;
            }
            if (resp.contains("errorMessage")) {
                int i = resp.indexOf("errorMessage");
                return resp.substring(Math.max(0, i), Math.min(resp.length(), i + 120));
            }
            return "Apply failed (HTTP " + code + ")";
        } catch (Exception e) {
            return e.getMessage() == null ? "Apply failed" : e.getMessage();
        }
    }

    public static String copyFromUsername(String ign) {
        if (ign == null) return "Enter a username";
        ign = ign.trim();
        if (ign.length() < 3 || ign.length() > 16 || !ign.matches("[A-Za-z0-9_]+")) {
            return "Enter a valid Minecraft username";
        }
        try {
            String profileJson = httpGet("https://api.mojang.com/users/profiles/minecraft/" + ign);
            if (profileJson == null || profileJson.isEmpty() || !profileJson.contains("\"id\"")) {
                return "Player not found";
            }
            String uuid = extractJsonString(profileJson, "id");
            String name = extractJsonString(profileJson, "name");
            if (uuid == null) return "Player not found";
            uuid = uuid.replace("-", "");
            if (name == null || name.isEmpty()) name = ign;
            byte[] png;
            try {
                png = httpGetBytes("https://mc-heads.net/skin/" + uuid);
            } catch (Exception e) {
                png = httpGetBytes("https://crafatar.com/skins/" + uuid);
            }
            if (png == null || png.length < 100) {
                return "Could not download skin";
            }
            String model = detectSlim(uuid);
            SkinLibrary.Entry entry = SkinLibrary.addPng(png, name, model, "username", name, uuid);
            if (entry == null) {
                return "Could not save skin";
            }
            SkinOverrideManager.INSTANCE.applyEntry(entry);
            return null;
        } catch (Exception e) {
            return e.getMessage() == null ? "Copy failed" : e.getMessage();
        }
    }

    private static String detectSlim(String uuidNoDashes) {
        try {
            String profile = httpGet("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidNoDashes);
            if (profile == null) return "classic";
            // crude check for slim in base64 textures blob
            if (profile.contains("slim") || profile.toLowerCase().contains("\"model\" : \"slim\"")) {
                // decode is heavy; mc-heads default is fine — check textures value for slim keyword after decode not needed
            }
            int idx = profile.indexOf("\"value\"");
            if (idx >= 0) {
                String value = extractJsonString(profile.substring(idx - 1), "value");
                if (value != null) {
                    String decoded = new String(java.util.Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
                    if (decoded.contains("\"model\"") && decoded.contains("slim")) {
                        return "slim";
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "classic";
    }

    private static void writeFormField(ByteArrayOutputStream body, String boundary, String name, String value) throws Exception {
        body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(value.getBytes(StandardCharsets.UTF_8));
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String httpGet(String url) throws Exception {
        byte[] bytes = httpGetBytes(url);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] httpGetBytes(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "OnyxClient/1.0");
        int code = conn.getResponseCode();
        InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        byte[] data = in == null ? null : readAll(in);
        conn.disconnect();
        if (code == 404) return null;
        if (code < 200 || code >= 300) {
            throw new Exception("HTTP " + code);
        }
        return data;
    }

    private static byte[] readAll(File file) throws Exception {
        FileInputStream in = new FileInputStream(file);
        try {
            return readAll(in);
        } finally {
            in.close();
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static String extractJsonString(String json, String key) {
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return null;
        int startQuote = json.indexOf('"', colon + 1);
        if (startQuote < 0) return null;
        int endQuote = json.indexOf('"', startQuote + 1);
        if (endQuote < 0) return null;
        return json.substring(startQuote + 1, endQuote);
    }
}
