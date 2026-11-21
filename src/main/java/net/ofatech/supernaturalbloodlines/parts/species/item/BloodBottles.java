package net.ofatech.supernaturalbloodlines.parts.species.item;

import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Drinker;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.TransitionBlood;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.TransitionHandler;
import net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler;

/**
 * Drinking a bottle of blood applies the rules of its kind (R210, {@link BloodBottleRules}): the one place that decides, for every drinkable blood
 * (Vampirism's vampire blood bottle, Original's Blood, Klaus's Blood). A bottle with no stamp is common vampire blood.
 */
public final class BloodBottles {
    public static final String NO_EFFECT = "supernaturalbloodlines.bottle.no_effect";
    /** What a bottle with no readable source counts as. */
    public static final BloodSource UNSTAMPED = new BloodSource(Util.NIL_UUID, "", true, BloodKind.COMMON_VAMPIRE);

    private static final ResourceLocation WEREWOLF_ID = ResourceLocation.parse("werewolves:werewolf");

    private BloodBottles() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BloodBottles::onRightClick);
    }

    public static BloodSource sourceOf(ItemStack stack) {
        return BloodStamp.read(stack).orElse(UNSTAMPED);
    }

    public static Drinker drinkerOf(ServerPlayer p) {
        var faction = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        BloodlineState s = BloodlineAccess.get(p);
        if (faction != null && WEREWOLF_ID.equals(faction.getID())) {
            return s.inTransition() ? Drinker.WEREWOLF_IN_TRANSITION : Drinker.WEREWOLF;
        }
        return faction == null || s.ghoul() || s.inTurning() ? Drinker.HUMAN : Drinker.OTHER;
    }

    /** What one drink of {@code source} would do to {@code target}, without doing it. */
    public static Effect decide(ServerPlayer target, BloodSource source) {
        boolean starts = TransitionBlood.accepts(SpeciesConfig.transitionBloodAnyOriginal(), source.kind().speciesName());
        return BloodBottleRules.decide(drinkerOf(target), source.kind(), TurningHandler.wouldMark(target), starts);
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
        switch (effect) {
            case MARK -> TurningHandler.drankVampireBlood(p);
            case MARK_AND_NAME_SIRE -> {
                BloodDrink.drank(p, source.id(), source.kind().speciesName(), literalName);
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
     * Vampirism's bottle is not drinkable by itself (it refuses anyone below vampire level 1), so the click is taken over: one bottle is used up and the
     * drink applied, unless the rules refuse (then nothing is cancelled and the bottle is kept).
     */
    private static void onRightClick(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        ItemStack stack = e.getItemStack();
        if (!stack.is(TurningHandler.VAMPIRE_BLOOD) || stack.getUseDuration(p) > 0) {
            return;
        }
        BloodSource source = sourceOf(stack);
        Effect effect = decide(p, source);
        if (effect == Effect.REFUSED) {
            return;
        }
        e.setCancellationResult(InteractionResult.CONSUME);
        e.setCanceled(true);
        TransitionHandler.useUpBottle(p, stack);
        run(p, source, effect, false);
    }
}
