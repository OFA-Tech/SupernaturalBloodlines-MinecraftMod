package net.ofatech.supernaturalbloodlines.parts.species.hybrid;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.api.entity.factions.IFactionEntity;
import net.minecraft.core.registries.Registries;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
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

    private static final ResourceLocation WEREWOLF = ResourceLocation.fromNamespaceAndPath("werewolves", "werewolf");
    private static final ResourceLocation VAMPIRE = ResourceLocation.fromNamespaceAndPath("vampirism", "vampire");
    private static final int HALLUCINATION_EVERY = 600;
    private static final int HALLUCINATION_TICKS = 100;

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
        if (isFaction(attacker, WEREWOLF) && isFaction(target, VAMPIRE)) {
            target.addEffect(new MobEffectInstance(SpeciesRegistries.HYBRID_VENOM, DURATION_TICKS));
        }
    }

    private static boolean isFaction(Entity e, ResourceLocation id) {
        IFaction<?> faction = e instanceof Player p ? VampirismAPI.factionPlayerHandler(p).getCurrentFaction()
                : e instanceof IFactionEntity fe ? fe.getFaction() : null;
        return faction != null && faction.getID().equals(id);
    }

    /**
     * One pulse of the effect, {@code remaining} ticks before it runs out. 1 damage that cannot kill (like poison);
     * Nausea II every 600 ticks (the hallucination, plan 7 may swap in its payload); Originals are kept in Weakness II;
     * on the last pulse a non-Original dies of {@code supernaturalbloodlines:venom}, which ignores Resistance and
     * Vampirism's dying state. Creative and spectator players are not touched.
     */
    public static void pulse(LivingEntity target, int remaining) {
        if (!target.isAlive() || target instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return;
        }
        boolean original = target instanceof Player p && OriginalRules.isOriginal(BloodlineAccess.get(p).species());
        if (remaining <= 1) {
            if (!original) {
                kill(target);
            }
            return;
        }
        if (original) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, true, false, true));
        }
        if (target.getHealth() > 1f) {
            target.hurt(source(target), 1f);
        }
        if (remaining % HALLUCINATION_EVERY == 0) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, HALLUCINATION_TICKS, 1));
        }
    }

    private static DamageSource source(LivingEntity target) {
        return new DamageSource(target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE));
    }

    private static void kill(LivingEntity target) {
        target.hurt(source(target), Float.MAX_VALUE);
        if (target.isAlive()) {
            target.kill(); // something (a totem, another mod) kept it alive
        }
    }
}
