package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.DuelBoard;
import net.ofatech.supernaturalbloodlines.core.bloodline.DuelBoard.Reason;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import org.jetbrains.annotations.Nullable;

/**
 * The leadership duel (plan 14, owner): a second Alpha challenges the leader, the leader accepts, and they fight to the first
 * defeat. A defeat is any death of one of them while the duel is on (by the other one, or by lava, a fall, a mob): it is turned
 * into 1 HP, and the other one wins and leads. Nobody loses the spark and it is no steal. For 5 seconds the winner cannot hurt
 * the loser (a guard against that one hit, not a totem). The same two cannot duel again for 10 minutes, and a challenger whose
 * challenge ran out or was declined cannot ask the same leader for 5. It is called off by a timeout of 2 minutes, by either of
 * them logging out, leaving the pack, or being in another dimension, or by a third player killing one of them (that death stands).
 */
public final class PackDuel {
    private static final String KEY = "supernaturalbloodlines.pack.duel.";
    /** A challenge or an acceptance needs both in one dimension and this close. */
    public static final double RANGE = 64;
    private static final DuelBoard BOARD = new DuelBoard();
    private static final int CHECK_EVERY = 20;

    private PackDuel() {
    }

    public static DuelBoard board() {
        return BOARD;
    }

    public static void register() {
        // before the death is handled by anything that would act on it (a totem is asked first, and a duel is not a death)
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, LivingDeathEvent.class, PackDuel::onDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingIncomingDamageEvent.class, PackDuel::onDamage);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                forfeit(p.server, p);
            }
        });
        // a world that was stopped leaves no half-fought duel behind (the board lives in memory)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> onStopped());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % CHECK_EVERY == 0) {
                check(e.getServer());
            }
        });
    }

    /** @return the error to show when {@code a} and {@code b} are too far apart or in other dimensions, or null when they are close */
    @Nullable
    private static Component apart(ServerPlayer a, ServerPlayer b) {
        if (a.level().dimension() != b.level().dimension()) {
            return PackCommand.error("duel_dimension");
        }
        return a.distanceTo(b) > RANGE ? PackCommand.error("duel_far") : null;
    }

    /** @return the error to show, or null when the leader was asked */
    @Nullable
    public static Component challenge(ServerPlayer challenger, String word) {
        PackIndex index = PackIndex.get(challenger.serverLevel());
        UUID leaderId = index.alphaOf(challenger.getUUID()).orElse(null);
        if (leaderId == null || !PackAccess.isAlpha(PackAccess.wolf(challenger).rank()) || !VampirismSupernatural.isWerewolf(challenger)) {
            return PackCommand.error("duel_not_coalpha");
        }
        String name = PackAccess.nameOf(challenger.server, leaderId);
        if (!leaderId.toString().equalsIgnoreCase(word) && (name == null || !name.equalsIgnoreCase(word))) {
            return PackCommand.error("duel_not_leader", word);
        }
        ServerPlayer leader = SireCall.player(challenger.server, leaderId);
        if (leader == null || !SparkHandler.hasSpark(challenger.server, leaderId)) {
            return PackCommand.error("duel_leader_away");
        }
        Component far = apart(challenger, leader);
        if (far != null) {
            return far;
        }
        long now = challenger.serverLevel().getGameTime();
        var refused = BOARD.challenge(challenger.getUUID(), leaderId, now);
        if (refused.isPresent()) {
            long minutes = Math.max(1, (BOARD.cooldownLeft(challenger.getUUID(), leaderId, now) + 1199) / 1200);
            return switch (refused.get()) {
                case BUSY -> PackCommand.error("duel_busy");
                case PAIR_COOLDOWN -> PackCommand.error("duel_cooldown", minutes);
                case RETRY_COOLDOWN -> PackCommand.error("duel_retry", minutes);
            };
        }
        Component accept = Component.translatable(KEY + "accept").withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pack challenge accept"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(KEY + "accept_hover", PackCommand.name(challenger)))));
        leader.sendSystemMessage(Component.translatable(KEY + "asked", PackCommand.name(challenger)).append(" ").append(accept));
        challenger.sendSystemMessage(Component.translatable(KEY + "sent", PackCommand.name(leader)));
        return null;
    }

    /** @return the error to show, or null when the duel is on */
    @Nullable
    public static Component accept(ServerPlayer leader) {
        PackIndex index = PackIndex.get(leader.serverLevel());
        long now = leader.serverLevel().getGameTime();
        DuelBoard.Duel asked = BOARD.asked(leader.getUUID(), now).orElse(null);
        if (asked == null) {
            return PackCommand.error("duel_none");
        }
        ServerPlayer challenger = SireCall.player(leader.server, asked.challenger());
        // still the same pack: the leader leads it and the challenger is still a second Alpha in it
        if (challenger == null || !index.isLeader(leader.getUUID()) || !leader.getUUID().equals(index.alphaOf(asked.challenger()).orElse(null))
                || !PackAccess.isAlpha(PackAccess.wolf(challenger).rank()) || !VampirismSupernatural.isWerewolf(challenger)) {
            BOARD.cancelInvolving(leader.getUUID());
            if (challenger != null) {
                challenger.sendSystemMessage(Component.translatable(KEY + "off." + Reason.PACK.name().toLowerCase(java.util.Locale.ROOT)));
            }
            return PackCommand.error("duel_gone");
        }
        Component far = apart(leader, challenger);
        if (far != null) {
            return far;
        }
        BOARD.accept(leader.getUUID(), now);
        Component on = Component.translatable(KEY + "on", PackCommand.name(challenger), PackCommand.name(leader));
        challenger.sendSystemMessage(on);
        leader.sendSystemMessage(on);
        return null;
    }

    /** @return the error to show, or null when the challenge was turned down */
    @Nullable
    public static Component decline(ServerPlayer leader) {
        DuelBoard.Duel d = BOARD.decline(leader.getUUID(), leader.serverLevel().getGameTime()).orElse(null);
        if (d == null) {
            return PackCommand.error("duel_none");
        }
        ServerPlayer challenger = SireCall.player(leader.server, d.challenger());
        if (challenger != null) {
            challenger.sendSystemMessage(Component.translatable(KEY + "off.declined", PackCommand.name(leader)));
        }
        leader.sendSystemMessage(Component.translatable(KEY + "declined_you"));
        return null;
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        Entity by = e.getSource().getEntity();
        if (by != null && e.getEntity() instanceof ServerPlayer hurt) {
            BOARD.recordHit(hurt.getUUID(), by.getUUID(), hurt.level().getGameTime());
        }
        if (by != null && e.getEntity() instanceof ServerPlayer loser && BOARD.guarded(loser.getUUID(), by.getUUID(), loser.level().getGameTime())) {
            e.setCanceled(true);
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer loser) || loser.level().isClientSide) {
            return;
        }
        DuelBoard.Duel duel = BOARD.on(loser.getUUID()).orElse(null);
        if (duel == null) {
            callOff(loser.server, loser.getUUID(), Reason.KILLED); // a challenge nobody accepted yet, or no duel at all
            return;
        }
        Entity by = e.getSource().getEntity();
        UUID opponent = duel.other(loser.getUUID());
        // the void takes everybody: it is no defeat to be saved from, whoever pushed
        if (e.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD) || loser.getY() < loser.level().getMinBuildHeight()) {
            callOff(loser.server, loser.getUUID(), Reason.DIED);
            return;
        }
        boolean byOpponent = by instanceof Player other && other.getUUID().equals(opponent);
        if (by instanceof Player && !byOpponent) {
            callOff(loser.server, loser.getUUID(), Reason.KILLED); // a third player killed them: the duel is off and the death stands
            return;
        }
        // lava, a fall, a mob, a pet: it is the opponent's win only if they hit this one in the last 10 seconds. Otherwise it is no
        // defeat but a death (or a way out of a duel that was going badly), so the duel is off and the death stands
        if (!byOpponent && !fought(loser, opponent)) {
            callOff(loser.server, loser.getUUID(), Reason.DIED);
            return;
        }
        // the first defeat: no death, one heart left
        e.setCanceled(true);
        loser.setHealth(1f);
        loser.clearFire();
        loser.fallDistance = 0;
        if (!byOpponent) {
            // the world was about to finish them: 5 seconds of Fire Resistance, and out of the lava if they stand in it, or the next
            // tick would be the death the duel just turned into a defeat
            loser.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100));
            // only out of lava, and only to a spot in line of sight (no teleport through a wall, none for somebody stuck in one)
            if (loser.isInLava()) {
                BlockPos safe = safeSpot(loser.serverLevel(), loser.blockPosition(), loser);
                if (safe != null) {
                    loser.teleportTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
                }
            }
        }
        DuelBoard.Result r = BOARD.defeat(loser.getUUID(), loser.level().getGameTime()).orElse(null);
        if (r != null) {
            settle(loser.server, r);
        }
    }

    /** The opponent hit {@code loser} in the last 10 seconds of this duel (the board keeps the tick of the blow: vanilla forgets its last attacker after 5). */
    static boolean fought(ServerPlayer loser, UUID opponent) {
        return BOARD.hasCredit(loser.getUUID(), opponent, loser.level().getGameTime());
    }

    /**
     * The nearest block to stand on within 4 blocks of {@code from}: two free blocks (no fluid) over a solid one, and in line of
     * sight from the eyes of {@code viewer} (a ray that hits no block on the way). Null when there is none, and then the Fire
     * Resistance is all the loser gets.
     */
    @Nullable
    public static BlockPos safeSpot(ServerLevel level, BlockPos from, Entity viewer) {
        for (int radius = 0; radius <= 4; radius++) {
            for (int dy = 0; dy <= 4; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        BlockPos feet = from.offset(dx, dy, dz);
                        BlockPos below = feet.below();
                        if (free(level, feet) && free(level, feet.above()) && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getFluidState(below).isEmpty()
                                && level.clip(new ClipContext(viewer.getEyePosition(), net.minecraft.world.phys.Vec3.atCenterOf(feet), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer)).getType()
                                == net.minecraft.world.phys.HitResult.Type.MISS) {
                            return feet;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }

    /**
     * {@code leaver} logs out. A duel that is on is lost by them, at once (the winner is told, and leads if they were the challenger);
     * a challenge nobody accepted yet is just called off.
     */
    public static void forfeit(MinecraftServer server, ServerPlayer leaver) {
        if (BOARD.on(leaver.getUUID()).isEmpty()) {
            callOff(server, leaver.getUUID(), Reason.LOGOUT);
            return;
        }
        DuelBoard.Result r = BOARD.defeat(leaver.getUUID(), leaver.level().getGameTime()).orElse(null);
        if (r != null) {
            ServerPlayer winner = SireCall.player(server, r.winner());
            if (winner != null) {
                winner.sendSystemMessage(Component.translatable(KEY + "forfeit", name(server, r.loser())));
            }
            settle(server, r);
        }
    }

    private static void settle(MinecraftServer server, DuelBoard.Result r) {
        ServerPlayer winner = SireCall.player(server, r.winner());
        ServerPlayer loser = SireCall.player(server, r.loser());
        PackIndex index = PackIndex.get(server.overworld());
        if (r.challengerWon()) {
            // the pack may have changed since the duel began: the loser must still lead the winner's pack and the winner still be an Alpha
            boolean valid = winner != null && index.isLeader(r.loser()) && r.loser().equals(index.alphaOf(r.winner()).orElse(null))
                    && PackAccess.isAlpha(PackAccess.wolf(winner).rank()) && VampirismSupernatural.isWerewolf(winner);
            if (!valid) {
                tellOff(server, new UUID[] {r.winner(), r.loser()}, Reason.PACK, null);
                return;
            }
            List<UUID> members = index.betasOf(r.loser());
            index.promote(r.loser(), r.winner());
            for (UUID id : members) {
                if (!id.equals(r.winner())) {
                    SparkHandler.repoint(server, id, r.winner());
                }
            }
            PackMembers.moved(server, members.stream().filter(id -> !id.equals(r.winner()) && !id.equals(r.loser())).toList(), r.winner());
            PartyLink.sync(server, r.winner(), true);
        }
        Component won = Component.translatable(KEY + (r.challengerWon() ? "won_challenger" : "won_leader"), name(server, r.winner()));
        for (ServerPlayer p : new ServerPlayer[] {winner, loser}) {
            if (p != null) {
                p.sendSystemMessage(won);
            }
        }
    }

    private static String name(MinecraftServer server, UUID id) {
        String n = PackAccess.nameOf(server, id);
        return n != null ? n : id.toString().substring(0, 8);
    }

    /** Whatever {@code player} was in (a challenge, a duel) is over; the other one (and a player who did not leave) is told why. */
    public static void callOff(MinecraftServer server, UUID player, Reason reason) {
        for (DuelBoard.Duel d : BOARD.cancelInvolving(player)) {
            tellOff(server, new UUID[] {d.challenger(), d.leader()}, reason, reason == Reason.LOGOUT ? player : null);
        }
    }

    private static void tellOff(MinecraftServer server, UUID[] both, Reason reason, @Nullable UUID except) {
        for (UUID id : both) {
            ServerPlayer p = id.equals(except) ? null : SireCall.player(server, id);
            if (p != null) {
                p.sendSystemMessage(Component.translatable(KEY + "off." + reason.name().toLowerCase(java.util.Locale.ROOT)));
            }
        }
    }

    /** Timeouts, and a pack, a dimension or a login that is not what the duel was asked in any more. */
    public static void check(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (DuelBoard.Duel d : BOARD.expire(now)) {
            tellOff(server, new UUID[] {d.challenger(), d.leader()}, Reason.TIMEOUT, null);
        }
        PackIndex index = PackIndex.get(server.overworld());
        for (DuelBoard.Duel d : BOARD.all()) {
            ServerPlayer c = SireCall.player(server, d.challenger());
            ServerPlayer l = SireCall.player(server, d.leader());
            Reason why = null;
            if (c == null || l == null) {
                why = Reason.LOGOUT;
            } else if (!d.leader().equals(index.alphaOf(d.challenger()).orElse(null)) || !index.isLeader(d.leader())) {
                why = Reason.PACK;
            } else if (c.level().dimension() != l.level().dimension() || (d.phase() == DuelBoard.Phase.ON && c.distanceTo(l) > RANGE)) {
                why = Reason.FAR;
            }
            if (why != null) {
                if (d.phase() == DuelBoard.Phase.ON && why == Reason.FAR) {
                    BOARD.bar(d.challenger(), d.leader(), now); // walking out of a duel is no way to end it for free
                }
                BOARD.cancelInvolving(d.challenger());
                tellOff(server, new UUID[] {d.challenger(), d.leader()}, why, null);
            }
        }
    }

    /** The world stopped: no half-fought duel is left behind (the board lives in memory). */
    public static void onStopped() {
        BOARD.clear();
    }

    /** For tests: nothing is left on the board. */
    public static void reset() {
        BOARD.clear();
    }
}
