package net.ofatech.supernaturalbloodlines.parts.species.group.clan;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterState;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.CharterRow;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/**
 * A dropped Charter (R279): only the clan's members can pick it up, the entity cannot be hurt and never despawns; a void fall or a kill is a loss, a pick-up
 * or a hopper is not (the stack is empty by then, and the next scan says who holds it).
 */
final class CharterGuard {
    private CharterGuard() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(CharterGuard::onPickup);
        NeoForge.EVENT_BUS.addListener(CharterGuard::onJoin);
        NeoForge.EVENT_BUS.addListener(CharterGuard::onLeave);
    }

    private static void onPickup(ItemEntityPickupEvent.Pre e) {
        ItemStack stack = e.getItemEntity().getItem();
        if (!Charters.isCharter(stack) || !(e.getItemEntity().level() instanceof ServerLevel level)) {
            return;
        }
        GroupIndex groups = GroupIndex.get(level.getServer().overworld());
        Optional<CharterRow> row = Charters.rowOf(groups, stack);
        if (row.isPresent() && !Charters.isMember(level.getServer(), e.getPlayer().getUUID(), stack.get(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.CHARTER_GROUP.get()))) {
            e.setCanPickup(TriState.FALSE);
        }
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getEntity() instanceof ItemEntity item) || !Charters.isCharter(item.getItem())) {
            return;
        }
        GroupIndex groups = GroupIndex.get(level.getServer().overworld());
        Optional<CharterRow> row = Charters.rowOf(groups, item.getItem());
        if (row.isEmpty()) {
            UUID of = item.getItem().get(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.CHARTER_GROUP.get());
            // a clan that is gone, or a clan whose Charter has another id (an old copy, a made-up stack), never enters the world; a clan with no row at all
            // is a row that was unreadable on load: that is not this stack's doing, so it is left alone
            if (of == null || groups.leaderOfGroup(of).isEmpty() || groups.charterOf(of).isPresent()) {
                e.setCanceled(true);
            }
            return;
        }
        if (row.get().state() == CharterState.LOST) {
            return;
        }
        item.setInvulnerable(true);
        item.setUnlimitedLifetime();
        UUID group = item.getItem().get(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.CHARTER_GROUP.get());
        CharterRow r = row.get();
        long now = PackIndex.now();
        if (r.state() == CharterState.DROPPED && r.entity().filter(item.getUUID()::equals).isPresent()) {
            if (now - r.seenAt() >= CharterScan.STALE) {
                groups.setCharter(group, new CharterRow(r.id(), Optional.empty(), CharterState.DROPPED, r.forgedAt(), now, r.entity(), 0)); // its chunk loaded again: seen
            }
            return;
        }
        groups.setCharter(group, new CharterRow(r.id(), Optional.empty(), CharterState.DROPPED, r.forgedAt(), now, Optional.of(item.getUUID()), 0));
    }

    private static void onLeave(EntityLeaveLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getEntity() instanceof ItemEntity item) || item.getRemovalReason() == null || !item.getRemovalReason().shouldDestroy()) {
            return;
        }
        GroupIndex groups = GroupIndex.get(level.getServer().overworld());
        Optional<UUID> group = Charters.groupOfRow(groups, item.getUUID());
        if (group.isEmpty()) {
            return;
        }
        CharterRow r = groups.charterOf(group.get()).orElseThrow();
        long now = PackIndex.now();
        if (item.getItem().isEmpty()) {
            groups.setCharter(group.get(), new CharterRow(r.id(), Optional.empty(), CharterState.STORED, r.forgedAt(), now, Optional.empty(), 0));
            return;
        }
        groups.setCharter(group.get(), new CharterRow(r.id(), Optional.empty(), CharterState.LOST, r.forgedAt(), r.seenAt(), Optional.empty(), now));
        groups.leaders().stream().filter(l -> groups.existingGroupOf(l).filter(group.get()::equals).isPresent()).findFirst()
                .ifPresent(leader -> Charters.tellClan(level.getServer(), leader, ClanText.key("charter_lost")));
    }
}
