package com.aulaclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

/** Kleine Zeichenhelfer: abgerundete Rechtecke, Toggle, Icons. Alles aus fill()-Aufrufen. */
final class Ui {
    private Ui() {}

    /** Kleiner Klick-Sound fuer Menue-Interaktionen (nur wenn aktiviert). */
    static void click(float pitch) {
        if (!com.aulaclient.config.AulaConfig.get().menuSounds) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch, 0.5f));
        } catch (Throwable ignored) { }
    }

    static void click() { click(1.0f); }

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

    private static int inset(int i, int r) {
        double dy = r - i - 0.5;
        return r - (int) Math.round(Math.sqrt(r * (double) r - dy * dy));
    }

    /** Nur der 1px-Rand eines abgerundeten Rechtecks (Mitte bleibt unberuehrt -> Fuellung darf durchsichtig sein). */
    static void rrRing(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if (w <= 2 || h <= 2) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 1) {
            g.fill(x, y, x + w, y + 1, color);
            g.fill(x, y + h - 1, x + w, y + h, color);
            g.fill(x, y + 1, x + 1, y + h - 1, color);
            g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            int io = inset(i, r);
            int top = y + i, bot = y + h - 1 - i;
            if (i == 0) {
                g.fill(x + io, top, x + w - io, top + 1, color);
                g.fill(x + io, bot, x + w - io, bot + 1, color);
            } else {
                int ii = 1 + inset(i - 1, r - 1);
                int l = Math.max(ii, io + 1);
                g.fill(x + io, top, x + l, top + 1, color);
                g.fill(x + w - l, top, x + w - io, top + 1, color);
                g.fill(x + io, bot, x + l, bot + 1, color);
                g.fill(x + w - l, bot, x + w - io, bot + 1, color);
            }
        }
        g.fill(x, y + r, x + 1, y + h - r, color);
        g.fill(x + w - 1, y + r, x + w, y + h - r, color);
    }

    /** Rechteck mit Fuellung und 1px Rand; Fuellung und Rand duerfen durchsichtig sein. */
    static void rrBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int fill, int border) {
        rr(g, x, y, w, h, r, fill);
        rrRing(g, x, y, w, h, r, border);
    }

    /** rgb + Alpha (0..1) -> ARGB. */
    static int argb(int rgb, float alpha) {
        return ((int) (Math.max(0f, Math.min(1f, alpha)) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    // ---- Logo (assets/aula-client/textures/gui/logo.png, 468x427)
    static final Identifier LOGO = Identifier.fromNamespaceAndPath("aula-client", "textures/gui/logo.png");
    static final int LOGO_W = 468, LOGO_H = 427;

    static final Identifier HORNS = Identifier.fromNamespaceAndPath("aula-client", "textures/gui/horns.png");

    /** Hoerner-Cosmetic (250x250) in Rechteck (x, y, w, h). */
    static void horns(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, HORNS, x, y, 0f, 0f, w, h, 250, 250, 250, 250);
    }

    /** Zeichnet das Logo skaliert in das Rechteck (x, y, w, h). */
    static void logo(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, x, y, 0f, 0f, w, h, LOGO_W, LOGO_H, LOGO_W, LOGO_H);
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
