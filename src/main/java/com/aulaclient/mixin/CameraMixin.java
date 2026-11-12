package com.aulaclient.mixin;

import com.aulaclient.AulaClient;
import com.aulaclient.config.AulaConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Stretched Resolution: aendert die Breite, die der Welt-Kamera uebergeben wird,
 * sodass das effektive Seitenverhaeltnis dem Ziel-Ratio entspricht.
 *
 * Ziel: Camera.setupPerspective(float zNear, float zFar, float fov, float width, float height)
 * -> width ist das 4. float-Argument (ordinal 3).
 * require = 0: Wenn sich die Signatur aendert, wird der Mixin uebersprungen statt das Spiel zu crashen.
 */
@Mixin(Camera.class)
public class CameraMixin {

    @ModifyVariable(method = "setupPerspective", at = @At("HEAD"), argsOnly = true, ordinal = 3, require = 0)
    private float aula$stretchWidth(float width) {
        try {
            if (AulaClient.modules == null || !AulaClient.modules.stretch.enabled) return width;
            var win = Minecraft.getInstance().getWindow();
            float windowAspect = (float) win.getWidth() / (float) win.getHeight();
            float target = AulaConfig.get().stretchRatio;
            if (target < 0.1f || windowAspect <= 0f) return width;
            return width * (target / windowAspect);
        } catch (Throwable t) {
            return width;
        }
    }
}
