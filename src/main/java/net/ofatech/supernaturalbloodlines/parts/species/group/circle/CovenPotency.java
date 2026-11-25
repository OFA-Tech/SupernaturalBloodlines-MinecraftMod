package net.ofatech.supernaturalbloodlines.parts.species.group.circle;

import net.ofatech.supernaturalbloodlines.core.group.circle.Potency;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;

/** What a circle adds to a rite (R254): the staged plan's size through {@link Potency}; a solo cast (or no plan) is 1. */
public final class CovenPotency {
    private CovenPotency() {
    }

    public static double factor() {
        return Potency.of(CircleStage.size(), GroupsConfig.potencyMaxBonus(), GroupsConfig.potencyFalloff());
    }

    public static double scale(double base) {
        return base * factor();
    }
}