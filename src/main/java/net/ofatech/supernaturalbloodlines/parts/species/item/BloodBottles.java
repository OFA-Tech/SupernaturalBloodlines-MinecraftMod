package net.ofatech.supernaturalbloodlines.parts.species.item;

import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Drinker;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.TransitionBlood;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.TransitionHandler;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler;

/**
 * Drinking a bottle of blood applies the rules of its kind (R210, {@link BloodBottleRules}): the one place that decides, for every drinkable blood
 * (Vampirism's vampire blood bottle, Original's Blood, Klaus's Blood). A bottle with no readable source counts as common vampire blood.
 */
public final class BloodBottles {
    public static final String NO_EFFECT = "supernaturalbloodlines.bottle.no_effect";
    public static final String REFUSED = "supernaturalbloodlines.bottle.refused";
    public static final String MARKED = "supernaturalbloodlines.bottle.marked";
    /** What a bottle with no readable source counts as. */
    public static final BloodSource UNSTAMPED = new BloodSource(Util.NIL_UUID, "", true, BloodKind.COMMON_VAMPIRE);

    private BloodBottles() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BloodBottles::onRightClick);
        // bottles made before sealing: the player's own are sealed at login (the rest, when used)
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                sealOld(p);
            }
        });
    }

    /** Seals the old plain sources on the stacks a player carries. @return how many stacks changed */
    public static int sealOld(ServerPlayer p) {
        int changed = 0;
        for (var list : java.util.List.of(p.getInventory().items, p.getInventory().armor, p.getInventory().offhand)) {
            for (ItemStack s : list) {
                if (BloodStamp.migrate(s)) {
                    changed++;
                }
            }
        }
        return changed;
    }

    public static int vampireBloodMinutes() {
        return (int) Math.ceil(SpeciesConfig.vampireBloodTicks() / 1200.0);
    }

    /**
     * The source of a bottle as the drink sees it. A sealed Original-kind bottle (one that names the drinker's sire) only counts while its source is still
     * that Original: an NPC Original of that kind, or a player who is one now (online, the exact species; offline, an Original). A cured Original's old
     * bottle, or a forged one, is unstamped. A bottle in the old plain form predates the seal and is believed as it was.
     */
    public static BloodSource sourceOf(MinecraftServer server, ItemStack stack) {
        Optional<BloodSource> read = BloodStamp.read(stack);
        if (read.isEmpty()) {
            return UNSTAMPED;
        }
        BloodSource s = read.get();
        return BloodStamp.sealed(stack) && s.kind().namesSire() && !stillThatOriginal(server, s) ? UNSTAMPED : s;
    }

    private static boolean stillThatOriginal(MinecraftServer server, BloodSource s) {
        Optional<NpcOriginal> npc = NpcOriginal.byUuid(s.id());
        if (npc.isPresent()) {
            return s.kind() == (npc.get() == NpcOriginal.KLAUS ? BloodKind.ORIGINAL_HYBRID : BloodKind.ORIGINAL);
        }
        ServerPlayer online = SireCall.player(server, s.id());
        if (online != null) {
            return BloodKind.of(BloodlineAccess.get(online).species()) == s.kind();
        }
        return Lines.isOriginalId(server, s.id());
    }

    public static Drinker drinkerOf(ServerPlayer p) {
        var faction = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        BloodlineState s = BloodlineAccess.get(p);
        if (faction != null && TurningHandler.WEREWOLF_ID.equals(faction.getID())) {
            return s.inTransition() ? Drinker.WEREWOLF_IN_TRANSITION : Drinker.WEREWOLF;
        }
        return faction == null || s.ghoul() || s.inTurning() ? Drinker.HUMAN : Drinker.OTHER;
    }

    /** What one drink of {@code source} would do to {@code target}, without doing it. */
    public static Effect decide(ServerPlayer target, BloodSource source) {
        boolean starts = TransitionBlood.accepts(SpeciesConfig.transitionBloodAnyOriginal(), source.kind().speciesName());
        return BloodBottleRules.decide(drinkerOf(target), source.kind(), TurningHandler.wouldMark(target), starts);
    }

    /** What the drinker is told when the bottle is kept: the explanation of the transition rule for a werewolf and an Original's blood, else one general line. */
    public static Component refusal(ServerPlayer p, BloodSource source) {
        Drinker d = drinkerOf(p);
        boolean werewolf = d == Drinker.WEREWOLF || d == Drinker.WEREWOLF_IN_TRANSITION;
        return Component.translatable(werewolf && source.kind().namesSire() ? OriginalBloodItem.REFUSED : REFUSED);
    }

    /** Applies one drink of {@code source} to {@code target}; does not touch an item. @return the effect applied (REFUSED: nothing happened, keep the bottle) */
    public static Effect apply(ServerPlayer target, BloodSource source) {
        return apply(target, source, false);
    }

    /** @param literalName print the source's real name in the drink message (Klaus's Blood is named after him) */
    public static Effect apply(ServerPlayer target, BloodSource source, boolean literalName) {
        Effect effect = decide(target, source);
        run(target, source, effect, literalName);
        return effect;
    }

    private static void run(ServerPlayer p, BloodSource source, Effect effect, boolean literalName) {
        boolean warn = !BloodlineAccess.get(p).inTurning();
        switch (effect) {
            case MARK -> {
                TurningHandler.drankVampireBlood(p);
                if (warn) {
                    p.sendSystemMessage(Component.translatable(MARKED, vampireBloodMinutes()));
                }
            }
            case MARK_AND_NAME_SIRE -> {
                BloodDrink.drank(p, source.id(), source.kind().speciesName(), literalName, warn);
                TurningHandler.drankVampireBlood(p);
            }
            case NAME_SIRE, TRANSITION_BLOOD -> BloodDrink.drank(p, source.id(), source.kind().speciesName(), literalName);
            case UNSTABLE -> TransitionHandler.drankOtherBlood(p);
            case NO_EFFECT -> p.displayClientMessage(Component.translatable(NO_EFFECT), true);
            case REFUSED -> {
            }
        }
    }

    /**
     * Vampirism's bottle is not drinkable by anyone (its item has no use of its own), so the click is taken over: one bottle is used up and the drink
     * applied, unless the rules refuse (then nothing is cancelled, the bottle is kept and the player is told).
     */
    private static void onRightClick(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        ItemStack stack = e.getItemStack();
        if (!stack.is(TurningHandler.VAMPIRE_BLOOD) || stack.getUseDuration(p) > 0) {
            return;
        }
        BloodSource source = sourceOf(p.server, stack);
        Effect effect = decide(p, source);
        if (effect == Effect.REFUSED) {
            p.displayClientMessage(refusal(p, source), true);
            return;
        }
        e.setCancellationResult(InteractionResult.CONSUME);
        e.setCanceled(true);
        TransitionHandler.useUpBottle(p, stack);
        run(p, source, effect, false);
    }
}
