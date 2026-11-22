package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;

/**
 * Wes's syringe (R202): a plain vampire who takes it becomes an Augustine. It works on yourself (right-click in the air, sneaking) or on another player who is
 * sneaking (their consent). Whatever the reason a use does nothing (not a vampire, a species, a Ghoul, not sneaking), the player sees the same one line, so the
 * syringe cannot be used to find out what someone is. One syringe is used up on success.
 */
public final class AugustineSyringeItem extends Item {
    public static final String NOTHING = "supernaturalbloodlines.syringe.nothing";
    public static final String DONE = "supernaturalbloodlines.syringe.done";
    public static final String DONE_TO = "supernaturalbloodlines.syringe.done_to";
    public static final String ARMED = "supernaturalbloodlines.syringe.armed";
    public static final String ASKED = "supernaturalbloodlines.syringe.asked";
    private static final net.ofatech.supernaturalbloodlines.core.bloodline.SyringeConfirm CONFIRM = new net.ofatech.supernaturalbloodlines.core.bloodline.SyringeConfirm();

    /** For tests: forgets every arming. */
    public static void forgetArmedForTest() {
        CONFIRM.clear();
    }

    public AugustineSyringeItem(Properties properties) {
        super(properties);
    }

    /** The whole rule of a use: true when {@code target} became an Augustine. The caller says nothing of why not. */
    public static boolean inject(ServerPlayer target) {
        if (!target.isShiftKeyDown()) {
            return false;
        }
        Outcome out = BloodlineAccess.applyEvent(target, BloodlineEvent.AUGUSTINE_SYRINGE, SpeciesContext.of(target, null));
        return !out.refused() && out.state().species() == Species.AUGUSTINE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer self)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (inject(self)) {
            stack.consume(1, self);
            self.sendSystemMessage(Component.translatable(DONE));
        } else {
            self.displayClientMessage(Component.translatable(NOTHING), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!(player instanceof ServerPlayer user) || !(target instanceof ServerPlayer other)) {
            return InteractionResult.PASS; // only players take it
        }
        // the target agrees as a link does: sneaking, a lead in the main hand, and PvP on (a deliberate act, and the server's own rule for hurting a player)
        boolean consent = net.ofatech.supernaturalbloodlines.core.boss.LinkRules.consents(false, other.isShiftKeyDown(),
                other.getMainHandItem().is(net.minecraft.world.item.Items.LEAD), user.server.isPvpAllowed());
        if (!consent) {
            user.displayClientMessage(Component.translatable(NOTHING), true);
            return InteractionResult.sidedSuccess(false);
        }
        // the first use only arms it for 5 s and tells them; a species is judged by the second (so the answer to "who is a plain vampire?" is never given)
        if (!CONFIRM.confirm(user.getUUID(), other.getUUID(), user.level().getGameTime())) {
            user.displayClientMessage(Component.translatable(ARMED), true);
            other.displayClientMessage(Component.translatable(ASKED, user.getDisplayName()), true);
            return InteractionResult.sidedSuccess(false);
        }
        if (inject(other)) {
            stack.consume(1, user);
            user.sendSystemMessage(Component.translatable(DONE_TO, other.getDisplayName()));
            other.sendSystemMessage(Component.translatable(DONE));
        } else {
            user.displayClientMessage(Component.translatable(NOTHING), true);
        }
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.supernaturalbloodlines.augustine_syringe"));
    }
}
