package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Finds out who infects someone with sanguinare. Vampirism does not say: {@code IBiteableEntity.tryInfect(IVampire)} knows
 * the vampire but adds the effect without a source, and a bite alone infects nobody. Only the Infect action reaches
 * players and creatures, so: the action's activation event (it runs just before the infection, in the same call) notes
 * the vampire for the current tick, and the sanguinare effect that is added to a victim in that tick, within reach of
 * that vampire, is theirs. The infector is kept in {@link SpeciesRegistries#INFECTION_SIRE} for a day: a player who
 * joins the vampires in that time is sired by them ({@link SireRecorder}), a creature that is converted likewise.
 */
public final class InfectionTracker {
    private static final ResourceLocation INFECT = ResourceLocation.fromNamespaceAndPath("vampirism", "infect");
    private static final ResourceLocation SANGUINARE = ResourceLocation.fromNamespaceAndPath("vampirism", "sanguinare");
    static final double REACH_SQR = 8 * 8;

    private record Note(ServerPlayer player, long tick) {
    }

    // vampires who used Infect this tick; server thread only
    private static final Map<UUID, Note> NOTES = new HashMap<>();

    private InfectionTracker() {
    }

    public static void register() {
        // after everyone who may cancel the action
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ActionEvent.ActionActivatedEvent.class, InfectionTracker::onAction);
        NeoForge.EVENT_BUS.addListener(InfectionTracker::onAdded);
        NeoForge.EVENT_BUS.addListener(InfectionTracker::onRemoved);
        NeoForge.EVENT_BUS.addListener(InfectionTracker::onJoined);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> NOTES.clear());
    }

    /** The infection still counting for this creature, or null. */
    @Nullable
    static InfectionSire pending(LivingEntity e) {
        if (!e.hasData(SpeciesRegistries.INFECTION_SIRE)) {
            return null;
        }
        InfectionSire inf = e.getData(SpeciesRegistries.INFECTION_SIRE);
        return inf.expired(e.level().getGameTime()) ? null : inf;
    }

    private static void onAction(ActionEvent.ActionActivatedEvent e) {
        if (e.getFactionPlayer().asEntity() instanceof ServerPlayer p
                && INFECT.equals(VampirismRegistries.ACTION.get().getKey(e.getAction()))) {
            NOTES.put(p.getUUID(), new Note(p, p.level().getGameTime()));
        }
    }

    private static void onAdded(MobEffectEvent.Added e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || !isSanguinare(e.getEffectInstance().getEffect())) {
            return;
        }
        long now = victim.level().getGameTime();
        if (pending(victim) != null) {
            return; // the first infector wins
        }
        victim.removeData(SpeciesRegistries.INFECTION_SIRE); // a stale one
        ServerPlayer infector = null;
        double best = REACH_SQR;
        for (Note n : NOTES.values()) {
            double d = n.player().distanceToSqr(victim);
            if (n.tick() == now && n.player() != victim && n.player().level() == victim.level() && d <= best) {
                infector = n.player();
                best = d;
            }
        }
        if (infector != null) {
            victim.setData(SpeciesRegistries.INFECTION_SIRE, new InfectionSire(infector.getUUID(), SireRecorder.nameOf(infector), now + InfectionSire.WINDOW));
        }
    }

    /** Cured or cleared: the infection no longer counts. */
    private static void onRemoved(MobEffectEvent.Remove e) {
        if (!e.getEntity().level().isClientSide && isSanguinare(e.getEffect())) {
            e.getEntity().removeData(SpeciesRegistries.INFECTION_SIRE);
        }
    }

    /** Sanguinare ends by joining the vampire faction; so does a rite, which a pending infection also sires. */
    private static void onJoined(PlayerFactionEvent.FactionLevelChanged event) {
        if (event.getCurrentFaction() != VReference.VAMPIRE_FACTION || event.getOldFaction() == VReference.VAMPIRE_FACTION
                || !(event.getPlayer().getPlayer() instanceof ServerPlayer p) || p.level().isClientSide) {
            return;
        }
        InfectionSire inf = pending(p);
        p.removeData(SpeciesRegistries.INFECTION_SIRE);
        if (inf != null) {
            SireRecorder.record(inf.sire(), SireRecorder.knownName(p.server, inf), p);
        }
    }

    private static boolean isSanguinare(Holder<MobEffect> effect) {
        return effect.is(SANGUINARE);
    }
}
