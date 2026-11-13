package com.aulaclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Kleine Zeichenhelfer: abgerundete Rechtecke, Toggle, Icons. Alles aus fill()-Aufrufen. */
final class Ui {
    private Ui() {}

    /** Gefuelltes Rechteck mit abgerundeten Ecken (Radius r). Farbe sollte undurchsichtig sein. */
    static void rr(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * (double) r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    /** Rechteck mit 1px Rand. */
    static void rrBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int fill, int border) {
        rr(g, x, y, w, h, r, border);
        rr(g, x + 1, y + 1, w - 2, h - 2, Math.max(r - 1, 0), fill);
    }

    /** Toggle-Schalter 24x12. */
    static void toggle(GuiGraphicsExtractor g, int x, int y, boolean on, int onColor) {
        rr(g, x, y, 24, 12, 6, on ? onColor : 0xFF34343F);
        int kx = on ? x + 14 : x + 2;
        rr(g, kx, y + 2, 8, 8, 4, on ? 0xFF15151C : 0xFFC8C8D2);
    }

    /** Kleines X (7x7). */
    static void cross(GuiGraphicsExtractor g, int x, int y, int color) {
        for (int i = 0; i < 7; i++) {
            g.fill(x + i, y + i, x + i + 1, y + i + 1, color);
            g.fill(x + 6 - i, y + i, x + 7 - i, y + i + 1, color);
        }
    }

    /** Pfeil nach unten (7x4), x/y = linke obere Ecke. */
    static void chevron(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x, y, x + 7, y + 1, color);
        g.fill(x + 1, y + 1, x + 6, y + 2, color);
        g.fill(x + 2, y + 2, x + 5, y + 3, color);
        g.fill(x + 3, y + 3, x + 4, y + 4, color);
    }

    /** Lupe (ca. 10x10). */
    static void magnifier(GuiGraphicsExtractor g, int x, int y, int bg, int color) {
        rrBorder(g, x, y, 8, 8, 4, bg, color);
        for (int i = 0; i < 3; i++) {
            g.fill(x + 7 + i, y + 7 + i, x + 9 + i, y + 9 + i, color);
        }
    }
}
