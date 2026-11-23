package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.ofatech.supernaturalbloodlines.core.boss.ManPoseTable;
import net.ofatech.supernaturalbloodlines.core.boss.MorphRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ShiftBody;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.LyingBody;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BossNpc;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;

/** A boss that is a person: the player model with the finished skin in {@code textures/entity/boss/<id>.png}, armor and the held item. A desiccated one lies flat. */
public final class HumanoidBossRenderer<T extends BossNpc> extends HumanoidMobRenderer<T, MorphPlayerModel<T>> {
    /** The man's pose for this frame, sampled once in {@link #render} and read by the model and by {@link #scale}: nothing is allocated per frame. */
    private final float[] pose = new float[ManPoseTable.SIZE];
    private boolean morphing;

    public HumanoidBossRenderer(EntityRendererProvider.Context context, boolean slim) {
        super(context, new MorphPlayerModel<>(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5F);
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
        // the man's half of a shift: a pose from the table, a function of the synced form and ticks (and the partial tick), so a late joiner sees the right one
        morphing = boss instanceof ShiftBody shifter && MorphRules.manHalf(shifter.form(), shifter.shiftTicks());
        if (morphing) {
            ShiftBody shifter = (ShiftBody) boss;
            ManPose.table(boss.boss()).sample(MorphRules.manProgress(shifter.shiftTicks(), partialTick), pose);
        }
        getModel().pose = morphing ? pose : null;
        try {
            super.render(boss, yaw, partialTick, poses, buffers, light);
        } finally {
            getModel().pose = null;
            morphing = false;
        }
    }

    /** The squat and the size of the pose (the same sample): the body goes down by {@code offset_y} blocks and is scaled about its feet. */
    @Override
    protected void scale(T boss, PoseStack poses, float partialTick) {
        super.scale(boss, poses, partialTick);
        if (morphing) {
            // the stack is upside down here (vanilla scales by -1, -1, 1 first), so a squat is a translation the other way
            poses.translate(0.0F, -pose[ManPoseTable.SIZE - 2], 0.0F);
            float s = pose[ManPoseTable.SCALE];
            poses.scale(s, s, s);
        }
    }

    @Override
    protected void setupRotations(T boss, PoseStack poses, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(boss, poses, bob, yBodyRot, partialTick, scale);
        if (boss instanceof NpcOriginalBoss original && original.isDesiccated()) {
            // on its back, head toward -x, as vanilla draws a sleeper, but with the middle of the body over the entity and the back on the floor (R184)
            poses.translate(LyingBody.OFFSET_X, LyingBody.OFFSET_Y, 0.0F);
            poses.mulPose(LyingBody.rotation());
        }
    }
}
