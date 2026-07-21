package com.onyxclient.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;

/**
 * Local-player skin override via DynamicTexture + NetworkPlayerInfo reflection
 * (works even when Mixin is not loaded as a coremod).
 */
public final class SkinOverrideManager {

    public static final SkinOverrideManager INSTANCE = new SkinOverrideManager();

    private ResourceLocation location;
    private String skinType = "default";
    private String loadedPath;
    private DynamicTexture texture;
    private Field locationSkinField;
    private Field skinTypeField;
    private boolean fieldsResolved;
    private boolean registered;

    private SkinOverrideManager() {
    }

    public void register() {
        if (registered) return;
        MinecraftForge.EVENT_BUS.register(this);
        registered = true;
    }

    public ResourceLocation getOverrideLocation() {
        return location;
    }

    public String getOverrideSkinType() {
        return skinType;
    }

    public boolean hasOverride() {
        return location != null;
    }

    public synchronized void clear() {
        location = null;
        loadedPath = null;
        skinType = "default";
        texture = null;
    }

    public synchronized boolean applyEntry(SkinLibrary.Entry entry) {
        if (entry == null) {
            clear();
            return false;
        }
        File file = entry.resolveFile(SkinLibrary.resolveSkinsDir());
        if (!file.isFile()) {
            clear();
            return false;
        }
        String path = file.getAbsolutePath();
        if (path.equals(loadedPath) && location != null) {
            skinType = "slim".equals(entry.model) ? "slim" : "default";
            return true;
        }
        try {
            BufferedImage image = ImageIO.read(new FileInputStream(file));
            if (image == null) {
                clear();
                return false;
            }
            Minecraft mc = Minecraft.getMinecraft();
            texture = new DynamicTexture(image);
            location = mc.getTextureManager().getDynamicTextureLocation("onyx_skin_" + entry.id, texture);
            loadedPath = path;
            skinType = "slim".equals(entry.model) ? "slim" : "default";
            return true;
        } catch (Exception e) {
            clear();
            return false;
        }
    }

    public void applyActiveFromLibrary() {
        SkinLibrary.Index idx = SkinLibrary.load();
        SkinLibrary.Entry entry = SkinLibrary.find(idx, idx.activeId);
        if (entry != null) {
            applyEntry(entry);
        } else {
            clear();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (location == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        pushOntoPlayer(mc.thePlayer);
    }

    private void ensureFields(NetworkPlayerInfo info) {
        if (fieldsResolved) return;
        fieldsResolved = true;
        try {
            for (Field f : NetworkPlayerInfo.class.getDeclaredFields()) {
                if (f.getType() == ResourceLocation.class && locationSkinField == null) {
                    // First ResourceLocation is typically locationSkin; second may be cape
                    if (locationSkinField == null) {
                        f.setAccessible(true);
                        locationSkinField = f;
                    } else if (f != locationSkinField) {
                        // keep skin field as first ResourceLocation found in declaration order
                    }
                }
                if (f.getType() == String.class && (f.getName().toLowerCase().contains("skin") || f.getName().equals("field_178862_f"))) {
                    f.setAccessible(true);
                    skinTypeField = f;
                }
            }
            // Prefer known SRG / MCP names when present
            try {
                Field skin = NetworkPlayerInfo.class.getDeclaredField("locationSkin");
                skin.setAccessible(true);
                locationSkinField = skin;
            } catch (NoSuchFieldException ignored) {
                try {
                    Field skin = NetworkPlayerInfo.class.getDeclaredField("field_178865_e");
                    skin.setAccessible(true);
                    locationSkinField = skin;
                } catch (NoSuchFieldException ignored2) {
                }
            }
            try {
                Field type = NetworkPlayerInfo.class.getDeclaredField("skinType");
                type.setAccessible(true);
                skinTypeField = type;
            } catch (NoSuchFieldException ignored) {
                try {
                    Field type = NetworkPlayerInfo.class.getDeclaredField("field_178863_g");
                    type.setAccessible(true);
                    skinTypeField = type;
                } catch (NoSuchFieldException ignored2) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void pushOntoPlayer(AbstractClientPlayer player) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getNetHandler() == null) return;
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getGameProfile().getId());
            if (info == null) return;
            ensureFields(info);
            if (locationSkinField != null) {
                locationSkinField.set(info, location);
            }
            if (skinTypeField != null) {
                skinTypeField.set(info, skinType);
            }
        } catch (Exception ignored) {
        }
    }
}
