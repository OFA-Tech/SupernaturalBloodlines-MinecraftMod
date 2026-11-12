package net.ofatech.supernaturalbloodlines.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.core.network.BloodlineSyncPayload;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/** The one door to a player's bloodline state: read it, change it (synced), and listen for species changes. */
public final class BloodlineAccess {
    public record BloodlineChange(BloodlineState before, BloodlineState after) {
    }

    private static final List<BiConsumer<ServerPlayer, BloodlineChange>> LISTENERS = new CopyOnWriteArrayList<>();

    private BloodlineAccess() {
    }

    public static BloodlineState get(Player p) {
        return p.getData(ModAttachments.BLOODLINE);
    }

    public static void set(ServerPlayer p, BloodlineState s) {
        BloodlineState before = get(p);
        p.setData(ModAttachments.BLOODLINE, s);
        if (canReceive(p)) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new BloodlineSyncPayload(p.getId(), s));
        }
        notifyListeners(LISTENERS, p, before, s);
    }

    /** Applies the rules and stores the result. Faction actions in the Outcome are left to the parts. */
    public static Outcome applyEvent(ServerPlayer p, BloodlineEvent e, BloodlineContext c) {
        Outcome out = BloodlineRules.apply(get(p), e, c);
        set(p, out.state());
        return out;
    }

    public static void addListener(BiConsumer<ServerPlayer, BloodlineChange> l) {
        LISTENERS.add(l);
    }

    public static void syncToSelf(ServerPlayer p) {
        sendTo(p, p);
    }

    /** Sends {@code subject}'s state to {@code to}. */
    public static void sendTo(ServerPlayer to, Player subject) {
        if (canReceive(to)) {
            PacketDistributor.sendToPlayer(to, new BloodlineSyncPayload(subject.getId(), get(subject)));
        }
    }

    // Mock/fake players and clients without the mod have no channel for the payload; sending would throw.
    private static boolean canReceive(ServerPlayer p) {
        return p.connection != null && p.connection.hasChannel(BloodlineSyncPayload.TYPE);
    }

    /** Listeners hear about species changes only, not about thirst ticks and the like. */
    static <P> void notifyListeners(List<BiConsumer<P, BloodlineChange>> listeners, P player,
                                    BloodlineState before, BloodlineState after) {
        if (before.species() == after.species()) {
            return;
        }
        BloodlineChange change = new BloodlineChange(before, after);
        for (BiConsumer<P, BloodlineChange> l : listeners) {
            l.accept(player, change);
        }
    }
}
