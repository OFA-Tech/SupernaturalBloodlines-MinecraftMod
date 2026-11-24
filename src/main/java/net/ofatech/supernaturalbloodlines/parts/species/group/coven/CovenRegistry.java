package net.ofatech.supernaturalbloodlines.parts.species.group.coven;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleStage;
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
        });
        GroupSwitcher.onSwitched(e -> CircleLink.closeGroup(e.server(), e.leader()));
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
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> CircleLink.reset());
    }
}