package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;

/** Which doppelgänger the compass points at, as pure rules (the positions come from the caller). */
public final class CompassRules {
    private CompassRules() {
    }

    /**
     * The nearest of the villagers within {@code range} blocks of {@code from} and all of the players (a gene player counts
     * wherever they are in the dimension). Empty when there is none.
     */
    public static Optional<Vec3> nearestDoppelganger(Vec3 from, Collection<Vec3> villagers, double range, Collection<Vec3> players) {
        Vec3 best = null;
        double bestSqr = Double.MAX_VALUE;
        double rangeSqr = range * range;
        for (Vec3 v : villagers) {
            double d = v.distanceToSqr(from);
            if (d <= rangeSqr && d < bestSqr) {
                best = v;
                bestSqr = d;
            }
        }
        for (Vec3 p : players) {
            double d = p.distanceToSqr(from);
            if (d < bestSqr) {
                best = p;
                bestSqr = d;
            }
        }
        return Optional.ofNullable(best);
    }
}
