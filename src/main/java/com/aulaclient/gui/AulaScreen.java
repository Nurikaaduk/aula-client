package com.aulaclient.gui;

import com.aulaclient.AulaClient;
import com.aulaclient.config.AulaConfig;
import com.aulaclient.config.Themes;
import com.aulaclient.module.HudModule;
import com.aulaclient.module.Setting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;

/**
 * Aula-Menue: abgerundetes dunkles Panel, Sidebar mit Kategorien, Suchleiste,
 * Modul-Karten (Favorit-Stern + Toggle) und ein Einstellungs-Panel rechts daneben.
 * Eigene Implementierung; alle Klickflaechen werden pro Frame neu registriert.
 */
public class AulaScreen extends Screen {
    private static final String[] PAGES = {"Favorites", "HUD", "Visuals", "Cosmetics"};
    private static String page = "Visuals";

    private static final int PANEL = 0xF2101016;
    private static final int BORDER = 0x55FFFFFF;
    private static final int CARD = 0xFF18181F;
    private static final int CARD_HOVER = 0xFF20202A;
    private static final int FIELD = 0xFF0E0E13;
    private static final int TXT = 0xFFF2F2F7;
    private static final int TXT2 = 0xFF8B8B9A;
    private static final int TXT3 = 0xFF5E5E6B;
    private static final int LINE = 0x22FFFFFF;
    private static final int SETTINGS_W = 170;

    private record Region(int x, int y, int w, int h, DoubleConsumer click) {}

    private final List<Region> regions = new ArrayList<>();

    private HudModule selected;
    private String query = "";
    private boolean searchFocused;
    private int scroll;

    private Setting.Slider dragSlider;
    private int dragX, dragW;

    private Setting.Choice openChoice;
    private int choiceX, choiceY, choiceW;

    // Layout (pro Frame berechnet, fuer Scroll-Hit-Tests)
    private int px, py, pw, ph, cx, cw, ay, ah;

    private final Screen parent;

    public AulaScreen() {
        this(null);
    }

    public AulaScreen(Screen parent) {
        super(Component.literal(AulaClient.NAME));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    // ---------------------------------------------------------------- helpers

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void region(int x, int y, int w, int h, Runnable r) {
        regions.add(new Region(x, y, w, h, mx -> r.run()));
    }

    private void regionX(int x, int y, int w, int h, DoubleConsumer c) {
        regions.add(new Region(x, y, w, h, c));
    }

    private void setPage(String p) {
        page = p;
        scroll = 0;
        selected = null;
        query = "";
        searchFocused = false;
        openChoice = null;
    }

    private List<HudModule> visibleModules() {
        AulaConfig cfg = AulaConfig.get();
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<HudModule> out = new ArrayList<>();
        for (HudModule m : AulaClient.modules.modules) {
            if (!q.isEmpty()) {
                if (m.name.toLowerCase(Locale.ROOT).contains(q)
                        || m.description.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
            } else if (page.equals("Favorites")) {
                if (cfg.favorites.contains(m.name)) out.add(m);
            } else if (m.category.equals(page)) {
                out.add(m);
            }
        }
        return out;
    }

    private String fit(String text, int maxW) {
        if (font.width(text) <= maxW) return text;
        int dots = font.width("...");
        return font.plainSubstrByWidth(text, Math.max(0, maxW - dots)) + "...";
    }

    // ---------------------------------------------------------------- render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        regions.clear();
        AulaConfig cfg = AulaConfig.get();
        int accent = cfg.accent;

        boolean panelOpen = selected != null && !page.equals("Settings");
        pw = Math.max(340, Math.min(470, width - 40 - SETTINGS_W - 8));
        ph = Math.max(220, Math.min(280, height - 30));
        int total = panelOpen ? pw + 8 + SETTINGS_W : pw;
        px = (width - total) / 2;
        py = (height - ph) / 2;
        cx = px + 104;
        cw = pw - 104 - 10;
        ay = py + 32;
        ah = ph - 32 - 24;

        g.fill(0, 0, width, height, 0x66000000);
        Ui.rrBorder(g, px, py, pw, ph, 7, PANEL, BORDER);
        g.fill(px + 96, py + 8, px + 97, py + ph - 8, LINE);

        renderSidebar(g, mx, my, accent);

        if (page.equals("Settings")) {
            renderThemes(g, mx, my);
        } else {
            renderModules(g, mx, my, accent);
        }

        if (panelOpen) {
            renderSettingsPanel(g, mx, my, px + pw + 8, py, SETTINGS_W, ph, accent);
        }

        if (openChoice != null) {
            renderChoiceOverlay(g, mx, my);
        }
    }

    private void navItem(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w,
                         String label, boolean sel, Runnable r) {
        boolean hov = over(mx, my, x, y, w, 18);
        if (sel) Ui.rrBorder(g, x, y, w, 18, 4, 0xFF1D1D26, 0x66FFFFFF);
        else if (hov) Ui.rr(g, x, y, w, 18, 4, 0xFF15151C);
        g.text(font, label, x + 10, y + 5, sel ? TXT : TXT2, false);
        region(x, y, w, 18, r);
    }

    private void renderSidebar(GuiGraphicsExtractor g, int mx, int my, int accent) {
        // Logo-Marke + Titel
        Ui.rr(g, px + 10, py + 10, 16, 16, 4, accent);
        g.text(font, "A", px + 16, py + 14, 0xFF111116, false);
        g.text(font, "Aula Client", px + 32, py + 10, TXT, false);
        g.text(font, "Version 1.3.0", px + 32, py + 20, TXT3, false);

        g.text(font, "FEATURES", px + 10, py + 38, TXT3, false);
        for (int i = 0; i < PAGES.length; i++) {
            final String p = PAGES[i];
            navItem(g, mx, my, px + 6, py + 50 + i * 20, 84, p, page.equals(p) && !"Settings".equals(page), () -> setPage(p));
        }

        g.text(font, "OTHERS", px + 10, py + 138, TXT3, false);
        navItem(g, mx, my, px + 6, py + 150, 84, "Editor", false,
                () -> minecraft.setScreenAndShow(new HudEditorScreen(parent)));
        navItem(g, mx, my, px + 6, py + 170, 84, "Settings", page.equals("Settings"), () -> setPage("Settings"));

        g.text(font, fit(minecraft.getUser().getName(), 80), px + 10, py + ph - 16, TXT2, false);
    }

    // ---------------------------------------------------------------- modules page

    private void renderModules(GuiGraphicsExtractor g, int mx, int my, int accent) {
        AulaConfig cfg = AulaConfig.get();

        // Suchleiste
        Ui.rrBorder(g, cx, py + 8, cw, 18, 5, FIELD, searchFocused ? 0x88FFFFFF : 0x33FFFFFF);
        Ui.magnifier(g, cx + 7, py + 12, FIELD, TXT2);
        boolean empty = query.isEmpty();
        String shown = fit(empty ? "Search modules..." : query, cw - 34);
        g.text(font, shown, cx + 22, py + 13, empty ? TXT3 : TXT, false);
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cxp = cx + 22 + (empty ? 0 : font.width(shown));
            g.fill(cxp, py + 12, cxp + 1, py + 22, TXT);
        }
        regionX(cx, py + 8, cw, 18, m -> searchFocused = true);

        // Karten
        List<HudModule> list = visibleModules();
        int cardW = (cw - 6) / 2;
        int pitch = 38;
        int rows = (list.size() + 1) / 2;
        int maxScroll = Math.max(0, rows * pitch - 6 - ah);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        g.enableScissor(cx, ay, cx + cw, ay + ah);
        for (int i = 0; i < list.size(); i++) {
            HudModule m = list.get(i);
            int x = cx + (i % 2) * (cardW + 6);
            int y = ay + (i / 2) * pitch - scroll;
            if (y + 32 < ay || y > ay + ah) continue;

            boolean hov = over(mx, my, x, y, cardW, 32) && over(mx, my, cx, ay, cw, ah);
            boolean sel = m == selected;
            int bg = hov ? CARD_HOVER : CARD;
            if (sel) Ui.rrBorder(g, x, y, cardW, 32, 5, bg, 0xAAFFFFFF);
            else Ui.rr(g, x, y, cardW, 32, 5, bg);

            g.text(font, m.name, x + 10, y + 6, TXT, false);
            g.text(font, fit(m.description, cardW - 20 - 60), x + 10, y + 18, TXT2, false);

            boolean fav = cfg.favorites.contains(m.name);
            g.text(font, fav ? "★" : "☆", x + cardW - 60, y + 12, fav ? 0xFFFFD166 : TXT2, false);
            Ui.toggle(g, x + cardW - 34, y + 10, m.enabled, accent);

            // Klickflaechen (auf den sichtbaren Bereich zugeschnitten)
            int top = Math.max(y, ay), bot = Math.min(y + 32, ay + ah);
            region(x, top, cardW, bot - top, () -> selected = m);
            int sTop = Math.max(y + 6, ay), sBot = Math.min(y + 26, ay + ah);
            if (sBot > sTop) {
                region(x + cardW - 66, sTop, 20, sBot - sTop, () -> {
                    if (!cfg.favorites.remove(m.name)) cfg.favorites.add(m.name);
                    AulaConfig.save();
                });
                region(x + cardW - 38, sTop, 32, sBot - sTop, m::toggle);
            }
        }
        g.disableScissor();

        if (list.isEmpty()) {
            String msg = page.equals("Favorites") && query.isEmpty()
                    ? "Click the star on a module to add it here"
                    : "No modules found";
            g.text(font, msg, cx + (cw - font.width(msg)) / 2, ay + 30, TXT3, false);
        }

        g.text(font, "Click a module to open its settings", cx, py + ph - 16, TXT3, false);
    }

    // ---------------------------------------------------------------- settings page (themes)

    private void renderThemes(GuiGraphicsExtractor g, int mx, int my) {
        AulaConfig cfg = AulaConfig.get();
        g.text(font, "Theme", cx, py + 12, TXT, false);
        g.text(font, "Accent color for toggles, sliders and the HUD", cx, py + 24, TXT2, false);

        int cardW = (cw - 6) / 2;
        for (int i = 0; i < Themes.NAMES.length; i++) {
            final int idx = i;
            int x = cx + (i % 2) * (cardW + 6);
            int y = py + 44 + (i / 2) * 28;
            boolean sel = cfg.theme == i;
            boolean hov = over(mx, my, x, y, cardW, 24);
            int bg = hov ? CARD_HOVER : CARD;
            if (sel) Ui.rrBorder(g, x, y, cardW, 24, 5, bg, 0xAAFFFFFF);
            else Ui.rr(g, x, y, cardW, 24, 5, bg);
            Ui.rr(g, x + 8, y + 6, 12, 12, 6, Themes.COLORS[i]);
            g.text(font, Themes.NAMES[i], x + 28, y + 8, TXT, false);
            if (sel) g.text(font, "active", x + cardW - 10 - font.width("active"), y + 8, TXT2, false);
            region(x, y, cardW, 24, () -> Themes.apply(idx));
        }

        // Titelbildschirm-Schalter
        int ty = py + 44 + 4 * 28 + 4;
        int rowW = cw;
        boolean on = cfg.customTitle;
        boolean hov = over(mx, my, cx, ty, rowW, 24);
        Ui.rr(g, cx, ty, rowW, 24, 5, hov ? CARD_HOVER : CARD);
        g.text(font, "Custom title screen", cx + 10, ty + 8, TXT, false);
        Ui.toggle(g, cx + rowW - 34, ty + 6, on, cfg.accent);
        region(cx, ty, rowW, 24, () -> {
            cfg.customTitle = !cfg.customTitle;
            AulaConfig.save();
        });
    }

    // ---------------------------------------------------------------- settings panel

    private void renderSettingsPanel(GuiGraphicsExtractor g, int mx, int my,
                                     int sx, int sy, int sw, int sh, int accent) {
        HudModule m = selected;
        Ui.rrBorder(g, sx, sy, sw, sh, 7, PANEL, BORDER);

        // Titel (vergroessert) + Schliessen
        g.pose().pushMatrix();
        g.pose().translate(sx + 12, sy + 10);
        g.pose().scale(1.4f, 1.4f);
        g.text(font, fit(m.name, (int) ((sw - 44) / 1.4f)), 0, 0, TXT, false);
        g.pose().popMatrix();
        boolean xHov = over(mx, my, sx + sw - 22, sy + 8, 14, 14);
        Ui.cross(g, sx + sw - 18, sy + 12, xHov ? TXT : TXT2);
        region(sx + sw - 22, sy + 8, 14, 14, () -> selected = null);

        // Enabled-Zeile
        g.text(font, m.enabled ? "Enabled" : "Disabled", sx + 12, sy + 30, TXT2, false);
        Ui.toggle(g, sx + sw - 36, sy + 28, m.enabled, accent);
        region(sx + sw - 38, sy + 26, 28, 16, m::toggle);
        g.fill(sx + 10, sy + 46, sx + sw - 10, sy + 47, LINE);

        // Beschreibung (umgebrochen)
        int y = sy + 54;
        for (FormattedCharSequence line : font.split(Component.literal(m.description), sw - 24)) {
            g.text(font, line, sx + 12, y, TXT2, false);
            y += 10;
        }
        y += 6;

        List<Setting> settings = m.settings();
        if (!settings.isEmpty()) {
            g.text(font, "SETTINGS", sx + 12, y, TXT3, false);
            g.fill(sx + 12 + font.width("SETTINGS") + 6, y + 4, sx + sw - 12, y + 5, LINE);
            y += 16;
            for (Setting s : settings) {
                renderSetting(g, mx, my, s, sx, sw, y, accent);
                y += 24;
            }
        }

        if (m.movable()) {
            int bx = sx + 12, by = sy + sh - 30, bw = sw - 24;
            boolean hov = over(mx, my, bx, by, bw, 20);
            Ui.rrBorder(g, bx, by, bw, 20, 5, hov ? CARD_HOVER : FIELD, 0x33FFFFFF);
            String label = "Reset position";
            g.text(font, label, bx + (bw - font.width(label)) / 2, by + 6, TXT2, false);
            region(bx, by, bw, 20, m::resetPosition);
        }
    }

    private void renderSetting(GuiGraphicsExtractor g, int mx, int my, Setting s,
                               int sx, int sw, int y, int accent) {
        int right = sx + sw - 12;
        g.text(font, s.label, sx + 12, y + 4, TXT, false);

        if (s instanceof Setting.Toggle t) {
            boolean on = t.get.getAsBoolean();
            Ui.toggle(g, right - 24, y + 2, on, accent);
            region(right - 26, y, 28, 16, () -> {
                t.set.accept(!on);
                AulaConfig.save();
            });
        } else if (s instanceof Setting.Slider sl) {
            int boxW = 32, trackW = 44;
            int trackX = right - boxW - 6 - trackW;
            Ui.rr(g, right - boxW, y - 1, boxW, 16, 4, FIELD);
            String val = sl.display();
            g.text(font, val, right - 5 - font.width(val), y + 4, TXT2, false);

            Ui.rr(g, trackX, y + 5, trackW, 4, 2, 0xFF34343F);
            int fill = (int) Math.round(trackW * sl.fraction());
            if (fill >= 2) Ui.rr(g, trackX, y + 5, fill, 4, 2, accent);
            Ui.rr(g, trackX + fill - 3, y + 3, 6, 8, 3, 0xFFEDEDF5);

            regionX(trackX - 4, y - 1, trackW + 8, 16, mouseX -> {
                dragSlider = sl;
                dragX = trackX;
                dragW = trackW;
                sl.setFromFraction((mouseX - trackX) / (double) trackW);
            });
        } else if (s instanceof Setting.Choice c) {
            int bw = 72, bx = right - bw;
            boolean open = openChoice == c;
            Ui.rrBorder(g, bx, y - 1, bw, 16, 4, FIELD, open ? 0x88FFFFFF : 0x33FFFFFF);
            int idx = Math.max(0, Math.min(c.options.length - 1, c.get.getAsInt()));
            g.text(font, fit(c.options[idx], bw - 22), bx + 6, y + 4, TXT, false);
            Ui.chevron(g, bx + bw - 12, y + 4, TXT2);
            region(bx, y - 1, bw, 16, () -> {
                openChoice = open ? null : c;
                choiceX = bx;
                choiceY = y + 17;
                choiceW = bw;
            });
        }
    }

    private void renderChoiceOverlay(GuiGraphicsExtractor g, int mx, int my) {
        Setting.Choice c = openChoice;
        int rowH = 14;
        int h = c.options.length * rowH + 4;
        int cy = Math.min(choiceY, height - h - 4);

        // Klick ausserhalb schliesst (wird vor den Optionen registriert -> Optionen haben Vorrang)
        regionX(0, 0, width, height, m -> openChoice = null);

        Ui.rrBorder(g, choiceX, cy, choiceW, h, 4, 0xFF14141A, 0x55FFFFFF);
        int current = c.get.getAsInt();
        for (int i = 0; i < c.options.length; i++) {
            final int idx = i;
            int oy = cy + 2 + i * rowH;
            boolean hov = over(mx, my, choiceX + 2, oy, choiceW - 4, rowH);
            if (hov) Ui.rr(g, choiceX + 2, oy, choiceW - 4, rowH, 3, 0xFF2A2A35);
            g.text(font, fit(c.options[i], choiceW - 16), choiceX + 7, oy + 3, i == current ? TXT : TXT2, false);
            regionX(choiceX + 2, oy, choiceW - 4, rowH, m -> {
                c.set.accept(idx);
                AulaConfig.save();
                openChoice = null;
            });
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            searchFocused = false;
            int mx = (int) event.x(), my = (int) event.y();
            for (int i = regions.size() - 1; i >= 0; i--) {
                Region r = regions.get(i);
                if (over(mx, my, r.x, r.y, r.w, r.h)) {
                    r.click.accept(event.x());
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragSlider != null) {
            dragSlider.setFromFraction((event.x() - dragX) / (double) dragW);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragSlider != null) {
            dragSlider = null;
            AulaConfig.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double hx, double vy) {
        if (!page.equals("Settings") && over((int) x, (int) y, cx, ay, cw, ah)) {
            scroll -= (int) Math.round(vy * 18);
            return true;
        }
        return super.mouseScrolled(x, y, hx, vy);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused) {
            int cp = event.codepoint();
            if (cp >= 32 && cp != 127 && query.length() < 24) {
                query += event.codepointAsString();
                scroll = 0;
            }
            return true;
        }
        return super.charTyped(event);
    }

    private static boolean isEscape(KeyEvent e) {
        return e.key() == InputConstants.KEY_ESCAPE || e.keycode() == 27 || e.keycode() == 256;
    }

    private static boolean isBackspace(KeyEvent e) {
        return e.key() == InputConstants.KEY_BACKSPACE || e.keycode() == 8 || e.keycode() == 259;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isEscape(event)) {
            if (openChoice != null) {
                openChoice = null;
                return true;
            }
            if (searchFocused) {
                searchFocused = false;
                return true;
            }
            return super.keyPressed(event);
        }
        if (searchFocused) {
            if (isBackspace(event) && !query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                scroll = 0;
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
