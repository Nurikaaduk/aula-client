package com.aulaclient.gui;

import com.aulaclient.AulaClient;
import com.aulaclient.config.AulaConfig;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
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

    // ---- Charakter (laeuft herein, mit der Maus greif- und werfbar)
    private int CW = 90, CH = 150;
    private Model.Simple wideModel, slimModel;
    private ResolvableProfile profile;
    private boolean charReady = false;
    private double px, py, vx, vy;          // Mittelpunkt + Geschwindigkeit
    private float rotY = 30f, rotX = -5f, spin;
    private boolean grabbed = false;
    private double grabDX, grabDY;
    private long lastFrame = 0, releasedAt = 0, walkStart = 0;
    private boolean charStarted = false;
    private boolean walking = false, flying = false;

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
        sizeChar();
        try {
            wideModel = new Model.Simple(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), RenderTypes::entityTranslucent);
            slimModel = new Model.Simple(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), RenderTypes::entityTranslucent);
            profile = ResolvableProfile.createResolved(minecraft.getGameProfile());
            charReady = true;
        } catch (Throwable t) {
            charReady = false;
        }
    }

    private double homeX() { return width * 0.80; }
    private double homeY() { return height - 14 - CH / 2.0; }
    private void sizeChar() {
        CH = Math.max(120, (int) (height * 0.62));
        CW = (int) (CH * 0.62);
    }

    private void startWalk() {
        walkStart = System.currentTimeMillis() + 250;
        walking = true;
        px = width + CW;
        py = homeY();
    }

    private boolean overChar(double mx, double my) {
        return mx >= px - CW / 2.0 && mx <= px + CW / 2.0 && my >= py - CH / 2.0 && my <= py + CH / 2.0;
    }

    private void updateChar(int mx, int my, long now) {
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        double hx = homeX(), hy = homeY();
        if (walking) {
            float k = clamp01((now - walkStart) / 2400f);
            float e = 1f - (float) Math.pow(1f - k, 3);
            px = (width + CW) + (hx - (width + CW)) * e;
            py = hy;
            vx = vy = 0;
            // Lauf-Wippen, dreht sich beim Ankommen zum Menue
            rotY = -40f + 70f * e + (k < 1f ? 14f * (float) Math.sin(now / 130.0) : 0f);
            rotX = -5f + (k < 1f ? 4f * (float) Math.sin(now / 65.0) : 0f);
            if (k >= 1f) walking = false;
            return;
        }
        if (grabbed) {
            double tx = mx - grabDX, ty = my - grabDY;
            double nx = px + (tx - px) * Math.min(1.0, dt * 22), ny = py + (ty - py) * Math.min(1.0, dt * 22);
            vx = (nx - px) / dt * 0.6 + vx * 0.4;
            vy = (ny - py) / dt * 0.6 + vy * 0.4;
            px = nx; py = ny;
            rotX += (-5f + (float) (vy * -0.02) - rotX) * Math.min(1f, dt * 8);
            rotY += (float) (vx * 0.04) * dt * 10f;
            return;
        }
        if (flying) {
            vy += 1100 * dt;
            px += vx * dt; py += vy * dt;
            double floor = height - 8 - CH / 2.0;
            if (py > floor) { py = floor; vy = -vy * 0.55; vx *= 0.85; if (Math.abs(vy) < 40) vy = 0; spin *= 0.85f; }
            if (px < CW / 2.0) { px = CW / 2.0; vx = Math.abs(vx) * 0.6; spin = -spin * 0.7f; }
            if (px > width - CW / 2.0) { px = width - CW / 2.0; vx = -Math.abs(vx) * 0.6; spin = -spin * 0.7f; }
            if (py < CH / 2.0) { py = CH / 2.0; vy = Math.abs(vy) * 0.6; }
            rotY += spin * dt;
            rotX += (-5f - rotX) * Math.min(1f, dt * 3);
            if (now - releasedAt > 1700) flying = false;
        } else {
            // zurueck zur Ausgangsposition
            vx += (hx - px) * 14 * dt; vy += (hy - py) * 14 * dt;
            vx *= Math.max(0, 1 - 6 * dt); vy *= Math.max(0, 1 - 6 * dt);
            px += vx * dt; py += vy * dt;
            float target = 30f;
            float diff = ((rotY - target) % 360f + 540f) % 360f - 180f;
            rotY -= diff * Math.min(1f, dt * 5);
            rotX += (-5f - rotX) * Math.min(1f, dt * 5);
            spin = 0;
        }
    }

    /** Eigener Flammen-/Horn-Effekt ueber dem Kopf des Charakters (billig: nur wenige fill-Aufrufe). */
    private void drawFlame(GuiGraphicsExtractor g, long now, float alpha) {
        if (!charReady || alpha <= 0f) return;
        double tt = now / 1000.0;
        int cxf = (int) px;
        int base = (int) (py - CH / 2.0) + CH / 14;      // knapp ueber der Kopfoberkante
        int fh = Math.max(40, CH / 3);
        int[][] layers = {{0xFF3A00, 0}, {0xFF9A00, 1}, {0xFFE66A, 2}};
        for (int[] L : layers) {
            int hh = (int) (fh * (1.0 - L[1] * 0.28) * (1 + 0.06 * Math.sin(tt * 9 + L[1])));
            int ww = (int) (CW * (0.30 - L[1] * 0.07));
            for (int r = 0; r < hh; r += 2) {
                double f = r / (double) hh;                 // 0 unten .. 1 Spitze
                double wdt = ww * Math.sin(Math.min(1.0, f * 1.15 + 0.12) * Math.PI * 0.5) * (1 - f * f);
                int sway = (int) (Math.sin(tt * 6 + f * 4 + L[1]) * ww * 0.18 * f);
                int y = base - r;
                g.fill(cxf + sway - (int) wdt, y - 2, cxf + sway + (int) wdt + 1, y, argb(L[0], (0.35f + 0.15f * L[1]) * alpha));
            }
        }
        // Hoerner: zwei geschwungene, dunkelrote Spitzen
        int hornCol = argb(0x8A1020, 0.95f * alpha), tip = argb(0xFF5A3C, alpha);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 14; i++) {
                int hx = cxf + side * (CW / 4 + i * 2);
                int hy = base + CH / 22 - (int) (i * i * 0.35) ;
                int th = Math.max(1, 5 - i / 3);
                g.fill(hx, hy, hx + 3, hy + th, i > 11 ? tip : hornCol);
            }
        }
    }

    private void drawShadow(GuiGraphicsExtractor g, float alpha) {
        if (!charReady) return;
        double floor = height - 8;
        double dist = Math.max(0, floor - (py + CH / 2.0));
        float k = (float) Math.max(0.15, 1.0 - dist / (height * 0.7));
        int w = (int) (CW * 0.9 * k), h = Math.max(2, (int) (6 * k));
        int cxs = (int) px, cys = (int) floor - 2;
        for (int i = 0; i < h; i++) {
            float f = 1f - i / (float) h;
            int ww = (int) (w * Math.sqrt(1 - Math.pow(i / (double) h, 2)));
            g.fill(cxs - ww / 2, cys + i - h / 2, cxs + ww / 2, cys + i - h / 2 + 1, argb(0xFFFFFF, 0.10f * f * k * alpha));
        }
    }

    private void drawChar(GuiGraphicsExtractor g, int mx, int my, long now, float alpha) {
        if (!charReady || alpha <= 0f) return;
        try {
            updateChar(mx, my, now);
            PlayerSkin skin = minecraft.playerSkinRenderCache().getOrDefault(profile).playerSkin();
            Model.Simple m = skin.model() == PlayerModelType.SLIM ? slimModel : wideModel;
            float scale = 0.97f * CH / 2.125f;
            g.skin(m, skin.body().texturePath(), scale, rotX, rotY, -0.0625f,
                    (int) (px - CW / 2.0), (int) (py - CH / 2.0), (int) (px + CW / 2.0), (int) (py + CH / 2.0));
        } catch (Throwable t) {
            charReady = false;
        }
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
        final int strands = 7, step = 6;
        for (int side = 0; side < 2; side++) {
            for (int k = 0; k < strands; k++) {
                double frac = (k + 0.5) / strands;
                double base = side == 0 ? width * (0.01 + 0.25 * frac) : width * (0.99 - 0.25 * frac);
                float alpha = (0.07f + 0.09f * (float) Math.sin(frac * Math.PI * 0.9 + 0.2)) * fade;
                int col = argb(0xFFFFFF, alpha);
                int prev = Integer.MIN_VALUE;
                for (int y = 0; y < height; y += step) {
                    double yn = (y + step / 2.0) / height;
                    double bulge = Math.exp(-Math.pow((yn - 0.38) / 0.28, 2)) * width * 0.055 * (0.6 + frac);
                    double x = base
                            + width * 0.012 * Math.sin(yn * 7.0 + t * 0.35 + k * 0.5 + side * 2.1)
                            + width * 0.008 * Math.sin(yn * 3.0 - t * 0.22 + k);
                    x += (side == 0 ? 1 : -1) * bulge;
                    int xi = (int) x;
                    int lo = prev == Integer.MIN_VALUE ? xi : Math.min(prev, xi);
                    int hi = prev == Integer.MIN_VALUE ? xi : Math.max(prev, xi);
                    g.fill(lo, y, hi + 1, y + step, col);
                    prev = xi;
                }
            }
        }
    }

    private void drawParticles(GuiGraphicsExtractor g, float t, float fade) {
        for (int i = 0; i < 18; i++) {
            double xr = rnd(i, 1);
            double x = (i % 2 == 0) ? xr * width * 0.27 : width * (0.73 + xr * 0.27);
            double speed = 3 + rnd(i, 2) * 6;
            double y = height - ((rnd(i, 3) * height + t * speed) % height);
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 1.2 + i);
            int r = 3 + (int) (rnd(i, 4) * 3);
            int col = argb(0xFFFFFF, (0.10f + 0.22f * tw) * fade);
            int xi = (int) x, yi = (int) y;
            for (int d = -r; d <= r; d++) {
                int e = r - Math.abs(d);
                g.fill(xi + d, yi - e, xi + d + 1, yi - e + 1, col);
                g.fill(xi + d, yi + e, xi + d + 1, yi + e + 1, col);
            }
        }
    }

    /** Logo (Textur) + Titel darunter; y = Oberkante, logoW = Breite des Logos. */
    private void drawLogo(GuiGraphicsExtractor g, int centerX, int y, int logoW, float alpha) {
        int logoH = Math.round(logoW * (float) Ui.LOGO_H / Ui.LOGO_W);
        Ui.logo(g, centerX - logoW / 2, y, logoW, logoH);
        if (alpha < 1f) {
            // Einblenden: dunkle Flaeche ueber dem Logo wird ausgeblendet
            g.fill(centerX - logoW / 2, y, centerX + logoW / 2, y + logoH, argb(0x08080C, 1f - alpha));
        }
        g.pose().pushMatrix();
        g.pose().translate(centerX, y + logoH + 10);
        g.pose().scale(1.6f, 1.6f);
        g.centeredText(font, AulaClient.NAME, 0, 0, argb(0xFFFFFF, alpha));
        g.pose().popMatrix();
    }

    /** Menue-Kopf: kleines Logo links, Name rechts daneben, in einer Zeile. */
    private void drawHeader(GuiGraphicsExtractor g, int cx, int cy, float alpha) {
        float ts = 1.7f;
        int nameW = (int) (font.width(AulaClient.NAME) * ts);
        int logoW = 28;
        int logoH = Math.round(logoW * (float) Ui.LOGO_H / Ui.LOGO_W);
        int gap = 10;
        int total = logoW + gap + nameW;
        int x0 = cx - total / 2;
        Ui.logo(g, x0, cy - logoH / 2, logoW, logoH);
        if (alpha < 1f) g.fill(x0, cy - logoH / 2, x0 + logoW, cy + logoH / 2, argb(0x040406, 1f - alpha));
        g.pose().pushMatrix();
        g.pose().translate(x0 + logoW + gap, cy - 4 * ts);
        g.pose().scale(ts, ts);
        g.text(font, AulaClient.NAME, 0, 0, argb(0xFFFFFF, alpha), false);
        g.pose().popMatrix();
    }

    /** Konto-Auswahl oben rechts (Name + Pfeil, Klick klappt Liste auf). */
    private boolean accountOpen = false;

    private void drawAccount(GuiGraphicsExtractor g, int mx, int my, float alpha) {
        String name = minecraft.getGameProfile().name();
        int w = font.width(name) + 12;
        int x = (int) (width * 0.80) - w / 2, y = 14;
        g.text(font, name, x, y, argb(0xE8E8EE, alpha), false);
        int ax = x + font.width(name) + 5;
        for (int i = 0; i < 4; i++) {
            int yy = accountOpen ? y + 6 - i : y + 3 + i;
            g.fill(ax + i, yy, ax + 7 - i, yy + 1, argb(0xB0B0BC, alpha));
        }
        g.fill(x, y + 11, x + w - 4, y + 12, argb(0xFFFFFF, 0.25f * alpha));
        if (accountOpen) {
            int bx = (int) (width * 0.80) - 78, by = y + 20, bw = 156, bh = 54;
            Ui.rr(g, bx, by, bw, bh, 4, argb(0x101014, 0.92f));
            Ui.rrRing(g, bx, by, bw, bh, 4, argb(0xFFFFFF, 0.10f));
            g.text(font, name, bx + 10, by + 9, argb(0xE8E8EE, 1f), false);
            g.text(font, "Current", bx + bw - 10 - font.width("Current"), by + 9, argb(0x8A8A96, 1f), false);
            g.fill(bx + 8, by + 24, bx + bw - 8, by + 25, argb(0xFFFFFF, 0.10f));
            g.centeredText(font, "+ Add another account", bx + bw / 2, by + 36, argb(0xB0B0BC, 1f));
        }
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
            drawLogo(g, cx, height / 2 - 82, 80, fade);
            String s = "Press to enter";
            int w = font.width(s);
            float pulse = 0.7f + 0.3f * (float) Math.sin(t * 2.2);
            g.centeredText(font, s, cx, height / 2 + 40, argb(0xFFFFFF, pulse * fade));
            g.fill(cx - w / 2, height / 2 + 51, cx + w / 2, height / 2 + 52, argb(0xFFFFFF, 0.5f * fade));
        } else {
            float m = clamp01((now - enteredAt) / 500f);
            drawHeader(g, cx, (int) (height * 0.315), m);
            drawMenu(g, mx, my, cx, (int) (height * 0.405), m);
            if (!charStarted) { charStarted = true; startWalk(); }
            drawShadow(g, m);
            drawChar(g, mx, my, now, m);
            drawFlame(g, now, m);
            drawAccount(g, mx, my, m);
        }

        g.text(font, "Version 26.3 (Aula 1.7.1)", 6, height - 12, argb(0x8A8A96, 0.55f * fade), false);
        String brand = "AULA CLIENT";
        int bw = font.width(brand);
        Ui.logo(g, width - bw - 12 - 14, height - 15, 11, Math.round(11f * Ui.LOGO_H / Ui.LOGO_W));
        g.text(font, brand, width - bw - 6, height - 12, argb(0xE8E8EE, 0.8f * fade), false);
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
            float scale = it.tier == 1 ? 1.3f : it.tier == 2 ? 1.05f : 0.95f;
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
            y += it.tier == 1 ? Math.max(22, height / 17) : it.tier == 2 ? Math.max(20, height / 19) : Math.max(18, height / 21);
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
            if (my >= 10 && my <= 26 && Math.abs(mx - width * 0.80) < 50) { accountOpen = !accountOpen; return true; }
            if (accountOpen) accountOpen = false;
            if (charReady && !walking && overChar(mx, my)) {
                grabbed = true; flying = false;
                grabDX = mx - px; grabDY = my - py;
                vx = vy = 0;
                return true;
            }
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
    public boolean mouseReleased(MouseButtonEvent event) {
        if (grabbed && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            grabbed = false;
            flying = true;
            releasedAt = System.currentTimeMillis();
            vx = Math.max(-2500, Math.min(2500, vx));
            vy = Math.max(-2500, Math.min(2500, vy));
            spin = (float) (vx * 0.5 + (vy < 0 ? -200 : 200) * Math.signum(vx == 0 ? 1 : vx));
            return true;
        }
        return super.mouseReleased(event);
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
