package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackOutcome;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.GeneAwakening;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.WerewolfFaction;
import org.jetbrains.annotations.Nullable;

/**
 * The hidden werewolf gene (R10, R11): rolled once at first login, awakened for a human by a villager or player kill.
 * A vampire's awakening (R24) stays with {@link GeneAwakening}; a hunter's gene stays latent. Touched only behind
 * {@code ModList.isLoaded("werewolves")}.
 */
public final class GeneHandler {
    private static final DoubleSupplier RANDOM = () -> ThreadLocalRandom.current().nextDouble();
    private static volatile DoubleSupplier roll = RANDOM;

    private GeneHandler() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                onLogin(p);
            }
        });
        // LOW and not receiving cancelled events, like GeneAwakening: a prevented death is no kill
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, LivingDeathEvent.class, GeneHandler::onDeath);
    }

    /** For tests: makes the gene roll deterministic (a value in [0, 1)). */
    public static void useRoll(DoubleSupplier forced) {
        roll = forced;
    }

    public static void resetRoll() {
        roll = RANDOM;
    }

    /** First login only: rolls {@code latentGene} with {@code latent_gene_chance} and remembers that it rolled. */
    public static void onLogin(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        if (s.wolf().geneRolled()) {
            return;
        }
        boolean gene = roll.getAsDouble() < SpeciesConfig.latentGeneChance();
        BloodlineAccess.set(p, s.toBuilder().wolf(s.wolf().toBuilder().geneRolled(true).latentGene(gene).build()).build());
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer killer) || !GeneAwakening.isGeneVictim(killer, e.getEntity())
                || !PackAccess.wolf(killer).latentGene()
                || FactionRiteFactory.baseFaction(VampirismAPI.factionPlayerHandler(killer)) != BaseFaction.NONE) {
            return;
        }
        awakenHuman(killer, PackEvent.GENE_AWAKENED);
    }

    /**
     * A player with no faction (or a werewolf) takes the lineage {@code event} gives and joins the werewolves at level 1.
     * Nothing changes if the pack rules refuse or the faction join is denied. Returns the refusal key, null on success.
     */
    @Nullable
    public static String awakenHuman(ServerPlayer p, PackEvent event) {
        BloodlineState before = BloodlineAccess.get(p);
        IFactionPlayerHandler h = VampirismAPI.factionPlayerHandler(p);
        if (!FactionRiteFactory.joinAllowed(h, WerewolfFaction.faction(), false)) {
            return FactionRiteFactory.JOIN_DENIED;
        }
        PackOutcome out = PackAccess.applyPack(p, event, PackContext.defaults());
        if (out.refused()) {
            return out.refusalKey();
        }
        if (!WerewolfFaction.join(p)) {
            // a faction mod vetoed the join late: do not leave a lineage without the faction
            SupernaturalBloodlines.LOGGER.warn("GeneHandler: {} could not join the werewolves, undoing {}", p.getGameProfile().getName(), event);
            BloodlineAccess.set(p, before);
            return FactionRiteFactory.JOIN_DENIED;
        }
        return null;
    }
}
