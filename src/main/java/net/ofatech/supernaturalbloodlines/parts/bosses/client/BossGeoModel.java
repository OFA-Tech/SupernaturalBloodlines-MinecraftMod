package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/** The geo, animation and texture files of a creature form: {@code geo/entity/<id>.geo.json}, {@code animations/entity/<id>.animation.json}, {@code textures/entity/boss/<texture>.png}. Client only. */
public final class BossGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    private final ResourceLocation model;
    private final ResourceLocation animation;
    private final ResourceLocation texture;

    public BossGeoModel(String id, String texture) {
        this.model = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "geo/entity/" + id + ".geo.json");
        this.animation = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "animations/entity/" + id + ".animation.json");
        this.texture = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/entity/boss/" + texture + ".png");
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return animation;
    }
}
