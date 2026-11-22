package net.ofatech.supernaturalbloodlines.parts.species.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.TransitionBlood;

/**
 * A bottle of an Original's blood. Our own drinkable item: werewolves cannot drink Vampirism's blood bottles. Drinking
 * records whose blood it was and gives the {@code original_blood} effect; the last drink wins.
 */
public final class OriginalBloodItem extends Item {
    public static final String REFUSED = "supernaturalbloodlines.hybrid.blood_refused";
    private static final int DRINK_TICKS = 32;

    public OriginalBloodItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_TICKS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BloodStamp.has(stack)) {
            return InteractionResultHolder.pass(stack); // blood from nobody
        }
        // the client is not told the source (R210), so it cannot judge the drink: the server decides and tells the player why
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }
        BloodSource source = BloodBottles.sourceOf(sp.server, stack);
        if (refuses(sp, source)) {
            player.displayClientMessage(BloodBottles.refusal(sp, source), true); // the bottle is kept; the drinker is told why (R40a)
            return InteractionResultHolder.fail(stack);
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    /** Whether this bottle's source may start the hybrid transition under {@code hybrid_transition_blood}; a bottle that did not record its source may. */
    public static boolean startsTransition(ItemStack stack) {
        return TransitionBlood.accepts(SpeciesConfig.transitionBloodAnyOriginal(), stack.get(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT));
    }

    /** Original's Blood is for the blood of an Original: a bottle the rules would refuse, or do nothing with (a werewolf and common blood), is kept. */
    private static boolean refuses(ServerPlayer p, BloodSource source) {
        BloodBottleRules.Effect e = BloodBottles.decide(p, source);
        return e == BloodBottleRules.Effect.REFUSED || e == BloodBottleRules.Effect.NO_EFFECT;
    }

    /**
     * Who may drink it (R210): the rules of its kind, see {@link BloodBottles#decide}. A werewolf, when the bottle's source may start the hybrid transition;
     * a player with no faction, a Ghoul, or someone in the turning, whatever {@code hybrid_transition_blood} says. Anyone else is refused.
     */
    public static boolean mayDrink(ItemStack stack, LivingEntity drinker) {
        if (!(drinker instanceof Player p)) {
            return false;
        }
        return !(p instanceof ServerPlayer sp) || !refuses(sp, BloodBottles.sourceOf(sp.server, stack));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return stack; // the client never drinks: the server decides and sends the result
        }
        BloodSource source = BloodBottles.sourceOf(player.server, stack);
        if (refuses(player, source)) {
            return stack; // not drunk by any other route either; nothing is used up
        }
        BloodBottles.apply(player, source);
        return ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE));
    }
}
