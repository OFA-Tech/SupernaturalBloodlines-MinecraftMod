package net.ofatech.supernaturalbloodlines.parts.species.skill;

import de.teamlapen.vampirism.api.VampirismAPI;
import java.lang.reflect.Field;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.death.DamageProbe;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.SnappedEffect;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/**
 * What every targeted attack skill (Rip Heart, Neck Snap) asks before it does anything. They all answer the same one refusal, whatever the reason, so a refusal
 * never says what the target is: a creative or spectating player, a player under respawn protection, a target already down (or still in the grace after a snap),
 * a claim or PvP rule, a duel, a pack-mate or an allied pack.
 */
final class TargetChecks {
    /** The tiny amount a claim or PvP mod is asked about; nothing of it is ever dealt. */
    static final float PROBE = 0.01F;
    /** How far the crosshair may be from the target (the cosine of the angle): the client names the target, the server checks it is where they look. */
    static final double MIN_AIM = 0.5;

    private static final Field SPAWN_INVULNERABLE = spawnField();

    private TargetChecks() {
    }

    private static Field spawnField() {
        try {
            Field f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** Creative and spectating players are never a target, nor is a player under respawn protection (nothing would land, which would tell an Original from anyone else). */
    static boolean untouchable(LivingEntity target) {
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return true;
        }
        if (target instanceof ServerPlayer sp && SPAWN_INVULNERABLE != null) {
            try {
                return SPAWN_INVULNERABLE.getInt(sp) > 0;
            } catch (IllegalAccessException e) {
                return false;
            }
        }
        return false;
    }

    /** The target is in front of the user (within 60 degrees of where they look), whoever the client says it is. */
    static boolean inView(ServerPlayer user, LivingEntity target) {
        Vec3 to = target.getEyePosition().subtract(user.getEyePosition());
        return to.lengthSqr() < 1.0E-6 || user.getLookAngle().dot(to.normalize()) > MIN_AIM;
    }

    /**
     * Already down: ours (dying for the turning, an Original's, snapped), Vampirism's dying state, a snapped creature, or still in the grace a snap leaves
     * behind. Visible anyway, so a snap is a plain refusal for all of them.
     */
    static boolean alreadyDown(LivingEntity target) {
        return target instanceof ServerPlayer sp && (Downed.isDowned(sp) || inVampirismDying(sp)) || SnappedEffect.isSnapped(target) || SnappedEffect.inGrace(target);
    }

    static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> SNAP_IMMUNE = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.MODID, "snap_immune"));

    /** A boss: any entity type of the bosses part (the tag), or the boss of the Alpha Trial. Neither Neck Snap nor Rip Heart moves one. */
    static boolean isBoss(LivingEntity target) {
        return target.getType().is(SNAP_IMMUNE) || net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrialBoss.isBoss(target);
    }

    /** Whether Vampirism's own dying state holds this player: no damage reaches it, so a heart-rip finishes it by the Original rules' kill. */
    static boolean inVampirismDying(LivingEntity target) {
        return target instanceof ServerPlayer sp && VampirismAPI.vampirePlayer(sp).isDBNO();
    }

    /**
     * PvP, friendly fire, a pack-mate, and the attack event claim and PvP mods answer to. False is the one refusal. (A pack-mate and a party are allies: an
     * Open Parties and Claims party is protected by its own claim mod through the same event; this mod has no ally list of its own for it.)
     */
    static boolean mayAttack(ServerPlayer user, LivingEntity target) {
        if (target instanceof ServerPlayer other) {
            if (!user.canHarmPlayer(other) || packMates(user, other)) {
                return false;
            }
        }
        return CommonHooks.onPlayerAttackTarget(user, target);
    }

    private static boolean packMates(ServerPlayer a, ServerPlayer b) {
        if (!ModList.get().isLoaded("werewolves")) {
            return false;
        }
        PackIndex packs = PackIndex.get(a.serverLevel());
        return packs.inPack(a.getUUID()) && packs.inPack(b.getUUID()) && packs.leaderOf(a.getUUID()).equals(packs.leaderOf(b.getUUID()));
    }

    /**
     * Asks what a claim, a duel or a PvP mod would say to this damage without dealing it: the incoming-damage event, cancelled or not, posted while
     * {@link DamageProbe} is active so none of our own listeners counts it. A target that something protects (a duel, an invulnerability) is a refusal; so
     * is one the damage cannot reach. Other mods see a pre-damage event of {@code amount} that never lands.
     */
    static boolean damageWouldLand(LivingEntity target, DamageSource source, float amount) {
        if (target.isInvulnerableTo(source)) {
            return false;
        }
        return !DamageProbe.run(() -> NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(target,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(source, amount))).isCanceled());
    }

    /** A creature of an Original's root (an NPC Original), or an Original player. */
    static boolean isOriginal(LivingEntity target) {
        if (target instanceof Player p) {
            return OriginalRules.isOriginal(BloodlineAccess.get(p).species());
        }
        return target.hasData(SpeciesRegistries.ORIGINAL_ROOT);
    }
}
