package com.onyxclient.modules;

import com.onyxclient.OnyxClient;
import com.onyxclient.core.ModuleToggleEvent;
import com.onyxclient.modules.settings.Setting;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {

    private final String name;
    private final String description;
    private final ModuleCategory category;
    private boolean enabled;
    private int keybind = -1;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();

    protected Module(String name, String description, ModuleCategory category) {
        this(name, description, category, false);
    }

    protected Module(String name, String description, ModuleCategory category, boolean defaultEnabled) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = defaultEnabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
        OnyxClient.getEventBus().post(new ModuleToggleEvent(this, enabled));
        OnyxClient.getConfigManager().saveModule(this);
    }

    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick(TickEvent.ClientTickEvent event) {
    }

    public void onRender2D(float partialTicks) {
    }

    public void onRender3D(float partialTicks) {
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKeybind() {
        return keybind;
    }

    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }
}
