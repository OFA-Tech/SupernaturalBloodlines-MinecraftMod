package net.ofatech.supernaturalbloodlines.parts.species;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import net.minecraft.core.registries.Registries;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.WerewolfFaction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.ReentrancyGuard;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;

/** Does what an {@link Outcome} asks of the player: the faction changes, the death of a failed transition, the refusal message. */
public final class OutcomeExecutor {
    public static final ResourceKey<DamageType> FAILED_TRANSITION = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "failed_transition"));

    private static final ResourceLocation WEREWOLF_ID = ResourceLocation.parse("werewolves:werewolf");

    private OutcomeExecutor() {
    }

    /**
     * Vampire to werewolf (or back) in one outcome: the level carries over, {@code max(1, min(old, new faction's highest))},
     * instead of the runner's level 1.
     */
    private static void carryLevel(ServerPlayer p, Outcome o, IPlayableFaction<?> oldFaction, int oldLevel) {
        if (oldFaction == null || !o.actions().contains(Outcome.Action.LEAVE_FACTION)) {
            return;
        }
        boolean wasVampire = oldFaction == VReference.VAMPIRE_FACTION;
        boolean wasWerewolf = WEREWOLF_ID.equals(oldFaction.getID());
        IPlayableFaction<?> to = null;
        if (wasVampire && o.actions().contains(Outcome.Action.JOIN_WEREWOLF) && ModList.get().isLoaded("werewolves")) {
            to = WerewolfFaction.faction();
        } else if (wasWerewolf && o.actions().contains(Outcome.Action.JOIN_VAMPIRE)) {
            to = VReference.VAMPIRE_FACTION;
        }
        IFactionPlayerHandler h = VampirismAPI.factionPlayerHandler(p);
        if (to == null || h.getCurrentFaction() != to) {
            return;
        }
        int level = Math.max(1, Math.min(oldLevel, to.getHighestReachableLevel()));
        if (h.getCurrentLevel() != level && !h.setFactionAndLevel(to, level)) {
            SupernaturalBloodlines.LOGGER.warn("OutcomeExecutor: could not carry level {} over for {}", level, p.getGameProfile().getName());
        }
    }

    public static void run(ServerPlayer p, Outcome o) {
        boolean ran = ReentrancyGuard.run(p.getUUID(), "species", () -> {
            if (o.refused()) {
                p.sendSystemMessage(Component.translatable(o.refusalKey()));
                return;
            }
            // leave, then join, in that order (the rite factory's own runner); the kill comes last
            IFactionPlayerHandler h = VampirismAPI.factionPlayerHandler(p);
            IPlayableFaction<?> oldFaction = h.getCurrentFaction();
            int oldLevel = h.getCurrentLevel();
            FactionRiteFactory.runFactionActions(p, o);
            carryLevel(p, o, oldFaction, oldLevel);
            if (o.actions().contains(Outcome.Action.KILL_PLAYER) && !p.isCreative() && !p.isSpectator()) {
                // failed_transition is in #bypasses_invulnerability, so a Totem of Undying does not save them
                p.hurt(p.damageSources().source(FAILED_TRANSITION), Float.MAX_VALUE);
                if (p.isAlive()) {
                    p.kill();
                }
            }
        });
        if (!ran) {
            SupernaturalBloodlines.LOGGER.warn("OutcomeExecutor: skipped a nested outcome for {} (actions {}, refusal {})",
                    p.getGameProfile().getName(), o.actions(), o.refusalKey());
        }
    }
}
