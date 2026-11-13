package com.aulaclient.gui;

import com.aulaclient.AulaClient;
import com.aulaclient.config.AulaConfig;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Aula-Startbildschirm: schwarzer Hintergrund mit wehenden Vorhaengen und schwebenden Partikeln,
 * zentriertes Logo und Menueliste. Beim ersten Start pro Spielstart zeigt er zuerst einen
 * "Press to enter"-Splash. Ersetzt den Vanilla-TitleScreen (siehe AulaClient).
 */
public class AulaTitleScreen extends Screen {
    private static boolean entered = false;

    private record Item(String label, int tier, Runnable action) {}

    private final List<Item> items = new ArrayList<>();
    private int[] itemY = new int[0];
    private int[] itemW = new int[0];
    private int[] itemH = new int[0];

    private final long openedAt = System.currentTimeMillis();
    private long enteredAt = entered ? openedAt - 10_000 : 0;

    public AulaTitleScreen() {
        super(Component.literal(AulaClient.NAME));
    }

    @Override
    protected void init() {
        items.clear();
        items.add(new Item("Singleplayer", 1, () -> minecraft.setScreenAndShow(new SelectWorldScreen(this))));
        items.add(new Item("Multiplayer", 1, () -> minecraft.setScreenAndShow(new JoinMultiplayerScreen(this))));
        items.add(new Item("Minecraft Realms", 1, () -> minecraft.setScreenAndShow(new RealmsMainScreen(this))));
        items.add(new Item("Minecraft Settings", 2, () -> minecraft.setScreenAndShow(new OptionsScreen(this, minecraft.options))));
        items.add(new Item("Client", 2, () -> minecraft.setScreenAndShow(new AulaScreen(this))));
        items.add(new Item("Quit Game", 3, () -> minecraft.stop()));
        itemY = new int[items.size()];
        itemW = new int[items.size()];
        itemH = new int[items.size()];
    }

    // ---------------------------------------------------------------- helpers

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    /** rgb + Alpha (0..1) -> ARGB. */
    private static int argb(int rgb, float alpha) {
        return ((int) (clamp01(alpha) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    private static double rnd(int i, int salt) {
        return frac(Math.sin(i * 12.9898 + salt * 78.233) * 43758.5453);
    }

    // ---------------------------------------------------------------- background

    private void drawCurtains(GuiGraphicsExtractor g, float t, float fade) {
        for (int side = 0; side < 2; side++) {
            for (int k = 0; k < 6; k++) {
                double base = side == 0 ? width * (0.03 + 0.05 * k) : width * (0.97 - 0.05 * k);
                double amp = width * 0.02 + k * 1.6;
                float alpha = (0.045f + (5 - k) * 0.012f) * fade;
                int col = argb(0xFFFFFF, alpha);
                for (int y = 0; y < height; y += 2) {
                    double x = base
                            + amp * Math.sin(y * 0.018 + t * 0.45 + k * 0.7 + side * 1.9)
                            + amp * 0.5 * Math.sin(y * 0.007 - t * 0.3 + k);
                    // Vorhaenge buchten sich zur Mitte hin aus
                    x += (side == 0 ? 1 : -1) * Math.sin(y / (double) height * Math.PI) * width * 0.045;
                    g.fill((int) x, y, (int) x + 2, y + 2, col);
                }
            }
        }
    }

    private void drawParticles(GuiGraphicsExtractor g, float t, float fade) {
        for (int i = 0; i < 30; i++) {
            double xr = rnd(i, 1);
            double x = (i % 2 == 0) ? xr * width * 0.3 : width * (0.7 + xr * 0.3);
            double speed = 3 + rnd(i, 2) * 7;
            double y = height - ((rnd(i, 3) * height + t * speed) % height);
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 1.5 + i);
            int col = argb(0xFFFFFF, (0.12f + 0.25f * tw) * fade);
            int xi = (int) x, yi = (int) y;
            g.fill(xi - 1, yi, xi + 2, yi + 1, col);
            g.fill(xi, yi - 1, xi + 1, yi + 2, col);
        }
    }

    private void drawLogo(GuiGraphicsExtractor g, int centerX, int y, float alpha) {
        int accent = AulaConfig.get().accent;
        Ui.rr(g, centerX - 16, y, 32, 32, 8, argb(accent, alpha));
        g.pose().pushMatrix();
        g.pose().translate(centerX, y + 8);
        g.pose().scale(2f, 2f);
        g.centeredText(font, "A", 0, 0, argb(0x111116, alpha));
        g.pose().popMatrix();

        g.pose().pushMatrix();
        g.pose().translate(centerX, y + 42);
        g.pose().scale(1.6f, 1.6f);
        g.centeredText(font, AulaClient.NAME, 0, 0, argb(0xFFFFFF, alpha));
        g.pose().popMatrix();
    }

    // ---------------------------------------------------------------- render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        float t = (now - openedAt) / 1000f;
        float fade = clamp01(t / 0.9f);

        g.fillGradient(0, 0, width, height, 0xFF040406, 0xFF0B0B11);
        drawCurtains(g, t, fade);
        drawParticles(g, t, fade);

        int cx = width / 2;

        if (!entered) {
            drawLogo(g, cx, height / 2 - 42, fade);
            String s = "Press to enter";
            int w = font.width(s);
            float pulse = 0.7f + 0.3f * (float) Math.sin(t * 2.2);
            g.centeredText(font, s, cx, height / 2 + 40, argb(0xFFFFFF, pulse * fade));
            g.fill(cx - w / 2, height / 2 + 51, cx + w / 2, height / 2 + 52, argb(0xFFFFFF, 0.5f * fade));
        } else {
            float m = clamp01((now - enteredAt) / 500f);
            drawLogo(g, cx, height / 2 - 92, m);
            drawMenu(g, mx, my, cx, height / 2 - 28, m);
        }

        g.text(font, "Aula Client 1.4.0  ·  Minecraft 26.3", 8, height - 14, argb(0x8A8A96, 0.6f * fade), false);
    }

    private void drawMenu(GuiGraphicsExtractor g, int mx, int my, int cx, int startY, float alpha) {
        int y = startY;
        int lastTier = 1;
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            if (it.tier != lastTier) {
                y += 8;
                lastTier = it.tier;
            }
            float scale = it.tier == 1 ? 1.25f : it.tier == 2 ? 1.0f : 0.9f;
            int w = (int) (font.width(it.label) * scale);
            int h = (int) (9 * scale);
            int top = y;
            boolean hov = alpha >= 1f && mx >= cx - w / 2 - 10 && mx <= cx + w / 2 + 10 && my >= top - 3 && my <= top + h + 3;
            int base = it.tier == 1 ? 0xE8E8EE : it.tier == 2 ? 0xB0B0BC : 0x8A8A96;
            int rgb = hov ? 0xFFFFFF : base;

            g.pose().pushMatrix();
            g.pose().translate(cx, top);
            g.pose().scale(scale, scale);
            g.centeredText(font, it.label, 0, 0, argb(rgb, alpha));
            g.pose().popMatrix();
            if (hov) {
                g.fill(cx - w / 2, top + h + 2, cx + w / 2, top + h + 3, argb(0xFFFFFF, 0.55f));
            }

            itemY[i] = top - 3;
            itemW[i] = w + 20;
            itemH[i] = h + 6;
            y += it.tier == 1 ? 22 : it.tier == 2 ? 20 : 18;
        }
    }

    // ---------------------------------------------------------------- input

    private void enter() {
        entered = true;
        enteredAt = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (!entered) {
                enter();
                return true;
            }
            int mx = (int) event.x(), my = (int) event.y();
            int cx = width / 2;
            for (int i = 0; i < items.size(); i++) {
                if (itemW[i] > 0 && mx >= cx - itemW[i] / 2 && mx <= cx + itemW[i] / 2
                        && my >= itemY[i] && my <= itemY[i] + itemH[i]) {
                    items.get(i).action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!entered) {
            enter();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
