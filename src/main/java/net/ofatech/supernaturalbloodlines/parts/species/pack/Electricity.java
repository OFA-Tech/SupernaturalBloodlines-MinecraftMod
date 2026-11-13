package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.Collection;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Electricity, the werewolves' weakness (and every other supernatural's): a lightning strike on a supernatural or within
 * 3 blocks of one, or a hit from the Taser, gives Shocked for 600 ticks: Slowness I, no natural regeneration, and no
 * Werewolves form actions ({@link FormBlocks}). Registered always: it links no Werewolves class.
 */
public final class Electricity {
    public static final int SHOCK_TICKS = 600;
    public static final double STRIKE_RANGE = 3;

    private static volatile Function<LightningBolt, Collection<LivingEntity>> candidates = Electricity::around;

    private Electricity() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityStruckByLightningEvent e) -> {
            if (!e.getEntity().level().isClientSide && e.getEntity() instanceof LivingEntity struck) {
                shockIfSupernatural(struck);
            }
        });
        // a bolt does nothing to its surroundings in its own tick, so the scan runs once, when it appears
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.getEntity() instanceof LightningBolt bolt && e.getLevel() instanceof ServerLevel) {
                for (LivingEntity near : candidates.apply(bolt)) {
                    if (near.distanceToSqr(bolt) <= STRIKE_RANGE * STRIKE_RANGE) {
                        shockIfSupernatural(near);
                    }
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(Electricity::onHeal);
    }

    public static void shock(LivingEntity target) {
        target.addEffect(new MobEffectInstance(SpeciesRegistries.SHOCKED, SHOCK_TICKS, 0));
    }

    public static void shockIfSupernatural(LivingEntity target) {
        if (Supernatural.is(target)) {
            shock(target);
        }
    }

    private static Collection<LivingEntity> around(LightningBolt bolt) {
        return bolt.level().getEntitiesOfClass(LivingEntity.class, bolt.getBoundingBox().inflate(STRIKE_RANGE));
    }

    /**
     * No natural regeneration while shocked. The event does not say who heals, so the stack is read for {@code FoodData}
     * (only for a shocked victim), as the wolf lichen does; potions, food and other healing are untouched.
     */
    private static void onHeal(LivingHealEvent e) {
        if (e.getEntity().hasEffect(SpeciesRegistries.SHOCKED)
                && StackWalker.getInstance().walk(s -> s.anyMatch(f -> f.getClassName().equals(FoodData.class.getName())))) {
            e.setCanceled(true);
        }
    }

    /** For tests: who stands near a bolt (mock players are not in the level's entity lookup); null resets. */
    @VisibleForTesting
    public static void overrideCandidates(@Nullable Function<LightningBolt, Collection<LivingEntity>> lookup) {
        candidates = lookup != null ? lookup : Electricity::around;
    }
}
