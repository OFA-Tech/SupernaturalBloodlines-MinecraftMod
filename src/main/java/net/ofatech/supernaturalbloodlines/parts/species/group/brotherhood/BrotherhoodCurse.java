package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.CurseExtend;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterCurse;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.MarkTracker;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * The Hunter's Curse for every brother (R295): prey who kill any brother who is not marked are cursed by the same curse, and it ends by itself after
 * {@code brotherhood_curse_hours} unless a Potential awakens first. The curse of a marked hunter is the old path, with no timer. A curse already carried
 * is never touched. The end runs whatever the switches say: switching a perk off never leaves a curse running forever.
 */
public final class BrotherhoodCurse {
    private BrotherhoodCurse() {
    }

    static void onResolved(MarkTracker.Resolved r) {
        if (!GroupsConfig.curseExtendsEnabled() || !GroupsConfig.brotherhoodPerksOn() || r.victimWasMarked()
                || !(r.killer() instanceof ServerPlayer killer) || !(r.victim() instanceof ServerPlayer victim)) {
            return;
        }
        ServerLevel level = victim.serverLevel();
        BrotherhoodScope.Brotherhood b = BrotherhoodScope.of(level, victim.getUUID());
        if (b == null || !BrotherhoodScope.appliesTo(level, b.leader(), victim) || !MarkTracker.isPrey(killer) || PackIndex.samePack(killer, victim)) {
            return;
        }
        boolean already = BloodlineAccess.get(killer).hunterCurse();
        if (already) {
            return;
        }
        HunterCurse.curse(killer);
        if (BloodlineAccess.get(killer).hunterCurse()) {
            OptionalLong end = CurseExtend.endAt(false, false, PackIndex.now(), GroupsConfig.brotherhoodCurseHours());
            end.ifPresent(until -> GroupIndex.get(level).setCurse(killer.getUUID(), until));
        }
    }

    /** Ends the curses whose time is up and drops the rows of players who no longer carry it. Whatever the switches say. */
    public static void expire(MinecraftServer server) {
        GroupIndex groups = GroupIndex.get(server.overworld());
        long now = PackIndex.now();
        for (Map.Entry<UUID, Long> row : groups.curses().entrySet()) {
            ServerPlayer p = SireCall.player(server, row.getKey());
            if (p != null && !BloodlineAccess.get(p).hunterCurse()) {
                groups.clearCurse(row.getKey());
            } else if (CurseExtend.expired(row.getValue(), now)) {
                if (p != null) {
                    HunterCurse.liftOne(p);
                } else {
                    HunterCurse.liftLater(server, row.getKey());
                }
                groups.clearCurse(row.getKey());
            }
        }
    }
}