package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.DuelBoard;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import org.jetbrains.annotations.Nullable;

/**
 * The leadership duel (plan 14, owner): a second Alpha challenges the leader, the leader accepts, and they fight to the first
 * defeat. The one who would die is left at 1 HP instead and loses; the winner leads. Nobody loses the spark and it is no steal.
 * It is called off by a timeout of 2 minutes, by either of them logging out or leaving the pack, or by anyone else killing one of them.
 */
public final class PackDuel {
    private static final String KEY = "supernaturalbloodlines.pack.duel.";
    private static final DuelBoard BOARD = new DuelBoard();
    private static final int CHECK_EVERY = 20;
    private static final int SHIELD_TICKS = 100;

    private PackDuel() {
    }

    public static DuelBoard board() {
        return BOARD;
    }

    public static void register() {
        // before the death is handled by anything that would act on it (a totem is asked first, and a duel is not a death)
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, LivingDeathEvent.class, PackDuel::onDeath);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                callOff(p.server, p.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % CHECK_EVERY == 0) {
                check(e.getServer());
            }
        });
    }

    private static UUID leaderOf(ServerPlayer p) {
        return PackIndex.get(p.serverLevel()).leaderOf(p.getUUID());
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
        long now = challenger.serverLevel().getGameTime();
        if (!BOARD.challenge(challenger.getUUID(), leaderId, now)) {
            return PackCommand.error("duel_busy");
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
                || !PackAccess.isAlpha(PackAccess.wolf(challenger).rank())) {
            BOARD.cancelInvolving(leader.getUUID());
            return PackCommand.error("duel_gone");
        }
        BOARD.accept(leader.getUUID(), now);
        Component on = Component.translatable(KEY + "on", PackCommand.name(challenger), PackCommand.name(leader));
        challenger.sendSystemMessage(on);
        leader.sendSystemMessage(on);
        return null;
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer loser) || loser.level().isClientSide) {
            return;
        }
        DuelBoard.Duel duel = BOARD.on(loser.getUUID()).orElse(null);
        if (duel == null) {
            callOff(loser.server, loser.getUUID()); // a challenge nobody accepted yet, or no duel at all
            return;
        }
        UUID by = e.getSource().getEntity() == null ? null : e.getSource().getEntity().getUUID();
        if (!duel.other(loser.getUUID()).equals(by)) {
            callOff(loser.server, loser.getUUID()); // somebody else killed them (or the world did): the duel is off and the death stands
            return;
        }
        // the first defeat: no death, one heart left, and a moment in which the winner cannot finish them
        e.setCanceled(true);
        loser.setHealth(1f);
        loser.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SHIELD_TICKS, 4, false, false));
        DuelBoard.Result r = BOARD.defeat(loser.getUUID()).orElse(null);
        if (r != null) {
            settle(loser.server, r);
        }
    }

    private static void settle(MinecraftServer server, DuelBoard.Result r) {
        ServerPlayer winner = SireCall.player(server, r.winner());
        ServerPlayer loser = SireCall.player(server, r.loser());
        PackIndex index = PackIndex.get(server.overworld());
        if (r.challengerWon() && winner != null) {
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

    /** Whatever {@code player} was in (a challenge, a duel) is over: both are told. */
    public static void callOff(MinecraftServer server, UUID player) {
        for (DuelBoard.Duel d : BOARD.cancelInvolving(player)) {
            tellOff(server, d);
        }
    }

    private static void tellOff(MinecraftServer server, DuelBoard.Duel d) {
        for (UUID id : new UUID[] {d.challenger(), d.leader()}) {
            ServerPlayer p = SireCall.player(server, id);
            if (p != null) {
                p.sendSystemMessage(Component.translatable(KEY + "off"));
            }
        }
    }

    /** Timeouts, and a pack that is not the one the duel was asked in any more. */
    public static void check(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (DuelBoard.Duel d : BOARD.expire(now)) {
            tellOff(server, d);
        }
        PackIndex index = PackIndex.get(server.overworld());
        for (DuelBoard.Duel d : BOARD.all()) {
            boolean here = SireCall.player(server, d.challenger()) != null && SireCall.player(server, d.leader()) != null;
            boolean samePack = d.leader().equals(index.alphaOf(d.challenger()).orElse(null)) && index.isLeader(d.leader());
            if (!here || !samePack) {
                callOff(server, d.challenger());
            }
        }
    }

    /** For tests: nothing is left on the board. */
    public static void reset() {
        BOARD.clear();
    }
}