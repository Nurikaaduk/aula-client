package com.aulaclient.render;

import com.aulaclient.AulaClient;
import com.aulaclient.config.AulaConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/**
 * Eigene, rein lokale Spieler-Cosmetics: Cape, Fluegel, "Liquid Glass"-Koerper.
 * Nichts davon wird ueber das Netzwerk gesendet; andere Spieler sehen es nicht.
 * Registrierung ueber Fabric's Render-Layer-Callback, keine Mixins noetig.
 */
public final class CosmeticLayers {
    private CosmeticLayers() {}

    private static final Identifier BLANK = Identifier.fromNamespaceAndPath("aula-client", "textures/gui/cosmetic_blank.png");
    private static final UvMapping IDENTITY_UV = new UvMapping() {
        @Override
        public float getU(float u) { return u; }
        @Override
        public float getV(float v) { return v; }
    };

    @SuppressWarnings("unchecked")
    public static void register() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper) -> {
            if (entityType != EntityType.PLAYER) return;
            try {
                RenderLayerParent<AvatarRenderState, PlayerModel> parent =
                        (RenderLayerParent<AvatarRenderState, PlayerModel>) entityRenderer;
                registrationHelper.register(new CapeLayer(parent));
                registrationHelper.register(new WingsLayer(parent));
                registrationHelper.register(new GlassBodyLayer(parent));
            } catch (Throwable ignored) {
                // Falls sich die Render-API aendert, einfach ohne diese Cosmetics weitermachen.
            }
        });
    }

    private static boolean isLocalPlayer(AvatarRenderState state) {
        var mc = Minecraft.getInstance();
        return mc.player != null && state.id == mc.player.getId();
    }

    /** Einfache, segmentierte Stoff-Cape, die mit der Cape-Schwung-Physik des Spiels mitschwingt. */
    static class CapeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private final ModelPart cape;

        CapeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition pd = mesh.getRoot();
            pd.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 0).addBox(-4f, 0f, -1f, 8f, 14f, 0.5f),
                    PartPose.offset(0f, 0f, 2.2f));
            cape = LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("cape");
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
            if (!AulaClient.modules.cape.enabled || !isLocalPlayer(state)) return;
            poseStack.pushPose();
            float sway = (float) Math.sin(System.currentTimeMillis() / 180.0) * 4f;
            cape.xRot = (float) Math.toRadians(6f + state.capeLean * 0.5f + sway);
            cape.zRot = state.capeLean2 * 0.02f;
            int col = AulaConfig.get().accent;
            collector.submitModelPart(cape, poseStack, RenderTypes.entityCutoutNoCull(BLANK), light, OverlayTexture.NO_OVERLAY, IDENTITY_UV, col);
            poseStack.popPose();
        }
    }

    /** Zwei flache, leicht flatternde Fluegel am Ruecken. */
    static class WingsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private final ModelPart left, right;

        WingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition pd = mesh.getRoot();
            pd.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(0, 0).addBox(0f, -8f, -1f, 10f, 8f, 0.3f),
                    PartPose.offsetAndRotation(1f, 2f, 2.5f, 0f, 0.5f, 0.2f));
            pd.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(0, 0).addBox(-10f, -8f, -1f, 10f, 8f, 0.3f),
                    PartPose.offsetAndRotation(-1f, 2f, 2.5f, 0f, -0.5f, -0.2f));
            var baked = LayerDefinition.create(mesh, 16, 16).bakeRoot();
            left = baked.getChild("wing_l");
            right = baked.getChild("wing_r");
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
            if (!AulaClient.modules.wings.enabled || !isLocalPlayer(state)) return;
            poseStack.pushPose();
            float flap = (float) Math.sin(System.currentTimeMillis() / 260.0) * 0.25f;
            left.yRot = 0.5f + flap;
            right.yRot = -0.5f - flap;
            int col = AulaConfig.get().accent;
            RenderType rt = RenderTypes.entityTranslucentCull(BLANK);
            collector.submitModelPart(left, poseStack, rt, light, OverlayTexture.NO_OVERLAY, IDENTITY_UV, col);
            collector.submitModelPart(right, poseStack, rt, light, OverlayTexture.NO_OVERLAY, IDENTITY_UV, col);
            poseStack.popPose();
        }
    }

    /** Rendert den Koerper ein zweites Mal durchsichtig ("Liquid Glass"). */
    static class GlassBodyLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        GlassBodyLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
            if (!AulaClient.modules.glassBody.enabled || !isLocalPlayer(state)) return;
            float a = Math.max(0.05f, Math.min(0.9f, AulaConfig.get().glassOpacity));
            int alpha = (int) (a * 255);
            int tint = (alpha << 24) | 0xD8ECFF;
            collector.submitModel(getParentModel(), state, poseStack, RenderTypes.entityTranslucent(BLANK), light, OverlayTexture.NO_OVERLAY, tint);
        }
    }
}
