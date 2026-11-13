package net.ofatech.supernaturalbloodlines.core;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * "Can this creature be compelled": core cannot reference the parts, so each registers its own predicates at init
 * (vervain in artifacts, the hunter's mark in species) and the answers are OR-combined. Players are never compelled.
 */
public final class CompulsionImmunity {
    private static final List<Predicate<LivingEntity>> PREDICATES = new CopyOnWriteArrayList<>();

    private CompulsionImmunity() {
    }

    public static void register(Predicate<LivingEntity> immune) {
        PREDICATES.add(Objects.requireNonNull(immune, "immune"));
    }

    public static boolean isImmune(LivingEntity e) {
        if (e instanceof Player) {
            return true;
        }
        for (Predicate<LivingEntity> p : PREDICATES) {
            if (p.test(e)) {
                return true;
            }
        }
        return false;
    }

    static void clearForTests() {
        PREDICATES.clear();
    }
}
