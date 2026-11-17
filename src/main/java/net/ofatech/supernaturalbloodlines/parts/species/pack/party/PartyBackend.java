package net.ofatech.supernaturalbloodlines.parts.species.pack.party;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules.Party;
import org.jetbrains.annotations.Nullable;

/** The one party system the packs mirror: Open Parties and Claims (and its Team Claims fork, same API), or nothing. */
public interface PartyBackend {
    /** @param reachable false when the party system did not answer (not the same as "there is no such party") */
    record Found(boolean reachable, @Nullable Party party) {
    }

    /** The party {@code owner} owns, if it is the one with this id (a manual party of the same owner is not ours). */
    Found find(MinecraftServer server, UUID owner, UUID partyId);

    /** Makes a party for {@code owner}; empty when it was refused (they own one already, they are offline, an error). Never adopts one. */
    Optional<UUID> create(MinecraftServer server, UUID owner);

    /** Adds {@code member} with the plain member rank. False when refused: they are in another party, a limit, an error. */
    boolean add(MinecraftServer server, UUID owner, UUID member);

    /** @return true when {@code member} is out of the party (or was never in it); false when the call failed and must be tried again */
    boolean remove(MinecraftServer server, UUID owner, UUID member);
}
