package net.ofatech.supernaturalbloodlines.parts.species.moon;

import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.AwakeningRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.GeneHandler;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;

/**
 * Delayed awakening (R142). The kill that would awaken a human's latent werewolf gene ({@link GeneHandler}) only queues it, when
 * {@code delayed_awakening_nights} is above 0; the awakening comes at a later dusk (the overworld's day time), or after that many days of game time
 * when the clock is frozen. A message tells the player something stirs. The queue ({@link AwakeningQueue}) is kept through death and logout, and
 * ends without an awakening when the player stopped being a human with the gene. A rite and a wild bite are not delayed, so nothing here touches
 * them. This class reaches Werewolves through {@link GeneHandler}, so it is registered and called only when Werewolves is loaded; the hub reads
 * {@link AwakeningQueue}, which is safe without it.
 */
public final class DelayedAwakening {
    public static final String STIRS = "supernaturalbloodlines.awaken.stirs";
    public static final String STIRS_LATER = "supernaturalbloodlines.awaken.stirs_later";
    private static final int CHECK_PERIOD = 20;

    private DelayedAwakening() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % CHECK_PERIOD == 0) {
                check(p);
            }
        });
    }

    /** Queues the awakening of {@code p}; a second kill while one waits changes nothing. */
    public static void queue(ServerPlayer p) {
        if (AwakeningQueue.pending(p)) {
            return;
        }
        int nights = SpeciesConfig.delayedAwakeningNights();
        long due = Math.max(1, AwakeningRules.due(p.level().getDayTime(), nights));
        AwakeningQueue.set(p, new AwakeningRules.Pending(due, p.level().getGameTime()));
        p.sendSystemMessage(nights <= 1 ? Component.translatable(STIRS) : Component.translatable(STIRS_LATER, nights));
    }

    /** One look at one player: awakens them when it is time, forgets the queue when they are no longer who it was for. */
    public static void check(ServerPlayer p) {
        AwakeningRules.Pending pending = AwakeningQueue.of(p);
        if (!pending.waiting()) {
            return;
        }
        boolean stillHuman = PackAccess.wolf(p).latentGene()
                && FactionRiteFactory.baseFaction(VampirismAPI.factionPlayerHandler(p)) == BaseFaction.NONE;
        if (!stillHuman) {
            AwakeningQueue.clear(p);
            return;
        }
        var level = p.level();
        if (AwakeningRules.isDue(level.getDayTime(), pending.due(), SpeciesConfig.delayedAwakeningNights(), level.getGameTime(), pending.queuedAt())) {
            AwakeningQueue.clear(p);
            String refused = GeneHandler.awakenHuman(p, PackEvent.GENE_AWAKENED);
            if (refused != null) {
                // the rules said no (a faction mod, the pack): the wait is over either way, so say why instead of leaving a silent "stirs"
                p.sendSystemMessage(Component.translatable(refused));
            }
        }
    }
}
