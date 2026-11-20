package net.ofatech.supernaturalbloodlines.parts.bosses.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.boss.KolRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;

/**
 * Kol's reward (R174). Right-click reads it, once: it pays {@code kol_grimoire_sp} skill points into the one shared pool and marks that copy as read. The
 * Rite of Cloaking asks for a read one and gives it back, so it is never used up.
 */
public final class KolGrimoireItem extends Item {
    private static final String READ = "read";

    public KolGrimoireItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    public static boolean isRead(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(READ);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        int sp = KolRules.grimoireSp(isRead(stack), BossesConfig.kolGrimoireSp());
        if (sp <= 0) {
            p.displayClientMessage(Component.translatable("item.supernaturalbloodlines.kol_grimoire.already"), true);
            return InteractionResultHolder.success(stack);
        }
        var state = SupernaturalAccess.get(p);
        SupernaturalAccess.set(p, state.toBuilder().sp(state.sp() + sp).spEarned(state.spEarned() + sp).build());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(READ, true));
        p.displayClientMessage(Component.translatable("item.supernaturalbloodlines.kol_grimoire.read", sp), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !isRead(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(isRead(stack) ? "item.supernaturalbloodlines.kol_grimoire.tooltip_read" : "item.supernaturalbloodlines.kol_grimoire.tooltip",
                BossesConfig.kolGrimoireSp()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.supernaturalbloodlines.kol_grimoire.tooltip_who").withStyle(ChatFormatting.DARK_GRAY));
    }
}
