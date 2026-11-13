package net.ofatech.supernaturalbloodlines.parts.species.hybrid;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.parts.species.OutcomeExecutor;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;

/**
 * R24: a vampire (an Original, or a plain Vampirism vampire) who carries the werewolf gene and kills a villager (not a wandering trader) or a player awakens as a bound hybrid ({@code GENE_AWAKENED_AS_VAMPIRE}). The gene itself is rolled by plan 9; this only
 * reads {@code wolf.latentGene}. Heretics are left alone (the rules refuse them). Touched only behind
 * {@code ModList.isLoaded("werewolves")}.
 * <p>
 * LOW and not receiving cancelled events: a death that was prevented (our downed state, Vampirism's dying state, a
 * totem) is no kill.
 */
public final class GeneAwakening {
    private GeneAwakening() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, LivingDeathEvent.class, GeneAwakening::onDeath);
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer killer) || e.getEntity() == killer
                || !(e.getEntity() instanceof Villager || e.getEntity() instanceof Player)) {
            return;
        }
        if (!BloodlineAccess.get(killer).wolf().latentGene()) {
            return;
        }
        // the rules decide who qualifies; a refusal or a no-op (a human with the gene, a heretic) stays silent and does nothing
        Outcome out = BloodlineAccess.applyEvent(killer, BloodlineEvent.GENE_AWAKENED_AS_VAMPIRE, SpeciesContext.of(killer, null));
        if (!out.refused() && !out.actions().isEmpty()) {
            OutcomeExecutor.run(killer, out);
        }
    }
}
