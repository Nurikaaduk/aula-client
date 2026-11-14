package com.aulaclient;

import com.aulaclient.config.AulaConfig;
import com.aulaclient.gui.AulaScreen;
import com.aulaclient.gui.AulaTitleScreen;
import com.aulaclient.module.ModuleManager;
import com.aulaclient.render.CosmeticLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Screenshot;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Aula Client - Einstiegspunkt.
 * Menue (Rechts-Shift), Zoom (C), HUD-Module, Crosshair-Cosmetics, Farbthemen.
 */
public class AulaClient implements ClientModInitializer {
    public static final String ID = "aula-client";
    public static final String NAME = "Aula Client";

    public static ModuleManager modules;

    @Override
    public void onInitializeClient() {
        AulaConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ID, "main"));
        KeyMapping openMenu = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.aula.menu", InputConstants.KEY_RSHIFT, category));
        KeyMapping zoomKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.aula.zoom", InputConstants.KEY_C, category));
        KeyMapping freeLookKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.aula.freelook", InputConstants.KEY_V, category));
        KeyMapping screenshotKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.aula.screenshot", InputConstants.KEY_F6, category));

        modules = new ModuleManager(zoomKey, freeLookKey);
        modules.applyConfig();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            modules.tick(mc);
            if (AulaConfig.get().customTitle && mc.gui.screen() instanceof TitleScreen) {
                mc.setScreenAndShow(new AulaTitleScreen());
            }
            while (openMenu.consumeClick()) {
                mc.setScreenAndShow(new AulaScreen());
            }
            while (screenshotKey.consumeClick()) {
                takeFramedScreenshot(mc);
            }
        });

        CosmeticLayers.register();

        // Eigene HUD-Module
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ID, "hud"),
                (graphics, tickCounter) -> modules.renderHud(graphics));

        // Crosshair ersetzen, wenn das Modul aktiv ist; sonst Vanilla-Crosshair
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original ->
                (graphics, tickCounter) -> {
                    var mc = net.minecraft.client.Minecraft.getInstance();
                    if (modules.crosshair.enabled) {
                        modules.crosshair.draw(graphics);
                    } else {
                        original.extractRenderState(graphics, tickCounter);
                    }
                });
    }

    /** Macht einen normalen Screenshot und zeichnet danach einen duennen Rahmen in Theme-Farbe hinein. */
    private static void takeFramedScreenshot(Minecraft mc) {
        try {
            Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), (Component msg) -> {
                mc.execute(() -> {
                    mc.gui.getChat().addMessage(msg);
                    frameLastScreenshot(mc);
                });
            });
        } catch (Throwable ignored) {
            // Falls sich die Screenshot-API geaendert hat, einfach nichts weiter tun.
        }
    }

    private static void frameLastScreenshot(Minecraft mc) {
        try {
            java.io.File dir = new java.io.File(mc.gameDirectory, "screenshots");
            java.io.File[] files = dir.listFiles((d, n) -> n.endsWith(".png"));
            if (files == null || files.length == 0) return;
            java.io.File latest = files[0];
            for (java.io.File f : files) if (f.lastModified() > latest.lastModified()) latest = f;
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(latest);
            if (img == null) return;
            java.awt.Graphics2D gfx = img.createGraphics();
            int accent = com.aulaclient.config.AulaConfig.get().accent;
            gfx.setColor(new java.awt.Color(accent, true));
            int t = Math.max(3, img.getWidth() / 250);
            gfx.setStroke(new java.awt.BasicStroke(t));
            gfx.drawRect(t, t, img.getWidth() - 2 * t, img.getHeight() - 2 * t);
            gfx.dispose();
            javax.imageio.ImageIO.write(img, "png", latest);
        } catch (Throwable ignored) {
            // Rahmen ist nur Dekoration; wenn es schiefgeht, bleibt der normale Screenshot erhalten.
        }
    }
}
