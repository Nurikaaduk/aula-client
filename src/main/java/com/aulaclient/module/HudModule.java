package com.aulaclient.module;

import com.aulaclient.config.AulaConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Basisklasse fuer alle Module. */
public abstract class HudModule {
    public final String name;
    public final String description;
    public final int defX, defY;
    public boolean enabled;
    public int x, y;
    /** Groesse beim letzten Rendern (fuer den HUD-Editor). */
    public int w = 60, h = 14;

    protected HudModule(String name, String description, int defX, int defY) {
        this.name = name;
        this.description = description;
        this.defX = defX;
        this.defY = defY;
        this.x = defX;
        this.y = defY;
    }

    /** false = Modul hat keine eigene HUD-Position (Zoom, Crosshair). */
    public boolean movable() {
        return true;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean on) {
        enabled = on;
        if (!on) onDisable(Minecraft.getInstance());
        AulaConfig.get().enabled.put(name, enabled);
        AulaConfig.save();
    }

    public void onDisable(Minecraft mc) {}

    public void tick(Minecraft mc) {}

    public abstract void render(GuiGraphics g, Minecraft mc);

    public void resetPosition() {
        x = defX;
        y = defY;
        AulaConfig.get().positions.remove(name);
        AulaConfig.save();
    }

    /** Kleine halbtransparente Box mit Akzentstreifen und Text. */
    protected void box(GuiGraphics g, Minecraft mc, String text) {
        w = mc.font.width(text) + 8;
        h = 14;
        g.fill(x, y, x + w, y + h, 0x90101018);
        g.fill(x, y, x + 2, y + h, AulaConfig.get().accent);
        g.drawString(mc.font, text, x + 5, y + 3, 0xFFFFFFFF, true);
    }
}
