package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.GevaudanSpiritEntity;

/** The Spirit of Gevaudan has no body: it is the smoke and the souls the server sends, so this draws nothing. Client only. */
public final class SpiritRenderer extends EntityRenderer<GevaudanSpiritEntity> {
    private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/entity/boss/beast_of_gevaudan_geo.png");

    public SpiritRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(GevaudanSpiritEntity spirit) {
        return NONE;
    }
}
