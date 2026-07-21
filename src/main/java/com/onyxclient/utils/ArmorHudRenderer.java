package com.onyxclient.utils;

import net.minecraft.item.ItemStack;

public final class ArmorHudRenderer {

    private ArmorHudRenderer() {
    }

    public static int remaining(ItemStack stack) {
        if (stack == null || !stack.isItemStackDamageable()) {
            return 0;
        }
        return Math.max(0, stack.getMaxDamage() - stack.getItemDamage());
    }

    public static int percent(ItemStack stack) {
        if (stack == null || !stack.isItemStackDamageable() || stack.getMaxDamage() <= 0) {
            return 100;
        }
        return (int) (remaining(stack) * 100.0F / stack.getMaxDamage());
    }

    public static int tierColor(int pct, int highest, int high, int medium, int mediumLow, int low, int lowest) {
        if (pct >= 95) {
            return highest;
        }
        if (pct >= 75) {
            return high;
        }
        if (pct >= 55) {
            return medium;
        }
        if (pct >= 35) {
            return mediumLow;
        }
        if (pct >= 15) {
            return low;
        }
        return lowest;
    }
}
