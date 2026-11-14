package net.ofatech.supernaturalbloodlines.parts.species.rite;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.UUID;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.doppelganger.DoppelgangerVillagers;
import net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler;

/**
 * Drops a bottle of Doppelgänger Blood for the filled taglock on the circle (R14). Which taglocks are accepted is
 * {@code doppelganger_rite_taglock}: a doppelgänger villager or a player with the gene (default), or any human-like villager or
 * player. A refusal tells the caster why and cancels the rite, so Enchanted gives the ingredients back. A player owner has their
 * doppelgänger gene discovered and is told; the owner must be loaded or online, since an offline player's state cannot be read.
 */
public record DoppelgangerBloodRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "doppelganger_blood");
    public static final String REVEALED_YES = "supernaturalbloodlines.gene.revealed.doppelganger.yes";
    public static final String REVEALED_NO = "supernaturalbloodlines.gene.revealed.doppelganger.no";
    public static final MapCodec<DoppelgangerBloodRiteFactory> CODEC = MapCodec.unit(DoppelgangerBloodRiteFactory::new);

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new DoppelgangerBloodRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of(new ItemStack(SpeciesRegistries.DOPPELGANGER_BLOOD.get()));
    }

    /** Whether {@code owner} is a doppelgänger: a villager that rolled it, or a player carrying the gene. */
    static boolean isDoppelganger(LivingEntity owner) {
        return owner instanceof ServerPlayer p ? BloodlineAccess.get(p).doppelgangerGene() : owner instanceof Villager v && DoppelgangerVillagers.isDoppelganger(v);
    }

    private static final class DoppelgangerBloodRite extends Rite {
        DoppelgangerBloodRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            UUID id = TaglockOwner.uuid(params.consumedItems);
            LivingEntity owner = id == null ? null : TaglockOwner.find(level, id);
            String refusal = id == null ? DoppelgangerRiteRules.NO_OWNER : owner == null ? DoppelgangerRiteRules.OWNER_AWAY
                    : DoppelgangerRiteRules.refusal(SpeciesConfig.doppelgangerRiteAcceptsHumans(), isDoppelganger(owner),
                    TurningHandler.isHumanLikeBlood(owner));
            if (refusal != null) {
                if (findEntity(params.caster) instanceof ServerPlayer caster) {
                    caster.displayClientMessage(Component.translatable(refusal), true);
                }
                return cancel();
            }
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                    new ItemStack(SpeciesRegistries.DOPPELGANGER_BLOOD.get())));
            if (owner instanceof ServerPlayer player) {
                boolean carries = BloodlineAccess.get(player).doppelgangerGene();
                BloodlineAccess.reveal(player, Genes.DOPPELGANGER);
                player.sendSystemMessage(Component.translatable(carries ? REVEALED_YES : REVEALED_NO));
            }
            return false;
        }
    }
}
