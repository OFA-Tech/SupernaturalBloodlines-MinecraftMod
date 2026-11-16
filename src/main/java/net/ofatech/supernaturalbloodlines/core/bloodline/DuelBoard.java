package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Leadership duels (plan 14): a co-Alpha challenges the leader, the leader accepts, and they fight to the first defeat. This is
 * only the bookkeeping, with no game classes: who has asked whom, which duel is on, when it runs out. Nobody is in more than one
 * challenge or duel at a time.
 */
public final class DuelBoard {
    /** A challenge waits, and a duel lasts, this long. */
    public static final long TIMEOUT_TICKS = 2 * 60 * 20;

    public enum Phase { ASKED, ON }

    public static final class Duel {
        private final UUID challenger;
        private final UUID leader;
        private Phase phase = Phase.ASKED;
        private long expires;

        Duel(UUID challenger, UUID leader, long expires) {
            this.challenger = challenger;
            this.leader = leader;
            this.expires = expires;
        }

        public UUID challenger() {
            return challenger;
        }

        public UUID leader() {
            return leader;
        }

        public Phase phase() {
            return phase;
        }

        public boolean involves(UUID p) {
            return challenger.equals(p) || leader.equals(p);
        }

        public UUID other(UUID p) {
            return challenger.equals(p) ? leader : challenger;
        }
    }

    public record Result(UUID winner, UUID loser, boolean challengerWon) {
    }

    private final List<Duel> duels = new ArrayList<>();

    private Optional<Duel> of(UUID p) {
        return duels.stream().filter(d -> d.involves(p)).findFirst();
    }

    /** @return false when either of them is already in a challenge or a duel, or it is the same player */
    public boolean challenge(UUID challenger, UUID leader, long now) {
        if (challenger.equals(leader) || of(challenger).isPresent() || of(leader).isPresent()) {
            return false;
        }
        duels.add(new Duel(challenger, leader, now + TIMEOUT_TICKS));
        return true;
    }

    /** The challenge waiting for {@code leader}, if it is still on time. */
    public Optional<Duel> asked(UUID leader, long now) {
        return duels.stream().filter(d -> d.phase == Phase.ASKED && d.leader.equals(leader) && d.expires >= now).findFirst();
    }

    /** The leader accepts: the duel is on and runs for the full time from now. */
    public Optional<Duel> accept(UUID leader, long now) {
        Optional<Duel> d = asked(leader, now);
        d.ifPresent(x -> {
            x.phase = Phase.ON;
            x.expires = now + TIMEOUT_TICKS;
        });
        return d;
    }

    public Optional<Duel> on(UUID p) {
        return of(p).filter(d -> d.phase == Phase.ON);
    }

    /** {@code loser} would have died: the duel is settled, and the other one wins. Empty when {@code loser} is in no duel that is on. */
    public Optional<Result> defeat(UUID loser) {
        Optional<Duel> d = on(loser);
        d.ifPresent(duels::remove);
        return d.map(x -> new Result(x.other(loser), loser, x.challenger.equals(x.other(loser))));
    }

    /** One of them left, logged out, was killed by someone else or is no longer fit to fight: whatever they were in is called off. */
    public List<Duel> cancelInvolving(UUID player) {
        List<Duel> gone = duels.stream().filter(d -> d.involves(player)).toList();
        duels.removeAll(gone);
        return gone;
    }

    /** What ran out of time. */
    public List<Duel> expire(long now) {
        List<Duel> gone = duels.stream().filter(d -> d.expires < now).toList();
        duels.removeAll(gone);
        return gone;
    }

    public List<Duel> all() {
        return List.copyOf(duels);
    }

    public void clear() {
        duels.clear();
    }
}