package net.ofatech.supernaturalbloodlines.parts.species.turning;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.parts.species.OutcomeExecutor;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesEligibility;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.TransitionHandler;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.TransitionTimer;
import org.jetbrains.annotations.Nullable;

/**
 * Turning the show's way (plan 11, R20c, R20d): vampire blood in the system, then death, then human blood.
 * A player who dies while vampire blood is still in them comes back in the turning, with {@code transition_ticks} of online time to
 * drink human-like blood. Doing so makes them a vampire (a Ghoul just stops being one); running out of time kills them for good.
 * The rules are in {@code BloodlineRules}; this class is the event glue. It needs no Werewolves.
 *
 * Where blood comes from. Vampirism lets only vampires bite, so a bite reaches {@link BloodDrinkEvent.PlayerDrinkBloodEvent} only
 * for a Ghoul. Everyone else drinks from an item, taken over on the right click because Vampirism's own items refuse a player
 * below vampire level 1 (the pattern of {@link TransitionHandler}): the blood bottle is human blood, and an item in
 * {@link #VAMPIRE_BLOOD} is vampire blood.
 */
public final class TurningHandler {
    /** Items that put vampire blood in the system when drunk (a datapack can add more). */
    public static final TagKey<Item> VAMPIRE_BLOOD = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "vampire_blood"));

    private static final ResourceLocation WEREWOLF_ID = ResourceLocation.parse("werewolves:werewolf");

    private TurningHandler() {
    }

    public static void register() {
        // LOW and not receiving cancelled events, like the hybrid transition: a death something else prevented is not a death
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, LivingDeathEvent.class, TurningHandler::onDeath);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && TransitionTimer.secondIsUp(p)) {
                secondPassed(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingEntityUseItemEvent.Finish e) -> {
            if (e.getEntity() instanceof ServerPlayer p && e.getItem().is(VAMPIRE_BLOOD)) {
                drankVampireBlood(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(TurningHandler::onRightClick);
        NeoForge.EVENT_BUS.addListener(TurningHandler::onDrinkBlood);
    }

    /**
     * Human blood as the turning means it. Nothing in the vampire faction counts (so a converted villager does not), nor does a
     * player of the vampire side (Original, hybrids, tribrid, heretic) or in the werewolf faction. Then: a villager or trader, a
     * hunter, or a player (witches and hunters included) whose race is human-like: Tensura's check when species_tensura installs it,
     * everyone without it.
     */
    public static boolean isHumanLikeBlood(LivingEntity victim) {
        if (isFaction(victim, VReference.VAMPIRE_FACTION)) {
            return false;
        }
        if (victim instanceof ServerPlayer p) {
            var faction = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
            return !(faction != null && WEREWOLF_ID.equals(faction.getID()))
                    && !BloodlineRules.isVampireSpecies(BloodlineAccess.get(p).species()) && SpeciesEligibility.isHumanLike(p);
        }
        return victim instanceof AbstractVillager || isFaction(victim, VReference.HUNTER_FACTION);
    }

    static boolean isFaction(Entity e, IFaction<?> faction) {
        return VampirismAPI.factionRegistry().getFaction(e) == faction;
    }

    private static void onDeath(LivingDeathEvent e) {
        // a failed turning is the end of it, not a new dose
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getSource().is(OutcomeExecutor.FAILED_TRANSITION)
                || BloodlineAccess.get(p).vampireBloodUntil() == 0) {
            return;
        }
        apply(p, BloodlineEvent.DIED_WITH_VAMPIRE_BLOOD, null);
        if (BloodlineAccess.get(p).inTurning()) {
            p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.started"));
        }
    }

    /** 20 ticks of online time passed for a player. */
    public static void secondPassed(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        if (s.inTurning() && TransitionTimer.tick(p, s.turningTicksLeft(), left -> s.toBuilder().turningTicksLeft((int) left).build())) {
            Outcome out = apply(p, BloodlineEvent.TURNING_FAILED, null); // the rules ask for the kill
            if (out.actions().contains(Outcome.Action.KILL_PLAYER)) {
                p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.failed"));
            }
        }
    }

    /** Vampire blood reached the player's system. True when it changed anything (a vampire or a werewolf is not marked). */
    public static boolean drankVampireBlood(ServerPlayer p) {
        BloodlineState before = BloodlineAccess.get(p);
        apply(p, BloodlineEvent.DRANK_VAMPIRE_BLOOD, null);
        return !BloodlineAccess.get(p).equals(before);
    }

    /** Human-like blood reached a player in the turning. */
    public static void drankHumanBlood(ServerPlayer p) {
        BloodlineState before = BloodlineAccess.get(p);
        if (!before.inTurning()) {
            return;
        }
        Outcome[] out = new Outcome[1];
        GhoulGates.completing(p, () -> out[0] = apply(p, BloodlineEvent.TURNING_FED, null));
        boolean joins = out[0].actions().contains(Outcome.Action.JOIN_VAMPIRE);
        if (joins && VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != VReference.VAMPIRE_FACTION) {
            // the turning is over and what drank it is gone: Vampirism or another mod refused the join
            SupernaturalBloodlines.LOGGER.warn("TurningHandler: {} drank human blood but could not join the vampires", p.getGameProfile().getName());
            p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.join_refused"));
        } else if (joins || before.ghoul() && !BloodlineAccess.get(p).ghoul()) {
            p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.completed"));
        }
    }

    /**
     * The click takes the item over: a blood bottle during the turning is human blood, and a vampire-blood item that cannot be drunk
     * by itself marks the player. Either way one is used up and the click is consumed. The hybrid transition (R40) goes first.
     */
    static void onRightClick(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        ItemStack stack = e.getItemStack();
        BloodlineState s = BloodlineAccess.get(p);
        if (s.inTransition()) {
            return;
        }
        boolean human = stack.is(TransitionHandler.BLOOD_DRINKS) && s.inTurning() && wouldComplete(p);
        boolean vampire = !human && stack.is(VAMPIRE_BLOOD) && stack.getUseDuration(p) <= 0;
        if (!human && !vampire) {
            return;
        }
        if (vampire && !wouldMark(p)) {
            return; // nothing to gain: keep the item
        }
        e.setCancellationResult(InteractionResult.CONSUME);
        e.setCanceled(true);
        TransitionHandler.useUpBottle(p, stack);
        if (human) {
            drankHumanBlood(p);
        } else {
            drankVampireBlood(p);
        }
    }

    /** The turning would do something for this player (a hunter or a werewolf in it would only end it quietly). */
    private static boolean wouldComplete(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        Outcome out = BloodlineRules.apply(s, BloodlineEvent.TURNING_FED, SpeciesContext.of(p, null));
        return !out.actions().isEmpty() || out.state().ghoul() != s.ghoul();
    }

    private static boolean wouldMark(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        return !s.equals(BloodlineRules.apply(s, BloodlineEvent.DRANK_VAMPIRE_BLOOD, SpeciesContext.of(p, null)).state());
    }

    /** A vampire (only a Ghoul, in practice) drank from someone: from a vampire it marks them, from a human-like victim it feeds the turning. */
    private static void onDrinkBlood(BloodDrinkEvent.PlayerDrinkBloodEvent e) {
        if (!(e.getVampire().getRepresentingEntity() instanceof ServerPlayer p)) {
            return;
        }
        LivingEntity source = e.getBloodSource().getEntity().orElse(null);
        if (source == null) {
            return;
        }
        if (isFaction(source, VReference.VAMPIRE_FACTION)) {
            drankVampireBlood(p);
        } else if (isHumanLikeBlood(source)) {
            drankHumanBlood(p);
        }
    }

    /** Runs the rule, stores the state and performs the outcome's actions. */
    private static Outcome apply(ServerPlayer p, BloodlineEvent e, @Nullable UUID bloodSource) {
        Outcome out = BloodlineAccess.applyEvent(p, e, SpeciesContext.of(p, bloodSource));
        OutcomeExecutor.run(p, out);
        return out;
    }
}
