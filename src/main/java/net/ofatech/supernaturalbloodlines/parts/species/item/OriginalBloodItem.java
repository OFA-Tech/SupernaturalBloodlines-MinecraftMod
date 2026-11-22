package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.UUID;
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
        if (!stack.has(SpeciesRegistries.BLOOD_SOURCE_COMPONENT)) {
            return InteractionResultHolder.pass(stack); // blood from nobody
        }
        if (!mayDrink(stack, player)) {
            // the bottle is kept; the player is told why (R40a)
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(REFUSED), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    /** Whether this bottle's source may start the hybrid transition under {@code hybrid_transition_blood}; a bottle that did not record its source may. */
    public static boolean startsTransition(ItemStack stack) {
        return TransitionBlood.accepts(SpeciesConfig.transitionBloodAnyOriginal(), stack.get(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT));
    }

    private static final net.minecraft.resources.ResourceLocation WEREWOLF_ID = net.minecraft.resources.ResourceLocation.parse("werewolves:werewolf");

    /**
     * Who may drink it. A werewolf, when the bottle's source may start the hybrid transition: {@code hybrid_transition_blood} gates only that. A player with no
     * faction, a Ghoul, or someone in the turning, whatever the setting says: the blood is their pending sire for the vampire turning. Anyone else is refused.
     */
    public static boolean mayDrink(ItemStack stack, LivingEntity drinker) {
        if (!(drinker instanceof Player p)) {
            return false;
        }
        var faction = de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        if (faction != null && WEREWOLF_ID.equals(faction.getID())) {
            return startsTransition(stack);
        }
        var state = net.ofatech.supernaturalbloodlines.core.BloodlineAccess.get(p);
        return faction == null || state.ghoul() || state.inTurning();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        UUID source = stack.get(SpeciesRegistries.BLOOD_SOURCE_COMPONENT);
        if (!mayDrink(stack, entity)) {
            return stack; // not drunk by any other route either; nothing is used up
        }
        if (source != null && entity instanceof ServerPlayer player) {
            BloodDrink.drank(player, source, stack.get(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT), false);
        }
        return entity instanceof Player player ? ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)) : stack;
    }
}
