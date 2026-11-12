package com.aulaclient.config;

/** Farbthemen (Akzentfarben). */
public final class Themes {
    public static final String[] NAMES = {
            "Violett", "Ozean", "Mint", "Sonnenuntergang", "Rose", "Gold", "Schnee"
    };
    public static final int[] COLORS = {
            0xFF7C5CFF, 0xFF2E9BFF, 0xFF2FD6A0, 0xFFFF7A3D, 0xFFFF5C93, 0xFFFFC53D, 0xFFE8EAF2
    };

    private Themes() {}

    public static void apply(int index) {
        AulaConfig c = AulaConfig.get();
        c.theme = Math.floorMod(index, COLORS.length);
        c.accent = COLORS[c.theme];
        AulaConfig.save();
    }
}
