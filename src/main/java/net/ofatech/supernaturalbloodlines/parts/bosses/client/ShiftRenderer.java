package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BossNpc;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ShiftBody;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * One renderer for a boss that shifts (plan 17, decision 4): the player-model renderer for the man and the GeckoLib renderer for the wolf, picked every frame by
 * the synced form ({@link ShiftRules#showsBeast}). The entity is one, so the UUID, the bar and the index never notice. Client only.
 */
public final class ShiftRenderer<T extends BossNpc & GeoEntity & ShiftBody> extends EntityRenderer<T> {
    private final HumanoidBossRenderer<T> human;
    private final GeoEntityRenderer<T> beast;

    /** @param model the geo and animation id of the wolf, {@code texture} the name of its texture (the glow mask is {@code <texture>_glowmask}). */
    public ShiftRenderer(EntityRendererProvider.Context context, String model, String texture, float wolfShadow) {
        super(context);
        this.human = new HumanoidBossRenderer<>(context, false);
        this.beast = new BeastRenderer<>(context, model, texture, wolfShadow);
        this.shadowRadius = 0.5F;
    }

    private boolean beast(T boss) {
        return ShiftRules.showsBeast(boss.form(), boss.shiftTicks());
    }

    @Override
    public void render(T boss, float yaw, float partialTick, PoseStack poses, MultiBufferSource buffers, int light) {
        if (beast(boss)) {
            beast.render(boss, yaw, partialTick, poses, buffers, light);
        } else {
            human.render(boss, yaw, partialTick, poses, buffers, light);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T boss) {
        return beast(boss) ? beast.getTextureLocation(boss) : human.getTextureLocation(boss);
    }

    @Override
    public boolean shouldRender(T boss, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return beast(boss) ? beast.shouldRender(boss, frustum, x, y, z) : human.shouldRender(boss, frustum, x, y, z);
    }

    @Override
    public Vec3 getRenderOffset(T boss, float partialTick) {
        return beast(boss) ? beast.getRenderOffset(boss, partialTick) : human.getRenderOffset(boss, partialTick);
    }

    /** The wolf: the glow mask makes the eyes shine, and the death animation is the only fall (the root bone moves, so the entity adds no rotation). */
    private static final class BeastRenderer<T extends BossNpc & GeoEntity & ShiftBody> extends GeoEntityRenderer<T> {
        BeastRenderer(EntityRendererProvider.Context context, String model, String texture, float shadow) {
            super(context, new BossGeoModel<>(model, texture));
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            this.shadowRadius = shadow;
        }

        @Override
        protected float getDeathMaxRotation(T boss) {
            return 0.0F;
        }
    }
}
