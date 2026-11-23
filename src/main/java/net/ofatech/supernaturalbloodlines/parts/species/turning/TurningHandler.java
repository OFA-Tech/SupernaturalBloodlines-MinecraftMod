package net.ofatech.supernaturalbloodlines.parts.species.turning;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
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

    public static final ResourceLocation WEREWOLF_ID = ResourceLocation.parse("werewolves:werewolf");

    private TurningHandler() {
    }

    public static void register() {
        // A mortal's lethal blow is caught first of all (HIGHEST, never a death something else prevented), so nothing that counts deaths (kill credit,
        // the mark, a duel) sees a death that did not happen. A Ghoul has Vampirism's own dying state: it catches at HIGHEST (its onLivingDeathFirst), and
        // ours, one step later and still before everything that counts or saves (HIGH, never a cancelled event), takes only the blow it did not catch.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, LivingDeathEvent.class, e -> onDeath(e, false));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, LivingDeathEvent.class, e -> onDeath(e, true));
        // A real death (void, /kill: nothing is caught) starts the turning only at the very end, and only if nothing saved them on the way: a Totem
        // never reaches this event at all, and a saver that cancels it leaves the player as they were
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDeathEvent.class, TurningHandler::onRealDeath);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> GHOULS_DYING.remove(e.getEntity().getUUID()));
        // a Ghoul's death is caught by Vampirism's own dying state (a cancelled event): that is the dying of the turning for them
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, true, LivingDeathEvent.class, TurningHandler::onGhoulDying);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, LivingDeathEvent.class, TurningHandler::onGaveUpDying);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                watchGhoulDying(p);
                if (TransitionTimer.secondIsUp(p)) {
                    secondPassed(p);
                }
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

    private static void onDeath(LivingDeathEvent e, boolean ghouls) {
        // a failed turning is the end of it, not a new dose
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getSource().is(OutcomeExecutor.FAILED_TRANSITION)
                || BloodlineAccess.get(p).vampireBloodUntil() == 0 || BloodlineAccess.get(p).ghoul() != ghouls) {
            return;
        }
        if (OriginalRules.bypassesProtection(e.getSource())) {
            return; // a real death: see onRealDeath
        }
        // only the death that STARTS the turning is caught: drinking vampire blood again in it (the rule ignores it now) must not buy another life
        boolean wasTurning = BloodlineAccess.get(p).inTurning();
        // Original's blood still in them names the sire they get if the turning completes (the pending sire is kept in the state, so the dying loses nothing)
        UUID source = p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) && p.hasData(SpeciesRegistries.BLOOD_SOURCE) ? p.getData(SpeciesRegistries.BLOOD_SOURCE) : null;
        apply(p, BloodlineEvent.DIED_WITH_VAMPIRE_BLOOD, source);
        if (wasTurning || !BloodlineAccess.get(p).inTurning()) {
            return;
        }
        // the blow is caught: they lie where they fell for the 30 s of the dying, then stand up at half health, still in the turning
        e.setCanceled(true);
        Downed.startDying(p, e.getSource());
        // a Ghoul that Vampirism's dying state did not catch (Neonatal, a blow that kills vampires, no level) is taken by ours, with its 30 s
        p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.dying", Downed.TICKS / 20));
    }

    /** The last word on a death nothing caught or saved: a blow that bypasses protection puts vampire blood into the turning, and a Ghoul's dying state ends with them. */
    private static void onRealDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        GHOULS_DYING.remove(p.getUUID()); // dead: no wake-up is waiting
        if (e.getSource().is(OutcomeExecutor.FAILED_TRANSITION) || !OriginalRules.bypassesProtection(e.getSource()) || BloodlineAccess.get(p).vampireBloodUntil() == 0
                || net.ofatech.supernaturalbloodlines.core.death.FinalDeath.is(p)) { // a final death (a heart-rip, Give up) starts no turning
            return;
        }
        UUID source = p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) && p.hasData(SpeciesRegistries.BLOOD_SOURCE) ? p.getData(SpeciesRegistries.BLOOD_SOURCE) : null;
        boolean wasTurning = BloodlineAccess.get(p).inTurning();
        apply(p, BloodlineEvent.DIED_WITH_VAMPIRE_BLOOD, source);
        if (!wasTurning && BloodlineAccess.get(p).inTurning()) {
            p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.started")); // void, /kill, white oak: they respawn in the turning
        }
    }

    private static void onGhoulDying(LivingDeathEvent e) {
        if (e.isCanceled() && e.getEntity() instanceof ServerPlayer p && BloodlineAccess.get(p).ghoul() && BloodlineAccess.get(p).vampireBloodUntil() != 0
                && !BloodlineAccess.get(p).inTurning() && VampirismAPI.vampirePlayer(p).isDBNO()) {
            apply(p, BloodlineEvent.DIED_WITH_VAMPIRE_BLOOD, null);
            if (BloodlineAccess.get(p).inTurning()) {
                // only the death that STARTS the turning is waited on: a second dying state of Vampirism's gives no wake-up and no Resistance
                GHOULS_DYING.add(p.getUUID());
                p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.dying_ghoul")); // how long it lasts is Vampirism's
            }
        }
    }

    /** Ghouls whose Vampirism dying state began the turning (and only that one), to wake them when it ends. */
    private static final java.util.Set<UUID> GHOULS_DYING = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** A Ghoul whose dying state began the turning and has now ended, alive, wakes like anyone else (once: this only removes, it never adds). */
    public static void watchGhoulDying(ServerPlayer p) {
        if (GHOULS_DYING.isEmpty() || !GHOULS_DYING.contains(p.getUUID()) || VampirismAPI.vampirePlayer(p).isDBNO()) {
            return;
        }
        GHOULS_DYING.remove(p.getUUID());
        if (p.isAlive() && BloodlineAccess.get(p).inTurning()) {
            Downed.woke(p);
        }
    }

    /** Vampirism's own Give up (its {@code vampirism:dbno} damage) ends the turning of a Ghoul just as ours does. */
    private static void onGaveUpDying(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && BloodlineAccess.get(p).inTurning() && e.getSource().typeHolder().is(ResourceLocation.fromNamespaceAndPath("vampirism", "dbno"))) {
            apply(p, BloodlineEvent.TURNING_GAVE_UP, null);
        }
    }

    /** The turning's clock stands still while they are dying (our state, or a Ghoul in Vampirism's): the 30 s are free. */
    private static boolean isDying(ServerPlayer p) {
        return Downed.isDyingForTurning(p) || BloodlineAccess.get(p).ghoul() && VampirismAPI.vampirePlayer(p).isDBNO();
    }

    /** 20 ticks of online time passed for a player. */
    public static void secondPassed(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        if (s.inTurning() && !isDying(p) && TransitionTimer.tick(p, s.turningTicksLeft(), left -> s.toBuilder().turningTicksLeft((int) left).build())) {
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
        boolean changed = !BloodlineAccess.get(p).equals(before);
        if (changed) {
            net.ofatech.supernaturalbloodlines.parts.species.blood.VampireBloodBuff.drank(p); // every way the blood goes in comes through here
        }
        return changed;
    }

    /** Human-like blood reached a player in the turning. */
    public static void drankHumanBlood(ServerPlayer p) {
        BloodlineState before = BloodlineAccess.get(p);
        if (!before.inTurning()) {
            return;
        }
        boolean lineGone = PendingSire.settle(p, true);
        BloodlineState settled = BloodlineAccess.get(p);
        Outcome[] out = new Outcome[1];
        GhoulGates.completing(p, () -> out[0] = apply(p, BloodlineEvent.TURNING_FED, null));
        boolean joins = out[0].actions().contains(Outcome.Action.JOIN_VAMPIRE);
        if (joins && VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != VReference.VAMPIRE_FACTION) {
            // the turning is over and what drank it is gone: Vampirism or another mod refused the join, so the sire it gave goes too
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().sire(before.sire()).build());
            if (before.sire() == null) {
                net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex.get(p.server).remove(p.getUUID()); // the line it gave goes with the sire
            }
            SupernaturalBloodlines.LOGGER.warn("TurningHandler: {} drank human blood but could not join the vampires", p.getGameProfile().getName());
            p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.join_refused"));
        } else if (joins || before.ghoul() && !BloodlineAccess.get(p).ghoul()) {
            p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.completed"));
            PendingSire.completed(p, settled, lineGone);
        }
    }

    /**
     * The click takes the item over: a blood bottle during the turning is human blood, and one is used up. The hybrid transition (R40) goes first. A
     * vampire-blood bottle is taken over by {@code BloodBottles}, which applies the rules of its kind.
     */
    static void onRightClick(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        ItemStack stack = e.getItemStack();
        BloodlineState s = BloodlineAccess.get(p);
        if (s.inTransition() || !stack.is(TransitionHandler.BLOOD_DRINKS) || !s.inTurning() || !wouldComplete(p)) {
            return;
        }
        e.setCancellationResult(InteractionResult.CONSUME);
        e.setCanceled(true);
        TransitionHandler.useUpBottle(p, stack);
        drankHumanBlood(p);
    }

    /** The turning would do something for this player (a hunter or a werewolf in it would only end it quietly). */
    public static boolean wouldComplete(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        Outcome out = BloodlineRules.apply(s, BloodlineEvent.TURNING_FED, SpeciesContext.of(p, null));
        return !out.actions().isEmpty() || out.state().ghoul() != s.ghoul();
    }

    /** Vampire blood would change this player (a hunter or a werewolf is not marked). */
    public static boolean wouldMark(ServerPlayer p) {
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
