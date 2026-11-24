package net.ofatech.supernaturalbloodlines.parts.species.group.circle;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.group.circle.CirclePlan;
import org.jetbrains.annotations.Nullable;

/**
 * The plan the rite cost hook stages for the rite that is starting, in the thread that starts it: {@code RiteEp}, the grand-rite gate, potency and
 * Mastery's Ultimate read it from here. It exists from {@code RiteCost.begin} until {@code commit} or {@code discard}.
 */
public final class CircleStage {
    /** The anchor's plan for the chalk at {@code chalk}; {@code result} has no shares when the cast is solo. */
    public record Staged(UUID anchor, ResourceKey<Level> dimension, BlockPos chalk, CirclePlan.Result result) {
        /** The circle's size after the drops: the members who pay, 1 for a solo cast. */
        public int size() {
            return Math.max(1, result.shares().size());
        }
    }

    private static final ThreadLocal<Staged> CURRENT = new ThreadLocal<>();

    private CircleStage() {
    }

    public static void set(@Nullable Staged staged) {
        if (staged == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(staged);
        }
    }

    @Nullable
    public static Staged current() {
        return CURRENT.get();
    }

    /** The members who pay, 1 when nothing is staged. */
    public static int size() {
        Staged s = CURRENT.get();
        return s == null ? 1 : s.size();
    }
}