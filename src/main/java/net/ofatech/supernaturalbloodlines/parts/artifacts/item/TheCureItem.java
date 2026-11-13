package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteHooks;

/**
 * One drink removes every vampire, werewolf and species layer ({@code DRANK_THE_CURE}). The faction side of the
 * outcome goes through {@link FactionRiteHooks}, like the rites: the species part's runner when it is on, the plain
 * one otherwise. That runner is the only place the faction is left.
 */
public class TheCureItem extends Item {
    public TheCureItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).food(new FoodProperties.Builder()
                .nutrition(0).saturationModifier(0f).alwaysEdible().usingConvertsTo(Items.GLASS_BOTTLE).build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack left = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer p) {
            cure(p);
        }
        return left;
    }

    /** What the drink does to {@code p}, whoever held the bottle. */
    public static void cure(ServerPlayer p) {
        var ctx = FactionRiteHooks.context(p, FactionRiteFactory.baseFaction(VampirismAPI.factionPlayerHandler(p)), p.level().getGameTime());
        Outcome out = BloodlineAccess.applyEvent(p, BloodlineEvent.DRANK_THE_CURE, ctx);
        FactionRiteHooks.run(p, out);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }
}
