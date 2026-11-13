package net.ofatech.supernaturalbloodlines.parts.artifacts;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/**
 * R33: a hit with the White Oak Ash Dagger leaves it in an Original ({@code desiccated}, the stack kept in the
 * {@code lodged_dagger} attachment) until another player pulls it, the effect runs out or is removed, or the Original
 * dies. Anyone supernatural who hits with it takes {@link #BACKLASH}.
 */
public final class AshDagger {
    /** Heavy: half of a player's health. */
    public static final float BACKLASH = 10.0f;

    private AshDagger() {
    }

    public static void register() {
        // LOWEST: after Original protection and other cancels, and a cancelled removal is not a removal
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDamageEvent.Post.class, AshDagger::onHit);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, PlayerInteractEvent.EntityInteract.class, AshDagger::onPull);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, MobEffectEvent.Expired.class, e -> onEnded(e.getEntity(), e.getEffectInstance()));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, MobEffectEvent.Remove.class, e -> onEnded(e.getEntity(), e.getEffectInstance()));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, e -> release(e.getEntity()));

        // the desiccated can do nothing; HIGHEST, so nothing else sees the action first
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (AttackEntityEvent e) -> deny(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerInteractEvent.RightClickItem e) -> deny(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerInteractEvent.RightClickBlock e) -> deny(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerInteractEvent.LeftClickBlock e) -> deny(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerInteractEvent.EntityInteract e) -> deny(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerInteractEvent.EntityInteractSpecific e) -> deny(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (BlockEvent.BreakEvent e) -> deny(e.getPlayer(), e));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (LivingEntityUseItemEvent.Start e) -> deny(e.getEntity(), e));
    }

    /** The dagger stuck in {@code p}, or empty. */
    public static ItemStack lodged(Player p) {
        return p.getData(ArtifactItems.LODGED_DAGGER.get());
    }

    public static boolean isDesiccated(LivingEntity e) {
        return e.hasEffect(ArtifactItems.DESICCATED);
    }

    private static void deny(Entity who, ICancellableEvent event) {
        if (who instanceof LivingEntity e && isDesiccated(e)) {
            event.setCanceled(true);
        }
    }

    private static boolean isOriginal(ServerPlayer p) {
        Species s = BloodlineAccess.get(p).species();
        return s == Species.ORIGINAL || s == Species.ORIGINAL_HYBRID;
    }

    private static boolean isMelee(DamageSource s) {
        return s.is(DamageTypes.PLAYER_ATTACK) || s.is(DamageTypes.MOB_ATTACK) || s.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
    }

    // ---- the hit ----

    private static void onHit(LivingDamageEvent.Post e) {
        DamageSource source = e.getSource();
        if (!isMelee(source) || !(source.getDirectEntity() instanceof LivingEntity wielder) || wielder != source.getEntity()
                || !wielder.getMainHandItem().is(ArtifactItems.ASH_DAGGER.get())) {
            return;
        }
        if (e.getEntity() instanceof ServerPlayer victim && isOriginal(victim) && !isDesiccated(victim)) {
            lodge(victim, wielder);
        }
        if (Supernatural.is(wielder)) {
            // attributed to the wielder as the direct entity only: a player cannot hurt themselves as the "causing" entity when PvP is off
            wielder.hurt(new DamageSource(wielder.damageSources().magic().typeHolder(), wielder, null), BACKLASH);
        }
    }

    private static void lodge(ServerPlayer victim, LivingEntity wielder) {
        ItemStack dagger = wielder.getMainHandItem().copyWithCount(1);
        if (!(wielder instanceof Player p && p.hasInfiniteMaterials())) {
            wielder.getMainHandItem().shrink(1);
        }
        victim.setData(ArtifactItems.LODGED_DAGGER.get(), dagger);
        victim.addEffect(new MobEffectInstance(ArtifactItems.DESICCATED, ArtifactItems.DESICCATED_TICKS, 0, false, true, true));
    }

    // ---- the end ----

    private static void onPull(PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide() || !(e.getTarget() instanceof ServerPlayer victim) || e.getEntity() == victim
                || !isDesiccated(victim)) {
            return;
        }
        release(victim);
        victim.removeEffect(ArtifactItems.DESICCATED);
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }

    private static void onEnded(LivingEntity e, MobEffectInstance instance) {
        if (instance != null && instance.is(ArtifactItems.DESICCATED)) {
            release(e);
        }
    }

    /** Drops the lodged dagger where {@code e} stands. Safe to call twice: the second finds nothing. */
    private static void release(LivingEntity e) {
        if (!(e instanceof ServerPlayer p) || lodged(p).isEmpty()) {
            return;
        }
        ItemStack dagger = lodged(p);
        p.setData(ArtifactItems.LODGED_DAGGER.get(), ItemStack.EMPTY);
        p.level().addFreshEntity(new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), dagger));
    }
}
