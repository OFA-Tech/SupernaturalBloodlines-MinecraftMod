package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BossNpc;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;

/** A boss that is a person: the player model with the finished skin in {@code textures/entity/boss/<id>.png}, armor and the held item. A desiccated one lies flat. */
public final class HumanoidBossRenderer<T extends BossNpc> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    public HumanoidBossRenderer(EntityRendererProvider.Context context, boolean slim) {
        super(context, new PlayerModel<>(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5F);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(T boss) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/entity/boss/" + boss.skin() + ".png");
    }

    /** The hat layer is the second layer of the head: only Deucalion's glasses are on it, and they go when he shifts ({@link BossNpc#hatVisible}). */
    @Override
    public void render(T boss, float yaw, float partialTick, PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        getModel().hat.visible = boss.hatVisible();
        super.render(boss, yaw, partialTick, poses, buffers, light);
    }

    @Override
    protected void setupRotations(T boss, PoseStack poses, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(boss, poses, bob, yBodyRot, partialTick, scale);
        if (boss instanceof NpcOriginalBoss original && original.isDesiccated()) {
            poses.translate(0.0F, 0.1F, 0.0F);
            poses.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }
}
