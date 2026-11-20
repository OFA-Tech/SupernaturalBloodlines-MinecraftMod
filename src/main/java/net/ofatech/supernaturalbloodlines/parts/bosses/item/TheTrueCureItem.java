package net.ofatech.supernaturalbloodlines.parts.bosses.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteHooks;

/**
 * Rebekah's reward (R171): the cure an Original drinks willingly, with no rite and no desiccation. One drink clears every vampire, werewolf and species
 * layer ({@code DRANK_THE_TRUE_CURE}); the Original is then human, and the ones they sired keep their line (a cured Original is not a line death).
 */
public final class TheTrueCureItem extends Item {
    public TheTrueCureItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant().food(new FoodProperties.Builder()
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

    public static void cure(ServerPlayer p) {
        var ctx = FactionRiteHooks.context(p, FactionRiteFactory.baseFaction(de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(p)), p.level().getGameTime());
        Outcome out = BloodlineAccess.applyEvent(p, BloodlineEvent.DRANK_THE_TRUE_CURE, ctx);
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.supernaturalbloodlines.the_true_cure.tooltip").withStyle(ChatFormatting.GRAY));
    }
}