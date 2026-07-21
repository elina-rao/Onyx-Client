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

    public void setRed(int red) {
        setRGBA(clamp(red), getGreen(), getBlue(), getAlpha());
    }

    public void setGreen(int green) {
        setRGBA(getRed(), clamp(green), getBlue(), getAlpha());
    }

    public void setBlue(int blue) {
        setRGBA(getRed(), getGreen(), clamp(blue), getAlpha());
    }

    public void setAlpha(int alpha) {
        setRGBA(getRed(), getGreen(), getBlue(), clamp(alpha));
    }

    public void setRGBA(int red, int green, int blue, int alpha) {
        setValue((clamp(alpha) << 24) | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
