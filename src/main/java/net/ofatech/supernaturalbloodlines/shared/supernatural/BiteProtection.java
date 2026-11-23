package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;

/**
 * Whether something already made a bite give nothing on purpose: a desiccated biter (R33), a vervain-protected victim (R94). Those rules
 * live in other parts and zero the amount of Vampirism's drink event; a skill that adds blood on its own (Predator's Thirst) must not
 * turn that zero back into blood. Each part registers its guard; with none registered nothing is protected.
 */
public final class BiteProtection {
    private static final List<BiPredicate<LivingEntity, LivingEntity>> GUARDS = new CopyOnWriteArrayList<>();

    private BiteProtection() {
    }

    /** @param guard (biter, victim) to true when the bite must stay empty */
    public static void register(BiPredicate<LivingEntity, LivingEntity> guard) {
        GUARDS.add(guard);
    }

    private static final List<Predicate<LivingEntity>> VICTIM_GUARDS = new CopyOnWriteArrayList<>();

    /** @param guard a victim that keeps vampire blood out whoever the vampire is (vervain: a bite and a forced drink alike) */
    public static void registerVictim(Predicate<LivingEntity> guard) {
        VICTIM_GUARDS.add(guard);
    }

    public static boolean victimProtected(LivingEntity victim) {
        for (Predicate<LivingEntity> g : VICTIM_GUARDS) {
            if (g.test(victim)) {
                return true;
            }
        }
        return false;
    }

    public static boolean protectedBite(LivingEntity biter, LivingEntity victim) {
        for (BiPredicate<LivingEntity, LivingEntity> g : GUARDS) {
            if (g.test(biter, victim)) {
                return true;
            }
        }
        return false;
    }
}