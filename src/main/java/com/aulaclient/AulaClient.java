package com.aulaclient;

import com.aulaclient.config.AulaConfig;
import com.aulaclient.gui.AulaScreen;
import com.aulaclient.gui.AulaTitleScreen;
import com.aulaclient.module.ModuleManager;
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

        modules = new ModuleManager(zoomKey);
        modules.applyConfig();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            modules.tick(mc);
            if (AulaConfig.get().customTitle && mc.gui.screen() instanceof TitleScreen) {
                mc.setScreenAndShow(new AulaTitleScreen());
            }
            while (openMenu.consumeClick()) {
                mc.setScreenAndShow(new AulaScreen());
            }
        });

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
}
