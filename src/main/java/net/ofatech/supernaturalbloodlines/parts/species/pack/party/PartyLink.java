package net.ofatech.supernaturalbloodlines.parts.species.pack.party;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules.Plan;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackMembers;

/**
 * A pack is a party in Open Parties and Claims (plan 14), but only a party the pack made itself: the glue records the id when
 * {@code create} works and never looks at any other party. The leader owns it and everyone else is a plain member; a player who
 * left the party leaves the pack, and a player who left the pack leaves the party. A player who is already in another party is
 * left out of ours and stays in the pack. Does nothing when the mod (or its Team Claims fork, same id) is not there or
 * {@code opac_link_enabled} is off.
 */
public final class PartyLink {
    public static final String MOD_ID = "openpartiesandclaims";
    private static final int PERIOD = 200;
    private static volatile PartyBackend backend;

    private PartyLink() {
    }

    public static void register() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        backend = OpacBackend.tryCreate();
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (backend != null && e.getServer().getTickCount() % PERIOD == 0) {
                syncAll(e.getServer());
            }
        });
    }

    /** For tests: a backend in place of Open Parties and Claims, null for none. */
    public static void setBackend(PartyBackend b) {
        backend = b;
    }

    public static boolean active() {
        return backend != null && SpeciesConfig.opacLinkEnabled();
    }

    /** The leadership of a pack moved or a pack ended: parties of owners who lead nothing any more are cleaned up, and the new leader gets theirs. */
    public static void leadershipChanged(MinecraftServer server, @Nullable UUID newLeader) {
        cleanup(server);
        if (newLeader != null) {
            sync(server, newLeader, true);
        }
    }

    /**
     * A party the pack made for an owner who is in no pack any more (the pack dissolved, the owner was kicked or lost the lead
     * and left): our mirrored members leave it, the record is cleared, and the owner's party itself is left as it is.
     */
    public static void cleanup(MinecraftServer server) {
        PartyBackend b = backend;
        if (b == null || !SpeciesConfig.opacLinkEnabled()) {
            return;
        }
        PackIndex index = PackIndex.get(server.overworld());
        for (UUID owner : index.partyOwners()) {
            if (index.inPack(owner)) {
                continue; // the pack's own pass sees to a party whose owner is still in a pack
            }
            Optional<PackIndex.Recorded> recorded = index.recordedParty(List.of(owner));
            PartyBackend.Found found = recorded.map(r -> b.find(server, r.owner(), r.id())).orElse(new PartyBackend.Found(true, null));
            if (!found.reachable()) {
                continue;
            }
            boolean allOut = true;
            if (found.party() != null) {
                for (UUID m : index.mirrorsOf(List.of(owner))) {
                    allOut &= b.remove(server, owner, m);
                }
            }
            if (allOut) {
                index.forgetParty(owner); // a failed removal keeps the record, so the next pass tries again
            }
        }
    }

    public static void syncAll(MinecraftServer server) {
        cleanup(server);
        for (UUID leader : new ArrayList<>(PackIndex.get(server.overworld()).leaders())) {
            sync(server, leader, false);
        }
    }

    /** One pass for the pack {@code member} is in. {@code rosterChanged}: somebody joined, left or was kicked just now. */
    public static void sync(MinecraftServer server, UUID member, boolean rosterChanged) {
        PartyBackend b = backend;
        if (b == null || !SpeciesConfig.opacLinkEnabled()) {
            return;
        }
        PackIndex index = PackIndex.get(server.overworld());
        UUID leader = index.leaderOf(member);
        List<UUID> pack = index.pack(leader);
        Optional<PackIndex.Recorded> recorded = index.recordedParty(pack);
        PartySyncRules.Party party = null;
        if (recorded.isPresent()) {
            PartyBackend.Found found = b.find(server, recorded.get().owner(), recorded.get().id());
            if (!found.reachable()) {
                return; // not answered: nothing is concluded from it
            }
            party = found.party();
        }
        UUID owner = recorded.map(PackIndex.Recorded::owner).orElse(leader);
        Set<UUID> ours = recorded.isPresent() ? index.mirrorsOf(List.of(owner)) : new HashSet<>();
        Plan plan = PartySyncRules.plan(leader, new HashSet<>(pack), party, recorded.isPresent(), ours, index.partyEnded(pack), rosterChanged);

        if (plan.forget()) {
            index.forgetParty(owner);
            index.setPartyEnded(owner, true);
            return;
        }
        if (plan.abandon()) {
            boolean allOut = true;
            for (UUID id : plan.removeFrom()) {
                allOut &= b.remove(server, owner, id);
            }
            if (!allOut) {
                return; // the old party still holds our members: nothing is forgotten, and the next pass tries again
            }
            index.forgetParty(owner);
        }
        if (plan.create()) {
            Optional<UUID> id = b.create(server, leader);
            if (id.isEmpty()) {
                return; // refused (they own a party of their own, or are away): tried again at the next pass
            }
            index.recordParty(leader, id.get());
            pack.forEach(m -> index.setPartyEnded(m, false));
            owner = leader;
        }
        for (UUID m : plan.add()) {
            if (b.add(server, owner, m)) {
                index.setMirror(m, owner);
                index.clearPartyRefused(m);
            } else if (index.markPartyRefused(m)) {
                // told once, not at every retry: they stay in the pack, and are tried again later
                PackMembers.tell(server, m, "supernaturalbloodlines.pack.party_refused", "");
            }
        }
        for (UUID m : plan.removeFrom()) {
            if (plan.abandon() || b.remove(server, owner, m)) {
                index.setMirror(m, null);
            }
        }
        for (UUID m : plan.leavePack()) {
            index.setMirror(m, null);
            PackMembers.remove(server, m, "supernaturalbloodlines.pack.left_party", "");
        }
    }
}