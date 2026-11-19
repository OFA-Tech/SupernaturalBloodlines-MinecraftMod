package net.ofatech.supernaturalbloodlines.parts.species.npc;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Order;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import org.jetbrains.annotations.Nullable;

/**
 * What {@code /pack npc ...} and the hub's buttons do to a pack member (R148). The token is the ledger's short id, never an entity id; the sender must be the
 * member's owner or the leader of the pack the owner is in ({@link NpcRules#mayOrder}); the member must be loaded to change what it does. A Tensura
 * subordinate keeps Tensura's own orders: nothing here changes it. Every method returns the error to show, or null when it worked.
 */
public final class NpcCommands {
    private static final String KEY = "supernaturalbloodlines.npc.";

    private NpcCommands() {
    }

    /** The member behind a token, if the sender may command it; otherwise the error. */
    private record Target(@Nullable Component error, @Nullable NpcIndex.Entry entry, @Nullable Mob mob) {
    }

    private static Target find(ServerPlayer sender, int token, boolean needLoaded) {
        PackIndex packs = PackIndex.get(sender.serverLevel());
        Optional<NpcIndex.Entry> found = NpcIndex.get(sender.serverLevel()).byToken(token);
        if (found.isEmpty()) {
            return new Target(Component.translatable(KEY + "unknown"), null, null);
        }
        NpcIndex.Entry e = found.get();
        boolean asOwner = e.owner().equals(sender.getUUID());
        boolean asLeader = packs.isLeader(sender.getUUID()) && packs.samePackNow(sender.getUUID(), e.owner());
        if (!NpcRules.mayOrder(asOwner, asLeader)) {
            return new Target(Component.translatable(KEY + "unknown"), null, null); // not yours: the same answer as for a token that is nobody's
        }
        Optional<Mob> mob = NpcMembers.live(sender.server, e.npc());
        if (needLoaded && mob.isEmpty()) {
            return new Target(Component.translatable(KEY + "not_loaded", e.name()), e, null);
        }
        return new Target(null, e, mob.orElse(null));
    }

    private static Component ourOrders(NpcBond bond) {
        return NpcRules.hasOurOrders(bond.source()) ? null : Component.translatable(KEY + "tensura_orders");
    }

    public static Component order(ServerPlayer sender, int token, String word) {
        Order order = Order.of(word);
        if (order == null) {
            return Component.translatable(KEY + "bad_order", word);
        }
        Target t = find(sender, token, true);
        if (t.error() != null) {
            return t.error();
        }
        NpcBond bond = t.mob().getData(SpeciesRegistries.NPC_BOND);
        if (ourOrders(bond) != null) {
            return ourOrders(bond);
        }
        // a post is where it stands when told to guard or rest; following has none
        t.mob().setData(SpeciesRegistries.NPC_BOND, bond.withOrder(order, order == Order.FOLLOW ? 0 : t.mob().blockPosition().asLong()));
        NpcOrders.reset(t.mob());
        sender.sendSystemMessage(Component.translatable(KEY + "order_set", t.mob().getDisplayName(), Component.translatable(KEY + "order." + order.name().toLowerCase(java.util.Locale.ROOT))));
        return null;
    }

    public static Component toggleHunt(ServerPlayer sender, int token) {
        Target t = find(sender, token, true);
        if (t.error() != null) {
            return t.error();
        }
        NpcBond bond = t.mob().getData(SpeciesRegistries.NPC_BOND);
        if (ourOrders(bond) != null) {
            return ourOrders(bond);
        }
        t.mob().setData(SpeciesRegistries.NPC_BOND, bond.withHunt(!bond.hunt()));
        sender.sendSystemMessage(Component.translatable(KEY + (bond.hunt() ? "hunt_off" : "hunt_on"), t.mob().getDisplayName()));
        return null;
    }

    public static Component toggleSort(ServerPlayer sender, int token) {
        Target t = find(sender, token, true);
        if (t.error() != null) {
            return t.error();
        }
        NpcBond bond = t.mob().getData(SpeciesRegistries.NPC_BOND);
        if (ourOrders(bond) != null) {
            return ourOrders(bond);
        }
        t.mob().setData(SpeciesRegistries.NPC_BOND, bond.withSort(!bond.sort()));
        sender.sendSystemMessage(Component.translatable(KEY + (bond.sort() ? "sort_off" : "sort_on"), t.mob().getDisplayName()));
        return null;
    }

    public static Component release(ServerPlayer sender, int token) {
        Target t = find(sender, token, false);
        if (t.error() != null) {
            return t.error();
        }
        if (t.entry().source() != NpcBond.Source.BROTHER) {
            return Component.translatable(KEY + "tensura_orders"); // a Tensura subordinate is released by its Tensura owner, not here
        }
        if (t.mob() != null) {
            NpcMembers.release(t.mob());
        } else {
            NpcIndex.get(sender.serverLevel()).remove(t.entry().npc());
        }
        sender.sendSystemMessage(Component.translatable(KEY + "released", t.entry().name()));
        return null;
    }

    /** One line per member of the sender's pack, for typed use; the hub shows the same list as rows. */
    public static List<Component> list(ServerPlayer sender) {
        PackIndex packs = PackIndex.get(sender.serverLevel());
        return NpcMembers.ofPack(sender.serverLevel(), packs.pack(sender.getUUID())).stream()
                .map(e -> (Component) Component.translatable(KEY + "list_row", e.token(), e.name(), Component.translatable(KEY + "source." + e.source().name().toLowerCase(java.util.Locale.ROOT)))).toList();
    }
}
