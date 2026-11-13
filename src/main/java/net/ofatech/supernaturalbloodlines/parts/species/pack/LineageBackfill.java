package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.HybridTraits;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.WerewolfFaction;

/**
 * A werewolf with no lineage (a save from before the packs, or someone who joined the faction by a Vampirism command or
 * another mod) is made a bitten Omega, as if a wild wolf had bitten them: without a lineage the pack rules, the rank
 * perks and the moon control cannot see them. Hybrids are left alone (their type decides what they are), and so is
 * anyone the rules refuse. At login at once; after a join two ticks later, so the bite, gene and transition paths that
 * set the lineage themselves in the same event (or reject the bite) have finished first. Touched only behind
 * {@code ModList.isLoaded("werewolves")}.
 */
public final class LineageBackfill {
    private LineageBackfill() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                backfill(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> {
            if (e.getPlayer().getPlayer() instanceof ServerPlayer p && !p.level().isClientSide
                    && e.getCurrentFaction() == WerewolfFaction.faction() && e.getOldFaction() != WerewolfFaction.faction()) {
                // tell() queues behind whatever the other listeners queued; once more so that a queued rejection runs first
                p.server.tell(new TickTask(p.server.getTickCount(), () -> p.server.tell(new TickTask(p.server.getTickCount(), () -> backfill(p)))));

            }
        });
    }

    /** True when it changed something. */
    public static boolean backfill(ServerPlayer p) {
        if (!VampirismSupernatural.isWerewolf(p) || PackAccess.wolf(p).lineage() != Lineage.NONE
                || HybridTraits.isHybrid(BloodlineAccess.get(p).species())) {
            return false;
        }
        return !PackAccess.applyPack(p, PackEvent.BITTEN_BY_WILD, PackContext.defaults()).refused()
                && PackAccess.wolf(p).lineage() != Lineage.NONE;
    }
}
