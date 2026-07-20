package com.onyxclient.modules.settings;

public class ModeSetting extends Setting<String> {

    private final String[] modes;

    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes = modes;
    }

    public String[] getModes() {
        return modes;
    }

    public void cycle() {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(getValue())) {
                setValue(modes[(i + 1) % modes.length]);
                return;
            }
        }
        setValue(modes[0]);
    }
}
