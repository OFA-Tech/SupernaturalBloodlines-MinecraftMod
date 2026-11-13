package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest;

import de.teamlapen.vampirism.api.datamaps.IEntityBlood;
import java.util.List;
import net.favouriteless.enchanted.common.enchanted.altar.PowerProvider;
import net.favouriteless.enchanted.common.init.EData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Registered by the part itself (RegisterGameTestsEvent), so these only exist when the part is enabled. */
@PrefixGameTestTemplate(false)
public final class VampirismEnchantedDataGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final TagKey<Item> GARLIC = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:crops/garlic"));

    private VampirismEnchantedDataGameTests() {
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    /** The result id of crafting these items in a row, or null when no recipe matches. */
    private static String craft(GameTestHelper helper, String... ids) {
        List<ItemStack> stacks = java.util.Arrays.stream(ids).map(id -> new ItemStack(item(id))).toList();
        CraftingInput input = CraftingInput.of(stacks.size(), 1, stacks);
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(RecipeHolder::value)
                .map((CraftingRecipe r) -> BuiltInRegistries.ITEM.getKey(r.assemble(input, helper.getLevel().registryAccess()).getItem()).toString())
                .orElse(null);
    }

    private static PowerProvider altarPower(GameTestHelper helper, String blockId) {
        return PowerProvider.get(ResourceLocation.parse(blockId), helper.getLevel().registryAccess().registryOrThrow(EData.ALTAR_BLOCK_REGISTRY));
    }

    @SuppressWarnings("unchecked")
    private static int entityBlood(String entityId) throws ReflectiveOperationException {
        DataMapType<EntityType<?>, IEntityBlood> map = (DataMapType<EntityType<?>, IEntityBlood>)
                Class.forName("de.teamlapen.vampirism.core.ModDataMaps").getField("ENTITY_BLOOD_MAP").get(null);
        IEntityBlood entry = BuiltInRegistries.ENTITY_TYPE.getData(map, ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(entityId)));
        return entry == null ? -1 : entry.blood();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void enchantedGarlicCountsAsGarlic(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(item("vampirism:garlic")).is(GARLIC), "control: vampirism:garlic is not in c:crops/garlic");
        helper.assertTrue(new ItemStack(item("enchanted:garlic")).is(GARLIC), "enchanted:garlic is not in c:crops/garlic");
        // control, then the real Vampirism recipe (it reads the tag) accepts the Enchanted item
        helper.assertTrue("vampirism:garlic_bread".equals(craft(helper, "vampirism:garlic", "minecraft:bread")), "control: garlic bread recipe");
        String result = craft(helper, "enchanted:garlic", "minecraft:bread");
        helper.assertTrue("vampirism:garlic_bread".equals(result), "Enchanted garlic + bread crafts " + result);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void enchantedWolfsbaneConverts(GameTestHelper helper) {
        boolean werewolves = ModList.get().isLoaded("werewolves");
        // Werewolves has no wolfsbane tag, so the parity is two 1:1 recipes that exist only with Werewolves
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(NS, "wolfsbane_from_enchanted")).isPresent() == werewolves,
                "wolfsbane_from_enchanted present=" + !werewolves + " with Werewolves loaded=" + werewolves);
        if (werewolves) {
            String toWerewolves = craft(helper, "enchanted:wolfsbane_flower");
            helper.assertTrue("werewolves:wolfsbane".equals(toWerewolves), "Enchanted wolfsbane crafts " + toWerewolves);
            String toEnchanted = craft(helper, "werewolves:wolfsbane");
            helper.assertTrue("enchanted:wolfsbane_flower".equals(toEnchanted), "Werewolves wolfsbane crafts " + toEnchanted);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampirismPlantsPowerAltar(GameTestHelper helper) {
        // control: Enchanted's own data is found the same way
        PowerProvider own = altarPower(helper, "enchanted:garlic");
        helper.assertTrue(own != null && own.power() == 8 && own.limit() == 20, "control: enchanted:garlic altar power is " + own);
        for (String block : List.of("vampirism:garlic", "vampirism:vampire_orchid")) {
            PowerProvider p = altarPower(helper, block);
            helper.assertTrue(p != null && p.power() == 8 && p.limit() == 20, block + " altar power is " + p);
        }
        boolean werewolves = ModList.get().isLoaded("werewolves");
        for (String block : List.of("werewolves:wolfsbane", "werewolves:wolf_berry_bush")) {
            PowerProvider p = altarPower(helper, block);
            if (werewolves) {
                helper.assertTrue(p != null && p.power() == 8 && p.limit() == 20, block + " altar power is " + p);
            } else {
                helper.assertTrue(p == null, block + " has altar data without Werewolves: " + p);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void enchantedMobsHaveBlood(GameTestHelper helper) throws Exception {
        // control: the data map is readable and our entries did not replace Vampirism's own
        helper.assertTrue(entityBlood("minecraft:cat") == 3, "control: cat blood is " + entityBlood("minecraft:cat"));
        helper.assertTrue(entityBlood("enchanted:familiar_cat") == 3, "familiar cat blood is " + entityBlood("enchanted:familiar_cat"));
        helper.assertTrue(entityBlood("enchanted:mandrake") == 0, "mandrake blood is " + entityBlood("enchanted:mandrake"));
        helper.succeed();
    }
}
