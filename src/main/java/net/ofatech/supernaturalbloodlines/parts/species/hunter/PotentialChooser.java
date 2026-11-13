package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * (by the UUID of the hunter who lost the mark, with a count) and retried when a player logs in or someone joins the hunters.
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

    /** A marked hunter lost the mark: one potential is owed. It is chosen now if anyone is eligible, else kept until someone is. */
    public static void owe(MinecraftServer server, UUID lostMarkOf) {
        FiveRoster.get(server.overworld()).addPendingPotentialChoice(lostMarkOf);
        retryPending(server);
    }

    /** Picks with the overworld's random. */
    public static boolean choose(MinecraftServer server, UUID lostMarkOf) {
        return choose(server, lostMarkOf, server.overworld().getRandom());
    }

    /**
     * Makes one eligible online hunter a potential. Does not touch the pending list.
     *
     * @param lostMarkOf the hunter who lost the mark: not eligible for this choice
     * @return false when nobody could be chosen
     */
    public static boolean choose(MinecraftServer server, UUID lostMarkOf, RandomSource random) {
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
                return true;
            }
        }
        return false;
    }

    /** Settles every owed potential that someone online can take now. */
    public static void retryPending(MinecraftServer server) {
        FiveRoster roster = FiveRoster.get(server.overworld());
        for (Map.Entry<UUID, Integer> owed : Map.copyOf(roster.pendingPotentialChoice()).entrySet()) {
            for (int i = 0; i < owed.getValue() && choose(server, owed.getKey()); i++) {
                roster.resolvePendingPotentialChoice(owed.getKey());
            }
        }
    }

    private static boolean eligible(ServerPlayer p) {
        return SpeciesEligibility.isHumanLike(p) && BloodlineAccess.get(p).species() == Species.NONE
                && VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == VReference.HUNTER_FACTION;
    }
}
