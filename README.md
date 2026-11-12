# Aula Client

Eigener Fabric-Client fuer Minecraft 26.3 (Java 25). Komplett eigener Code, keine Combat-/Movement-/Automations-Module.

## Features
- **Menue** auf Rechts-Shift mit Tabs: Module, Cosmetics, Themes
- **HUD-Module:** FPS, Koordinaten, Uhrzeit, CPS, Keystrokes, Ruestung (mit Haltbarkeit)
- **Zoom** (Taste C, weich interpoliert, FOV einstellbar)
- **Cosmetics:** eigenes Crosshair (Kreuz, Punkt, Plus, Quadrat; Groesse; Akzentfarbe oder Weiss) mit Live-Vorschau
- **Stretched Resolution:** Presets 4:3, 5:4, 1:1, 16:10, 3:2 oder frei einstellbar (Mixin in `GameRenderer`)
- **Charakter-Cosmetics:** Partikel-Effekte um deinen Charakter (Halo, Wirbel, Spur, Aura) mit 7 Partikelarten, nur lokal sichtbar
- **HUD-Editor:** Module per Maus verschieben, Raster, Zuruecksetzen
- **Farbthemen:** 7 Akzentfarben
- Alles wird in `config/aula-client.json` gespeichert

## Bauen
1. In `gradle.properties` die Fabric-API-Version fuer 26.3 eintragen (https://fabricmc.net/develop).
2. `gradle build` - die JAR liegt in `build/libs/`.

## Hinweis
Der Code wurde ohne Internetzugang geschrieben und nicht kompiliert. Bei Minecraft 26.x aendern sich Namen
teils (z. B. `GuiGraphics`, `MouseButtonEvent`, `KeyMapping.Category`, `Identifier`,
`HudElementRegistry`); Compilerfehler zeigen die Stellen zum Anpassen.
