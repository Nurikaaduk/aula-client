package com.aulaclient.mixin;

import com.aulaclient.AulaClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stretched Resolution: skaliert die X-Achse der Welt-Projektion.
 * Effektives Seitenverhaeltnis = Ziel-Ratio statt Fensterverhaeltnis.
 *
 * require = 0: Falls sich die Methode in 26.x anders heisst, wird der Mixin still uebersprungen
 * (Stretch wirkt dann nicht), das Spiel startet aber trotzdem.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getProjectionMatrix", at = @At("RETURN"), cancellable = true, require = 0)
    private void aula$stretch(float fov, CallbackInfoReturnable cir) {
        try {
            if (AulaClient.modules == null || !AulaClient.modules.stretch.enabled) return;
            Object ret = cir.getReturnValue();
            if (!(ret instanceof Matrix4f m)) return;

            var win = Minecraft.getInstance().getWindow();
            float windowAspect = (float) win.getWidth() / (float) win.getHeight();
            float target = com.aulaclient.config.AulaConfig.get().stretchRatio;
            if (target <= 0.1f) return;

            // m00 = f / aspect -> effektives Aspect = target
            m.m00(m.m00() * (windowAspect / target));
            cir.setReturnValue(m);
        } catch (Throwable ignored) {
            // Rendering darf nie wegen des Cosmetics-Moduls abstuerzen
        }
    }
}
