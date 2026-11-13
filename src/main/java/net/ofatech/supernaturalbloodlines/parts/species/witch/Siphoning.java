package net.ofatech.supernaturalbloodlines.parts.species.witch;

import net.favouriteless.enchanted.common.enchanted.altar.PowerProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/**
 * R62/R63: a siphoner (and the heretic and tribrid) sneaks with an empty hand on a block that holds altar power, or on a
 * living thing, and takes {@link #GAIN} power into {@code siphonedPower} (cap {@link #CAP}). A block gives power once per
 * {@link #COOLDOWN_TICKS} per player, a creature takes {@link #ENTITY_DAMAGE} magic damage. Rites then spend it first
 * ({@link RiteCost}).
 */
public final class Siphoning {
    public static final int GAIN = 50;
    public static final int CAP = 1000;
    public static final int COOLDOWN_TICKS = 600;
    public static final float ENTITY_DAMAGE = 2f;
    public static final String FULL = "supernaturalbloodlines.siphon.full";
    public static final String COOLING = "supernaturalbloodlines.siphon.cooling";

    private static final SiphonCooldown<GlobalPos> BLOCKS = new SiphonCooldown<>();

    private Siphoning() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Siphoning::onBlock);
        NeoForge.EVENT_BUS.addListener(Siphoning::onEntity);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> BLOCKS.forget(e.getEntity().getUUID()));
    }

    public static boolean canSiphon(Species s) {
        return s == Species.SIPHONER || s == Species.HERETIC || s == Species.TRIBRID;
    }

    /** Does Enchanted give this block altar power (its own entry, or through a tag)? */
    public static boolean hasAltarData(Level level, BlockState state) {
        return PowerProvider.get(level, state.getBlock()) != null
                || state.getTags().anyMatch(tag -> PowerProvider.get(level, tag) != null);
    }

    private static boolean ready(ServerPlayer p) {
        return p.isShiftKeyDown() && p.getMainHandItem().isEmpty() && canSiphon(BloodlineAccess.get(p).species());
    }

    private static void onBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p)
                || !(p.level() instanceof ServerLevel level) || !ready(p) || !hasAltarData(level, level.getBlockState(e.getPos()))) {
            return;
        }
        siphonBlock(p, level, e.getPos());
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void onEntity(PlayerInteractEvent.EntityInteract e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p)
                || !(e.getTarget() instanceof LivingEntity target) || !ready(p)) {
            return;
        }
        siphonEntity(p, target);
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Takes power from the block unless the player is full or the block is cooling down; tells the player why not. */
    public static boolean siphonBlock(ServerPlayer p, ServerLevel level, BlockPos pos) {
        if (full(p)) {
            return false;
        }
        if (!BLOCKS.tryUse(p.getUUID(), GlobalPos.of(level.dimension(), pos.immutable()), level.getGameTime(), COOLDOWN_TICKS)) {
            p.displayClientMessage(Component.translatable(COOLING), true);
            return false;
        }
        gain(p);
        return true;
    }

    /** Hurts the creature (magic) and takes power if it was hurt. */
    public static boolean siphonEntity(ServerPlayer p, LivingEntity target) {
        if (full(p) || target == p || !target.hurt(target.damageSources().magic(), ENTITY_DAMAGE)) {
            return false;
        }
        gain(p);
        return true;
    }

    private static boolean full(ServerPlayer p) {
        if (BloodlineAccess.get(p).siphonedPower() >= CAP) {
            p.displayClientMessage(Component.translatable(FULL), true);
            return true;
        }
        return false;
    }

    private static void gain(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        BloodlineAccess.set(p, s.toBuilder().siphonedPower(Math.min(CAP, s.siphonedPower() + GAIN)).build());
    }
}
