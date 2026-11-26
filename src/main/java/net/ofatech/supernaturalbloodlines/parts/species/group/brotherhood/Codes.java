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
import net.ofatech.supernaturalbloodlines.parts.species.pack.FightLedger;
import net.ofatech.supernaturalbloodlines.parts.species.pack.Innocence;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackMembers;

/**
 * The Code (R294): "We hunt those who hunt us". It punishes and never rewards. A brother who kills an innocent loses a share of the mark, is a Code-breaker for
 * a day and has a strike; the third strike in a week puts them out of that brotherhood for a week (a leader is never put out). A Code-breaker is a player row,
 * so it follows the player to another brotherhood and ends by the clock.
 */
public final class Codes {
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

    /** The kill feed: the Code judges the player behind the death ({@link #culprit}). */
    static void onResolved(MarkTracker.Resolved r) {
        ServerPlayer killer = culprit(r);
        if (killer != null) {
            judge(killer, r.victim());
        }
    }

    /**
     * The player a death is put on (R294, R295): whoever dealt the killing blow, or owns the tamed pet that did; for a death the world dealt (fire, lava, a
     * fall, poison, wither, drowning), the player whose direct melee hit last landed on the victim in the last 10 seconds ({@link FightLedger}), never the kill
     * credit, which a later snowball can steal. Kill credit with no such hit, the void and /kill are nobody's.
     */
    static ServerPlayer culprit(MarkTracker.Resolved r) {
        if (r.environmental()) {
            UUID striker = FightLedger.lastMeleeStriker(r.victim().getUUID(), PackIndex.now());
            return striker == null || r.victim().getServer() == null ? null
                    : net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.player(r.victim().getServer(), striker);
        }
        return r.killingBlow() ? ownerOrSelf(r.killer()) : null;
    }

    /** Whether, in the last 10 seconds, {@code outsider} deliberately hurt any brother of this brotherhood ({@code outsiderStruck}), or any brother hurt them. */
    static boolean fought(ServerLevel level, UUID leader, UUID outsider, boolean outsiderStruck) {
        long now = PackIndex.now();
        for (UUID brother : PackIndex.get(level).roster(leader)) {
            if (outsiderStruck ? FightLedger.struck(outsider, brother, now) : FightLedger.struck(brother, outsider, now)) {
                return true;
            }
        }
        return false;
    }

    /** The player behind a kill: the player, or the owner of a tamed pet. */
    private static ServerPlayer ownerOrSelf(net.minecraft.world.entity.Entity e) {
        if (e instanceof ServerPlayer p) {
            return p;
        }
        if (e instanceof net.minecraft.world.entity.OwnableEntity pet && pet.getOwnerUUID() != null && e.getServer() != null) {
            return net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.player(e.getServer(), pet.getOwnerUUID());
        }
        return null;
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
        // defending a brother: a victim who hurt any brother of this brotherhood was fighting every one of them
        boolean fighting = victim instanceof Player && (!Innocence.of(killer, victim) || fought(level, b.leader(), victim.getUUID(), true));
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
        // read before the mark loss: a leader that loss lets a brother pass is still judged as the leader who broke the Code
        BrotherhoodScope.Brotherhood before = BrotherhoodScope.of(level, id);
        boolean wasLeader = before != null && before.leader().equals(id);
        BloodlineAccess.applyEvent(killer, BloodlineEvent.CODE_BROKEN, SpeciesContext.of(killer, null));
        BrotherhoodScope.Brotherhood b = BrotherhoodScope.of(level, id);
        if (b == null) {
            return;
        }
        GroupIndex groups = GroupIndex.get(level);
        List<Long> strikes = new ArrayList<>(groups.breakerOf(id).map(BreakerRow::strikes).orElse(List.of()));
        strikes.add(now);
        List<Long> counting = CodeRules.strikesIn(strikes, now, CodeRules.windowMillis(GroupsConfig.codeStrikesDays()));
        long until = CodeRules.breakerUntil(now, GroupsConfig.codeBreakerHours());
        List<UUID> roster = PackIndex.get(level).roster(b.leader());
        String name = killer.getGameProfile().getName();
        int hours = GroupsConfig.codeBreakerHours();
        int limit = GroupsConfig.codeStrikes();
        boolean marked = BloodlineAccess.get(killer).species() == net.ofatech.supernaturalbloodlines.core.bloodline.Species.HUNTER_OF_THE_FIVE;
        killer.sendSystemMessage(marked ? BrotherhoodText.of("code_you", BloodlineAccess.get(killer).markKills(), hours, counting.size(), limit)
                : BrotherhoodText.of("code_you.unmarked", hours, counting.size(), limit));
        if (!CodeRules.expels(counting.size(), limit)) {
            groups.setBreaker(id, new BreakerRow(until, counting));
            tellAll(server, roster, null, "code_broken", name);
        } else if (wasLeader) {
            groups.setBreaker(id, new BreakerRow(until, List.of()));
            // a leader the mark loss has just passed is no longer the one who leads: the plain line says what happened
            tellAll(server, roster, null, b.leader().equals(id) ? "code_leader" : "code_broken", name);
        } else {
            String leaderName = BrotherhoodText.nameOf(server, b.leader());
            PackMembers.remove(server, id, BrotherhoodText.key(leaderName != null ? "broke_code" : "broke_code.someone"), leaderName != null ? leaderName : "");
            killer.sendSystemMessage(BrotherhoodText.of("code_out_days", GroupsConfig.codeExileDays()));
            groups.ban(b.group().toString(), id, ExileRules.until(now, GroupsConfig.codeExileDays()));
            groups.setBreaker(id, new BreakerRow(until, List.of()));
            tellAll(server, roster, id, "code_expelled", name);
        }
        net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.changed(killer);
        BrotherhoodViews.push(server, b.leader());
    }

    private static void tellAll(MinecraftServer server, List<UUID> roster, UUID except, String key, String name) {
        for (UUID member : roster) {
            if (!member.equals(except)) {
                PackMembers.tell(server, member, BrotherhoodText.key(key), name);
            }
        }
    }
}