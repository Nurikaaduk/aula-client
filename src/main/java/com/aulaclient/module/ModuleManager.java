package com.aulaclient.module;

import com.aulaclient.config.AulaConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

public class ModuleManager {
    public final List<HudModule> modules = new ArrayList<>();
    public final Zoom zoom;
    public final Crosshair crosshair;
    public final Stretch stretch;
    public final CharacterFx characterFx;

    public ModuleManager(KeyMapping zoomKey) {
        modules.add(new SimpleText("FPS", "Zeigt die aktuellen FPS", 4, 4,
                mc -> mc.getFps() + " FPS"));
        modules.add(new SimpleText("Koordinaten", "Zeigt XYZ-Position", 4, 20,
                mc -> mc.player == null ? "" : String.format("XYZ %.1f / %.1f / %.1f",
                        mc.player.getX(), mc.player.getY(), mc.player.getZ())));
        modules.add(new SimpleText("Uhrzeit", "Zeigt die lokale Uhrzeit", 4, 36,
                mc -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        modules.add(new Cps());
        modules.add(new Keystrokes());
        modules.add(new Armor());
        zoom = new Zoom(zoomKey);
        crosshair = new Crosshair();
        stretch = new Stretch();
        characterFx = new CharacterFx();
        modules.add(zoom);
        modules.add(crosshair);
        modules.add(stretch);
        modules.add(characterFx);
    }

    /** Uebernimmt gespeicherten Zustand aus der Config. */
    public void applyConfig() {
        AulaConfig cfg = AulaConfig.get();
        for (HudModule m : modules) {
            boolean defaultOn = !(m instanceof Crosshair || m instanceof Stretch || m instanceof CharacterFx);
            m.enabled = cfg.enabled.getOrDefault(m.name, defaultOn);
            int[] p = cfg.positions.get(m.name);
            if (p != null && p.length == 2) {
                m.x = p[0];
                m.y = p[1];
            }
        }
    }

    public void tick(Minecraft mc) {
        for (HudModule m : modules) if (m.enabled) m.tick(mc);
    }

    public void renderHud(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null) return;
        for (HudModule m : modules) if (m.enabled) m.render(g, mc);
    }

    // ---------------------------------------------------------------

    static class SimpleText extends HudModule {
        private final Function<Minecraft, String> supplier;

        SimpleText(String name, String desc, int x, int y, Function<Minecraft, String> supplier) {
            super(name, desc, x, y);
            this.supplier = supplier;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {
            String t = supplier.apply(mc);
            if (!t.isEmpty()) box(g, mc, t);
        }
    }

    /** Klicks pro Sekunde (links | rechts). */
    static class Cps extends HudModule {
        private final Deque<Long> left = new ArrayDeque<>();
        private final Deque<Long> right = new ArrayDeque<>();
        private boolean lastL, lastR;

        Cps() {
            super("CPS", "Klicks pro Sekunde", 4, 52);
        }

        @Override
        public void tick(Minecraft mc) {
            boolean l = mc.options.keyAttack.isDown();
            boolean r = mc.options.keyUse.isDown();
            long now = System.currentTimeMillis();
            if (l && !lastL) left.add(now);
            if (r && !lastR) right.add(now);
            lastL = l;
            lastR = r;
            while (!left.isEmpty() && now - left.peekFirst() > 1000) left.poll();
            while (!right.isEmpty() && now - right.peekFirst() > 1000) right.poll();
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {
            box(g, mc, left.size() + " | " + right.size() + " CPS");
        }
    }

    /** WASD + Maustasten + Leertaste. */
    static class Keystrokes extends HudModule {
        Keystrokes() {
            super("Keystrokes", "Zeigt gedrueckte Tasten", 4, 72);
            w = 58;
            h = 78;
        }

        private void key(GuiGraphicsExtractor g, Minecraft mc, int kx, int ky, int kw, String label, boolean down) {
            int bg = down ? AulaConfig.get().accent : 0x90101018;
            g.fill(kx, ky, kx + kw, ky + 18, bg);
            int tw = mc.font.width(label);
            g.text(mc.font, label, kx + (kw - tw) / 2, ky + 5, down ? 0xFF101018 : 0xFFFFFFFF, true);
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {
            var o = mc.options;
            key(g, mc, x + 20, y, 18, "W", o.keyUp.isDown());
            key(g, mc, x, y + 20, 18, "A", o.keyLeft.isDown());
            key(g, mc, x + 20, y + 20, 18, "S", o.keyDown.isDown());
            key(g, mc, x + 40, y + 20, 18, "D", o.keyRight.isDown());
            key(g, mc, x, y + 40, 28, "LMB", o.keyAttack.isDown());
            key(g, mc, x + 30, y + 40, 28, "RMB", o.keyUse.isDown());
            key(g, mc, x, y + 60, 58, "SPACE", o.keyJump.isDown());
        }
    }

    /** Ruestungsanzeige mit Haltbarkeit. */
    static class Armor extends HudModule {
        private static final EquipmentSlot[] SLOTS = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };

        Armor() {
            super("Ruestung", "Zeigt Ruestung und Haltbarkeit", 4, 156);
            w = 60;
            h = 72;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {
            if (mc.player == null) return;
            int row = 0;
            for (EquipmentSlot slot : SLOTS) {
                ItemStack st = mc.player.getItemBySlot(slot);
                int ry = y + row * 18;
                g.fill(x, ry, x + 60, ry + 17, 0x90101018);
                if (!st.isEmpty()) {
                    g.item(st, x + 1, ry);
                    g.itemDecorations(mc.font, st, x + 1, ry);
                    if (st.isDamageableItem()) {
                        int left = st.getMaxDamage() - st.getDamageValue();
                        int pct = Math.round(100f * left / st.getMaxDamage());
                        g.text(mc.font, pct + "%", x + 22, ry + 5, 0xFFFFFFFF, true);
                    }
                }
                row++;
            }
        }
    }

    /** Zoom auf Taste (Standard: C). Weich interpoliert, stellt FOV beim Loslassen wieder her. */
    public static class Zoom extends HudModule {
        private final KeyMapping key;
        private double current = -1;
        private int original = -1;

        Zoom(KeyMapping key) {
            super("Zoom", "Halte die Zoom-Taste (Standard: C)", 0, 0);
            this.key = key;
        }

        @Override
        public boolean movable() {
            return false;
        }

        @Override
        public void tick(Minecraft mc) {
            var fov = mc.options.fov();
            boolean active = key.isDown() && mc.gui.screen() == null;
            if (active) {
                if (original < 0) {
                    original = fov.get();
                    current = original;
                }
                double target = AulaConfig.get().zoomFov;
                current += (target - current) * 0.45;
                fov.set((int) Math.round(Math.max(30, current)));
            } else if (original >= 0) {
                current += (original - current) * 0.45;
                if (Math.abs(original - current) < 1.0) {
                    fov.set(original);
                    original = -1;
                } else {
                    fov.set((int) Math.round(current));
                }
            }
        }

        @Override
        public void onDisable(Minecraft mc) {
            if (original >= 0) {
                mc.options.fov().set(original);
                original = -1;
            }
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {}
    }

    /** Eigenes Crosshair (Cosmetic). Ersetzt das Vanilla-Crosshair, solange aktiv. */
    public static class Crosshair extends HudModule {
        Crosshair() {
            super("Crosshair", "Eigenes Crosshair (Stil, Groesse, Farbe)", 0, 0);
        }

        @Override
        public boolean movable() {
            return false;
        }

        /** Wird vom HudElement-Replace aufgerufen. */
        public void draw(GuiGraphicsExtractor g) {
            AulaConfig c = AulaConfig.get();
            int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
            int col = c.crosshairAccent ? c.accent : 0xFFFFFFFF;
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
                default -> { // Kreuz mit Luecke
                    int gap = 2;
                    g.fill(cx - s - gap, cy - 1, cx - gap, cy + 1, col);
                    g.fill(cx + gap + 1, cy - 1, cx + s + gap + 1, cy + 1, col);
                    g.fill(cx - 1, cy - s - gap, cx + 1, cy - gap, col);
                    g.fill(cx - 1, cy + gap + 1, cx + 1, cy + s + gap + 1, col);
                }
            }
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {}
    }

    /** Stretched Resolution. Die eigentliche Arbeit macht GameRendererMixin. */
    public static class Stretch extends HudModule {
        Stretch() {
            super("Stretched Res", "Streckt das Bild auf ein anderes Seitenverhaeltnis (z. B. 4:3)", 0, 0);
        }

        @Override
        public boolean movable() {
            return false;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {}
    }

    /** Lokale Charakter-Effekte per Partikel (Halo, Wirbel, Spur, Aura). Nur fuer dich sichtbar. */
    public static class CharacterFx extends HudModule {
        public static final String[] STYLE_NAMES = {"Halo", "Wirbel", "Spur", "Aura"};
        public static final String[] PARTICLE_NAMES = {
                "Herzen", "Funken", "Flammen", "Wolken", "Noten", "Glitzer", "Runen"
        };
        private static final ParticleOptions[] PARTICLES = {
                ParticleTypes.HEART, ParticleTypes.END_ROD, ParticleTypes.FLAME, ParticleTypes.CLOUD,
                ParticleTypes.NOTE, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.ENCHANT
        };

        private int ticks;

        CharacterFx() {
            super("Charakter-Effekt", "Partikel-Cosmetic um deinen Charakter (lokal)", 0, 0);
        }

        @Override
        public boolean movable() {
            return false;
        }

        @Override
        public void tick(Minecraft mc) {
            var p = mc.player;
            var level = mc.level;
            if (p == null || level == null || mc.isPaused()) return;
            ticks++;

            AulaConfig c = AulaConfig.get();
            ParticleOptions part = PARTICLES[Math.floorMod(c.fxParticle, PARTICLES.length)];
            var rnd = java.util.concurrent.ThreadLocalRandom.current();
            double px = p.getX(), py = p.getY(), pz = p.getZ();
            double height = p.getBbHeight();

            for (int i = 0; i < c.fxDensity; i++) {
                switch (c.fxStyle) {
                    case 0 -> { // Halo ueber dem Kopf
                        double a = ticks * 0.35 + i * (Math.PI * 2 / c.fxDensity);
                        level.addParticle(part, px + Math.cos(a) * 0.38, py + height + 0.25,
                                pz + Math.sin(a) * 0.38, 0, 0, 0);
                    }
                    case 1 -> { // Aufsteigender Wirbel
                        double a = ticks * 0.45 + i * (Math.PI * 2 / c.fxDensity);
                        double hy = ((ticks * 0.06) + i * 0.3) % height;
                        level.addParticle(part, px + Math.cos(a) * 0.7, py + hy,
                                pz + Math.sin(a) * 0.7, 0, 0.01, 0);
                    }
                    case 2 -> { // Spur an den Fuessen, nur in Bewegung
                        var v = p.getDeltaMovement();
                        if (v.x * v.x + v.z * v.z > 0.0004) {
                            level.addParticle(part, px + rnd.nextGaussian() * 0.12, py + 0.1,
                                    pz + rnd.nextGaussian() * 0.12, 0, 0.02, 0);
                        }
                    }
                    default -> { // Aura um den ganzen Koerper
                        level.addParticle(part,
                                px + (rnd.nextDouble() - 0.5) * 1.0,
                                py + rnd.nextDouble() * height,
                                pz + (rnd.nextDouble() - 0.5) * 1.0,
                                0, 0.02, 0);
                    }
                }
            }
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {}
    }
}
