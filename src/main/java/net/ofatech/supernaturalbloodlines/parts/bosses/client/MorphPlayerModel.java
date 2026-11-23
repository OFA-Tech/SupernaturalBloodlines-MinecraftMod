package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.core.boss.ManPoseTable;

/**
 * The player model of a boss, with the man's half of a shift put on top of what the game already poses (UI overhaul phase 7). The renderer sets {@link #pose}
 * (the 12 channels of {@link ManPoseTable}, sampled once for the frame) while the man is drawn and clears it after; with none the model is the vanilla one. The
 * head, body, arms and legs are rotated by the channels (degrees) and the second skin layer is copied from them so that it follows. Client only.
 */
public final class MorphPlayerModel<T extends LivingEntity> extends PlayerModel<T> {
    /** The channels of {@link ManPoseTable#CHANNELS} for this frame, or null for no morph. Owned by the renderer; never copied. */
    public float[] pose;

    public MorphPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float[] p = pose;
        if (p == null) {
            return;
        }
        head.xRot += rad(p[0]);
        head.yRot += rad(p[1]);
        head.zRot += rad(p[2]);
        body.xRot += rad(p[3]);
        rightArm.xRot += rad(p[4]);
        rightArm.zRot += rad(p[5]);
        leftArm.xRot += rad(p[6]);
        leftArm.zRot += rad(p[7]);
        rightLeg.xRot += rad(p[8]);
        leftLeg.xRot += rad(p[9]);
        // the second skin layer is copied from the parts at the end of the vanilla setup, so it has to be copied again
        hat.copyFrom(head);
        jacket.copyFrom(body);
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
        rightPants.copyFrom(rightLeg);
        leftPants.copyFrom(leftLeg);
    }

    private static float rad(float degrees) {
        return degrees * ((float) Math.PI / 180.0F);
    }
}
