package com.onyxclient.modules.settings;

public class ColorSetting extends Setting<Integer> {

    public ColorSetting(String name, int defaultColor) {
        super(name, defaultColor);
    }

    public int getRed() {
        return (getValue() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getValue() >> 8) & 0xFF;
    }

    public int getBlue() {
        return getValue() & 0xFF;
    }

    public int getAlpha() {
        return (getValue() >> 24) & 0xFF;
    }
}
