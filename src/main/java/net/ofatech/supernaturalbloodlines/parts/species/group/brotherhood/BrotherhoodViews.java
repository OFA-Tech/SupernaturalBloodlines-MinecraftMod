package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodView;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodWire;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.CodeRules;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.MarkTracker;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;

/** The viewer's own brotherhood, as the hub draws it (R297): their own mark and bonus, the leader's recorded mark, and the Code's counts. Never another brother's kills. */
public final class BrotherhoodViews {
    private static final long DAY = 24L * 3_600_000L;

    private BrotherhoodViews() {
    }

    public static BrotherhoodView of(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BrotherhoodScope.Brotherhood b = BrotherhoodScope.of(level, p.getUUID());
        if (b == null) {
            return BrotherhoodView.NONE;
        }
        boolean fits = BrotherhoodScope.fits(p);
        boolean perks = GroupsConfig.brotherhoodPerksOn();
        if (!BrotherhoodScope.appliesTo(level, b.leader(), p)) {
            return new BrotherhoodView(true, fits, perks, 0, 0, false, 0, 0, false, false, 0, 0, 0, false);
        }
        PackIndex packs = PackIndex.get(level);
        GroupIndex groups = GroupIndex.get(level);
        long now = PackIndex.now();
        int own = BrotherhoodMarks.of(p);
        int leaderMark = p.getUUID().equals(b.leader()) ? own : BrotherhoodMarks.recorded(level, b.leader());
        Map<UUID, Integer> recorded = groups.marks();
        boolean anyMax = own >= MarkTracker.CAP || packs.roster(b.leader()).stream().anyMatch(id -> recorded.getOrDefault(id, 0) >= MarkTracker.CAP);
        int strikes = groups.breakerOf(p.getUUID()).map(r -> CodeRules.strikesIn(r.strikes(), now, GroupsConfig.codeStrikesDays() * DAY).size()).orElse(0);
        int seconds = groups.breakerOf(p.getUUID()).map(r -> BrotherhoodWire.clamp((r.until() - now) / 1000, BrotherhoodWire.MAX_SECONDS)).orElse(0);
        return new BrotherhoodView(true, true, true, own, leaderMark, anyMax, BrotherhoodArms.brothersNear(p), (int) Math.round(BrotherhoodArms.bonusFor(p) * 100),
                GroupsConfig.sightShareEnabled(), GroupsConfig.codeEnabled(), strikes, GroupsConfig.codeStrikes(), seconds, GroupsConfig.leadByMark());
    }

    /** Something about the brotherhood changed (a mark, a strike, a breaker, the lead): every online member's page is refreshed. */
    public static void push(MinecraftServer server, UUID anyMember) {
        ServerLevel level = server.overworld();
        PackIndex packs = PackIndex.get(level);
        UUID leader = packs.leaderOf(anyMember);
        List<UUID> roster = packs.roster(leader);
        for (UUID id : roster) {
            ServerPlayer p = SireCall.player(server, id);
            if (p != null) {
                HubRequests.changed(p);
            }
        }
    }
}
