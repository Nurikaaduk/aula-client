package com.aulaclient.gui;

import com.aulaclient.AulaClient;
import com.aulaclient.config.AulaConfig;
import com.aulaclient.config.Themes;
import com.aulaclient.module.HudModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Aula-Menue mit Tabs: Module, Cosmetics, Themes.
 * Alle Klickflaechen werden pro Frame neu registriert.
 */
public class AulaScreen extends Screen {
    private static final int PANEL_W = 400;
    private static final int PANEL_H = 250;
    private static final int BG = 0xEE12121A;
    private static final int SIDE = 0xFF0C0C12;
    private static final int CARD = 0xFF1C1C28;
    private static final int CARD_HOVER = 0xFF262637;
    private static final int OFF = 0xFF3A3A4D;
    private static final String[] TABS = {"Module", "Crosshair", "Charakter", "Display", "Themes"};
    private static final String[] STYLES = {"Kreuz", "Punkt", "Plus", "Quadrat"};

    private static int tab = 0;
    private int scroll = 0;

    private record Region(int x, int y, int w, int h, Runnable action) {}

    private final List<Region> regions = new ArrayList<>();

    public AulaScreen() {
        super(Component.literal(AulaClient.NAME));
    }

    private int left() { return (width - PANEL_W) / 2; }
    private int top() { return (height - PANEL_H) / 2; }

    private void region(int x, int y, int w, int h, Runnable r) {
        regions.add(new Region(x, y, w, h, r));
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void button(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, int h, String label, boolean active, Runnable r) {
        int accent = AulaConfig.get().accent;
        boolean hov = over(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, active ? accent : (hov ? CARD_HOVER : CARD));
        int tw = font.width(label);
        g.text(font, label, x + (w - tw) / 2, y + (h - 8) / 2, active ? 0xFF101018 : 0xFFFFFFFF, false);
        region(x, y, w, h, r);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        regions.clear();
        AulaConfig cfg = AulaConfig.get();
        int accent = cfg.accent;
        int l = left(), t = top();

        g.fill(0, 0, width, height, 0x70000000);
        g.fill(l, t, l + PANEL_W, t + PANEL_H, BG);
        g.fill(l, t, l + 96, t + PANEL_H, SIDE);
        g.fill(l, t, l + PANEL_W, t + 2, accent);

        g.text(font, "AULA", l + 14, t + 14, accent, true);
        g.text(font, "CLIENT", l + 14, t + 25, 0xFFFFFFFF, true);

        // Tabs
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            int ty = t + 52 + i * 22;
            boolean sel = tab == i;
            boolean hov = over(mx, my, l + 6, ty, 84, 18);
            if (sel) g.fill(l + 6, ty, l + 90, ty + 18, CARD);
            else if (hov) g.fill(l + 6, ty, l + 90, ty + 18, 0xFF15151F);
            if (sel) g.fill(l + 6, ty, l + 8, ty + 18, accent);
            g.text(font, TABS[i], l + 16, ty + 5, sel ? 0xFFFFFFFF : 0xFF9090A8, false);
            region(l + 6, ty, 84, 18, () -> { tab = idx; scroll = 0; });
        }

        button(g, mx, my, l + 6, t + PANEL_H - 52, 84, 18, "HUD-Editor", false,
                () -> minecraft.setScreenAndShow(new HudEditorScreen()));
        g.text(font, "v1.2.0", l + 14, t + PANEL_H - 22, 0xFF606078, false);

        int cx = l + 106, cy = t + 14, cw = PANEL_W - 106 - 14;
        switch (tab) {
            case 0 -> renderModules(g, mx, my, cx, cy, cw);
            case 1 -> renderCosmetics(g, mx, my, cx, cy, cw);
            case 2 -> renderCharacter(g, mx, my, cx, cy, cw);
            case 3 -> renderDisplay(g, mx, my, cx, cy, cw);
            default -> renderThemes(g, mx, my, cx, cy, cw);
        }
    }

    private void renderModules(GuiGraphicsExtractor g, int mx, int my, int cx, int cy, int cw) {
        int accent = AulaConfig.get().accent;
        List<HudModule> mods = AulaClient.modules.modules;
        int visible = 6;
        int max = Math.max(0, mods.size() - visible);
        scroll = Math.min(Math.max(scroll, 0), max);

        for (int i = 0; i < visible && i + scroll < mods.size(); i++) {
            HudModule m = mods.get(i + scroll);
            int y = cy + i * 36;
            boolean hov = over(mx, my, cx, y, cw, 32);
            g.fill(cx, y, cx + cw, y + 32, hov ? CARD_HOVER : CARD);
            g.text(font, m.name, cx + 10, y + 7, 0xFFFFFFFF, true);
            g.text(font, m.description, cx + 10, y + 19, 0xFF8888A0, false);

            int tx = cx + cw - 36, ty = y + 9;
            g.fill(tx, ty, tx + 26, ty + 14, m.enabled ? accent : OFF);
            int knob = m.enabled ? tx + 14 : tx + 2;
            g.fill(knob, ty + 2, knob + 10, ty + 12, 0xFFFFFFFF);
            region(cx, y, cw, 32, m::toggle);
        }
        if (mods.size() > visible) {
            g.text(font, "Mausrad: scrollen (" + (scroll + 1) + "-" +
                    Math.min(scroll + visible, mods.size()) + " / " + mods.size() + ")",
                    cx, top() + PANEL_H - 14, 0xFF606078, false);
        }
    }

    private void renderCosmetics(GuiGraphicsExtractor g, int mx, int my, int cx, int cy, int cw) {
        AulaConfig c = AulaConfig.get();
        g.text(font, "Crosshair", cx, cy, 0xFFFFFFFF, true);
        g.text(font, "Aktivieren im Tab \"Module\" (Modul: Crosshair)", cx, cy + 12, 0xFF8888A0, false);

        g.text(font, "Stil", cx, cy + 32, 0xFFB0B0C0, false);
        int bw = (cw - 3 * 4) / 4;
        for (int i = 0; i < STYLES.length; i++) {
            final int s = i;
            button(g, mx, my, cx + i * (bw + 4), cy + 44, bw, 18, STYLES[i], c.crosshairStyle == i,
                    () -> { c.crosshairStyle = s; AulaConfig.save(); });
        }

        g.text(font, "Groesse: " + c.crosshairSize, cx, cy + 76, 0xFFB0B0C0, false);
        button(g, mx, my, cx + 90, cy + 72, 18, 16, "-", false,
                () -> { c.crosshairSize = Math.max(2, c.crosshairSize - 1); AulaConfig.save(); });
        button(g, mx, my, cx + 112, cy + 72, 18, 16, "+", false,
                () -> { c.crosshairSize = Math.min(12, c.crosshairSize + 1); AulaConfig.save(); });

        button(g, mx, my, cx, cy + 98, 150, 18,
                c.crosshairAccent ? "Farbe: Akzentfarbe" : "Farbe: Weiss", false,
                () -> { c.crosshairAccent = !c.crosshairAccent; AulaConfig.save(); });

        // Vorschau
        int px = cx + cw - 70, py = cy + 150;
        g.fill(px - 40, py - 40, px + 40, py + 40, 0xFF08080D);
        int col = c.crosshairAccent ? c.accent : 0xFFFFFFFF;
        previewCrosshair(g, px, py, c, col);

    }

    private void renderCharacter(GuiGraphicsExtractor g, int mx, int my, int cx, int cy, int cw) {
        AulaConfig c = AulaConfig.get();
        var fx = AulaClient.modules.characterFx;
        g.text(font, "Charakter-Effekt", cx, cy, 0xFFFFFFFF, true);
        g.text(font, "Partikel um deinen Charakter, nur fuer dich sichtbar (F5 = Third Person)",
                cx, cy + 12, 0xFF8888A0, false);

        button(g, mx, my, cx, cy + 28, cw, 18, fx.enabled ? "Effekt: AN" : "Effekt: AUS", fx.enabled, fx::toggle);

        g.text(font, "Stil", cx, cy + 56, 0xFFB0B0C0, false);
        String[] styles = com.aulaclient.module.ModuleManager.CharacterFx.STYLE_NAMES;
        int bw = (cw - 3 * 4) / 4;
        for (int i = 0; i < styles.length; i++) {
            final int s = i;
            button(g, mx, my, cx + i * (bw + 4), cy + 68, bw, 18, styles[i], c.fxStyle == i,
                    () -> { c.fxStyle = s; AulaConfig.save(); });
        }

        g.text(font, "Partikel", cx, cy + 96, 0xFFB0B0C0, false);
        String[] parts = com.aulaclient.module.ModuleManager.CharacterFx.PARTICLE_NAMES;
        int pw = (cw - 3 * 4) / 4;
        for (int i = 0; i < parts.length; i++) {
            final int p = i;
            int row = i / 4, col = i % 4;
            button(g, mx, my, cx + col * (pw + 4), cy + 108 + row * 22, pw, 18, parts[i], c.fxParticle == i,
                    () -> { c.fxParticle = p; AulaConfig.save(); });
        }

        g.text(font, "Dichte: " + c.fxDensity, cx, cy + 160, 0xFFB0B0C0, false);
        button(g, mx, my, cx + 70, cy + 156, 18, 16, "-", false,
                () -> { c.fxDensity = Math.max(1, c.fxDensity - 1); AulaConfig.save(); });
        button(g, mx, my, cx + 92, cy + 156, 18, 16, "+", false,
                () -> { c.fxDensity = Math.min(4, c.fxDensity + 1); AulaConfig.save(); });
    }

    private void renderDisplay(GuiGraphicsExtractor g, int mx, int my, int cx, int cy, int cw) {
        AulaConfig c = AulaConfig.get();
        var st = AulaClient.modules.stretch;
        var zm = AulaClient.modules.zoom;

        g.text(font, "Stretched Resolution", cx, cy, 0xFFFFFFFF, true);
        g.text(font, "Streckt die Spielwelt auf ein anderes Seitenverhaeltnis", cx, cy + 12, 0xFF8888A0, false);
        button(g, mx, my, cx, cy + 28, cw, 18, st.enabled ? "Stretch: AN" : "Stretch: AUS", st.enabled, st::toggle);

        String[] names = {"4:3", "5:4", "1:1", "16:10", "3:2"};
        float[] ratios = {4f / 3f, 5f / 4f, 1f, 16f / 10f, 3f / 2f};
        int bw = (cw - 4 * 4) / 5;
        for (int i = 0; i < names.length; i++) {
            final float r = ratios[i];
            button(g, mx, my, cx + i * (bw + 4), cy + 52, bw, 18, names[i],
                    Math.abs(c.stretchRatio - r) < 0.005f,
                    () -> { c.stretchRatio = r; AulaConfig.save(); });
        }
        g.text(font, String.format("Ratio: %.2f", c.stretchRatio), cx, cy + 82, 0xFFB0B0C0, false);
        button(g, mx, my, cx + 90, cy + 78, 18, 16, "-", false,
                () -> { c.stretchRatio = Math.max(0.5f, c.stretchRatio - 0.05f); AulaConfig.save(); });
        button(g, mx, my, cx + 112, cy + 78, 18, 16, "+", false,
                () -> { c.stretchRatio = Math.min(2.5f, c.stretchRatio + 0.05f); AulaConfig.save(); });

        g.text(font, "Zoom", cx, cy + 112, 0xFFFFFFFF, true);
        button(g, mx, my, cx, cy + 126, cw, 18, zm.enabled ? "Zoom: AN (Taste C)" : "Zoom: AUS", zm.enabled, zm::toggle);
        g.text(font, "Zoom-FOV: " + c.zoomFov, cx, cy + 156, 0xFFB0B0C0, false);
        button(g, mx, my, cx + 90, cy + 152, 18, 16, "-", false,
                () -> { c.zoomFov = Math.max(30, c.zoomFov - 5); AulaConfig.save(); });
        button(g, mx, my, cx + 112, cy + 152, 18, 16, "+", false,
                () -> { c.zoomFov = Math.min(70, c.zoomFov + 5); AulaConfig.save(); });
    }

    private void previewCrosshair(GuiGraphicsExtractor g, int cx, int cy, AulaConfig c, int col) {
        int s = c.crosshairSize;
        switch (c.crosshairStyle) {
            case 1 -> g.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
            case 2 -> {
                g.fill(cx - s, cy, cx + s + 1, cy + 1, col);
                g.fill(cx, cy - s, cx + 1, cy + s + 1, col);
            }
            case 3 -> {
                g.fill(cx - s, cy - s, cx + s + 1, cy - s + 1, col);
                g.fill(cx - s, cy + s, cx + s + 1, cy + s + 1, col);
                g.fill(cx - s, cy - s, cx - s + 1, cy + s + 1, col);
                g.fill(cx + s, cy - s, cx + s + 1, cy + s + 1, col);
            }
            default -> {
                int gap = 2;
                g.fill(cx - s - gap, cy - 1, cx - gap, cy + 1, col);
                g.fill(cx + gap + 1, cy - 1, cx + s + gap + 1, cy + 1, col);
                g.fill(cx - 1, cy - s - gap, cx + 1, cy - gap, col);
                g.fill(cx - 1, cy + gap + 1, cx + 1, cy + s + gap + 1, col);
            }
        }
    }

    private void renderThemes(GuiGraphicsExtractor g, int mx, int my, int cx, int cy, int cw) {
        AulaConfig c = AulaConfig.get();
        g.text(font, "Farbthema", cx, cy, 0xFFFFFFFF, true);
        g.text(font, "Akzentfarbe fuer Menue, HUD und Crosshair", cx, cy + 12, 0xFF8888A0, false);
        for (int i = 0; i < Themes.NAMES.length; i++) {
            final int idx = i;
            int y = cy + 32 + i * 26;
            boolean sel = c.theme == i;
            boolean hov = over(mx, my, cx, y, cw, 22);
            g.fill(cx, y, cx + cw, y + 22, hov || sel ? CARD_HOVER : CARD);
            g.fill(cx + 6, y + 4, cx + 20, y + 18, Themes.COLORS[i]);
            g.text(font, Themes.NAMES[i], cx + 28, y + 7, 0xFFFFFFFF, false);
            if (sel) g.text(font, "aktiv", cx + cw - 36, y + 7, Themes.COLORS[i], false);
            region(cx, y, cw, 22, () -> Themes.apply(idx));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (int i = regions.size() - 1; i >= 0; i--) {
                Region r = regions.get(i);
                if (over((int) event.x(), (int) event.y(), r.x, r.y, r.w, r.h)) {
                    r.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double hx, double vy) {
        if (tab == 0) {
            scroll -= (int) Math.signum(vy);
            return true;
        }
        return super.mouseScrolled(x, y, hx, vy);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
