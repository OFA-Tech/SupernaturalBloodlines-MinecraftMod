package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Which boss is the full card. The boss this player hit last, within {@link #HIT_WINDOW} ticks (15 s); else the nearest. To keep the card from flickering between two
 * bosses a switch that is not a hit waits {@link #MIN_HOLD} ticks (2 s) after the last one and needs the other boss to be clearly nearer; a hit on another boss switches
 * at once. Pure: times are ticks of one clock, distances are squared.
 */
public final class BossFocus {
    public static final long HIT_WINDOW = 300;
    public static final long MIN_HOLD = 40;
    /** A boss takes the focus by distance only when its squared distance is under this share of the current one's (about 20 % nearer). */
    public static final double NEARER = 0.64;
    /** The distance of a boss the client has no entity for (out of tracking range): far. */
    public static final double UNKNOWN = 1.0e12;
    public static final long NEVER = Long.MIN_VALUE;

    /** @param lastHit the tick this player last damaged the boss, or {@link #NEVER} */
    public record Candidate(UUID id, double distSq, long lastHit) {
    }

    private BossFocus() {
    }

    /** A tie between two candidates: the current focus keeps it (no flip on the same tick), else the lower id (the same answer every time). */
    private static boolean wins(Candidate a, Candidate b, @Nullable UUID current) {
        if (a.id().equals(current)) {
            return true;
        }
        if (b.id().equals(current)) {
            return false;
        }
        return a.id().compareTo(b.id()) < 0;
    }

    /**
     * @param current the focus until now, or null; @param since the tick it became the focus
     * @return the focus now (the id of one of the candidates), or null with none
     */
    @Nullable
    public static UUID choose(List<Candidate> candidates, @Nullable UUID current, long since, long now) {
        if (candidates.isEmpty()) {
            return null;
        }
        Candidate cur = null;
        Candidate hit = null;
        Candidate nearest = null;
        for (Candidate c : candidates) {
            if (c.id().equals(current)) {
                cur = c;
            }
            if (c.lastHit() != NEVER && now - c.lastHit() <= HIT_WINDOW && (hit == null || c.lastHit() > hit.lastHit() || c.lastHit() == hit.lastHit() && wins(c, hit, current))) {
                hit = c;
            }
            if (nearest == null || c.distSq() < nearest.distSq() || c.distSq() == nearest.distSq() && wins(c, nearest, current)) {
                nearest = c;
            }
        }
        if (cur == null) {
            return (hit != null ? hit : nearest).id();
        }
        if (hit != null) {
            return hit.id(); // the boss hit last, at once: it is the current one already, or the player has just turned to another
        }
        if (now - since < MIN_HOLD || nearest == cur) {
            return cur.id();
        }
        return nearest.distSq() < cur.distSq() * NEARER ? nearest.id() : cur.id();
    }
}
