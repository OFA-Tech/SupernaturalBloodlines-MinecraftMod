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
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;

/**
 * The Rite of Revelation (R10b): the filled taglock of a player makes every gene that player was rolled for known to them, those they
 * carry and those they do not, and tells them which is which. A gene that was never rolled (the werewolf gene without Werewolves) is
 * neither revealed nor reported. The owner is the subject: the message goes to them only, and nothing is sent to
 * the caster beyond the rite working. A villager's taglock, or an owner who is not loaded or online, is refused and the ingredients
 * come back.
 */
public record RevelationRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "revelation");
    public static final String PLAYERS_ONLY = "supernaturalbloodlines.gene.refusal.revelation_players_only";
    public static final String HEADER = "supernaturalbloodlines.gene.revelation";
    public static final MapCodec<RevelationRiteFactory> CODEC = MapCodec.unit(RevelationRiteFactory::new);

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new RevelationRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    /** The message each player is told: one line per gene, "yes" or "no". */
    static Component report(BloodlineState s) {
        var out = Component.translatable(HEADER);
        for (String gene : Genes.rolled(s)) {
            out.append("\n").append(Component.translatable("supernaturalbloodlines.gene.revelation." + gene
                    + (Genes.has(s, gene) ? ".yes" : ".no")));
        }
        return out;
    }

    private static final class RevelationRite extends Rite {
        RevelationRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            UUID id = TaglockOwner.uuid(params.consumedItems);
            LivingEntity owner = id == null ? null : TaglockOwner.find(level, id);
            String refusal = id == null ? DoppelgangerRiteRules.NO_OWNER : owner == null ? DoppelgangerRiteRules.OWNER_AWAY
                    : !(owner instanceof ServerPlayer) ? PLAYERS_ONLY : null;
            if (refusal != null) {
                if (findEntity(params.caster) instanceof ServerPlayer caster) {
                    caster.displayClientMessage(Component.translatable(refusal), true);
                }
                return cancel();
            }
            ServerPlayer player = (ServerPlayer) owner;
            BloodlineAccess.reveal(player, Genes.rolled(BloodlineAccess.get(player)).toArray(String[]::new));
            player.sendSystemMessage(report(BloodlineAccess.get(player)));
            return false;
        }
    }
}
