package net.ofatech.supernaturalbloodlines.parts.species.doppelganger;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;

/**
 * The hidden doppelgänger gene (R14): rolled once for every player who has not been rolled yet, at login, so a new player and
 * a player from before the gene existed are the same case. It is independent of the werewolf gene and does nothing by itself.
 * Needs no Werewolves.
 */
public final class DoppelgangerGene {
    private static final DoubleSupplier RANDOM = () -> ThreadLocalRandom.current().nextDouble();
    private static volatile DoubleSupplier roll = RANDOM;

    private DoppelgangerGene() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                onLogin(p);
            }
        });
    }

    /** For tests: makes the roll deterministic (a value in [0, 1)). */
    public static void useRoll(DoubleSupplier forced) {
        roll = forced;
    }

    public static void resetRoll() {
        roll = RANDOM;
    }

    /** Rolls the gene unless this player was rolled already (or the admin command set it). */
    public static void onLogin(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        if (s.doppelgangerRolled()) {
            return;
        }
        boolean gene = roll.getAsDouble() < SpeciesConfig.doppelgangerGeneChance();
        BloodlineAccess.set(p, Genes.migrateDiscovered(s.toBuilder().doppelgangerRolled(true).doppelgangerGene(gene).build()));
    }

    /** The admin command: sets the gene and counts as rolled, so the next login does not roll again. */
    public static void set(ServerPlayer p, boolean gene) {
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().doppelgangerRolled(true).doppelgangerGene(gene).build());
    }
}
