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
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import org.jetbrains.annotations.Nullable;

/**
 * The Rite of Revelation (R10b): the filled taglock of a player makes every gene that player was rolled for known to them, those they
 * carry and those they do not, and tells them which is which. A gene that was never rolled (the werewolf gene without Werewolves) is
 * neither revealed nor reported. The owner is the subject: the message goes to them only, and nothing is sent to
 * the caster beyond the rite working. It also reveals the Original of their line (discovery is per player, never shared with their sired). A villager's taglock, or an owner who is not loaded or online, is refused and the ingredients
 * come back.
 */
public record RevelationRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "revelation");
    public static final String PLAYERS_ONLY = "supernaturalbloodlines.gene.refusal.revelation_players_only";
    public static final String HEADER = "supernaturalbloodlines.gene.revelation";
    public static final String LINE = "supernaturalbloodlines.gene.revelation.line";
    public static final String NO_LINE = "supernaturalbloodlines.gene.revelation.no_line";
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

    /**
     * The message each player is told: one line per gene, "yes" or "no", then the Original of their line (R123) or that they have none.
     *
     * @param original the name of the root Original, or null when the player belongs to no line
     */
    public static Component report(BloodlineState s, @Nullable Component original) {
        var out = Component.translatable(HEADER);
        for (String gene : Genes.rolled(s)) {
            out.append("\n").append(Component.translatable("supernaturalbloodlines.gene.revelation." + gene
                    + (Genes.has(s, gene) ? ".yes" : ".no")));
        }
        out.append("\n").append(original != null ? Component.translatable(LINE, original) : Component.translatable(NO_LINE));
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
            Lines.reveal(player);
            player.sendSystemMessage(report(BloodlineAccess.get(player),
                    Lines.rootOf(player.server, player.getUUID()).map(root -> Lines.rootName(player.server, root)).orElse(null)));
            return false;
        }
    }
}
