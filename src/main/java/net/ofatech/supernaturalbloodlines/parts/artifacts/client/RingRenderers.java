package net.ofatech.supernaturalbloodlines.parts.artifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.artifacts.RingPose;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Draws the three rings on the arm (Curios). Client only; load it only when Curios is loaded. */
public final class RingRenderers implements ICurioRenderer {
    private final String ring;

    private RingRenderers(String ring) {
        this.ring = ring;
    }

    private static ModelResourceLocation model(String ring, boolean slim, boolean left) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, RingPose.modelFile(ring, slim, left)));
    }

    /** Mod bus: the four arm variants of each ring are not used by any block or item model, so they must be asked for. */
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (String ring : RingPose.RINGS) {
            for (int variant = 0; variant < 4; variant++) {
                event.register(model(ring, (variant & 1) != 0, (variant & 2) != 0));
            }
        }
    }

    public static void register() {
        CuriosRendererRegistry.register(ArtifactItems.SUN_RING.get(), () -> new RingRenderers("sun_ring"));
        if (ArtifactItems.MOON_RING != null) {
            CuriosRendererRegistry.register(ArtifactItems.MOON_RING.get(), () -> new RingRenderers("moon_ring"));
        }
        CuriosRendererRegistry.register(ArtifactItems.RESURRECTION_RING.get(), () -> new RingRenderers("resurrection_ring"));
    }

    @Override
    public <T extends LivingEntity, M extends net.minecraft.client.model.EntityModel<T>> void render(ItemStack stack, SlotContext slot,
            PoseStack poses, RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
            float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity wearer = slot.entity();
        if (!(parent.getModel() instanceof PlayerModel<?> player) || wearer.isInvisible() || wearer.isSpectator()) {
            return;
        }
        boolean left = RingPose.left(slot.index());
        ModelPart arm = left ? player.leftArm : player.rightArm;
        if (!arm.visible) {
            return;
        }
        boolean slim = wearer instanceof AbstractClientPlayer p && p.getSkin().model() == PlayerSkin.Model.SLIM;
        BakedModel baked = Minecraft.getInstance().getModelManager().getModel(model(ring, slim, left));
        poses.pushPose();
        arm.translateAndRotate(poses);
        poses.translate(RingPose.offsetX(slim, left) / 16f, RingPose.Y / 16f, 0f);
        poses.mulPose(Axis.XP.rotationDegrees(180f));
        poses.translate(-0.5f, -0.5f, -0.5f);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poses.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()),
                null, baked, 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
        poses.popPose();
    }
}
