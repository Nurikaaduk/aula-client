package com.aulaclient.render;

import com.aulaclient.AulaClient;
import com.aulaclient.config.AulaConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/**
 * Eigene, rein lokale Spieler-Cosmetics: Cape, Fluegel, "Liquid Glass"-Koerper.
 * Nichts davon wird ueber das Netzwerk gesendet; andere Spieler sehen es nicht.
 * Registrierung ueber Fabric's Feature-Renderer-Callback, keine Mixins noetig.
 */
public final class CosmeticLayers {
    private CosmeticLayers() {}

    private static final Identifier BLANK = Identifier.fromNamespaceAndPath("aula-client", "textures/gui/cosmetic_blank.png");

    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityType != EntityType.PLAYER) return;
            if (!(entityRenderer instanceof RenderLayerParent)) return;
            try {
                registrationHelper.register(new CapeLayer((RenderLayerParent<PlayerRenderState, ?>) entityRenderer));
                registrationHelper.register(new WingsLayer((RenderLayerParent<PlayerRenderState, ?>) entityRenderer));
                registrationHelper.register(new GlassBodyLayer((RenderLayerParent<PlayerRenderState, ?>) entityRenderer));
            } catch (Throwable ignored) {
                // Falls sich die Render-API zwischen Versionen unterscheidet, einfach ohne diese Cosmetics weitermachen.
            }
        });
    }

    private static boolean isLocalPlayer(PlayerRenderState state) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc.player != null && state.name != null && state.name.getString().equals(mc.player.getGameProfile().name());
    }

    /** Einfache, segmentierte Stoff-Cape, die in der Luft leicht nachschwingt. */
    static class CapeLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<PlayerRenderState, net.minecraft.client.model.PlayerModel> {
        private final ModelPart root;

        CapeLayer(RenderLayerParent<PlayerRenderState, net.minecraft.client.model.PlayerModel> parent) {
            super(parent);
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition pd = mesh.getRoot();
            pd.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 0).addBox(-4f, 0f, -1f, 8f, 14f, 0.5f),
                    PartPose.offset(0f, 0f, 2.2f));
            root = LayerDefinition.create(mesh, 16, 16).bakeRoot();
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, PlayerRenderState state, float yRot, float xRot) {
            if (!AulaClient.modules.cape.enabled || !isLocalPlayer(state)) return;
            poseStack.pushPose();
            poseStack.translate(0f, 0f, 0f);
            float sway = (float) Math.sin(System.currentTimeMillis() / 180.0) * 6f;
            root.getChild("cape").xRot = (float) Math.toRadians(8 + sway * 0.4);
            int col = AulaConfig.get().accent;
            root.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(BLANK)), light,
                    OverlayTexture.NO_OVERLAY, col);
            poseStack.popPose();
        }
    }

    /** Zwei flache, leicht flatternde Fluegel am Ruecken. */
    static class WingsLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<PlayerRenderState, net.minecraft.client.model.PlayerModel> {
        private final ModelPart left, right;

        WingsLayer(RenderLayerParent<PlayerRenderState, net.minecraft.client.model.PlayerModel> parent) {
            super(parent);
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition pd = mesh.getRoot();
            pd.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(0, 0).addBox(0f, -8f, -1f, 10f, 8f, 0.3f),
                    PartPose.offsetAndRotation(1f, 2f, 2.5f, 0f, 0.5f, 0.2f));
            pd.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(0, 0).addBox(-10f, -8f, -1f, 10f, 8f, 0.3f),
                    PartPose.offsetAndRotation(-1f, 2f, 2.5f, 0f, -0.5f, -0.2f));
            LayerDefinition def = LayerDefinition.create(mesh, 16, 16);
            var baked = def.bakeRoot();
            left = baked.getChild("wing_l");
            right = baked.getChild("wing_r");
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, PlayerRenderState state, float yRot, float xRot) {
            if (!AulaClient.modules.wings.enabled || !isLocalPlayer(state)) return;
            poseStack.pushPose();
            float flap = (float) Math.sin(System.currentTimeMillis() / 260.0) * 0.25f;
            left.yRot = 0.5f + flap;
            right.yRot = -0.5f - flap;
            int col = AulaConfig.get().accent;
            var consumer = buffer.getBuffer(RenderType.entityTranslucentCull(BLANK));
            left.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, col);
            right.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, col);
            poseStack.popPose();
        }
    }

    /** Rendert den Koerper ein zweites Mal durchsichtig und leicht froststichig ("Liquid Glass"). */
    static class GlassBodyLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<PlayerRenderState, net.minecraft.client.model.PlayerModel> {
        GlassBodyLayer(RenderLayerParent<PlayerRenderState, net.minecraft.client.model.PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, PlayerRenderState state, float yRot, float xRot) {
            if (!AulaClient.modules.glassBody.enabled || !isLocalPlayer(state)) return;
            float a = Math.max(0.05f, Math.min(0.9f, AulaConfig.get().glassOpacity));
            int alpha = (int) (a * 255);
            int tint = (alpha << 24) | 0xD8ECFF;
            getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucent(BLANK)),
                    light, OverlayTexture.NO_OVERLAY, tint);
        }
    }
}
