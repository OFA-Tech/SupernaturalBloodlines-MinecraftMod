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
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.DuelBoard;
import net.ofatech.supernaturalbloodlines.core.bloodline.DuelBoard.Reason;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
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
    /** True from the moment the server starts to stop: the players who are sent away then have not left any duel. */
    private static volatile boolean stopping;
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
        // the credit for a blow is given when it landed: a cancelled one (a shield, a protected player) never gets here
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (LivingDamageEvent.Post e) -> onHurt(e));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                forfeit(p.server, p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.getServer() != null) {
                onLogin(p.getServer(), p);
            }
        });
        // a world that is stopping leaves no half-fought duel behind (the board lives in memory): the logouts that follow must not settle one
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> onStopping());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> onStopped());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartingEvent e) -> stopping = false);
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
                case PAIR_CALLED_OFF -> PackCommand.error("duel_called_off", minutes);
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

    /** A blow that landed on a player: the attacker has hit them, for the credit of a death by the world that comes soon after. */
    static void onHurt(LivingDamageEvent.Post e) {
        Entity by = e.getSource().getEntity();
        if (by != null && e.getNewDamage() > 0 && e.getEntity() instanceof ServerPlayer hurt) {
            BOARD.recordHit(hurt.getUUID(), by.getUUID(), hurt.level().getGameTime());
        }
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        Entity by = e.getSource().getEntity();
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
        if (duel.pausedBy() != null) {
            // the other one is away and cannot be given the win or the loss: a death in the meantime ends the duel, it does not settle it
            callOff(loser.server, loser.getUUID(), Reason.DIED);
            return;
        }
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
     * {@code leaver} lost their connection (or logged out). A challenge nobody accepted yet is just called off. A duel that is on
     * waits for them for {@code duel_reconnect_seconds}: it is paused and the other one is told how long they have; with 0 it is
     * lost at once. If the other one is away already, there is nobody left to wait for and it is called off.
     */
    public static void forfeit(MinecraftServer server, ServerPlayer leaver) {
        if (stopping || !server.isRunning()) {
            return; // the shutdown sends everybody away: nobody has walked out of anything
        }
        DuelBoard.Duel duel = BOARD.on(leaver.getUUID()).orElse(null);
        if (duel == null) {
            callOff(server, leaver.getUUID(), Reason.LOGOUT);
            return;
        }
        int seconds = SpeciesConfig.duelReconnectSeconds();
        if (duel.pausedBy() != null && !duel.pausedBy().equals(leaver.getUUID())) {
            // the other one left first and has been waited for: they lose it, and this one wins it (they are leaving too, and read it later)
            settleForfeit(server, duel.pausedBy(), leaver.getUUID());
            return;
        }
        if (seconds <= 0 || duel.pauseSpent(leaver.getUUID())) {
            settleForfeit(server, leaver.getUUID(), leaver.getUUID()); // no wait, or the one wait of this duel was used already
            return;
        }
        long now = server.overworld().getGameTime();
        if (!BOARD.pause(leaver.getUUID(), now, seconds * 20L)) {
            callOff(server, leaver.getUUID(), Reason.LOGOUT);
            return;
        }
        ServerPlayer other = SireCall.player(server, duel.other(leaver.getUUID()));
        if (other != null) {
            other.sendSystemMessage(Component.translatable(KEY + "disconnected", name(server, leaver.getUUID()), seconds));
        }
    }

    /** A duelist who was away is back (a login): if the wait is not over the duel goes on, and both are told. */
    public static void onLogin(MinecraftServer server, ServerPlayer back) {
        long now = server.overworld().getGameTime();
        DuelBoard.Duel d = BOARD.resume(back.getUUID(), now).orElse(null);
        if (d == null) {
            check(server); // the wait may have run out a moment ago: settle it before anything else
            // the login already delivered the notices: what that settled would wait for the next one
            for (String[] n : PackIndex.get(server.overworld()).takeNotices(back.getUUID())) {
                back.sendSystemMessage(Component.translatable(n[0], n[1]));
            }
            return;
        }
        back.sendSystemMessage(Component.translatable(KEY + "resumed_you"));
        ServerPlayer other = SireCall.player(server, d.other(back.getUUID()));
        if (other != null) {
            other.sendSystemMessage(Component.translatable(KEY + "resumed", name(server, back.getUUID())));
        }
    }

    /** {@code loser} did not come back in time: the duel is theirs to lose, now. They are not here, so they read how it ended later. */
    static void settleForfeit(MinecraftServer server, UUID loser) {
        settleForfeit(server, loser, null);
    }

    /**
     * {@code loser} lost the duel by not being there. Whoever is online and not {@code leaving} (somebody who is just disconnecting
     * cannot read) is told now; the others find a notice at their next login. Nothing is said when the duel could not be settled
     * (the pack changed), and then only the loser is told it was called off.
     */
    static void settleForfeit(MinecraftServer server, UUID loser, @Nullable UUID leaving) {
        DuelBoard.Result r = BOARD.defeat(loser, server.overworld().getGameTime()).orElse(null);
        if (r == null) {
            return;
        }
        PackIndex index = PackIndex.get(server.overworld());
        boolean applied = settle(server, r);
        if (!applied) {
            if (!isHere(server, r.loser(), leaving)) {
                index.addNotice(r.loser(), KEY + "off.pack", "");
            }
            return;
        }
        UUID leads = index.leaderOf(r.loser());
        boolean moved = r.challengerWon() && leads.equals(r.winner());
        String loserName = name(server, r.loser());
        if (isHere(server, r.winner(), leaving)) {
            SireCall.player(server, r.winner()).sendSystemMessage(Component.translatable(KEY + "forfeit", loserName));
        } else {
            index.addNotice(r.winner(), KEY + (moved ? "forfeit_won_now" : "forfeit_won_same"), loserName);
        }
        String lostKey = KEY + (moved ? "forfeit_lost_now" : "forfeit_lost_same");
        if (isHere(server, r.loser(), leaving)) {
            SireCall.player(server, r.loser()).sendSystemMessage(Component.translatable(lostKey, name(server, leads)));
        } else {
            index.addNotice(r.loser(), lostKey, name(server, leads));
        }
    }

    private static boolean isHere(MinecraftServer server, UUID id, @Nullable UUID leaving) {
        return !id.equals(leaving) && SireCall.player(server, id) != null;
    }
    /** @return whether the result was applied (false: the pack was not the one the duel was asked in, and the duel was called off) */
    private static boolean settle(MinecraftServer server, DuelBoard.Result r) {
        ServerPlayer winner = SireCall.player(server, r.winner());
        ServerPlayer loser = SireCall.player(server, r.loser());
        PackIndex index = PackIndex.get(server.overworld());
        if (r.challengerWon()) {
            // the pack may have changed since the duel began: the loser must still lead the winner's pack and the winner still be an Alpha
            boolean valid = winner != null && index.isLeader(r.loser()) && r.loser().equals(index.alphaOf(r.winner()).orElse(null))
                    && PackAccess.isAlpha(PackAccess.wolf(winner).rank()) && VampirismSupernatural.isWerewolf(winner);
            if (!valid) {
                tellOff(server, new UUID[] {r.winner(), r.loser()}, Reason.PACK, null);
                return false;
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
        return true;
    }

    private static String name(MinecraftServer server, UUID id) {
        String n = PackAccess.nameOf(server, id);
        return n != null ? n : id.toString().substring(0, 8);
    }

    /** Whatever {@code player} was in (a challenge, a duel) is over; the other one (and a player who did not leave) is told why. */
    public static void callOff(MinecraftServer server, UUID player, Reason reason) {
        long now = server.overworld().getGameTime();
        for (DuelBoard.Duel d : BOARD.cancelInvolving(player)) {
            if (d.phase() == DuelBoard.Phase.ON && (reason == Reason.DIED || reason == Reason.KILLED || reason == Reason.RANGE || reason == Reason.FAR)) {
                BOARD.bar(d.challenger(), d.leader(), now); // dying of something else, or being killed by a third player, is no way out for free either
            }
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
        // somebody who did not come back in time forfeits
        for (DuelBoard.Duel d : BOARD.expiredPauses(now)) {
            settleForfeit(server, d.pausedBy());
        }
        PackIndex index = PackIndex.get(server.overworld());
        for (DuelBoard.Duel d : BOARD.all()) {
            if (d.pausedBy() != null) {
                continue; // waiting for somebody: nothing about it is judged until they are back or the wait is over
            }
            ServerPlayer c = SireCall.player(server, d.challenger());
            ServerPlayer l = SireCall.player(server, d.leader());
            Reason why = null;
            if (c == null || l == null) {
                why = Reason.LOGOUT;
            } else if (!d.leader().equals(index.alphaOf(d.challenger()).orElse(null)) || !index.isLeader(d.leader())) {
                why = Reason.PACK;
            } else if (c.level().dimension() != l.level().dimension()) {
                why = Reason.FAR;
            } else if (d.phase() == DuelBoard.Phase.ON && c.distanceTo(l) > RANGE) {
                why = Reason.RANGE;
            }
            if (why != null) {
                callOff(server, d.challenger(), why); // an on duel that is called off for the range (or by a death) also bars the pair
            }
        }
    }

    /** The server is about to stop: the board is cleared first, so the logouts of the shutdown settle nothing. */
    public static void onStopping() {
        stopping = true;
        BOARD.clear();
    }

    /** The world stopped: a backstop for {@link #onStopping}. */
    public static void onStopped() {
        BOARD.clear();
    }

    /** For tests: nothing is left on the board. */
    public static void reset() {
        stopping = false;
        BOARD.clear();
    }
}
