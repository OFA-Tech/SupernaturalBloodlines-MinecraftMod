package net.ofatech.supernaturalbloodlines.parts.species.pack.party;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
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

    public static void syncAll(MinecraftServer server) {
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
            for (UUID id : plan.removeFrom()) {
                b.remove(server, owner, id);
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
            }
        }
        for (UUID m : plan.removeFrom()) {
            if (!plan.abandon()) {
                b.remove(server, owner, m);
            }
            index.setMirror(m, null);
        }
        for (UUID m : plan.leavePack()) {
            index.setMirror(m, null);
            PackMembers.remove(server, m, "supernaturalbloodlines.pack.left_party", "");
        }
    }
}