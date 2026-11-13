package net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest;

import io.github.manasmods.tensura.data.existence.EntityExistenceData;
import io.github.manasmods.tensura.registry.block.TensuraBlocks;
import io.github.manasmods.tensura.registry.data.TensuraCustomData;
import java.util.List;
import net.favouriteless.enchanted.common.blocks.entity.AltarBlockEntity;
import net.favouriteless.enchanted.common.enchanted.altar.AltarHelper;
import net.favouriteless.enchanted.common.enchanted.altar.PowerProvider;
import net.favouriteless.enchanted.common.init.EBlocks;
import net.favouriteless.enchanted.common.menus.AltarMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;

/** Registered by the part itself (RegisterGameTestsEvent), so these only exist when the part is enabled. */
@PrefixGameTestTemplate(false)
public final class EnchantedTensuraDataGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private EnchantedTensuraDataGameTests() {
    }

    private record Altar(String block, int power, int limit) {
    }

    private static final List<Altar> ALTAR_BLOCKS = List.of(
            new Altar("tensura:magic_ore", 6, 20),
            new Altar("tensura:deepslate_magic_ore", 6, 20),
            new Altar("tensura:magic_ore_block", 10, 10),
            new Altar("tensura:low_quality_magic_crystal_block", 10, 10),
            new Altar("tensura:medium_quality_magic_crystal_block", 10, 10),
            new Altar("tensura:high_quality_magic_crystal_block", 10, 10),
            new Altar("tensura:low_magisteel_block", 10, 10),
            new Altar("tensura:high_magisteel_block", 10, 10),
            new Altar("tensura:pure_magisteel_block", 10, 10),
            new Altar("tensura:mithril_block", 10, 10),
            new Altar("tensura:orichalcum_block", 10, 10),
            new Altar("tensura:adamantite_block", 10, 10),
            new Altar("tensura:hihiirokane_block", 10, 10));

    /** Builds a formed 3x2 altar at {@code base} with ten {@code ring} blocks beside it, and returns its capacity. */
    private static int altarCapacity(GameTestHelper helper, BlockPos base, Block ring) {
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < 10; i++) {
            level.setBlock(base.offset(i % 5, 0, 4 + i / 5), ring.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 2; z++) {
                level.setBlock(base.offset(x, 0, z), EBlocks.ALTAR.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        helper.assertTrue(AltarHelper.tryFormAltar(level, base.offset(2, 0, 1)), "the altar did not form");
        AltarBlockEntity altar = (AltarBlockEntity) level.getBlockEntity(base);
        helper.assertTrue(altar != null, "no altar block entity at " + base);
        altar.firstTick();
        ServerPlayer player = TestPlayers.create(helper);
        return ((AltarMenu) altar.createMenu(0, player.getInventory(), player)).getMaxPower();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void magicBlocksPowerAltar(GameTestHelper helper) {
        // both altars are 40+ blocks up, outside altar_range (32) of the grass and dirt floor (those have altar power
        // of their own) and of each other
        BlockPos base = helper.absolutePos(new BlockPos(0, 40, 0));
        int withStone = altarCapacity(helper, base, Blocks.STONE);
        int withOre = altarCapacity(helper, base.above(40), TensuraBlocks.MAGIC_ORE.get());
        helper.assertTrue(withOre > withStone, "altar capacity with magic ore " + withOre + " is not above stone " + withStone);
        helper.assertTrue(withOre - withStone == 60, "ten magic ore should add 10 x 6, but the difference is " + (withOre - withStone));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void altarPowerDataLoaded(GameTestHelper helper) {
        for (Altar a : ALTAR_BLOCKS) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(a.block()));
            helper.assertTrue(block != Blocks.AIR, "unknown Tensura block " + a.block());
            PowerProvider p = PowerProvider.get(helper.getLevel(), block);
            helper.assertTrue(p != null, "no altar power data for " + a.block());
            helper.assertTrue(p.power() == a.power() && p.limit() == a.limit(),
                    a.block() + " has " + p.power() + "/" + p.limit() + ", expected " + a.power() + "/" + a.limit());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void enchantedMobsHaveEp(GameTestHelper helper) {
        // control: Tensura's own data is found the same way
        helper.assertTrue(existence(helper, "tensura:orc") != null, "control: no existence data for tensura:orc");
        for (String id : List.of("enchanted:mandrake", "enchanted:familiar_cat")) {
            EntityExistenceData d = existence(helper, id);
            helper.assertTrue(d != null, "no existence data for " + id);
            helper.assertTrue(d.maxMagicule() > 0 && d.maxMagicule() >= d.minMagicule() && d.spiritualHP() > 0,
                    "empty existence data for " + id);
        }
        helper.succeed();
    }

    private static EntityExistenceData existence(GameTestHelper helper, String entityId) {
        return helper.getLevel().registryAccess().registryOrThrow(TensuraCustomData.ENTITY_EXISTENCE).stream()
                .filter(d -> d.entity().toString().equals(entityId)).findFirst().orElse(null);
    }
}
