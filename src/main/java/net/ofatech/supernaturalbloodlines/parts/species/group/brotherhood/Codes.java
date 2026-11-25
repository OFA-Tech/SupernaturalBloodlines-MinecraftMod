package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.CodeLedger;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.CodeRules;
import net.ofatech.supernaturalbloodlines.core.group.clan.ExileRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.BreakerRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.MarkTracker;
import net.ofatech.supernaturalbloodlines.parts.species.pack.Innocence;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackMembers;

/**
 * The Code (R294): "We hunt those who hunt us". It punishes and never rewards. A brother who kills an innocent loses a share of the mark, is a Code-breaker for
 * a day and has a strike; the third strike in a week puts them out of that brotherhood for a week (a leader is never put out). A Code-breaker is a player row,
 * so it follows the player to another brotherhood and ends by the clock.
 */
public final class Codes {
    private static final long DAY = 24L * 3_600_000L;
    private static CodeLedger ledger = new CodeLedger();

    private Codes() {
    }

    /** For tests: forgets the breaks the ledger has counted. */
    public static void resetLedger() {
        ledger = new CodeLedger();
    }

    /** The one question the four perks and the lead ask after {@link BrotherhoodScope#appliesTo}. */
    public static boolean isBreaker(MinecraftServer server, UUID id) {
        return GroupIndex.get(server.overworld()).breakerOf(id).map(r -> CodeRules.isBreaker(r.until(), PackIndex.now())).orElse(false);
    }

    /** The kill feed: only a player who is a brother is ever judged. */
    static void onResolved(MarkTracker.Resolved r) {
        if (r.killer() instanceof ServerPlayer killer && !killer.level().isClientSide) {
            judge(killer, r.victim());
        }
    }

    /** What the Code makes of this kill; public so a test can say a kill happened without a death. */
    public static void judge(ServerPlayer killer, LivingEntity victim) {
        if (!GroupsConfig.codeEnabled()) {
            return;
        }
        ServerLevel level = killer.serverLevel();
        BrotherhoodScope.Brotherhood b = BrotherhoodScope.of(level, killer.getUUID());
        if (b == null || !BrotherhoodScope.appliesTo(level, b.leader(), killer)) {
            return;
        }
        CodeRules.Kind kind = victim instanceof Villager ? CodeRules.Kind.VILLAGER : victim instanceof Player ? CodeRules.Kind.PLAYER : CodeRules.Kind.OTHER;
        boolean fighting = victim instanceof Player && !Innocence.of(killer, victim);
        if (!CodeRules.breaks(new CodeRules.Victim(kind, MarkTracker.isPrey(victim), fighting))) {
            return;
        }
        long now = PackIndex.now();
        if (!ledger.countsOnce(killer.getUUID(), victim.getUUID(), now)) {
            return;
        }
        broke(killer, now);
    }

    private static void broke(ServerPlayer killer, long now) {
        MinecraftServer server = killer.server;
        ServerLevel level = killer.serverLevel();
        UUID id = killer.getUUID();
        // the mark first: a leader who drops below a brother can be passed by it, and what follows is about the group as it stands then
        BloodlineAccess.applyEvent(killer, BloodlineEvent.CODE_BROKEN, SpeciesContext.of(killer, null));
        BrotherhoodScope.Brotherhood b = BrotherhoodScope.of(level, id);
        if (b == null) {
            return;
        }
        GroupIndex groups = GroupIndex.get(level);
        List<Long> strikes = new ArrayList<>(groups.breakerOf(id).map(BreakerRow::strikes).orElse(List.of()));
        strikes.add(now);
        List<Long> counting = CodeRules.strikesIn(strikes, now, GroupsConfig.codeStrikesDays() * DAY);
        long until = CodeRules.breakerUntil(now, GroupsConfig.codeBreakerHours());
        List<UUID> roster = PackIndex.get(level).roster(b.leader());
        String name = killer.getGameProfile().getName();
        if (!CodeRules.expels(counting.size(), GroupsConfig.codeStrikes())) {
            groups.setBreaker(id, new BreakerRow(until, counting));
            tellAll(server, roster, null, "code_broken", name);
        } else if (b.leader().equals(id)) {
            groups.setBreaker(id, new BreakerRow(until, List.of()));
            tellAll(server, roster, null, "code_leader", name);
        } else {
            String leaderName = PackAccess.nameOf(server, b.leader());
            PackMembers.remove(server, id, BrotherhoodText.key("broke_code"), leaderName != null ? leaderName : b.leader().toString().substring(0, 8));
            groups.ban(b.group().toString(), id, ExileRules.until(now, GroupsConfig.codeExileDays()));
            groups.setBreaker(id, new BreakerRow(until, List.of()));
            tellAll(server, roster, id, "code_expelled", name);
        }
        net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.changed(killer);
    }

    private static void tellAll(MinecraftServer server, List<UUID> roster, UUID except, String key, String name) {
        for (UUID member : roster) {
            if (!member.equals(except)) {
                PackMembers.tell(server, member, BrotherhoodText.key(key), name);
            }
        }
    }

    static void warnOnce(Exception e) {
        SupernaturalBloodlines.LOGGER.error("The Code failed on a kill", e);
    }
}