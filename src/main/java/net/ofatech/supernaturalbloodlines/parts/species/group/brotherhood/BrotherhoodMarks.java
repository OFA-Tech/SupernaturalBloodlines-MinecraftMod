package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.Marks;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.MarkTracker;

/** The mark as the brotherhood reads it: a Hunter of the Five's kills (capped), nobody else's; and the last one seen of each member, for those who are away. */
public final class BrotherhoodMarks {
    private BrotherhoodMarks() {
    }

    public static int of(BloodlineState s) {
        return Marks.of(s.species(), s.markKills(), MarkTracker.CAP);
    }

    public static int of(ServerPlayer p) {
        return of(BloodlineAccess.get(p));
    }

    /** The mark recorded when the member was last seen; 0 when none was. */
    public static int recorded(ServerLevel level, UUID id) {
        return GroupIndex.get(level).markOf(id).orElse(0);
    }

    /** Writes the mark of a member of a brotherhood (a group of another type is not recorded: its rows sleep). */
    public static void record(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (BrotherhoodScope.of(level, p.getUUID()) != null) {
            GroupIndex.get(level).setMark(p.getUUID(), of(p));
        }
    }
}