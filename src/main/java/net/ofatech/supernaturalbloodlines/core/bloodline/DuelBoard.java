package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Leadership duels (plan 14): a co-Alpha challenges the leader, the leader accepts, and they fight to the first defeat. This is
 * only the bookkeeping, with no game classes: who has asked whom, which duel is on, when it runs out, who may not ask again yet,
 * and who is guarded from whom for a moment after a defeat. Nobody is in more than one challenge or duel at a time.
 */
public final class DuelBoard {
    /** A challenge waits, and a duel lasts, this long. */
    public static final long TIMEOUT_TICKS = 2 * 60 * 20;
    /** After a duel was settled the same two cannot fight another for this long, whoever challenges. */
    public static final long PAIR_COOLDOWN_TICKS = 10 * 60 * 20;
    /** A challenger whose challenge ran out (or was declined) cannot ask the same leader again for this long. */
    public static final long RETRY_COOLDOWN_TICKS = 5 * 60 * 20;
    /** After a defeat the winner cannot hurt the loser for this long (instead of a totem's protection). */
    public static final long GUARD_TICKS = 5 * 20;

    public enum Phase { ASKED, ON }

    /** Why a challenge or a duel was called off, for the message the two of them get. */
    public enum Reason { TIMEOUT, LOGOUT, KILLED, PACK, FAR, DECLINED, DIED }

    /** Combat credit from a hit lasts this long: a death by the world counts as the opponent's win only within it. */
    public static final long COMBAT_CREDIT_TICKS = 10 * 20;

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

    /** Why a challenge was not made. */
    public enum Refusal { BUSY, PAIR_COOLDOWN, RETRY_COOLDOWN }

    private record Guard(UUID winner, long until) {
    }

    private final List<Duel> duels = new ArrayList<>();
    private final Map<String, Long> pairCooldown = new HashMap<>();
    private final Map<String, Long> retryCooldown = new HashMap<>();
    private final Map<UUID, Guard> guards = new HashMap<>();
    // challenges that ran out while somebody was asking for a new one: PackDuel.check still tells their two players
    private final List<Duel> expiredBacklog = new ArrayList<>();

    private record Hit(UUID attacker, long at) {
    }

    // the last blow each duelist took from their opponent while the duel was on (the vanilla "last attacker" is forgotten after 5 seconds)
    private final Map<UUID, Hit> hits = new HashMap<>();

    private static String pair(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    private static String ordered(UUID challenger, UUID leader) {
        return challenger + ">" + leader;
    }

    private Optional<Duel> of(UUID p) {
        return duels.stream().filter(d -> d.involves(p)).findFirst();
    }

    /** @return empty when the challenge was made, otherwise why not */
    public Optional<Refusal> challenge(UUID challenger, UUID leader, long now) {
        // one that ran out and nobody has swept yet is gone: it must not keep the leader busy
        expiredBacklog.addAll(sweep(now));
        if (challenger.equals(leader) || of(challenger).isPresent() || of(leader).isPresent()) {
            return Optional.of(Refusal.BUSY);
        }
        if (pairCooldown.getOrDefault(pair(challenger, leader), Long.MIN_VALUE) > now) {
            return Optional.of(Refusal.PAIR_COOLDOWN);
        }
        if (retryCooldown.getOrDefault(ordered(challenger, leader), Long.MIN_VALUE) > now) {
            return Optional.of(Refusal.RETRY_COOLDOWN);
        }
        duels.add(new Duel(challenger, leader, now + TIMEOUT_TICKS));
        return Optional.empty();
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

    /** The leader says no: the challenge is gone and the challenger waits before asking again. */
    public Optional<Duel> decline(UUID leader, long now) {
        Optional<Duel> d = asked(leader, now);
        d.ifPresent(x -> {
            duels.remove(x);
            retryCooldown.put(ordered(x.challenger, x.leader), now + RETRY_COOLDOWN_TICKS);
        });
        return d;
    }

    public Optional<Duel> on(UUID p) {
        return of(p).filter(d -> d.phase == Phase.ON);
    }

    /**
     * {@code loser} would have died: the duel is settled, the other one wins, the two cannot fight again for a while and the
     * loser is guarded from the winner for a moment. Empty when {@code loser} is in no duel that is on.
     */
    public Optional<Result> defeat(UUID loser, long now) {
        Optional<Duel> d = on(loser);
        d.ifPresent(x -> {
            duels.remove(x);
            forget(x);
            pairCooldown.put(pair(x.challenger, x.leader), now + PAIR_COOLDOWN_TICKS);
            guards.put(loser, new Guard(x.other(loser), now + GUARD_TICKS));
        });
        return d.map(x -> new Result(x.other(loser), loser, x.challenger.equals(x.other(loser))));
    }

    /** A blow from {@code attacker} on {@code victim}: kept (for the combat credit of what follows) only if they are each other's opponent in a duel that is on. */
    public void recordHit(UUID victim, UUID attacker, long now) {
        on(victim).filter(d -> d.other(victim).equals(attacker)).ifPresent(d -> hits.put(victim, new Hit(attacker, now)));
    }

    /** Whether {@code attacker} hit {@code victim} within the last {@link #COMBAT_CREDIT_TICKS}, while this duel was on. */
    public boolean hasCredit(UUID victim, UUID attacker, long now) {
        Hit h = hits.get(victim);
        return h != null && h.attacker.equals(attacker) && now - h.at <= COMBAT_CREDIT_TICKS;
    }

    /** The two cannot duel again for {@link #PAIR_COOLDOWN_TICKS}: used when one of them walked out of a duel that was on. */
    public void bar(UUID a, UUID b, long now) {
        pairCooldown.put(pair(a, b), now + PAIR_COOLDOWN_TICKS);
    }

    /** Whether a hit from {@code attacker} on {@code victim} is stopped: the victim just lost to them. */
    public boolean guarded(UUID victim, UUID attacker, long now) {
        Guard g = guards.get(victim);
        if (g == null) {
            return false;
        }
        if (g.until <= now) {
            guards.remove(victim);
            return false;
        }
        return g.winner.equals(attacker);
    }

    /** One of them left, logged out, was killed by someone else or is no longer fit to fight: whatever they were in is called off. */
    public List<Duel> cancelInvolving(UUID player) {
        List<Duel> gone = duels.stream().filter(d -> d.involves(player)).toList();
        duels.removeAll(gone);
        gone.forEach(this::forget);
        return gone;
    }

    /**
     * What ran out of time. A challenge nobody answered makes its challenger wait before asking the same leader again, and a duel
     * that was on and ran out bars the same two for as long as a settled one (waiting it out is no way to save a life).
     */
    public List<Duel> expire(long now) {
        List<Duel> gone = new ArrayList<>(expiredBacklog);
        expiredBacklog.clear();
        gone.addAll(sweep(now));
        return gone;
    }

    private void forget(Duel d) {
        hits.remove(d.challenger);
        hits.remove(d.leader);
    }

    private List<Duel> sweep(long now) {
        List<Duel> gone = duels.stream().filter(d -> d.expires < now).toList();
        duels.removeAll(gone);
        gone.forEach(this::forget);
        for (Duel d : gone) {
            if (d.phase == Phase.ASKED) {
                retryCooldown.put(ordered(d.challenger, d.leader), now + RETRY_COOLDOWN_TICKS);
            } else {
                pairCooldown.put(pair(d.challenger, d.leader), now + PAIR_COOLDOWN_TICKS);
            }
        }
        return gone;
    }

    /** Ticks until {@code challenger} may ask {@code leader} again (the longer of the pair and the retry wait); 0 when they may. */
    public long cooldownLeft(UUID challenger, UUID leader, long now) {
        long pairEnd = pairCooldown.getOrDefault(pair(challenger, leader), Long.MIN_VALUE);
        long retryEnd = retryCooldown.getOrDefault(ordered(challenger, leader), Long.MIN_VALUE);
        return Math.max(0, Math.max(pairEnd, retryEnd) - now);
    }
    public List<Duel> all() {
        return List.copyOf(duels);
    }

    public void clear() {
        duels.clear();
        pairCooldown.clear();
        retryCooldown.clear();
        guards.clear();
        hits.clear();
        expiredBacklog.clear();
    }
}
