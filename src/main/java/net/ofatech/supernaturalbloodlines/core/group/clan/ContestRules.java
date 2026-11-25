package net.ofatech.supernaturalbloodlines.core.group.clan;

import java.util.Optional;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;

/** R272, R277: who may challenge for the Charter, when the Game of Kings ends and who then leads. */
public final class ContestRules {
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long HOUR = 60L * 60 * 1000;
    private static final long TICKS_PER_MINUTE = 1200;

    /** In the order they are checked. */
    public enum Refusal {
        NOT_DAY_WALKER, TOO_NEW, LEADER_AWAY, NO_CHARTER, TOO_FEW, ALREADY, COOLDOWN;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** 0 is no refusal; a refusal is its place in the list plus one, so the wire holds one small number and the bound is {@code values().length}. */
        public static int wire(Optional<Refusal> refusal) {
            return refusal.map(r -> r.ordinal() + 1).orElse(0);
        }

        public static Optional<Refusal> fromWire(int number) {
            return number >= 1 && number <= values().length ? Optional.of(values()[number - 1]) : Optional.empty();
        }
    }

    /** Who holds the Charter at the end: its state, who, and the facts that decide whether they lead. {@code id} is null when nobody is on record. */
    public record Holder(CharterState state, UUID id, boolean online, boolean alive, GroupRank rank, boolean inClan) {
    }

    /** Why a midnight changed nobody: each one has its own line for the clan. */
    public static final java.util.List<String> REASONS = java.util.List.of("not_held", "left", "leader_holds", "offline", "dead", "not_day_walker", "forfeit_challenger_away");

    /** {@code reason} is empty exactly when somebody changes; {@code forfeit} says the head's Charter was not in hand and the challenger won by that. */
    public record Outcome(Optional<UUID> newLeader, String reason, boolean forfeit) {
        static Outcome change(UUID id, boolean forfeit) {
            return new Outcome(Optional.of(id), "", forfeit);
        }

        static Outcome none(String reason) {
            return new Outcome(Optional.empty(), reason, false);
        }
    }

    private ContestRules() {
    }

    public static Optional<Refusal> mayChallenge(GroupRank rank, int daysInClan, int members, boolean leaderOnline, boolean liveCharter, boolean pending,
                                                 long cooldownUntil, long nowMillis, int minDays, int minMembers) {
        if (rank != GroupRank.DAY_WALKER) {
            return Optional.of(Refusal.NOT_DAY_WALKER);
        }
        if (daysInClan < minDays) {
            return Optional.of(Refusal.TOO_NEW);
        }
        if (!leaderOnline) {
            return Optional.of(Refusal.LEADER_AWAY);
        }
        if (!liveCharter) {
            return Optional.of(Refusal.NO_CHARTER);
        }
        if (members < minMembers) {
            return Optional.of(Refusal.TOO_FEW);
        }
        if (pending) {
            return Optional.of(Refusal.ALREADY);
        }
        return cooldownUntil > nowMillis ? Optional.of(Refusal.COOLDOWN) : Optional.empty();
    }

    public static Outcome resolve(Holder holder, UUID leader) {
        return resolve(holder, leader, null, false);
    }

    /**
     * An online, alive Day Walker of the clan holding the Charter in {@code HELD} leads. A Charter that is {@code STORED} because the head was its last
     * holder is forfeited: the challenger leads when they are online, alive and a Day Walker of the clan. Anything else changes nobody.
     */
    public static Outcome resolve(Holder holder, UUID leader, Holder challenger, boolean headStored) {
        if (holder.state() == CharterState.STORED && headStored && challenger != null) {
            boolean can = challenger.id() != null && challenger.inClan() && challenger.online() && challenger.alive() && challenger.rank() == GroupRank.DAY_WALKER;
            return can ? Outcome.change(challenger.id(), true) : Outcome.none("forfeit_challenger_away");
        }
        if (holder.state() != CharterState.HELD) {
            return Outcome.none("not_held");
        }
        if (holder.id() == null || !holder.inClan()) {
            return Outcome.none("left");
        }
        if (holder.id().equals(leader)) {
            return Outcome.none("leader_holds");
        }
        if (!holder.online()) {
            return Outcome.none("offline");
        }
        if (!holder.alive()) {
            return Outcome.none("dead");
        }
        return holder.rank() == GroupRank.DAY_WALKER ? Outcome.change(holder.id(), false) : Outcome.none("not_day_walker");
    }

    /** The day time at which a contest started at {@code startedDayTime} ends: the first midnight at least {@code minMinutes} after the start. */
    public static long endsAt(long startedDayTime, int minMinutes) {
        return Midnight.nextAtOrAfter(startedDayTime + minMinutes * TICKS_PER_MINUTE);
    }

    /** The midnight has come (a day time at or after {@link #endsAt}, however it got there), or {@code maxHours} of wall time have passed. */
    public static boolean due(long startedDayTime, long nowDayTime, long startedMillis, long nowMillis, int minMinutes, int maxHours) {
        return nowDayTime >= endsAt(startedDayTime, minMinutes) || nowMillis - startedMillis >= maxHours * HOUR;
    }

    public static boolean autoStart(long acceptBy, long now) {
        return now >= acceptBy;
    }
}