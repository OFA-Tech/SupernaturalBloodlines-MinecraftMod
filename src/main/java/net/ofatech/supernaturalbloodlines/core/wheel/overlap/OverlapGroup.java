package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import java.util.List;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;

/**
 * One power that several mods give: its members are whole powers (never a mode of a skill), {@code main} is the index of the member that runs and is shown when the player has
 * it. {@code research} is the row of {@code docs/superpowers/research/skill-overlaps.md} the group comes from.
 */
public record OverlapGroup(String id, String research, boolean same, int main, List<PowerRef> members) {
    public OverlapGroup {
        members = List.copyOf(members);
    }

    public PowerRef mainMember() {
        return members.get(main);
    }

    /** The side number of a member, -1 when it is not one. */
    public int sideOf(PowerRef ref) {
        return members.indexOf(ref);
    }
}
