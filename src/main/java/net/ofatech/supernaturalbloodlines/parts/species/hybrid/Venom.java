package net.ofatech.supernaturalbloodlines.parts.species.hybrid;

import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;
import net.ofatech.supernaturalbloodlines.shared.supernatural.TaskSignals;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.network.HallucinationPayload;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.VenomStages;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

/**
 * Werewolf venom (R42v). A melee hit from anyone in the werewolf faction (a player or a Werewolves mob; plain
 * werewolves, hybrids, Original hybrids and tribrids alike) on a Vampirism vampire (a player or a mob) gives the
 * {@code hybrid_venom} effect for {@link #DURATION_TICKS}. The effect pulses every 40 ticks ({@link #pulse}); the last
 * pulse kills a non-Original. Factions are matched by id, so this links no Werewolves class.
 * <p>
 * Deferred: the Werewolves bite action (no hook, see docs/parts/species.md); a bite is also a melee hit when it comes
 * from the werewolf's attack.
 */
public final class Venom {
    public static final int DURATION_TICKS = 12000;
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "venom"));

    private static final int HALLUCINATION_EVERY = 600;
    private static final int HALLUCINATION_TICKS = 100;
    /** The effect pulses this often (see {@code HybridVenomEffect}). */
    public static final int PULSE_TICKS = 40;
    private static final int SICK_EFFECT_TICKS = 60;

    private Venom() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Venom::onDamaged);
    }

    private static void onDamaged(LivingDamageEvent.Post e) {
        DamageSource source = e.getSource();
        Entity attacker = source.getEntity();
        LivingEntity target = e.getEntity();
        // melee only: a projectile or a potion has a different direct entity
        if (e.getNewDamage() <= 0 || attacker == null || attacker != source.getDirectEntity() || source.is(DamageTypes.THORNS)
                || target.hasEffect(SpeciesRegistries.HYBRID_VENOM)) {
            return;
        }
        if (VampirismSupernatural.isWerewolf(attacker) && VampirismSupernatural.isVampire(target)) {
            target.addEffect(new MobEffectInstance(SpeciesRegistries.HYBRID_VENOM, DURATION_TICKS));
            if (attacker instanceof Player p) {
                int slow = (int) LadderQuery.value(p, "S-HYB-2", "slowness_ticks", 0);
                if (slow > 0) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 0));
                }
                TaskSignals.fire(p, "venom_hit", 1);
            }
        }
    }

    /**
     * One pulse of the effect, {@code remaining} ticks before it runs out, by stage ({@link VenomStages}, R206). MILD: 1 damage that cannot kill (like
     * poison) and nothing else. SICK (from {@code venom_sick_after_ticks} on): the same damage, Nausea and Slowness I kept up, a hallucination every 600 ticks
     * ({@link HallucinationPayload}; Nausea II for a mob or a client that cannot receive it), and one line the first time. Someone who cannot act (down for
     * the turning or a snap, or in Vampirism's dying state) is spared the sickness: they are lying there already. DEATH: a non-Original dies of
     * {@code supernaturalbloodlines:venom}, which ignores Resistance, even down or in Vampirism's dying state. Originals are kept in Weakness II, with no stages.
     * Creative and spectator players are not touched.
     */
    public static void pulse(LivingEntity target, int remaining) {
        if (!target.isAlive() || target instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return;
        }
        boolean original = target instanceof Player p && OriginalRules.isOriginal(BloodlineAccess.get(p).species());
        int sickAfter = SpeciesConfig.venomSickAfterTicks();
        VenomStages.Stage stage = VenomStages.of(remaining, DURATION_TICKS, sickAfter);
        if (stage == VenomStages.Stage.DEATH) {
            if (!original) {
                kill(target);
            }
            return;
        }
        if (original) { // R42v: weakened only, no tick damage, no stages
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, true, false, true));
            return;
        }
        if (target.getHealth() > 1f) {
            target.hurt(source(target), 1f);
        }
        if (stage == VenomStages.Stage.SICK && !(target instanceof Player p && Downed.cannotAct(p))) {
            sicken(target, remaining, VenomStages.justFellSick(remaining, DURATION_TICKS, sickAfter, PULSE_TICKS));
        }
    }

    private static void sicken(LivingEntity target, int remaining, boolean first) {
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, SICK_EFFECT_TICKS, 0, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SICK_EFFECT_TICKS, 0, false, true, true));
        if (first && target instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("supernaturalbloodlines.venom.sick"), true);
        }
        if (remaining % HALLUCINATION_EVERY == 0
                && !(target instanceof ServerPlayer sp && HallucinationPayload.send(sp, HallucinationPayload.Kind.random(sp.getRandom())))) {
            // a mob, or a player whose client cannot take the payload
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, HALLUCINATION_TICKS, 1));
        }
    }

    private static DamageSource source(LivingEntity target) {
        return new DamageSource(target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE));
    }

    private static void kill(LivingEntity target) {
        target.hurt(source(target), Float.MAX_VALUE);
        if (target.isAlive()) {
            target.kill(); // something (a totem, another mod) kept it alive; also a player down or in Vampirism's dying state, which the blow above cannot reach
        }
    }
}
