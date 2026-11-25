package net.ofatech.supernaturalbloodlines.parts.species.group.clan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterRules;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterState;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.CharterRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/**
 * Every 40 ticks (and when a contest ends): who holds the Charter. Only the main inventory, the hotbar and the off hand are read: not armour, not Curios,
 * not a shulker box or any other container item. Sightings are collected first and the inventories edited after, so the loop never edits what it reads.
 */
public final class CharterScan {
    private static final long DAY = 24L * 60 * 60 * 1000;

    private static boolean entityLoaded(MinecraftServer server, CharterRow row) {
        if (row.entity().isEmpty()) {
            return false;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(row.entity().get()) != null) {
                return true;
            }
        }
        return false;
    }

    /** A held Charter is seen again (and its row rewritten) only when its last sighting is this old. */
    static final long STALE = 60L * 60 * 1000;
    private static Supplier<List<ServerPlayer>> players;
    private static boolean warned;

    private CharterScan() {
    }

    /** For tests: the players the scan reads instead of the server's list; null restores it. */
    public static void overridePlayers(Supplier<List<ServerPlayer>> supplier) {
        players = supplier;
    }

    private record Found(ServerPlayer player, boolean offhand, int slot, ItemStack stack) {
        UUID id() {
            return stack.get(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.CHARTER_ID.get());
        }

        UUID group() {
            return stack.get(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.CHARTER_GROUP.get());
        }

        void clear() {
            if (offhand) {
                player.getInventory().offhand.set(0, ItemStack.EMPTY);
            } else {
                player.getInventory().items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    /** A thrown exception is logged once: a Charter must never break the tick. */
    public static void scanNow(MinecraftServer server) {
        try {
            run(server);
        } catch (RuntimeException e) {
            if (!warned) {
                warned = true;
                SupernaturalBloodlines.LOGGER.warn("Charter scan failed (logged once)", e);
            }
        }
    }

    private static void run(MinecraftServer server) {
        if (!GroupsConfig.charterActive()) {
            return;
        }
        ServerLevel level = server.overworld();
        GroupIndex groups = GroupIndex.get(level);
        PackIndex packs = PackIndex.get(level);
        long now = PackIndex.now();
        List<ServerPlayer> online = players != null ? players.get() : server.getPlayerList().getPlayers();
        Set<UUID> onlineIds = new HashSet<>();
        online.forEach(p -> onlineIds.add(p.getUUID()));
        Map<UUID, UUID> leaderOf = new HashMap<>();
        for (UUID leader : groups.leaders()) {
            groups.existingGroupOf(leader).ifPresent(g -> leaderOf.put(g, leader));
        }

        List<Found> found = new ArrayList<>();
        for (ServerPlayer p : online) {
            for (int i = 0; i < p.getInventory().items.size(); i++) {
                ItemStack s = p.getInventory().items.get(i);
                if (Charters.isCharter(s)) {
                    found.add(new Found(p, false, i, s));
                }
            }
            if (Charters.isCharter(p.getOffhandItem())) {
                found.add(new Found(p, true, 0, p.getOffhandItem()));
            }
        }

        List<Found> dust = new ArrayList<>();
        Map<UUID, List<Found>> valid = new LinkedHashMap<>();
        for (Found f : found) {
            UUID group = f.group();
            Optional<CharterRow> row = Charters.rowOf(groups, f.stack());
            UUID leader = group == null ? null : leaderOf.get(group);
            if (row.isEmpty() || leader == null) {
                dust.add(f);
            } else if (ClanScope.isClan(level, leader)) {
                valid.computeIfAbsent(group, k -> new ArrayList<>()).add(f);
            }
        }

        List<Found> strip = new ArrayList<>();
        Set<UUID> handled = new HashSet<>();
        Map<UUID, CharterRow> writes = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<Found>> e : valid.entrySet()) {
            UUID group = e.getKey();
            CharterRow row = groups.charterOf(group).orElseThrow();
            List<CharterRules.Sighting> sightings = new ArrayList<>();
            for (int i = 0; i < e.getValue().size(); i++) {
                sightings.add(new CharterRules.Sighting(e.getValue().get(i).player().getUUID(), i));
            }
            CharterRules.Kept kept = CharterRules.keep(sightings, row.holder());
            kept.dust().forEach(s -> dust.add(e.getValue().get(s.index())));
            Found keep = e.getValue().get(kept.kept().orElseThrow().index());
            handled.add(group);
            groups.contestOf(group).filter(GroupIndex.ContestRow::headStored).ifPresent(c ->
                    groups.setContest(group, new GroupIndex.ContestRow(c.challenger(), c.phase(), c.openedAt(), c.acceptBy(), c.startedAt(), c.startedDayTime(), false)));
            if (Charters.isMember(server, keep.player().getUUID(), group)) {
                boolean same = row.state() == CharterState.HELD && row.holder().filter(keep.player().getUUID()::equals).isPresent() && row.entity().isEmpty();
                long seen = same && now - row.seenAt() < STALE ? row.seenAt() : now;
                writes.put(group, new CharterRow(row.id(), Optional.of(keep.player().getUUID()), CharterState.HELD, row.forgedAt(), seen, Optional.empty(), 0));
            } else {
                strip.add(keep);
            }
        }

        for (Map.Entry<UUID, CharterRow> e : groups.charters().entrySet()) {
            UUID group = e.getKey();
            UUID leader = leaderOf.get(group);
            if (handled.contains(group) || leader == null || !ClanScope.isClan(level, leader)) {
                continue;
            }
            CharterRow row = e.getValue();
            if (row.state() == CharterState.HELD && row.holder().filter(onlineIds::contains).isPresent()) {
                if (row.holder().filter(leader::equals).isPresent()) {
                    groups.contestOf(group).filter(c -> c.phase() == GroupIndex.ContestRow.Phase.RUNNING && !c.headStored()).ifPresent(c ->
                            groups.setContest(group, new GroupIndex.ContestRow(c.challenger(), c.phase(), c.openedAt(), c.acceptBy(), c.startedAt(), c.startedDayTime(), true)));
                }
                row = new CharterRow(row.id(), Optional.empty(), CharterState.STORED, row.forgedAt(), row.seenAt(), Optional.empty(), 0);
            }
            if (row.state() == CharterState.HELD || row.state() == CharterState.STORED) {
                long offlineSince = row.holder().filter(h -> !onlineIds.contains(h)).flatMap(packs::lastSeen).orElse(0L);
                if (CharterRules.isLost(row.seenAt(), row.holder().isPresent(), offlineSince, now, GroupsConfig.charterLostDays(), SpeciesConfig.absentLeaderDays())) {
                    row = new CharterRow(row.id(), Optional.empty(), CharterState.LOST, row.forgedAt(), row.seenAt(), Optional.empty(), now);
                    Charters.tellClan(server, leader, ClanText.key("charter_lost"));
                }
            }
            if (row.state() == CharterState.DROPPED && !entityLoaded(server, row) && now - row.seenAt() >= GroupsConfig.charterLostDays() * DAY) {
                // its entity is in no loaded level and nothing has seen it for the days (a world that lost it, a jar that never knew the item): it is lost like any other
                row = new CharterRow(row.id(), Optional.empty(), CharterState.LOST, row.forgedAt(), row.seenAt(), Optional.empty(), now);
                Charters.tellClan(server, leader, ClanText.key("charter_lost"));
            }
            if (!row.equals(e.getValue())) {
                writes.put(group, row);
            }
        }

        writes.forEach((group, row) -> {
            CharterRow old = groups.charterOf(group).orElse(null);
            if (!row.equals(old)) {
                groups.setCharter(group, row);
                // only a change of state or holder is news to the clan; a refreshed seenAt is not
                if (old == null || old.state() != row.state() || !old.holder().equals(row.holder())) {
                    Charters.refresh(server, leaderOf.get(group));
                }
            }
        });
        for (Found f : dust) {
            f.clear();
            ServerLevel at = f.player().serverLevel();
            at.sendParticles(ParticleTypes.SMOKE, f.player().getX(), f.player().getY() + 1, f.player().getZ(), 12, 0.3, 0.4, 0.3, 0.01);
            f.player().sendSystemMessage(ClanText.of("charter_dust"));
        }
        for (Found f : strip) {
            ItemStack stack = f.stack().copy();
            f.clear();
            ItemEntity drop = new ItemEntity(f.player().serverLevel(), f.player().getX(), f.player().getY(), f.player().getZ(), stack);
            drop.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            drop.setDefaultPickUpDelay();
            f.player().serverLevel().addFreshEntity(drop);
            f.player().sendSystemMessage(ClanText.of("charter_taken"));
        }
    }
}
