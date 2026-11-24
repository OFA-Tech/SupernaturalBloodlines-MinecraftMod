package net.ofatech.supernaturalbloodlines.parts.species.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.group.GroupSwitch;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackMembers;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * A leader who changes kind changes the group's type (spec 3.4). Nobody is kicked and no row is deleted: what belongs to another
 * type is kept and hidden, and comes back with the type. Checked when the leader's kind changes, when the lead moves, at login and
 * every 200 ticks; an offline leader is never switched, their facts cannot be read.
 */
public final class GroupSwitcher {
    private static final int PERIOD = 200;
    private static final List<Consumer<Switched>> HOOKS = new CopyOnWriteArrayList<>();
    private static boolean registered;

    /** What the type-specific plans cancel when a group changes type (a contest, a vote, a circle). */
    public record Switched(MinecraftServer server, UUID leader, UUID group, GroupType from, GroupType to) {
    }

    private GroupSwitcher() {
    }

    public static void onSwitched(Consumer<Switched> hook) {
        HOOKS.add(hook);
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        BloodlineAccess.addListener((p, change) -> refresh(p));
        // joining or leaving the vampires or the hunters changes what a leader is without touching the bloodline state
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> {
            if (e.getPlayer().getPlayer() instanceof ServerPlayer p && !p.level().isClientSide) {
                refresh(p);
            }
        });
        PackIndex.onLeadershipMoved((old, next) -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerPlayer leader = server == null ? null : SireCall.player(server, next);
            if (leader != null) {
                refresh(leader);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                refresh(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % PERIOD == 0) {
                sweep(e.getServer());
            }
        });
    }

    /** Every online leader, and the ranks of people who are in no group. */
    public static void sweep(MinecraftServer server) {
        PackIndex packs = PackIndex.get(server.overworld());
        for (UUID leader : List.copyOf(packs.leaders())) {
            ServerPlayer p = SireCall.player(server, leader);
            if (p != null) {
                refresh(p);
            }
        }
        GroupIndex.get(server.overworld()).prune(packs);
    }

    /** @return the switch made, empty when the group is the type its leader's kind says (or the player leads nobody) */
    public static Optional<GroupSwitch.Switch> refresh(ServerPlayer leader) {
        PackIndex packs = PackIndex.get(leader.serverLevel());
        UUID id = leader.getUUID();
        if (!packs.isLeader(id) || packs.betasOf(id).isEmpty()) {
            return Optional.empty();
        }
        GroupIndex groups = GroupIndex.get(leader.serverLevel());
        GroupType derived = Groups.derive(leader);
        long now = PackIndex.now();
        Optional<UUID> existing = groups.existingGroupOf(id);
        if (derived == GroupType.COVEN && existing.map(groups::typeOf).orElse(GroupType.PACK) != GroupType.COVEN
                && (GroupGate.shunned(leader.server, id) || packs.betasOf(id).stream().anyMatch(member -> GroupGate.shunned(leader.server, member)))) {
            return Optional.empty();
        }
        if (existing.isEmpty()) {
            groups.ensure(id, derived, now);
            return Optional.empty();
        }
        UUID group = existing.get();
        Optional<GroupSwitch.Switch> plan = GroupSwitch.plan(groups.typeOf(group), derived);
        if (plan.isEmpty()) {
            return plan;
        }
        groups.setType(group, derived, now);
        MinecraftServer server = leader.server;
        // the per-type key names the type itself, so the notice carries no argument
        PackMembers.tell(server, id, GroupText.key(derived, "type_changed"), "");
        for (UUID member : packs.betasOf(id)) {
            PackMembers.tell(server, member, GroupText.key(derived, "type_changed"), "");
        }
        HOOKS.forEach(h -> h.accept(new Switched(server, id, group, plan.get().from(), derived)));
        return plan;
    }
}
