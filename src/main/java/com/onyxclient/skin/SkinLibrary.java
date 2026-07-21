package com.onyxclient.skin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared skin library with the Electron launcher under Application Support / AppData.
 */
public final class SkinLibrary {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class Entry {
        public String id;
        public String name;
        public String file;
        public String model;
        public String source;
        public String ign;
        public String uuid;

        public File resolveFile(File dir) {
            return new File(dir, file);
        }
    }

    public static final class Index {
        public String activeId;
        public String model = "classic";
        public final List<Entry> skins = new ArrayList<Entry>();
    }

    private SkinLibrary() {
    }

    public static File resolveSkinsDir() {
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name", "").toLowerCase();
        List<File> candidates = new ArrayList<File>();
        if (os.contains("mac")) {
            File appSupport = new File(home, "Library/Application Support");
            candidates.add(new File(appSupport, "Onyx Launcher/skins"));
            candidates.add(new File(appSupport, "onyx-launcher/skins"));
        } else if (os.contains("win")) {
            String appdata = System.getenv("APPDATA");
            if (appdata == null || appdata.isEmpty()) {
                appdata = new File(home, "AppData/Roaming").getAbsolutePath();
            }
            candidates.add(new File(appdata, "Onyx Launcher/skins"));
            candidates.add(new File(appdata, "onyx-launcher/skins"));
        } else {
            candidates.add(new File(home, ".config/Onyx Launcher/skins"));
            candidates.add(new File(home, ".config/onyx-launcher/skins"));
        }
        for (File c : candidates) {
            if (c.isDirectory() || new File(c, "index.json").isFile()) {
                return c;
            }
        }
        File preferred = candidates.get(0);
        preferred.mkdirs();
        return preferred;
    }

    public static Index load() {
        File dir = resolveSkinsDir();
        File indexFile = new File(dir, "index.json");
        Index idx = new Index();
        if (!indexFile.isFile()) {
            return idx;
        }
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(indexFile), StandardCharsets.UTF_8);
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            if (root.has("activeId") && !root.get("activeId").isJsonNull()) {
                idx.activeId = root.get("activeId").getAsString();
            }
            if (root.has("model") && !root.get("model").isJsonNull()) {
                idx.model = root.get("model").getAsString();
            }
            if (root.has("skins") && root.get("skins").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("skins");
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    Entry e = new Entry();
                    e.id = getStr(o, "id");
                    e.name = getStr(o, "name");
                    e.file = getStr(o, "file");
                    e.model = getStr(o, "model");
                    e.source = getStr(o, "source");
                    e.ign = getStr(o, "ign");
                    e.uuid = getStr(o, "uuid");
                    if (e.id != null && e.file != null) {
                        idx.skins.add(e);
                    }
                }
            }
        } catch (Exception ignored) {
            /* keep empty */
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
        if (idx.model == null || (!idx.model.equals("slim") && !idx.model.equals("classic"))) {
            idx.model = "classic";
        }
        return idx;
    }

    public static void save(Index idx) {
        File dir = resolveSkinsDir();
        dir.mkdirs();
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        if (idx.activeId != null) {
            root.addProperty("activeId", idx.activeId);
        } else {
            root.add("activeId", null);
        }
        root.addProperty("model", idx.model == null ? "classic" : idx.model);
        JsonArray arr = new JsonArray();
        for (Entry e : idx.skins) {
            JsonObject o = new JsonObject();
            o.addProperty("id", e.id);
            o.addProperty("name", e.name);
            o.addProperty("file", e.file);
            o.addProperty("model", e.model == null ? "classic" : e.model);
            if (e.source != null) o.addProperty("source", e.source);
            if (e.ign != null) o.addProperty("ign", e.ign);
            if (e.uuid != null) o.addProperty("uuid", e.uuid);
            arr.add(o);
        }
        root.add("skins", arr);
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(new File(dir, "index.json")), StandardCharsets.UTF_8);
            GSON.toJson(root, writer);
        } catch (Exception ignored) {
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static Entry addPng(byte[] png, String name, String model, String source, String ign, String uuid) {
        File dir = resolveSkinsDir();
        dir.mkdirs();
        Index idx = load();
        Entry e = new Entry();
        e.id = UUID.randomUUID().toString();
        e.name = name == null || name.isEmpty() ? "skin" : name;
        String safe = e.name.replaceAll("[^a-zA-Z0-9._-]+", "_");
        if (safe.length() > 48) safe = safe.substring(0, 48);
        e.file = e.id + "-" + safe + ".png";
        e.model = "slim".equals(model) ? "slim" : "classic";
        e.source = source;
        e.ign = ign;
        e.uuid = uuid;
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(e.resolveFile(dir));
            out.write(png);
        } catch (Exception ex) {
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
            }
        }
        idx.skins.add(0, e);
        idx.activeId = e.id;
        idx.model = e.model;
        save(idx);
        return e;
    }

    public static Entry find(Index idx, String id) {
        if (idx == null || id == null) return null;
        for (Entry e : idx.skins) {
            if (id.equals(e.id)) return e;
        }
        return null;
    }

    public static void setActive(String id) {
        Index idx = load();
        if (id != null && find(idx, id) == null) return;
        idx.activeId = id;
        save(idx);
    }

    public static void setModel(String model) {
        Index idx = load();
        idx.model = "slim".equals(model) ? "slim" : "classic";
        Entry active = find(idx, idx.activeId);
        if (active != null) {
            active.model = idx.model;
        }
        save(idx);
    }

    public static void delete(String id) {
        Index idx = load();
        Entry e = find(idx, id);
        if (e == null) return;
        File f = e.resolveFile(resolveSkinsDir());
        if (f.isFile()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
        List<Entry> next = new ArrayList<Entry>();
        for (Entry skin : idx.skins) {
            if (!id.equals(skin.id)) next.add(skin);
        }
        idx.skins.clear();
        idx.skins.addAll(next);
        if (id.equals(idx.activeId)) {
            idx.activeId = idx.skins.isEmpty() ? null : idx.skins.get(0).id;
        }
        save(idx);
    }

    private static String getStr(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) return null;
        return o.get(key).getAsString();
    }
}
