package net.ofatech.supernaturalbloodlines.parts.species.witch;

import java.util.UUID;
import net.favouriteless.enchanted.common.enchanted.altar.PowerProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;

/**
 * R62/R63: a siphoner (and the heretic and tribrid) sneaks with an empty hand on a block that holds altar power, or on a
 * living thing, and takes {@link #GAIN} power into {@code siphonedPower} (cap {@link #CAP}). A block gives power once per
 * {@link #COOLDOWN_TICKS} per player and so does a creature; a creature takes {@link #ENTITY_DAMAGE} magic damage dealt
 * by the siphoner (so PvP rules and claims apply). Rites then spend it first ({@link RiteCost}). R65: it resets on death.
 * The click is cancelled on the client too, from what the client knows (sneaking, empty hand, its own synced species), so
 * nothing else reacts to it before the server answers.
 */
public final class Siphoning {
    public static final int GAIN = 50;
    public static final int CAP = 1000;
    public static final int COOLDOWN_TICKS = 600;
    public static final float ENTITY_DAMAGE = 2f;
    public static final String FULL = "supernaturalbloodlines.siphon.full";
    public static final String COOLING = "supernaturalbloodlines.siphon.cooling";

    private static final SiphonCooldown<GlobalPos> BLOCKS = new SiphonCooldown<>();
    private static final SiphonCooldown<UUID> ENTITIES = new SiphonCooldown<>();

    private Siphoning() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Siphoning::onBlock);
        NeoForge.EVENT_BUS.addListener(Siphoning::onEntity);
        NeoForge.EVENT_BUS.addListener(Siphoning::onRespawn);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            BLOCKS.forget(e.getEntity().getUUID());
            ENTITIES.forget(e.getEntity().getUUID());
        });
    }

    public static boolean canSiphon(Species s) {
        return s == Species.SIPHONER || s == Species.HERETIC || s == Species.TRIBRID;
    }

    /** Does Enchanted give this block altar power (its own entry, or through a tag)? False if the data is not there (a client). */
    public static boolean hasAltarData(Level level, BlockState state) {
        try {
            return PowerProvider.get(level, state.getBlock()) != null
                    || state.getTags().anyMatch(tag -> PowerProvider.get(level, tag) != null);
        } catch (RuntimeException unknownToThisSide) {
            return false;
        }
    }

    private static boolean ready(Player p) {
        return p.isShiftKeyDown() && p.getMainHandItem().isEmpty() && canSiphon(BloodlineAccess.get(p).species());
    }

    private static void onBlock(PlayerInteractEvent.RightClickBlock e) {
        Player p = e.getEntity();
        if (e.getHand() != InteractionHand.MAIN_HAND || !ready(p) || !hasAltarData(p.level(), p.level().getBlockState(e.getPos()))) {
            return;
        }
        if (p instanceof ServerPlayer sp && sp.level() instanceof ServerLevel level) {
            siphonBlock(sp, level, e.getPos());
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void onEntity(PlayerInteractEvent.EntityInteract e) {
        Player p = e.getEntity();
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getTarget() instanceof LivingEntity target) || !ready(p)) {
            return;
        }
        if (p instanceof ServerPlayer sp) {
            siphonEntity(sp, target);
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** R65: a death empties the siphoned power (the bloodline state is copied to the new body, then reset here). */
    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!e.isEndConquered() && e.getEntity() instanceof ServerPlayer p) {
            BloodlineAccess.applyEvent(p, BloodlineEvent.DIED, SpeciesContext.of(p, null));
        }
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

    /** Hurts the creature (magic, dealt by the siphoner) and takes power if it was hurt; a creature gives power once per cooldown. */
    public static boolean siphonEntity(ServerPlayer p, LivingEntity target) {
        if (full(p) || target == p) {
            return false;
        }
        long now = p.level().getGameTime();
        if (ENTITIES.isCooling(p.getUUID(), target.getUUID(), now)) {
            p.displayClientMessage(Component.translatable(COOLING), true);
            return false;
        }
        if (!target.hurt(p.damageSources().indirectMagic(p, p), ENTITY_DAMAGE)) {
            return false; // PvP off, a claim, or immunity: no power and no cooldown
        }
        ENTITIES.tryUse(p.getUUID(), target.getUUID(), now, COOLDOWN_TICKS);
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
