package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactsConfig;
import net.ofatech.supernaturalbloodlines.shared.species.DoppelgangerIndex;

/**
 * Points to the nearest vampire, hybrid or Original within {@link #RANGE} blocks. It rides on the vanilla
 * {@code lodestone_tracker} component (untracked, so no lodestone is needed) which the server refreshes once a second;
 * with nobody in range the tracker is empty and the needle spins. The client side is {@code ArtifactsClient}.
 * <p>
 * Sneak + use switches it to the second mode (D8), which looks for the nearest doppelgänger instead: a villager within
 * {@code compass_doppelganger_range} (only the loaded ones, through {@link DoppelgangerIndex}, never a world scan) or a player
 * carrying the gene anywhere in the holder's dimension. Only the holder learns anything: the target is never touched or told.
 */
public class VampireCompassItem extends Item {
    public static final int RANGE = 64;
    public static final int PERIOD = 20;

    public VampireCompassItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static CompassMode mode(ItemStack stack) {
        return stack.getOrDefault(ArtifactItems.COMPASS_MODE.get(), CompassMode.VAMPIRES);
    }

    /**
     * The needle is only kept up to date while the compass is in the main hand or the off hand, and its target is cleared as soon as it
     * is not (put away in the inventory here; dropped or put in a container in {@link CompassPrivacy}). A target in the tracker is synced
     * to clients and saved with the stack, and in Doppelgänger mode it is a player's exact position, which only the holder looking at the
     * needle may learn.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide) {
            return;
        }
        if (!heldBy(stack, holder)) {
            clearTarget(stack);
        } else if (holder.tickCount % PERIOD == 0) {
            update(stack, level, holder);
        }
    }

    /** In the main hand or the off hand of {@code holder} (the very same stack, not an equal one). */
    public static boolean heldBy(ItemStack stack, Entity holder) {
        return holder instanceof LivingEntity living && (living.getMainHandItem() == stack || living.getOffhandItem() == stack);
    }

    /** Empties the needle if it points anywhere; a stack with no target is left untouched (no component is added). */
    public static void clearTarget(ItemStack stack) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker != null && tracker.target().isPresent()) {
            stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), false));
        }
    }

    /**
     * Sneak + use toggles the mode. The toggle does not search: it only empties the needle, so it does not keep pointing at
     * the old mode's target, and the next periodic update finds the new one.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        CompassMode next = mode(stack).toggled();
        stack.set(ArtifactItems.COMPASS_MODE.get(), next);
        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), false));
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("supernaturalbloodlines.compass.switched", Component.translatable(next.langKey())), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("supernaturalbloodlines.compass.mode", Component.translatable(mode(stack).langKey())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("supernaturalbloodlines.compass.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** A new target is not a new item: no re-equip animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    public static void update(ItemStack stack, Level level, Entity holder) {
        Optional<GlobalPos> target = mode(stack) == CompassMode.DOPPELGANGERS
                ? nearestDoppelganger(level, holder).map(pos -> GlobalPos.of(level.dimension(), pos))
                : nearestVampire(level, holder).map(e -> GlobalPos.of(level.dimension(), e.blockPosition()));
        LodestoneTracker tracker = new LodestoneTracker(target, false);
        // set only on a change, so the stack is not re-sent every second
        if (!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER))) {
            stack.set(DataComponents.LODESTONE_TRACKER, tracker);
        }
    }

    private static Optional<LivingEntity> nearestVampire(Level level, Entity holder) {
        return level.getEntitiesOfClass(LivingEntity.class, holder.getBoundingBox().inflate(RANGE),
                        e -> e != holder && e.isAlive() && e.distanceToSqr(holder) <= (double) RANGE * RANGE && Supernatural.isVampiric(e)
                                && !net.ofatech.supernaturalbloodlines.core.boss.CloakAccess.cloaked(e)
                                && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(holder)));
    }

    private static Optional<net.minecraft.core.BlockPos> nearestDoppelganger(Level level, Entity holder) {
        if (!(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        List<Entity> villagers = new ArrayList<>();
        for (Entity e : DoppelgangerIndex.villagers(server)) {
            if (e.isAlive() && e != holder) {
                villagers.add(e);
            }
        }
        List<Entity> players = new ArrayList<>();
        for (Player p : server.players()) {
            if (p != holder && p.isAlive() && !p.isCreative() && !p.isSpectator() && BloodlineAccess.get(p).doppelgangerGene()) {
                players.add(p);
            }
        }
        List<Vec3> villagerPositions = villagers.stream().map(Entity::position).toList();
        List<Vec3> playerPositions = players.stream().map(Entity::position).toList();
        return CompassRules.nearestDoppelganger(holder.position(), villagerPositions, ArtifactsConfig.compassDoppelgangerRange(), playerPositions)
                .map(net.minecraft.core.BlockPos::containing);
    }
}
