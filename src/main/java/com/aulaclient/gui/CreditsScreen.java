package com.aulaclient.gui;

import com.aulaclient.AulaClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Kleine Credits-/Versionshistorie-Seite im Startmenue. */
public class CreditsScreen extends Screen {
    private final Screen parent;

    private static final String[][] HISTORY = {
            {"1.9.x", "Entfernbare Cosmetics, 15 neue Features (Cape, Fluegel, Glaskoerper, Schadenszahlen, Potion-Leiste, FPS-Graph, Favoriten-Leiste, Freilook, Rahmen-Screenshot, Szenen, Saison-Theme, Credits)"},
            {"1.6 - 1.8", "Lauffaehiger, greifbarer Charakter im Startmenue"},
            {"1.5", "Uebersetzte UI im Larp-Stil, eigenes Logo, durchsichtiges Menue"},
            {"1.0 - 1.4", "HUD-Module, Crosshair, Stretched Res, Character FX, Zoom"},
    };

    public CreditsScreen(Screen parent) {
        super(Component.literal("Credits"));
        this.parent = parent;
    }

    @Override
    protected void init() {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fillGradient(0, 0, width, height, 0xFF040406, 0xFF0B0B11);
        int cx = width / 2;
        g.centeredText(font, AulaClient.NAME, cx, 50, 0xFFFFFFFF);
        g.centeredText(font, "Credits & Versionshistorie", cx, 64, 0xFFB0B0BC);

        int y = 96;
        for (String[] row : HISTORY) {
            g.text(font, row[0], cx - 170, y, 0xFFE8E8EE, false);
            g.text(font, wrap(row[1], 300), cx - 110, y, 0xFF9AA0AC, false);
            y += 28;
        }

        g.centeredText(font, "Danke, dass du den Aula Client nutzt.", cx, height - 46, 0xFF8A8A96);
        boolean hov = my > height - 30 && my < height - 14 && Math.abs(mx - cx) < 40;
        g.centeredText(font, "Zurueck", cx, height - 28, hov ? 0xFFFFFFFF : 0xFFB0B0BC);
    }

    private String wrap(String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        int cut = s.length();
        while (cut > 0 && font.width(s.substring(0, cut) + "...") > maxW) cut--;
        return s.substring(0, Math.max(0, cut)) + "...";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int cx = width / 2;
            int mx = (int) event.x(), my = (int) event.y();
            if (my > height - 30 && my < height - 14 && Math.abs(mx - cx) < 40) {
                Ui.click();
                back();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void back() {
        if (minecraft != null) minecraft.setScreenAndShow(parent);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        back();
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        back();
    }
}
