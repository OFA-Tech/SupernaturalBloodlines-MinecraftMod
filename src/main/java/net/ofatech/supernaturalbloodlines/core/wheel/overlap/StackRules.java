package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;

/** Whether a side of a group may start now: not while another side is on, not while the shared cooldown runs. Turning the active side off is never a start. */
public final class StackRules {
    private StackRules() {
    }

    public static Optional<LockReason> mayStart(GroupClock clock, String groupId, int side, long now) {
        var active = clock.activeSide(groupId);
        if (active.isPresent() && active.getAsInt() != side) {
            return Optional.of(LockReason.GROUP_ACTIVE);
        }
        if (clock.cooldownLeft(groupId, now) > 0) {
            return Optional.of(LockReason.GROUP_COOLDOWN);
        }
        return Optional.empty();
    }
}
