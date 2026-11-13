package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest;

import net.favouriteless.enchanted.common.blocks.entity.AltarBlockEntity;
import net.favouriteless.enchanted.common.enchanted.altar.AltarHelper;
import net.favouriteless.enchanted.common.enchanted.altar.PowerProvider;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.enchanted.poppet.PoppetColour;
import net.favouriteless.enchanted.common.enchanted.poppet.PoppetHelper;
import net.favouriteless.enchanted.common.init.EBlocks;
import net.favouriteless.enchanted.common.init.EItems;
import net.favouriteless.enchanted.common.items.poppets.PlayerPoppetItem;
import net.favouriteless.enchanted.common.menus.AltarMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.PartBlocks;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.PartItems;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.*;

/**
 * Blood Offering and the Sun Poppet. Enchanted only consults player poppets on a would-be lethal hit (damage at least
 * the player's health), so a poppet "absorbs" a lethal hit of its damage type: that hit is cancelled and the poppet is used.
 */
@PrefixGameTestTemplate(false)
public final class VampirismEnchantedPoppetGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private VampirismEnchantedPoppetGameTests() {
    }

    static DamageSource damage(GameTestHelper helper, ResourceKey<DamageType> key) {
        return new DamageSource(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }

    static DamageSource damage(GameTestHelper helper, String id) {
        return damage(helper, ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(id)));
    }

    static void bound(ServerPlayer player, Item poppet) {
        ItemStack stack = new ItemStack(poppet);
        PoppetHelper.bind(stack, player);
        player.getInventory().add(stack);
    }

    /** {@code Inventory.add} copies a damageable stack, so look at what is in the inventory, not the original. */
    static boolean used(ServerPlayer player, Item poppet) {
        for (ItemStack s : player.getInventory().items) {
            if (s.is(poppet)) {
                return s.getDamageValue() > 0;
            }
        }
        return true;
    }

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
    public static void bloodOfferingPowersAltar(GameTestHelper helper) {
        PowerProvider p = PowerProvider.get(helper.getLevel(), PartBlocks.BLOOD_STONE.get());
        helper.assertTrue(p != null && p.power() == 25 && p.limit() == 16, "blood_stone altar data is " + p);
        // 120 and 160 blocks up: outside altar_range (32) of the floor (grass and dirt have altar power), of each other,
        // and of the altars other parts' tests build at 40 and 80 in neighbouring cells (their blocks stay in the world)
        BlockPos base = helper.absolutePos(new BlockPos(0, 120, 0));
        int withStone = altarCapacity(helper, base, Blocks.STONE);
        int withBlood = altarCapacity(helper, base.above(40), PartBlocks.BLOOD_STONE.get());
        helper.assertTrue(withBlood - withStone == 250, "ten blood stone should add 10 x 25, but the difference is " + (withBlood - withStone));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodOfferingRiteMakesBloodStone(GameTestHelper helper) {
        RiteType type = type(helper, "blood_offering");
        helper.assertTrue(type != null, "the blood_offering rite did not load");
        DataComponentType<?> bottleBlood = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("vampirism:bottle_blood"));
        ItemStack bottle = type.getItems().stream().filter(s -> s.is(item("vampirism:blood_bottle"))).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(bottle.getCount() == 4, "the rite wants " + bottle.getCount() + " blood bottles, not 4");
        helper.assertTrue(bottleBlood != null && bottle.getComponentsPatch().get(bottleBlood) != null,
                "the rite does not ask for a FULL bottle, so empty bottles would pay for it");
        helper.assertTrue(type.getItems().stream().anyMatch(s -> s.is(item("minecraft:stone"))), "the rite wants no stone");
        helper.assertTrue(type.getOutputs().stream().anyMatch(s -> s.is(PartItems.BLOOD_STONE.get())), "the rite does not list blood_stone as its output");

        clearDropped(helper);
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        try {
            start(helper, "blood_offering", caster, List.of());
            helper.assertTrue(dropped(helper, "supernaturalbloodlines:blood_stone") == 1, "the rite did not produce one blood stone");
        } finally {
            remove(helper, caster);
        }
        helper.succeed();
    }

    /** Four bottles (one per ItemEntity, they do not stack) and a stone, lying on a small ritual chalk circle. */
    private static boolean riteMatches(GameTestHelper helper, RiteType type, ItemStack bottle) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = centre(helper);
        List<BlockPos> chalk = new ArrayList<>();
        type.getShapes().forEach((shape, block) -> {
            List<String> rows = shape.value().getStrings();
            int mid = rows.size() / 2;
            for (int z = 0; z < rows.size(); z++) {
                for (int x = 0; x < rows.get(z).length(); x++) {
                    if (rows.get(z).charAt(x) == 'X') {
                        BlockPos p = centre.offset(x - mid, 0, z - mid);
                        // chalk needs a floor and a plain placement (no neighbour update) or it breaks at once
                        level.setBlock(p.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                        level.setBlock(p, block.defaultBlockState(), Block.UPDATE_CLIENTS);
                        chalk.add(p);
                    }
                }
            }
        });
        try {
            for (int i = 0; i < 4; i++) {
                ItemStack one = bottle.copy();
                one.setCount(1);
                level.addFreshEntity(new ItemEntity(level, centre.getX() + 0.5, centre.getY() + 0.1, centre.getZ() + 0.5, one));
            }
            level.addFreshEntity(new ItemEntity(level, centre.getX() + 0.5, centre.getY() + 0.1, centre.getZ() + 0.5, new ItemStack(item("minecraft:stone"))));
            List<Entity> found = new ArrayList<>(level.getEntitiesOfClass(ItemEntity.class, type.getBounds(centre)));
            helper.assertTrue(found.size() == 5, "expected 5 item entities on the circle, found " + found.size());
            return type.matches(level, centre, found);
        } finally {
            chalk.forEach(p -> {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(p.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            });
            clearDropped(helper);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodOfferingNeedsFullBottles(GameTestHelper helper) {
        RiteType type = type(helper, "blood_offering");
        ItemStack full = type.getItems().stream().filter(s -> s.is(item("vampirism:blood_bottle"))).findFirst().orElseThrow();
        helper.assertTrue(riteMatches(helper, type, full), "control: four full bottles and a stone on the circle do not match the rite");
        helper.assertTrue(!riteMatches(helper, type, new ItemStack(item("vampirism:blood_bottle"))),
                "four EMPTY blood bottles and a stone match the rite");
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunPoppetAbsorbsSunDamage(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.createVulnerable(helper);
        Item poppet = PartItems.SUN_POPPET.get();
        bound(player, poppet);
        float before = player.getHealth();
        player.hurt(damage(helper, "vampirism:sun_damage"), before + 100);
        helper.assertTrue(player.isAlive(), "a lethal sun hit killed a player with a bound sun poppet");
        helper.assertTrue(player.getHealth() == before, "lethal sun damage got through: " + player.getHealth() + " of " + before);
        helper.assertTrue(used(player, poppet), "the sun poppet was not used");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunPoppetIgnoresOtherDamage(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.createVulnerable(helper);
        Item poppet = PartItems.SUN_POPPET.get();
        bound(player, poppet);
        float before = player.getHealth();
        player.hurt(damage(helper, DamageTypes.ON_FIRE), before + 100);
        helper.assertTrue(!player.isAlive() && player.getHealth() <= 0, "a lethal fire hit was absorbed by the sun poppet");
        helper.assertTrue(!used(player, poppet), "a non-sun hit used up the sun poppet");

        PlayerPoppetItem item = (PlayerPoppetItem) poppet;
        helper.assertTrue(item.protectsAgainst(damage(helper, "vampirism:sun_damage")), "sun damage is not protected");
        for (String other : List.of("vampirism:holy_water", "vampirism:vampire_on_fire", "vampirism:blood_loss", "minecraft:generic_kill", "minecraft:magic")) {
            helper.assertTrue(!item.protectsAgainst(damage(helper, other)), other + " is protected by the sun poppet");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunPoppetIgnoresNonLethalHits(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.createVulnerable(helper);
        Item poppet = PartItems.SUN_POPPET.get();
        bound(player, poppet);
        float before = player.getHealth();
        player.hurt(damage(helper, "vampirism:sun_damage"), 1);
        helper.assertTrue(player.getHealth() < before, "a non-lethal sun hit did no damage");
        helper.assertTrue(!used(player, poppet), "a non-lethal hit used up the poppet");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void poppetsMatchEnchantedStyle(GameTestHelper helper) {
        PlayerPoppetItem sun = PartItems.SUN_POPPET.get();
        int durability = EItems.MAGIC_POPPET.get().getDefaultInstance().getMaxDamage();
        helper.assertTrue(sun.getColour().equals(PoppetColour.FIRE), "the sun poppet is not fire coloured");
        helper.assertTrue(sun.getDefaultInstance().getMaxDamage() == durability, "durability differs from Enchanted's poppets: " + sun.getDefaultInstance().getMaxDamage());
        helper.assertTrue(sun.getDescriptionId().equals("item." + NS + ".sun_poppet"), "wrong item id");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(NS, "spinning/sun_poppet")).isPresent(),
                "the sun poppet recipe did not load");

        boolean werewolves = ModList.get().isLoaded("werewolves");
        helper.assertTrue((PartItems.SILVER_POPPET != null) == werewolves, "the silver poppet must exist exactly when Werewolves is loaded");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(NS, "silver_poppet")) == werewolves,
                "silver_poppet registry entry does not follow Werewolves");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(NS, "spinning/silver_poppet")).isPresent() == werewolves,
                "the silver poppet recipe does not follow Werewolves");
        helper.assertTrue((type(helper, "blood_offering") != null), "blood_offering is missing");
        helper.succeed();
    }
}
