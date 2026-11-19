package net.ofatech.supernaturalbloodlines.parts.species.moon;

import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.AwakeningRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.GeneHandler;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;

/**
 * Delayed awakening (R142). The kill that would awaken a human's latent werewolf gene ({@link GeneHandler}) only queues it, when
 * {@code delayed_awakening_nights} is above 0; the awakening comes at a later dusk (the overworld's day time), a message tells the player something
 * stirs. The queue is a player attachment kept through death and logout. It ends without an awakening when the player stopped being a human with the
 * gene (they joined a faction, or the gene is gone). A rite and a wild bite are not delayed, so nothing here touches them. Touched only behind
 * {@code ModList.isLoaded("werewolves")}.
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

    /** The day time the awakening is due, or 0. */
    public static long dueOf(Player p) {
        return p.getData(SpeciesRegistries.PENDING_AWAKENING);
    }

    public static boolean pending(Player p) {
        return dueOf(p) > 0;
    }

    /** Queues the awakening of {@code p}; a second kill while one waits changes nothing. */
    public static void queue(ServerPlayer p) {
        if (pending(p)) {
            return;
        }
        int nights = SpeciesConfig.delayedAwakeningNights();
        p.setData(SpeciesRegistries.PENDING_AWAKENING, Math.max(1, AwakeningRules.due(p.level().getDayTime(), nights)));
        p.sendSystemMessage(nights <= 1 ? Component.translatable(STIRS) : Component.translatable(STIRS_LATER, nights));
    }

    /** One look at one player: awakens them when it is time, forgets the queue when they are no longer who it was for. */
    public static void check(ServerPlayer p) {
        long due = dueOf(p);
        if (due <= 0) {
            return;
        }
        boolean stillHuman = PackAccess.wolf(p).latentGene()
                && FactionRiteFactory.baseFaction(VampirismAPI.factionPlayerHandler(p)) == BaseFaction.NONE;
        if (!stillHuman) {
            p.removeData(SpeciesRegistries.PENDING_AWAKENING);
            return;
        }
        if (AwakeningRules.isDue(p.level().getDayTime(), due, SpeciesConfig.delayedAwakeningNights())) {
            p.removeData(SpeciesRegistries.PENDING_AWAKENING);
            GeneHandler.awakenHuman(p, PackEvent.GENE_AWAKENED);
        }
    }
}
