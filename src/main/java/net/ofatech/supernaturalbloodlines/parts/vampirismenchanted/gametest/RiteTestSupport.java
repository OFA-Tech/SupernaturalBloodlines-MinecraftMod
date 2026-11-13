package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest;

import java.util.List;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.favouriteless.enchanted.common.init.EData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Shared by the rite GameTests: start the real rite JSON and look at what it left behind. */
final class RiteTestSupport {
    private static final String NS = SupernaturalBloodlines.MODID;

    private RiteTestSupport() {
    }

    static RiteType type(GameTestHelper helper, String path) {
        return helper.getLevel().registryAccess().registryOrThrow(EData.RITE_TYPES_REGISTRY)
                .get(ResourceLocation.fromNamespaceAndPath(NS, path));
    }

    static BlockPos centre(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(1, 1, 1));
    }

    static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    /** Starts the rite as {@code caster} with {@code items} as the consumed ingredients. */
    static Rite start(GameTestHelper helper, String path, ServerPlayer caster, List<ItemStack> items) {
        Rite rite = type(helper, path).create(helper.getLevel(), centre(helper), caster.getUUID(), items);
        rite.start();
        return rite;
    }

    /** FakePlayers are not in the level, which is where the rites look for players: put one there on the circle. */
    static void place(GameTestHelper helper, ServerPlayer p) {
        p.setPos(Vec3.atBottomCenterOf(centre(helper)).add(0, 1, 0));
        helper.getLevel().addNewPlayer(p);
    }

    static void remove(GameTestHelper helper, ServerPlayer p) {
        ServerLevel level = helper.getLevel();
        level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        // every rite test removes its players in a finally: leave no ItemEntity behind for the next test in this cell
        clearDropped(helper);
    }

    /** Items lying within 1.5 blocks of the circle centre (the test cells are further apart than that): what Enchanted gives back when a rite is cancelled. */
    static int dropped(GameTestHelper helper, String itemId) {
        int n = 0;
        for (ItemEntity i : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(centre(helper)).inflate(1.5))) {
            if (i.getItem().is(item(itemId))) {
                n += i.getItem().getCount();
            }
        }
        return n;
    }

    static void clearDropped(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(centre(helper)).inflate(1.5)).forEach(Entity::discard);
    }
}
