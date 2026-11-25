package net.ofatech.supernaturalbloodlines.parts.species.group.coven;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleStage;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.shared.supernatural.CircleQuery;

/** What the coven perks listen to, with or without Werewolves (the species part registers it through {@code GroupsRegistry}). */
public final class CovenRegistry {
    private static boolean registered;

    private CovenRegistry() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        CircleQuery.set(new CircleQuery.Source() {
            @Override
            public int size(ServerLevel level, BlockPos chalk) {
                return CircleLink.sizeAt(level, chalk);
            }

            @Override
            public int staged() {
                CircleStage.Staged s = CircleStage.current();
                return s == null ? 0 : s.result().shares().size();
            }

            @Override
            public boolean inPack(ServerLevel level, java.util.UUID player) {
                PackIndex packs = PackIndex.get(level);
                return packs.inPack(player) && Groups.typeOfGroup(level, packs.leaderOf(player)) == GroupType.PACK;
            }

            @Override
            public boolean dayWalker(ServerLevel level, java.util.UUID player) {
                var clan = net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanScope.of(level, player);
                if (clan == null || !GroupsConfig.clanPerksOn()) {
                    return false;
                }
                var groups = net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.get(level);
                return clan.leader().equals(player) || groups.rankOf(player, net.ofatech.supernaturalbloodlines.core.group.GroupType.CLAN) == net.ofatech.supernaturalbloodlines.core.group.GroupRank.DAY_WALKER;
            }

            @Override
            public boolean switchOn(String name) {
                return switch (name) {
                    case "moon_ring" -> GroupsConfig.moonlightRingEnabled();
                    case "sun_ring" -> GroupsConfig.sunRingForgeEnabled();
                    case "sun_ring_day_walker_only" -> GroupsConfig.sunRingDayWalkerOnly();
                    default -> false;
                };
            }
        });
        net.ofatech.supernaturalbloodlines.parts.species.group.circle.LinkWindow.register();
        GroupSwitcher.onSwitched(e -> {
            CircleLink.closeGroup(e.server(), e.leader());
            CovenGovernance.cancelAll(e.server(), e.group());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                CircleLink.dropMember(p.server, p.getUUID(), true);
                CircleLink.calls().forget(p.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                CircleLink.anchorLeft(p.server, p.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 200 == 0) {
                CircleLink.sweep(e.getServer());
                CovenGovernance.sweep(e.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> CircleLink.reset());
    }
}
