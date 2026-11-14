package com.aulaclient.module;

import com.aulaclient.config.AulaConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
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
    public final FreeLook freeLook;
    public final Cape cape;
    public final Wings wings;
    public final GlassBody glassBody;

    public ModuleManager(KeyMapping zoomKey, KeyMapping freeLookKey) {
        modules.add(new SimpleText("FPS", "Show your current frames per second", 4, 4,
                mc -> mc.getFps() + " FPS"));
        modules.add(new SimpleText("Coordinates", "Show your XYZ position", 4, 20,
                mc -> mc.player == null ? "" : String.format("XYZ %.1f / %.1f / %.1f",
                        mc.player.getX(), mc.player.getY(), mc.player.getZ())));
        modules.add(new SimpleText("Clock", "Show the local time", 4, 36,
                mc -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        modules.add(new Cps());
        modules.add(new Keystrokes());
        modules.add(new Armor());
        modules.add(new DamageIndicator());
        modules.add(new PotionBar());
        modules.add(new FpsGraph());
        modules.add(new FavoritesBar());
        zoom = new Zoom(zoomKey);
        crosshair = new Crosshair();
        stretch = new Stretch();
        characterFx = new CharacterFx();
        modules.add(zoom);
        modules.add(crosshair);
        modules.add(stretch);
        modules.add(characterFx);
        freeLook = new FreeLook(freeLookKey);
        cape = new Cape();
        wings = new Wings();
        glassBody = new GlassBody();
        modules.add(freeLook);
        modules.add(cape);
        modules.add(wings);
        modules.add(glassBody);

        for (HudModule m : modules) {
            if (m instanceof Zoom || m instanceof Stretch || m instanceof FreeLook) m.category = "Visuals";
            else if (m instanceof Crosshair || m instanceof CharacterFx
                    || m instanceof Cape || m instanceof Wings || m instanceof GlassBody) m.category = "Cosmetics";
            else m.category = "HUD";
        }
    }

    /** Uebernimmt gespeicherten Zustand aus der Config. */
    public void applyConfig() {
        AulaConfig cfg = AulaConfig.get();
        for (HudModule m : modules) {
            boolean defaultOn = !(m instanceof Crosshair || m instanceof Stretch || m instanceof CharacterFx
                    || m instanceof FreeLook || m instanceof Cape || m instanceof Wings || m instanceof GlassBody);
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
            super("CPS", "Show your clicks per second", 4, 52);
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
            super("Keystrokes", "Show pressed movement and mouse keys", 4, 72);
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
            super("Armor Status", "Show armor pieces and durability", 4, 156);
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
            super("Zoom", "Hold a key to zoom smoothly (default: C)", 0, 0);
            this.key = key;
        }

        @Override
        public List<Setting> settings() {
            AulaConfig c = AulaConfig.get();
            return List.of(new Setting.Slider("FOV", 30, 70, 5, 0,
                    () -> c.zoomFov, v -> c.zoomFov = (int) Math.round(v)));
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
            super("Custom Crosshair", "Customize the style, size and color of your crosshair", 0, 0);
        }

        @Override
        public List<Setting> settings() {
            AulaConfig c = AulaConfig.get();
            return List.of(
                    new Setting.Choice("Style", new String[]{"Cross", "Dot", "Plus", "Square"},
                            () -> c.crosshairStyle, v -> c.crosshairStyle = v),
                    new Setting.Slider("Size", 2, 12, 1, 0,
                            () -> c.crosshairSize, v -> c.crosshairSize = (int) Math.round(v)),
                    new Setting.Toggle("Accent", () -> c.crosshairAccent, v -> c.crosshairAccent = v));
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
            super("Stretched Res", "Widen the world view, keeping the HUD unchanged", 0, 0);
        }

        private static final String[] PRESETS = {"Custom", "4:3", "5:4", "1:1", "16:10", "3:2"};
        private static final float[] RATIOS = {0f, 4f / 3f, 5f / 4f, 1f, 16f / 10f, 3f / 2f};

        @Override
        public List<Setting> settings() {
            AulaConfig c = AulaConfig.get();
            return List.of(
                    new Setting.Choice("Preset", PRESETS, () -> {
                        for (int i = 1; i < RATIOS.length; i++) {
                            if (Math.abs(c.stretchRatio - RATIOS[i]) < 0.005f) return i;
                        }
                        return 0;
                    }, v -> {
                        if (v > 0) c.stretchRatio = RATIOS[v];
                    }),
                    new Setting.Slider("Ratio", 0.5, 2.5, 0.05, 2,
                            () -> c.stretchRatio, v -> c.stretchRatio = (float) v));
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
        public static final String[] STYLE_NAMES = {"Halo", "Spiral", "Trail", "Aura"};
        public static final String[] PARTICLE_NAMES = {
                "Hearts", "Sparks", "Flames", "Clouds", "Notes", "Glitter", "Runes"
        };
        private static final ParticleOptions[] PARTICLES = {
                ParticleTypes.HEART, ParticleTypes.END_ROD, ParticleTypes.FLAME, ParticleTypes.CLOUD,
                ParticleTypes.NOTE, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.ENCHANT
        };

        private int ticks;

        CharacterFx() {
            super("Character FX", "Particle effects around your character (only you see them)", 0, 0);
        }

        @Override
        public List<Setting> settings() {
            AulaConfig c = AulaConfig.get();
            return List.of(
                    new Setting.Choice("Style", STYLE_NAMES, () -> c.fxStyle, v -> c.fxStyle = v),
                    new Setting.Choice("Particles", PARTICLE_NAMES, () -> c.fxParticle, v -> c.fxParticle = v),
                    new Setting.Slider("Density", 1, 4, 1, 0,
                            () -> c.fxDensity, v -> c.fxDensity = (int) Math.round(v)));
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

    /** Zeigt kurz aufploppende, verblassende Schadenszahlen, wenn du Schaden nimmst. */
    static class DamageIndicator extends HudModule {
        private record Pop(float amount, long at) {}
        private final List<Pop> pops = new ArrayList<>();
        private float lastHealth = -1f;

        DamageIndicator() {
            super("Damage Indicator", "Show a floating number when you take damage", 4, 232);
            w = 70; h = 16;
        }

        @Override
        public void tick(Minecraft mc) {
            if (mc.player == null) return;
            float hp = mc.player.getHealth();
            if (lastHealth >= 0 && hp < lastHealth - 0.01f) {
                pops.add(new Pop(lastHealth - hp, System.currentTimeMillis()));
            }
            lastHealth = hp;
            long now = System.currentTimeMillis();
            pops.removeIf(p -> now - p.at > 1200);
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {
            long now = System.currentTimeMillis();
            int i = 0;
            for (Pop p : pops) {
                float age = (now - p.at) / 1200f;
                float alpha = 1f - age;
                int yy = y - (int) (age * 14);
                String t = "-" + (Math.round(p.amount * 10) / 10.0);
                g.text(mc.font, t, x, yy, ((int) (alpha * 255) << 24) | 0xFF4040, true);
                i++;
            }
            w = 70;
            h = 16;
        }
    }

    /** Eigene, kompakte Anzeige aktiver Trankeffekte statt der Vanilla-Icons. */
    static class PotionBar extends HudModule {
        PotionBar() {
            super("Potion Effects", "Show active potion effects in a compact list", 4, 252);
            w = 110;
            h = 14;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {
            if (mc.player == null) return;
            var effects = mc.player.getActiveEffects();
            if (effects.isEmpty()) return;
            int row = 0;
            for (MobEffectInstance eff : effects) {
                int ry = y + row * 16;
                String name = eff.getEffect().value().getDisplayName().getString();
                String dur = formatTime(eff.getDuration());
                String line = name + " " + (eff.getAmplifier() + 1) + "  " + dur;
                w = Math.max(w, mc.font.width(line) + 10);
                g.fill(x, ry, x + w, ry + 15, 0x90101018);
                g.fill(x, ry, x + 2, ry + 15, AulaConfig.get().accent);
                g.text(mc.font, line, x + 5, ry + 4, 0xFFFFFFFF, true);
                row++;
            }
            h = Math.max(14, row * 16);
        }

        private static String formatTime(int ticks) {
            int s = ticks / 20;
            return String.format("%d:%02d", s / 60, s % 60);
        }
    }

    /** Verlauf der FPS der letzten Sekunden als kleines Liniendiagramm. */
    static class FpsGraph extends HudModule {
        private final Deque<Integer> history = new ArrayDeque<>();

        FpsGraph() {
            super("FPS Graph", "Show a short history of your FPS as a line graph", 4, 290);
            w = 90;
            h = 34;
        }

        @Override
        public void tick(Minecraft mc) {
            history.addLast(mc.getFps());
            while (history.size() > 60) history.pollFirst();
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {
            g.fill(x, y, x + w, y + h, 0x90101018);
            g.fill(x, y, x + 2, y + h, AulaConfig.get().accent);
            if (history.isEmpty()) return;
            int max = Math.max(30, history.stream().mapToInt(Integer::intValue).max().orElse(60));
            int gx = x + 6, gy = y + 4, gw = w - 10, gh = h - 16;
            int i = 0, n = history.size();
            Integer prevV = null;
            for (int v : history) {
                int px = gx + (int) ((i / (float) Math.max(1, n - 1)) * gw);
                int py = gy + gh - (int) ((v / (float) max) * gh);
                g.fill(px, py, px + 1, gy + gh, (AulaConfig.get().accent & 0xFFFFFF) | 0x30000000);
                if (prevV != null) {
                    int ppy = gy + gh - (int) ((prevV / (float) max) * gh);
                    int steps = Math.max(1, Math.abs(py - ppy));
                    for (int s2 = 0; s2 <= steps; s2++) {
                        int yy2 = ppy + (py - ppy) * s2 / steps;
                        g.fill(px - 1, yy2, px + 1, yy2 + 1, 0xFFFFFFFF);
                    }
                }
                prevV = v;
                i++;
            }
            g.text(mc.font, mc.getFps() + " FPS", x + 5, y + h - 11, 0xFFFFFFFF, true);
        }
    }

    /** Zeigt deine Favoriten-Module als kleine Liste mit An/Aus-Punkt (nur Uebersicht, umschalten im Client-Menue). */
    static class FavoritesBar extends HudModule {
        FavoritesBar() {
            super("Favorites Bar", "Show your favorite modules and their on/off state at a glance", 4, 330);
            w = 100;
            h = 16;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {
            List<String> favs = AulaConfig.get().favorites;
            if (favs.isEmpty()) return;
            int row = 0;
            ModuleManager mgr = com.aulaclient.AulaClient.modules;
            for (String fav : favs) {
                HudModule m = mgr == null ? null : mgr.modules.stream().filter(mm -> mm.name.equals(fav)).findFirst().orElse(null);
                if (m == null) continue;
                int ry = y + row * 16;
                int dotCol = m.enabled ? AulaConfig.get().accent : 0xFF55555A;
                g.fill(x, ry + 4, x + 6, ry + 10, dotCol);
                g.text(mc.font, m.name, x + 11, ry + 3, 0xFFE0E0E8, true);
                w = Math.max(w, mc.font.width(m.name) + 16);
                row++;
            }
            h = Math.max(16, row * 16);
        }
    }

    /** Taste haelt Third-Person-Rueckansicht + Zoom gleichzeitig (einfache Freilook-Kombo). */
    public static class FreeLook extends HudModule {
        private final KeyMapping key;
        private net.minecraft.client.CameraType previous;
        private boolean active;

        FreeLook(KeyMapping key) {
            super("Free Look", "Hold a key to combine third-person view with zoom", 0, 0);
            this.key = key;
        }

        @Override
        public boolean movable() {
            return false;
        }

        @Override
        public void tick(Minecraft mc) {
            boolean down = key.isDown() && mc.gui.screen() == null;
            if (down && !active) {
                previous = mc.options.getCameraType();
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                active = true;
            } else if (!down && active) {
                if (previous != null) mc.options.setCameraType(previous);
                active = false;
            }
        }

        @Override
        public void onDisable(Minecraft mc) {
            if (active && previous != null) mc.options.setCameraType(previous);
            active = false;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {}
    }

    /** Eigene Cape-Cosmetic, nur lokal sichtbar (Rendering siehe render/CosmeticLayers). */
    public static class Cape extends HudModule {
        Cape() {
            super("Cape", "Wear a cape only you can see (made by Aula Client, not sent to others)", 0, 0);
        }

        @Override
        public boolean movable() {
            return false;
        }

        @Override
        public void setEnabled(boolean on) {
            super.setEnabled(on);
            AulaConfig.get().capeEnabled = on;
            AulaConfig.save();
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {}
    }

    /** Eigene Fluegel-Cosmetic, nur lokal sichtbar. */
    public static class Wings extends HudModule {
        Wings() {
            super("Wings", "Wear wings only you can see", 0, 0);
        }

        @Override
        public boolean movable() {
            return false;
        }

        @Override
        public void setEnabled(boolean on) {
            super.setEnabled(on);
            AulaConfig.get().wingsEnabled = on;
            AulaConfig.save();
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {}
    }

    /** Durchsichtiger "Liquid Glass"-Koerper, nur lokal sichtbar. */
    public static class GlassBody extends HudModule {
        GlassBody() {
            super("Liquid Glass Body", "Render your own body translucent, like frosted glass (only you see it)", 0, 0);
        }

        @Override
        public List<Setting> settings() {
            AulaConfig c = AulaConfig.get();
            return List.of(new Setting.Slider("Opacity", 0.15, 0.85, 0.05, 2,
                    () -> c.glassOpacity, v -> c.glassOpacity = (float) v));
        }

        @Override
        public boolean movable() {
            return false;
        }

        @Override
        public void setEnabled(boolean on) {
            super.setEnabled(on);
            AulaConfig.get().glassBodyEnabled = on;
            AulaConfig.save();
        }

        @Override
        public void render(GuiGraphicsExtractor g, Minecraft mc) {}
    }
}
