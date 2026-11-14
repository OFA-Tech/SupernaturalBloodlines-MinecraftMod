package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;

/**
 * Whose blood counts as human-like for the "feed" tasks. The species part installs plan 11's {@code isHumanLikeBlood};
 * without it only villagers and traders count.
 */
public final class HumanLikeBlood {
    private static volatile Predicate<LivingEntity> check = e -> e instanceof AbstractVillager;

    private HumanLikeBlood() {
    }

    public static void set(Predicate<LivingEntity> predicate) {
        check = predicate;
    }

    public static boolean test(LivingEntity e) {
        return check.test(e);
    }
}
