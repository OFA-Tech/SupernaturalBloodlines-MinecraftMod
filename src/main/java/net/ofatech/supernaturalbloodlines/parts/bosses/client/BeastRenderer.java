package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BeastEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** The Beast of Gevaudan (GeckoLib): {@code geo/entity/beast_of_gevaudan.geo.json} with its ten animations. The collapse and the death are the model's; the entity adds no rotation. Client only. */
public final class BeastRenderer extends GeoEntityRenderer<BeastEntity> {
    public BeastRenderer(EntityRendererProvider.Context context) {
        super(context, new BossGeoModel<>("beast_of_gevaudan", "beast_of_gevaudan_geo"));
        this.shadowRadius = 1.4F;
    }

    @Override
    protected float getDeathMaxRotation(BeastEntity beast) {
        return 0.0F;
    }
}
