package net.ofatech.supernaturalbloodlines.parts.species.skill;

import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/**
 * What every targeted attack skill (Rip Heart, Neck Snap) asks before it does anything. They all answer the same one refusal, whatever the reason, so a refusal
 * never says what the target is: a creative or spectating player, a target that is in a state nothing can reach, a claim or PvP rule, a pack-mate or a
 * friendly-fire team.
 */
final class TargetChecks {
    private TargetChecks() {
    }

    /** Creative and spectating players are never a target. */
    static boolean untouchable(LivingEntity target) {
        return target instanceof Player p && (p.isCreative() || p.isSpectator());
    }

    /** Already down: ours (dying for the turning, an Original's, snapped), Vampirism's dying state, or a snapped creature. Visible anyway, so a snap is a plain refusal. */
    static boolean alreadyDown(LivingEntity target) {
        return target instanceof ServerPlayer sp && (Downed.isDowned(sp) || inVampirismDying(sp)) || net.ofatech.supernaturalbloodlines.parts.species.item.SnappedEffect.isSnapped(target);
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
     * Asks what a claim or PvP mod would say to this damage without dealing it: the incoming-damage event, cancelled or not. A target that something protects
     * (a duel, an invulnerability) is a refusal; so is one the damage cannot reach.
     */
    static boolean damageWouldLand(LivingEntity target, DamageSource source, float amount) {
        if (target.isInvulnerableTo(source)) {
            return false;
        }
        return !NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(target, new net.neoforged.neoforge.common.damagesource.DamageContainer(source, amount))).isCanceled();
    }

    /** A creature of an Original's root (an NPC Original), or an Original player. */
    static boolean isOriginal(LivingEntity target) {
        if (target instanceof Player p) {
            return OriginalRules.isOriginal(BloodlineAccess.get(p).species());
        }
        return target.hasData(SpeciesRegistries.ORIGINAL_ROOT);
    }

    static boolean isVampireSpeciesPlayer(Player p) {
        return BloodlineRules.isVampireSpecies(BloodlineAccess.get(p).species());
    }
}
