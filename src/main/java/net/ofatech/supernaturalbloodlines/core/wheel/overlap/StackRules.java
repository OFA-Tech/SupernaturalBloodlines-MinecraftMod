package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.wheel.CooldownClock;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;

/** Whether a side of a group may start now: not while another side is on, not while the shared cooldown runs. Turning the active side off is never a start. Pure. */
public final class StackRules {
    private StackRules() {
    }

    /** How often the clock is made to agree with what is really on, in ticks of the player. */
    public static final int RECONCILE_EVERY = 10;

    /** Whether the periodic check is due on this tick of the player. */
    public static boolean reconcileDue(int tickCount) {
        return tickCount % RECONCILE_EVERY == 0;
    }

    /** What a side is about to do this tick (it is counted at the end of the tick, so the clock does not show it yet). */
    public record Intent(String side, SideEvent event) {
    }

    public static Optional<LockReason> mayStart(GroupClock clock, String groupId, String side, long now) {
        return mayStart(clock, groupId, side, now, List.of());
    }

    /** @param pending what other sides of the group did this tick: any of it, not only an "on", keeps this side out (two sides must not both start in one tick) */
    public static Optional<LockReason> mayStart(GroupClock clock, String groupId, String side, long now, Collection<Intent> pending) {
        var active = clock.activeSide(groupId);
        if (active.isPresent() && !active.get().equals(side)) {
            return Optional.of(LockReason.GROUP_ACTIVE);
        }
        if (clock.cooldownLeft(groupId, now) > 0) {
            return Optional.of(LockReason.GROUP_COOLDOWN);
        }
        for (Intent other : pending) {
            if (!other.side().equals(side)) {
                return Optional.of(other.event() == SideEvent.FIRED ? LockReason.GROUP_COOLDOWN : LockReason.GROUP_ACTIVE);
            }
        }
        return Optional.empty();
    }

    /** The entry of a power in a group as the wheel shows it: the group's shared cooldown when it ends later than the power's own, and locked while another side is on. */
    public static PowerState overlay(PowerState s, GroupClock clock, String groupId, String side, long now) {
        long end = clock.cooldownEnd(groupId);
        if (end > now && end > s.cooldownEnd()) {
            int left = (int) Math.min(end - now, Integer.MAX_VALUE);
            int total = clock.cooldownTotal(groupId);
            s = s.withCooldown(new CooldownClock.Reading(end, Math.max(total, left)));
        }
        if (s.lock() == LockReason.NONE && mayStart(clock, groupId, side, now).filter(r -> r == LockReason.GROUP_ACTIVE).isPresent()) {
            s = s.withLock(LockReason.GROUP_ACTIVE);
        }
        return s;
    }
}
