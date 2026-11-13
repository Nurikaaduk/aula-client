package com.aulaclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Speichert alle Einstellungen in config/aula-client.json. */
public class AulaConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aula-client.json");

    public Map<String, Boolean> enabled = new LinkedHashMap<>();
    public Map<String, int[]> positions = new LinkedHashMap<>();

    public int theme = 6;
    public int accent = Themes.COLORS[6];
    public java.util.List<String> favorites = new java.util.ArrayList<>();
    public boolean customTitle = true;
    /** Deckkraft der Menue-Flaechen (0.3 .. 1.0). */
    public float uiOpacity = 0.72f;

    // Cosmetics: Crosshair
    public int crosshairStyle = 0;   // 0 Kreuz, 1 Punkt, 2 Plus (duenn), 3 Quadrat
    public int crosshairSize = 5;    // 2..12
    public boolean crosshairAccent = true;

    // Zoom
    public int zoomFov = 30;         // 30..70

    // Stretched Resolution: Ziel-Seitenverhaeltnis (Breite/Hoehe), z. B. 4:3 = 1.333
    public float stretchRatio = 1.3333f;

    // Charakter-Cosmetics (nur lokal sichtbar)
    public int fxStyle = 0;          // 0 Halo, 1 Wirbel, 2 Spur, 3 Aura
    public int fxParticle = 1;       // Index in CharacterFx.PARTICLE_NAMES
    public int fxDensity = 2;        // 1..4

    private static AulaConfig instance = new AulaConfig();

    public static AulaConfig get() {
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    AulaConfig c = GSON.fromJson(r, AulaConfig.class);
                    if (c != null) instance = c;
                }
            }
        } catch (Exception e) {
            System.err.println("[Aula Client] Konfiguration konnte nicht geladen werden: " + e);
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(instance, w);
        } catch (Exception e) {
            System.err.println("[Aula Client] Konfiguration konnte nicht gespeichert werden: " + e);
        }
    }
}
