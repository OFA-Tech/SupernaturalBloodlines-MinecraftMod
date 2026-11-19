package net.ofatech.supernaturalbloodlines.parts.species.npc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Order;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.SortRules;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;

/**
 * Hunting trips and chest sorting of a Brother (R148). A Brother with Hunt on that rests or guards, at night, once per {@code npc_hunt_period_minutes},
 * kills up to three animals of the {@code werewolf_prey} tag within 48 blocks of its post (never a tamed one, a named one or a baby), keeps what they drop in a
 * carry list (the drops event of those kills is taken over, so nothing lies about), and puts it in the nearest container within 12 blocks of its post
 * through the container's item handler. What does not fit is dropped at the post. With Sort on (and {@code npc_chest_sort}) it then tidies that container:
 * partial stacks merged, then ordered ({@link SortRules}). Shortcut: the Brother does not walk there and back, the trip is one action; and a claim of
 * Open Parties and Claims is not looked at, so hunting stays inside 48 blocks of the post (the Pack page says so).
 */
public final class NpcHunt {
    public static final int CHECK_PERIOD = 200;
    public static final double RANGE = 48;
    public static final int STORE_RANGE = 12;
    public static final int MAX_PREY = 3;

    /** The game time of each Brother's last trip: in memory, so a restart makes the next one due at once at night. */
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

    /** What a hunt in progress collects: the hunter and what its kills dropped. */
    private static final class Carry {
        final Mob hunter;
        final List<ItemStack> items = new ArrayList<>();

        Carry(Mob hunter) {
            this.hunter = hunter;
        }
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
            if (c != null && e.getSource().getEntity() == c.hunter) {
                for (ItemEntity drop : e.getDrops()) {
                    c.items.add(drop.getItem().copy());
                }
                e.setCanceled(true); // nothing lies on the ground: the hunter carries it
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> LAST.clear());
    }

    public static boolean isNight(ServerLevel level) {
        long t = Math.floorMod(level.getDayTime(), 24000L);
        return t >= MoonRule.NIGHT_START && t < MoonRule.NIGHT_END;
    }

    /** One look at whether this Brother goes now; goes if so. @return how many animals it killed (0 when it did not go) */
    public static int maybeHunt(Mob m) {
        NpcBond bond = m.getData(SpeciesRegistries.NPC_BOND);
        if (!bond.bonded() || !NpcRules.hasOurOrders(bond.source()) || !(m.level() instanceof ServerLevel level)) {
            return 0;
        }
        SpeciesConfig.MemberTuning t = SpeciesConfig.members();
        long now = level.getGameTime();
        Long last = LAST.get(m.getUUID());
        Order order = NpcMembers.effectiveOrder(level.getServer(), bond);
        if (!NpcRules.huntDue(t.huntTrips(), bond.hunt(), order, isNight(level), last == null ? Long.MAX_VALUE : now - last, t.huntPeriodMinutes() * 1200L, m.getTarget() != null)) {
            return 0;
        }
        LAST.put(m.getUUID(), now);
        return go(m, bond, t.chestSort() && bond.sort());
    }

    /** The trip itself, with no checks: the animals, the carry, the store, the sort. */
    public static int go(Mob m, NpcBond bond, boolean sort) {
        ServerLevel level = (ServerLevel) m.level();
        BlockPos post = bond.post() != 0 ? BlockPos.of(bond.post()) : m.blockPosition();
        List<LivingEntity> prey = level.getEntitiesOfClass(LivingEntity.class, new AABB(post).inflate(RANGE), e -> isPrey(e)).stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5))).limit(MAX_PREY).toList();
        Carry carry = new Carry(m);
        CARRY.set(carry);
        int killed = 0;
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
        store(level, m, post, carry.items, sort);
        return killed;
    }

    /** An animal a Brother may hunt: of the prey tag, alive, not tamed, not named, not a baby. */
    static boolean isPrey(LivingEntity e) {
        return e.isAlive() && e.getType().is(NpcPackBrain.PREY) && !e.hasCustomName() && !e.isBaby()
                && !(e instanceof OwnableEntity pet && pet.getOwnerUUID() != null);
    }

    /** Puts {@code items} into the nearest container within 12 blocks of {@code post}; what does not fit is dropped at the post. Sorts it when asked. */
    static void store(ServerLevel level, Mob m, BlockPos post, List<ItemStack> items, boolean sort) {
        IItemHandler chest = nearestContainer(level, post);
        for (ItemStack stack : items) {
            ItemStack left = stack;
            if (chest != null) {
                for (int i = 0; i < chest.getSlots() && !left.isEmpty(); i++) {
                    left = chest.insertItem(i, left, false);
                }
            }
            if (!left.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, post.getX() + 0.5, post.getY() + 0.5, post.getZ() + 0.5, left));
            }
        }
        if (sort && chest != null) {
            sortContainer(level, chest, post);
        }
    }

    static IItemHandler nearestContainer(ServerLevel level, BlockPos post) {
        IItemHandler best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(post.offset(-STORE_RANGE, -STORE_RANGE / 2, -STORE_RANGE), post.offset(STORE_RANGE, STORE_RANGE / 2, STORE_RANGE))) {
            double d = p.distSqr(post);
            if (d < bestD && d <= STORE_RANGE * STORE_RANGE) {
                IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, p, null);
                if (h != null && h.getSlots() > 0) {
                    best = h;
                    bestD = d;
                }
            }
        }
        return best;
    }

    /** Tidies a container: everything out, merged and ordered, back in; whatever does not go back in is dropped beside it, never lost. */
    static void sortContainer(ServerLevel level, IItemHandler chest, BlockPos near) {
        List<ItemStack> taken = new ArrayList<>();
        for (int i = 0; i < chest.getSlots(); i++) {
            ItemStack s = chest.getStackInSlot(i);
            if (!s.isEmpty()) {
                ItemStack out = chest.extractItem(i, s.getCount(), false);
                if (!out.isEmpty()) {
                    taken.add(out);
                }
            }
        }
        List<ItemStack> protos = new ArrayList<>();
        List<SortRules.Slot> slots = new ArrayList<>();
        for (ItemStack s : taken) {
            int idx = -1;
            for (int k = 0; k < protos.size(); k++) {
                if (ItemStack.isSameItemSameComponents(protos.get(k), s)) {
                    idx = k;
                    break;
                }
            }
            if (idx < 0) {
                protos.add(s);
                idx = protos.size() - 1;
            }
            slots.add(new SortRules.Slot(BuiltInRegistries.ITEM.getKey(s.getItem()) + "#" + idx, s.getCount(), s.getMaxStackSize()));
        }
        for (SortRules.Slot slot : SortRules.sort(slots, key -> category(protos.get(Integer.parseInt(key.substring(key.lastIndexOf('#') + 1)))))) {
            ItemStack left = protos.get(Integer.parseInt(slot.id().substring(slot.id().lastIndexOf('#') + 1))).copyWithCount(slot.count());
            for (int i = 0; i < chest.getSlots() && !left.isEmpty(); i++) {
                left = chest.insertItem(i, left, false);
            }
            if (!left.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, near.getX() + 0.5, near.getY() + 0.5, near.getZ() + 0.5, left));
            }
        }
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

    /** For tests: forget when Brothers last hunted. */
    public static void reset() {
        LAST.clear();
    }
}
