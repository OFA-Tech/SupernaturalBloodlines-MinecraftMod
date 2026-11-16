package net.ofatech.supernaturalbloodlines.parts.species.pack.party;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules.Party;
import org.jetbrains.annotations.Nullable;

/** The one party system the packs mirror: Open Parties and Claims (and its Team Claims fork, same API), or nothing. */
public interface PartyBackend {
    /** The party owned by one of {@code candidates} (the leader first), or null. */
    @Nullable
    Party find(MinecraftServer server, List<UUID> candidates);

    /** @return false when the owner is offline or the party could not be made */
    boolean create(MinecraftServer server, UUID owner);

    void disband(MinecraftServer server, UUID owner);

    void add(MinecraftServer server, UUID owner, UUID member, boolean admin);

    void remove(MinecraftServer server, UUID owner, UUID member);
}
