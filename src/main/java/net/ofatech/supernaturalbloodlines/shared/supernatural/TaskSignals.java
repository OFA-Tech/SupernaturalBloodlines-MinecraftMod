package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.jetbrains.annotations.Nullable;

/**
 * A tiny bus: the parts tell the supernatural part that something happened to a player. Other parts fire
 * {@code (player, signalId, amount)}; the {@code supernatural} part listens. Firing with no listener (the part is off) does nothing.
 * Ids the ladders know: {@code compel}, {@code siphon_power}, {@code siphon_entity} (attr {@code prey}), {@code venom_hit},
 * {@code dying_survived}, {@code forced_shift_survived}, {@code vampire_bite_refused}, {@code sun_ring_bound},
 * {@code mountain_ash_placed}, and {@code rite:<rite id>:<performer|target>} (attr {@code other}) from {@link #rite}.
 */
public final class TaskSignals {
    @FunctionalInterface
    public interface Listener {
        void on(Player player, String id, long amount, Map<String, String> attrs);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private TaskSignals() {
    }

    public static void addListener(Listener l) {
        LISTENERS.add(l);
    }

    public static void fire(Player player, String id, long amount) {
        fire(player, id, amount, Map.of());
    }

    public static void fire(Player player, String id, long amount, Map<String, String> attrs) {
        for (Listener l : LISTENERS) {
            try {
                l.on(player, id, amount, attrs);
            } catch (RuntimeException e) {
                SupernaturalBloodlines.LOGGER.error("Task signal listener failed for {}", id, e);
            }
        }
    }

    /**
     * A rite of ours worked. {@code riteId} is the rite's id ({@code supernaturalbloodlines:rite_of_the_moon}); either player may
     * be null (a rite with no target), and when they are the same player both roles fire. {@code other} says whether the
     * performer and the target are different players.
     */
    public static void rite(@Nullable Player performer, @Nullable Player target, String riteId) {
        Map<String, String> attrs = Map.of("other", String.valueOf(performer != null && target != null && performer != target));
        if (performer != null) {
            fire(performer, "rite:" + riteId + ":performer", 1, attrs);
        }
        if (target != null) {
            fire(target, "rite:" + riteId + ":target", 1, attrs);
        }
    }
}
