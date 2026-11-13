package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesEligibility;

/**
 * R82: when a marked hunter loses the mark, a random online eligible hunter (human-like, in the Vampirism hunter faction,
 * no species yet) becomes a potential. With nobody eligible the choice is kept in {@link FiveRoster#pendingPotentialChoice()}
 * (by the UUID of the hunter who lost the mark) and retried when a player logs in or someone joins the hunters.
 */
public final class PotentialChooser {
    private PotentialChooser() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                retryPending(p.server);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> {
            if (e.getCurrentFaction() == VReference.HUNTER_FACTION && e.getPlayer().getPlayer() instanceof ServerPlayer p && !p.level().isClientSide) {
                retryPending(p.server);
            }
        });
    }

    /** Picks with the overworld's random. */
    public static boolean choose(MinecraftServer server, UUID lostMarkOf) {
        return choose(server, lostMarkOf, server.overworld().getRandom());
    }

    /**
     * @param lostMarkOf the hunter who lost the mark: not eligible for this choice, and the key of the pending entry
     * @return false when nobody could be chosen (the choice is then pending)
     */
    public static boolean choose(MinecraftServer server, UUID lostMarkOf, RandomSource random) {
        FiveRoster roster = FiveRoster.get(server.overworld());
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer p : HunterEvents.online(server)) {
            if (eligible(p) && !p.getUUID().equals(lostMarkOf)) {
                candidates.add(p);
            }
        }
        while (!candidates.isEmpty()) {
            ServerPlayer pick = candidates.remove(random.nextInt(candidates.size()));
            BloodlineAccess.applyEvent(pick, BloodlineEvent.CHOSEN_AS_POTENTIAL, SpeciesContext.of(pick, null));
            if (BloodlineAccess.get(pick).species() == Species.POTENTIAL) {
                roster.setPendingPotentialChoice(lostMarkOf, false);
                return true;
            }
        }
        roster.setPendingPotentialChoice(lostMarkOf, true);
        return false;
    }

    public static void retryPending(MinecraftServer server) {
        FiveRoster roster = FiveRoster.get(server.overworld());
        for (UUID lostMarkOf : List.copyOf(roster.pendingPotentialChoice())) {
            choose(server, lostMarkOf);
        }
    }

    private static boolean eligible(ServerPlayer p) {
        return SpeciesEligibility.isHumanLike(p) && BloodlineAccess.get(p).species() == Species.NONE
                && VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == VReference.HUNTER_FACTION;
    }
}
