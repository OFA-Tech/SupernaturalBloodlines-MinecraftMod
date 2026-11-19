package net.ofatech.supernaturalbloodlines.parts.species.npc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Order;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.SortRules;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * Hunting trips and chest sorting of a Brother (R148). Only while its owner is online and it is not following. A trip, at night, once per
 * {@code npc_hunt_period_minutes} with Hunt on: the Brother kills up to three animals of the {@code werewolf_prey} tag within 48 blocks of its post (never a tamed,
 * named or baby one); what they drop goes into the container by its post if it fits, and what does not fit stays where the animal fell (nothing is ever dropped at
 * the post, nothing is lost). With Sort on (and {@code npc_chest_sort}) it also tidies that container, at any hour.
 *
 * <p>Which container: the nearest, searched outward from the post (12 blocks), of a plain chest, trapped chest or barrel (not a furnace, a hopper, a shulker box or
 * a modded machine: they are not somebody's store) that the owner could open: the owner's right click on it is offered to the game
 * ({@code CommonHooks.onRightClickBlock}) and a cancelled or denied one (a claim of Open Parties and Claims, any other protection mod) skips the block. That is the
 * smallest check that works with every protection mod; an unclaimed chest of another player cannot be told apart from one of the owner's, so a post is best
 * kept away from them. Shortcut: the Brother does not walk there and back, a trip is one action.
 */
public final class NpcHunt {
    public static final int CHECK_PERIOD = 200;
    public static final double RANGE = 48;
    public static final int STORE_RANGE = 12;
    public static final int MAX_PREY = 3;

    /** The game time of each Brother's last trip and last tidying: in memory, so a restart makes the next one due at once. */
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SORT = new ConcurrentHashMap<>();

    /** What a hunt in progress does with the drops of its kills: the hunter, and the container they go into (null: nowhere, they stay where they fall). */
    private record Carry(Mob hunter, Container chest) {
    }

    private static final ThreadLocal<Carry> CARRY = new ThreadLocal<>();

    private NpcHunt() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post e) -> {
            if (e.getEntity() instanceof Mob m && !m.level().isClientSide && m.tickCount % CHECK_PERIOD == m.getId() % CHECK_PERIOD && m.hasData(SpeciesRegistries.NPC_BOND)) {
                maybeHunt(m);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (LivingDropsEvent e) -> {
            Carry c = CARRY.get();
            if (c != null && c.chest() != null && e.getSource().getEntity() == c.hunter()) {
                for (ItemEntity drop : e.getDrops()) {
                    ItemStack left = HopperBlockEntity.addItem(null, c.chest(), drop.getItem().copy(), null);
                    drop.setItem(left); // an empty stack is not spawned; what did not fit stays on the ground where the animal fell
                }
                e.getDrops().removeIf(d -> d.getItem().isEmpty());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            LAST.clear();
            LAST_SORT.clear();
        });
    }

    public static boolean isNight(ServerLevel level) {
        long t = Math.floorMod(level.getDayTime(), 24000L);
        return t >= MoonRule.NIGHT_START && t < MoonRule.NIGHT_END;
    }

    public static int maybeHunt(Mob m) {
        return m.level() instanceof ServerLevel level ? maybeHunt(m, isNight(level)) : 0;
    }

    /** One look at whether this Brother hunts or tidies now; does so if so. @return how many animals it killed (0 when it did not hunt) */
    public static int maybeHunt(Mob m, boolean night) {
        NpcBond bond = m.getData(SpeciesRegistries.NPC_BOND);
        if (!bond.bonded() || !NpcRules.hasOurOrders(bond.source()) || !(m.level() instanceof ServerLevel level)) {
            return 0;
        }
        SpeciesConfig.MemberTuning t = SpeciesConfig.members();
        long now = level.getGameTime();
        long period = t.huntPeriodMinutes() * 1200L;
        boolean ownerOnline = SireCall.player(level.getServer(), bond.owner()) != null;
        Order order = NpcMembers.effectiveOrder(level.getServer(), m);
        boolean fighting = m.getTarget() != null;
        Long lastHunt = LAST.get(m.getUUID());
        if (NpcRules.huntDue(t.huntTrips(), bond.hunt(), order, ownerOnline, night, lastHunt == null ? Long.MAX_VALUE : now - lastHunt, period, fighting)) {
            LAST.put(m.getUUID(), now);
            return go(m, bond, true, t.chestSort() && bond.sort());
        }
        Long lastSort = LAST_SORT.get(m.getUUID());
        if (NpcRules.sortDue(t.chestSort(), bond.sort(), order, ownerOnline, lastSort == null ? Long.MAX_VALUE : now - lastSort, period, fighting)) {
            LAST_SORT.put(m.getUUID(), now);
            go(m, bond, false, true);
        }
        return 0;
    }

    /** The trip itself, with no timing checks: the animals, the store, the sort. Nothing happens while the owner is not online. */
    public static int go(Mob m, NpcBond bond, boolean hunt, boolean sort) {
        ServerLevel level = (ServerLevel) m.level();
        ServerPlayer owner = SireCall.player(level.getServer(), bond.owner());
        if (owner == null) {
            return 0;
        }
        BlockPos post = bond.post() != 0 ? BlockPos.of(bond.post()) : m.blockPosition();
        Container chest = findContainer(level, owner, post);
        int killed = 0;
        if (hunt) {
            List<LivingEntity> prey = level.getEntitiesOfClass(LivingEntity.class, new AABB(post).inflate(RANGE), NpcHunt::isPrey).stream()
                    .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5))).limit(MAX_PREY).toList();
            CARRY.set(new Carry(m, chest));
            try {
                for (LivingEntity p : prey) {
                    p.hurt(level.damageSources().mobAttack(m), Float.MAX_VALUE);
                    if (!p.isAlive()) {
                        killed++;
                    }
                }
            } finally {
                CARRY.remove();
            }
        }
        if (sort && chest != null) {
            sortContainer(chest);
        }
        return killed;
    }

    /** An animal a Brother may hunt: of the prey tag, alive, not tamed, not named, not a baby. */
    static boolean isPrey(LivingEntity e) {
        return e.isAlive() && e.getType().is(NpcPackBrain.PREY) && !e.hasCustomName() && !e.isBaby()
                && !(e instanceof OwnableEntity pet && pet.getOwnerUUID() != null);
    }

    /** Whether a block may be a Brother's store at all: a plain chest, trapped chest or barrel. */
    public static boolean isStoreBlock(BlockEntity be) {
        return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity;
    }

    /**
     * The nearest store within {@link #STORE_RANGE} of {@code post}, looking outward from the post, that the owner could open; null when there is none. A double chest
     * is its whole container.
     */
    static Container findContainer(ServerLevel level, ServerPlayer owner, BlockPos post) {
        for (BlockPos p : BlockPos.withinManhattan(post, STORE_RANGE, STORE_RANGE / 2, STORE_RANGE)) {
            if (p.distSqr(post) > STORE_RANGE * STORE_RANGE) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(p);
            if (be == null || !isStoreBlock(be) || !mayUse(owner, p)) {
                continue;
            }
            if (be instanceof ChestBlockEntity && level.getBlockState(p).getBlock() instanceof ChestBlock chest) {
                Container whole = ChestBlock.getContainer(chest, level.getBlockState(p), level, p, true);
                return whole != null ? whole : (Container) be;
            }
            return (Container) be;
        }
        return null;
    }

    /** Whether the owner's right click on this block would be allowed: claims and other protection mods cancel it for someone who may not open the block. */
    static boolean mayUse(ServerPlayer owner, BlockPos p) {
        var event = CommonHooks.onRightClickBlock(owner, InteractionHand.MAIN_HAND, p, new BlockHitResult(Vec3.atCenterOf(p), Direction.UP, p, false));
        return !event.isCanceled() && event.getUseBlock() != TriState.FALSE;
    }

    /** Tidies a container in place: stacks merged, then ordered. Nothing is taken out of it: if the result would not fit the way it is, it is left as it was. */
    static void sortContainer(Container chest) {
        List<ItemStack> original = new ArrayList<>();
        List<ItemStack> protos = new ArrayList<>();
        List<SortRules.Slot> slots = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            original.add(s.copy());
            if (s.isEmpty()) {
                continue;
            }
            total += s.getCount();
            int idx = -1;
            for (int k = 0; k < protos.size(); k++) {
                if (ItemStack.isSameItemSameComponents(protos.get(k), s)) {
                    idx = k;
                    break;
                }
            }
            if (idx < 0) {
                protos.add(s.copyWithCount(1));
                idx = protos.size() - 1;
            }
            slots.add(new SortRules.Slot(BuiltInRegistries.ITEM.getKey(s.getItem()) + "#" + idx, s.getCount(), s.getMaxStackSize()));
        }
        List<SortRules.Slot> sorted = SortRules.sort(slots, key -> category(protos.get(Integer.parseInt(key.substring(key.lastIndexOf('#') + 1)))));
        if (sorted.size() > chest.getContainerSize() || sorted.stream().mapToInt(SortRules.Slot::count).sum() != total) {
            return;
        }
        for (int i = 0; i < chest.getContainerSize(); i++) {
            chest.setItem(i, i < sorted.size()
                    ? protos.get(Integer.parseInt(sorted.get(i).id().substring(sorted.get(i).id().lastIndexOf('#') + 1))).copyWithCount(sorted.get(i).count())
                    : ItemStack.EMPTY);
        }
        chest.setChanged();
    }

    /** Tools and armour first, then food, then the rest, then blocks. */
    static int category(ItemStack s) {
        if (s.getItem() instanceof TieredItem || s.getItem() instanceof ArmorItem) {
            return 0;
        }
        if (s.has(net.minecraft.core.component.DataComponents.FOOD)) {
            return 1;
        }
        return s.getItem() instanceof BlockItem ? 3 : 2;
    }

    /** For tests: forget when Brothers last hunted and sorted. */
    public static void reset() {
        LAST.clear();
        LAST_SORT.clear();
    }
}
