package net.ofatech.supernaturalbloodlines.parts.species.pack;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteHooks;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.WerewolfFaction;
import org.jetbrains.annotations.Nullable;

/**
 * The Rite of Lineage (R10, R12), one factory for both rites. {@code reveal} tells the player on the circle, and only
 * them, whether they carry the gene. {@code awaken} gives the gene and awakens it: a human becomes a born werewolf
 * (pack rules), a vampire or Original a bound hybrid (bloodline rules); hunters and werewolves are refused (R12 lists no faction, vampire and Original only). A refusal cancels the rite,
 * so Enchanted gives the ingredients back.
 */
public record LineageRiteFactory(Mode mode) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "lineage");
    public static final String GENE_YES = "supernaturalbloodlines.lineage.gene_yes";
    public static final String GENE_NO = "supernaturalbloodlines.lineage.gene_no";
    public static final String HUNTER_REFUSED = "supernaturalbloodlines.refusal.hunter_lineage";
    public static final String WEREWOLF_REFUSED = "supernaturalbloodlines.refusal.werewolf_lineage";

    public enum Mode { REVEAL, AWAKEN }

    public static final MapCodec<LineageRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FactionRiteFactory.enumCodec(Mode.class).fieldOf("mode").forGetter(LineageRiteFactory::mode)
    ).apply(i, LineageRiteFactory::new));

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new LineageRite(base, params, mode);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    /** Awakens {@code target} by the rules of their faction. Returns the refusal key, null when it worked. */
    @Nullable
    public static String awaken(ServerPlayer target, long gameTime) {
        BaseFaction faction = FactionRiteFactory.baseFaction(VampirismAPI.factionPlayerHandler(target));
        BloodlineState before = BloodlineAccess.get(target);
        switch (faction) {
            case HUNTER:
                return HUNTER_REFUSED;
            case WEREWOLF:
                return WEREWOLF_REFUSED;
            case VAMPIRE: {
                BloodlineContext ctx = FactionRiteHooks.context(target, faction, gameTime);
                Outcome preview = BloodlineRules.apply(before, BloodlineEvent.RITE_OF_LINEAGE_AWAKEN, ctx);
                if (preview.refused()) {
                    return preview.refusalKey();
                }
                if (preview.actions().isEmpty() && preview.state().equals(before)) {
                    return FactionRiteFactory.NO_EFFECT;
                }
                if (!FactionRiteFactory.joinAllowed(VampirismAPI.factionPlayerHandler(target), WerewolfFaction.faction(), true)) {
                    return FactionRiteFactory.JOIN_DENIED;
                }
                FactionRiteHooks.run(target, BloodlineAccess.applyEvent(target, BloodlineEvent.RITE_OF_LINEAGE_AWAKEN, ctx));
                return null;
            }
            default:
                return GeneHandler.awakenHuman(target, PackEvent.RITE_OF_LINEAGE_AWAKEN);
        }
    }

    private static final class LineageRite extends Rite {
        private final Mode mode;

        LineageRite(BaseRiteParams base, RiteParams params, Mode mode) {
            super(base, params);
            this.mode = mode;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!ModList.get().isLoaded("werewolves")) {
                return cancel();
            }
            Optional<ServerPlayer> found = RiteTargets.centrePlayerOrCaster(this, params, level);
            if (found.isEmpty()) {
                return cancel();
            }
            ServerPlayer target = found.get();
            if (mode == Mode.REVEAL) {
                target.sendSystemMessage(Component.translatable(BloodlineAccess.get(target).wolf().latentGene() ? GENE_YES : GENE_NO));
                return false;
            }
            String refusal = awaken(target, level.getGameTime());
            if (refusal != null) {
                ServerPlayer caster = findEntity(params.caster) instanceof ServerPlayer p ? p : target;
                caster.displayClientMessage(Component.translatable(refusal), true);
                return cancel();
            }
            return false;
        }
    }
}
