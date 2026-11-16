package net.ofatech.supernaturalbloodlines.parts.species.pack.party;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackLeadership;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules.Party;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules.Plan;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackMembers;

/**
 * A pack is a party in Open Parties and Claims (plan 14): the founding leader is its owner, the other Alphas its admins, the
 * rest members; a player who leaves the party leaves the pack, and a player who leaves the pack leaves the party. Does
 * nothing when the mod (or its Team Claims fork, same id) is not there.
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
        return backend != null;
    }

    public static void syncAll(MinecraftServer server) {
        for (UUID leader : new ArrayList<>(PackIndex.get(server.overworld()).leaders())) {
            sync(server, leader, false);
        }
    }

    /** One pass for the pack {@code member} is in. {@code rosterChanged}: somebody joined, left or was kicked just now. */
    public static void sync(MinecraftServer server, UUID member, boolean rosterChanged) {
        PartyBackend b = backend;
        if (b == null) {
            return;
        }
        PackIndex index = PackIndex.get(server.overworld());
        UUID leader = index.leaderOf(member);
        List<UUID> pack = index.pack(leader);
        Set<UUID> alphas = new HashSet<>();
        pack.forEach(id -> {
            if (PackLeadership.isAlpha(index.strengthOf(id))) {
                alphas.add(id);
            }
        });
        Party party = b.find(server, pack);
        Set<UUID> ours = new HashSet<>();
        index.mirrorsOf(pack).forEach(ours::add);
        Plan plan = PartySyncRules.plan(leader, new HashSet<>(pack), alphas, party, ours, rosterChanged);

        UUID owner = party == null ? leader : party.owner();
        if (plan.disband() && party != null) {
            b.disband(server, party.owner());
            owner = leader;
        }
        if (plan.create() && !b.create(server, leader)) {
            return;
        }
        if (plan.create()) {
            owner = leader;
        }
        for (Map.Entry<UUID, Boolean> e : plan.add().entrySet()) {
            if (!e.getKey().equals(owner)) {
                b.add(server, owner, e.getKey(), e.getValue());
            }
        }
        plan.removeFrom().forEach(id -> b.remove(server, party != null ? party.owner() : leader, id));
        for (UUID id : plan.leavePack()) {
            PackMembers.remove(server, id, Component.translatable("supernaturalbloodlines.pack.left_party"));
        }
        for (UUID id : ours) {
            if (!plan.ours().contains(id)) {
                index.setMirror(id, null);
            }
        }
        for (UUID id : plan.ours()) {
            index.setMirror(id, owner);
        }
    }
}
