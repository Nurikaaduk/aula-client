package com.aulaclient.gui;

import com.aulaclient.AulaClient;
import com.aulaclient.config.AulaConfig;
import com.aulaclient.module.HudModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * HUD-Editor: Module mit gedrueckter linker Maustaste verschieben (Raster 2px).
 * Unten: "Reset positions" setzt alle Positionen zurueck. ESC = zurueck ins Menue.
 */
public class HudEditorScreen extends Screen {
    private static final int GRID = 2;

    private HudModule dragging;
    private int dx, dy;

    private final Screen parent;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int accent = AulaConfig.get().accent;
        g.fill(0, 0, width, height, 0x60000000);

        // Mittellinien als Orientierung
        g.fill(width / 2, 0, width / 2 + 1, height, 0x30FFFFFF);
        g.fill(0, height / 2, width, height / 2 + 1, 0x30FFFFFF);

        for (HudModule m : AulaClient.modules.modules) {
            if (!m.enabled || !m.movable()) continue;
            m.render(g, minecraft);
            boolean hov = mx >= m.x && mx <= m.x + m.w && my >= m.y && my <= m.y + m.h;
            int col = (m == dragging || hov) ? accent : 0x80FFFFFF;
            outline(g, m.x - 1, m.y - 1, m.x + m.w + 1, m.y + m.h + 1, col);
            if (m == dragging || hov) {
                g.text(font, m.name, m.x, m.y - 11, accent, true);
            }
        }

        // Zuruecksetzen-Button
        int bw = 110, bh = 20, bx = (width - bw) / 2, by = height - 34;
        boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        g.fill(bx, by, bx + bw, by + bh, hov ? accent : 0xEE1C1C28);
        String label = "Reset positions";
        g.text(font, label, bx + (bw - font.width(label)) / 2, by + 6,
                hov ? 0xFF101018 : 0xFFFFFFFF, false);

        String hint = "Drag to move  |  ESC: back";
        g.text(font, hint, (width - font.width(hint)) / 2, 8, 0xFFB0B0C0, true);
    }

    private void outline(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int col) {
        g.fill(x1, y1, x2, y1 + 1, col);
        g.fill(x1, y2 - 1, x2, y2, col);
        g.fill(x1, y1, x1 + 1, y2, col);
        g.fill(x2 - 1, y1, x2, y2, col);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x(), my = (int) event.y();
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int bw = 110, bh = 20, bx = (width - bw) / 2, by = height - 34;
            if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                for (HudModule m : AulaClient.modules.modules) m.resetPosition();
                return true;
            }
            var mods = AulaClient.modules.modules;
            for (int i = mods.size() - 1; i >= 0; i--) {
                HudModule m = mods.get(i);
                if (m.enabled && m.movable()
                        && mx >= m.x && mx <= m.x + m.w && my >= m.y && my <= m.y + m.h) {
                    dragging = m;
                    dx = mx - m.x;
                    dy = my - m.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double ddx, double ddy) {
        if (dragging != null) {
            int nx = (int) event.x() - dx;
            int ny = (int) event.y() - dy;
            nx = Math.round((float) nx / GRID) * GRID;
            ny = Math.round((float) ny / GRID) * GRID;
            dragging.x = Math.max(0, Math.min(width - dragging.w, nx));
            dragging.y = Math.max(0, Math.min(height - dragging.h, ny));
            return true;
        }
        return super.mouseDragged(event, ddx, ddy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            AulaConfig.get().positions.put(dragging.name, new int[]{dragging.x, dragging.y});
            AulaConfig.save();
            dragging = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(new AulaScreen(parent));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
