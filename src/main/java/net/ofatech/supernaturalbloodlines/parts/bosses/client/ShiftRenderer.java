package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KlausEntity;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * One renderer for a boss that shifts (plan 17, decision 4): the player-model renderer for the man and the GeckoLib renderer for the wolf, picked every frame by
 * the synced form ({@link ShiftRules#showsBeast}). The entity is one, so the UUID, the bar and the index never notice. Client only.
 */
public final class ShiftRenderer extends EntityRenderer<KlausEntity> {
    private final HumanoidBossRenderer<KlausEntity> human;
    private final GeoEntityRenderer<KlausEntity> beast;

    public ShiftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.human = new HumanoidBossRenderer<>(context, false);
        this.beast = new BeastRenderer(context);
        this.shadowRadius = 0.5F;
    }

    private boolean beast(KlausEntity k) {
        return ShiftRules.showsBeast(k.form(), k.shiftTicks());
    }

    @Override
    public void render(KlausEntity k, float yaw, float partialTick, PoseStack poses, MultiBufferSource buffers, int light) {
        if (beast(k)) {
            beast.render(k, yaw, partialTick, poses, buffers, light);
        } else {
            human.render(k, yaw, partialTick, poses, buffers, light);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(KlausEntity k) {
        return beast(k) ? beast.getTextureLocation(k) : human.getTextureLocation(k);
    }

    @Override
    public boolean shouldRender(KlausEntity k, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return beast(k) ? beast.shouldRender(k, frustum, x, y, z) : human.shouldRender(k, frustum, x, y, z);
    }

    @Override
    public Vec3 getRenderOffset(KlausEntity k, float partialTick) {
        return beast(k) ? beast.getRenderOffset(k, partialTick) : human.getRenderOffset(k, partialTick);
    }

    /** The wolf: the glow mask makes the golden irises shine, and the death animation is the only fall (the root bone moves, so the entity adds no rotation). */
    private static final class BeastRenderer extends GeoEntityRenderer<KlausEntity> {
        BeastRenderer(EntityRendererProvider.Context context) {
            super(context, new BossGeoModel<>("klaus_hybrid", "klaus_hybrid_geo"));
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            this.shadowRadius = 0.6F;
        }

        @Override
        protected float getDeathMaxRotation(KlausEntity k) {
            return 0.0F;
        }
    }
}
